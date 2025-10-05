package com.example.itemmanagement.adapter

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.UrgencyLevel
import com.example.itemmanagement.data.model.Item
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

/**
 * 购物清单适配器 - Material 3设计
 */
class ShoppingListAdapter(
    private val onItemClick: (Item) -> Unit,
    private val onRecordPrice: (Item) -> Unit,
    private val onMarkPurchased: (Item) -> Unit,
    private val onTransferToInventory: (Item) -> Unit,
    private val onDelete: (Item) -> Unit
) : ListAdapter<Item, ShoppingListAdapter.ShoppingItemViewHolder>(DiffCallback) {
    
    private var onListClickListener: ((Long) -> Unit)? = null
    
    // 已购买物品的起始位置（用于显示分割线）
    private var purchasedStartPosition: Int = -1
    
    fun setOnListClickListener(listener: (Long) -> Unit) {
        onListClickListener = listener
    }
    
    /**
     * 设置已购买物品的起始位置
     */
    fun setPurchasedStartPosition(position: Int) {
        purchasedStartPosition = position
    }
    
    /**
     * 获取已购买物品的起始位置
     */
    fun getPurchasedStartPosition(): Int = purchasedStartPosition

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Item>() {
            override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_item, parent, false)
        return ShoppingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShoppingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 卡片视图
        private val cardView: MaterialCardView = itemView as MaterialCardView
        
        // 卡片内部视图
        // 物品图片
        private val itemImage: com.google.android.material.imageview.ShapeableImageView = 
            cardView.findViewById(R.id.itemImage)
        
        // 物品基本信息
        private val itemName: TextView = cardView.findViewById(R.id.itemName)
        private val itemQuantityAndPrice: TextView = cardView.findViewById(R.id.itemQuantityAndPrice)
        
        // Chip组
        private val urgencyChip: Chip = cardView.findViewById(R.id.urgencyChip)
        private val priorityChip: Chip = cardView.findViewById(R.id.priorityChip)
        
        // 分类和商店文本
        private val categoryAndStoreText: TextView = cardView.findViewById(R.id.categoryAndStoreText)
        
        // 备注
        private val itemNotes: TextView = cardView.findViewById(R.id.itemNotes)
        
        // 菜单按钮
        private val menuButton: android.widget.ImageButton = cardView.findViewById(R.id.menuButton)

        fun bind(item: Item) {
            val shoppingDetail = item.shoppingDetail
            if (shoppingDetail == null) {
                itemView.visibility = View.GONE
                return
            }
            
            itemView.visibility = View.VISIBLE
            
            // 1. 加载图片（如果有）
            if (item.photos.isNotEmpty()) {
                val firstPhoto = item.photos.first()
                try {
                    val uri = android.net.Uri.parse(firstPhoto.uri)
                    itemImage.setImageURI(uri)
                    itemImage.visibility = View.VISIBLE
                } catch (e: Exception) {
                    android.util.Log.w("ShoppingListAdapter", "加载图片失败: ${e.message}")
                    itemImage.setImageResource(R.drawable.ic_image_placeholder)
                    itemImage.visibility = View.VISIBLE
                }
            } else {
                itemImage.visibility = View.GONE
            }
            
            // 2. 设置基本信息
            itemName.text = item.name
            
            // 3. 设置数量和价格（不带小数点）
            val quantityText = "数量：${shoppingDetail.quantity.toInt()} ${shoppingDetail.quantityUnit}"
            val priceText = if (shoppingDetail.estimatedPrice != null) {
                "预估：¥${shoppingDetail.estimatedPrice.toInt()}"
            } else {
                "预估：未设置"
            }
            itemQuantityAndPrice.text = "$quantityText | $priceText"
            
            // 4. 设置分类和商店文本（合并在同一行）
            val categoryPart = if (!item.category.isNullOrBlank()) {
                if (!item.subCategory.isNullOrBlank()) {
                    "分类：${item.category} - ${item.subCategory}"
                } else {
                    "分类：${item.category}"
                }
            } else {
                null
            }
            
            val storePart = if (!shoppingDetail.storeName.isNullOrBlank()) {
                "商店：${shoppingDetail.storeName}"
            } else {
                null
            }
            
            // 组合分类和商店信息
            when {
                categoryPart != null && storePart != null -> {
                    categoryAndStoreText.text = "$categoryPart | $storePart"
                    categoryAndStoreText.visibility = View.VISIBLE
                }
                categoryPart != null -> {
                    categoryAndStoreText.text = categoryPart
                    categoryAndStoreText.visibility = View.VISIBLE
                }
                storePart != null -> {
                    categoryAndStoreText.text = storePart
                    categoryAndStoreText.visibility = View.VISIBLE
                }
                else -> {
                    categoryAndStoreText.visibility = View.GONE
                }
            }
            
            // 5. 设置紧急程度Chip
            setUrgencyChip(shoppingDetail.urgencyLevel)
            
            // 6. 设置重要程度Chip
            setPriorityChip(shoppingDetail.getOverallPriority())
            
            // 7. 设置备注
            if (!item.customNote.isNullOrBlank()) {
                itemNotes.text = item.customNote
                itemNotes.visibility = View.VISIBLE
            } else {
                itemNotes.visibility = View.GONE
            }
            
            // 8. 设置购买状态样式（删除线效果）
            updatePurchaseStatus(shoppingDetail.isPurchased)
            
            // 9. 卡片点击事件 - 进入详情页
            cardView.setOnClickListener {
                onItemClick(item)
            }
            
            // 10. 菜单按钮点击事件
            menuButton.setOnClickListener { view ->
                showPopupMenu(view, item)
            }
        }
        
        /**
         * 显示弹出菜单
         */
        private fun showPopupMenu(view: View, item: Item) {
            val popup = androidx.appcompat.widget.PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.menu_shopping_item, popup.menu)
            
            // 根据购买状态动态设置菜单文本
            val markPurchasedItem = popup.menu.findItem(R.id.action_mark_purchased)
            if (item.shoppingDetail?.isPurchased == true) {
                markPurchasedItem?.title = view.context.getString(R.string.unmark_as_purchased)
            } else {
                markPurchasedItem?.title = view.context.getString(R.string.mark_as_purchased)
            }
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_record_price -> {
                        onRecordPrice(item)
                        true
                    }
                    R.id.action_mark_purchased -> {
                        onMarkPurchased(item)
                        true
                    }
                    R.id.action_transfer_to_inventory -> {
                        onTransferToInventory(item)
                        true
                    }
                    R.id.action_delete -> {
                        onDelete(item)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        
        /**
         * 设置重要程度Chip
         */
        private fun setPriorityChip(overallPriority: Int) {
            when {
                overallPriority >= 9 -> {
                    // 关键：使用Error色
                    priorityChip.text = "关键"
                    priorityChip.chipBackgroundColor = ColorStateList.valueOf(
                        getColorFromAttr(com.google.android.material.R.attr.colorError)
                    )
                    priorityChip.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnError))
                    priorityChip.visibility = View.VISIBLE
                }
                overallPriority >= 7 -> {
                    // 重要：使用Primary色
                    priorityChip.text = "重要"
                    priorityChip.chipBackgroundColor = ColorStateList.valueOf(
                        getColorFromAttr(com.google.android.material.R.attr.colorPrimary)
                    )
                    priorityChip.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnPrimary))
                    priorityChip.visibility = View.VISIBLE
                }
                overallPriority >= 5 -> {
                    // 一般：使用TertiaryContainer色
                    priorityChip.text = "一般"
                    priorityChip.chipBackgroundColor = ColorStateList.valueOf(
                        getColorFromAttr(com.google.android.material.R.attr.colorTertiaryContainer)
                    )
                    priorityChip.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnTertiaryContainer))
                    priorityChip.visibility = View.VISIBLE
                }
                else -> {
                    // 次要：不显示
                    priorityChip.visibility = View.GONE
                }
            }
        }

        /**
         * 设置紧急程度Chip (使用M3语义化颜色)
         * 优化：显示所有非"不急"级别，突出紧急物品
         */
        private fun setUrgencyChip(urgencyLevel: com.example.itemmanagement.data.entity.UrgencyLevel) {
            android.util.Log.d("ShoppingAdapter", "setUrgencyChip被调用: $urgencyLevel")
            
            // 显示"一般"、"紧急"、"立即"三个级别
            if (urgencyLevel == UrgencyLevel.NORMAL || urgencyLevel == UrgencyLevel.URGENT || urgencyLevel == UrgencyLevel.CRITICAL) {
                urgencyChip.text = urgencyLevel.displayName
                urgencyChip.visibility = View.VISIBLE
                
                android.util.Log.d("ShoppingAdapter", "显示紧急Chip: ${urgencyLevel.displayName}")
                
                // 根据紧急程度设置颜色 (柔和背景色 + 白字/深字)
                when (urgencyLevel) {
                    UrgencyLevel.CRITICAL -> {
                        // 立即：使用Error (深色背景 + 白字，最突出)
                        urgencyChip.chipBackgroundColor = ColorStateList.valueOf(
                            getColorFromAttr(com.google.android.material.R.attr.colorError)
                        )
                        urgencyChip.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnError))
                        urgencyChip.chipIconTint = ColorStateList.valueOf(
                            getColorFromAttr(com.google.android.material.R.attr.colorOnError)
                        )
                    }
                    UrgencyLevel.URGENT -> {
                        // 紧急：使用ErrorContainer (柔和红色背景 + 深红字)
                        urgencyChip.chipBackgroundColor = ColorStateList.valueOf(
                            getColorFromAttr(com.google.android.material.R.attr.colorErrorContainer)
                        )
                        urgencyChip.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnErrorContainer))
                        urgencyChip.chipIconTint = ColorStateList.valueOf(
                            getColorFromAttr(com.google.android.material.R.attr.colorOnErrorContainer)
                        )
                    }
                    UrgencyLevel.NORMAL -> {
                        // 一般：使用TertiaryContainer (中性色调)
                        urgencyChip.chipBackgroundColor = ColorStateList.valueOf(
                            getColorFromAttr(com.google.android.material.R.attr.colorTertiaryContainer)
                        )
                        urgencyChip.setTextColor(getColorFromAttr(com.google.android.material.R.attr.colorOnTertiaryContainer))
                        urgencyChip.chipIconTint = ColorStateList.valueOf(
                            getColorFromAttr(com.google.android.material.R.attr.colorOnTertiaryContainer)
                        )
                    }
                    else -> {}
                }
            } else {
                // 不急：不显示Chip
                urgencyChip.visibility = View.GONE
                android.util.Log.d("ShoppingAdapter", "隐藏紧急Chip (不急级别)")
            }
        }

        /**
         * 更新购买状态的UI样式
         * 已购买物品显示删除线并降低透明度
         */
        private fun updatePurchaseStatus(isPurchased: Boolean) {
            if (isPurchased) {
                // 已购买：添加删除线、降低透明度
                itemName.paintFlags = itemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemQuantityAndPrice.paintFlags = itemQuantityAndPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                categoryAndStoreText.paintFlags = categoryAndStoreText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemNotes.paintFlags = itemNotes.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                
                // 降低整体透明度，营造"已完成"的氛围
                cardView.alpha = 0.6f
            } else {
                // 待购买：移除删除线、恢复透明度
                itemName.paintFlags = itemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemQuantityAndPrice.paintFlags = itemQuantityAndPrice.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                categoryAndStoreText.paintFlags = categoryAndStoreText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemNotes.paintFlags = itemNotes.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                
                // 恢复透明度
                cardView.alpha = 1.0f
            }
        }
        
        /**
         * 从主题属性获取颜色
         */
        private fun getColorFromAttr(attr: Int): Int {
            val typedValue = android.util.TypedValue()
            itemView.context.theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }
    }
}
