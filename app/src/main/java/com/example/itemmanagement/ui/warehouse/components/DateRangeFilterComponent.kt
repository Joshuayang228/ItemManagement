package com.example.itemmanagement.ui.warehouse.components

import android.view.View
import androidx.fragment.app.Fragment
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.example.itemmanagement.ui.warehouse.FilterState
import com.example.itemmanagement.ui.warehouse.WarehouseViewModel
import com.example.itemmanagement.ui.warehouse.components.base.BaseFilterComponent
import com.example.itemmanagement.ui.warehouse.components.base.DateRangeFilterComponent
import com.example.itemmanagement.utils.SnackbarHelper
import com.example.itemmanagement.ui.utils.showMaterial3DatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日期范围筛选组件
 * 
 * 功能：
 * 1. 管理多种日期范围（过期、添加、购买、生产）
 * 2. 集成Material 3日期选择器
 * 3. 日期验证和格式化
 * 4. 与ViewModel同步状态
 */
class DateRangeFilterComponent(
    private val fragment: Fragment,
    private val binding: FragmentFilterBottomSheetBinding,
    private val viewModel: WarehouseViewModel
) : BaseFilterComponent(), DateRangeFilterComponent {
    
    companion object {
        private const val COMPONENT_ID = "date_range_filter"
    }
    
    // 防止无限循环的状态更新标志
    private var isUpdatingFromState = false
    
    // 日期格式化器
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // 当前日期范围值
    private var expirationStartDate: Long? = null
    private var expirationEndDate: Long? = null
    private var creationStartDate: Long? = null
    private var creationEndDate: Long? = null
    private var purchaseStartDate: Long? = null
    private var purchaseEndDate: Long? = null
    private var productionStartDate: Long? = null
    private var productionEndDate: Long? = null
    
    // 日期输入框映射
    private data class DateInputPair(
        val startInput: com.google.android.material.textfield.TextInputEditText,
        val endInput: com.google.android.material.textfield.TextInputEditText,
        val updateMethod: (Long?, Long?) -> Unit,
        var startDate: Long?,
        var endDate: Long?
    )
    
    private lateinit var dateInputs: List<DateInputPair>
    
    override fun getComponentId(): String = COMPONENT_ID
    
    /**
     * 初始化组件
     */
    fun initialize() {
        setupDateInputMappings()
        setupDateInputListeners()
        setReady()
    }
    
    /**
     * 设置日期输入框映射
     */
    private fun setupDateInputMappings() {
        dateInputs = listOf(
            DateInputPair(
                binding.dateSection.expirationStartDateInput,
                binding.dateSection.expirationEndDateInput,
                { start, end -> 
                    expirationStartDate = start
                    expirationEndDate = end
                    viewModel.updateExpirationDateRange(start, end)
                },
                null,
                null
            ),
            DateInputPair(
                binding.dateSection.creationStartDateInput,
                binding.dateSection.creationEndDateInput,
                { start, end -> 
                    creationStartDate = start
                    creationEndDate = end
                    viewModel.updateCreationDateRange(start, end)
                },
                null,
                null
            ),
            DateInputPair(
                binding.dateSection.purchaseStartDateInput,
                binding.dateSection.purchaseEndDateInput,
                { start, end -> 
                    purchaseStartDate = start
                    purchaseEndDate = end
                    viewModel.updatePurchaseDateRange(start, end)
                },
                null,
                null
            ),
            DateInputPair(
                binding.dateSection.productionStartDateInput,
                binding.dateSection.productionEndDateInput,
                { start, end -> 
                    productionStartDate = start
                    productionEndDate = end
                    viewModel.updateProductionDateRange(start, end)
                },
                null,
                null
            )
        )
    }
    
    /**
     * 设置日期输入框监听器
     */
    private fun setupDateInputListeners() {
        dateInputs.forEach { dateInputPair ->
            setupDateInputListeners(dateInputPair)
        }
    }
    
    /**
     * 为单个日期输入对设置监听器
     */
    private fun setupDateInputListeners(dateInputPair: DateInputPair) {
        // 开始日期输入框
        dateInputPair.startInput.setOnClickListener {
            showDatePicker("选择开始日期") { date ->
                val dateTimestamp = date.time
                
                // 验证日期范围
                if (validateDateRange(dateTimestamp, dateInputPair.endDate)) {
                    dateInputPair.startDate = dateTimestamp
                    dateInputPair.startInput.setText(dateFormat.format(date))
                    
                    // 如果不是从FilterState更新UI，才调用ViewModel更新
                    if (!isUpdatingFromState) {
                        dateInputPair.updateMethod(dateInputPair.startDate, dateInputPair.endDate)
                        notifyValueChanged(Triple("start", dateInputPair, dateTimestamp))
                    }
                } else {
                    showDateRangeError("开始日期不能晚于结束日期")
                }
            }
        }
        
        // 结束日期输入框
        dateInputPair.endInput.setOnClickListener {
            showDatePicker("选择结束日期") { date ->
                val dateTimestamp = date.time
                
                // 验证日期范围
                if (validateDateRange(dateInputPair.startDate, dateTimestamp)) {
                    dateInputPair.endDate = dateTimestamp
                    dateInputPair.endInput.setText(dateFormat.format(date))
                    
                    // 如果不是从FilterState更新UI，才调用ViewModel更新
                    if (!isUpdatingFromState) {
                        dateInputPair.updateMethod(dateInputPair.startDate, dateInputPair.endDate)
                        notifyValueChanged(Triple("end", dateInputPair, dateTimestamp))
                    }
                } else {
                    showDateRangeError("结束日期不能早于开始日期")
                }
            }
        }
    }
    
    /**
     * 显示日期选择器
     */
    private fun showDatePicker(title: String, onDateSelected: (Date) -> Unit) {
        fragment.showMaterial3DatePicker(title = title) { selectedDate ->
            onDateSelected(selectedDate)
        }
    }
    
    /**
     * 显示日期范围错误
     */
    private fun showDateRangeError(message: String) {
        // 可以显示Toast或其他形式的错误提示
        SnackbarHelper.show(binding.root, message)
    }
    
    /**
     * 验证日期范围是否有效
     */
    private fun validateDateRange(startDate: Long?, endDate: Long?): Boolean {
        return if (startDate != null && endDate != null) {
            startDate <= endDate
        } else {
            true // 如果有一个为null，则认为有效
        }
    }
    
    override fun updateFromState(filterState: FilterState) {
        isUpdatingFromState = true
        try {
            // 更新过期日期范围
            if (filterState.expirationStartDate != expirationStartDate || 
                filterState.expirationEndDate != expirationEndDate) {
                updateExpirationDateRange(filterState.expirationStartDate, filterState.expirationEndDate)
            }
            
            // 更新添加日期范围
            if (filterState.creationStartDate != creationStartDate || 
                filterState.creationEndDate != creationEndDate) {
                updateCreationDateRange(filterState.creationStartDate, filterState.creationEndDate)
            }
            
            // 更新购买日期范围
            if (filterState.purchaseStartDate != purchaseStartDate || 
                filterState.purchaseEndDate != purchaseEndDate) {
                updatePurchaseDateRange(filterState.purchaseStartDate, filterState.purchaseEndDate)
            }
            
            // 更新生产日期范围
            if (filterState.productionStartDate != productionStartDate || 
                filterState.productionEndDate != productionEndDate) {
                updateProductionDateRange(filterState.productionStartDate, filterState.productionEndDate)
            }
        } finally {
            isUpdatingFromState = false
        }
    }
    
    /**
     * 更新过期日期范围
     */
    private fun updateExpirationDateRange(startDate: Long?, endDate: Long?) {
        expirationStartDate = startDate
        expirationEndDate = endDate
        
        binding.dateSection.expirationStartDateInput.setText(
            startDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.dateSection.expirationEndDateInput.setText(
            endDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        
        dateInputs[0].startDate = startDate
        dateInputs[0].endDate = endDate
    }
    
    /**
     * 更新添加日期范围
     */
    private fun updateCreationDateRange(startDate: Long?, endDate: Long?) {
        creationStartDate = startDate
        creationEndDate = endDate
        
        binding.dateSection.creationStartDateInput.setText(
            startDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.dateSection.creationEndDateInput.setText(
            endDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        
        dateInputs[1].startDate = startDate
        dateInputs[1].endDate = endDate
    }
    
    /**
     * 更新购买日期范围
     */
    private fun updatePurchaseDateRange(startDate: Long?, endDate: Long?) {
        purchaseStartDate = startDate
        purchaseEndDate = endDate
        
        binding.dateSection.purchaseStartDateInput.setText(
            startDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.dateSection.purchaseEndDateInput.setText(
            endDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        
        dateInputs[2].startDate = startDate
        dateInputs[2].endDate = endDate
    }
    
    /**
     * 更新生产日期范围
     */
    private fun updateProductionDateRange(startDate: Long?, endDate: Long?) {
        productionStartDate = startDate
        productionEndDate = endDate
        
        binding.dateSection.productionStartDateInput.setText(
            startDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        binding.dateSection.productionEndDateInput.setText(
            endDate?.let { dateFormat.format(Date(it)) } ?: ""
        )
        
        dateInputs[3].startDate = startDate
        dateInputs[3].endDate = endDate
    }
    
    override fun resetToDefault() {
        // 重置所有日期值
        expirationStartDate = null
        expirationEndDate = null
        creationStartDate = null
        creationEndDate = null
        purchaseStartDate = null
        purchaseEndDate = null
        productionStartDate = null
        productionEndDate = null
        
        // 清空所有输入框
        binding.dateSection.expirationStartDateInput.setText("")
        binding.dateSection.expirationEndDateInput.setText("")
        binding.dateSection.creationStartDateInput.setText("")
        binding.dateSection.creationEndDateInput.setText("")
        binding.dateSection.purchaseStartDateInput.setText("")
        binding.dateSection.purchaseEndDateInput.setText("")
        binding.dateSection.productionStartDateInput.setText("")
        binding.dateSection.productionEndDateInput.setText("")
        
        // 重置映射数据
        dateInputs.forEach { dateInputPair ->
            dateInputPair.startDate = null
            dateInputPair.endDate = null
        }
    }
    
    // DateRangeFilterComponent implementation (主要针对过期日期)
    override fun getStartDate(): Long? {
        return expirationStartDate
    }
    
    override fun getEndDate(): Long? {
        return expirationEndDate
    }
    
    override fun setDateRange(startDate: Long?, endDate: Long?) {
        if (validateDateRange(startDate, endDate)) {
            updateExpirationDateRange(startDate, endDate)
            viewModel.updateExpirationDateRange(startDate, endDate)
        }
    }
    
    override fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }
    
    // RangeFilterComponent implementation
    override fun getMinValue(): Any? {
        return getStartDate()
    }
    
    override fun getMaxValue(): Any? {
        return getEndDate()
    }
    
    override fun setRange(minValue: Any?, maxValue: Any?) {
        val startDate = minValue as? Long
        val endDate = maxValue as? Long
        setDateRange(startDate, endDate)
    }
    
    override fun validateRange(minValue: Any?, maxValue: Any?): Boolean {
        val startDate = minValue as? Long
        val endDate = maxValue as? Long
        return validateDateRange(startDate, endDate)
    }
    
    /**
     * 获取所有日期范围
     */
    fun getAllDateRanges(): Map<String, Pair<Long?, Long?>> {
        return mapOf(
            "expiration" to Pair(expirationStartDate, expirationEndDate),
            "creation" to Pair(creationStartDate, creationEndDate),
            "purchase" to Pair(purchaseStartDate, purchaseEndDate),
            "production" to Pair(productionStartDate, productionEndDate)
        )
    }
    
    /**
     * 设置特定类型的日期范围
     */
    fun setDateRangeByType(type: String, startDate: Long?, endDate: Long?) {
        if (!validateDateRange(startDate, endDate)) {
            showDateRangeError("日期范围无效")
            return
        }
        
        when (type) {
            "expiration" -> {
                updateExpirationDateRange(startDate, endDate)
                viewModel.updateExpirationDateRange(startDate, endDate)
            }
            "creation" -> {
                updateCreationDateRange(startDate, endDate)
                viewModel.updateCreationDateRange(startDate, endDate)
            }
            "purchase" -> {
                updatePurchaseDateRange(startDate, endDate)
                viewModel.updatePurchaseDateRange(startDate, endDate)
            }
            "production" -> {
                updateProductionDateRange(startDate, endDate)
                viewModel.updateProductionDateRange(startDate, endDate)
            }
        }
    }
    
    /**
     * 检查是否有日期筛选
     */
    fun hasDateFilter(): Boolean {
        return expirationStartDate != null || expirationEndDate != null ||
               creationStartDate != null || creationEndDate != null ||
               purchaseStartDate != null || purchaseEndDate != null ||
               productionStartDate != null || productionEndDate != null
    }
    
    /**
     * 获取日期筛选摘要
     */
    fun getDateFilterSummary(): String {
        val parts = mutableListOf<String>()
        
        if (expirationStartDate != null || expirationEndDate != null) {
            val startStr = expirationStartDate?.let { formatDate(it) } ?: "不限"
            val endStr = expirationEndDate?.let { formatDate(it) } ?: "不限"
            parts.add("过期: $startStr - $endStr")
        }
        
        if (creationStartDate != null || creationEndDate != null) {
            val startStr = creationStartDate?.let { formatDate(it) } ?: "不限"
            val endStr = creationEndDate?.let { formatDate(it) } ?: "不限"
            parts.add("添加: $startStr - $endStr")
        }
        
        if (purchaseStartDate != null || purchaseEndDate != null) {
            val startStr = purchaseStartDate?.let { formatDate(it) } ?: "不限"
            val endStr = purchaseEndDate?.let { formatDate(it) } ?: "不限"
            parts.add("购买: $startStr - $endStr")
        }
        
        if (productionStartDate != null || productionEndDate != null) {
            val startStr = productionStartDate?.let { formatDate(it) } ?: "不限"
            val endStr = productionEndDate?.let { formatDate(it) } ?: "不限"
            parts.add("生产: $startStr - $endStr")
        }
        
        return if (parts.isEmpty()) {
            "未设置日期筛选"
        } else {
            parts.joinToString(" | ")
        }
    }
    
    /**
     * 清除特定类型的日期范围
     */
    fun clearDateRangeByType(type: String) {
        setDateRangeByType(type, null, null)
    }
    
    /**
     * 验证所有日期范围
     */
    fun validateAllDateRanges(): Boolean {
        return dateInputs.all { dateInputPair ->
            validateDateRange(dateInputPair.startDate, dateInputPair.endDate)
        }
    }
    
    override fun cleanup() {
        super.cleanup()
        
        // 清理所有点击监听器
        dateInputs.forEach { dateInputPair ->
            dateInputPair.startInput.setOnClickListener(null)
            dateInputPair.endInput.setOnClickListener(null)
        }
        
        // 清空所有日期值
        expirationStartDate = null
        expirationEndDate = null
        creationStartDate = null
        creationEndDate = null
        purchaseStartDate = null
        purchaseEndDate = null
        productionStartDate = null
        productionEndDate = null
    }
}
