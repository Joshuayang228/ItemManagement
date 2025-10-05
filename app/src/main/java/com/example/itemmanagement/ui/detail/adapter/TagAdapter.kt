package com.example.itemmanagement.ui.detail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.data.entity.TagEntity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * 简化版标签适配器
 */
class TagAdapter : ListAdapter<TagEntity, TagAdapter.TagViewHolder>(TagDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        // 创建简单的TextView作为标签
        val textView = TextView(parent.context).apply {
            setPadding(16, 8, 16, 8)
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#6200EE"))
            textSize = 12f
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 16, 8) // 右边距和下边距
            }
        }
        
        return TagViewHolder(textView)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TagViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(tag: TagEntity) {
            textView.text = tag.name
            // 如果有颜色信息，可以设置背景色
            try {
                if (!tag.color.isNullOrBlank() && tag.color != "#6200EE") {
                    textView.setBackgroundColor(android.graphics.Color.parseColor(tag.color))
                }
            } catch (e: IllegalArgumentException) {
                // 忽略无效颜色
            }
        }
    }

    companion object {
        private val TagDiffCallback = object : DiffUtil.ItemCallback<TagEntity>() {
            override fun areItemsTheSame(oldItem: TagEntity, newItem: TagEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: TagEntity, newItem: TagEntity): Boolean {
                return oldItem.name == newItem.name && oldItem.color == newItem.color
            }
        }
    }
}