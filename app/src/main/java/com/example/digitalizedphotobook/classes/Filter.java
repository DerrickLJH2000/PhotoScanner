package com.example.digitalizedphotobook.classes;

import android.graphics.Bitmap;
import android.widget.ImageView;

public class Filter {

    public Filter(String name, Bitmap image) {
        this.name = name;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    String name;
    Bitmap image;
}
