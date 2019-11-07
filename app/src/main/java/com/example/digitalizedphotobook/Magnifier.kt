package com.rmkrishna.demo.magnifier

import android.graphics.Rect
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Magnifier
import androidx.annotation.RequiresApi
import com.example.digitalizedphotobook.R
import kotlinx.android.synthetic.main.activity_adjustment.*
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Reference activity to know the usage Magnifier in Android P
 */
class Magnifier : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val magnifier = Magnifier(polygonView)

        // Getting Visible drawing bounds of your view
        val outRect = Rect()

        // Getting Views's X and Y location from the screen
        val viewPosition = IntArray(2)

        // Wait till the screen to render
//        sampleText.post {
//            sampleText.getDrawingRect(outRect)
//            sampleText.getLocationOnScreen(viewPosition)
//
//            // Setting offset to compare the rect with touch event raw values
//            outRect.offset(viewPosition[0], viewPosition[1])
//        }

        polygonView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {

                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())
                    ) {
                        magnifier.dismiss()
                        return@setOnTouchListener false
                    }
                    magnifier.show(event.rawX - viewPosition[0], event.rawY - viewPosition[1])
                }
                MotionEvent.ACTION_UP -> {
                    magnifier.dismiss()
                }
            }
            true
        }
    }
}
