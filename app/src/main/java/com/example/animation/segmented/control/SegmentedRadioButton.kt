package com.example.animation.segmented.control

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatRadioButton

class SegmentedRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatRadioButton(context, attrs) {

    override fun getHitRect(outRect: Rect?) {
        super.getHitRect(outRect)
        if (outRect == null)
            return

        val pixels = 150
        outRect.run {
            top -= pixels
            left -= pixels
            bottom += pixels
            right += pixels
        }
    }
}