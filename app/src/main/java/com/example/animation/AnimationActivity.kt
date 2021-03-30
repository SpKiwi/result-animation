package com.example.animation

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Job

class AnimationActivity : AppCompatActivity() {

    private val resultView: ResultView by lazy { findViewById<ResultView>(R.id.result_view) }
    private val testView: View by lazy { findViewById<View>(R.id.testView) }

    private var animationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.animation_activity)

        testView.setOnClickListener {
            launchProgressAnimation()
//            ResultView().animate()
        }
    }

    private fun launchProgressAnimation() {
        resultView.changeProgressValues(3_000)
//        animationJob?.cancel()
//        animationJob = lifecycleScope.launch {
//            flow<Int> {
//                var aggregator = 0
//                repeat(100) {
//                    delay(50)
//                    aggregator++
//                    emit(aggregator)
//                }
//            }.collect {
//                progress.progress = it
//            }
//        }
    }

}