package com.example.digitalizedphotobook;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.content.res.AppCompatResources;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.digitalizedphotobook.classes.Quadrilateral;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.security.AccessController.getContext;


public class AdjustmentActivity extends AppCompatActivity {
    private static final String TAG = "AdjustmentActivity123";
    private static final int REQUEST_CODE = 99;
    private ImageView ivBack, ivCrop, ivConfirm, ivRotateLeft, ivRotateRight, ivResult;
    private PolygonView polygonView;
    private RelativeLayout rellay;
    private FrameLayout frmSource;
    private File mFile , mFile2;
    private String imagePath;
    private Bitmap bmp, newBmp, resizedBmp;
    private Mat mat;
    private Quadrilateral quad;
    private boolean isFourPointed = false;
    private boolean isCropped = false;
    private int reqCode;
    private double gammaValue = 1.0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public AdjustmentActivity() {
    }

    private void showToast(final String text) {
        Toast toast = Toast.makeText(AdjustmentActivity.this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 30);
        toast.show();
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_adjustment);
        ivBack = findViewById(R.id.ivBack);
        ivRotateLeft = findViewById(R.id.ivRotateLeft);
        ivRotateRight = findViewById(R.id.ivRotateRight);
        ivCrop = findViewById(R.id.ivCrop);
        ivConfirm = findViewById(R.id.ivConfirm);
        ivResult = findViewById(R.id.ivResult);
        frmSource = findViewById(R.id.sourceFrame);
        polygonView = findViewById(R.id.polygonView);
        rellay = findViewById(R.id.rellay2);
        if (!OpenCVLoader.initDebug()) {
            return;
        }
        int permissionCheck = ContextCompat.checkSelfPermission(AdjustmentActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PermissionChecker.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission Not Granted");
            ActivityCompat.requestPermissions(AdjustmentActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            return;
        }

        imagePath = getIntent().getStringExtra("image");
        reqCode = getIntent().getIntExtra("reqCode", -1);
        Log.i(TAG, (reqCode) + imagePath);
        mFile = new File(imagePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        bmp = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
        Matrix matrix = new Matrix();
//        if (reqCode != 0) {
        matrix.postRotate(90);

//        }
        newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);


        Log.i(TAG, "Height: " + newBmp.getHeight() + "Width: " + newBmp.getWidth());

        mat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(newBmp, mat);
//        Imgproc.resize(mat, mat, new Size(984, 1312));
        resizedBmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);

        performGammaCorrection(mat);
        Utils.matToBitmap(mat, resizedBmp);
        findContours(mat);
        if (!isFourPointed) {
            Map<Integer, PointF> pointFs = getOutlinePoints(newBmp);
            polygonView.setPoints(pointFs);
        }
        setBitmap(resizedBmp);

        ivBack.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int eid = event.getAction();
                switch (eid) {
                    case MotionEvent.ACTION_DOWN:
                        ivBack.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.blue), PorterDuff.Mode.SRC_IN);
                        alertDialog();
                        break;
                    case MotionEvent.ACTION_UP:
                        ivBack.setColorFilter(Color.argb(255, 255, 255, 255));
                        break;
                }
                return true;
            }
        });

        ivRotateLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int eid = event.getAction();
                switch (eid) {
                    case MotionEvent.ACTION_DOWN:
                        ivRotateLeft.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.blue), PorterDuff.Mode.SRC_IN);
                        ivResult.setRotation(ivResult.getRotation() - 90);
                        polygonView.setRotation(polygonView.getRotation() - 90);
                        if (ivResult.getRotation() == -360 || polygonView.getRotation() == -360) {
                            ivResult.setRotation(0);
                            polygonView.setRotation(0);
                        }
                        if (ivResult.getRotation() == 90 || ivResult.getRotation() == -90 || ivResult.getRotation() == 270 || ivResult.getRotation() == -270) {

                        }
                        if (ivResult.getRotation() == 90 || ivResult.getRotation() == -90 || ivResult.getRotation() == 270 || ivResult.getRotation() == -270) {
                            float scaledRatio = Float.parseFloat(Integer.toString(ivResult.getWidth()))
                                    / Float.parseFloat(Integer.toString(ivResult.getHeight()));
                            ivResult.setScaleX(scaledRatio);
                            ivResult.setScaleY(scaledRatio);
                            polygonView.setScaleX(scaledRatio);
                            polygonView.setScaleY(scaledRatio);
                        } else {
                            ivResult.setScaleX(1.0f);
                            ivResult.setScaleY(1.0f);
                            polygonView.setScaleX(1.0f);
                            polygonView.setScaleY(1.0f);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        ivRotateLeft.setColorFilter(Color.argb(255, 255, 255, 255));
                        break;
                }
                return true;
            }
        });

        ivRotateRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int eid = event.getAction();
                switch (eid) {
                    case MotionEvent.ACTION_DOWN:
                        ivRotateRight.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.blue), PorterDuff.Mode.SRC_IN);
                        ivResult.setRotation(ivResult.getRotation() + 90);
                        polygonView.setRotation(polygonView.getRotation() + 90);
                        if (ivResult.getRotation() == 360 || polygonView.getRotation() == 360) {
                            ivResult.setRotation(0);
                            polygonView.setRotation(0);
                        }
                        if (ivResult.getRotation() == 90 || ivResult.getRotation() == -90 || ivResult.getRotation() == 270 || ivResult.getRotation() == -270) {
                            float scaledRatio = Float.parseFloat(Integer.toString(ivResult.getWidth()))
                                    / Float.parseFloat(Integer.toString(ivResult.getHeight()));
                            ivResult.setScaleX(scaledRatio);
                            ivResult.setScaleY(scaledRatio);
                            polygonView.setScaleX(scaledRatio);
                            polygonView.setScaleY(scaledRatio);
                        } else {
                            ivResult.setScaleX(1.0f);
                            ivResult.setScaleY(1.0f);
                            polygonView.setScaleX(1.0f);
                            polygonView.setScaleY(1.0f);
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        ivRotateRight.setColorFilter(Color.argb(255, 255, 255, 255));
                        break;
                }
                return true;
            }
        });

        ivCrop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFourPointed) {
                    Map<Integer, PointF> pointFs = getOutlinePoints(newBmp);
                    polygonView.setPoints(pointFs);
                } else {
                    if (isCropped) {
                        // Undo Crop Here
                        ivCrop.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.blue), PorterDuff.Mode.SRC_IN);
                        Map<Integer, PointF> pointFs = getOutlinePoints(newBmp);
                        polygonView.setPoints(pointFs);
                        polygonView.invalidate();
                        polygonView.setVisibility(View.VISIBLE);
                        isCropped = false;
                    } else {
                        // Auto Crop Here
                        ivCrop.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.color_white), PorterDuff.Mode.SRC_IN);
                        Map<Integer, PointF> pointFs = getEdgePoints(newBmp);
                        polygonView.setPoints(pointFs);
                        polygonView.invalidate();
                        polygonView.setVisibility(View.VISIBLE);
                        isCropped = true;
                    }
                }
            }
        });

        ivConfirm.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int eid = event.getAction();
                switch (eid) {
                    case (MotionEvent.ACTION_DOWN):
                        ivConfirm.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.blue), PorterDuff.Mode.SRC_IN);
                        Map<Integer, PointF> points = polygonView.getPoints();
                        Point[] pointArr = new Point[4];

                        for (int i = 0; i < points.size(); i++) {
//                            if (reqCode != 0) {
                                pointArr[i] = new Point((double) points.get(i).x / 1.295, (double) points.get(i).y / 1.27);

//                            }
                        }
                        if (polygonView.isValidShape(points)) {
//                            Log.d(TAG, "Crop Points: " + pointArr[0].toString() + " , " + pointArr[1].toString() + " , " + pointArr[2].toString() + " , " + pointArr[3].toString());
                            Mat dest = perspectiveChange(mat, pointArr);
                            Matrix matrix = new Matrix();
                            matrix.postRotate(ivResult.getRotation());
//                            Imgproc.resize(dest, dest, new Size(984, 1312));
                            Bitmap tfmBmp = Bitmap.createBitmap(dest.width(), dest.height(), Bitmap.Config.ARGB_8888);
                            Utils.matToBitmap(dest, tfmBmp);
                            Bitmap rotatedBmp = Bitmap.createBitmap(tfmBmp, 0, 0, tfmBmp.getWidth(), tfmBmp.getHeight(), matrix, true);
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            byte[] bytes = stream.toByteArray();
                            mFile2 = new File(getExternalFilesDir("Temp"), "temp2.jpg");
                            try {
                                mFile2.createNewFile();
                                FileOutputStream fileOutputStream = new FileOutputStream(mFile2);
                                fileOutputStream.write(bytes);
                                fileOutputStream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        } else {
                            showToast("Invalid Shape!");
                        }
                        break;
                    case (MotionEvent.ACTION_UP):
                        ivConfirm.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.color_white), PorterDuff.Mode.SRC_IN);
                        Intent intent = new Intent(AdjustmentActivity.this, ResultActivity.class);
                        intent.putExtra("croppedPoints", mFile2.getAbsolutePath());
                        if (ivResult.getRotation() == 90 || ivResult.getRotation() == -90 || ivResult.getRotation() == 270 || ivResult.getRotation() == -270){
                            intent.putExtra("isRotated", true);
                        }
                        startActivity(intent);
                        break;
                }
                return true;
            }
        });
    }

    private byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }

    private void performGammaCorrection(Mat src) {
        //! [changing-contrast-brightness-gamma-correction]
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total() * lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(Math.pow(i / 255.0, gammaValue) * 255.0);
        }
        lookUpTable.put(0, 0, lookUpTableData);
        Mat img = new Mat();
        Core.LUT(src, lookUpTable, img);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private Mat perspectiveChange(Mat src, Point[] points) {

//        double ratio = src.size().height / 500;
//        for (int i = 0; i < points.length; i++) {
//            points[i].x /= 1.29;
//            points[i].y /= 1.29;
//        }
        Point bl = points[0];
        Point br = points[1];
        Point tl = points[2];
        Point tr = points[3];
        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);
        int maxWidth = Double.valueOf(dw).intValue();

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);
        int maxHeight = Double.valueOf(dh).intValue();

        Mat destImage = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);
        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);


        srcMat.put(0, 0, bl.x, bl.y, br.x, br.y, tr.x, tr.y, tl.x, tl.y);
        dstMat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat transform = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Imgproc.warpPerspective(src, destImage, transform, destImage.size());
        return destImage;
    }

    private void alertDialog() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Are you sure you want to discard this image?");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Discard",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        File file = new File(imagePath);
                        boolean deleted = file.delete();
                        if (!deleted) {
                            showToast("Error Discarding Image!");
                        }
                        dialog.cancel();
                        finish();
                    }
                });

        builder1.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    @Override
    public void onBackPressed() {
        alertDialog();
    }


    private ArrayList<MatOfPoint> findContours(Mat src) {

        Size size = new Size(newBmp.getWidth(), newBmp.getHeight());
        Mat grayImage = new Mat(size, CvType.CV_8UC1);
        Mat cannedImage = new Mat(size, CvType.CV_8UC1);
//        Imgproc.resize(src,resizedImage,size);
//        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
//        Imgproc.dilate(grayImage, grayImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
//        Imgproc.erode(grayImage, grayImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        Imgproc.cvtColor(src, grayImage, Imgproc.COLOR_RGBA2GRAY, 1);
        Imgproc.morphologyEx(grayImage, grayImage, 3, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        Imgproc.morphologyEx(src, grayImage, 4, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        Imgproc.Canny(grayImage, cannedImage, 0, 260);
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(cannedImage, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);

        hierarchy.release();
        Collections.sort(contours, new Comparator<MatOfPoint>() {

            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                return Double.valueOf(Imgproc.contourArea(rhs)).compareTo(Imgproc.contourArea(lhs));
            }
        });

        double maxVal = -1;
        int maxValIdx = 0;

        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            try {
                if (maxVal < contourArea) {
                    maxVal = contourArea;
                    maxValIdx = contourIdx;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            MatOfPoint2f c2f = new MatOfPoint2f(contours.get(maxValIdx).toArray());
            double peri = Imgproc.arcLength(c2f, true) * 0.02;
            MatOfPoint2f approx = new MatOfPoint2f();
            if (peri > 1) {
                Imgproc.approxPolyDP(c2f, approx, peri, true);

                MatOfPoint matOfPoint = new MatOfPoint(approx.toArray());
                Point[] points = approx.toArray();
//                Imgproc.drawContours(mat, contours, maxValIdx, new Scalar(0, 255, 0), 2);
                // select biggest 4 angles polygon
                if (matOfPoint.total() >= 4 & Math.abs(Imgproc.contourArea(matOfPoint)) > 1000) {
                    Point[] foundPoints = sortPoints(points);
                    isFourPointed = true;
                    isCropped = true;
                    quad = new Quadrilateral(contours.get(maxValIdx), foundPoints);
//                    for (Point point : quad.points) {
//                        Imgproc.circle(mat, point, 10, new Scalar(255, 0, 255), 4);
//                    }
                } else {
                    quad = null;
                }
                Utils.matToBitmap(mat, resizedBmp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        grayImage.release();
        cannedImage.release();
        return contours;
    }

    private Point[] sortPoints(Point[] src) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = {null, null, null, null};

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    private void setBitmap(Bitmap original) {
        ivResult.setImageBitmap(original);
        Bitmap tempBitmap = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
        Map<Integer, PointF> pointFs = getEdgePoints(tempBitmap);
        polygonView.setPoints(pointFs);
        polygonView.setVisibility(View.VISIBLE);
        //int padding = (int) getResources().getDimension(R.dimen.scanPadding);
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointFs = getContourEdgePoints(tempBitmap);
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointList = new ArrayList<>();
        if (quad != null) {
            Point[] quadPoints = quad.points;
            for (int i = 0; i < quadPoints.length; i++) {
                float x = Float.parseFloat(Double.toString(quadPoints[i].x * 1.27));
                float y = Float.parseFloat(Double.toString(quadPoints[i].y * 1.27));
                pointList.add(new PointF(x, y));
            }
        }
        return pointList;

    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        if (ivResult.getWidth() == 0 && ivResult.getHeight() == 0){
            outlinePoints.put(0, new PointF(0, 0));
            outlinePoints.put(1, new PointF(tempBitmap.getWidth()* 1.293f, 0));
            outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()* 1.268f));
            outlinePoints.put(3, new PointF(tempBitmap.getWidth() * 1.293f, tempBitmap.getHeight()* 1.268f));
        } else {
            outlinePoints.put(0, new PointF(0, 0));
            outlinePoints.put(1, new PointF(ivResult.getWidth(), 0));
            outlinePoints.put(2, new PointF(0, ivResult.getHeight()));
            outlinePoints.put(3, new PointF(ivResult.getWidth(), ivResult.getHeight()));
        }
        return outlinePoints;

    }

    private Map<Integer, PointF> orderedValidEdgePoints(Bitmap tempBitmap, List<PointF> pointFs) {
        Map<Integer, PointF> orderedPoints = polygonView.getOrderedPoints(pointFs);
        if (!polygonView.isValidShape(orderedPoints)) {
            orderedPoints = getOutlinePoints(tempBitmap);
        }
        return orderedPoints;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    showToast("Permission not Granted!");
                }
            }
        }
    }
}
