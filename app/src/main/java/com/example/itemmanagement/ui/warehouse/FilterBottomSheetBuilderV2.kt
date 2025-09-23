package com.example.itemmanagement.ui.warehouse

import androidx.fragment.app.FragmentManager
import com.example.itemmanagement.ui.warehouse.FilterCategory
import com.example.itemmanagement.ui.warehouse.FilterState

/**
 * FilterBottomSheetFragmentV2 构建器
 * 
 * 提供便捷的方式创建和配置筛选界面
 */
class FilterBottomSheetBuilderV2 {
    
    private var initialFilterState: FilterState? = null
    private var scrollToSection: FilterCategory? = null
    private var onDismissListener: (() -> Unit)? = null
    private var onApplyListener: ((FilterState) -> Unit)? = null
    private var onResetListener: (() -> Unit)? = null
    
    /**
     * 设置初始筛选状态
     */
    fun withInitialState(filterState: FilterState): FilterBottomSheetBuilderV2 {
        this.initialFilterState = filterState
        return this
    }
    
    /**
     * 设置初始滚动到的区域
     */
    fun scrollToSection(category: FilterCategory): FilterBottomSheetBuilderV2 {
        this.scrollToSection = category
        return this
    }
    
    /**
     * 设置关闭监听器
     */
    fun onDismiss(listener: () -> Unit): FilterBottomSheetBuilderV2 {
        this.onDismissListener = listener
        return this
    }
    
    /**
     * 设置应用筛选监听器
     */
    fun onApply(listener: (FilterState) -> Unit): FilterBottomSheetBuilderV2 {
        this.onApplyListener = listener
        return this
    }
    
    /**
     * 设置重置监听器
     */
    fun onReset(listener: () -> Unit): FilterBottomSheetBuilderV2 {
        this.onResetListener = listener
        return this
    }
    
    /**
     * 构建并显示Fragment
     */
    fun show(fragmentManager: FragmentManager, tag: String = FilterBottomSheetFragmentV2.TAG): FilterBottomSheetFragmentV2 {
        val fragment = FilterBottomSheetFragmentV2.newInstance()
        
        // 应用配置
        applyConfiguration(fragment)
        
        // 显示Fragment
        fragment.show(fragmentManager, tag)
        
        return fragment
    }
    
    /**
     * 仅构建Fragment，不显示
     */
    fun build(): FilterBottomSheetFragmentV2 {
        val fragment = FilterBottomSheetFragmentV2.newInstance()
        applyConfiguration(fragment)
        return fragment
    }
    
    /**
     * 应用配置到Fragment
     */
    private fun applyConfiguration(fragment: FilterBottomSheetFragmentV2) {
        // 这里可以通过Fragment的方法或Arguments来传递配置
        // 由于Fragment还没有完全初始化，我们可以通过其他方式传递参数
        
        // 如果需要传递复杂参数，可以使用Fragment的arguments
        // 或者提供公开的配置方法
    }
    
    companion object {
        /**
         * 创建构建器实例
         */
        fun create(): FilterBottomSheetBuilderV2 {
            return FilterBottomSheetBuilderV2()
        }
        
        /**
         * 快速显示筛选界面（默认配置）
         */
        fun showDefault(fragmentManager: FragmentManager): FilterBottomSheetFragmentV2 {
            return create().show(fragmentManager)
        }
        
        /**
         * 显示并滚动到指定区域
         */
        fun showAndScrollTo(
            fragmentManager: FragmentManager,
            category: FilterCategory
        ): FilterBottomSheetFragmentV2 {
            return create()
                .scrollToSection(category)
                .show(fragmentManager)
        }
    }
}

/**
 * 扩展函数：为FragmentManager添加便捷方法
 */
fun FragmentManager.showFilterBottomSheetV2(
    configure: FilterBottomSheetBuilderV2.() -> Unit = {}
): FilterBottomSheetFragmentV2 {
    val builder = FilterBottomSheetBuilderV2.create()
    builder.configure()
    return builder.show(this)
}

/**
 * 筛选界面配置类
 */
data class FilterBottomSheetConfig(
    val initialState: FilterState? = null,
    val scrollToSection: FilterCategory? = null,
    val enableAnimations: Boolean = true,
    val enableTouchOptimization: Boolean = true,
    val showDebugInfo: Boolean = false
)

/**
 * 使用示例：
 * 
 * // 基本用法
 * FilterBottomSheetBuilderV2.showDefault(fragmentManager)
 * 
 * // 高级配置
 * FilterBottomSheetBuilderV2.create()
 *     .scrollToSection(FilterCategory.STATUS_RATING)
 *     .onApply { filterState ->
 *         // 处理应用筛选
 *     }
 *     .onReset {
 *         // 处理重置
 *     }
 *     .show(fragmentManager)
 * 
 * // 使用扩展函数
 * fragmentManager.showFilterBottomSheetV2 {
 *     scrollToSection(FilterCategory.VALUE_RANGE)
 *     onApply { filterState ->
 *         // 处理筛选应用
 *     }
 * }
 */
