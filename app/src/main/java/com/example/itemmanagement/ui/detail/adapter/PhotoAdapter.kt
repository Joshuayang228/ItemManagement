package com.example.itemmanagement.ui.detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.Photo
import com.example.itemmanagement.databinding.ItemDetailPhotoBinding

class PhotoAdapter : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {
    
    private var photos: List<Photo> = emptyList()
    private var onPhotoClickListener: ((Photo) -> Unit)? = null
    
    fun submitList(newPhotos: List<Photo>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
    
    fun setOnPhotoClickListener(listener: (Photo) -> Unit) {
        onPhotoClickListener = listener
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemDetailPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }
    
    override fun getItemCount(): Int = photos.size
    
    inner class PhotoViewHolder(private val binding: ItemDetailPhotoBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(photo: Photo) {
            // 加载图片
            Glide.with(binding.root.context)
                .load(photo.uri)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .centerCrop()
                .into(binding.photoImageView)
                
            // 设置点击事件
            binding.root.setOnClickListener {
                onPhotoClickListener?.invoke(photo)
            }
        }
    }
} 