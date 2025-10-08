package com.example.itemmanagement.ui.add

/**
 * 购物字段管理器（已废弃）
 * 
 * @deprecated 购物管理字段已集成到统一的字段管理体系中
 * 
 * ### 购物管理字段定义位置
 * 现在购物字段统一在 `EditFieldsFragment.getShoppingFields()` 中定义，
 * 包含14个购物管理专有字段：
 * 
 * #### 价格预算（4个）
 * - 预估价格、预算上限、实际价格、总价
 * 
 * #### 优先级规划（3个）
 * - 优先级、紧急程度、截止日期
 * 
 * #### 购买执行（3个）
 * - 是否已购买、购买日期、购买商店
 * 
 * #### 周期性管理（2个）
 * - 是否重复购买、重复间隔
 * 
 * #### 提醒与备注（2个）
 * - 提醒日期、推荐原因
 * 
 * ### 默认选择字段
 * 购物模式默认选中8个字段（在 `AddShoppingItemViewModel.initializeDefaultShoppingFields()` 中定义）：
 * - 名称、数量、分类、备注（基础信息）
 * - 预估价格、优先级、截止日期、购买商店（购物管理）
 * 
 * @see EditFieldsFragment.getShoppingFields
 * @see com.example.itemmanagement.ui.shopping.AddShoppingItemViewModel.initializeDefaultShoppingFields
 */
@Deprecated(
    message = "购物字段已集成到EditFieldsFragment中，不再需要独立的管理器",
    replaceWith = ReplaceWith("EditFieldsFragment.getShoppingFields()"),
    level = DeprecationLevel.WARNING
)
class ShoppingFieldManager {
    companion object {
        // 保留空实现以保证向后兼容，但不再使用
    }
}
