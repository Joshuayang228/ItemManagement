package com.example.itemmanagement.ui.warehouse.managers

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.example.itemmanagement.ui.warehouse.FilterState
import com.example.itemmanagement.ui.warehouse.WarehouseViewModel
import com.example.itemmanagement.ui.warehouse.components.base.FilterComponent
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 筛选状态管理器
 * 
 * 功能：
 * 1. 统一管理筛选状态的观察和更新
 * 2. 防止循环更新问题
 * 3. 优化UI更新性能
 * 4. 协调各个筛选组件的状态同步
 */
class FilterStateManager(
    private val viewModel: WarehouseViewModel,
    private val binding: FragmentFilterBottomSheetBinding
) {
    
    // 更新锁，防止循环更新
    private val updateLock = AtomicBoolean(false)
    
    // 筛选组件列表
    private val filterComponents = mutableListOf<FilterComponent>()
    
    // 滚动位置保存
    private var savedScrollPosition = 0
    
    /**
     * 注册筛选组件
     */
    fun registerFilterComponent(component: FilterComponent) {
        filterComponents.add(component)
    }
    
    /**
     * 注销筛选组件
     */
    fun unregisterFilterComponent(component: FilterComponent) {
        filterComponents.remove(component)
    }
    
    /**
     * 开始观察筛选状态变化
     */
    fun observeState(lifecycleScope: LifecycleCoroutineScope) {
        lifecycleScope.launch {
            viewModel.filterState
                .debounce(50) // UI更新防抖，提升性能
                .collect { state ->
                    handleStateUpdate(state)
                }
        }
    }
    
    /**
     * 处理状态更新
     */
    private suspend fun handleStateUpdate(state: FilterState) {
        // 使用原子操作防止并发更新
        if (updateLock.compareAndSet(false, true)) {
            try {
                updateUI(state)
            } finally {
                updateLock.set(false)
            }
        }
    }
    
    /**
     * 更新UI - 在主线程执行
     */
    private suspend fun updateUI(state: FilterState) = withContext(Dispatchers.Main) {
        // 保存当前滚动位置
        savedScrollPosition = binding.contentScrollView.scrollY
        
        // 只更新筛选组件，让每个组件负责自己的UI更新
        updateAllFilterComponents(state)
        
        // 不再在StateManager中重复更新基础输入框
        // 各个FilterComponent会处理自己的UI更新
        
        // 恢复滚动位置
        binding.contentScrollView.post {
            binding.contentScrollView.scrollTo(0, savedScrollPosition)
        }
    }
    
    /**
     * 更新所有筛选组件
     */
    private fun updateAllFilterComponents(state: FilterState) {
        filterComponents.forEach { component ->
            try {
                component.updateFromState(state)
            } catch (e: Exception) {
                // 日志记录，但不中断其他组件的更新
                android.util.Log.e("FilterStateManager", "Error updating component: ${component.javaClass.simpleName}", e)
            }
        }
    }
    
    /**
     * 更新基础输入框（品牌、数值范围、日期等）
     */
    private fun updateBasicInputs(state: FilterState) {
        // 品牌输入框：防止光标位置被重置
        updateBrandInput(state.brand)
        
        // 数值输入框
        updateValueInputs(state)
        
        // 日期输入框
        updateDateInputs(state)
    }
    
    /**
     * 更新品牌输入框
     */
    private fun updateBrandInput(brand: String) {
        val currentText = binding.coreSection.brandDropdown.text.toString()
        if (currentText != brand) {
            // 保存光标位置
            val currentSelection = binding.coreSection.brandDropdown.selectionStart
            binding.coreSection.brandDropdown.setText(brand, false)
            
            // 恢复光标位置
            val newSelection = minOf(currentSelection, brand.length)
            binding.coreSection.brandDropdown.setSelection(newSelection)
        }
    }
    
    /**
     * 更新数值输入框
     */
    private fun updateValueInputs(state: FilterState) {
        binding.valueRangeSection.minQuantityInput.setText(state.minQuantity?.toString() ?: "")
        binding.valueRangeSection.maxQuantityInput.setText(state.maxQuantity?.toString() ?: "")
        binding.valueRangeSection.minPriceInput.setText(state.minPrice?.toString() ?: "")
        binding.valueRangeSection.maxPriceInput.setText(state.maxPrice?.toString() ?: "")
    }
    
    /**
     * 更新日期输入框
     */
    private fun updateDateInputs(state: FilterState) {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        
        // 过期日期
        binding.dateSection.expirationStartDateInput.setText(
            state.expirationStartDate?.let { dateFormat.format(java.util.Date(it)) } ?: ""
        )
        binding.dateSection.expirationEndDateInput.setText(
            state.expirationEndDate?.let { dateFormat.format(java.util.Date(it)) } ?: ""
        )
        
        // 添加日期
        binding.dateSection.creationStartDateInput.setText(
            state.creationStartDate?.let { dateFormat.format(java.util.Date(it)) } ?: ""
        )
        binding.dateSection.creationEndDateInput.setText(
            state.creationEndDate?.let { dateFormat.format(java.util.Date(it)) } ?: ""
        )
        
        // 购买日期
        binding.dateSection.purchaseStartDateInput.setText(
            state.purchaseStartDate?.let { dateFormat.format(java.util.Date(it)) } ?: ""
        )
        binding.dateSection.purchaseEndDateInput.setText(
            state.purchaseEndDate?.let { dateFormat.format(java.util.Date(it)) } ?: ""
        )
        
        // 生产日期
        binding.dateSection.productionStartDateInput.setText(
            state.productionStartDate?.let { dateFormat.format(java.util.Date(it)) } ?: ""
        )
        binding.dateSection.productionEndDateInput.setText(
            state.productionEndDate?.let { dateFormat.format(java.util.Date(it)) } ?: ""
        )
    }
    
    /**
     * 重置所有筛选条件
     */
    fun resetAllFilters() {
        // 重置ViewModel状态
        viewModel.resetFilters()
        
        // 重置所有组件
        filterComponents.forEach { component ->
            component.resetToDefault()
        }
        
        // 重置基础输入框
        resetBasicInputs()
    }
    
    /**
     * 重置基础输入框
     */
    private fun resetBasicInputs() {
        binding.coreSection.brandDropdown.setText("", false)
        binding.valueRangeSection.minQuantityInput.setText("")
        binding.valueRangeSection.maxQuantityInput.setText("")
        binding.valueRangeSection.minPriceInput.setText("")
        binding.valueRangeSection.maxPriceInput.setText("")
        
        // 重置日期输入框
        binding.dateSection.expirationStartDateInput.setText("")
        binding.dateSection.expirationEndDateInput.setText("")
        binding.dateSection.creationStartDateInput.setText("")
        binding.dateSection.creationEndDateInput.setText("")
        binding.dateSection.purchaseStartDateInput.setText("")
        binding.dateSection.purchaseEndDateInput.setText("")
        binding.dateSection.productionStartDateInput.setText("")
        binding.dateSection.productionEndDateInput.setText("")
    }
    
    /**
     * 清除所有输入框焦点
     */
    fun clearAllFocus() {
        binding.coreSection.brandDropdown.clearFocus()
        binding.valueRangeSection.minQuantityInput.clearFocus()
        binding.valueRangeSection.maxQuantityInput.clearFocus()
        binding.valueRangeSection.minPriceInput.clearFocus()
        binding.valueRangeSection.maxPriceInput.clearFocus()
        
        // 清除日期输入框焦点
        binding.dateSection.expirationStartDateInput.clearFocus()
        binding.dateSection.expirationEndDateInput.clearFocus()
        binding.dateSection.creationStartDateInput.clearFocus()
        binding.dateSection.creationEndDateInput.clearFocus()
        binding.dateSection.purchaseStartDateInput.clearFocus()
        binding.dateSection.purchaseEndDateInput.clearFocus()
        binding.dateSection.productionStartDateInput.clearFocus()
        binding.dateSection.productionEndDateInput.clearFocus()
    }
    
    /**
     * 获取当前筛选状态
     */
    fun getCurrentFilterState(): StateFlow<FilterState> {
        return viewModel.filterState
    }
    
    /**
     * 手动触发状态更新
     */
    suspend fun forceUpdateUI() {
        val currentState = viewModel.filterState.value
        handleStateUpdate(currentState)
    }
    
    /**
     * 检查是否正在更新
     */
    fun isUpdating(): Boolean {
        return updateLock.get()
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        filterComponents.clear()
        updateLock.set(false)
    }
}
