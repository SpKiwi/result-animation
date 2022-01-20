package com.example.animation.cursor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.animation.R

class CursorAdapter : RecyclerView.Adapter<CursorAdapter.CursorAdapterHolder>() {

    var elements: List<CursorItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CursorAdapterHolder =
        CursorAdapterHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.image_item, parent, false)
        )

    override fun onBindViewHolder(holder: CursorAdapterHolder, position: Int) {
        holder.bind(elements[position])
    }

    override fun getItemCount(): Int = elements.size

    class CursorAdapterHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val imageView: ImageView = itemView.findViewById(R.id.recyclerImageItem)

        fun bind(item: CursorItem) {
            Glide.with(imageView).load(item.uri).into(imageView)
        }

    }

}