package com.example.animation.animation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
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

    private var progressColor: Int = context.resources.getColor(R.color.result_progress_default_color, context.theme)
    private var progressWidth: Float = context.resources.getDimension(R.dimen.result_default_width)

    var progressAngle: Float = 0.0f
        set(value) {
            field = value
            invalidate()
        }
    var circleFillPercentage: Float = 0.0f
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val height = MeasureSpec.getSize(heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)

        val horizontalCenter = (width / 2).toFloat()
        val verticalCenter = (height / 2).toFloat()

        // When calculating radius we need to consider line width (stroke), so that the view would fit exactly
        val radius: Float
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

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawArc(progressRect, 270f, progressAngle, false, progressPaint)
    }

}