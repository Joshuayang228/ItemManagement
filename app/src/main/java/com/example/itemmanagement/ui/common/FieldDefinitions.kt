package com.example.itemmanagement.ui.common

import java.io.Serializable

/**
 * UI字段定义公共库
 * 
 * 此文件包含了物品管理系统中所有UI字段的核心类型定义。
 * 这些定义被新架构的各个组件广泛使用，确保了UI的一致性和类型安全。
 * 
 * 核心组件：
 * - FieldProperties: 字段属性配置
 * - ValidationType: 输入验证类型
 * - DisplayStyle: UI显示样式
 * 
 * 迁移说明：
 * 这些定义原本位于已废弃的 AddItemViewModel 中，现在被提取到此独立文件中，
 * 以实现新架构与旧代码的完全解耦。
 */

/**
 * 字段属性定义
 * 用于配置UI字段的各种行为和样式
 */
data class FieldProperties(
    val fieldName: String? = null,
    val defaultValue: String? = null,
    val options: List<String>? = null,
    val min: Number? = null,
    val max: Number? = null,
    val unit: String? = null,
    val isRequired: Boolean = false,
    val validationType: ValidationType? = null,
    val hint: String? = null,
    val isMultiline: Boolean = false,
    val isCustomizable: Boolean = false,  // 是否允许自定义选项
    val defaultDate: Boolean = false,     // 是否默认选择当前日期
    val maxLines: Int? = null,           // 最大行数
    val unitOptions: List<String>? = null, // 单位选项
    val isMultiSelect: Boolean = false,   // 是否允许多选
    val displayStyle: DisplayStyle = DisplayStyle.DEFAULT, // 显示样式
    val periodRange: IntRange? = null,    // 期限范围（如1-36个月）
    val periodUnits: List<String>? = null // 期限单位（如年、月、日）
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

/**
 * 输入验证类型枚举
 * 定义字段的数据验证规则
 */
enum class ValidationType : Serializable {
    TEXT, NUMBER, DATE, DATETIME, EMAIL, PHONE, URL
}

/**
 * UI显示样式枚举
 * 定义字段在界面上的展示方式
 */
enum class DisplayStyle : Serializable {
    DEFAULT,           // 默认显示
    TAG,              // 标签样式
    RATING_STAR,      // 评分星星
    PERIOD_SELECTOR,  // 期限选择器
    LOCATION_SELECTOR // 位置选择器（三级）
}
