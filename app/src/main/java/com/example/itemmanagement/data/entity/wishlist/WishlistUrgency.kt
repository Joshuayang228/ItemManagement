package com.example.itemmanagement.data.entity.wishlist

/**
 * 心愿单紧急程度枚举
 * 用于标识获得该物品的时间紧迫性
 */
enum class WishlistUrgency(
    val displayName: String,
    val level: Int,
    val description: String
) {
    NOT_URGENT("不急", 1, "随时都可以购买"),
    NORMAL("一般", 2, "近期有购买需求"),
    URGENT("急需", 3, "短期内需要购买"),
    CRITICAL("非常急需", 4, "立即需要购买");
    
    companion object {
        /**
         * 根据级别获取紧急程度
         */
        fun fromLevel(level: Int): WishlistUrgency {
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
        fun isHighUrgency(urgency: WishlistUrgency): Boolean {
            return urgency.level >= URGENT.level
        }
    }
}
