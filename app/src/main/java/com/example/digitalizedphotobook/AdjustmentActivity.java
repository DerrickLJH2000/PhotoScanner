package com.example.digitalizedphotobook;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
                                blurmat = new Mat();
                                Imgproc.blur(greyscale, blurmat, new Size(3,3));
                                Utils.matToBitmap(blurmat, newBmp);
                                ivResult.setImageBitmap(newBmp);
                                break;
                            case "Canny":
                                Mat greymat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
                                Imgproc.cvtColor(mat, greymat, Imgproc.COLOR_RGB2GRAY, 1);
                                blurmat = new Mat();
                                Imgproc.blur(greymat, blurmat, new Size(3,3));
                                Mat cannymat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
                                Imgproc.Canny(blurmat, cannymat, 100, 300);
                                List<MatOfPoint> contours = new ArrayList<>();
                                Mat hierarchy = new Mat();
                                Imgproc.findContours(cannymat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0,0));
                                Mat dest = Mat.zeros(cannymat.size(), CvType.CV_8UC3);
                                Scalar green = new Scalar(81, 190, 0);
                                Scalar white = new Scalar(255,255,255);
                                Imgproc.drawContours(dest, contours, -1, white, 1);
                                MatOfPoint2f approxCurve = new MatOfPoint2f();
                                double maxVal = 0.0;
                                int maxValIdx = 0;
                                for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++)
                                {
                                    double contourArea = Imgproc.contourArea(contours.get(contourIdx));
                                    if (maxVal < contourArea)
                                    {
                                        maxVal = contourArea;
                                        maxValIdx = contourIdx;
                                    }
                                }



                                Imgproc.drawContours(dest,contours ,maxValIdx, white, 1);
                                MatOfPoint pts = contours.get(maxValIdx);
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
                                    double dist_from_tl = Math.sqrt(Math.pow((pt.x-0.0),2.0)+Math.pow((pt.y-0.0),2.0));
                                    double dist_from_tr = Math.sqrt(Math.pow((dest.cols()-pt.x),2.0)+Math.pow((pt.y-0.0),2.0));
                                    double dist_from_bl = Math.sqrt(Math.pow((pt.x-0.0),2.0)+Math.pow((dest.rows()-pt.y),2.0));
                                    double dist_from_br = Math.sqrt(Math.pow((dest.cols()-pt.x),2.0)+Math.pow((dest.rows()-pt.y),2.0));
                                    if(ltd>dist_from_tl){
                                        lt=pt;
                                    }
                                    if(rtd>dist_from_tr){
                                        rt=pt;
                                    }
                                    if(lbd>dist_from_bl){
                                        lb=pt;
                                    }
                                    if(rbd>dist_from_br){
                                        rb=pt;
                                    }
                                }
//                                MatOfInt hull = new MatOfInt();
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
                                Imgproc.circle(dest, lt, 5, new Scalar(0, 255, 0, 0), 4); // Green
                                Imgproc.circle(dest, rt, 5, new Scalar(255, 0, 0, 0), 4); // Red
                                Imgproc.circle(dest, lb, 5, new Scalar(255, 255, 0, 0), 4); // Yellow
                                Imgproc.circle(dest, rb, 5, new Scalar(255, 255, 255, 0), 4); // White
                                Utils.matToBitmap(dest, newBmp);
                                ivResult.setImageBitmap(newBmp);
                                break;
                            default:
                                return;
                        }
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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


}
