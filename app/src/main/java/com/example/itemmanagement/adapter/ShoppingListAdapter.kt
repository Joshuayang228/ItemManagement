package com.example.itemmanagement.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.ShoppingItemEntity
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class ShoppingListAdapter(
    private val onItemChecked: (ShoppingItemEntity, Boolean) -> Unit,
    private val onItemDelete: (ShoppingItemEntity) -> Unit,
    private val onItemAddToInventory: (ShoppingItemEntity) -> Unit
) : ListAdapter<ShoppingItemEntity, ShoppingListAdapter.ShoppingItemViewHolder>(DiffCallback) {

    private var onListClickListener: ((Long) -> Unit)? = null

    fun setOnListClickListener(listener: (Long) -> Unit) {
        onListClickListener = listener
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ShoppingItemEntity>() {
            override fun areItemsTheSame(oldItem: ShoppingItemEntity, newItem: ShoppingItemEntity): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ShoppingItemEntity, newItem: ShoppingItemEntity): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_item_m3, parent, false)
        return ShoppingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShoppingItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val checkboxPurchased: CheckBox = itemView.findViewById(R.id.checkboxPurchased)
        private val itemName: TextView = itemView.findViewById(R.id.itemName)
        private val itemQuantity: TextView = itemView.findViewById(R.id.itemQuantity)
        private val categoryChip: Chip = itemView.findViewById(R.id.categoryChip)
        private val brandChip: Chip = itemView.findViewById(R.id.brandChip)
        private val itemNotes: TextView = itemView.findViewById(R.id.itemNotes)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
        private val buttonAddToInventory: MaterialButton = itemView.findViewById(R.id.buttonAddToInventory)

        fun bind(item: ShoppingItemEntity) {
            // 设置基本信息
            itemName.text = item.name
            itemQuantity.text = "${item.quantity}"
            
            // 设置备注（如果有的话）
            if (!item.customNote.isNullOrBlank()) {
                itemNotes.text = "备注：${item.customNote}"
                itemNotes.visibility = View.VISIBLE
            } else {
                itemNotes.visibility = View.GONE
            }

            // 设置分类Chip - M3新设计
            if (!item.category.isNullOrBlank()) {
                categoryChip.text = item.category
                categoryChip.visibility = View.VISIBLE
            } else {
                categoryChip.visibility = View.GONE
            }
            
            // 设置品牌Chip - M3新设计
            if (!item.brand.isNullOrBlank()) {
                brandChip.text = item.brand
                brandChip.visibility = View.VISIBLE
            } else {
                brandChip.visibility = View.GONE
            }

            // 设置购买状态
            checkboxPurchased.isChecked = item.isPurchased
            
            // 根据购买状态设置UI样式
            updatePurchaseStatus(item.isPurchased)

            // 设置事件监听器
            checkboxPurchased.setOnCheckedChangeListener { _, isChecked ->
                onItemChecked(item, isChecked)
                updatePurchaseStatus(isChecked)
            }

            // 删除按钮
            buttonDelete.setOnClickListener {
                onItemDelete(item)
            }

            // 添加入库按钮（只在已购买时显示）
            if (item.isPurchased) {
                buttonAddToInventory.visibility = View.VISIBLE
                buttonAddToInventory.setOnClickListener {
                    onItemAddToInventory(item)
                }
            } else {
                buttonAddToInventory.visibility = View.GONE
            }

            // 长按编辑功能（可选，后续实现）
            itemView.setOnLongClickListener {
                // TODO: 实现编辑功能
                true
            }
        }

        private fun updatePurchaseStatus(isPurchased: Boolean) {
            if (isPurchased) {
                // 已购买状态：添加删除线、变灰
                itemName.paintFlags = itemName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                itemName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                itemQuantity.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                itemView.alpha = 0.7f
            } else {
                // 待购买状态：移除删除线、恢复颜色
                itemName.paintFlags = itemName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                itemName.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                itemQuantity.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                itemView.alpha = 1.0f
            }
        }
    }
} 