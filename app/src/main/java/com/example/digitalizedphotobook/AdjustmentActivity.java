package com.example.digitalizedphotobook;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.digitalizedphotobook.helper.Quadrilateral;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static android.media.CamcorderProfile.get;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgcodecs.Imgcodecs.imread;

public class AdjustmentActivity extends AppCompatActivity {
    private static final String TAG = "AdjustmentActivity";
    ImageView ivBack, ivColour, ivConfirm, ivRotateLeft, ivRotateRight, ivResult;
    RelativeLayout rellay1;
    ProgressBar progressBar;
    private String selection;
    File mFile;
    Bitmap bmp, newBmp;
    Mat mat, blurmat;
    private Random rng = new Random(12345);
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
        ivColour = findViewById(R.id.ivColour);
        ivConfirm = findViewById(R.id.ivConfirm);
        ivResult = findViewById(R.id.ivResult);
        rellay1 = findViewById(R.id.rellay1);
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
        int reqCode = getIntent().getIntExtra("reqCode", -1);
        Log.i("Testing", (reqCode) + imagePath);
        mFile = new File(imagePath);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 8;
        bmp = BitmapFactory.decodeFile(mFile.getAbsolutePath(), options);
        Matrix matrix = new Matrix();
        if (reqCode != 0) {
            matrix.postRotate(90);
        }
        newBmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);

        Log.i(TAG, "Invalid request Code");

        ivResult.setImageBitmap(newBmp);

        mat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(newBmp, mat);

        final String[] matArr = this.getResources().getStringArray(R.array.mats);

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ivRotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivResult.setRotation(ivResult.getRotation() - 90);
            }
        });

        ivRotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivResult.setRotation(ivResult.getRotation() + 90);
            }
        });
        ivColour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AdjustmentActivity.this);
                builder.setTitle("Select Image Mode");
                builder.setSingleChoiceItems(R.array.mats, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selection = matArr[which];
                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (selection) {
                            case "RGBA":
                                Utils.matToBitmap(mat, newBmp);
                                ivResult.setImageBitmap(newBmp);
                                break;
                            case "Grey Scale":
                                Mat greyscale = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
                                Imgproc.cvtColor(mat, greyscale, Imgproc.COLOR_RGB2GRAY, 1);
                                Utils.matToBitmap(greyscale, newBmp);
                                ivResult.setImageBitmap(newBmp);
                                break;
                            case "Canny":
                                findContours(mat);
                                ivResult.setImageBitmap(newBmp);
                                break;
                            default:
                                return;
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

                AlertDialog alert = builder.create();
                alert.show();
            }
        });

        ivConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
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

    private Mat fourPointTransform( Mat src , Point[] pts ) {

        double ratio = src.size().height / 500;
        int height = Double.valueOf(src.size().height / ratio).intValue();
        int width = Double.valueOf(src.size().width / ratio).intValue();

        Point tl = pts[0];
        Point tr = pts[1];
        Point br = pts[2];
        Point bl = pts[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB)*ratio;
        int maxWidth = Double.valueOf(dw).intValue();


        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB)*ratio;
        int maxHeight = Double.valueOf(dh).intValue();

        Mat doc = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0, tl.x*ratio, tl.y*ratio, tr.x*ratio, tr.y*ratio, br.x*ratio, br.y*ratio, bl.x*ratio, bl.y*ratio);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Imgproc.warpPerspective(src, doc, m, doc.size());

        return doc;
    }

    private ArrayList<MatOfPoint> findContours(Mat src) {

        //Size size = new Size(newBmp.getWidth(),newBmp.getHeight());

        Mat grayImage = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
        Mat cannedImage = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);

        //Imgproc.resize(src,resizedImage,size);
        Imgproc.cvtColor(src, grayImage, Imgproc.COLOR_RGBA2GRAY, 1);
        Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
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

        Utils.matToBitmap(cannedImage,newBmp);

        grayImage.release();
        cannedImage.release();
        return contours;
    }

    private Quadrilateral getQuadrilateral( ArrayList<MatOfPoint> contours , Size srcSize ) {

        double ratio = srcSize.height / 500;
        int height = Double.valueOf(srcSize.height / ratio).intValue();
        int width = Double.valueOf(srcSize.width / ratio).intValue();
        Size size = new Size(width,height);

        for ( MatOfPoint c: contours ) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

            Point[] points = approx.toArray();

            // select biggest 4 angles polygon
            if (points.length == 4) {
                Point[] foundPoints = sortPoints(points);

                if (insideArea(foundPoints, size)) {
                    return new Quadrilateral( c , foundPoints );
                }
            }
        }

        return null;
    }

    private Point[] sortPoints( Point[] src ) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

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

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal diference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    private boolean insideArea(Point[] rp, Size size) {

        int width = Double.valueOf(size.width).intValue();
        int height = Double.valueOf(size.height).intValue();
        int baseMeasure = height/4;

        int bottomPos = height-baseMeasure;
        int topPos = baseMeasure;
        int leftPos = width/2-baseMeasure;
        int rightPos = width/2+baseMeasure;

        return (
                rp[0].x <= leftPos && rp[0].y <= topPos
                        && rp[1].x >= rightPos && rp[1].y <= topPos
                        && rp[2].x >= rightPos && rp[2].y >= bottomPos
                        && rp[3].x <= leftPos && rp[3].y >= bottomPos

        );
    }

    public void identifyEdges(){
        Mat greymat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
        Imgproc.cvtColor(mat, greymat, Imgproc.COLOR_RGB2GRAY, 1);
        Mat cannymat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
        Imgproc.Canny(greymat, cannymat, 100, 300);
        blurmat = new Mat();
        Imgproc.GaussianBlur(cannymat, blurmat,new Size(5, 5),5);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(cannymat, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        Mat dest = Mat.zeros(cannymat.size(), CvType.CV_8UC3);

        MatOfPoint matOfPoint = new MatOfPoint();
        double maxVal = 0.0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
        {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea)
            {
                maxVal = contourArea;
                maxValIdx = contourIdx;
                matOfPoint = contours.get(contourIdx);
            }
        }



        Imgproc.drawContours(dest,contours ,maxValIdx,new Scalar(255,255,255), 1);
        MatOfPoint pts = contours.get(maxValIdx);
        Imgproc.fillConvexPoly(dest,matOfPoint,new Scalar(255,255,255));
        Point[] points = pts.toArray();
        Point lt = new Point();
        Point lb= new Point();
        Point rt=new Point();
        Point rb=new Point();

        double ltd=dest.cols();
        double rtd=dest.cols();
        double lbd=dest.rows();
        double rbd=dest.rows();
        for(Point pt : points){
            double dist_from_tl = Math.sqrt(Math.pow((0 - pt.x), 2) + Math.pow((dest.rows() - pt.y), 2)); //(0,656)
            double dist_from_tr = Math.sqrt(Math.pow((dest.cols() - pt.x), 2) + Math.pow((dest.rows() - pt.y), 2)); // (492,656)
            double dist_from_bl = Math.sqrt(Math.pow((0 - pt.x), 2) + Math.pow((0 - pt.y), 2)); // (0,0)
            double dist_from_br = Math.sqrt(Math.pow((dest.cols() - pt.x), 2) + Math.pow((0 - pt.y), 2)); // (492,0)

            if (ltd > dist_from_tl){
                lt = pt;
            }
            if (rtd > dist_from_tr){
                rt = pt;
            }
            if (lbd > dist_from_bl){
                lb = pt;
            }
            if (rbd > dist_from_br){
                rb = pt;
            }
        }
        Imgproc.circle(dest, lt, 5, new Scalar(0, 255, 0, 0), 4); // Green
        Imgproc.circle(dest, rt, 5, new Scalar(255, 0, 0, 0), 4); // Red
        Imgproc.circle(dest, lb, 5, new Scalar(255, 255, 0, 0), 4); // Yellow
        Imgproc.circle(dest, rb, 5, new Scalar(255, 255, 255, 0), 4); // White
//                                MatOfInt hull = new MatOfInt();*/
//                                Imgproc.convexHull(contours.get(maxValIdx),hull);
//
//                                contours.get(maxValIdx).ge


        //List<MatOfPoint> hullList = new ArrayList<>();
                                /*for (MatOfPoint contour : contours) {
                                    MatOfInt hull = new MatOfInt();
                                    Imgproc.convexHull(contour, hull);
                                    Point[] contourArray = contour.toArray();
                                    Point[] hullPoints = new Point[hull.rows()];
                                    List<Integer> hullContourIdxList = hull.toList();
                                    for (int i = 0; i < hullContourIdxList.size(); i++) {
                                        hullPoints[i] = contourArray[hullContourIdxList.get(i)];
                                    }
                                    hullList.add(new MatOfPoint(hullPoints));
                                }*/


//                                for (int i=0; i <contours.size();i++){
//                                    //Convert contours(i) from MatOfPoint to MatOfPoint2f
//                                    MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(i).toArray());
//
//                                    //Processing on mMOP2f1 which is in type MatOfPoint2f
//                                    double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
//                                    Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);
//                                    //Convert back to MatOfPoint
//                                    MatOfPoint points = new MatOfPoint( approxCurve.toArray() );
//                                    Imgproc.drawContours(dest, points.toList(), i, green);
//                                    // Get bounding rect of contour
//                                    Rect rect = Imgproc.boundingRect(points);
//
//                                    Imgproc.rectangle(dest, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height), green, 2);
//                                }
        //Imgproc.polylines();
        Utils.matToBitmap(dest, newBmp);

        Bitmap resultImg = Bitmap.createBitmap(newBmp.getWidth(), newBmp.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap maskImg = Bitmap.createBitmap(newBmp.getWidth(), newBmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas mCanvas = new Canvas(resultImg);
        Canvas maskCanvas = new Canvas(maskImg);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);;
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));

        Path path = new Path();
        path.lineTo(150, 0);
        path.lineTo(230, 120);
        path.lineTo(70, 120);
        path.lineTo(150, 0);
        path.close();

        maskCanvas.drawPath(path, paint);

        mCanvas.drawBitmap(newBmp, 0, 0, null);
        mCanvas.drawBitmap(maskImg, 0, 0, paint);
        ivResult.setImageBitmap(newBmp);
    }
}
