package com.example.itemmanagement.ui.detail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.PhotoEntity

/**
 * 简化版照片适配器
 */
class PhotoAdapter(
    private val onPhotoClick: ((photoUri: String, position: Int) -> Unit)? = null
) : ListAdapter<PhotoEntity, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        
        // 创建简单的ImageView
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        
        return PhotoViewHolder(imageView, onPhotoClick)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class PhotoViewHolder(
        private val imageView: ImageView,
        private val onPhotoClick: ((photoUri: String, position: Int) -> Unit)?
    ) : RecyclerView.ViewHolder(imageView) {
        fun bind(photo: PhotoEntity, position: Int) {
            Glide.with(imageView.context)
                .load(photo.uri)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(imageView)
            
            // 设置点击事件
            imageView.setOnClickListener {
                onPhotoClick?.invoke(photo.uri, position)
            }
        }
    }

    companion object {
        private val PhotoDiffCallback = object : DiffUtil.ItemCallback<PhotoEntity>() {
            override fun areItemsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
                return oldItem.uri == newItem.uri
            }
        }
    }
}