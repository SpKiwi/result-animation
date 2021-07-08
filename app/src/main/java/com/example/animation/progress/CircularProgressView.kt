package com.example.animation.progress

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.animation.R
import java.lang.Integer.min

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val progressRect = RectF()
    private val progressPaint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeWidth = progressWidth.toFloat()
            strokeCap = Paint.Cap.ROUND
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), gradientColors, gradientPercentages, Shader.TileMode.MIRROR)
        }
    }

    private val gradientColors: IntArray = IntArray(3).apply {
        set(0, ContextCompat.getColor(context, R.color.purple_200))
        set(1, ContextCompat.getColor(context, R.color.purple_200))
        set(2, ContextCompat.getColor(context, R.color.purple_200))
    }
    private val gradientPercentages: FloatArray = FloatArray(3).apply {
        set(0, 0f)
        set(1, 0.5f)
        set(2, 1f)
    }

    var progressColor: Int = context.resources.getColor(R.color.circular_progress_view_default_color, context.theme)
        set(value) {
            field = value
            invalidate()
        }
    var progressWidth: Int = context.resources.getDimensionPixelOffset(R.dimen.circular_progress_view_default_progress_width)
        set(value) {
            field = value
            invalidate()
        }
    var progressAngle: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircularProgressView,
            0,
            0
        ).run {
            try {
                progressWidth = getDimensionPixelOffset(R.styleable.CircularProgressView_progressWidth, progressWidth)
                progressColor = getColor(R.styleable.CircularProgressView_progressColor, progressColor)
            } finally {
                recycle()
            }
        }
    }

    private var radius: Float = 0f
    private var horizontalCenter: Float = 0f
    private var verticalCenter: Float = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        horizontalCenter = w.toFloat() / 2
        verticalCenter = h.toFloat() / 2

        // When calculating radius we need to consider line width (stroke), so that the view would fit exactly
        radius = ((min(height, width) - progressWidth) / 2).toFloat()

        progressRect.set(
            horizontalCenter - radius,
            verticalCenter - radius,
            horizontalCenter + radius,
            verticalCenter + radius
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawArc(progressRect, 270f, progressAngle, false, progressPaint)
    }

}