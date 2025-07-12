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

class PhotoAdapter : ListAdapter<PhotoEntity, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    private var onPhotoClickListener: ((PhotoEntity, Int) -> Unit)? = null

    fun setOnPhotoClickListener(listener: (PhotoEntity, Int) -> Unit) {
        onPhotoClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_detail_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = getItem(position)
        holder.bind(photo, position)
    }

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)

        fun bind(photo: PhotoEntity, position: Int) {
            // 使用Glide加载图片
            Glide.with(itemView.context)
                .load(photo.uri)
                .centerCrop()
                .error(R.drawable.ic_error_placeholder)
                .into(photoImageView)

            // 设置点击事件
            itemView.setOnClickListener {
                onPhotoClickListener?.invoke(photo, position)
            }
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<PhotoEntity>() {
        override fun areItemsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean {
            return oldItem.uri == newItem.uri
        }
    }
} 