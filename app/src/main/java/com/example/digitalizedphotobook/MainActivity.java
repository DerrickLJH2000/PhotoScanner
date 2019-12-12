package com.example.digitalizedphotobook;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.digitalizedphotobook.adapters.SavedPhotoAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "Magnifier";
    final int IMAGE_PICKER_SELECT = 2001;
    final int reqPermissionId = 200;
    private TextView tvUnavailable;
    private FloatingActionButton fabAdd;
    private RecyclerView rvSavedPhotos;
    private RecyclerView.LayoutManager layManager;
    private String[] directories;
    ArrayList<File> photoArr = new ArrayList<File>();
    SavedPhotoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fabAdd = findViewById(R.id.fabAdd);
        rvSavedPhotos = findViewById(R.id.rvSavedPhotos);
        tvUnavailable = findViewById(R.id.tvUnavailable);

        if (checkPermission()) {

        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, reqPermissionId);
        }

        rvSavedPhotos.setHasFixedSize(true);
        layManager = new GridLayoutManager(this, 3);
        rvSavedPhotos.setLayoutManager(layManager);
        adapter = new SavedPhotoAdapter(photoArr);

        File folder = getExternalFilesDir("Temp");
        directories = folder.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });
        for( int i = 0; i< directories.length;i++){
            File file1 = getExternalFilesDir("Temp/" + directories[i]);
            File[] files = file1.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getPath().contains("temp");
                }
            });
            if (files.length != 0) {
                for (int x =0;x < files.length;x++){
                    photoArr.add(files[x]);
                }
            }
        }
        Collections.sort(photoArr);

//        if (folder != null){
//            File[] files = folder.listFiles();
//            Log.i(TAG, "Files in Photobook: " + files.toString());
//            for (int i = 0; i < files.length; i++){
//                photoArr.add(files[i]);
//            }
//            Collections.sort(photoArr);
//        }

        if (photoArr.size() == 0){
            tvUnavailable.setVisibility(View.VISIBLE);
        } else {
            tvUnavailable.setVisibility(View.GONE);
        }

        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ScanActivity.class);
                startActivity(i);
            }
        });

        adapter.setOnItemClickListener(new SavedPhotoAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View v) {
                final File currItem = photoArr.get(position);
                Toast.makeText(v.getContext(), "click on item: " + currItem.getName(), Toast.LENGTH_SHORT).show();
                Intent i = new Intent(v.getContext(), ImageFullScreen.class);
                i.putExtra("path", currItem.getAbsolutePath());
                v.getContext().startActivity(i);
            }

            @Override
            public void onItemLongClick(int position, View v) {
                deleteDialog(position);
            }
        });

        rvSavedPhotos.setAdapter(adapter);
    }

    private void deleteDialog(int position){
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Do you want to delete this image?");
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                "Delete",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String tempPath = photoArr.get(position).getParent();
                        File file = new File(tempPath);
                        File[] allFiles = file.listFiles();
                        for (int i = 0;i<allFiles.length;i++){
                            allFiles[i].delete();
                        }
                        boolean deleted = file.delete();

                        if (deleted){
                            photoArr.remove(position);
                            Toast.makeText(MainActivity.this, "Successfully Deleted!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Error deleting image!", Toast.LENGTH_SHORT).show();
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

        builder1.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    };

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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        Toast.makeText(this, "Note: Older phones have problem rendering the image", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
    }
}
