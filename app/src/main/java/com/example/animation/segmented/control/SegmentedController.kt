package com.example.animation.segmented.control

import android.animation.ValueAnimator
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.get
import androidx.core.view.updateLayoutParams
import com.example.animation.R

class SegmentedController(
    private val layout: ConstraintLayout,
    private val radioGroup: RadioGroup,
) {

    private val toggleView: View =
        View(layout.context).apply {
            setBackgroundResource(R.drawable.toggle_item_bg_selected)
            layout.addView(this)
        }.also { view ->
            view.setBackgroundResource(R.drawable.toggle_item_bg_selected)
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                width = 0
                height = radioGroup[0].height
                topToTop = radioGroup.id
                startToStart = radioGroup.id
                bottomToBottom = radioGroup.id

                setMargins(radioGroup.paddingLeft, radioGroup.paddingTop, radioGroup.paddingRight, radioGroup.paddingBottom)
            }
        }

    private var toggleAnimator: ValueAnimator? = null

    init {
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedRadioView = radioGroup.findViewById<RadioButton>(checkedId)
            changeToggleViewPosition(selectedRadioView)
        }
        View(layout.context).apply {
            setBackgroundResource(R.drawable.toggle_group_bg)
            layout.addView(this)
        }.also { view ->
            view.setBackgroundResource(R.drawable.toggle_group_bg)
            view.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topToTop = radioGroup.id
                startToStart = radioGroup.id
                bottomToBottom = radioGroup.id
                endToEnd = radioGroup.id
                width = radioGroup.width
                height = radioGroup.height
            }
            ViewCompat.setElevation(view, 0f)
        }

        ViewCompat.setElevation(toggleView, 4f)
        ViewCompat.setElevation(radioGroup, 8f)
    }

    fun check(@IdRes id: Int) {
        radioGroup.check(id)
    }

    fun setOnCheckedChangeListener(listener: RadioGroup.OnCheckedChangeListener?) {
        radioGroup.setOnCheckedChangeListener { group, checkedId ->
            listener?.onCheckedChanged(group, checkedId)
            val selectedRadioView = radioGroup.findViewById<RadioButton>(checkedId)
            changeToggleViewPosition(selectedRadioView)
        }
    }

    private fun changeToggleViewPosition(selectedRadioButton: RadioButton) {
        toggleAnimator?.cancel()

        val currentWidth = toggleView.width
        val fullWidth = selectedRadioButton.width
        val widthDelta = fullWidth - toggleView.width

        val translateBy = selectedRadioButton.xOnScreen() - toggleView.xOnScreen()
        val initialToggleViewTranslation = toggleView.translationX

        toggleAnimator = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
            addUpdateListener {
                val animationPercentage = it.animatedValue as Float

                toggleView.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    width = currentWidth + (widthDelta * animationPercentage).toInt()
                }

                toggleView.translationX = initialToggleViewTranslation + (translateBy * animationPercentage)
            }
            duration = 300
            start()
        }
    }

    private fun View.xOnScreen(): Int {
        val locationArray = IntArray(2)
        getLocationOnScreen(locationArray)
        return locationArray[0]
    }

}