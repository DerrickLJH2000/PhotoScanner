package com.example.digitalizedphotobook;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

public class ImageFullScreen extends AppCompatActivity {


    ImageView ivBack,ivEdit;
    PhotoView myImage;
    String path = "";
    Bitmap bitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_image_full_screen);
        myImage = findViewById(R.id.myImage);
        ivBack = findViewById(R.id.ivBack);
//        ivEdit = findViewById(R.id.ivEdit);

        path = getIntent().getStringExtra("path");
//        File test = new File(path);
//        String uuid = test.getParentFile().getName();
        myImage = findViewById(R.id.myImage);
        bitmap = BitmapFactory.decodeFile(path);
        myImage.setImageBitmap(bitmap);

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        /*ivEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ImageFullScreen.this, AdjustmentActivity.class);
                intent.putExtra("image", path);
                intent.putExtra("name", test.getName());
                intent.putExtra("process_id", uuid);
                startActivity(intent);
            }
        });*/
    }

}
