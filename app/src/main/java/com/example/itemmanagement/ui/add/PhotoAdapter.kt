package com.example.itemmanagement.ui.add

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.ItemPhotoBinding
import com.example.itemmanagement.databinding.ItemAddPhotoBinding
import com.example.itemmanagement.utils.SnackbarHelper
import android.view.View

/**
 * 新架构的照片适配器
 * 为新架构设计，功能与旧版本完全一致，但更加简洁和独立
 */
class PhotoAdapter(context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val MAX_PHOTOS = 9 // 最大照片数量限制
        private const val VIEW_TYPE_PHOTO = 0
        private const val VIEW_TYPE_ADD = 1
    }

    private val photos = mutableListOf<Uri>()
    private var onDeleteClickListener: ((Int) -> Unit)? = null
    private var onAddPhotoClickListener: (() -> Unit)? = null
    private var onPhotoClickListener: ((Uri, Int) -> Unit)? = null
    private var itemSize: Int = calculateInitialSize(context)
    
    /**
     * 计算初始照片尺寸，避免加载时的闪烁
     */
    private fun calculateInitialSize(context: Context): Int {
        val screenWidth = context.resources.displayMetrics.widthPixels
        // 实际padding: 外层16dp + card内8dp + recycler 8dp = 32dp，两边 = 64dp
        // 再加上一点点余量让初始尺寸稍微小一点
        val horizontalPadding = (32 * 2 + 3) * context.resources.displayMetrics.density.toInt() // 64dp + 3dp余量
        val spanCount = 3
        val spacing = context.resources.getDimensionPixelSize(R.dimen.photo_grid_spacing)
        val totalSpacing = (spanCount - 1) * spacing
        return (screenWidth - horizontalPadding - totalSpacing) / spanCount
    }

    fun setItemSize(size: Int) {
        itemSize = size
        notifyDataSetChanged()
    }

    fun setPhotos(newPhotos: List<Uri>) {
        photos.clear()
        photos.addAll(newPhotos.take(MAX_PHOTOS))
        notifyDataSetChanged()
    }

    fun addPhoto(uri: Uri) {
        if (photos.size >= MAX_PHOTOS || photos.contains(uri)) {
            return
        }
        photos.add(uri)
        notifyDataSetChanged()
    }

    fun removePhoto(position: Int) {
        if (position in 0 until photos.size) {
            photos.removeAt(position)
            notifyDataSetChanged()
        }
    }

    /**
     * 移动照片位置（用于拖动排序）
     */
    fun movePhoto(fromPosition: Int, toPosition: Int) {
        if (fromPosition in 0 until photos.size && toPosition in 0 until photos.size) {
            val photo = photos.removeAt(fromPosition)
            photos.add(toPosition, photo)
            notifyItemMoved(fromPosition, toPosition)
        }
    }

    fun getPhotos(): List<Uri> = photos.toList()

    /**
     * 清除所有照片
     */
    fun clearPhotos() {
        photos.clear()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < photos.size) VIEW_TYPE_PHOTO else VIEW_TYPE_ADD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_PHOTO -> {
                val binding = ItemPhotoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                PhotoViewHolder(binding)
            }
            VIEW_TYPE_ADD -> {
                val binding = ItemAddPhotoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AddPhotoViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // 设置item的高度等于宽度
        holder.itemView.layoutParams = holder.itemView.layoutParams.apply {
            height = itemSize
        }

        when (holder) {
            is PhotoViewHolder -> {
                val uri = photos[position]
                try {
                    // 使用Glide加载图片
                    Glide.with(holder.binding.root.context)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_error)
                        .centerCrop()
                        .addListener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                holder.binding.root.post {
                                    SnackbarHelper.showError(holder.binding.root, "无法加载图片")
                                }
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }
                        })
                        .into(holder.binding.photoImageView)

                } catch (e: Exception) {
                    holder.binding.photoImageView.setImageResource(R.drawable.ic_image_error)
                    holder.binding.root.post {
                        SnackbarHelper.showError(holder.binding.root, "加载图片时出错")
                    }
                }

                // 设置删除按钮点击事件
                holder.binding.deletePhotoButton.setOnClickListener {
                    val adapterPosition = holder.adapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onDeleteClickListener?.invoke(adapterPosition)
                    }
                }

                // 设置照片点击事件
                holder.binding.root.setOnClickListener {
                    val adapterPosition = holder.adapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onPhotoClickListener?.invoke(uri, adapterPosition)
                    }
                }
            }
            is AddPhotoViewHolder -> {
                // 设置添加按钮点击事件
                holder.binding.root.setOnClickListener {
                    if (photos.size < MAX_PHOTOS) {
                        onAddPhotoClickListener?.invoke()
                    } else {
                        SnackbarHelper.show(holder.binding.root, "最多只能添加${MAX_PHOTOS}张照片")
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (photos.size < MAX_PHOTOS) photos.size + 1 else photos.size
    }

    fun setOnDeleteClickListener(listener: (Int) -> Unit) {
        onDeleteClickListener = listener
    }

    fun setOnAddPhotoClickListener(listener: () -> Unit) {
        onAddPhotoClickListener = listener
    }

    fun setOnPhotoClickListener(listener: (Uri, Int) -> Unit) {
        onPhotoClickListener = listener
    }

    // ViewHolder classes
    inner class PhotoViewHolder(val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root)
    inner class AddPhotoViewHolder(val binding: ItemAddPhotoBinding) : RecyclerView.ViewHolder(binding.root)
}
