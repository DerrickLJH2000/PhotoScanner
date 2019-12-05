package com.example.digitalizedphotobook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.digitalizedphotobook.adapters.AlbumAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class AlbumActivity extends AppCompatActivity {

    public static String TAG = "Magnifier";
    final int IMAGE_PICKER_SELECT = 2001;
    final int reqPermissionId = 200;
    private TextView tvUnavailable;
    private FloatingActionButton fabAdd;
    private RecyclerView rvSavedAlbums;
    private RecyclerView.LayoutManager layManager;
    ArrayList<File> photoArr = new ArrayList<File>();
    AlbumAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        fabAdd = findViewById(R.id.fabAdd);
        rvSavedAlbums = findViewById(R.id.rvSavedAlbums);
        tvUnavailable = findViewById(R.id.tvUnavailable);

        if (checkPermission()) {

        } else {
            ActivityCompat.requestPermissions(AlbumActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, reqPermissionId);
        }

        rvSavedAlbums.setHasFixedSize(true);
        layManager = new LinearLayoutManager(this);
        rvSavedAlbums.setLayoutManager(layManager);
        adapter = new AlbumAdapter(photoArr);

        File folder = getExternalFilesDir("Photobook");
        if (folder != null){
            File[] files = folder.listFiles();
            Log.i(TAG, "Files in Photobook: " + files.toString());
            for (int i = 0; i < files.length; i++){
                photoArr.add(files[i]);
            }
            Collections.sort(photoArr);
        }

        if (photoArr.size() == 0){
            tvUnavailable.setVisibility(View.VISIBLE);
        } else {
            tvUnavailable.setVisibility(View.GONE);
        }
        rvSavedAlbums.setAdapter(adapter);


        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(AlbumActivity.this, ScanActivity.class);
                startActivity(i);
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 200: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(AlbumActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    private boolean checkPermission() {
        int permissionCheck_Cam = ContextCompat.checkSelfPermission(AlbumActivity.this,
                Manifest.permission.CAMERA);
        int permissionCheck_Write = ContextCompat.checkSelfPermission(AlbumActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck_Read = ContextCompat.checkSelfPermission(AlbumActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permissionCheck_Cam == PermissionChecker.PERMISSION_GRANTED && permissionCheck_Read == PermissionChecker.PERMISSION_GRANTED
                && permissionCheck_Write == PermissionChecker.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        Toast.makeText(this, "Note: Older phones have problem rendering the image", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
    }
}
