package com.example.digitalizedphotobook.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.digitalizedphotobook.R;
import com.example.digitalizedphotobook.ResultActivity;
import com.example.digitalizedphotobook.classes.Filter;

import java.util.ArrayList;

import static org.opencv.imgproc.Imgproc.cvtColor;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {

    private static ClickListener clickListener;
    private ArrayList<Filter> filterArr;
    private Context context;

    // RecyclerView recyclerView;
    public FilterAdapter(ArrayList<Filter> filterArr) {
        this.filterArr = filterArr;
    }

    @NonNull
    @Override
    public FilterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        context = viewGroup.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View listItem = layoutInflater.inflate(R.layout.row_filters, viewGroup, false);
        FilterAdapter.ViewHolder viewHolder = new FilterAdapter.ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int i) {
        final Filter currItem = filterArr.get(i);
        String name = currItem.getName();
        Bitmap bitmap = currItem.getImage();
        viewHolder.imageView.setImageBitmap(bitmap);
        viewHolder.textView.setText(name);
    }



    @Override
    public int getItemCount() {
        return filterArr.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener{
        public ImageView imageView;
        public TextView textView;
        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
            this.imageView = (ImageView) itemView.findViewById(R.id.ivFilter);
            this.textView = (TextView) itemView.findViewById(R.id.tvFilter);
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
        FilterAdapter.clickListener = clickListener;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
        void onItemLongClick(int position, View v);
    }
}
