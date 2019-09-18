package com.example.digitalizedphotobook;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    final int IMAGE_PICKER_SELECT = 2001;
    final int reqPermissionId = 200;
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

        if (checkPermission()) {

        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, reqPermissionId);
        }

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == IMAGE_PICKER_SELECT && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Log.d(TAG, String.valueOf(bitmap));
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] bytes = stream.toByteArray();
            File mFile = new File(getExternalFilesDir("Temp"), "temp.jpg");
            try {
                mFile.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(mFile);
                fileOutputStream.write(bytes);
                fileOutputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Intent intent = new Intent(MainActivity.this, AdjustmentActivity.class);
            intent.putExtra("image", mFile.getAbsolutePath());
            intent.putExtra("reqCode", 0);

            startActivity(intent);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 200: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(MainActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private boolean checkPermission() {
        int permissionCheck_Cam = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CAMERA);
        int permissionCheck_Write = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck_Read = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck_Cam == PermissionChecker.PERMISSION_GRANTED && permissionCheck_Read == PermissionChecker.PERMISSION_GRANTED
                && permissionCheck_Write == PermissionChecker.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
}
