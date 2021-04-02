package com.example.animation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.animation.animation.AutoFollowGroup

class AnimationActivity : AppCompatActivity() {

    private val autoFollowGroup: AutoFollowGroup by lazy { findViewById<AutoFollowGroup>(R.id.animationGroup) }
    private val testView: View by lazy { findViewById<View>(R.id.testView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.animation_activity)

        testView.setOnClickListener {
            launchProgressAnimation()
        }
    }

    private fun launchProgressAnimation() {
        autoFollowGroup.startAutoFollowAnimation(5_000, object : AutoFollowGroup.AutofollowListener {
            override fun onAutofollowTimerElapsed() {
                Toast.makeText(this@AnimationActivity, "Time elapsed", Toast.LENGTH_SHORT).show()
            }

            override fun onAutofollowEnd() {
                Toast.makeText(this@AnimationActivity, "Autofollow end", Toast.LENGTH_SHORT).show()
            }
        })
    }

}