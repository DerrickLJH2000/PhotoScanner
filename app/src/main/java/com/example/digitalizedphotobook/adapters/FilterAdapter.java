package com.example.digitalizedphotobook.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.digitalizedphotobook.AdjustmentActivity;
import com.example.digitalizedphotobook.R;
import com.example.digitalizedphotobook.classes.Filter;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

import static org.opencv.imgproc.Imgproc.cvtColor;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {

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
        viewHolder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Not Implemented yet.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return filterArr.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView textView;
        public ViewHolder(View itemView) {
            super(itemView);
            this.imageView = (ImageView) itemView.findViewById(R.id.ivFilter);
            this.textView = (TextView) itemView.findViewById(R.id.tvFilter);
        }
    }
}