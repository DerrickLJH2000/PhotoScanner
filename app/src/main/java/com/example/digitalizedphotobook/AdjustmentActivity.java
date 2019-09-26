package com.example.digitalizedphotobook;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.digitalizedphotobook.classes.Quadrilateral;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AdjustmentActivity extends AppCompatActivity {
    private static final String TAG = "AdjustmentActivity123";
    private static final int REQUEST_CODE = 99;
    private ImageView ivBack, ivCrop, ivConfirm, ivRotateLeft, ivRotateRight, ivResult;
    private PolygonView polygonView;
    private FrameLayout frmSource;
    private File mFile;
    private Bitmap bmp, newBmp;
    private Mat mat, drawing;
    private Quadrilateral quad;
    private boolean colorMode = false;
    private boolean filterMode = true;
    private double colorGain = 1.5;       // contrast
    private double colorBias = 0;         // bright
    private int colorThresh = 110;

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

        final String imagePath = getIntent().getStringExtra("image");
        final int reqCode = getIntent().getIntExtra("reqCode", -1);
        Log.i(TAG, (reqCode) + imagePath);
        mFile = new File(imagePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        bmp = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
        Matrix matrix = new Matrix();
        if (reqCode != 0) {
            matrix.postRotate(90);
        }
        newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);


        ivResult.setImageBitmap(newBmp);
        polygonView.paintZoom(ivResult.getDrawable());

        mat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(newBmp, mat);

        findContours(mat);

//        Mat doc = new Mat(mat.size(), CvType.CV_8UC4);

//        if (quad != null) {
        setBitmap(newBmp);
//        } else {
//            mat.copyTo(doc);
//            Log.i("Points", "failed");
//        }
//        enhanceDocument(doc);

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog();
            }
        });

        ivRotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivResult.setRotation(ivResult.getRotation() - 90);
                polygonView.setRotation(polygonView.getRotation() - 90);
            }
        });

        ivRotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivResult.setRotation(ivResult.getRotation() + 90);
                polygonView.setRotation(polygonView.getRotation() + 90);
            }
        });

        ivConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Map<Integer, PointF> points = polygonView.getPoints();
                Point[] pointArr = new Point[4];

                for (int i = 0; i < points.size(); i++) {
                    Log.i(TAG, "x = " + Float.toString(points.get(i).x) + ",y = " + Float.toString(points.get(i).y));
                    if (reqCode != 0) {
                        pointArr[i] = new Point((double) points.get(i).x / 2, (double) points.get(i).y / 2);
                    } else {
                        pointArr[i] = new Point((double) points.get(i).x / 4, (double) points.get(i).y / 4);
                    }
                }
                Log.d(TAG, "Quad Points: " + quad.points[0].toString() + " , " + quad.points[1].toString() + " , " + quad.points[2].toString() + " , " + quad.points[3].toString());

                Log.d(TAG, "Crop Points: " + pointArr[0].toString() + " , " + pointArr[1].toString() + " , " + pointArr[2].toString() + " , " + pointArr[3].toString());
//                Mat dest = fourPointTransform(mat,pointArr);
                Mat dest = perspectiveChange(mat, pointArr);
                Bitmap tfmBmp = Bitmap.createBitmap(dest.width(), dest.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(dest, tfmBmp);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                tfmBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] bytes = stream.toByteArray();
                File mFile = new File(getExternalFilesDir("Temp"), "temp2.jpg");
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
    }

    private Mat perspectiveChange(Mat src, Point[] points) {
        Point bl = points[0];
        Point br = points[1];
        Point tl = points[2];
        Point tr = points[3];

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

        Mat destImage = new Mat(maxWidth, maxHeight, src.type());
        Log.d(TAG, "destImage:" + (destImage.cols()) + ", " + (destImage.rows()));

        Mat src_mat = new MatOfPoint2f(new Point(points[0].x, points[0].y), new Point(points[1].x, points[1].y), new Point(points[3].x, points[3].y), new Point(points[2].x, points[2].y));
        Mat dst_mat = new MatOfPoint2f(new Point(0, 0), new Point(destImage.width(), 0), new Point(destImage.width(), destImage.height()), new Point(0, destImage.height()));
        Log.d(TAG, "src_mat:" + (src_mat.cols()) + ", " + (src_mat.rows()));
        Log.d(TAG, "dst_mat:" + (dst_mat.cols()) + ", " + (dst_mat.rows()));
        Mat transform = Imgproc.getPerspectiveTransform(src_mat, dst_mat);
        Log.d(TAG, "transform:" + (transform.cols()) + ", " + (transform.rows()));
        Imgproc.warpPerspective(src, destImage, transform, destImage.size());
        Log.d(TAG, "destImage:" + (destImage.cols()) + ", " + (destImage.rows()));
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
                        dialog.cancel();
                        finish();
                    }
                });

        builder1.setNegativeButton(
                "No",
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
        super.onBackPressed();
        alertDialog();
    }


    private ArrayList<MatOfPoint> findContours(Mat src) {

        //Size size = new Size(newBmp.getWidth(),newBmp.getHeight());
        Size size = new Size(newBmp.getWidth(), newBmp.getHeight());
        Mat grayImage = new Mat(size, CvType.CV_8UC1);
        Mat cannedImage = new Mat(size, CvType.CV_8UC1);

        //Imgproc.resize(src,resizedImage,size);
        Imgproc.cvtColor(src, grayImage, Imgproc.COLOR_RGBA2GRAY, 1);
        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
        Imgproc.dilate(grayImage, grayImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
        Imgproc.erode(grayImage, grayImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
        Imgproc.Canny(grayImage, cannedImage, 75, 200);

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

        MatOfPoint matOfPoint = new MatOfPoint();
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
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

            Point[] points = approx.toArray();
//            Imgproc.drawContours(mat, contours, maxValIdx, new Scalar(0, 255, 0), 1);
            // select biggest 4 angles polygon
            if (points.length == 4) {
                Point[] foundPoints = sortPoints(points);

                quad = new Quadrilateral(contours.get(maxValIdx), foundPoints);
//                for (Point point : quad.points) {
//                    Imgproc.circle(mat, point, 10, new Scalar(255, 0, 255), 4);
//                }
            } else {
                Toast.makeText(this, "Please Retake Photo!", Toast.LENGTH_SHORT).show();
                finish();
            }

            Utils.matToBitmap(mat, newBmp);

        } catch (Exception e) {
            e.printStackTrace();
        }


        grayImage.release();
        cannedImage.release();
        return contours;
    }

    private void colorThresh(Mat src, int threshold) {
        Size srcSize = src.size();
        int size = (int) (srcSize.height * srcSize.width) * 3;
        byte[] d = new byte[size];
        src.get(0, 0, d);

        for (int i = 0; i < size; i += 3) {

            // the "& 0xff" operations are needed to convert the signed byte to double

            // avoid unneeded work
            if ((double) (d[i] & 0xff) == 255) {
                continue;
            }

            double max = Math.max(Math.max((double) (d[i] & 0xff), (double) (d[i + 1] & 0xff)),
                    (double) (d[i + 2] & 0xff));
            double mean = ((double) (d[i] & 0xff) + (double) (d[i + 1] & 0xff)
                    + (double) (d[i + 2] & 0xff)) / 3;

            if (max > threshold && mean < max * 0.8) {
                d[i] = (byte) ((double) (d[i] & 0xff) * 255 / max);
                d[i + 1] = (byte) ((double) (d[i + 1] & 0xff) * 255 / max);
                d[i + 2] = (byte) ((double) (d[i + 2] & 0xff) * 255 / max);
            } else {
                d[i] = d[i + 1] = d[i + 2] = 0;
            }
        }
        src.put(0, 0, d);
    }

    private void enhanceDocument(Mat src) {
        if (colorMode && filterMode) {
            src.convertTo(src, -1, colorGain, colorBias);
            Mat mask = new Mat(src.size(), CvType.CV_8UC1);
            Imgproc.cvtColor(src, mask, Imgproc.COLOR_RGBA2GRAY);

            Mat copy = new Mat(src.size(), CvType.CV_8UC3);
            src.copyTo(copy);

            Imgproc.adaptiveThreshold(mask, mask, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, 15);

            src.setTo(new Scalar(255, 255, 255));
            copy.copyTo(src, mask);

            copy.release();
            mask.release();

            // special color threshold algorithm
            colorThresh(src, colorThresh);
        } else if (!colorMode) {
            Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
            if (filterMode) {
                Imgproc.adaptiveThreshold(src, src, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15);
            }
        }
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
        List<PointF> pointFs = getContourEdgePoints();
        Map<Integer, PointF> orderedPoints = orderedValidEdgePoints(tempBitmap, pointFs);
        return orderedPoints;
    }

    private List<PointF> getContourEdgePoints() {
        List<PointF> pointList = new ArrayList<>();
        if (quad != null) {
            Point[] quadPoints = quad.points;
            for (int i = 0; i < quadPoints.length; i++) {
                float x = Float.parseFloat(Double.toString(quadPoints[i].x));
                float y = Float.parseFloat(Double.toString(quadPoints[i].y));
                pointList.add(new PointF(x, y));
            }
        } else {
            pointList.add(new PointF(0, 0));
            pointList.add(new PointF(newBmp.getWidth(), 0));
            pointList.add(new PointF(newBmp.getWidth(), newBmp.getHeight()));
            pointList.add(new PointF(0, newBmp.getHeight()));
        }
        return pointList;

    }

    private Map<Integer, PointF> getOutlinePoints(Bitmap tempBitmap) {
        Map<Integer, PointF> outlinePoints = new HashMap<>();
        outlinePoints.put(0, new PointF(0, 0));
        outlinePoints.put(1, new PointF(tempBitmap.getWidth(), 0));
        outlinePoints.put(2, new PointF(0, tempBitmap.getHeight()));
        outlinePoints.put(3, new PointF(tempBitmap.getWidth(), tempBitmap.getHeight()));
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
                    Toast.makeText(AdjustmentActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
