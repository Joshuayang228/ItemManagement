package com.example.itemmanagement.ui.warehouse.components.base

import com.example.itemmanagement.ui.warehouse.FilterState

/**
 * 筛选组件基础接口
 * 
 * 所有筛选组件都必须实现此接口，以便统一管理
 */
interface FilterComponent {
    
    /**
     * 根据筛选状态更新组件UI
     * 
     * @param filterState 当前筛选状态
     */
    fun updateFromState(filterState: FilterState)
    
    /**
     * 重置组件到默认状态
     */
    fun resetToDefault()
    
    /**
     * 设置值变化监听器
     * 
     * @param listener 当组件值发生变化时的回调
     */
    fun setOnValueChangedListener(listener: (Any) -> Unit)
    
    /**
     * 获取组件的唯一标识符
     */
    fun getComponentId(): String
    
    /**
     * 检查组件是否已准备就绪
     */
    fun isReady(): Boolean
    
    /**
     * 清理组件资源
     */
    fun cleanup()
}

/**
 * 筛选组件抽象基类
 * 
 * 提供通用实现，减少重复代码
 */
abstract class BaseFilterComponent : FilterComponent {
    
    // 值变化监听器
    protected var valueChangedListener: ((Any) -> Unit)? = null
    
    // 组件准备状态
    protected var isComponentReady = false
    
    override fun setOnValueChangedListener(listener: (Any) -> Unit) {
        this.valueChangedListener = listener
    }
    
    override fun isReady(): Boolean {
        return isComponentReady
    }
    
    /**
     * 通知值变化
     */
    protected fun notifyValueChanged(value: Any) {
        valueChangedListener?.invoke(value)
    }
    
    /**
     * 设置组件为准备状态
     */
    protected fun setReady() {
        isComponentReady = true
    }
    
    override fun cleanup() {
        valueChangedListener = null
        isComponentReady = false
    }
}

/**
 * 多选筛选组件接口
 */
interface MultiSelectFilterComponent : FilterComponent {
    
    /**
     * 获取当前选中的值集合
     */
    fun getSelectedValues(): Set<String>
    
    /**
     * 设置选中的值集合
     */
    fun setSelectedValues(values: Set<String>)
    
    /**
     * 清除所有选择
     */
    fun clearSelection()
    
    /**
     * 获取所有可选项
     */
    fun getAllOptions(): List<String>
    
    /**
     * 更新可选项列表
     */
    fun updateOptions(options: List<String>)
}

/**
 * 范围筛选组件接口
 */
interface RangeFilterComponent : FilterComponent {
    
    /**
     * 获取范围最小值
     */
    fun getMinValue(): Any?
    
    /**
     * 获取范围最大值
     */
    fun getMaxValue(): Any?
    
    /**
     * 设置范围值
     */
    fun setRange(minValue: Any?, maxValue: Any?)
    
    /**
     * 验证范围值是否有效
     */
    fun validateRange(minValue: Any?, maxValue: Any?): Boolean
}

/**
 * 日期范围筛选组件接口
 */
interface DateRangeFilterComponent : RangeFilterComponent {
    
    /**
     * 获取开始日期时间戳
     */
    fun getStartDate(): Long?
    
    /**
     * 获取结束日期时间戳
     */
    fun getEndDate(): Long?
    
    /**
     * 设置日期范围
     */
    fun setDateRange(startDate: Long?, endDate: Long?)
    
    /**
     * 格式化日期显示
     */
    fun formatDate(timestamp: Long): String
}

/**
 * 组件状态变化事件
 */
data class ComponentStateChangeEvent(
    val componentId: String,
    val oldValue: Any?,
    val newValue: Any?,
    val changeType: ChangeType
)

/**
 * 变化类型枚举
 */
enum class ChangeType {
    SELECTION_CHANGED,  // 选择变化
    RANGE_CHANGED,     // 范围变化
    INPUT_CHANGED,     // 输入变化
    RESET,            // 重置
    OPTIONS_UPDATED   // 选项更新
}
