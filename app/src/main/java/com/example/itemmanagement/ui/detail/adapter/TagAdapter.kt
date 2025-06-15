package com.example.itemmanagement.ui.detail.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.data.model.Tag
import com.example.itemmanagement.databinding.ItemTagBinding
import android.content.res.ColorStateList

class TagAdapter : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {
    
    private var tags: List<Tag> = emptyList()
    private var onTagClickListener: ((Tag) -> Unit)? = null
    
    fun submitList(newTags: List<Tag>) {
        tags = newTags
        notifyDataSetChanged()
    }
    
    fun setOnTagClickListener(listener: (Tag) -> Unit) {
        onTagClickListener = listener
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = ItemTagBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(tags[position])
    }
    
    override fun getItemCount(): Int = tags.size
    
    inner class TagViewHolder(private val binding: ItemTagBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(tag: Tag) {
            binding.tagChip.text = tag.name
            
            // 设置标签颜色（如果有）
            tag.color?.let { colorString ->
                try {
                    val color = Color.parseColor(colorString)
                    binding.tagChip.chipBackgroundColor = ColorStateList.valueOf(color)
                } catch (e: Exception) {
                    // 使用默认颜色
                }
            }
            
            // 设置点击事件
            binding.tagChip.setOnClickListener {
                onTagClickListener?.invoke(tag)
            }
        }
    }
} 