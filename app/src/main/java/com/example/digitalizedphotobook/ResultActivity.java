package com.example.digitalizedphotobook;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.digitalizedphotobook.adapters.FilterAdapter;
import com.example.digitalizedphotobook.classes.Filter;
import com.example.digitalizedphotobook.effects.MvEffects;
import com.github.chrisbanes.photoview.PhotoView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;


import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class ResultActivity extends AppCompatActivity{
    public static final String TAG = "ResultActivity";
    public ImageView ivFilters, ivBack, ivSave, ivRotate;
    private PhotoView ivResult;
    private LinearLayout linlay1, filterSettings;
    private String imagePath;
    private Toolbar toolbar;
    private Bitmap bitmap, newBitMap;
    private View mView;
    private File mFile;
    public Mat mat, newMat;
    private boolean isFilterExtended = false;
    private RecyclerView rvFilter;
    private RecyclerView.LayoutManager layManager;
    ArrayList<Filter> filterArr = new ArrayList<Filter>();
    FilterAdapter adapter;

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

    private void showToast(final String text) {
        Toast toast = Toast.makeText(ResultActivity.this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 30);
        toast.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);


        toolbar = (Toolbar) findViewById(R.id.include);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        ivBack = findViewById(R.id.ivBack);
        ivFilters = findViewById(R.id.ivFilters);
        ivResult = findViewById(R.id.ivResult);
        ivSave = findViewById(R.id.ivConfirm);
        ivRotate = findViewById(R.id.ivRotate);
        ivFilters = findViewById(R.id.ivFilters);
        rvFilter = findViewById(R.id.rvFilters);
        filterSettings = findViewById(R.id.filterSettings);
        linlay1 = findViewById(R.id.linlay1);
        mView = findViewById(R.id.clickView);


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

        rvFilter.setHasFixedSize(true);
        layManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvFilter.setLayoutManager(layManager);
        adapter = new FilterAdapter(filterArr);

        imagePath = getIntent().getStringExtra("croppedPoints");

        mFile = new File(imagePath);
        Log.i(TAG, "ABSOLUTE PATH" + mFile.getAbsolutePath());
        setPic(mFile.getAbsolutePath());

        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ivFilters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFilterExtended) {
                    filterSettings.setVisibility(View.VISIBLE);
                    mView.setVisibility(View.VISIBLE);
                    mView.animate().translationY(filterSettings.getHeight() * -1);
                    filterSettings.animate().translationY(filterSettings.getHeight() * -1);
                    isFilterExtended = true;
                    ivFilters.setColorFilter(ContextCompat.getColor(ResultActivity.this, R.color.blue), PorterDuff.Mode.SRC_IN);
                } else {
                    mView.setVisibility(View.GONE);
                    filterSettings.animate().translationY(filterSettings.getHeight() + linlay1.getHeight());
                    isFilterExtended = false;
                    ivFilters.setColorFilter(ContextCompat.getColor(ResultActivity.this, R.color.color_white), PorterDuff.Mode.SRC_IN);
                }
            }
        });
        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFilterExtended) {
                    filterSettings.animate().translationY(filterSettings.getHeight() + linlay1.getHeight());
                    isFilterExtended = false;
                }
                mView.setVisibility(View.GONE);
            }
        });

        newBitMap = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
        ArrayList<Bitmap> filterBmpArr = new ArrayList<>();
        String[] filterNames = {"Original", "Autofix", "Grayscale", "Sepia", "Sunset", "Saturate", "Sharpen", "Intensify"};

        for (int i = 0; i < filterNames.length; i++) {
            // Resize Bitmap to 90,90 Thumbnail
            Bitmap tempBmp;
            if (i == 0) {
                tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.NONE);
            } else if (i == 1) {
                tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.AUTOFIX);
            } else if (i == 2) {
                tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.GRAYSCALE);
            } else if (i == 3) {
                tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.SEPIA);
            } else if (i == 4) {
                tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.SUNSET);
            } else if (i == 5) {
                tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.SATURATE);
            } else if (i == 6) {
                tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.SHARPEN);
            } else {
                tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.COLORINTENSIFY);
            }
            filterBmpArr.add(tempBmp);
        }

        for (int i = 0; i < filterBmpArr.size(); i++) {
            Filter filter = new Filter(filterNames[i], filterBmpArr.get(i));
            filterArr.add(filter);
        }

        adapter.setOnItemClickListener(new FilterAdapter.ClickListener() {
            @Override
            public void onItemClick(int i, View v) {
                Bitmap tempBmp;
                if (i == 0) {
                    tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.NONE);
                } else if (i == 1) {
                    tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.AUTOFIX);
                } else if (i == 2) {
                    tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.GRAYSCALE);
                } else if (i == 3) {
                    tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.SEPIA);
                } else if (i == 4) {
                    tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.SUNSET);
                } else if (i == 5) {
                    tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.SATURATE);
                } else if (i == 6) {
                    tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.SHARPEN);
                } else {
                    tempBmp = MvEffects.applyFilter(bitmap, MvEffects.Type.COLORINTENSIFY);
                }
                ivResult.setImageBitmap(tempBmp);
            }

            @Override
            public void onItemLongClick(int position, View v) {

            }
        });
        rvFilter.setAdapter(adapter);

        ivRotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap tempBmp = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
                tempBmp = doRotate(tempBmp, 90);
                ivResult.setImageBitmap(tempBmp);
                Log.i(TAG, "Rotated 90 degrees");
            }
        });

        ivSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog();
            }
        });

    }

    private Bitmap doRotate(Bitmap source, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        source = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
        return source;
    }

    //Alert to Save Image
    private void alertDialog() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Are you sure you want to save this image?");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Save",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        File file = new File(imagePath);
                        boolean deleted = file.delete();
                        if (newBitMap != null) {
                            Bitmap tempBitmap = ((BitmapDrawable) ivResult.getDrawable()).getBitmap();
                            Matrix matrix = new Matrix();
                            matrix.postRotate(ivResult.getRotation());
                            Bitmap rotatedBmp = Bitmap.createBitmap(tempBitmap, 0, 0, tempBitmap.getWidth(), tempBitmap.getHeight(), matrix, true);
                            insertImage(getContentResolver(), rotatedBmp, UUID.randomUUID().toString(), "Saved Photo");
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                            byte[] bytes = stream.toByteArray();
                            long yourmilliseconds = System.currentTimeMillis();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
                            Date date = new Date(yourmilliseconds);
                            File mFile = new File(getExternalFilesDir("Photobook"), sdf.format(date) + ".jpg");
                            try {
                                mFile.createNewFile();
                                FileOutputStream fileOutputStream = new FileOutputStream(mFile);
                                fileOutputStream.write(bytes);
                                fileOutputStream.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Toast toast = Toast.makeText(ResultActivity.this, "Saved!", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            dialog.cancel();
                            Intent intent = new Intent(ResultActivity.this, PhotosActivity.class);
                            intent.putExtra("folderPath", getExternalFilesDir("Photobook"));
                            startActivity(intent);
                        } else {
                            showToast("Error Saving Image to Gallery!");
                        }
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
    }


    //Insert Image to Gallery
    public static final String insertImage(ContentResolver cr, Bitmap source, String title, String description) {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DISPLAY_NAME, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, description);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        // Add the date meta data to ensure the image is added at the front of the gallery
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

        Uri url = null;
        String stringUrl = null;    /* value to be returned */

        try {
            url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (source != null) {
                OutputStream imageOut = cr.openOutputStream(url);
                try {
                    source.compress(Bitmap.CompressFormat.JPEG, 100, imageOut);
                } finally {
                    imageOut.close();
                }

                long id = ContentUris.parseId(url);
                // Wait until MINI_KIND thumbnail is generated.
                Bitmap miniThumb = MediaStore.Images.Thumbnails.getThumbnail(cr, id, MediaStore.Images.Thumbnails.MINI_KIND, null);
                // This is for backward compatibility.
                storeThumbnail(cr, miniThumb, id, 50F, 50F, MediaStore.Images.Thumbnails.MICRO_KIND);
            } else {
                cr.delete(url, null, null);
                url = null;
            }
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
                url = null;
            }
        }

        if (url != null) {
            stringUrl = url.toString();
        }

        return stringUrl;
    }

    private static final Bitmap storeThumbnail(ContentResolver cr, Bitmap source, long id, float width, float height, int kind) {

        // create the matrix to scale it
        Matrix matrix = new Matrix();

        float scaleX = width / source.getWidth();
        float scaleY = height / source.getHeight();

        matrix.setScale(scaleX, scaleY);

        Bitmap thumb = Bitmap.createBitmap(source, 0, 0,
                source.getWidth(),
                source.getHeight(), matrix,
                true
        );

        ContentValues values = new ContentValues(4);
        values.put(MediaStore.Images.Thumbnails.KIND, kind);
        values.put(MediaStore.Images.Thumbnails.IMAGE_ID, (int) id);
        values.put(MediaStore.Images.Thumbnails.HEIGHT, thumb.getHeight());
        values.put(MediaStore.Images.Thumbnails.WIDTH, thumb.getWidth());

        Uri url = cr.insert(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, values);

        try {
            OutputStream thumbOut = cr.openOutputStream(url);
            thumb.compress(Bitmap.CompressFormat.PNG, 100, thumbOut);
            thumbOut.close();
            return thumb;
        } catch (FileNotFoundException ex) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }

    //Image Color Options Menu
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.original:
                Utils.matToBitmap(newMat, newBitMap);
                ivResult.setImageBitmap(newBitMap);
                break;
            case R.id.greyscale:
                Mat greyscale = new Mat(bitmap.getWidth(), bitmap.getHeight(), CV_8UC1);
                cvtColor(newMat, greyscale, Imgproc.COLOR_RGB2GRAY, 1);
                Utils.matToBitmap(greyscale, newBitMap);
                ivResult.setImageBitmap(newBitMap);
                break;
            case R.id.blackwhite:
                Mat doc = new Mat(newBitMap.getWidth(), newBitMap.getHeight(), CV_8UC4);
                Utils.bitmapToMat(newBitMap, doc);
//                enhanceDocument(doc);
                Utils.matToBitmap(doc, newBitMap);
                ivResult.setImageBitmap(newBitMap);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    // Set Image with Bitmap Option Settings
    private void setPic(String photoPath) {

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;


        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = 1;
        bmOptions.inPurgeable = true;

        bitmap = BitmapFactory.decodeFile(photoPath, bmOptions);
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
