package com.example.itemmanagement.ui.waste

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.dao.WasteCategoryInfo
import com.example.itemmanagement.data.dao.WasteDateInfo
import com.example.itemmanagement.data.dao.WastedItemInfo
import com.example.itemmanagement.data.dao.WasteSummaryInfo
import com.example.itemmanagement.data.model.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WasteReportViewModel(
    private val repository: ItemRepository
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

    /**
     * 检查过期物品并生成浪费报告
     */
    fun checkExpiredItemsAndGenerateReport(periodType: ReportPeriodType, customRange: DateRange? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 先更新过期物品状态
                println("WasteReportViewModel: 开始检查过期物品...")
                repository.checkAndUpdateExpiredItems(System.currentTimeMillis())
                println("WasteReportViewModel: 过期物品状态更新完成")
                
                // 然后生成报告
                generateWasteReport(periodType, customRange)
            } catch (e: Exception) {
                _errorMessage.value = "更新过期物品状态失败: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * 生成浪费报告
     */
    fun generateWasteReport(periodType: ReportPeriodType, customRange: DateRange? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val dateRange = when (periodType) {
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

                // 并行获取所有数据
                println("WasteReportViewModel: 查询时间范围 ${Date(startTime)} 到 ${Date(endTime)}")
                val summary = repository.getWasteSummaryInPeriod(startTime, endTime)
                val wastedItems = repository.getWastedItemsInPeriod(startTime, endTime)
                val categoryData = repository.getWasteByCategoryInPeriod(startTime, endTime)
                val dateData = repository.getWasteByDateInPeriod(startTime, endTime)
                
                println("WasteReportViewModel: 查询结果 - 总计: ${summary.totalItems}件, 价值: ${summary.totalValue}元")
                println("WasteReportViewModel: 浪费物品详情: ${wastedItems.size}件")
                println("WasteReportViewModel: 分类数据: ${categoryData.size}个类别")
                println("WasteReportViewModel: 时间数据: ${dateData.size}个日期")

                // 转换数据格式
                val wasteReport = buildWasteReportData(
                    summary = summary,
                    wastedItems = wastedItems,
                    categoryData = categoryData,
                    dateData = dateData,
                    dateRange = dateRange
                )

                _wasteReportData.value = wasteReport

                // 生成智能洞察
                val insights = WasteInsightGenerator.generateInsights(wasteReport)
                _insights.value = insights

            } catch (e: Exception) {
                _errorMessage.value = "生成浪费报告失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
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
                itemCount = category.itemCount,
                totalValue = category.totalValue,
                percentage = if (totalValue > 0) {
                    (category.totalValue / totalValue * 100).toFloat()
                } else 0f
            )
        }

        // 转换时间数据
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val wasteByTime = dateData.map { date ->
            WasteTimeData(
                date = dateFormat.parse(date.date) ?: Date(),
                itemCount = date.itemCount,
                totalValue = date.totalValue
            )
        }

        // 转换浪费物品数据（取前10个）
        val topWastedItems = wastedItems.take(10).map { item ->
            WastedItemData(
                itemId = item.id,
                name = item.name,
                category = item.category ?: "未分类",
                wasteReason = if (item.status == "EXPIRED") WasteReason.EXPIRED else WasteReason.DISCARDED,
                wasteDate = Date(item.addDate),
                value = if (item.totalPrice > 0) item.totalPrice else null,
                quantity = item.quantity,
                unit = item.unit,
                photoUri = item.photoUri
            )
        }

        return WasteReportData(
            totalWastedItems = summary.totalItems,
            totalWastedValue = summary.totalValue,
            expiredItems = summary.expiredItems,
            discardedItems = summary.discardedItems,
            wasteByCategory = wasteByCategory,
            wasteByTime = wasteByTime,
            topWastedItems = topWastedItems,
            reportPeriod = dateRange
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
        val endDate = Date()
        
        calendar.time = endDate
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = calendar.time
        
        return DateRange(startDate, endDate)
    }

    private fun getLastMonthRange(): DateRange {
        val calendar = Calendar.getInstance()
        val endDate = Date()
        
        calendar.time = endDate
        calendar.add(Calendar.MONTH, -1)
        val startDate = calendar.time
        
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
} 