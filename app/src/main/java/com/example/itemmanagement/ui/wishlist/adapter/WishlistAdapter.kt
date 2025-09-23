package com.example.itemmanagement.ui.wishlist.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.databinding.ItemWishlistBinding
import com.example.itemmanagement.ui.utils.Material3Feedback
import java.text.SimpleDateFormat
import java.util.*

/**
 * 心愿单物品适配器
 * 遵循MVVM架构，负责心愿单物品列表的展示
 */
class WishlistAdapter(
    private val onItemClick: (WishlistItemEntity) -> Unit,
    private val onItemLongClick: (WishlistItemEntity) -> Boolean,
    private val onPriorityClick: (WishlistItemEntity) -> Unit,
    private val onPriceClick: (WishlistItemEntity) -> Unit,
    private val onSelectionChanged: (Long, Boolean) -> Unit
) : ListAdapter<WishlistItemEntity, WishlistAdapter.WishlistViewHolder>(WishlistDiffCallback()) {
    
    private var isSelectionMode = false
    private var selectedItems = emptySet<Long>()
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WishlistViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    fun setSelectionMode(isSelectionMode: Boolean) {
        this.isSelectionMode = isSelectionMode
        notifyDataSetChanged()
    }
    
    fun setSelectedItems(selectedItems: Set<Long>) {
        this.selectedItems = selectedItems
        notifyDataSetChanged()
    }
    
    inner class WishlistViewHolder(
        private val binding: ItemWishlistBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: WishlistItemEntity) {
            binding.apply {
                // 基础信息
                textItemName.text = item.getDisplayName()
                textCategory.text = item.category
                textSpecification.text = item.specification ?: ""
                textSpecification.visibility = if (item.specification.isNullOrBlank()) View.GONE else View.VISIBLE
                
                // 价格信息
                bindPriceInfo(item)
                
                // 优先级
                bindPriority(item)
                
                // 状态指示器
                bindStatusIndicators(item)
                
                // 时间信息
                bindTimeInfo(item)
                
                // 选择模式
                bindSelectionMode(item)
                
                // 点击事件
                setupClickListeners(item)
            }
        }
        
        private fun ItemWishlistBinding.bindPriceInfo(item: WishlistItemEntity) {
            // 当前价格
            if (item.currentPrice != null) {
                textCurrentPrice.text = "¥%.0f".format(item.currentPrice)
                textCurrentPrice.visibility = View.VISIBLE
                
                // 价格变化指示
                val changePercentage = item.getPriceChangePercentage()
                if (changePercentage != null) {
                    val changeText = if (changePercentage >= 0) {
                        "+%.1f%%".format(changePercentage)
                    } else {
                        "%.1f%%".format(changePercentage)
                    }
                    textPriceChange.text = changeText
                    textPriceChange.visibility = View.VISIBLE
                    
                    // 设置颜色
                    val color = if (changePercentage < 0) {
                        ContextCompat.getColor(root.context, R.color.success)
                    } else {
                        ContextCompat.getColor(root.context, R.color.error)
                    }
                    textPriceChange.setTextColor(color)
                } else {
                    textPriceChange.visibility = View.GONE
                }
            } else {
                textCurrentPrice.text = if (item.price != null) {
                    "预估 ¥%.0f".format(item.price)
                } else {
                    "未设置价格"
                }
                textCurrentPrice.visibility = View.VISIBLE
                textPriceChange.visibility = View.GONE
            }
            
            // 目标价格
            if (item.targetPrice != null) {
                textTargetPrice.text = "目标 ¥%.0f".format(item.targetPrice)
                textTargetPrice.visibility = View.VISIBLE
            } else {
                textTargetPrice.visibility = View.GONE
            }
        }
        
        private fun ItemWishlistBinding.bindPriority(item: WishlistItemEntity) {
            chipPriority.apply {
                text = item.priority.displayName
                
                // 设置优先级颜色
                val colorResId = when (item.priority) {
                    WishlistPriority.LOW -> R.color.priority_low
                    WishlistPriority.NORMAL -> R.color.priority_normal
                    WishlistPriority.HIGH -> R.color.priority_high
                    WishlistPriority.URGENT -> R.color.priority_urgent
                }
                
                chipBackgroundColor = ContextCompat.getColorStateList(context, colorResId)
                
                // 设置点击事件
                setOnClickListener {
                    Material3Feedback.performHapticFeedback(this)
                    onPriorityClick(item)
                }
            }
        }
        
        private fun ItemWishlistBinding.bindStatusIndicators(item: WishlistItemEntity) {
            // 价格跟踪指示器
            iconPriceTracking.visibility = if (item.isPriceTrackingEnabled) View.VISIBLE else View.GONE
            
            // 价格下降指示器
            if (item.hasPriceDrop()) {
                iconPriceDrop.visibility = View.VISIBLE
                iconPriceDrop.setColorFilter(ContextCompat.getColor(root.context, R.color.success))
            } else {
                iconPriceDrop.visibility = View.GONE
            }
            
            // 达到目标价格指示器
            if (item.hasReachedTargetPrice()) {
                iconTargetReached.visibility = View.VISIBLE
                iconTargetReached.setColorFilter(ContextCompat.getColor(root.context, R.color.success))
            } else {
                iconTargetReached.visibility = View.GONE
            }
            
            // 紧急程度指示器
            if (item.needsImmediateAttention()) {
                iconUrgent.visibility = View.VISIBLE
                iconUrgent.setColorFilter(ContextCompat.getColor(root.context, R.color.error))
            } else {
                iconUrgent.visibility = View.GONE
            }
        }
        
        private fun ItemWishlistBinding.bindTimeInfo(item: WishlistItemEntity) {
            val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
            textAddedDate.text = "添加于 ${dateFormat.format(item.addDate)}"
            
            // 最后价格检查时间
            if (item.lastPriceCheck != null) {
                textLastPriceCheck.text = "价格更新 ${dateFormat.format(item.lastPriceCheck)}"
                textLastPriceCheck.visibility = View.VISIBLE
            } else {
                textLastPriceCheck.visibility = View.GONE
            }
        }
        
        private fun ItemWishlistBinding.bindSelectionMode(item: WishlistItemEntity) {
            if (isSelectionMode) {
                checkboxSelection.visibility = View.VISIBLE
                checkboxSelection.isChecked = selectedItems.contains(item.id)
                
                // 设置卡片选中状态
                root.isSelected = selectedItems.contains(item.id)
                
                checkboxSelection.setOnCheckedChangeListener { _, isChecked ->
                    onSelectionChanged(item.id, isChecked)
                }
            } else {
                checkboxSelection.visibility = View.GONE
                root.isSelected = false
            }
        }
        
        private fun ItemWishlistBinding.setupClickListeners(item: WishlistItemEntity) {
            // 卡片点击
            root.setOnClickListener {
                Material3Feedback.performHapticFeedback(it)
                if (isSelectionMode) {
                    onSelectionChanged(item.id, !selectedItems.contains(item.id))
                } else {
                    onItemClick(item)
                }
            }
            
            // 卡片长按
            root.setOnLongClickListener {
                Material3Feedback.performHapticFeedback(it)
                onItemLongClick(item)
            }
            
            // 价格点击
            layoutPriceInfo.setOnClickListener {
                Material3Feedback.performHapticFeedback(it)
                onPriceClick(item)
            }
            
            // 更多操作按钮
            buttonMore.setOnClickListener {
                Material3Feedback.performHapticFeedback(it)
                showMoreOptionsMenu(item)
            }
        }
        
        private fun showMoreOptionsMenu(item: WishlistItemEntity) {
            // TODO: 实现更多操作菜单
            // 可以包括：编辑、分享、复制链接、标记为已实现等操作
        }
    }
    
    private class WishlistDiffCallback : DiffUtil.ItemCallback<WishlistItemEntity>() {
        override fun areItemsTheSame(oldItem: WishlistItemEntity, newItem: WishlistItemEntity): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: WishlistItemEntity, newItem: WishlistItemEntity): Boolean {
            return oldItem == newItem
        }
    }
}
