package com.example.itemmanagement.ui.detail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.TagEntity

class TagAdapter : ListAdapter<TagEntity, TagAdapter.TagViewHolder>(TagDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag_detail, parent, false)
        return TagViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = getItem(position)
        holder.bind(tag)
    }

    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tagText: TextView = itemView.findViewById(R.id.tagText)

        fun bind(tag: TagEntity) {
            tagText.text = tag.name
        }
    }

    class TagDiffCallback : DiffUtil.ItemCallback<TagEntity>() {
        override fun areItemsTheSame(oldItem: TagEntity, newItem: TagEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TagEntity, newItem: TagEntity): Boolean {
            return oldItem == newItem
        }
    }
} 