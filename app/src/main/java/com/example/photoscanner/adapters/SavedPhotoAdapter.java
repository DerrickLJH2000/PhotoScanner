package com.example.photoscanner.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Vibrator;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.photoscanner.R;

import java.io.File;
import java.util.ArrayList;


public class SavedPhotoAdapter extends RecyclerView.Adapter<SavedPhotoAdapter.ViewHolder> {
    private static ClickListener clickListener;
    private ArrayList<File> photoArr;
    private static final int LONG_CLICK_DURATION = 500;
    private long startClicktime;
    Vibrator vibrator;

    // RecyclerView recyclerView;
    public SavedPhotoAdapter(ArrayList<File> photoArr) {
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
        final File currItem = photoArr.get(position);
        String imgPath = currItem.getAbsolutePath();
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
        holder.imageView.setImageBitmap(bitmap);
    }

    @Override
    public int getItemCount() {
        return photoArr.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {
        public ImageView imageView;
        public ImageView ivDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            this.imageView = (ImageView) itemView.findViewById(R.id.ivSavedPhotos);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(getAdapterPosition(), v);
        }

        @Override
        public boolean onLongClick(View v) {
            clickListener.onItemLongClick(getAdapterPosition(), v);
            return false;
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        SavedPhotoAdapter.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
        void onItemLongClick(int position, View v);
    }
}