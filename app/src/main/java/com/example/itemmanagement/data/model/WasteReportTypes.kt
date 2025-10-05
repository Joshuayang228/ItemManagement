package com.example.itemmanagement.data.model

import java.util.Date

/**
 * 报告时间段类型
 */
enum class ReportPeriodType {
    WEEK,           // 周报告
    MONTH,          // 月报告
    QUARTER,        // 季度报告
    HALF_YEAR,      // 半年报告
    YEAR,           // 年报告
    CUSTOM,         // 自定义时间段
    LAST_WEEK,      // 上周
    LAST_MONTH,     // 上月
    LAST_THREE_MONTHS, // 过去三个月
    PAST_YEAR,      // 过去一年
    THIS_YEAR       // 今年
}

/**
 * 废料原因枚举
 */
enum class WasteReason {
    EXPIRED,    // 过期
    DAMAGED,    // 损坏
    UNUSED,     // 未使用
    SPOILED,    // 变质
    BROKEN,     // 破损
    OTHER       // 其他
}

/**
 * 日期范围
 */
data class DateRange(
    val startDate: Date,
    val endDate: Date
)

/**
 * 废料报告数据
 */
data class WasteReportData(
    val summary: WasteSummaryInfo,
    val wastedItems: List<WastedItemInfo>,
    val categoryData: List<WasteCategoryInfo>,
    val dateData: List<WasteDateInfo>
)

/**
 * 废料分类数据
 */
data class WasteCategoryData(
    val category: String,
    val count: Int,
    val totalValue: Double,
    val percentage: Double
)

/**
 * 废料时间数据
 */
data class WasteTimeData(
    val period: String,
    val count: Int,
    val value: Double
)

/**
 * 废料物品数据
 */
data class WastedItemData(
    val id: Long,
    val name: String,
    val category: String,
    val wasteReason: WasteReason,
    val wasteDate: Date,
    val originalValue: Double,
    val quantity: Double,
    val unit: String,
    val photoUri: String?
)

/**
 * 废料报告摘要信息
 */
data class WasteSummaryInfo(
    val totalItems: Int,
    val totalValue: Double,
    val expiredItems: Int = 0,
    val discardedItems: Int = 0,
    val periodStartDate: Date,
    val periodEndDate: Date
)

/**
 * 废料分类信息
 */
data class WasteCategoryInfo(
    val category: String,
    val count: Int,
    val value: Double
)

/**
 * 废料日期信息
 */
data class WasteDateInfo(
    val date: String,
    val count: Int,
    val value: Double
)

/**
 * 洞察类型枚举
 */
enum class InsightType {
    WARNING,    // 警告
    INFO,       // 信息
    TIP,        // 提示
    ALERT       // 警报
}

/**
 * 洞察严重程度枚举
 */
enum class InsightSeverity {
    LOW,        // 低
    MEDIUM,     // 中
    HIGH        // 高
}

/**
 * 废料洞察分析
 */
data class WasteInsight(
    val title: String,
    val description: String,
    val type: InsightType,
    val severity: InsightSeverity
)

/**
 * 废料物品信息（用于ItemWithDetails映射）
 */
data class WastedItemInfo(
    val id: Long,
    val name: String,
    val category: String,
    val wasteDate: Date,
    val value: Double,
    val quantity: Double,
    val unit: String,
    val status: String? = null,
    val totalPrice: Double = value,
    val photoUri: String? = null
)
