package com.example.itemmanagement.ui.warehouse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.warehouse.FilterCategory

/**
 * 优化的筛选导航适配器 V2
 * 
 * 改进点：
 * 1. 使用ListAdapter和DiffUtil优化性能
 * 2. 更好的状态管理
 * 3. 支持动画效果
 * 4. 内存使用优化
 */
class FilterNavigationAdapterV2(
    private val onCategoryClick: (FilterCategory, Int) -> Unit
) : ListAdapter<FilterNavigationItem, FilterNavigationAdapterV2.ViewHolder>(NavigationDiffCallback()) {
    
    private var selectedPosition = 0
    
    init {
        // 启用稳定ID，提升性能
        setHasStableIds(true)
    }
    
    /**
     * ViewHolder类
     */
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.nav_title)
        val indicatorView: View = itemView.findViewById(R.id.nav_indicator)
        
        fun bind(
            item: FilterNavigationItem,
            isSelected: Boolean,
            onCategoryClick: (FilterCategory, Int) -> Unit
        ) {
            titleText.text = item.category.displayName
            
            // 设置选中状态
            titleText.isSelected = isSelected
            indicatorView.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            
            // 设置文本颜色
            val textColor = if (isSelected) {
                itemView.context.getColor(R.color.primary)
            } else {
                itemView.context.getColor(android.R.color.black)
            }
            titleText.setTextColor(textColor)
            
            // 设置点击监听器
            itemView.setOnClickListener {
                onCategoryClick(item.category, adapterPosition)
            }
            
            // 设置内容描述（无障碍访问）
            itemView.contentDescription = if (isSelected) {
                "${item.category.displayName}，已选中"
            } else {
                item.category.displayName
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_navigation, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val isSelected = position == selectedPosition
        
        holder.bind(item, isSelected, onCategoryClick)
    }
    
    override fun getItemId(position: Int): Long {
        return getItem(position).category.ordinal.toLong()
    }
    
    /**
     * 设置选中的位置
     */
    fun setSelectedPosition(position: Int) {
        if (position == selectedPosition) return // 避免不必要的更新
        
        val oldPosition = selectedPosition
        selectedPosition = position
        
        // 只更新变化的项
        if (oldPosition >= 0 && oldPosition < itemCount) {
            notifyItemChanged(oldPosition)
        }
        if (position >= 0 && position < itemCount) {
            notifyItemChanged(position)
        }
    }
    
    /**
     * 获取当前选中位置
     */
    fun getSelectedPosition(): Int {
        return selectedPosition
    }
    
    /**
     * 根据分类获取位置
     */
    fun getPositionByCategory(category: FilterCategory): Int {
        for (i in 0 until itemCount) {
            if (getItem(i).category == category) {
                return i
            }
        }
        return -1
    }
    
    /**
     * 根据位置获取分类
     */
    fun getCategoryByPosition(position: Int): FilterCategory? {
        return if (position >= 0 && position < itemCount) {
            getItem(position).category
        } else {
            null
        }
    }
    
    /**
     * 提交筛选分类列表
     */
    fun submitCategories(categories: List<FilterCategory>) {
        val items = categories.map { FilterNavigationItem(it) }
        submitList(items)
    }
    
    /**
     * 重置选中状态到第一项
     */
    fun resetSelection() {
        setSelectedPosition(0)
    }
    
    /**
     * 选择下一项
     */
    fun selectNext(): Boolean {
        return if (selectedPosition < itemCount - 1) {
            setSelectedPosition(selectedPosition + 1)
            true
        } else {
            false
        }
    }
    
    /**
     * 选择上一项
     */
    fun selectPrevious(): Boolean {
        return if (selectedPosition > 0) {
            setSelectedPosition(selectedPosition - 1)
            true
        } else {
            false
        }
    }
}

/**
 * 导航项数据类
 */
data class FilterNavigationItem(
    val category: FilterCategory,
    val badge: String? = null,  // 可选的徽章文本
    val isEnabled: Boolean = true  // 是否启用
)

/**
 * DiffUtil回调
 */
class NavigationDiffCallback : DiffUtil.ItemCallback<FilterNavigationItem>() {
    override fun areItemsTheSame(oldItem: FilterNavigationItem, newItem: FilterNavigationItem): Boolean {
        return oldItem.category == newItem.category
    }
    
    override fun areContentsTheSame(oldItem: FilterNavigationItem, newItem: FilterNavigationItem): Boolean {
        return oldItem == newItem
    }
    
    override fun getChangePayload(oldItem: FilterNavigationItem, newItem: FilterNavigationItem): Any? {
        // 可以返回具体的变化信息，用于部分更新
        return when {
            oldItem.badge != newItem.badge -> "badge_changed"
            oldItem.isEnabled != newItem.isEnabled -> "enabled_changed"
            else -> null
        }
    }
}

/**
 * 扩展函数：便于使用
 */
fun FilterNavigationAdapterV2.submitFilterCategories(categories: List<FilterCategory>) {
    submitCategories(categories)
}
