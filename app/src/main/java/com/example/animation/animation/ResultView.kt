package com.example.animation.animation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.animation.R

class ResultView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val progressRect = RectF()
    private val progressPaint by lazy {
        Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeWidth = progressWidth
            strokeCap = Paint.Cap.ROUND
            color = progressColor
        }
    }
    private val circlePaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = progressColor
        }
    }

    private var progressColor: Int = context.resources.getColor(R.color.result_progress_default_color, context.theme)
    private var progressWidth: Float = context.resources.getDimension(R.dimen.result_default_width)

    var progressAngle: Float = 0.0f
        set(value) {
            field = value
            invalidate()
        }
    var circleClipPercentage: Float? = null
        set(value) {
            field = value
            invalidate()
        }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ResultView,
            0, 0
        ).apply {
            try {
                progressWidth = getDimension(R.styleable.ResultView_progressWidth, progressWidth)
                progressColor = getColor(R.styleable.ResultView_progressColor, progressColor)
            } finally {
                recycle()
            }
        }
    }

    private var radius: Float = 0f
    private var horizontalCenter: Float = 0f
    private var verticalCenter: Float = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val height = MeasureSpec.getSize(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)

        horizontalCenter = (width / 2).toFloat()
        verticalCenter = (height / 2).toFloat()

        // When calculating radius we need to consider line width (stroke), so that the view would fit exactly
        radius = if (height >= width) {
            (width / 2).toFloat() - progressWidth
        } else {
            (height / 2).toFloat() - progressWidth
        }

        progressRect.set(
            horizontalCenter - radius,
            verticalCenter - radius,
            horizontalCenter + radius,
            verticalCenter + radius
        )
    }

    private val circleClipPath = Path()

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null)
            return

        canvas.drawArc(progressRect, 270f, progressAngle, false, progressPaint)
        circleClipPercentage?.let { circleClipPercentage ->
            circleClipPath.apply {
                reset()
                addCircle(horizontalCenter, verticalCenter, radius * circleClipPercentage, Path.Direction.CW)
            }
            canvas.clipPath(circleClipPath, Region.Op.DIFFERENCE)
            canvas.drawCircle(horizontalCenter, verticalCenter, radius, circlePaint)
        }
    }

}