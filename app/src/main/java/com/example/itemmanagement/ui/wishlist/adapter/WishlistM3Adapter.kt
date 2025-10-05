package com.example.itemmanagement.ui.wishlist.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.databinding.ItemWishlistSimpleBinding
import com.example.itemmanagement.data.view.WishlistItemView
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority

/**
 * 心愿单物品适配器 - 简化版本
 * 临时实现，用于解决编译错误
 */
class WishlistM3Adapter(
    private val onItemClick: (Long) -> Unit,
    private val onItemLongClick: (Long) -> Boolean,
    private val onPriorityClick: (Long, WishlistPriority) -> Unit,
    private val onPriceClick: (Long, Double?) -> Unit,
    private val onSelectionChanged: (Long, Boolean) -> Unit,
    private val onDeleteItem: (Long) -> Unit,
    private val onMarkPurchased: (Long) -> Unit
) : ListAdapter<WishlistItemView, WishlistM3Adapter.WishlistViewHolder>(WishlistDiffCallback()) {
    
    // === 状态管理 ===
    
    private var isSelectionMode = false
    private var selectedItems = emptySet<Long>()
    
    // === 公共方法 ===
    
    /**
     * 设置选择模式状态
     */
    fun setSelectionMode(enabled: Boolean) {
        if (isSelectionMode != enabled) {
            isSelectionMode = enabled
            notifyDataSetChanged()
        }
    }
    
    /**
     * 更新选中项目
     */
    fun updateSelection(selection: Set<Long>) {
        selectedItems = selection
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        // 临时使用简单的布局
        val binding = ItemWishlistSimpleBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return WishlistViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }
    
    inner class WishlistViewHolder(
        private val binding: ItemWishlistSimpleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: WishlistItemView) {
            with(binding) {
                // 基本信息显示
                textName.text = item.name
                textCategory.text = item.category
                textPrice.text = item.currentPrice?.let { "¥$it" } ?: "价格未知"
                
                // 优先级显示
                textPriority.text = when (item.priority) {
                    WishlistPriority.LOW -> "低"
                    WishlistPriority.NORMAL -> "普通"
                    WishlistPriority.HIGH -> "高"
                    WishlistPriority.URGENT -> "紧急"
                    else -> "普通"
                }
                
                // 点击事件
                root.setOnClickListener {
                    if (isSelectionMode) {
                        val isSelected = item.id in selectedItems
                        onSelectionChanged(item.id, !isSelected)
                    } else {
                        onItemClick(item.id)
                    }
                }
                
                root.setOnLongClickListener {
                    onItemLongClick(item.id)
                }
                
                // 选择状态显示
                checkboxSelect.isChecked = item.id in selectedItems
                checkboxSelect.setOnCheckedChangeListener { _, isChecked ->
                    onSelectionChanged(item.id, isChecked)
                }
            }
        }
    }
}

/**
 * DiffUtil回调
 */
class WishlistDiffCallback : DiffUtil.ItemCallback<WishlistItemView>() {
    override fun areItemsTheSame(oldItem: WishlistItemView, newItem: WishlistItemView): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: WishlistItemView, newItem: WishlistItemView): Boolean {
        return oldItem == newItem
    }
}

