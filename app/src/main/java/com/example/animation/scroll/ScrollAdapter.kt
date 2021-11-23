package com.example.animation.scroll

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.animation.R

class ScrollAdapter : RecyclerView.Adapter<ScrollAdapter.ScrollHolder>() {

    private val elements: List<Int> = listOf(1)// (1..3).toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScrollHolder =
        ScrollHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.scroll_item, parent, false)
        )

    override fun onBindViewHolder(holder: ScrollHolder, position: Int) {

    }

    override fun getItemCount(): Int = elements.size

    class ScrollHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

}