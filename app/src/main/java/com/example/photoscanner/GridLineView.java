package com.example.photoscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class GridLineView extends View {

    private static final int DEFAULT_PAINT_COLOR = Color.WHITE;
    private static final int DEFAULT_NUMBER_OF_ROWS = 3;
    private static final int DEFAULT_NUMBER_OF_COLUMNS = 3;
    private int width = getMeasuredWidth();
    private int height = getMeasuredHeight();
    private boolean showGrid = false;
    private final Paint paint = new Paint();
    private int numRows = DEFAULT_NUMBER_OF_ROWS, numColumns = DEFAULT_NUMBER_OF_COLUMNS;

    public GridLineView(Context context) {
        super(context);
        init();
    }

    public GridLineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GridLineView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        paint.setColor(DEFAULT_PAINT_COLOR);
    }

    public void setLineColor(int color) {
        paint.setColor(color);
    }

    public void setStrokeWidth(int pixels) {
        paint.setStrokeWidth(pixels);
    }

    public int getNumberOfRows() {
        return numRows;
    }

    public void setNumberOfRows(int numRows) {
        if (numRows > 0) {
            this.numRows = numRows;
        } else {
            throw new RuntimeException("Cannot have a negative number of rows");
        }
    }

    public boolean isGridShown() {
        return showGrid;
    }

    public void toggleGrid() {
        showGrid = !showGrid;
        invalidate();
    }

    public void setWidthHeight(int w, int h) {
        width = h;
        height = w;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (showGrid) {
            // Vertical lines
            for (int i = 1; i < numColumns; i++) {
                canvas.drawLine(width * i / numColumns, 0, width * i / numColumns, height, paint);
            }

            // Horizontal lines
            for (int i = 1; i < numRows; i++) {
                canvas.drawLine(0, height * i / numRows, width, height * i / numRows, paint);
            }
        }
    }
}