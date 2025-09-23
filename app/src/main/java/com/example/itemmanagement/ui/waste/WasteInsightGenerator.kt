package com.example.itemmanagement.ui.waste

import com.example.itemmanagement.data.model.WasteReportData
import com.example.itemmanagement.data.model.WasteCategoryData
import com.example.itemmanagement.data.model.WasteReason
import java.text.NumberFormat
import java.util.*

/**
 * 浪费洞察生成器
 * 分析浪费数据并生成智能建议
 */
object WasteInsightGenerator {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    /**
     * 生成浪费洞察和建议
     */
    fun generateInsights(reportData: WasteReportData): List<WasteInsight> {
        val insights = mutableListOf<WasteInsight>()

        // 1. 总体浪费评估
        insights.add(generateOverallInsight(reportData))

        // 2. 最严重的浪费类别分析
        if (reportData.wasteByCategory.isNotEmpty()) {
            insights.add(generateTopWasteCategoryInsight(reportData.wasteByCategory))
        }

        // 3. 浪费原因分析
        insights.add(generateWasteReasonInsight(reportData))

        // 4. 趋势分析
        if (reportData.wasteByTime.size >= 3) {
            insights.add(generateTrendInsight(reportData))
        }

        // 5. 节约建议
        insights.addAll(generateSavingSuggestions(reportData))

        return insights
    }

    private fun generateOverallInsight(reportData: WasteReportData): WasteInsight {
        val totalValue = reportData.totalWastedValue
        val totalItems = reportData.totalWastedItems

        return when {
            totalItems == 0 -> WasteInsight(
                type = InsightType.POSITIVE,
                title = "太棒了！",
                message = "在这个时间段内，您没有任何浪费！继续保持这种优秀的管理习惯。",
                severity = InsightSeverity.LOW
            )
            totalValue <= 0 && totalItems > 0 -> WasteInsight(
                type = InsightType.WARNING,
                title = "有浪费但无价值记录",
                message = "您浪费了${totalItems}件物品，但没有价值信息。建议为物品设置价格以便更好地分析浪费成本。",
                severity = InsightSeverity.MEDIUM
            )
            totalValue <= 50 -> WasteInsight(
                type = InsightType.POSITIVE,
                title = "浪费控制良好",
                message = "您的浪费金额为${currencyFormat.format(totalValue)}，总共${totalItems}件物品。这个水平相当不错！",
                severity = InsightSeverity.LOW
            )
            totalValue <= 200 -> WasteInsight(
                type = InsightType.WARNING,
                title = "有改善空间",
                message = "您浪费了${currencyFormat.format(totalValue)}，涉及${totalItems}件物品。考虑优化购买计划和储存方式。",
                severity = InsightSeverity.MEDIUM
            )
            else -> WasteInsight(
                type = InsightType.CRITICAL,
                title = "需要立即关注",
                message = "浪费金额达到${currencyFormat.format(totalValue)}，共${totalItems}件物品。建议重新评估购买和储存策略。",
                severity = InsightSeverity.HIGH
            )
        }
    }

    private fun generateTopWasteCategoryInsight(categories: List<WasteCategoryData>): WasteInsight {
        val topCategory = categories.first()
        val percentage = topCategory.percentage.toInt()

        return WasteInsight(
            type = InsightType.INFO,
            title = "头号浪费类别：${topCategory.category}",
            message = "「${topCategory.category}」类别占总浪费的${percentage}%，金额${currencyFormat.format(topCategory.totalValue)}。" +
                    "建议：减少此类物品的购买量，或改善储存条件。",
            severity = if (percentage >= 50) InsightSeverity.HIGH else InsightSeverity.MEDIUM
        )
    }

    private fun generateWasteReasonInsight(reportData: WasteReportData): WasteInsight {
        val expiredRatio = reportData.expiredItems.toFloat() / reportData.totalWastedItems
        
        return when {
            expiredRatio >= 0.8f -> WasteInsight(
                type = InsightType.WARNING,
                title = "过期是主要问题",
                message = "80%以上的浪费来自过期物品。建议：设置过期提醒，采用先进先出原则，购买适量物品。",
                severity = InsightSeverity.HIGH
            )
            expiredRatio >= 0.5f -> WasteInsight(
                type = InsightType.INFO,
                title = "过期和丢弃各半",
                message = "过期和主动丢弃的物品各占一半。建议同时关注保质期管理和购买决策。",
                severity = InsightSeverity.MEDIUM
            )
            else -> WasteInsight(
                type = InsightType.INFO,
                title = "主动丢弃较多",
                message = "大部分浪费来自主动丢弃。建议在购买前更仔细地评估需求。",
                severity = InsightSeverity.MEDIUM
            )
        }
    }

    private fun generateTrendInsight(reportData: WasteReportData): WasteInsight {
        val timeData = reportData.wasteByTime.sortedBy { it.date }
        val values = timeData.map { it.totalValue }
        
        if (values.size < 3) {
            return WasteInsight(
                type = InsightType.INFO,
                title = "数据不足",
                message = "需要更多数据才能分析趋势。建议持续记录浪费情况。",
                severity = InsightSeverity.LOW
            )
        }
        
        // 使用线性回归分析趋势
        val trend = calculateLinearTrend(values)
        val volatility = calculateVolatility(values)
        

        return when {
            trend < -0.1 && volatility < 0.5 -> WasteInsight(
                type = InsightType.POSITIVE,
                title = "浪费显著减少",
                message = "太棒了！您的浪费情况持续改善，减少了${String.format("%.1f", -trend * 100)}%。继续保持！",
                severity = InsightSeverity.LOW
            )
            trend < -0.05 -> WasteInsight(
                type = InsightType.POSITIVE,
                title = "浪费稳步下降",
                message = "您的浪费情况正在改善，继续保持当前的管理策略。",
                severity = InsightSeverity.LOW
            )
            trend > 0.1 && volatility < 0.5 -> WasteInsight(
                type = InsightType.WARNING,
                title = "浪费持续上升",
                message = "浪费增加了${String.format("%.1f", trend * 100)}%，建议检查购买计划和储存方式。",
                severity = InsightSeverity.HIGH
            )
            trend > 0.05 -> WasteInsight(
                type = InsightType.WARNING,
                title = "浪费有所增加",
                message = "最近浪费有上升趋势，建议重新审视购买和储存习惯。",
                severity = InsightSeverity.MEDIUM
            )
            volatility > 0.8 -> WasteInsight(
                type = InsightType.INFO,
                title = "浪费波动较大",
                message = "浪费情况变化较大，建议建立更稳定的管理习惯。",
                severity = InsightSeverity.MEDIUM
            )
            else -> WasteInsight(
                type = InsightType.INFO,
                title = "浪费相对稳定",
                message = "浪费水平保持相对稳定，可以考虑进一步优化以减少浪费。",
                severity = InsightSeverity.LOW
            )
        }
    }

    /**
     * 计算线性趋势（使用简单的斜率计算）
     */
    private fun calculateLinearTrend(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val n = values.size
        val x = (0 until n).map { it.toDouble() }
        val y = values
        
        val meanX = x.average()
        val meanY = y.average()
        
        var numerator = 0.0
        var denominator = 0.0
        
        for (i in 0 until n) {
            numerator += (x[i] - meanX) * (y[i] - meanY)
            denominator += (x[i] - meanX) * (x[i] - meanX)
        }
        
        return if (denominator != 0.0) numerator / denominator / meanY else 0.0
    }

    /**
     * 计算波动性（变异系数）
     */
    private fun calculateVolatility(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        
        val mean = values.average()
        if (mean == 0.0) return 0.0
        
        val variance = values.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = kotlin.math.sqrt(variance)
        
        return standardDeviation / mean
    }

    private fun generateSavingSuggestions(reportData: WasteReportData): List<WasteInsight> {
        val suggestions = mutableListOf<WasteInsight>()

        // 基于浪费金额的建议
        if (reportData.totalWastedValue > 100) {
            suggestions.add(WasteInsight(
                type = InsightType.SUGGESTION,
                title = "购买策略建议",
                message = "考虑建立购物清单，避免冲动购买。购买前检查现有库存。",
                severity = InsightSeverity.MEDIUM
            ))
        }

        // 基于过期物品的建议
        if (reportData.expiredItems > reportData.discardedItems) {
            suggestions.add(WasteInsight(
                type = InsightType.SUGGESTION,
                title = "储存优化建议",
                message = "建议使用透明储存容器，让物品一目了然。建立先进先出的使用习惯。",
                severity = InsightSeverity.MEDIUM
            ))
        }

        return suggestions
    }
}

/**
 * 浪费洞察数据类
 */
data class WasteInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val severity: InsightSeverity
)

/**
 * 洞察类型
 */
enum class InsightType {
    POSITIVE,    // 积极信息
    WARNING,     // 警告
    CRITICAL,    // 严重问题
    INFO,        // 信息
    SUGGESTION   // 建议
}

/**
 * 洞察严重程度
 */
enum class InsightSeverity {
    LOW,     // 低
    MEDIUM,  // 中
    HIGH     // 高
} 