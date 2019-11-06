package com.example.digitalizedphotobook;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.appcompat.content.res.AppCompatResources;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.lang.Math.log;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;


public class AdjustmentActivity extends AppCompatActivity {
    private static final String TAG = "AdjustmentActivity123";
    private ImageView ivBack, ivCrop, ivConfirm, ivRotateLeft, ivRotateRight, ivResult;
    private PolygonView polygonView;
    private File mFile, mFile2;
    private String imagePath;
    private double gammaValue = 1.0;
    private Bitmap bmp, newBmp, resizedBmp, bmpImg;
    private Mat mat;
    private Quadrilateral quad;
    private boolean isFourPointed = false;
    private boolean isCropped = false;
    private int reqCode;
    private Map<Integer, PointF> points;
    private boolean isEditing = false;

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
        polygonView = findViewById(R.id.polygonView);
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

        imagePath = new File(getExternalFilesDir("Temp"), "temp.jpg").getAbsolutePath();
        reqCode = getIntent().getIntExtra("reqCode", -1);
        Log.i(TAG, (reqCode) + imagePath);
        int orientation = getCameraPhotoOrientation(imagePath);
        mFile = new File(imagePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        // Calculate inSampleSize
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int reqHeight = displayMetrics.heightPixels;
        int reqWidth = displayMetrics.widthPixels;
        int sampleSize = 1;
        try {
            sampleSize = calculateInSampleSize(options,reqWidth,reqHeight);
        } catch (Exception e){
            sampleSize = 1;
        }
        options.inSampleSize = sampleSize;
        Log.i(TAG, "BITMAP SAMPLE SIZE: " + sampleSize);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        bmp = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);

        Matrix matrix = new Matrix();
        if (reqCode != 0) {
            ivResult.setScaleType(ImageView.ScaleType.FIT_XY);
        }
            matrix.postRotate(90);
//        matrix.postRotate(0);

        if (bmp == null) {
            showToast("Retake Photo");
            finish();
        }
        newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        Mat tempDisplayMat = new Mat();
        Utils.bitmapToMat(newBmp, tempDisplayMat);
        Mat tempClone = tempDisplayMat.clone();
        gammaValue = autoGammaValue(tempClone);

        doGammaCorrection(tempDisplayMat);
        SimplestColorBalance(tempDisplayMat, 5);
        Utils.matToBitmap(tempDisplayMat, newBmp);
        Log.i(TAG, "Height: " + newBmp.getHeight() + "Width: " + newBmp.getWidth());

        ivResult.setImageBitmap(newBmp);

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
                        float scaledRatio = Float.parseFloat(Integer.toString(ivResult.getWidth()))
                                / Float.parseFloat(Integer.toString(ivResult.getHeight()));
                        if (ivResult.getRotation() == -360 || polygonView.getRotation() == -360) {
                            ivResult.setRotation(0);
                            polygonView.setRotation(0);
                        }
                        if (ivResult.getRotation() == 90 || ivResult.getRotation() == -90 || ivResult.getRotation() == 270 || ivResult.getRotation() == -270) {

                        }
                        if (ivResult.getRotation() == 90 || ivResult.getRotation() == -90 || ivResult.getRotation() == 270 || ivResult.getRotation() == -270) {
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
                        float scaledRatio = Float.parseFloat(Integer.toString(ivResult.getWidth()))
                                / Float.parseFloat(Integer.toString(ivResult.getHeight()));
                        if (ivResult.getRotation() == 360 || polygonView.getRotation() == 360) {
                            ivResult.setRotation(0);
                            polygonView.setRotation(0);
                        }
                        if (ivResult.getRotation() == 90 || ivResult.getRotation() == -90 || ivResult.getRotation() == 270 || ivResult.getRotation() == -270) {
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
                        points = polygonView.getPoints();
                        Point[] pointArr = new Point[4];
                        for (int i = 0; i < points.size(); i++) {
                            pointArr[i] = new Point((double) points.get(i).x, (double) points.get(i).y);

                        }
                        if (polygonView.isValidShape(points)) {
                            Mat dest = perspectiveChange(mat, pointArr);
                            Matrix matrix = new Matrix();
                            matrix.postRotate(ivResult.getRotation());
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
                        isEditing = true;
                        Intent intent = new Intent(AdjustmentActivity.this, ResultActivity.class);
                        intent.putExtra("croppedPoints", mFile2.getAbsolutePath());
                        float scaledRatio = Float.parseFloat(Integer.toString(ivResult.getWidth()))
                                / Float.parseFloat(Integer.toString(ivResult.getHeight()));
                        intent.putExtra("scaledRatio", scaledRatio);
                        if (ivResult.getRotation() == 90 || ivResult.getRotation() == -90 || ivResult.getRotation() == 270 || ivResult.getRotation() == -270) {
                            intent.putExtra("isRotated", true);
                        }
                        startActivity(intent);
                        break;
                }
                return true;
            }
        });
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Mat otsuAutoCanny(Mat src) {
        Mat newSrc = new Mat();
        double otsu_thresh_val = Imgproc.threshold(src, new Mat(), 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        double high_thresh_val = otsu_thresh_val,
                lower_thresh_val = otsu_thresh_val * 0.5;
        Imgproc.Canny(src, newSrc, lower_thresh_val, high_thresh_val);
        return newSrc;
    }

    public int getCameraPhotoOrientation(String imagePath) {
        int rotate = 0;
        try {
            File imageFile = new File(imagePath);

            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }

            Log.i("RotateImage", "Exif orientation: " + orientation);
            Log.i("RotateImage", "Rotate value: " + rotate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
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
        if (isEditing) {
            if (points != null) {
                polygonView.setPoints(points);
            } else {
                Bitmap returnBmp = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
                polygonView.setPoints(getOutlinePoints(returnBmp));
            }
        }
    }

    private Mat perspectiveChange(Mat src, Point[] points) {

        Point bl = points[0];
        Point br = points[1];
        Point tl = points[2];
        Point tr = points[3];
        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = max(widthA, widthB);
        int maxWidth = Double.valueOf(dw).intValue();

        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = max(heightA, heightB);
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


    private Quadrilateral findContours(Mat src) {

        Size size = new Size(src.width(), src.height());
        Mat grayImage = new Mat(size, CvType.CV_8UC1);

        Imgproc.cvtColor(src, grayImage, Imgproc.COLOR_RGBA2GRAY, 1);

        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
        Mat cannedImage = otsuAutoCanny(grayImage);
        Imgproc.morphologyEx(cannedImage, cannedImage, 3, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7)));
        Imgproc.morphologyEx(cannedImage, cannedImage, 4, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(cannedImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

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
//                Imgproc.drawContours(mat, contours, -1, new Scalar(0, 255, 0), 10);
                // select biggest 4 angles polygon
                if (matOfPoint.total() >= 4 & Math.abs(Imgproc.contourArea(matOfPoint)) > 1000) {
                    Point[] foundPoints = sortPoints(points);
                    isFourPointed = true;
                    isCropped = true;

                    quad = new Quadrilateral(contours.get(maxValIdx), foundPoints);
//                    for (Point point : quad.points) {
//                        Imgproc.circle(mat, point, 40, new Scalar(255, 0, 255), 20);
//                    }
                } else {
                    quad = null;
                }
                Utils.matToBitmap(mat, resizedBmp);
                Log.i(TAG, "Width " + resizedBmp.getWidth() + ", Height" + resizedBmp.getHeight());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        grayImage.release();
        cannedImage.release();
        return quad;
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
        Map<Integer, PointF> pointFs = getEdgePoints(original);
        polygonView.setPoints(pointFs);
        polygonView.setVisibility(View.VISIBLE);
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
                // Unable to obtain width and height of imageview so use ratio to hardcode points
                float x = Float.parseFloat(Double.toString(quadPoints[i].x));
                float y = Float.parseFloat(Double.toString(quadPoints[i].y));
                pointList.add(new PointF(x, y));
            }
        }
        return pointList;

    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
//        Map<Integer, PointF> outlinePoints = new HashMap<>();
//        int h = ivResult.getHeight();
//        int w = ivResult.getWidth();
//        outlinePoints.put(0, new PointF(0, 0));
//        outlinePoints.put(1, new PointF(w, 0));
//        outlinePoints.put(2, new PointF(0, h));
//        outlinePoints.put(3, new PointF(w, h));
//        Log.i(TAG, w + ", " + h);
        Map<Integer, PointF> outlinePoints = new HashMap<>();

        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(resizedBmp.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, resizedBmp.getHeight()));
        outlinePoints.put(3, new PointF(resizedBmp.getWidth(), resizedBmp.getHeight()));
        Log.i(TAG, resizedBmp.getWidth() + ", " + resizedBmp.getHeight());
        return outlinePoints;

    }

    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (!isEditing) {
                // Ratio 3.125
//                bmpImg = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
//                mat = new Mat(ivResult.getWidth(), ivResult.getHeight(), CvType.CV_8UC4);
//                double ratio = bmpImg.getHeight() / ivResult.getHeight();
////                double ratioWidth = bmpImg.getWidth() / ivResult.getWidth();
//                double height = bmpImg.getHeight() / ratio;
////                double width = bmpImg.getWidth() / ratioWidth;
//                resizedBmp = Bitmap.createBitmap(ivResult.getWidth(), (int) height, Bitmap.Config.ARGB_8888);
//                Utils.bitmapToMat(bmpImg, mat);
//                Imgproc.resize(mat, mat, new Size(resizedBmp.getWidth(), height));
//                findContours(mat);
//
//                performGammaCorrection(mat);
//
//                Utils.matToBitmap(mat, resizedBmp);
//                    setBitmap(resizedBmp);
                bmpImg = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
                mat = new Mat(ivResult.getWidth(), ivResult.getHeight(), CvType.CV_8UC4);
                double ratio = bmpImg.getWidth() / ivResult.getWidth();
                double height = bmpImg.getHeight() / ratio;
                resizedBmp = Bitmap.createBitmap(ivResult.getWidth(), ivResult.getHeight(), Bitmap.Config.ARGB_8888);

                Utils.bitmapToMat(bmpImg, mat);
                Imgproc.resize(mat, mat, new Size(resizedBmp.getWidth(), resizedBmp.getHeight()));
                findContours(mat);
                Utils.matToBitmap(mat, resizedBmp);
                setBitmap(resizedBmp);
            }
        }

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

    private byte saturate(double val) {
        int iVal = (int) Math.round(val);
        iVal = iVal > 255 ? 255 : (iVal < 0 ? 0 : iVal);
        return (byte) iVal;
    }

    private double mean_pixel(Mat img) {
        if (img.channels() > 2) {
            cvtColor(img.clone(), img, COLOR_RGB2GRAY);
            return Core.mean(img).val[0];
        } else {
            return Core.mean(img).val[0];
        }
    }

    private double autoGammaValue(Mat src) {
        double max_pixel = 255;
        double middle_pixel = 128;
        double pixel_range = 256;
        double mean_l = mean_pixel(src);

        double gamma = log(middle_pixel / pixel_range) / log(mean_l / pixel_range); // Formula from ImageJ
        return gamma;
    }

    private void doGammaCorrection(Mat src) {
        //! [changing-contrast-brightness-gamma-correction]
        Mat lookUpTable = new Mat(1, 256, CvType.CV_8U);
        byte[] lookUpTableData = new byte[(int) (lookUpTable.total() * lookUpTable.channels())];
        for (int i = 0; i < lookUpTable.cols(); i++) {
            lookUpTableData[i] = saturate(Math.pow(i / 255.0, gammaValue) * 255.0);
        }
        lookUpTable.put(0, 0, lookUpTableData);
        Core.LUT(src, lookUpTable, src);
    }

    /**
     * Simplest Color Balance. Performs color balancing via histogram
     * normalization.
     *
     * @param img input color or gray scale image
     * @param percent controls the percentage of pixels to clip to white and black. (normally, choose 1~10)
     * @return Balanced image in CvType.CV_32F
     */
    public static Mat SimplestColorBalance(Mat img, int percent) {
        if (percent <= 0)
            percent = 5;
        Imgproc.cvtColor(img , img , Imgproc.COLOR_RGBA2BGR,0);
//        img.convertTo(img, CvType.CV_32F);
        List<Mat> channels = new ArrayList<>();
        int rows = img.rows(); // number of rows of image
        int cols = img.cols(); // number of columns of image
        int chnls = img.channels(); //  number of channels of image
        double halfPercent = percent / 200.0;
        if (chnls == 3) Core.split(img, channels);
        else channels.add(img);
        List<Mat> results = new ArrayList<>();
        for (int i = 0; i < chnls; i++) {
            // find the low and high precentile values (based on the input percentile)
            Mat flat = new Mat();
            channels.get(i).reshape(1, 1).copyTo(flat);
            Core.sort(flat, flat, Core.SORT_ASCENDING);
            double lowVal = flat.get(0, (int) Math.floor(flat.cols() * halfPercent))[0];
            double topVal = flat.get(0, (int) Math.ceil(flat.cols() * (1.0 - halfPercent)))[0];
            // saturate below the low percentile and above the high percentile
            Mat channel = channels.get(i);
            for (int m = 0; m < rows; m++) {
                for (int n = 0; n < cols; n++) {
                    if (channel.get(m, n)[0] < lowVal) channel.put(m, n, lowVal);
                    if (channel.get(m, n)[0] > topVal) channel.put(m, n, topVal);
                }
            }
            Core.normalize(channel, channel, 0.0, 255.0 / 2, Core.NORM_MINMAX);
//            channel.convertTo(channel, CvType.CV_32F);
            results.add(channel);

        }
        Mat outval = new Mat();
        Core.merge(results, outval);
        Imgproc.cvtColor(outval , outval , Imgproc.COLOR_GRAY2RGBA);
        return outval;
    }
}
