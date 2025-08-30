package com.example.itemmanagement.ui.warranty

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R

/**
 * 保修凭证图片适配器
 * 支持显示图片、删除图片和添加新图片
 */
class ReceiptImageAdapter(
    private val onImageClick: (Uri) -> Unit,
    private val onImageDelete: (Uri) -> Unit,
    private val onAddImageClick: () -> Unit
) : ListAdapter<Uri, RecyclerView.ViewHolder>(DIFF_CALLBACK) {

    companion object {
        private const val VIEW_TYPE_IMAGE = 0
        private const val VIEW_TYPE_ADD_BUTTON = 1
        
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Uri>() {
            override fun areItemsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Uri, newItem: Uri): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < super.getItemCount()) VIEW_TYPE_IMAGE else VIEW_TYPE_ADD_BUTTON
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + 1 // +1 for add button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        
        return when (viewType) {
            VIEW_TYPE_IMAGE -> {
                val view = inflater.inflate(R.layout.item_receipt_image, parent, false)
                ImageViewHolder(view, onImageClick, onImageDelete)
            }
            VIEW_TYPE_ADD_BUTTON -> {
                val view = inflater.inflate(R.layout.item_add_receipt_image, parent, false)
                AddButtonViewHolder(view, onAddImageClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageViewHolder -> {
                holder.bind(getItem(position))
            }
            is AddButtonViewHolder -> {
                // Add button doesn't need binding
            }
        }
    }

    /**
     * 图片项ViewHolder
     */
    class ImageViewHolder(
        itemView: View,
        private val onImageClick: (Uri) -> Unit,
        private val onImageDelete: (Uri) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val imageView: ImageView = itemView.findViewById(R.id.receiptImageView)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
        
        fun bind(uri: Uri) {
            // 使用Glide加载图片
            Glide.with(itemView.context)
                .load(uri)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image_24dp)
                .centerCrop()
                .into(imageView)
            
            // 设置点击事件
            imageView.setOnClickListener { onImageClick(uri) }
            deleteButton.setOnClickListener { onImageDelete(uri) }
        }
    }

    /**
     * 添加按钮ViewHolder
     */
    class AddButtonViewHolder(
        itemView: View,
        private val onAddImageClick: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        init {
            itemView.setOnClickListener { onAddImageClick() }
        }
    }
}
