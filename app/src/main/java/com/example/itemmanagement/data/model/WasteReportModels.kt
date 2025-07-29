package com.example.itemmanagement.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * 浪费报告数据
 */
@Parcelize
data class WasteReportData(
    val totalWastedItems: Int,              // 总浪费物品数量
    val totalWastedValue: Double,           // 总浪费价值
    val expiredItems: Int,                  // 过期物品数量
    val discardedItems: Int,                // 丢弃物品数量
    val wasteByCategory: List<WasteCategoryData>,  // 按类别统计
    val wasteByTime: List<WasteTimeData>,          // 按时间统计
    val topWastedItems: List<WastedItemData>,      // 浪费最多的物品
    val reportPeriod: DateRange             // 报告时间范围
) : Parcelable

/**
 * 按类别统计的浪费数据
 */
@Parcelize
data class WasteCategoryData(
    val category: String,                   // 类别名称
    val itemCount: Int,                     // 浪费物品数量
    val totalValue: Double,                 // 总价值
    val percentage: Float                   // 占总浪费的百分比
) : Parcelable

/**
 * 按时间统计的浪费数据
 */
@Parcelize
data class WasteTimeData(
    val date: Date,                         // 日期
    val itemCount: Int,                     // 当日浪费物品数量
    val totalValue: Double                  // 当日浪费总价值
) : Parcelable

/**
 * 浪费物品详情
 */
@Parcelize
data class WastedItemData(
    val itemId: Long,                       // 物品ID
    val name: String,                       // 物品名称
    val category: String,                   // 类别
    val wasteReason: WasteReason,           // 浪费原因
    val wasteDate: Date,                    // 浪费日期
    val value: Double?,                     // 价值
    val quantity: Double,                   // 数量
    val unit: String,                       // 单位
    val photoUri: String?                   // 照片URI
) : Parcelable

/**
 * 浪费原因枚举
 */
enum class WasteReason {
    EXPIRED,    // 过期
    DISCARDED   // 丢弃
}

/**
 * 日期范围
 */
@Parcelize
data class DateRange(
    val startDate: Date,
    val endDate: Date
) : Parcelable

/**
 * 报告时间范围类型
 */
enum class ReportPeriodType {
    LAST_WEEK,      // 上周
    LAST_MONTH,     // 上月
    LAST_THREE_MONTHS, // 最近三个月
    PAST_YEAR,      // 过去一年
    THIS_YEAR,      // 今年
    CUSTOM          // 自定义
} 