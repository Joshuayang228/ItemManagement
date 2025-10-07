package com.example.itemmanagement.ui.warranty

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.dao.WarrantyWithItemInfo
import com.example.itemmanagement.data.entity.WarrantyStatus
import com.example.itemmanagement.databinding.ItemWarrantyBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 保修列表适配器
 */
class WarrantyAdapter(
    private val onItemClick: (WarrantyWithItemInfo) -> Unit,
    private val onDeleteClick: (WarrantyWithItemInfo) -> Unit
) : ListAdapter<WarrantyWithItemInfo, WarrantyAdapter.WarrantyViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarrantyViewHolder {
        val binding = ItemWarrantyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WarrantyViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: WarrantyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class WarrantyViewHolder(
        private val binding: ItemWarrantyBinding,
        private val onItemClick: (WarrantyWithItemInfo) -> Unit,
        private val onDeleteClick: (WarrantyWithItemInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(warranty: WarrantyWithItemInfo) {
            with(binding) {
                // 基本信息
                itemName.text = warranty.itemName
                itemCategory.text = warranty.category
                itemBrand.text = warranty.brand ?: "未知品牌"
                
                // 保修信息
                purchaseDate.text = "购买：${dateFormat.format(Date(warranty.purchaseDate))}"
                warrantyPeriod.text = "${warranty.warrantyPeriodMonths}个月"
                warrantyEndDate.text = "到期：${dateFormat.format(Date(warranty.warrantyEndDate))}"
                
                // 计算剩余天数
                val today = System.currentTimeMillis()
                val endDate = warranty.warrantyEndDate
                val diffDays = TimeUnit.MILLISECONDS.toDays(endDate - today)
                
                when {
                    diffDays > 0 -> {
                        remainingDays.text = "剩余${diffDays}天"
                        remainingDays.setTextColor(getColorForRemainingDays(diffDays))
                        remainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(getBackgroundColorForRemainingDays(diffDays))
                    }
                    diffDays == 0L -> {
                        remainingDays.text = "今日到期"
                        remainingDays.setTextColor(Color.parseColor("#FF5722"))
                        remainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                    }
                    else -> {
                        remainingDays.text = "已过期${-diffDays}天"
                        remainingDays.setTextColor(Color.parseColor("#F44336"))
                        remainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                    }
                }
                
                // 设置保修状态
                setupWarrantyStatus(warranty.status)
                
                // 加载物品的第一张照片
                if (!warranty.firstPhotoUri.isNullOrEmpty()) {
                    Glide.with(itemImage.context)
                        .load(warranty.firstPhotoUri)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .centerCrop()
                        .into(itemImage)
                } else {
                    // 如果没有照片，显示占位符
                    Glide.with(itemImage.context)
                        .load(R.drawable.ic_image_placeholder)
                        .into(itemImage)
                }
                
                // 设置点击事件
                root.setOnClickListener { onItemClick(warranty) }
                deleteButton.setOnClickListener { onDeleteClick(warranty) }
            }
        }

        /**
         * 设置保修状态显示
         */
        private fun setupWarrantyStatus(status: WarrantyStatus) {
            when (status) {
                WarrantyStatus.ACTIVE -> {
                    binding.warrantyStatus.text = "保修中"
                    binding.warrantyStatus.setTextColor(Color.parseColor("#4CAF50"))
                    binding.warrantyStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E8"))
                }
                WarrantyStatus.EXPIRED -> {
                    binding.warrantyStatus.text = "已过期"
                    binding.warrantyStatus.setTextColor(Color.parseColor("#F44336"))
                    binding.warrantyStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                }
                WarrantyStatus.CLAIMED -> {
                    binding.warrantyStatus.text = "已报修"
                    binding.warrantyStatus.setTextColor(Color.parseColor("#FF9800"))
                    binding.warrantyStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
                }
                WarrantyStatus.VOID -> {
                    binding.warrantyStatus.text = "已作废"
                    binding.warrantyStatus.setTextColor(Color.parseColor("#9E9E9E"))
                    binding.warrantyStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5"))
                }
            }
        }

        /**
         * 根据剩余天数获取文本颜色
         */
        private fun getColorForRemainingDays(days: Long): Int {
            return when {
                days > 30 -> Color.parseColor("#4CAF50") // 绿色：充足时间
                days > 7 -> Color.parseColor("#FF9800")  // 橙色：即将到期
                else -> Color.parseColor("#F44336")      // 红色：紧急
            }
        }

        /**
         * 根据剩余天数获取背景颜色
         */
        private fun getBackgroundColorForRemainingDays(days: Long): Int {
            return when {
                days > 30 -> Color.parseColor("#E8F5E8") // 浅绿色
                days > 7 -> Color.parseColor("#FFF3E0")  // 浅橙色
                else -> Color.parseColor("#FFEBEE")      // 浅红色
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<WarrantyWithItemInfo>() {
            override fun areItemsTheSame(
                oldItem: WarrantyWithItemInfo,
                newItem: WarrantyWithItemInfo
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: WarrantyWithItemInfo,
                newItem: WarrantyWithItemInfo
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
