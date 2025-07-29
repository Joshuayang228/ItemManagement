package com.example.itemmanagement.ui.function

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.model.FunctionCard
import com.example.itemmanagement.data.model.FunctionSection
import com.example.itemmanagement.data.model.InventoryStats
import kotlinx.coroutines.launch
import java.util.*
import java.util.Calendar

class FunctionViewModel(private val repository: ItemRepository) : ViewModel() {

    private val _functionSections = MutableLiveData<List<FunctionSection>>()
    val functionSections: LiveData<List<FunctionSection>> = _functionSections

    private val _inventoryStats = MutableLiveData<InventoryStats>()
    val inventoryStats: LiveData<InventoryStats> = _inventoryStats

    private val _navigationEvent = MutableLiveData<Int?>()
    val navigationEvent: LiveData<Int?> = _navigationEvent

    init {
        loadFunctionSections()
        loadInventoryStats()
    }

    private fun loadFunctionSections() {
        // 数据洞察功能组
        val dataInsightsFunctions = listOf(
            FunctionCard(
                id = "inventory_analysis",
                title = "库存分析",
                description = "查看物品统计、分类分布、价值分析",
                iconResId = com.example.itemmanagement.R.drawable.ic_statistics,
                type = FunctionCard.Type.ANALYTICS
            ),
            FunctionCard(
                id = "item_calendar",
                title = "物品日历",
                description = "查看过期日期、保修期、重要时间节点",
                iconResId = com.example.itemmanagement.R.drawable.ic_calendar,
                type = FunctionCard.Type.CALENDAR
            ),
            FunctionCard(
                id = "waste_report",
                title = "浪费报告",
                description = "统计过期浪费，优化消费习惯",
                iconResId = com.example.itemmanagement.R.drawable.ic_cleanup,
                type = FunctionCard.Type.WASTE_REPORT
            )
        )

        // 智能助手功能组
        val smartAssistantFunctions = listOf(
            FunctionCard(
                id = "wishlist",
                title = "心愿单",
                description = "记录想买的物品，价格跟踪提醒",
                iconResId = com.example.itemmanagement.R.drawable.ic_star,
                type = FunctionCard.Type.WISHLIST
            ),
            FunctionCard(
                id = "recurring_reminders",
                title = "周期提醒",
                description = "定期检查、维护、订阅服务提醒",
                iconResId = com.example.itemmanagement.R.drawable.ic_calendar,
                type = FunctionCard.Type.REMINDER
            ),
            FunctionCard(
                id = "warranty_management",
                title = "保修管理",
                description = "管理保修期、上传凭证、到期提醒",
                iconResId = com.example.itemmanagement.R.drawable.ic_settings,
                type = FunctionCard.Type.WARRANTY
            )
        )

        // 实用工具功能组
        val utilitiesFunctions = listOf(
            FunctionCard(
                id = "lending_tracker",
                title = "借还管理",
                description = "记录借出归还、联系人管理",
                iconResId = com.example.itemmanagement.R.drawable.ic_list,
                type = FunctionCard.Type.LENDING
            ),
            FunctionCard(
                id = "data_backup",
                title = "数据导出",
                description = "备份数据、CSV导出、迁移同步",
                iconResId = com.example.itemmanagement.R.drawable.ic_save,
                type = FunctionCard.Type.BACKUP
            )
        )

        val sections = listOf(
            FunctionSection(
                id = "data_insights",
                title = "数据洞察",
                description = "了解您的资产和消费状况",
                iconResId = com.example.itemmanagement.R.drawable.ic_statistics,
                functions = dataInsightsFunctions
            ),
            FunctionSection(
                id = "smart_assistant",
                title = "智能助手",
                description = "主动帮助管理和决策",
                iconResId = com.example.itemmanagement.R.drawable.ic_star,
                functions = smartAssistantFunctions
            ),
            FunctionSection(
                id = "utilities",
                title = "实用工具",
                description = "解决特定场景问题",
                iconResId = com.example.itemmanagement.R.drawable.ic_settings,
                functions = utilitiesFunctions
            )
        )

        _functionSections.value = sections
    }

    private fun loadInventoryStats() {
        viewModelScope.launch {
            try {
                repository.getAllItems().collect { allItems ->
                    val totalItems = allItems.size
                    val totalValue = allItems.sumOf { it.price ?: 0.0 }
                    
                    val expiringItems = allItems.filter { item ->
                        item.expirationDate?.let { expDate ->
                            val diffDays = (expDate.time - Date().time) / (1000 * 60 * 60 * 24)
                            diffDays in 1..7 // 7天内过期
                        } ?: false
                    }.size
                    
                    val expiredItems = allItems.filter { item ->
                        item.expirationDate?.let { expDate ->
                            expDate.before(Date())
                        } ?: false
                    }.size
                    
                    val lowStockItems = allItems.filter { item ->
                        val threshold = item.stockWarningThreshold ?: 0
                        (item.quantity ?: 0.0) <= threshold && threshold > 0
                    }.size
                    
                    // 计算分类数量
                    val categoriesCount = allItems.mapNotNull { it.category }.distinct().size
                    
                    // 计算位置数量
                    val locationsCount = allItems.mapNotNull { it.location?.area }.distinct().size
                    
                    // 计算愿望单物品数量 (可以根据你的业务逻辑定义)
                    val wishlistItems = allItems.filter { it.isWishlistItem || it.quantity == 0.0 }.size
                    
                    // 计算最近添加的物品数量 (7天内)
                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                    }.time
                    val recentlyAddedItems = allItems.filter { item ->
                        item.addDate.after(sevenDaysAgo)
                    }.size

                    val stats = InventoryStats(
                        totalItems = totalItems,
                        totalValue = totalValue,
                        expiringItems = expiringItems,
                        expiredItems = expiredItems,
                        lowStockItems = lowStockItems,
                        categoriesCount = categoriesCount,
                        locationsCount = locationsCount,
                        wishlistItems = wishlistItems,
                        recentlyAddedItems = recentlyAddedItems
                    )
                    _inventoryStats.value = stats
                }
            } catch (e: Exception) {
                // 处理错误
            }
        }
    }

    fun handleFunctionClick(functionType: String) {
        // 这里可以处理不同功能卡片的点击事件
        when (functionType) {
            // 数据洞察类
            "inventory_analysis" -> {
                // 导航到库存分析详细页面，显示图表和深度分析
                _navigationEvent.value = com.example.itemmanagement.R.id.action_function_to_inventory_analysis
            }
            "item_calendar" -> {
                // 导航到物品日历页面，显示时间轴视图
                _navigationEvent.value = com.example.itemmanagement.R.id.action_function_to_item_calendar
            }
            "waste_report" -> {
                // 导航到浪费报告页面，显示浪费统计和建议
                _navigationEvent.value = com.example.itemmanagement.R.id.action_function_to_waste_report
            }
            
            // 智能助手类
            "wishlist" -> {
                // 导航到心愿单页面，记录想买的物品
                _navigationEvent.value = com.example.itemmanagement.R.id.action_function_to_wishlist
            }
            "recurring_reminders" -> {
                // 导航到周期提醒设置页面，创建和管理定期任务
            }
            "warranty_management" -> {
                // 导航到保修管理页面，管理保修信息和提醒
            }
            
            // 实用工具类
            "lending_tracker" -> {
                // 导航到借还管理页面，记录借出借入
            }
            "data_backup" -> {
                // 执行数据导出功能，生成备份文件
            }
        }
    }

    fun clearNavigationEvent() {
        _navigationEvent.value = null
    }
} 