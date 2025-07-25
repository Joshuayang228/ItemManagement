package com.example.itemmanagement.ui.warehouse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R

/**
 * 筛选导航栏适配器
 */
class FilterNavigationAdapter(
    private val categories: List<FilterCategory>,
    private val onCategoryClick: (FilterCategory, Int) -> Unit
) : RecyclerView.Adapter<FilterNavigationAdapter.ViewHolder>() {
    
    private var selectedPosition = 0
    
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.nav_title)
        val indicatorView: View = itemView.findViewById(R.id.nav_indicator)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter_navigation, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        
        holder.titleText.text = category.displayName
        
        // 设置选中状态
        val isSelected = position == selectedPosition
        holder.titleText.isSelected = isSelected
        holder.indicatorView.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        
        // 设置文本颜色
        val textColor = if (isSelected) {
            holder.itemView.context.getColor(R.color.primary)
        } else {
            holder.itemView.context.getColor(android.R.color.black)
        }
        holder.titleText.setTextColor(textColor)
        
        holder.itemView.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition)
            notifyItemChanged(selectedPosition)
            onCategoryClick(category, position)
        }
    }
    
    override fun getItemCount(): Int = categories.size
    
    /**
     * 设置选中的位置
     */
    fun setSelectedPosition(position: Int) {
        if (position == selectedPosition) return // 避免不必要的更新
        
        val oldPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(selectedPosition)
    }
} 