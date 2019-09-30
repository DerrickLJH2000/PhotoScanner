package com.example.digitalizedphotobook.adapters;

import android.os.Vibrator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.digitalizedphotobook.R;

import java.util.ArrayList;


public class SavedPhotoAdapter extends RecyclerView.Adapter<SavedPhotoAdapter.ViewHolder> {
    private ArrayList<String> photoArr;
    private static final int LONG_CLICK_DURATION = 500;
    private long startClicktime;
    Vibrator vibrator;

    // RecyclerView recyclerView;
    public SavedPhotoAdapter(ArrayList<String> photoArr) {
        this.photoArr = photoArr;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.row_saved_photo, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final String currItem = photoArr.get(position);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "click on item: " + currItem, Toast.LENGTH_SHORT).show();
            }
        });
        //holder.imageView.setImageResource(photoArr[position]);
        /*holder.imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.i("Testing", "User Press down");
                    Toast.makeText(v.getContext(), "click on item: " + currItem, Toast.LENGTH_SHORT).show();
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    Log.i("Testing", "Move Motion Detected");
                    startClicktime = Calendar.getInstance().getTimeInMillis();
                    long clickDuration = Calendar.getInstance().getTimeInMillis() - startClicktime;
                    if (clickDuration > LONG_CLICK_DURATION) {
                        holder.ivDelete.setVisibility(View.VISIBLE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(200);
                        }
                    }
                }
                return true;
            }
        });*/
    }


    @Override
    public int getItemCount() {
        return photoArr.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public ImageView ivDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView.findViewById(R.id.ivSavedPhotos);
        }
    }
}