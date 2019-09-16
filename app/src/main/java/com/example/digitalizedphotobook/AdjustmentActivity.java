package com.example.digitalizedphotobook;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class AdjustmentActivity extends AppCompatActivity {
    private static final String TAG = "AdjustmentActivity";
    ImageView ivBack, ivColour, ivConfirm, ivAdjust;
    RelativeLayout rellay1;
    ProgressBar progressBar;
    private int mProgressStatus = 0;
    private Handler mHandler = new Handler();
    private String selection;
    Mat imgMat;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    imgMat = new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adjustment);
        ivAdjust = findViewById(R.id.ivAdjust);
        ivBack = findViewById(R.id.ivBack);
        ivColour = findViewById(R.id.ivColour);
        ivConfirm = findViewById(R.id.ivConfirm);
        rellay1 = findViewById(R.id.rellay1);
        progressBar = findViewById(R.id.progressBar);

        Bitmap bitmap = ((BitmapDrawable) ivAdjust.getDrawable()).getBitmap();
        Utils.bitmapToMat(bitmap, imgMat);

        final String[] matArr = this.getResources().getStringArray(R.array.mats);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (mProgressStatus < 100) {
                    mProgressStatus++;
                    android.os.SystemClock.sleep(50);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(mProgressStatus);
                        }
                    });

                }
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        rellay1.setVisibility(View.GONE);
                    }
                });
            }
        }).start();

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
                                Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_GRAY2RGB,4);
                                break;
                            case "Grey Scale":
                                Imgproc.cvtColor(imgMat, imgMat, Imgproc.COLOR_RGB2GRAY);
                                break;
                            case "B & W":
                                Imgproc.cvtColor(imgMat, imgMat, Imgproc.THRESH_BINARY);
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


}
