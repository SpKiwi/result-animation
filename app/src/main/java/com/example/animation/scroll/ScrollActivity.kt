package com.example.animation.scroll

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animation.R

class ScrollActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scroll_activity)

        findViewById<RecyclerView>(R.id.scrollRecycler).apply {
            adapter = ScrollAdapter()
            layoutManager = LinearLayoutManager(
                this@ScrollActivity,
                LinearLayoutManager.VERTICAL,
                false
            )
        }

        findViewById<Button>(R.id.scrollButton).apply {

        }

    }
}