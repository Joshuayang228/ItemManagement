package com.example.itemmanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.FunctionCard
import com.example.itemmanagement.data.model.FunctionSection
import com.example.itemmanagement.data.model.InventoryStats
import com.example.itemmanagement.databinding.ItemFunctionCardBinding
import com.example.itemmanagement.databinding.ItemFunctionSectionHeaderBinding
import com.example.itemmanagement.databinding.ItemInventoryStatsBinding

class FunctionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_STATS = 0
        private const val TYPE_SECTION_HEADER = 1
        private const val TYPE_FUNCTION_CARD = 2
    }

    private var functionSections: List<FunctionSection> = emptyList()
    private var inventoryStats: InventoryStats? = null
    private var onFunctionClickListener: ((String) -> Unit)? = null
    private var flattenedItems: List<Any> = emptyList()

    fun submitSections(newSections: List<FunctionSection>) {
        functionSections = newSections
        flattenedItems = buildFlattenedList()
        notifyDataSetChanged()
    }

    private fun buildFlattenedList(): List<Any> {
        val items = mutableListOf<Any>()
        
        // 添加统计概览作为第一项
        items.add("STATS_HEADER")
        
        // 为每个分组添加标题和功能卡片
        functionSections.forEach { section ->
            items.add(section) // 分组标题
            items.addAll(section.functions) // 该分组下的功能卡片
        }
        
        return items
    }

    fun updateInventoryStats(stats: InventoryStats) {
        inventoryStats = stats
        if (flattenedItems.isNotEmpty() && flattenedItems[0] == "STATS_HEADER") {
            notifyItemChanged(0) // 更新统计卡片
        }
    }

    fun setOnFunctionClickListener(listener: (String) -> Unit) {
        onFunctionClickListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = flattenedItems[position]) {
            "STATS_HEADER" -> TYPE_STATS
            is FunctionSection -> TYPE_SECTION_HEADER
            is FunctionCard -> TYPE_FUNCTION_CARD
            else -> throw IllegalArgumentException("Unknown item type: $item")
        }
    }

    override fun getItemCount(): Int = flattenedItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_STATS -> {
                val binding = ItemInventoryStatsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                StatsViewHolder(binding)
            }
            TYPE_SECTION_HEADER -> {
                val binding = ItemFunctionSectionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SectionHeaderViewHolder(binding)
            }
            TYPE_FUNCTION_CARD -> {
                val binding = ItemFunctionCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                FunctionCardViewHolder(binding, onFunctionClickListener)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is StatsViewHolder -> {
                inventoryStats?.let { holder.bind(it) }
            }
            is SectionHeaderViewHolder -> {
                val section = flattenedItems[position] as FunctionSection
                holder.bind(section)
            }
            is FunctionCardViewHolder -> {
                val card = flattenedItems[position] as FunctionCard
                holder.bind(card)
            }
        }
    }

    class SectionHeaderViewHolder(
        private val binding: ItemFunctionSectionHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(section: FunctionSection) {
            binding.apply {
                sectionTitle.text = section.title
                sectionDescription.text = section.description
                sectionIcon.setImageResource(section.iconResId)
            }
        }
    }

    class StatsViewHolder(
        private val binding: ItemInventoryStatsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stats: InventoryStats) {
            binding.apply {
                totalItemsValue.text = stats.totalItems.toString()
                totalValueText.text = "¥${String.format("%.2f", stats.totalValue)}"
                expiringItemsValue.text = stats.expiringItems.toString()
                expiredItemsValue.text = stats.expiredItems.toString()
                lowStockItemsValue.text = stats.lowStockItems.toString()
                
                // 设置新的统计信息
                categoriesCountValue.text = stats.categoriesCount.toString()
                locationsCountValue.text = stats.locationsCount.toString()
                recentlyAddedValue.text = stats.recentlyAddedItems.toString()

                // 设置警告状态
                expiringItemsCard.setCardBackgroundColor(
                    if (stats.expiringItems > 0) 
                        itemView.context.getColor(R.color.status_warning_bg)
                    else 
                        itemView.context.getColor(R.color.card_background)
                )

                expiredItemsCard.setCardBackgroundColor(
                    if (stats.expiredItems > 0) 
                        itemView.context.getColor(R.color.status_error_bg)
                    else 
                        itemView.context.getColor(R.color.card_background)
                )

                lowStockCard.setCardBackgroundColor(
                    if (stats.lowStockItems > 0) 
                        itemView.context.getColor(R.color.status_warning_bg)
                    else 
                        itemView.context.getColor(R.color.card_background)
                )
            }
        }
    }

    class FunctionCardViewHolder(
        private val binding: ItemFunctionCardBinding,
        private val onFunctionClickListener: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(card: FunctionCard) {
            binding.apply {
                functionTitle.text = card.title
                functionDescription.text = card.description
                functionIcon.setImageResource(card.iconResId)

                // 根据功能类型设置颜色
                val (bgColor, accentColor) = when (card.type) {
                    FunctionCard.Type.ANALYTICS, 
                    FunctionCard.Type.CALENDAR, 
                    FunctionCard.Type.WASTE_REPORT -> {
                        Pair(R.color.data_insights_bg, R.color.data_insights_accent)
                    }
                    FunctionCard.Type.WISHLIST, 
                    FunctionCard.Type.REMINDER, 
                    FunctionCard.Type.WARRANTY -> {
                        Pair(R.color.smart_assistant_bg, R.color.smart_assistant_accent)
                    }
                    FunctionCard.Type.LENDING, 
                    FunctionCard.Type.BACKUP, 
                    FunctionCard.Type.UTILITY -> {
                        Pair(R.color.utilities_bg, R.color.utilities_accent)
                    }
                }

                // 设置卡片背景色和图标颜色
                root.setCardBackgroundColor(itemView.context.getColor(bgColor))
                functionIcon.setColorFilter(itemView.context.getColor(accentColor))

                // 设置徽章
                if (card.badgeCount != null && card.badgeCount > 0) {
                    badge.visibility = View.VISIBLE
                    badge.text = if (card.badgeCount > 99) "99+" else card.badgeCount.toString()
                } else {
                    badge.visibility = View.GONE
                }

                // 设置启用状态
                root.isEnabled = card.isEnabled
                root.alpha = if (card.isEnabled) 1.0f else 0.5f

                // 设置点击事件
                root.setOnClickListener {
                    if (card.isEnabled) {
                        onFunctionClickListener?.invoke(card.id)
                    }
                }
            }
        }
    }
} 