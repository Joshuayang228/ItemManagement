package com.example.itemmanagement.ui.wishlist.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.databinding.ItemWishlistM3Binding
// import com.example.itemmanagement.ui.utils.Material3Animations.animatePress
import com.example.itemmanagement.ui.utils.Material3Feedback
import com.example.itemmanagement.ui.wishlist.model.PriceStatus
import com.example.itemmanagement.ui.wishlist.model.WishlistItemUiState
import java.text.SimpleDateFormat
import java.util.*

/**
 * 心愿单物品适配器 - Material Design 3版本
 * 遵循MVVM架构，专注于UI数据绑定和用户交互
 * 支持选择模式、价格状态显示等高级功能
 */
class WishlistM3Adapter(
    private val onItemClick: (Long) -> Unit,
    private val onItemLongClick: (Long) -> Boolean,
    private val onPriorityClick: (Long, WishlistPriority) -> Unit,
    private val onPriceClick: (Long, Double?) -> Unit,
    private val onSelectionChanged: (Long, Boolean) -> Unit,
    private val onDeleteItem: (Long) -> Unit,
    private val onMarkPurchased: (Long) -> Unit
) : ListAdapter<WishlistItemEntity, WishlistM3Adapter.WishlistViewHolder>(WishlistDiffCallback()) {
    
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
            if (!enabled) {
                selectedItems = emptySet()
            }
            notifyDataSetChanged()
        }
    }
    
    /**
     * 更新选中项目
     */
    fun updateSelection(newSelection: Set<Long>) {
        val oldSelection = selectedItems
        selectedItems = newSelection
        
        // 只更新状态改变的项目
        val changedItems = (oldSelection - newSelection) + (newSelection - oldSelection)
        changedItems.forEach { itemId ->
            val position = currentList.indexOfFirst { it.id == itemId }
            if (position != -1) {
                notifyItemChanged(position, SelectionPayload(newSelection.contains(itemId)))
            }
        }
    }
    
    // === 适配器核心方法 ===
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistM3Binding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WishlistViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    override fun onBindViewHolder(
        holder: WishlistViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // 处理局部更新
            for (payload in payloads) {
                when (payload) {
                    is SelectionPayload -> holder.updateSelection(payload.isSelected)
                    is PricePayload -> holder.updatePrice(payload.newPrice)
                    is PriorityPayload -> holder.updatePriority(payload.newPriority)
                }
            }
        }
    }
    
    // === ViewHolder实现 ===
    
    inner class WishlistViewHolder(
        private val binding: ItemWishlistM3Binding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())
        private var currentItem: WishlistItemEntity? = null
        
        init {
            setupClickListeners()
        }
        
        fun bind(item: WishlistItemEntity) {
            currentItem = item
            val uiState = WishlistItemUiState(
                item = item,
                isSelected = selectedItems.contains(item.id)
            )
            
            bindBasicInfo(uiState)
            bindPriceInfo(uiState)
            bindPriorityInfo(uiState)
            bindDateInfo(uiState)
            bindImage(uiState)
            bindSelectionState(uiState)
            bindStatusIndicators(uiState)
        }
        
        private fun bindBasicInfo(uiState: WishlistItemUiState) {
            with(binding) {
                textItemName.text = uiState.item.getDisplayName()
                textCategory.text = uiState.item.category
                textDescription.text = uiState.item.specification ?: uiState.item.customNote
                textDescription.isVisible = !textDescription.text.isNullOrBlank()
                
                // 数量信息
                if (uiState.item.quantity != 1.0) {
                    textQuantity.text = "${uiState.item.quantity}${uiState.item.quantityUnit}"
                    textQuantity.isVisible = true
                } else {
                    textQuantity.isVisible = false
                }
            }
        }
        
        private fun bindPriceInfo(uiState: WishlistItemUiState) {
            with(binding) {
                textCurrentPrice.text = uiState.getPriceDisplayText()
                
                // 目标价格
                if (uiState.item.targetPrice != null) {
                    textTargetPrice.text = "目标 ¥${String.format("%.0f", uiState.item.targetPrice)}"
                    textTargetPrice.isVisible = true
                } else {
                    textTargetPrice.isVisible = false
                }
                
                // 价格变化指示器
                val changeText = uiState.getPriceChangeText()
                if (changeText != null && uiState.shouldShowPriceIndicator()) {
                    textPriceChange.text = changeText
                    textPriceChange.isVisible = true
                    
                    // 设置颜色
                    val colorRes = when (uiState.priceStatus) {
                        PriceStatus.PRICE_UP -> R.color.error
                        PriceStatus.PRICE_DOWN, PriceStatus.TARGET_REACHED -> R.color.success
                        else -> R.color.text_secondary
                    }
                    textPriceChange.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))
                } else {
                    textPriceChange.isVisible = false
                }
            }
        }
        
        private fun bindPriorityInfo(uiState: WishlistItemUiState) {
            with(binding) {
                // 优先级指示器
                val priorityColor = android.graphics.Color.parseColor(uiState.item.priority.colorCode)
                priorityIndicator.setBackgroundColor(priorityColor)
                
                // 优先级芯片
                chipPriority.text = uiState.item.priority.displayName
                chipPriority.setChipBackgroundColorResource(
                    when (uiState.item.priority) {
                        WishlistPriority.LOW -> R.color.surface_variant
                        WishlistPriority.NORMAL -> R.color.primary_container
                        WishlistPriority.HIGH -> R.color.orange_container
                        WishlistPriority.URGENT -> R.color.error_container
                    }
                )
                
                // 紧急程度指示器
                if (uiState.item.urgency.level >= 3) {
                    iconUrgent.isVisible = true
                    iconUrgent.setColorFilter(
                        ContextCompat.getColor(binding.root.context, R.color.error)
                    )
                } else {
                    iconUrgent.isVisible = false
                }
            }
        }
        
        private fun bindDateInfo(uiState: WishlistItemUiState) {
            with(binding) {
                textCreatedDate.text = "添加于 ${dateFormatter.format(uiState.item.addDate)}"
                
                // 最后查看时间
                if (uiState.item.lastViewDate != null) {
                    textLastView.text = "查看 ${uiState.item.viewCount} 次"
                    textLastView.isVisible = true
                } else {
                    textLastView.isVisible = false
                }
            }
        }
        
        private fun bindImage(uiState: WishlistItemUiState) {
            if (uiState.item.imageUrl != null) {
                binding.imageItem.isVisible = true
                Glide.with(binding.root.context)
                    .load(uiState.item.imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_placeholder_item)
                    .error(R.drawable.ic_placeholder_item)
                    .centerCrop()
                    .into(binding.imageItem)
            } else {
                binding.imageItem.isVisible = false
            }
        }
        
        private fun bindSelectionState(uiState: WishlistItemUiState) {
            with(binding) {
                checkboxSelection.isVisible = isSelectionMode
                checkboxSelection.isChecked = uiState.isSelected
                
                // 选中状态的视觉反馈
                cardItem.isSelected = uiState.isSelected
                cardItem.alpha = if (uiState.isSelected) 0.8f else 1.0f
            }
        }
        
        private fun bindStatusIndicators(uiState: WishlistItemUiState) {
            with(binding) {
                // 价格跟踪状态
                iconPriceTracking.isVisible = uiState.item.isPriceTrackingEnabled
                
                // 价格提醒状态
                when (uiState.priceStatus) {
                    PriceStatus.TARGET_REACHED -> {
                        chipStatus.text = "已达目标价"
                        chipStatus.setChipBackgroundColorResource(R.color.success_container)
                        chipStatus.isVisible = true
                    }
                    PriceStatus.PRICE_DOWN -> {
                        chipStatus.text = "降价了"
                        chipStatus.setChipBackgroundColorResource(R.color.success_container)
                        chipStatus.isVisible = true
                    }
                    PriceStatus.OVER_BUDGET -> {
                        chipStatus.text = "超出预算"
                        chipStatus.setChipBackgroundColorResource(R.color.warning_container)
                        chipStatus.isVisible = true
                    }
                    else -> {
                        chipStatus.isVisible = false
                    }
                }
                
                // 暂停状态
                if (uiState.item.isPaused) {
                    overlayPaused.isVisible = true
                    textPausedStatus.text = "已暂停跟踪"
                } else {
                    overlayPaused.isVisible = false
                }
            }
        }
        
        // === 局部更新方法 ===
        
        fun updateSelection(isSelected: Boolean) {
            binding.checkboxSelection.isChecked = isSelected
            binding.cardItem.isSelected = isSelected
            binding.cardItem.alpha = if (isSelected) 0.8f else 1.0f
        }
        
        fun updatePrice(newPrice: Double?) {
            currentItem?.let { item ->
                val updatedItem = item.copy(currentPrice = newPrice)
                val uiState = WishlistItemUiState(updatedItem)
                bindPriceInfo(uiState)
                bindStatusIndicators(uiState)
            }
        }
        
        fun updatePriority(newPriority: WishlistPriority) {
            currentItem?.let { item ->
                val updatedItem = item.copy(priority = newPriority)
                val uiState = WishlistItemUiState(updatedItem)
                bindPriorityInfo(uiState)
            }
        }
        
        // === 点击事件设置 ===
        
        private fun setupClickListeners() {
            with(binding) {
                // 主卡片点击
                cardItem.setOnClickListener {
                    // it.animatePress()
                    currentItem?.let { item ->
                        if (isSelectionMode) {
                            toggleSelection(item.id)
                        } else {
                            onItemClick(item.id)
                        }
                    }
                }
                
                // 长按事件
                cardItem.setOnLongClickListener {
                    currentItem?.let { item ->
                        onItemLongClick(item.id)
                    } ?: false
                }
                
                // 优先级芯片点击
                chipPriority.setOnClickListener {
                    // it.animatePress()
                    currentItem?.let { item ->
                        showPrioritySelector(item)
                    }
                }
                
                // 价格点击
                layoutPrice.setOnClickListener {
                    // it.animatePress()
                    currentItem?.let { item ->
                        onPriceClick(item.id, item.currentPrice)
                    }
                }
                
                // 选择框点击
                checkboxSelection.setOnClickListener {
                    currentItem?.let { item ->
                        toggleSelection(item.id)
                    }
                }
                
                // 快速操作按钮
                buttonQuickAction.setOnClickListener {
                    // it.animatePress()
                    currentItem?.let { item ->
                        showQuickActions(item)
                    }
                }
            }
        }
        
        private fun toggleSelection(itemId: Long) {
            val isCurrentlySelected = selectedItems.contains(itemId)
            onSelectionChanged(itemId, !isCurrentlySelected)
        }
        
        private fun showPrioritySelector(item: WishlistItemEntity) {
            val priorities = WishlistPriority.values()
            val currentIndex = priorities.indexOf(item.priority)
            
            // 循环到下一个优先级
            val nextIndex = (currentIndex + 1) % priorities.size
            val nextPriority = priorities[nextIndex]
            
            onPriorityClick(item.id, nextPriority)
            
            // 显示反馈
            Material3Feedback.showInfo(
                binding.root,
                "优先级已更改为 ${nextPriority.displayName}"
            )
        }
        
        private fun showQuickActions(item: WishlistItemEntity) {
            val popup = androidx.appcompat.widget.PopupMenu(binding.root.context, binding.buttonQuickAction)
            popup.menuInflater.inflate(R.menu.menu_wishlist_item_quick_actions, popup.menu)
            
            // 设置菜单项点击监听
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_mark_purchased -> {
                        showMarkPurchasedConfirmation(item)
                        true
                    }
                    R.id.action_delete -> {
                        showDeleteConfirmation(item)
                        true
                    }
                    else -> false
                }
            }
            
            popup.show()
        }
        
        private fun showMarkPurchasedConfirmation(item: WishlistItemEntity) {
            androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                .setTitle("标记为已购买")
                .setMessage("将「${item.name}」标记为已购买？")
                .setPositiveButton("确认") { _, _ ->
                    onMarkPurchased(item.id)
                }
                .setNegativeButton("取消", null)
                .show()
        }
        
        private fun showDeleteConfirmation(item: WishlistItemEntity) {
            androidx.appcompat.app.AlertDialog.Builder(binding.root.context)
                .setTitle("删除心愿单物品")
                .setMessage("确定要从心愿单中删除「${item.name}」吗？")
                .setPositiveButton("删除") { _, _ ->
                    onDeleteItem(item.id)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
}

/**
 * DiffUtil回调，用于高效列表更新
 */
private class WishlistDiffCallback : DiffUtil.ItemCallback<WishlistItemEntity>() {
    override fun areItemsTheSame(oldItem: WishlistItemEntity, newItem: WishlistItemEntity): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: WishlistItemEntity, newItem: WishlistItemEntity): Boolean {
        return oldItem == newItem
    }
    
    override fun getChangePayload(oldItem: WishlistItemEntity, newItem: WishlistItemEntity): Any? {
        return when {
            oldItem.currentPrice != newItem.currentPrice -> PricePayload(newItem.currentPrice)
            oldItem.priority != newItem.priority -> PriorityPayload(newItem.priority)
            else -> null
        }
    }
}

/**
 * 局部更新的Payload类
 */
private data class SelectionPayload(val isSelected: Boolean)
private data class PricePayload(val newPrice: Double?)
private data class PriorityPayload(val newPriority: WishlistPriority)
