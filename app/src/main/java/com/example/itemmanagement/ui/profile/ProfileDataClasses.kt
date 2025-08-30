package com.example.itemmanagement.ui.profile

import java.util.Date

/**
 * 用户统计摘要数据类
 */
data class UserStatsSummary(
    val nickname: String,
    val level: Int,
    val levelTitle: String,
    val experiencePoints: Int,
    val expToNextLevel: Int,
    val progress: Float,
    val usageDays: Int,
    val consecutiveDays: Int,
    val totalItemsManaged: Int,
    val currentItemCount: Int,
    val expiredItemsAvoided: Int,
    val totalSavedValue: Double,
    val unlockedBadgeCount: Int,
    val isActiveUser: Boolean,
    val joinDate: Date,
    val levelColor: String
)

/**
 * 库存概览数据类
 */
data class InventoryOverview(
    val totalItems: Int,
    val totalValue: Double,
    val expiringSoon: Int,
    val lowStock: Int,
    val categoryStats: List<CategoryStat>
)

/**
 * 分类统计数据类
 */
data class CategoryStat(
    val category: String,
    val count: Int
)
