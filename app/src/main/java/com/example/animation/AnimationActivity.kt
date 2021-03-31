package com.example.animation

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.animation.animation.AutoFollowLayout

class AnimationActivity : AppCompatActivity() {

    private val autoFollowLayout: AutoFollowLayout by lazy { findViewById<AutoFollowLayout>(R.id.animationGroup) }
    private val testView: View by lazy { findViewById<View>(R.id.testView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.animation_activity)

        testView.setOnClickListener {
            launchProgressAnimation()
        }
    }

    private fun launchProgressAnimation() {
        autoFollowLayout.startAutoFollowAnimation(5_000)
    }

}