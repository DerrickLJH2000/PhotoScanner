package com.example.digitalizedphotobook;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {
    public static final String TAG = "ResultActivity";
    private ImageView ivColour, ivResult ,ivBack, ivSave;
    private String selection;
    private Bitmap bmp, newBmp;
    private File mFile;

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
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        ivBack = findViewById(R.id.ivBack);
        ivColour = findViewById(R.id.ivColour);
        ivResult = findViewById(R.id.ivResult);
        ivSave = findViewById(R.id.ivConfirm);

        if (!OpenCVLoader.initDebug()) {
            return;
        }
        int permissionCheck = ContextCompat.checkSelfPermission(ResultActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PermissionChecker.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission Not Granted");
            ActivityCompat.requestPermissions(ResultActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            return;
        }
        final String imagePath = getIntent().getStringExtra("croppedPoints");
        mFile = new File(imagePath);
        setPic(mFile.getAbsolutePath());

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final String[] matArr = this.getResources().getStringArray(R.array.mats);
        ivColour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ResultActivity.this);
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
//                                Utils.matToBitmap(mat, newBmp);
//                                ivResult.setImageBitmap(newBmp);
                                break;
                            case "Grey Scale":
//                                Mat greyscale = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
//                                Imgproc.cvtColor(mat, greyscale, Imgproc.COLOR_RGB2GRAY, 1);
//                                Imgproc.GaussianBlur(greyscale, greyscale, new Size(5, 5), 0);
//                                Imgproc.dilate(greyscale, greyscale, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
//                                Imgproc.erode(greyscale, greyscale, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3,3)));
//
//                                Imgproc.Canny(greyscale, greyscale, 50, 150);
//                                Utils.matToBitmap(greyscale, newBmp);
//                                ivResult.setImageBitmap(newBmp);
                                break;
                            case "Canny":
//                                ArrayList<MatOfPoint> contours = findContours(mat);
//
//                                Mat doc = new Mat(mat.size(), CvType.CV_8UC4);
//
//                                if (quad != null) {
//
//                                } else {
//                                    mat.copyTo(doc);
//                                    Log.i("Points", "failed");
//                                }
//                                enhanceDocument(doc);
//                                ivResult.setImageBitmap(newBmp);
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
    }

    private void setPic(String photoPath) {
        // Get the dimensions of the View
        int targetW = ivResult.getWidth();
        int targetH = ivResult.getHeight();

        Log.i(TAG, "targetW: " + targetW + "targetW: " + targetH);

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = 1;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);
        ivResult.setImageBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(ResultActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
