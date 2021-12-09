package com.example.animation.recycler

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.animation.R

class RecyclerAdapter : RecyclerView.Adapter<RecyclerAdapter.RecyclerAdapterHolder>() {

    private val elements: MutableList<RecyclerColorItem> = (0..10).map { index ->
        RecyclerColorItem(randomColor(), index)
    }.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapterHolder =
        RecyclerAdapterHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.recycler_item, parent, false)
        )

    override fun onBindViewHolder(holder: RecyclerAdapterHolder, position: Int) {
        holder.bind(elements[position])
    }

    override fun getItemCount(): Int = elements.size

    override fun onViewAttachedToWindow(holder: RecyclerAdapterHolder) {
        super.onViewAttachedToWindow(holder)
//        holder.itemView.setOnTouchListener { v, event ->
//
//        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerAdapterHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.setOnLongClickListener(null)
    }

    fun addItem() {
        val nextIndex = elements.lastIndex + 1
        elements.add(RecyclerColorItem(randomColor(), nextIndex))
        notifyItemInserted(nextIndex)
    }

    class RecyclerAdapterHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textView: TextView = itemView.findViewById(R.id.recyclerItemNumber)

        fun bind(item: RecyclerColorItem) {
            itemView.setBackgroundColor(item.color)
            textView.text = item.index.toString()
        }

    }

}