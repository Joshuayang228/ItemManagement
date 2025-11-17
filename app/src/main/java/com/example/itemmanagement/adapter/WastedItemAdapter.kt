package com.example.itemmanagement.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.WastedItemData
import com.example.itemmanagement.data.model.WasteReason
import com.example.itemmanagement.databinding.ItemWastedItemBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class WastedItemAdapter(
    private val onItemClick: (WastedItemData) -> Unit = {}
) : ListAdapter<WastedItemData, WastedItemAdapter.WastedItemViewHolder>(WastedItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WastedItemViewHolder {
        val binding = ItemWastedItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WastedItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WastedItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WastedItemViewHolder(
        private val binding: ItemWastedItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
        }

        fun bind(item: WastedItemData) {
            with(binding) {
                // 设置物品名称
                textViewItemName.text = item.name

                // 设置物品详情（类别 + 数量单位）
                // 只有当用户输入过数量时才显示数量
                val detailText = if (item.isQuantityUserInput) {
                    "${item.category} • ${item.quantity}${item.unit}"
                } else {
                    item.category
                }
                textViewItemDetails.text = detailText

                // 设置浪费原因
                when (item.wasteReason) {
                    WasteReason.EXPIRED -> {
                        textViewWasteReason.text = "已过期"
                        textViewWasteReason.setBackgroundResource(R.drawable.bg_detail_tag)
                        textViewWasteReason.setBackgroundTintList(
                            binding.root.context.getColorStateList(R.color.orange_container)
                        )
                        textViewWasteReason.setTextColor(
                            binding.root.context.getColor(R.color.on_orange_container)
                        )
                    }
                    WasteReason.DAMAGED -> {
                        textViewWasteReason.text = "已损坏"
                        textViewWasteReason.setBackgroundResource(R.drawable.bg_detail_tag)
                        textViewWasteReason.setBackgroundTintList(
                            binding.root.context.getColorStateList(R.color.error_container)
                        )
                        textViewWasteReason.setTextColor(
                            binding.root.context.getColor(R.color.on_error_container)
                        )
                    }
                    WasteReason.UNUSED -> {
                        textViewWasteReason.text = "未使用"
                        textViewWasteReason.setBackgroundResource(R.drawable.bg_detail_tag)
                        textViewWasteReason.setBackgroundTintList(
                            binding.root.context.getColorStateList(R.color.surface_variant)
                        )
                        textViewWasteReason.setTextColor(
                            binding.root.context.getColor(R.color.on_surface_variant)
                        )
                    }
                    WasteReason.SPOILED -> {
                        textViewWasteReason.text = "已变质"
                        textViewWasteReason.setBackgroundResource(R.drawable.bg_detail_tag)
                        textViewWasteReason.setBackgroundTintList(
                            binding.root.context.getColorStateList(R.color.error_container)
                        )
                        textViewWasteReason.setTextColor(
                            binding.root.context.getColor(R.color.on_error_container)
                        )
                    }
                    WasteReason.BROKEN -> {
                        textViewWasteReason.text = "已破损"
                        textViewWasteReason.setBackgroundResource(R.drawable.bg_detail_tag)
                        textViewWasteReason.setBackgroundTintList(
                            binding.root.context.getColorStateList(R.color.error_container)
                        )
                        textViewWasteReason.setTextColor(
                            binding.root.context.getColor(R.color.on_error_container)
                        )
                    }
                    WasteReason.OTHER -> {
                        textViewWasteReason.text = "其他原因"
                        textViewWasteReason.setBackgroundResource(R.drawable.bg_detail_tag)
                        textViewWasteReason.setBackgroundTintList(
                            binding.root.context.getColorStateList(R.color.surface_variant)
                        )
                        textViewWasteReason.setTextColor(
                            binding.root.context.getColor(R.color.on_surface_variant)
                        )
                    }
                }

                // 设置浪费日期
                textViewWasteDate.text = dateFormat.format(item.wasteDate)

                // 设置价值
                if (item.originalValue > 0) {
                    textViewValue.text = currencyFormat.format(item.originalValue)
                } else {
                    textViewValue.text = "未知"
                }

                // 加载物品图片
                if (!item.photoUri.isNullOrEmpty()) {
                    Glide.with(binding.root.context)
                        .load(item.photoUri)
                        .placeholder(R.drawable.ic_inventory_24)
                        .error(R.drawable.ic_inventory_24)
                        .centerCrop()
                        .into(imageViewItem)
                    
                    // 清除背景色调
                    imageViewItem.backgroundTintList = null
                    imageViewItem.clearColorFilter()
                } else {
                    // 使用默认图标
                    imageViewItem.setImageResource(R.drawable.ic_inventory_24)
                    imageViewItem.setBackgroundResource(R.drawable.bg_circle)
                    imageViewItem.backgroundTintList = 
                        binding.root.context.getColorStateList(R.color.surface_variant)
                    imageViewItem.setColorFilter(
                        binding.root.context.getColor(R.color.on_surface_variant)
                    )
                }
            }
        }
    }

    private class WastedItemDiffCallback : DiffUtil.ItemCallback<WastedItemData>() {
        override fun areItemsTheSame(oldItem: WastedItemData, newItem: WastedItemData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WastedItemData, newItem: WastedItemData): Boolean {
            return oldItem == newItem
        }
    }
} 