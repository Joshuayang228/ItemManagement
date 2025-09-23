package com.example.itemmanagement.data.model.wishlist

import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency

/**
 * 心愿单统计信息数据类
 * 用于在界面中显示心愿单的概览统计
 */
data class WishlistStats(
    val totalItems: Int = 0,                    // 总心愿单物品数量
    val activeItems: Int = 0,                   // 激活状态的物品数量
    val achievedItems: Int = 0,                 // 已实现的愿望数量
    val pausedItems: Int = 0,                   // 暂停跟踪的物品数量
    
    // === 价格统计 ===
    val totalEstimatedValue: Double = 0.0,      // 总预估价值
    val totalCurrentValue: Double = 0.0,        // 总当前价值
    val totalBudgetLimit: Double = 0.0,         // 总预算限制
    val averageItemPrice: Double = 0.0,         // 平均物品价格
    
    // === 价格变动统计 ===
    val priceDroppedItems: Int = 0,             // 降价物品数量
    val priceIncreasedItems: Int = 0,           // 涨价物品数量
    val reachedTargetPriceItems: Int = 0,       // 达到目标价格的物品数量
    val totalPriceSavings: Double = 0.0,        // 总价格节省金额
    
    // === 优先级分布 ===
    val priorityDistribution: Map<WishlistPriority, Int> = emptyMap(),
    val urgencyDistribution: Map<WishlistUrgency, Int> = emptyMap(),
    
    // === 分类统计 ===
    val categoryDistribution: Map<String, Int> = emptyMap(),
    val topCategories: List<CategoryStat> = emptyList(),
    
    // === 时间统计 ===
    val itemsAddedThisWeek: Int = 0,            // 本周新增物品数量
    val itemsAddedThisMonth: Int = 0,           // 本月新增物品数量
    val averageDaysOnWishlist: Double = 0.0,    // 平均在心愿单中的天数
    
    // === 活跃度统计 ===
    val itemsWithPriceTracking: Int = 0,        // 启用价格跟踪的物品数量
    val lastPriceUpdateDate: Long? = null,      // 最后价格更新时间
    val mostViewedItem: String? = null,         // 最常查看的物品
    
    // === 购买转化统计 ===
    val purchaseConversionRate: Double = 0.0,   // 购买转化率（已实现/总数）
    val averageDaysToPurchase: Double = 0.0,    // 平均购买决策天数
    val monthlyPurchaseGoal: Int = 0,           // 月度购买目标
    val currentMonthPurchased: Int = 0          // 本月已购买数量
) {
    /**
     * 获取预算使用率
     */
    fun getBudgetUtilization(): Double {
        return if (totalBudgetLimit > 0) {
            (totalCurrentValue / totalBudgetLimit) * 100
        } else 0.0
    }
    
    /**
     * 获取价格节省率
     */
    fun getPriceSavingsRate(): Double {
        return if (totalEstimatedValue > 0) {
            (totalPriceSavings / totalEstimatedValue) * 100
        } else 0.0
    }
    
    /**
     * 获取高优先级物品数量
     */
    fun getHighPriorityItemsCount(): Int {
        return (priorityDistribution[WishlistPriority.HIGH] ?: 0) +
               (priorityDistribution[WishlistPriority.URGENT] ?: 0)
    }
    
    /**
     * 获取需要立即关注的物品数量
     */
    fun getItemsNeedingAttention(): Int {
        return reachedTargetPriceItems + priceDroppedItems + getHighPriorityItemsCount()
    }
    
    /**
     * 检查是否有需要关注的更新
     */
    fun hasUpdatesNeedingAttention(): Boolean {
        return getItemsNeedingAttention() > 0
    }
}

/**
 * 分类统计数据类
 */
data class CategoryStat(
    val categoryName: String,
    val itemCount: Int,
    val totalValue: Double,
    val averagePrice: Double
)

/**
 * 心愿单物品详情数据类
 * 用于创建新的心愿单物品
 */
data class WishlistItemDetails(
    val name: String,
    val category: String,
    val subCategory: String? = null,
    val brand: String? = null,
    val specification: String? = null,
    val estimatedPrice: Double? = null,
    val targetPrice: Double? = null,
    val priority: WishlistPriority = WishlistPriority.NORMAL,
    val urgency: WishlistUrgency = WishlistUrgency.NORMAL,
    val desiredQuantity: Double = 1.0,
    val quantityUnit: String = "个",
    val budgetLimit: Double? = null,
    val preferredStore: String? = null,
    val notes: String? = null,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val addedReason: String? = null
)
