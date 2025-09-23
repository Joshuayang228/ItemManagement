package com.example.itemmanagement.ui.warehouse.components

import android.view.View
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.example.itemmanagement.ui.warehouse.FilterState
import com.example.itemmanagement.ui.warehouse.WarehouseViewModel
import com.example.itemmanagement.ui.warehouse.components.base.BaseFilterComponent
import com.example.itemmanagement.ui.warehouse.components.base.RangeFilterComponent

/**
 * 数值范围筛选组件
 * 
 * 功能：
 * 1. 管理数量范围输入
 * 2. 管理价格范围输入
 * 3. 数值验证
 * 4. 与ViewModel同步状态
 */
class ValueRangeFilterComponent(
    private val binding: FragmentFilterBottomSheetBinding,
    private val viewModel: WarehouseViewModel
) : BaseFilterComponent(), RangeFilterComponent {
    
    companion object {
        private const val COMPONENT_ID = "value_range_filter"
    }
    
    // 防止无限循环的状态更新标志
    private var isUpdatingFromState = false
    
    // 当前数量范围
    private var currentMinQuantity: Int? = null
    private var currentMaxQuantity: Int? = null
    
    // 当前价格范围
    private var currentMinPrice: Double? = null
    private var currentMaxPrice: Double? = null
    
    // 输入框焦点监听器（用于清理）
    private var quantityFocusListeners: List<View.OnFocusChangeListener> = emptyList()
    private var priceFocusListeners: List<View.OnFocusChangeListener> = emptyList()
    
    override fun getComponentId(): String = COMPONENT_ID
    
    /**
     * 初始化组件
     */
    fun initialize() {
        setupQuantityInputs()
        setupPriceInputs()
        setReady()
    }
    
    /**
     * 设置数量输入框
     */
    private fun setupQuantityInputs() {
        val minQuantityFocusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && isReady()) {
                val minQuantity = binding.valueRangeSection.minQuantityInput.text.toString().toIntOrNull()
                if (minQuantity != currentMinQuantity) {
                    currentMinQuantity = minQuantity
                    updateQuantityRange()
                }
            }
        }
        
        val maxQuantityFocusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && isReady()) {
                val maxQuantity = binding.valueRangeSection.maxQuantityInput.text.toString().toIntOrNull()
                if (maxQuantity != currentMaxQuantity) {
                    currentMaxQuantity = maxQuantity
                    updateQuantityRange()
                }
            }
        }
        
        binding.valueRangeSection.minQuantityInput.onFocusChangeListener = minQuantityFocusListener
        binding.valueRangeSection.maxQuantityInput.onFocusChangeListener = maxQuantityFocusListener
        
        quantityFocusListeners = listOf(minQuantityFocusListener, maxQuantityFocusListener)
        
        // 添加文本变化监听（实时验证）
        binding.valueRangeSection.minQuantityInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // 如果正在从FilterState更新UI，跳过这个回调
                if (!isUpdatingFromState) {
                    validateQuantityInput()
                }
            }
        })
        
        binding.valueRangeSection.maxQuantityInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // 如果正在从FilterState更新UI，跳过这个回调
                if (!isUpdatingFromState) {
                    validateQuantityInput()
                }
            }
        })
    }
    
    /**
     * 设置价格输入框
     */
    private fun setupPriceInputs() {
        val minPriceFocusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && isReady()) {
                val minPrice = binding.valueRangeSection.minPriceInput.text.toString().toDoubleOrNull()
                if (minPrice != currentMinPrice) {
                    currentMinPrice = minPrice
                    updatePriceRange()
                }
            }
        }
        
        val maxPriceFocusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && isReady()) {
                val maxPrice = binding.valueRangeSection.maxPriceInput.text.toString().toDoubleOrNull()
                if (maxPrice != currentMaxPrice) {
                    currentMaxPrice = maxPrice
                    updatePriceRange()
                }
            }
        }
        
        binding.valueRangeSection.minPriceInput.onFocusChangeListener = minPriceFocusListener
        binding.valueRangeSection.maxPriceInput.onFocusChangeListener = maxPriceFocusListener
        
        priceFocusListeners = listOf(minPriceFocusListener, maxPriceFocusListener)
        
        // 添加文本变化监听（实时验证）
        binding.valueRangeSection.minPriceInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // 如果正在从FilterState更新UI，跳过这个回调
                if (!isUpdatingFromState) {
                    validatePriceInput()
                }
            }
        })
        
        binding.valueRangeSection.maxPriceInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // 如果正在从FilterState更新UI，跳过这个回调
                if (!isUpdatingFromState) {
                    validatePriceInput()
                }
            }
        })
    }
    
    /**
     * 更新数量范围
     */
    private fun updateQuantityRange() {
        if (validateQuantityRange(currentMinQuantity, currentMaxQuantity)) {
            viewModel.updateQuantityRange(currentMinQuantity, currentMaxQuantity)
            notifyValueChanged(Pair(currentMinQuantity, currentMaxQuantity))
        }
    }
    
    /**
     * 更新价格范围
     */
    private fun updatePriceRange() {
        if (validatePriceRange(currentMinPrice, currentMaxPrice)) {
            viewModel.updatePriceRange(currentMinPrice, currentMaxPrice)
            notifyValueChanged(Pair(currentMinPrice, currentMaxPrice))
        }
    }
    
    /**
     * 验证数量输入
     */
    private fun validateQuantityInput() {
        val minText = binding.valueRangeSection.minQuantityInput.text.toString()
        val maxText = binding.valueRangeSection.maxQuantityInput.text.toString()
        
        // 清除之前的错误状态
        binding.valueRangeSection.minQuantityInput.error = null
        binding.valueRangeSection.maxQuantityInput.error = null
        
        val minQuantity = minText.toIntOrNull()
        val maxQuantity = maxText.toIntOrNull()
        
        // 验证数值范围
        if (minText.isNotEmpty() && minQuantity == null) {
            binding.valueRangeSection.minQuantityInput.error = "请输入有效的数字"
        }
        
        if (maxText.isNotEmpty() && maxQuantity == null) {
            binding.valueRangeSection.maxQuantityInput.error = "请输入有效的数字"
        }
        
        if (minQuantity != null && maxQuantity != null && minQuantity > maxQuantity) {
            binding.valueRangeSection.maxQuantityInput.error = "最大数量不能小于最小数量"
        }
        
        if (minQuantity != null && minQuantity < 0) {
            binding.valueRangeSection.minQuantityInput.error = "数量不能为负数"
        }
        
        if (maxQuantity != null && maxQuantity < 0) {
            binding.valueRangeSection.maxQuantityInput.error = "数量不能为负数"
        }
    }
    
    /**
     * 验证价格输入
     */
    private fun validatePriceInput() {
        val minText = binding.valueRangeSection.minPriceInput.text.toString()
        val maxText = binding.valueRangeSection.maxPriceInput.text.toString()
        
        // 清除之前的错误状态
        binding.valueRangeSection.minPriceInput.error = null
        binding.valueRangeSection.maxPriceInput.error = null
        
        val minPrice = minText.toDoubleOrNull()
        val maxPrice = maxText.toDoubleOrNull()
        
        // 验证数值范围
        if (minText.isNotEmpty() && minPrice == null) {
            binding.valueRangeSection.minPriceInput.error = "请输入有效的价格"
        }
        
        if (maxText.isNotEmpty() && maxPrice == null) {
            binding.valueRangeSection.maxPriceInput.error = "请输入有效的价格"
        }
        
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            binding.valueRangeSection.maxPriceInput.error = "最高价格不能小于最低价格"
        }
        
        if (minPrice != null && minPrice < 0) {
            binding.valueRangeSection.minPriceInput.error = "价格不能为负数"
        }
        
        if (maxPrice != null && maxPrice < 0) {
            binding.valueRangeSection.maxPriceInput.error = "价格不能为负数"
        }
    }
    
    /**
     * 验证数量范围是否有效
     */
    private fun validateQuantityRange(minQuantity: Int?, maxQuantity: Int?): Boolean {
        if (minQuantity != null && minQuantity < 0) return false
        if (maxQuantity != null && maxQuantity < 0) return false
        if (minQuantity != null && maxQuantity != null && minQuantity > maxQuantity) return false
        return true
    }
    
    /**
     * 验证价格范围是否有效
     */
    private fun validatePriceRange(minPrice: Double?, maxPrice: Double?): Boolean {
        if (minPrice != null && minPrice < 0) return false
        if (maxPrice != null && maxPrice < 0) return false
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) return false
        return true
    }
    
    override fun updateFromState(filterState: FilterState) {
        isUpdatingFromState = true
        try {
            // 更新数量范围
            if (filterState.minQuantity != currentMinQuantity || filterState.maxQuantity != currentMaxQuantity) {
                currentMinQuantity = filterState.minQuantity
                currentMaxQuantity = filterState.maxQuantity
                
                binding.valueRangeSection.minQuantityInput.setText(currentMinQuantity?.toString() ?: "")
                binding.valueRangeSection.maxQuantityInput.setText(currentMaxQuantity?.toString() ?: "")
            }
            
            // 更新价格范围
            if (filterState.minPrice != currentMinPrice || filterState.maxPrice != currentMaxPrice) {
                currentMinPrice = filterState.minPrice
                currentMaxPrice = filterState.maxPrice
                
                binding.valueRangeSection.minPriceInput.setText(currentMinPrice?.toString() ?: "")
                binding.valueRangeSection.maxPriceInput.setText(currentMaxPrice?.toString() ?: "")
            }
        } finally {
            isUpdatingFromState = false
        }
    }
    
    override fun resetToDefault() {
        currentMinQuantity = null
        currentMaxQuantity = null
        currentMinPrice = null
        currentMaxPrice = null
        
        binding.valueRangeSection.minQuantityInput.setText("")
        binding.valueRangeSection.maxQuantityInput.setText("")
        binding.valueRangeSection.minPriceInput.setText("")
        binding.valueRangeSection.maxPriceInput.setText("")
        
        // 清除错误状态
        binding.valueRangeSection.minQuantityInput.error = null
        binding.valueRangeSection.maxQuantityInput.error = null
        binding.valueRangeSection.minPriceInput.error = null
        binding.valueRangeSection.maxPriceInput.error = null
    }
    
    // RangeFilterComponent implementation
    override fun getMinValue(): Any? {
        return Pair(currentMinQuantity, currentMinPrice)
    }
    
    override fun getMaxValue(): Any? {
        return Pair(currentMaxQuantity, currentMaxPrice)
    }
    
    override fun setRange(minValue: Any?, maxValue: Any?) {
        // 这个方法比较复杂，因为需要处理两种不同的范围类型
        // 使用具体的设置方法
    }
    
    override fun validateRange(minValue: Any?, maxValue: Any?): Boolean {
        // 验证两种范围类型
        return validateQuantityRange(currentMinQuantity, currentMaxQuantity) &&
               validatePriceRange(currentMinPrice, currentMaxPrice)
    }
    
    /**
     * 获取数量范围
     */
    fun getQuantityRange(): Pair<Int?, Int?> {
        return Pair(currentMinQuantity, currentMaxQuantity)
    }
    
    /**
     * 获取价格范围
     */
    fun getPriceRange(): Pair<Double?, Double?> {
        return Pair(currentMinPrice, currentMaxPrice)
    }
    
    /**
     * 设置数量范围
     */
    fun setQuantityRange(minQuantity: Int?, maxQuantity: Int?) {
        if (validateQuantityRange(minQuantity, maxQuantity)) {
            currentMinQuantity = minQuantity
            currentMaxQuantity = maxQuantity
            
            binding.valueRangeSection.minQuantityInput.setText(minQuantity?.toString() ?: "")
            binding.valueRangeSection.maxQuantityInput.setText(maxQuantity?.toString() ?: "")
            
            viewModel.updateQuantityRange(minQuantity, maxQuantity)
            notifyValueChanged(Pair(minQuantity, maxQuantity))
        }
    }
    
    /**
     * 设置价格范围
     */
    fun setPriceRange(minPrice: Double?, maxPrice: Double?) {
        if (validatePriceRange(minPrice, maxPrice)) {
            currentMinPrice = minPrice
            currentMaxPrice = maxPrice
            
            binding.valueRangeSection.minPriceInput.setText(minPrice?.toString() ?: "")
            binding.valueRangeSection.maxPriceInput.setText(maxPrice?.toString() ?: "")
            
            viewModel.updatePriceRange(minPrice, maxPrice)
            notifyValueChanged(Pair(minPrice, maxPrice))
        }
    }
    
    /**
     * 检查是否有数值范围筛选
     */
    fun hasValueRangeFilter(): Boolean {
        return currentMinQuantity != null || currentMaxQuantity != null ||
               currentMinPrice != null || currentMaxPrice != null
    }
    
    /**
     * 获取数值范围筛选摘要
     */
    fun getValueRangeSummary(): String {
        val parts = mutableListOf<String>()
        
        if (currentMinQuantity != null || currentMaxQuantity != null) {
            val minStr = currentMinQuantity?.toString() ?: "不限"
            val maxStr = currentMaxQuantity?.toString() ?: "不限"
            parts.add("数量: $minStr - $maxStr")
        }
        
        if (currentMinPrice != null || currentMaxPrice != null) {
            val minStr = currentMinPrice?.toString() ?: "不限"
            val maxStr = currentMaxPrice?.toString() ?: "不限"
            parts.add("价格: $minStr - $maxStr")
        }
        
        return if (parts.isEmpty()) {
            "未设置数值范围筛选"
        } else {
            parts.joinToString(" | ")
        }
    }
    
    /**
     * 强制验证所有输入
     */
    fun validateAllInputs(): Boolean {
        validateQuantityInput()
        validatePriceInput()
        
        return binding.valueRangeSection.minQuantityInput.error == null &&
               binding.valueRangeSection.maxQuantityInput.error == null &&
               binding.valueRangeSection.minPriceInput.error == null &&
               binding.valueRangeSection.maxPriceInput.error == null
    }
    
    /**
     * 清除所有输入错误
     */
    fun clearAllErrors() {
        binding.valueRangeSection.minQuantityInput.error = null
        binding.valueRangeSection.maxQuantityInput.error = null
        binding.valueRangeSection.minPriceInput.error = null
        binding.valueRangeSection.maxPriceInput.error = null
    }
    
    override fun cleanup() {
        super.cleanup()
        
        // 清理焦点监听器
        binding.valueRangeSection.minQuantityInput.onFocusChangeListener = null
        binding.valueRangeSection.maxQuantityInput.onFocusChangeListener = null
        binding.valueRangeSection.minPriceInput.onFocusChangeListener = null
        binding.valueRangeSection.maxPriceInput.onFocusChangeListener = null
        
        // 清理文本监听器（这里简化处理，实际使用中可能需要保存引用）
        // binding.valueRangeSection.minQuantityInput.removeTextChangedListener(...)
        
        // 清空当前值
        currentMinQuantity = null
        currentMaxQuantity = null
        currentMinPrice = null
        currentMaxPrice = null
    }
}
