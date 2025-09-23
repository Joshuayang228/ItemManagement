package com.example.itemmanagement.data.entity.wishlist

/**
 * 心愿单优先级枚举
 * 用于标识心愿单物品的重要程度
 */
enum class WishlistPriority(
    val displayName: String,
    val level: Int,
    val colorCode: String
) {
    LOW("低优先级", 1, "#9E9E9E"),
    NORMAL("普通", 2, "#2196F3"), 
    HIGH("高优先级", 3, "#FF9800"),
    URGENT("紧急", 4, "#F44336");
    
    companion object {
        /**
         * 根据级别获取优先级
         */
        fun fromLevel(level: Int): WishlistPriority {
            return values().firstOrNull { it.level == level } ?: NORMAL
        }
        
        /**
         * 获取所有优先级的显示名称
         */
        fun getDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
    }
}
