package com.example.animation

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.animation.animation.AutoFollowGroup
import com.example.animation.fresco.SmartImageView
import com.example.animation.progress.CircularProgressView
import com.example.animation.progress.ProgressLayout
import com.facebook.drawee.view.SimpleDraweeView

class AnimationActivity : AppCompatActivity() {

    private val testView: View by lazy { findViewById<View>(R.id.testView) }
    private val testViewRestore: View by lazy { findViewById<View>(R.id.testViewRestore) }

    private val progressIndicator: ProgressLayout by lazy {
        findViewById<ProgressLayout>(R.id.progressIndicator)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.animation_activity)

        testView.setOnClickListener {
//            progressIndicator.alpha = 0.3f
            progressIndicator.startProgress(5_000, object : ProgressLayout.Callbacks {
                override fun onProgressEnd() {
                    Log.d("myLog", "PROGRESS END")
                }

                override fun onProgressCancel() {
                    Log.d("myLog", "PROGRESS CANCEL")
                }

                override fun onProgressStart() {
                    Log.d("myLog", "PROGRESS START")
                }
            })
        }
        progressIndicator.setOnClickListener {
            Log.d("myLog", "CLICK PROGRESSINDICATOR")
            progressIndicator.cancelProgress(false)
        }
        testViewRestore.setOnClickListener {
            progressIndicator.cancelProgress(false)
        }

    }

}

/**
 * Used to guarantee a specific result in expressions
 **/
val <T> T.exhaustive: T
    get() = this