package com.example.itemmanagement.data.model

/**
 * 物品状态枚举
 */
enum class ItemStatus {
    IN_STOCK,   // 在库
    USED_UP,    // 已用完
    EXPIRED,    // 已过期
    GIVEN_AWAY, // 已转赠
    DISCARDED,  // 已丢弃
    BORROWED    // 已借出（添加这个状态以支持借还功能）
}

