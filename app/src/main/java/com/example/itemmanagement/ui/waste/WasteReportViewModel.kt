package com.example.itemmanagement.ui.waste

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.model.WasteCategoryInfo
import com.example.itemmanagement.data.model.WasteDateInfo
import com.example.itemmanagement.data.model.WastedItemInfo
import com.example.itemmanagement.data.model.WasteSummaryInfo
import com.example.itemmanagement.data.model.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private const val EXPIRED_CHECK_INTERVAL_MS = 60 * 60 * 1000L // 1小时

class WasteReportViewModel(
    private val repository: UnifiedItemRepository
) : ViewModel() {

    private val _wasteReportData = MutableLiveData<WasteReportData?>()
    val wasteReportData: LiveData<WasteReportData?> = _wasteReportData

    private val _insights = MutableLiveData<List<WasteInsight>>()
    val insights: LiveData<List<WasteInsight>> = _insights

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _currentPeriod = MutableLiveData<ReportPeriodType>(ReportPeriodType.LAST_MONTH)
    val currentPeriod: LiveData<ReportPeriodType> = _currentPeriod

    private val _customDateRange = MutableLiveData<DateRange?>()
    val customDateRange: LiveData<DateRange?> = _customDateRange
    
    private var lastExpiredCheckTime = 0L

    /**
     * 检查过期物品并生成浪费报告
     */
    fun checkExpiredItemsAndGenerateReport(periodType: ReportPeriodType, customRange: DateRange? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // 智能检查：如果距离上次检查不足1小时，跳过过期检查
                val shouldCheckExpired = shouldPerformExpiredCheck()
                if (shouldCheckExpired) {
                    repository.checkAndUpdateExpiredItems(System.currentTimeMillis())
                    updateLastExpiredCheckTime()
                }
                
                // 然后直接生成报告（避免嵌套协程）
                generateWasteReportInternal(periodType, customRange)
            } catch (e: Exception) {
                _errorMessage.value = "更新过期物品状态失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * 生成浪费报告（公共接口）
     */
    fun generateWasteReport(periodType: ReportPeriodType, customRange: DateRange? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                generateWasteReportInternal(periodType, customRange)
            } catch (e: Exception) {
                _errorMessage.value = "生成浪费报告失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 生成浪费报告（内部实现）
     */
    private suspend fun generateWasteReportInternal(periodType: ReportPeriodType, customRange: DateRange? = null) {
        val dateRange = when (periodType) {
            ReportPeriodType.WEEK -> getThisWeekRange()
            ReportPeriodType.MONTH -> getThisMonthRange()
            ReportPeriodType.QUARTER -> getThisQuarterRange()
            ReportPeriodType.HALF_YEAR -> getThisHalfYearRange()
            ReportPeriodType.YEAR -> getThisYearRange()
            ReportPeriodType.LAST_WEEK -> getLastWeekRange()
            ReportPeriodType.LAST_MONTH -> getLastMonthRange()
            ReportPeriodType.LAST_THREE_MONTHS -> getLastThreeMonthsRange()
            ReportPeriodType.PAST_YEAR -> getPastYearRange()
            ReportPeriodType.THIS_YEAR -> getThisYearRange()
            ReportPeriodType.CUSTOM -> customRange ?: getLastMonthRange()
        }

        _currentPeriod.value = periodType
        if (periodType == ReportPeriodType.CUSTOM) {
            _customDateRange.value = dateRange
        }

        val startTime = dateRange.startDate.time
        val endTime = dateRange.endDate.time

        // 使用事务获取所有数据确保一致性
        val wastedItems = repository.getWasteReportData(startTime, endTime)
        
        // 调试信息
        println("WasteReportViewModel调试:")
        println("- 查询时间范围: ${Date(startTime)} 到 ${Date(endTime)}")
        println("- 总结: 物品${wastedItems.size}件, 价值${wastedItems.sumOf { it.value }}元")
        println("- 浪费物品详情:")
        wastedItems.forEach { item ->
            println("  * ${item.name}: 浪费时间=${item.wasteDate}, 价格=${item.value}")
        }
        
        // 检查是否有浪费状态但没有wasteDate的物品
        val wastedItemsWithoutDate = repository.getWastedItemsWithoutWasteDate()
        if (wastedItemsWithoutDate.isNotEmpty()) {
            println("- 发现${wastedItemsWithoutDate.size}个浪费物品没有wasteDate:")
            wastedItemsWithoutDate.forEach { item ->
                println("  ! ${item.name}: 添加时间=${item.wasteDate}, 价格=${item.value}")
            }
            
            // 自动修复这些物品
            val fixedCount = repository.fixWastedItemsWithoutWasteDate(System.currentTimeMillis())
            println("- 已自动修复${fixedCount}个物品的wasteDate")
            
            // 修复后重新查询数据
            val updatedWastedItems = repository.getWasteReportData(startTime, endTime)
            println("- 修复后总结: 物品${updatedWastedItems.size}件, 价值${updatedWastedItems.sumOf { it.value }}元")
            
            // 使用修复后的数据
            val wasteReport = buildWasteReportDataFromItems(updatedWastedItems, dateRange)
            
            _wasteReportData.value = wasteReport
            val insights = WasteInsightGenerator.generateInsights(wasteReport)
            _insights.value = insights
            _isLoading.value = false
            return
        }

        // 转换数据格式
        val wasteReport = buildWasteReportDataFromItems(wastedItems, dateRange)

        _wasteReportData.value = wasteReport

        // 生成智能洞察
        val insights = WasteInsightGenerator.generateInsights(wasteReport)
        _insights.value = insights
        
        _isLoading.value = false
    }

    /**
     * 从废料物品列表构建报告数据
     */
    private fun buildWasteReportDataFromItems(
        wastedItems: List<WastedItemInfo>,
        dateRange: DateRange
    ): WasteReportData {
        // 统计过期和丢弃的数量
        val expiredCount = wastedItems.count { it.status == "EXPIRED" }
        val discardedCount = wastedItems.count { it.status == "DISCARDED" }
        
        // 构建摘要信息
        val summary = WasteSummaryInfo(
            totalItems = wastedItems.size,
            totalValue = wastedItems.sumOf { it.value },
            expiredItems = expiredCount,
            discardedItems = discardedCount,
            periodStartDate = dateRange.startDate,
            periodEndDate = dateRange.endDate
        )

        // 按类别分组
        val categoryData = wastedItems.groupBy { it.category }
            .map { (category, items) ->
                WasteCategoryInfo(
                    category = category,
                    count = items.size,
                    value = items.sumOf { it.value }
                )
            }

        // 按日期分组
        val dateData = wastedItems.groupBy { item ->
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(item.wasteDate)
        }.map { (date, items) ->
            WasteDateInfo(
                date = date,
                count = items.size,
                value = items.sumOf { it.value }
            )
        }

        return WasteReportData(
            summary = summary,
            wastedItems = wastedItems,
            categoryData = categoryData,
            dateData = dateData
        )
    }

    /**
     * 构建浪费报告数据
     */
    private fun buildWasteReportData(
        summary: WasteSummaryInfo,
        wastedItems: List<WastedItemInfo>,
        categoryData: List<WasteCategoryInfo>,
        dateData: List<WasteDateInfo>,
        dateRange: DateRange
    ): WasteReportData {
        // 转换类别数据
        val totalValue = summary.totalValue
        val wasteByCategory = categoryData.map { category ->
            WasteCategoryData(
                category = category.category,
                count = category.count,
                totalValue = category.value,
                percentage = if (totalValue > 0) {
                    (category.value / totalValue * 100).toDouble()
                } else 0.0
            )
        }

        // 转换时间数据
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val wasteByTime = dateData.map { date ->
            WasteTimeData(
                period = date.date,
                count = date.count,
                value = date.value
            )
        }

        // 转换浪费物品数据（取前10个）
        val topWastedItems = wastedItems.take(10).map { item ->
            WastedItemData(
                id = item.id,
                name = item.name,
                category = item.category,
                wasteReason = if (item.status == "EXPIRED") WasteReason.EXPIRED else WasteReason.OTHER,
                wasteDate = item.wasteDate,
                originalValue = item.totalPrice,
                quantity = item.quantity,
                unit = item.unit,
                isQuantityUserInput = item.isQuantityUserInput,
                photoUri = item.photoUri
            )
        }

        return WasteReportData(
            summary = summary,
            wastedItems = topWastedItems.map { wastedItem ->
                WastedItemInfo(
                    id = wastedItem.id,
                    name = wastedItem.name,
                    category = wastedItem.category,
                    wasteDate = wastedItem.wasteDate,
                    value = wastedItem.originalValue,
                    quantity = wastedItem.quantity,
                    unit = wastedItem.unit,
                    status = if (wastedItem.wasteReason == WasteReason.EXPIRED) "EXPIRED" else "DISCARDED",
                    totalPrice = wastedItem.originalValue,
                    photoUri = wastedItem.photoUri
                )
            },
            categoryData = categoryData,
            dateData = dateData
        )
    }

    /**
     * 刷新报告
     */
    fun refreshReport() {
        val currentPeriodValue = _currentPeriod.value ?: ReportPeriodType.LAST_MONTH
        val customRange = if (currentPeriodValue == ReportPeriodType.CUSTOM) {
            _customDateRange.value
        } else null
        checkExpiredItemsAndGenerateReport(currentPeriodValue, customRange)
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // =================== 日期范围计算方法 ===================

    private fun getLastWeekRange(): DateRange {
        val calendar = Calendar.getInstance()
        
        // 设置为本周一
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // 移动到上周一
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val startDate = calendar.time
        
        // 移动到上周日结束
        calendar.add(Calendar.DAY_OF_YEAR, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    private fun getLastMonthRange(): DateRange {
        val calendar = Calendar.getInstance()
        
        // 移动到上个月
        calendar.add(Calendar.MONTH, -1)
        
        // 设置为上个月第一天的开始
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        // 移动到上个月最后一天的结束
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    private fun getLastThreeMonthsRange(): DateRange {
        val calendar = Calendar.getInstance()
        val endDate = Date()
        
        calendar.time = endDate
        calendar.add(Calendar.MONTH, -3)
        val startDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    private fun getPastYearRange(): DateRange {
        val calendar = Calendar.getInstance()
        val endDate = Date()
        
        calendar.time = endDate
        calendar.add(Calendar.YEAR, -1)
        val startDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    private fun getThisYearRange(): DateRange {
        val calendar = Calendar.getInstance()
        val endDate = Date()
        
        // 设置为今年1月1日
        calendar.set(Calendar.MONTH, Calendar.JANUARY)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    // 添加缺失的日期范围获取方法
    private fun getThisWeekRange(): DateRange {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    private fun getThisMonthRange(): DateRange {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    private fun getThisQuarterRange(): DateRange {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val quarterStartMonth = (currentMonth / 3) * 3
        
        calendar.set(Calendar.MONTH, quarterStartMonth)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        calendar.add(Calendar.MONTH, 3)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    private fun getThisHalfYearRange(): DateRange {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val halfYearStartMonth = if (currentMonth < 6) 0 else 6
        
        calendar.set(Calendar.MONTH, halfYearStartMonth)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        calendar.add(Calendar.MONTH, 6)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    /**
     * 判断是否需要执行过期检查
     */
    private fun shouldPerformExpiredCheck(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastExpiredCheckTime > EXPIRED_CHECK_INTERVAL_MS
    }

    /**
     * 更新最后一次过期检查时间
     */
    private fun updateLastExpiredCheckTime() {
        lastExpiredCheckTime = System.currentTimeMillis()
    }
} 