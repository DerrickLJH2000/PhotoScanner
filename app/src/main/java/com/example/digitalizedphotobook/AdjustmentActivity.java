package com.example.digitalizedphotobook;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.digitalizedphotobook.classes.NativeClass;
import com.example.digitalizedphotobook.classes.Quadrilateral;
import com.example.digitalizedphotobook.classes.ScannerConstants;

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
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
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

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static android.graphics.Bitmap.createBitmap;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.minAreaRect;
import static org.opencv.imgproc.Imgproc.polylines;
import static org.opencv.imgproc.Imgproc.rectangle;


public class AdjustmentActivity extends AppCompatActivity {
    private static final String TAG = "AdjustmentActivity123";
    private ImageView ivBack, ivCrop, ivConfirm, ivRotate, ivToggleAuto;
    public static ImageView ivResult;
    private ProgressBar progressBar;
    private FrameLayout frmHolder;
    private PolygonView polygonView;
    private File mFile, mFile2;
    private String imagePath;
    private NativeClass nativeClass;
    private Bitmap bmp, newBmp, scaledBitmap;
    private ArrayList<Bitmap> bitmapArr;
    private Mat mat;
    private Quadrilateral quad;
    private boolean isFourPointed = false;
    private boolean isCropped = false;
    private int reqCode;
    private Map<Integer, PointF> points, tempPoints;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adjustment);

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
        mFile = new File(imagePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        bmp = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
        Matrix matrix = new Matrix();
        if (reqCode != 0) {
            matrix.postRotate(90);
        }
        bmp = createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        ScannerConstants.selectedImageBitmap = bmp;
        if (ScannerConstants.selectedImageBitmap != null) {
            initializeElement();
        } else {
            showToast("Retake Photo!");
            finish();
        }
    }

    private void setImageRotation() {
        Bitmap tempBitmap = bmp.copy(bmp.getConfig(), true);
        for (int i = 1; i <= 4; i++) {
            MatOfPoint2f point2f = nativeClass.getPoint(tempBitmap);
            if (point2f == null) {
                tempBitmap = rotateBitmap(tempBitmap, 90 * i);
            } else {
                newBmp = tempBitmap.copy(bmp.getConfig(), true);
                break;
            }
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void setProgressBar(boolean isShow) {
        RelativeLayout rlContainer = findViewById(R.id.rlContainer);
        setViewInterract(rlContainer, !isShow);
        if (isShow)
            progressBar.setVisibility(View.VISIBLE);
        else
            progressBar.setVisibility(View.GONE);
    }

    private void setViewInterract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInterract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    private void initializeElement() {
        nativeClass = new NativeClass();
        ivBack = findViewById(R.id.ivBack);
        ivRotate = findViewById(R.id.ivRotate);
        ivToggleAuto = findViewById(R.id.ivToggleAuto);
        ivCrop = findViewById(R.id.ivCrop);
        ivConfirm = findViewById(R.id.ivConfirm);
        ivResult = findViewById(R.id.ivResult);
        polygonView = findViewById(R.id.polygonView);
        frmHolder = findViewById(R.id.holderImageCrop);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar.getIndeterminateDrawable() != null)
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#ff59a9ff"), android.graphics.PorterDuff.Mode.MULTIPLY);
        else if (progressBar.getProgressDrawable() != null)
            progressBar.getProgressDrawable().setColorFilter(Color.parseColor("#ff59a9ff"), android.graphics.PorterDuff.Mode.MULTIPLY);
        setProgressBar(true);
        Observable.fromCallable(() -> {
            setImageRotation();
            return false;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    setProgressBar(false);
                    frmHolder.post(this::initializeCropping);
                    ivRotate.setOnClickListener(btnRotate);
                    ivToggleAuto.setOnClickListener(btnToggleAuto);
                    ivCrop.setOnClickListener(btnCropToFit);
                    ivConfirm.setOnClickListener(btnConfirmClick);
                    ivBack.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            alertDialog();
                        }
                    });
                });
    }

    private void initializeCropping() {
        newBmp = ScannerConstants.selectedImageBitmap;
        scaledBitmap = scaledBitmap(bmp, frmHolder.getWidth(), frmHolder.getHeight());
        ivResult.setImageBitmap(scaledBitmap);

        mat = new Mat(scaledBitmap.getWidth(), scaledBitmap.getHeight(), CvType.CV_8UC4);
        Bitmap tempBitmap = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
        Utils.bitmapToMat(tempBitmap, mat);
        findContours(mat);

        if (isCropped == true) {
            ivCrop.setImageResource(R.drawable.ic_magnet);
            ivCrop.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.blue), PorterDuff.Mode.SRC_IN);
        } else {
            ivCrop.setImageResource(R.drawable.ic_crop);
            ivCrop.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.color_white), PorterDuff.Mode.SRC_IN);
        }

        Map<Integer, PointF> pointFs = null;
        try {
            pointFs = getEdgePoints(tempBitmap);
            polygonView.setPoints(pointFs);
            polygonView.setVisibility(View.VISIBLE);

            int padding = (int) getResources().getDimension(R.dimen.scanPadding);

            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(tempBitmap.getWidth() + 2 * padding, tempBitmap.getHeight() + 2 * padding);
            layoutParams.gravity = Gravity.CENTER;

            polygonView.setLayoutParams(layoutParams);
            polygonView.setPointColor(getResources().getColor(R.color.blue));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private View.OnClickListener btnRotate = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setProgressBar(true);
            bmp = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
            Observable.fromCallable(() -> {
                bmp = rotateBitmap(bmp, 90);
                return false;
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((result) -> {
                        setProgressBar(false);
                        if (checkedItem == 0) {
                            initializeElement();
                        } else {
                            ivResult.setImageBitmap(bmp);
                        }
                    });
        }
    };

    private View.OnClickListener btnToggleAuto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            modeDialog();
        }
    };

    private View.OnClickListener btnCropToFit = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isFourPointed) {
                Map<Integer, PointF> pointFs = getOutlinePoints(newBmp);
                polygonView.setPoints(pointFs);
            } else {
                if (isCropped) {
                    // Undo Crop Here
                    ivCrop.setImageResource(R.drawable.ic_crop);
                    ivCrop.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.color_white), PorterDuff.Mode.SRC_IN);
                    Map<Integer, PointF> pointFs = getOutlinePoints(newBmp);
                    polygonView.setPoints(pointFs);
                    polygonView.invalidate();
                    polygonView.setVisibility(View.VISIBLE);
                    isCropped = false;
                } else {
                    // Auto Crop Here
                    ivCrop.setImageResource(R.drawable.ic_magnet);
                    ivCrop.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.blue), PorterDuff.Mode.SRC_IN);
                    Map<Integer, PointF> pointFs = getEdgePoints(newBmp);
                    polygonView.setPoints(pointFs);
                    polygonView.invalidate();
                    polygonView.setVisibility(View.VISIBLE);
                    isCropped = true;
                }
            }
        }
    };
    private View.OnClickListener btnConfirmClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (bitmapArr.size() == 0) {
                points = polygonView.getPoints();
                Point[] pointArr = new Point[4];
                for (int i = 0; i < points.size(); i++) {
                    pointArr[i] = new Point((double) points.get(i).x, (double) points.get(i).y);
                }
                if (polygonView.isValidShape(points)) {
                    Mat dest = perspectiveChange(mat, pointArr);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(ivResult.getRotation());
                    Bitmap tfmBmp = createBitmap(dest.width(), dest.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(dest, tfmBmp);
                    Bitmap rotatedBmp = createBitmap(tfmBmp, 0, 0, tfmBmp.getWidth(), tfmBmp.getHeight(), matrix, true);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] bytes = stream.toByteArray();
                    mFile2 = new File(getExternalFilesDir("Temp"), "test1.jpg");
                    try {
                        mFile2.createNewFile();
                        FileOutputStream fileOutputStream = new FileOutputStream(mFile2);
                        fileOutputStream.write(bytes);
                        fileOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//            ivConfirm.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.color_white), PorterDuff.Mode.SRC_IN);
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

                } else {
                    showToast("Invalid Shape!");
                }
            } else {
                ArrayList<String>
                for (int i = 0; i < bitmapArr.size(); i++) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmapArr.get(i).compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] bytes = stream.toByteArray();
                    mFile2 = new File(getExternalFilesDir("Temp"), "test" + i + ".jpg");
                    try {
                        mFile2.createNewFile();
                        FileOutputStream fileOutputStream = new FileOutputStream(mFile2);
                        fileOutputStream.write(bytes);
                        fileOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Intent intent = new Intent(AdjustmentActivity.this, ResultActivity.class);
                intent.putParcelableArrayListExtra("croppedImages", bitmapArr);
                startActivity(intent);
            }
        }
    };

    private Bitmap scaledBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    private Mat otsuAutoCanny(Mat src) {
        Mat newSrc = new Mat();
        double otsu_thresh_val = Imgproc.threshold(src, new Mat(), 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        double high_thresh_val = otsu_thresh_val,
                lower_thresh_val = otsu_thresh_val * 0.5;
        Imgproc.Canny(src, newSrc, lower_thresh_val, high_thresh_val);
        return newSrc;
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

    private int checkedItem = 0;

    private void modeDialog() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setTitle("Select Crop Mode");
        builder1.setSingleChoiceItems(R.array.mode, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                imagePath = new File(getExternalFilesDir("Temp"), "temp.jpg").getAbsolutePath();
                mFile = new File(imagePath);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                Bitmap tempBitmap = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
                Matrix matrix = new Matrix();
                if (reqCode != 0) {
                    matrix.postRotate(90);
                }
                Bitmap tempBmp = createBitmap(tempBitmap, 0, 0, tempBitmap.getWidth(), tempBitmap.getHeight(), matrix, true);
                checkedItem = which;
                if (which == 1) {
                    Bitmap bmp1 = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
                    Mat tempMat = new Mat();
                    Utils.bitmapToMat(bmp1, tempMat);
                    findMultipleContours(tempMat);
                    if (quadArr == null) {
                        showToast("No Image Detected!");
                    }
                    polygonView.setVisibility(View.GONE);
                    ivCrop.setEnabled(false);
                    ivCrop.setColorFilter(ContextCompat.getColor(AdjustmentActivity.this, R.color.color_dark_grey2), PorterDuff.Mode.SRC_IN);

                } else {
                    ivResult.setImageBitmap(tempBmp);
                    polygonView.setVisibility(View.VISIBLE);
                    ivCrop.setEnabled(true);
                }
                dialog.dismiss();// dismiss the alertbox after chose option
            }
        });
        AlertDialog alert = builder1.create();
        alert.show();

    }

    @Override
    public void onBackPressed() {
        alertDialog();
    }

    private ArrayList<Quadrilateral> quadArr = new ArrayList<>();

    private void findMultipleContours(Mat src) {
        bitmapArr = new ArrayList<>();
        Size size = new Size(src.width(), src.height());
        Mat grayImage = new Mat(size, CvType.CV_8UC1);

        Imgproc.cvtColor(src, grayImage, Imgproc.COLOR_RGBA2GRAY, 1);

        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
        Mat cannedImage = otsuAutoCanny(grayImage);
        Imgproc.morphologyEx(cannedImage, cannedImage, 3, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7)));
        Imgproc.morphologyEx(cannedImage, cannedImage, 4, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();

        Imgproc.findContours(cannedImage, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        hierarchy.release();
        Collections.sort(contours, new Comparator<MatOfPoint>() {

            @Override
            public int compare(MatOfPoint lhs, MatOfPoint rhs) {
                return Double.valueOf(Imgproc.contourArea(rhs)).compareTo(Imgproc.contourArea(lhs));
            }
        });
        try {
            Mat dest = Mat.zeros(mat.size(), CvType.CV_8UC4);
            for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
                MatOfPoint2f c2f = new MatOfPoint2f(contours.get(contourIdx).toArray());
                double peri = Imgproc.arcLength(c2f, true) * 0.02;
                MatOfPoint2f approx = new MatOfPoint2f();
                if (peri > 1) {
                    Imgproc.approxPolyDP(c2f, approx, peri, true);
                    MatOfPoint matOfPoint = new MatOfPoint(approx.toArray());
                    Point[] points = approx.toArray();
                    // select biggest 4 angles polygon
                    if (matOfPoint.total() == 4 & Math.abs(Imgproc.contourArea(matOfPoint)) > 1000) {
//                        Rect rect = Imgproc.boundingRect(matOfPoint);
//                        Imgproc.rectangle(src, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0,255,0),5);
//                        Imgproc.drawContours(src, contours, contourIdx, new Scalar(0, 255, 0), 5);
                        Point[] foundPoints = sortPoints(points);
                        isFourPointed = true;
                        quad = new Quadrilateral(contours.get(contourIdx), foundPoints);
                        for (int i = 0; i < foundPoints.length; i++) {
                            Imgproc.line(src, points[i], points[(i + 1) % 4], new Scalar(0, 255, 0), 5);
                        }

                        ArrayList<PointF> pointArr = new ArrayList<>();
                        for (int i = 0; i < points.length; i++) {
                            pointArr.add(new PointF((float) points[i].x, (float) points[i].y));
                        }

                        tempPoints = getOrderedPoints(pointArr);
                        Point[] sortedPoints = new Point[4];
                        for (int i = 0; i < tempPoints.size(); i++) {
                            sortedPoints[i] = new Point((double) tempPoints.get(i).x, (double) tempPoints.get(i).y);
                        }
                        Mat tempDest = perspectiveChange(src, sortedPoints);
                        Bitmap tfmBmp = createBitmap(tempDest.width(), tempDest.height(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(tempDest, tfmBmp);
                        bitmapArr.add(tfmBmp);
                    }
                }
                quadArr.add(quad);
            }
            Utils.matToBitmap(src, scaledBitmap);
            ivResult.setImageBitmap(scaledBitmap);
            Log.d(TAG, "Size: " + bitmapArr.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
        grayImage.release();
        cannedImage.release();
    }

    ArrayList<MatOfPoint> contours;

    private Quadrilateral findContours(Mat src) {

        Size size = new Size(src.width(), src.height());
        Mat grayImage = new Mat(size, CvType.CV_8UC1);

        Imgproc.cvtColor(src, grayImage, Imgproc.COLOR_RGBA2GRAY, 1);

        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
        Mat cannedImage = otsuAutoCanny(grayImage);
        Imgproc.morphologyEx(cannedImage, cannedImage, 3, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7)));
        Imgproc.morphologyEx(cannedImage, cannedImage, 4, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        contours = new ArrayList<MatOfPoint>();
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
//                Imgproc.drawContours(mat, contours, maxValIdx, new Scalar(255, 255, 255), 2);
                // select biggest 4 angles polygon
                if (matOfPoint.total() == 4 & Math.abs(Imgproc.contourArea(matOfPoint)) > 1000) {
                    Point[] foundPoints = sortPoints(points);
                    isFourPointed = true;
                    isCropped = true;
                    quad = new Quadrilateral(contours.get(maxValIdx), foundPoints);
                } else {
                    quad = null;
                    isCropped = false;
                }
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
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(scaledBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, scaledBitmap.getHeight()));
        outlinePoints.put(3, new PointF(scaledBitmap.getWidth(), scaledBitmap.getHeight()));
        /*        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF((float) frmHolder.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, (float) frmHolder.getHeight()));
        outlinePoints.put(3, new PointF((float) frmHolder.getWidth(), (float) frmHolder.getHeight()));*/
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

    public Map<Integer, PointF> getOrderedPoints(List<PointF> points) {

        PointF centerPoint = new PointF();
        int size = points.size();
        for (PointF pointF : points) {
            centerPoint.x += pointF.x / size;
            centerPoint.y += pointF.y / size;
        }
        Map<Integer, PointF> orderedPoints = new HashMap<>();
        for (PointF pointF : points) {
            int index = -1;
            if (pointF.x < centerPoint.x && pointF.y < centerPoint.y) {
                index = 0;
            } else if (pointF.x > centerPoint.x && pointF.y < centerPoint.y) {
                index = 1;
            } else if (pointF.x < centerPoint.x && pointF.y > centerPoint.y) {
                index = 2;
            } else if (pointF.x > centerPoint.x && pointF.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, pointF);
        }
        return orderedPoints;
    }
}