package com.example.itemmanagement.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.ItemWasteInsightBinding
import com.example.itemmanagement.data.model.InsightSeverity
import com.example.itemmanagement.data.model.InsightType
import com.example.itemmanagement.data.model.WasteInsight

class WasteInsightAdapter : ListAdapter<WasteInsight, WasteInsightAdapter.InsightViewHolder>(InsightDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsightViewHolder {
        val binding = ItemWasteInsightBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InsightViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InsightViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class InsightViewHolder(
        private val binding: ItemWasteInsightBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(insight: WasteInsight, position: Int) {
            with(binding) {
                // 设置标题和内容
                textViewTitle.text = insight.title
                textViewMessage.text = insight.description

                // 只有第一个卡片（position == 0）才使用彩色背景，其他都用白色
                if (position == 0) {
                    // 根据洞察类型设置图标和颜色
                    when (insight.type) {
                        InsightType.TIP -> {
                            imageViewIcon.setImageResource(R.drawable.ic_star)
                            setupColors(R.color.success, R.color.surface_variant, R.color.on_surface)
                        }
                        InsightType.WARNING -> {
                            imageViewIcon.setImageResource(R.drawable.ic_stock_warning)
                            setupColors(R.color.orange, R.color.orange_container, R.color.on_orange_container)
                        }
                        InsightType.ALERT -> {
                            imageViewIcon.setImageResource(R.drawable.ic_error_placeholder)
                            setupColors(R.color.error, R.color.error_container, R.color.on_error_container)
                        }
                        InsightType.INFO -> {
                            imageViewIcon.setImageResource(R.drawable.ic_statistics)
                            setupColors(R.color.primary, R.color.surface_variant, R.color.on_surface)
                        }
                    }
                } else {
                    // 其他卡片使用统一的图标和白色背景
                    when (insight.type) {
                        InsightType.TIP -> imageViewIcon.setImageResource(R.drawable.ic_star)
                        InsightType.WARNING -> imageViewIcon.setImageResource(R.drawable.ic_stock_warning)
                        InsightType.ALERT -> imageViewIcon.setImageResource(R.drawable.ic_error_placeholder)
                        InsightType.INFO -> imageViewIcon.setImageResource(R.drawable.ic_statistics)
                    }
                    setupColors(R.color.primary, android.R.color.white, R.color.on_surface)
                }

                // 根据严重程度调整透明度
                when (insight.severity) {
                    InsightSeverity.LOW -> root.alpha = 0.8f
                    InsightSeverity.MEDIUM -> root.alpha = 0.9f
                    InsightSeverity.HIGH -> root.alpha = 1.0f
                }
            }
        }

        private fun setupColors(iconColor: Int, backgroundColor: Int, textColor: Int) {
            with(binding) {
                imageViewIcon.setColorFilter(ContextCompat.getColor(root.context, iconColor))
                cardInsight.setCardBackgroundColor(ContextCompat.getColor(root.context, backgroundColor))
                textViewTitle.setTextColor(ContextCompat.getColor(root.context, textColor))
                textViewMessage.setTextColor(ContextCompat.getColor(root.context, textColor))
            }
        }
    }

    private class InsightDiffCallback : DiffUtil.ItemCallback<WasteInsight>() {
        override fun areItemsTheSame(oldItem: WasteInsight, newItem: WasteInsight): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: WasteInsight, newItem: WasteInsight): Boolean {
            return oldItem == newItem
        }
    }
} 