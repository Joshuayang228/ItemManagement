package com.example.itemmanagement.ui.reminder

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.CategoryThresholdEntity
import com.google.android.material.switchmaterial.SwitchMaterial

class CategoryThresholdAdapter(
    private val onUpdateThreshold: (String, Double) -> Unit,
    private val onDeleteThreshold: (String) -> Unit,
    private val onToggleEnabled: ((String, Boolean) -> Unit)? = null
) : ListAdapter<CategoryThresholdEntity, CategoryThresholdAdapter.ThresholdViewHolder>(CategoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThresholdViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_threshold, parent, false)
        return ThresholdViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ThresholdViewHolder, position: Int) {
        val threshold = getItem(position)
        holder.bind(threshold)
    }
    
    inner class ThresholdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryText: TextView = itemView.findViewById(R.id.textCategory)
        private val quantityEditText: EditText = itemView.findViewById(R.id.editTextMinQuantity)
        private val unitText: TextView = itemView.findViewById(R.id.textUnit)
        private val enabledSwitch: SwitchMaterial = itemView.findViewById(R.id.switchEnabled)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDelete)
        private val descriptionText: TextView = itemView.findViewById(R.id.textDescription)
        
        private var isUpdating = false
        
        fun bind(threshold: CategoryThresholdEntity) {
            isUpdating = true
            
            categoryText.text = threshold.category
            quantityEditText.setText(threshold.minQuantity.toString())
            unitText.text = threshold.unit
            enabledSwitch.isChecked = threshold.enabled
            
            // 显示描述（如果有）
            if (threshold.description.isNotEmpty()) {
                descriptionText.text = threshold.description
                descriptionText.visibility = View.VISIBLE
            } else {
                descriptionText.visibility = View.GONE
            }
            
            isUpdating = false
            
            // 设置监听器
            setupListeners(threshold)
        }
        
        private fun setupListeners(threshold: CategoryThresholdEntity) {
            // 数量输入监听
            quantityEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    if (isUpdating) return
                    
                    val quantityStr = s.toString().trim()
                    if (quantityStr.isNotEmpty()) {
                        val quantity = quantityStr.toDoubleOrNull()
                        if (quantity != null && quantity > 0) {
                            onUpdateThreshold(threshold.category, quantity)
                        }
                    }
                }
            })
            
            // 启用开关监听
            enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (isUpdating) return@setOnCheckedChangeListener
                onToggleEnabled?.invoke(threshold.category, isChecked)
            }
            
            // 删除按钮监听
            deleteButton.setOnClickListener {
                showDeleteConfirmDialog(threshold)
            }
        }
        
        private fun showDeleteConfirmDialog(threshold: CategoryThresholdEntity) {
            androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                .setTitle("删除分类阈值")
                .setMessage("确定要删除 \"${threshold.category}\" 的库存阈值设置吗？")
                .setPositiveButton("删除") { _, _ ->
                    onDeleteThreshold(threshold.category)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
    
    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryThresholdEntity>() {
        override fun areItemsTheSame(
            oldItem: CategoryThresholdEntity,
            newItem: CategoryThresholdEntity
        ): Boolean {
            return oldItem.category == newItem.category
        }
        
        override fun areContentsTheSame(
            oldItem: CategoryThresholdEntity,
            newItem: CategoryThresholdEntity
        ): Boolean {
            return oldItem == newItem
        }
    }
    
    /**
     * 获取指定分类的阈值实体
     */
    fun getThresholdByCategory(category: String): CategoryThresholdEntity? {
        return currentList.find { it.category == category }
    }
    
    /**
     * 获取所有启用的分类
     */
    fun getEnabledCategories(): List<String> {
        return currentList.filter { it.enabled }.map { it.category }
    }
    
    /**
     * 获取分类总数统计
     */
    fun getCategoriesStats(): CategoryStats {
        val total = currentList.size
        val enabled = currentList.count { it.enabled }
        val avgThreshold = if (enabled > 0) {
            currentList.filter { it.enabled }.map { it.minQuantity }.average()
        } else 0.0
        
        return CategoryStats(
            totalCategories = total,
            enabledCategories = enabled,
            averageThreshold = avgThreshold
        )
    }
}

/**
 * 分类阈值统计数据
 */
data class CategoryStats(
    val totalCategories: Int,
    val enabledCategories: Int,
    val averageThreshold: Double
) {
    fun getDescription(): String {
        return "共 $totalCategories 个分类，$enabledCategories 个已启用"
    }
}
