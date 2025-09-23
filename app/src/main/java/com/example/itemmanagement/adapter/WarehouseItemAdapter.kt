package com.example.itemmanagement.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.WarehouseItem
import com.example.itemmanagement.databinding.ItemWarehouseBinding
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * 仓库物品列表适配器，使用轻量级WarehouseItem模型
 */
class WarehouseItemAdapter(
    private val onItemClick: (Long) -> Unit,
    private val onEdit: (Long) -> Unit,
    private val onDelete: (Long) -> Unit
) : ListAdapter<WarehouseItem, WarehouseItemAdapter.ItemViewHolder>(ITEM_COMPARATOR) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemWarehouseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onItemClick(item.id) }
        holder.binding.editButton.setOnClickListener { onEdit(item.id) }
        holder.binding.deleteButton.setOnClickListener { onDelete(item.id) }
    }

    class ItemViewHolder(val binding: ItemWarehouseBinding) : RecyclerView.ViewHolder(binding.root) {
        // 标签背景资源ID数组
        private val tagBackgrounds = arrayOf(
            R.drawable.bg_tag_blue,
            R.drawable.bg_tag_green,
            R.drawable.bg_tag_purple,
            R.drawable.bg_tag_orange,
            R.drawable.bg_tag_red
        )
        
        // 标签文本颜色数组
        private val tagTextColors = arrayOf(
            Color.parseColor("#1976D2"), // 蓝色
            Color.parseColor("#388E3C"), // 绿色
            Color.parseColor("#7B1FA2"), // 紫色
            Color.parseColor("#F57C00"), // 橙色
            Color.parseColor("#D32F2F")  // 红色
        )
        
        fun bind(item: WarehouseItem) {
            // 设置物品名称
            binding.itemName.text = item.name
            
            // 设置位置信息 - 使用多级位置逻辑
            setupLocation(item)
            
            // 设置标签 - 最多显示3个
            setupTags(item)
            
            // 设置数量、分类和评分行
            setupQuantityCategoryRating(item)
            
            // 设置价格和开封状态行
            setupPriceAndOpenStatus(item)
            
            // 加载图片
            if (!item.primaryPhotoUri.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(item.primaryPhotoUri)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(binding.itemImage)
            } else {
                binding.itemImage.setImageResource(R.drawable.ic_image_placeholder)
            }
            
            // 更新保质期状态
            if (item.expirationDate != null) {
                updateExpirationStatus(item.expirationDate, item.addDate ?: Date().time)
                binding.expirationStatusView.visibility = View.VISIBLE
            } else {
                binding.expirationStatusView.visibility = View.GONE
            }
        }
        
        /**
         * 设置位置信息 - 使用多级位置逻辑
         */
        private fun setupLocation(item: WarehouseItem) {
            val hasArea = !item.locationArea.isNullOrBlank()
            val hasContainer = !item.locationContainer.isNullOrBlank()
            val hasSublocation = !item.locationSublocation.isNullOrBlank()
            
            if (!hasArea) {
                // 没有位置信息
                binding.locationIcon.visibility = View.GONE
                binding.itemLocation.visibility = View.GONE
                return
            }
            
            binding.locationIcon.visibility = View.VISIBLE
            binding.itemLocation.visibility = View.VISIBLE
            
            // 构建位置文本
            val locationText = buildString {
                append(item.locationArea)
                
                if (hasContainer) {
                    append(" > ")
                    append(item.locationContainer)
                    
                    if (hasSublocation) {
                        append(" > ")
                        append(item.locationSublocation)
                    }
                }
            }
            
            binding.itemLocation.text = locationText
        }
        
        /**
         * 设置标签 - 最多显示3个
         */
        private fun setupTags(item: WarehouseItem) {
            // 重置所有标签的可见性
            binding.itemTag1.visibility = View.GONE
            binding.itemTag2.visibility = View.GONE
            binding.itemTag3.visibility = View.GONE
            
            // 如果标签字符串不为空，解析为列表
            val tags = if (!item.tagsList.isNullOrBlank()) {
                item.tagsList.split(",").take(3)
            } else {
                emptyList()
            }
            
            if (tags.isNotEmpty()) {
                // 最多显示3个标签
                if (tags.size > 0) {
                    binding.itemTag1.text = tags[0]
                    binding.itemTag1.setBackgroundResource(tagBackgrounds[0 % tagBackgrounds.size])
                    binding.itemTag1.setTextColor(tagTextColors[0 % tagTextColors.size])
                    binding.itemTag1.visibility = View.VISIBLE
                }
                
                if (tags.size > 1) {
                    binding.itemTag2.text = tags[1]
                    binding.itemTag2.setBackgroundResource(tagBackgrounds[1 % tagBackgrounds.size])
                    binding.itemTag2.setTextColor(tagTextColors[1 % tagTextColors.size])
                    binding.itemTag2.visibility = View.VISIBLE
                }
                
                if (tags.size > 2) {
                    binding.itemTag3.text = tags[2]
                    binding.itemTag3.setBackgroundResource(tagBackgrounds[2 % tagBackgrounds.size])
                    binding.itemTag3.setTextColor(tagTextColors[2 % tagTextColors.size])
                    binding.itemTag3.visibility = View.VISIBLE
                }
            }
        }
        
        /**
         * 设置数量、分类和评分行
         */
        private fun setupQuantityCategoryRating(item: WarehouseItem) {
            // 设置数量
            val hasQuantity = item.quantity > 0
            if (hasQuantity) {
                binding.itemQuantity.text = "${item.quantity} 个"
                binding.itemQuantity.visibility = View.VISIBLE
            } else {
                binding.itemQuantity.visibility = View.GONE
            }
            
            // 设置分类
            val hasCategory = !item.category.isNullOrBlank()
            if (hasCategory) {
                binding.itemCategory.text = item.category
                binding.itemCategory.visibility = View.VISIBLE
                
                // 如果有数量和分类，显示分隔符1
                binding.separator1.visibility = if (hasQuantity) View.VISIBLE else View.GONE
            } else {
                binding.itemCategory.visibility = View.GONE
                binding.separator1.visibility = View.GONE
            }
            
            // 设置评分
            val hasRating = item.rating != null && item.rating > 0
            if (hasRating) {
                // 将评分转换为星星表示 - 使用实心星星⭐
                val ratingText = buildString {
                    val fullStars = item.rating!!.toInt()
                    
                    // 添加实心星星
                    repeat(fullStars) {
                        append("⭐")
                    }
                }
                
                binding.itemRating.text = ratingText
                binding.itemRating.visibility = View.VISIBLE
                
                // 如果有分类和评分，或者有数量和评分但没有分类，显示分隔符2
                binding.separator2.visibility = if ((hasCategory || hasQuantity)) View.VISIBLE else View.GONE
            } else {
                binding.itemRating.visibility = View.GONE
                binding.separator2.visibility = View.GONE
            }
        }
        
        /**
         * 设置价格、开封状态和添加时间行
         */
        private fun setupPriceAndOpenStatus(item: WarehouseItem) {
            val hasPrice = item.price != null && item.price > 0
            val hasOpenStatus = item.openStatus != null
            val hasAddTime = item.addDate != null
            
            // 如果价格、开封状态和添加时间都没有，隐藏整行
            if (!hasPrice && !hasOpenStatus && !hasAddTime) {
                binding.priceAndStatusGroup.visibility = View.GONE
                return
            }
            
            binding.priceAndStatusGroup.visibility = View.VISIBLE
            
            // 设置价格
            if (hasPrice) {
                // 获取价格单位对应的货币符号
                val currencySymbol = getCurrencySymbol(item.priceUnit)
                
                // 格式化价格，如果是整数则不显示小数点
                val priceValue = item.price!!
                val formattedPrice = if (priceValue == priceValue.toLong().toDouble()) {
                    // 整数价格
                    "$currencySymbol${priceValue.toLong()}"
                } else {
                    // 带小数的价格
                    "$currencySymbol$priceValue"
                }
                
                binding.itemPrice.text = formattedPrice
                binding.itemPrice.setTextColor(itemView.context.getColor(R.color.price_color))
                binding.itemPrice.setTypeface(null, Typeface.BOLD) // 设置价格文本为粗体
                binding.itemPrice.visibility = View.VISIBLE
            } else {
                binding.itemPrice.visibility = View.GONE
                
                // 如果没有价格，调整开封状态的约束，使其左对齐
                if (hasOpenStatus) {
                    val constraintSet = ConstraintSet()
                    constraintSet.clone(binding.infoContainer)
                    constraintSet.connect(
                        R.id.itemOpenStatus,
                        ConstraintSet.START,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.START,
                        0
                    )
                    constraintSet.applyTo(binding.infoContainer)
                }
            }
            
            // 设置开封状态
            if (hasOpenStatus) {
                binding.itemOpenStatus.text = if (item.openStatus == true) "已开封" else "未开封"
                
                // 根据开封状态设置不同的背景
                val backgroundResId = if (item.openStatus == true) {
                    R.drawable.bg_status_opened
                } else {
                    R.drawable.bg_status_unopened
                }
                binding.itemOpenStatus.setBackgroundResource(backgroundResId)
                
                // 设置文本颜色
                binding.itemOpenStatus.setTextColor(
                    if (item.openStatus == true) 
                        Color.parseColor("#F57C00") // 橙色
                    else 
                        Color.parseColor("#388E3C") // 绿色
                )
                binding.itemOpenStatus.visibility = View.VISIBLE
            } else {
                binding.itemOpenStatus.visibility = View.GONE
            }
            
            // 设置添加时间
            if (hasAddTime) {
                val addDate = Date(item.addDate!!)
                val dateFormat = java.text.SimpleDateFormat("yy-MM-dd", Locale.getDefault())
                binding.itemAddTime.text = dateFormat.format(addDate)
                binding.itemAddTime.visibility = View.VISIBLE
            } else {
                binding.itemAddTime.visibility = View.GONE
            }
        }
        
        /**
         * 根据价格单位获取对应的货币符号
         */
        private fun getCurrencySymbol(priceUnit: String?): String {
            return when (priceUnit?.lowercase()) {
                "usd", "美元", "dollar" -> "$"
                "eur", "欧元", "euro" -> "€"
                "jpy", "日元", "yen" -> "¥"
                "gbp", "英镑", "pound" -> "£"
                "krw", "韩元", "won" -> "₩"
                "rub", "卢布", "ruble" -> "₽"
                "inr", "卢比", "rupee" -> "₹"
                else -> "￥" // 默认使用人民币符号
            }
        }
        
        /**
         * 更新保质期状态
         */
        private fun updateExpirationStatus(expirationDate: Long, startDate: Long) {
            val now = Date().time
            val daysUntilExpiration = TimeUnit.MILLISECONDS.toDays(expirationDate - now)
            
            // 计算总时长和已经过去的时长
            val totalDuration = expirationDate - startDate
            val elapsedDuration = now - startDate
            
            // 计算进度百分比
            val progress = if (totalDuration > 0) {
                (elapsedDuration * 100 / totalDuration).toInt().coerceIn(0, 100)
            } else {
                100
            }
            
            // 使用自定义视图的方法更新保质期状态
            when {
                daysUntilExpiration < 0 -> {
                    // 已过期
                    val daysExpired = -daysUntilExpiration
                    binding.expirationStatusView.setStatus(
                        progress, 
                        "已过期 $daysExpired 天", 
                        itemView.context.getColor(R.color.status_error)
                    )
                }
                daysUntilExpiration < 30 -> {
                    // 临近过期（30天内）
                    binding.expirationStatusView.setStatus(
                        progress, 
                        "剩余 $daysUntilExpiration 天", 
                        itemView.context.getColor(R.color.status_warning)
                    )
                }
                else -> {
                    // 正常
                    binding.expirationStatusView.setStatus(
                        progress, 
                        "剩余 $daysUntilExpiration 天", 
                        itemView.context.getColor(R.color.status_good)
                    )
                }
            }
        }
    }
    
    companion object {
        private val ITEM_COMPARATOR = object : DiffUtil.ItemCallback<WarehouseItem>() {
            override fun areItemsTheSame(oldItem: WarehouseItem, newItem: WarehouseItem): Boolean {
                return oldItem.id == newItem.id
            }
            
            override fun areContentsTheSame(oldItem: WarehouseItem, newItem: WarehouseItem): Boolean {
                return oldItem == newItem
            }
        }
    }
} 