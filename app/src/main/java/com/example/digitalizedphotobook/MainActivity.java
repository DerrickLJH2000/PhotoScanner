package com.example.digitalizedphotobook;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    final int IMAGE_PICKER_SELECT = 2001;
    final int reqPermissionId = 1001;
    private ImageView ivLoadGallery;
    private ImageView ivCameraIntent;
    private RecyclerView rvSavedPhotos;
    private RecyclerView.LayoutManager layManager;
    ArrayList<String> photoArr = new ArrayList<String>();
    SavedPhotoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ivLoadGallery = findViewById(R.id.ivLoad);
        ivCameraIntent = findViewById(R.id.ivCamera);
        rvSavedPhotos = findViewById(R.id.rvSavedPhotos);

        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA);

        if (permissionCheck == PermissionChecker.PERMISSION_GRANTED) {
            rvSavedPhotos.setHasFixedSize(true);
            layManager = new GridLayoutManager(this, 3);
            rvSavedPhotos.setLayoutManager(layManager);

            adapter = new SavedPhotoAdapter(photoArr);
            rvSavedPhotos.setAdapter(adapter);
            photoArr.add("Sample 1");
            photoArr.add("Sample 2");
            photoArr.add("Sample 3");
            photoArr.add("Sample 4");

            ivCameraIntent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Toast.makeText(MainActivity.this, "Launch Scan Activity", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(MainActivity.this, ScanActivity.class);
                    startActivity(i);
                }
            });

            ivLoadGallery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, IMAGE_PICKER_SELECT);
                }
            });

        } else {
            Log.e("Permission Status" , "Permission not granted");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA},reqPermissionId);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // map.setMyLocationEnabled(true);
                } else {
                    Toast.makeText(MainActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
