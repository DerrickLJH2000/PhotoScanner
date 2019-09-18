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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class AdjustmentActivity extends AppCompatActivity {
    private static final String TAG = "AdjustmentActivity";
    ImageView ivBack, ivColour, ivConfirm, ivAdjust, ivResult;
    RelativeLayout rellay1;
    ProgressBar progressBar;
    private String selection;
    File mFile;
    Bitmap bmp , newBmp;
    Mat mat;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public AdjustmentActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adjustment);
        ivAdjust = findViewById(R.id.ivAdjust);
        ivBack = findViewById(R.id.ivBack);
        ivColour = findViewById(R.id.ivColour);
        ivConfirm = findViewById(R.id.ivConfirm);
        ivResult = findViewById(R.id.ivResult);
        rellay1 = findViewById(R.id.rellay1);
        progressBar = findViewById(R.id.progressBar);
        if (!OpenCVLoader.initDebug()) {
            return;
        }
        int permissionCheck = ContextCompat.checkSelfPermission(AdjustmentActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PermissionChecker.PERMISSION_GRANTED) {
            Log.i(TAG,"Permission Not Granted");
            ActivityCompat.requestPermissions(AdjustmentActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
            return;
        }

        String imagePath = getIntent().getStringExtra("image");
        int reqCode = getIntent().getIntExtra("reqCode",-1);
        Log.i("Testing", (reqCode) + imagePath);
        if (reqCode == 0){
            mFile = new File(imagePath);
            bmp = BitmapFactory.decodeFile(mFile.getAbsolutePath());
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            newBmp = Bitmap.createBitmap(bmp, 0, 0,bmp.getWidth(), bmp.getHeight(), matrix, true);
            ivResult.setImageBitmap(newBmp);

        } else if (reqCode == 1) {
            mFile = new File(imagePath);
            newBmp = BitmapFactory.decodeFile(mFile.getAbsolutePath());
            ivResult.setImageBitmap(newBmp);
        }

        mat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
        Utils.bitmapToMat(newBmp,mat);

        final String[] matArr = this.getResources().getStringArray(R.array.mats);

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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
                                Mat greymat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
                                Imgproc.cvtColor(mat, greymat, Imgproc.COLOR_RGB2GRAY,1);
                                Utils.matToBitmap(greymat, newBmp);
                                ivResult.setImageBitmap(newBmp);
                                break;
                            case "Canny":
                                Mat cannymat = new Mat(newBmp.getWidth(), newBmp.getHeight(), CvType.CV_8UC1);
                                Imgproc.Canny(mat, cannymat, 50,150);
                                Utils.matToBitmap(cannymat, newBmp);
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
