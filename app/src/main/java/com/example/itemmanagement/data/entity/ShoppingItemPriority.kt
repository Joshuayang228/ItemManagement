package com.example.itemmanagement.data.entity

/**
 * 购物物品重要程度枚举
 * 用于标识物品的重要性（Important）
 */
enum class ShoppingItemPriority(
    val displayName: String,
    val level: Int,
    val description: String,
    val colorCode: String
) {
    LOW("次要物品", 1, "可有可无，看情况购买", "#9E9E9E"),
    NORMAL("一般物品", 2, "日常所需，按计划购买", "#2196F3"),
    HIGH("重要物品", 3, "生活必需，优先购买", "#FF9800"),
    CRITICAL("关键物品", 4, "必须购买，影响生活质量", "#F44336");
    
    companion object {
        /**
         * 根据级别获取重要程度
         */
        fun fromLevel(level: Int): ShoppingItemPriority {
            return values().firstOrNull { it.level == level } ?: NORMAL
        }
        
        /**
         * 获取所有重要程度的显示名称
         */
        fun getDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
        
        /**
         * 从中文显示名称获取枚举值
         */
        fun fromDisplayName(name: String): ShoppingItemPriority {
            return values().firstOrNull { it.displayName == name } ?: NORMAL
        }
    }
}

