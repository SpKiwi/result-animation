package com.example.animation

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import kotlin.math.cos
import kotlin.math.sin

class BadgedImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var mainBadgeRadius = 20f
    private var secondBadgeRadius = 4f
    private var badgePosition = .0
    var isBadgeVisible = true
        set(value) {
            if (value != field) {
                field = value
                invalidate()
            }
        }

    init {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.BadgedImageView, defStyleAttr, 0)
        mainBadgeRadius = typedArray.getDimension(R.styleable.BadgedImageView_mainBadgeRadius, mainBadgeRadius)
        secondBadgeRadius = typedArray.getDimension(R.styleable.BadgedImageView_secondBadgeRadius, secondBadgeRadius)
        badgePosition = typedArray.getFloat(R.styleable.BadgedImageView_badgePosition, badgePosition.toFloat()).toDouble()
        typedArray.recycle()
    }

    private val paintMain by lazy {
        Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.purple_200)
        }
    }

    private val paintSecond by lazy {
        Paint().apply {
            isAntiAlias = true
            color = ContextCompat.getColor(context, R.color.white)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (isBadgeVisible) {
            val center = measuredWidth / 2f //
            val x = center + cos(Math.toRadians(badgePosition)) * center
            val y = center + sin(Math.toRadians(badgePosition)) * center

            canvas?.drawCircle(x.toFloat(), y.toFloat(), mainBadgeRadius, paintMain)
            canvas?.drawCircle(x.toFloat(), y.toFloat(), secondBadgeRadius, paintSecond)
        }
    }
}