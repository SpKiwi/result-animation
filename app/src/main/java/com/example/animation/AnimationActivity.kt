package com.example.animation

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.animation.animation.AutoFollowGroup
import com.example.animation.fresco.SmartImageView

class AnimationActivity : AppCompatActivity() {

    private val autoFollowGroup: AutoFollowGroup by lazy { findViewById<AutoFollowGroup>(R.id.animationGroup) }
    private val testView: View by lazy { findViewById<View>(R.id.testView) }
    private val testViewRestore: View by lazy { findViewById<View>(R.id.testViewRestore) }

    private val inTop: View by lazy { findViewById<View>(R.id.inTop) }
    private val inBottom: View by lazy { findViewById<View>(R.id.inBottom) }
    private val outTop: View by lazy { findViewById<View>(R.id.outTop) }
    private val outBottom: View by lazy { findViewById<View>(R.id.outBottom) }

    private val textToAnimate1: View by lazy { findViewById<View>(R.id.textToAnimate1) }
    private val textToAnimate2: View by lazy { findViewById<View>(R.id.textToAnimate2) }

    private val pidorView: SmartImageView by lazy { findViewById<SmartImageView>(R.id.pidorView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.animation_activity)

        testView.setOnClickListener {
            launchProgressAnimation()
        }
        testViewRestore.setOnClickListener {
            autoFollowGroup.restoreInitialState()
        }

        inTop.setOnClickListener {
            animateAutoFollowViews(TranslationType.IN_TOP, textToAnimate1, textToAnimate2)
        }
        inBottom.setOnClickListener {
            animateAutoFollowViews(TranslationType.IN_BOTTOM, textToAnimate1, textToAnimate2)
        }
        outTop.setOnClickListener {
            animateAutoFollowViews(TranslationType.OUT_TOP, textToAnimate1, textToAnimate2)
        }
        outBottom.setOnClickListener {
            animateAutoFollowViews(TranslationType.OUT_BOTTOM, textToAnimate1, textToAnimate2)
        }

        pidorView.setSmartBorder(R.color.black, R.dimen.auto_follow_default_width)

//        setSmartBorder
//        roundAsCircle
//        resizeToFit
//        placeholderImageScaleType
//        dimOnPressed
//        imageUrl
    }

    private fun launchProgressAnimation() {
        autoFollowGroup.startAutoFollowAnimation(5_000, object : AutoFollowGroup.AutoFollowListener {
            override fun onAutoFollowCancel() {
//                TODO("Not yet implemented")
            }

            override fun onAutoFollowTimerElapsed() {
//                TODO("Not yet implemented")
            }

            override fun onAutoFollowEnd() {
//                TODO("Not yet implemented")
            }
        })
    }

}

/**
 * Used to guarantee a specific result in expressions
 **/
val <T> T.exhaustive: T
    get() = this