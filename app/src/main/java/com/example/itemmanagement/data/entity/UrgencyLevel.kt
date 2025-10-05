package com.example.itemmanagement.data.entity

/**
 * 购物物品紧急程度枚举
 * 用于标识获得该物品的时间紧迫性（Urgent）
 */
enum class UrgencyLevel(
    val displayName: String,
    val level: Int,
    val description: String,
    val colorCode: String,
    val recommendedDays: String  // 建议购买时间
) {
    NOT_URGENT("不急", 1, "1周以上都可以", "#4CAF50", "1周+"),
    NORMAL("一般", 2, "3-7天内购买", "#2196F3", "3-7天"),
    URGENT("紧急", 3, "1-3天内购买", "#FF9800", "1-3天"),
    CRITICAL("立即", 4, "今天就要买", "#F44336", "今天");
    
    companion object {
        /**
         * 根据级别获取紧急程度
         */
        fun fromLevel(level: Int): UrgencyLevel {
            return values().firstOrNull { it.level == level } ?: NORMAL
        }
        
        /**
         * 获取所有紧急程度的显示名称
         */
        fun getDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
        
        /**
         * 判断是否需要立即关注
         */
        fun isHighUrgency(urgency: UrgencyLevel): Boolean {
            return urgency.level >= URGENT.level
        }
        
        /**
         * 从中文显示名称获取枚举值
         */
        fun fromDisplayName(name: String): UrgencyLevel {
            return values().firstOrNull { it.displayName == name } ?: NORMAL
        }
    }
}

