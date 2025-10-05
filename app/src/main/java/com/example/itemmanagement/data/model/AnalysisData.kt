package com.example.itemmanagement.data.model

data class CoreMetrics(
    val totalItems: Int,
    val totalValue: Double,
    val categoriesCount: Int,
    val locationsCount: Int,
    val tagsCount: Int, // 新增标签数量统计
    val expiringItems: Int = 0, // 即将过期的物品数量
    val expiredItems: Int = 0, // 已过期的物品数量
    val lowStockItems: Int = 0, // 库存不足的物品数量
    val recentlyAddedItems: Int = 0 // 最近添加的物品数量
)

data class CategoryValue(
    val category: String,
    val count: Int,
    val totalValue: Double
)

data class LocationValue(
    val location: String,
    val count: Int,
    val totalValue: Double // 新增价格统计
)

// 新增标签分析数据类
data class TagValue(
    val tag: String,
    val count: Int,
    val totalValue: Double
)

data class MonthlyTrend(
    val month: String, // 格式: "2023-10"
    val count: Int,
    val totalValue: Double
)

// 库存统计类型别名
typealias InventoryStats = CoreMetrics

data class InventoryAnalysisData(
    val inventoryStats: InventoryStats, // 替换 coreMetrics 为完整的库存统计
    val categoryAnalysis: List<CategoryValue>,
    val locationAnalysis: List<LocationValue>,
    val tagAnalysis: List<TagValue>, // 新增标签分析
    val monthlyTrends: List<MonthlyTrend>
) 