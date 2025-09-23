package com.example.itemmanagement.data.model

/**
 * 原生系统式功能列表项的数据模型
 * 用于在RecyclerView中显示原生风格的功能项
 */
sealed interface FunctionGroupItem

/**
 * 功能项包装类，简化版本（不再需要复杂的ViewType）
 */
data class FunctionGroupRow(
    val functionCard: FunctionCard,
    val showDivider: Boolean = true
) : FunctionGroupItem

/**
 * 自定义高度的间距项
 */
data class CustomSpacerItem(
    val height: Int = 12 // 默认12dp间距
) : FunctionGroupItem