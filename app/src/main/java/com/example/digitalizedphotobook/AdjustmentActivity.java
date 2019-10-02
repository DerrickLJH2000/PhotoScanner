package com.example.digitalizedphotobook;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
    private File mFile;
    private String imagePath;
    private Bitmap bmp, newBmp, resizedBmp;
    private Mat mat, drawing;
    private Quadrilateral quad;
    private boolean colorMode = false;
    private boolean filterMode = true;
    private double colorGain = 1.5;       // contrast
    private double colorBias = 0;         // bright
    private int colorThresh = 110;
    private boolean isFourPointed = false;
    private boolean isCropped = false;
    private int reqCode;

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
        if (reqCode != 0) {
            matrix.postRotate(90);
        }
        newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);


        Log.i(TAG, "Height: " + newBmp.getHeight() + "Width: " + newBmp.getWidth());
//        polygonView.paintZoom(ivResult.getDrawable());

        mat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(newBmp, mat);

        findContours(mat);
        ivResult.setImageBitmap(newBmp);
        Mat doc = new Mat(mat.size(), CvType.CV_8UC4);

//       if (quad != null) {
        setBitmap(newBmp);
//       } else {
//           mat.copyTo(doc);
//           Log.i("Points", "failed");
//       }
//        enhanceDocument(doc);

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
//                        for (int i = 0; i < quad.points.length; i++) {
//                            float x = Float.parseFloat(Double.toString(quad.points[i].x));
//                            float y = Float.parseFloat(Double.toString(quad.points[i].y));
//                            PointF pointF = new PointF(x, y);
//                            points.put(i, pointF);
//                        }
                        Map<Integer, PointF> pointFs = getEdgePoints(newBmp);
                        polygonView.setPoints(pointFs);
                        polygonView.invalidate();
                        polygonView.setVisibility(View.VISIBLE);
                        isCropped = true;
                    }
                }
            }
        });

        ivConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivConfirm.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.blue), PorterDuff.Mode.SRC_IN);
                Map<Integer, PointF> points = polygonView.getPoints();
                Point[] pointArr = new Point[4];

                for (int i = 0; i < points.size(); i++) {
                    Log.i(TAG, "x = " + Float.toString(points.get(i).x) + ",y = " + Float.toString(points.get(i).y));
                    if (reqCode != 0) {
                        pointArr[i] = new Point((double) points.get(i).x, (double) points.get(i).y);
//                        pointArr[i] = new Point((double) points.get(i).x, (double) points.get(i).y);
                    } else {
                        pointArr[i] = new Point((double) points.get(i).x, (double) points.get(i).y);
//                        pointArr[i] = new Point((double) points.get(i).x, (double) points.get(i).y);
                    }
                }

                Log.d(TAG, "Crop Points: " + pointArr[0].toString() + " , " + pointArr[1].toString() + " , " + pointArr[2].toString() + " , " + pointArr[3].toString());
                Mat dest = perspectiveChange(mat, pointArr);
                Matrix matrix = new Matrix();
                Log.i(TAG, "getRotation: " + ivResult.getRotation());
                matrix.postRotate(ivResult.getRotation());
                Bitmap tfmBmp = Bitmap.createBitmap(dest.width(), dest.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(dest, tfmBmp);
                Bitmap rotatedBmp = Bitmap.createBitmap(tfmBmp, 0, 0, tfmBmp.getWidth(), tfmBmp.getHeight(), matrix, true);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                rotatedBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] bytes = stream.toByteArray();
                File mFile = new File(getExternalFilesDir("Temp"), "temp2.png");
                try {
                    mFile.createNewFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(mFile);
                    fileOutputStream.write(bytes);
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(AdjustmentActivity.this, ResultActivity.class);
                intent.putExtra("croppedPoints", mFile.getAbsolutePath());
                startActivity(intent);
            }
        });

        BitmapShader mShader = new BitmapShader(newBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        Paint mPaint = new Paint();
        mPaint.setShader(mShader);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        ivConfirm.setColorFilter(Color.argb(255, 255, 255, 255));
    }

    private Mat perspectiveChange(Mat src, Point[] points) {
//        double ratio = src.size().height / 500;
        for (int i = 0; i < points.length; i++) {
            points[i].x += 15;
            points[i].y += 15;
        }
        Point bl = points[0];
        Point br = points[1];
        Point tl = points[2];
        Point tr = points[3];
        Log.i(TAG, "Src size:" + src.size());
        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        Log.d(TAG, "widthA:" + (widthA) + ", width B:" + Double.toString(widthB));

        double dw = Math.max(widthA, widthB);
        int maxWidth = Double.valueOf(dw).intValue();
        Log.d(TAG, "maxWidth:" + (maxWidth));

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        Log.d(TAG, "heightA:" + (heightA) + ", height B:" + Double.toString(heightB));

        double dh = Math.max(heightA, heightB);
        int maxHeight = Double.valueOf(dh).intValue();
        Log.d(TAG, "maxHeight:" + (maxHeight));

        Mat destImage = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);
        Log.d(TAG, "destImage:" + (destImage.cols()) + ", " + (destImage.rows()));
        Mat srcMat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dstMat = new Mat(4, 1, CvType.CV_32FC2);


        srcMat.put(0, 0, bl.x, bl.y, br.x, br.y, tr.x, tr.y, tl.x, tl.y);
        dstMat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);
//        Mat src_mat = new Mat();
//        Mat dst_mat = new Mat();
//        srcMat.convertTo(src_mat, CvType.CV_8UC4);
//        dstMat.convertTo(dst_mat, CvType.CV_8UC4);
//        Mat src_mat = new MatOfPoint2f(new Point(points[0].x, points[0].y), new Point(points[1].x, points[1].y), new Point(points[3].x, points[3].y), new Point(points[2].x, points[2].y));
//        Mat dst_mat = new MatOfPoint2f(new Point(0, 0), new Point(destImage.width(), 0), new Point(destImage.width(), destImage.height()), new Point(0, destImage.height()));

        Mat transform = Imgproc.getPerspectiveTransform(srcMat, dstMat);
        Log.d(TAG, "transform:" + (transform.cols()) + ", " + (transform.rows()));
        Imgproc.warpPerspective(src, destImage, transform, destImage.size());
        Log.d(TAG, "destImage:" + (destImage.size()));
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
        Imgproc.morphologyEx(grayImage,grayImage,3 ,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
        Imgproc.morphologyEx(src,grayImage,4,Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
        Imgproc.Canny(grayImage, cannedImage, 0, 240);
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(cannedImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

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
        drawing = Mat.zeros(mat.size(), CvType.CV_8UC1);
        try {
            MatOfPoint2f c2f = new MatOfPoint2f(contours.get(maxValIdx).toArray());
            double peri = Imgproc.arcLength(c2f, true) * 0.02;
            MatOfPoint2f approx = new MatOfPoint2f();
            if (peri > 1) {
                Imgproc.approxPolyDP(c2f, approx, peri, true);

                MatOfPoint matOfPoint = new MatOfPoint(approx.toArray());
                Point[] points = approx.toArray();
                Imgproc.drawContours(grayImage, contours, -1, new Scalar(255, 255, 255), 2);
                // select biggest 4 angles polygon
                if (matOfPoint.total() >= 4 & Math.abs(Imgproc.contourArea(matOfPoint)) > 1000) {
                    Point[] foundPoints = sortPoints(points);
                    isFourPointed = true;
                    isCropped = true;
                    quad = new Quadrilateral(contours.get(maxValIdx), foundPoints);
                    for (Point point : quad.points) {
//                        Imgproc.floodFill(grayImage, grayImage, point, new Scalar(0, 255, 0));
                        Imgproc.circle(grayImage, point, 10, new Scalar(255, 0, 255), 4);
                    }
                    Log.d(TAG, "Quad Points: " + quad.points[0].toString() + " , " + quad.points[1].toString() + " , " + quad.points[2].toString() + " , " + quad.points[3].toString());
                } else {
                    quad = null;
                }
                Utils.matToBitmap(grayImage, newBmp);
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
        Log.i(TAG, "setBitmap Crop Points: " + pointFs.toString());
        polygonView.setVisibility(View.VISIBLE);
        //int padding = (int) getResources().getDimension(R.dimen.scanPadding);
    }

    private Map<Integer, PointF> getEdgePoints(Bitmap tempBitmap) {
        List<PointF> pointFs = getContourEdgePoints();
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints() {
        List<PointF> pointList = new ArrayList<>();
        if (quad != null ) {
            Point[] quadPoints = quad.points;
            for (int i = 0; i < quadPoints.length; i++) {
                float x = Float.parseFloat(Double.toString(quadPoints[i].x - 15.0));
                float y = Float.parseFloat(Double.toString(quadPoints[i].y - 15.0));
//                Log.i(TAG, "original x: " + x + ", y = " + y + ", reqCode =" + reqCode);
//                if (quadPoints[i] == quadPoints[1]) {
//                    x -= 25.0;
//                } else if (quadPoints[i] == quadPoints[3]) {
//                    x -= 25.0;
//                    y -= 23.0;
//                } else if (quadPoints[i] == quadPoints[2]) {
//                    y -= 23.0;
//                }
//                if (reqCode == 0) {
//                    y = y * 4;
//                }
//                Log.i(TAG, "x: " + x + ", y = " + y + ", reqCode =" + reqCode);
                pointList.add(new PointF(x, y));
            }
        }
        return pointList;

    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth() * 0.9746f, 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight() * 0.98f));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth() * 0.9746f, tempBitmap.getHeight() * 0.98f));
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
