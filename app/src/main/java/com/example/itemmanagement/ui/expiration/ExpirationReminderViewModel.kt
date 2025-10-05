package com.example.itemmanagement.ui.expiration

import androidx.lifecycle.*
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.ReminderSettingsRepository
import com.example.itemmanagement.reminder.ReminderManager
import com.example.itemmanagement.reminder.model.*
import kotlinx.coroutines.launch

class ExpirationReminderViewModel(
    private val repository: UnifiedItemRepository,
    private val settingsRepository: ReminderSettingsRepository,
    private val reminderManager: ReminderManager
) : ViewModel() {

    // 统一的新提醒系统数据
    private val _reminderSummary = MutableLiveData<ReminderSummary>()
    val reminderSummary: LiveData<ReminderSummary> = _reminderSummary
    
    private val _reminderSettings = MutableLiveData<ReminderSettingsSnapshot>()
    val reminderSettings: LiveData<ReminderSettingsSnapshot> = _reminderSettings

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadReminderData()
    }
    
    /**
     * 加载提醒数据
     */
    fun loadReminderData() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // 加载综合提醒汇总
                val summary = reminderManager.getAllReminders()
                _reminderSummary.value = summary
                
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "加载数据失败：${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * 刷新提醒数据
     */
    fun refreshData() {
        loadReminderData()
    }

    /**
     * 加载设置快照
     */
    fun loadSettingsSnapshot() {
        viewModelScope.launch {
            try {
                val settings = settingsRepository.getSettings()
                val thresholds = settingsRepository.getAllThresholds()
                val rules = settingsRepository.getActiveRules()
                
                val snapshot = ReminderSettingsSnapshot(
                    expirationAdvanceDays = settings.expirationAdvanceDays,
                    includeWarranty = settings.includeWarranty,
                    notificationTime = settings.notificationTime,
                    stockReminderEnabled = settings.stockReminderEnabled,
                    pushNotificationEnabled = settings.pushNotificationEnabled,
                    quietHourStart = settings.quietHourStart,
                    quietHourEnd = settings.quietHourEnd,
                    categoryThresholds = thresholds.associate { it.category to it.minQuantity },
                    activeRulesCount = rules.size
                )
                
                _reminderSettings.value = snapshot
            } catch (e: Exception) {
                _errorMessage.value = "加载设置失败：${e.message}"
            }
        }
    }

    /**
     * 刷新提醒数据（用于测试）
     */
    fun refreshReminderData() {
        refreshData()
    }

    /**
     * 获取提醒统计数据
     */
    fun getReminderStats(): ReminderSummaryStats {
        val summary = _reminderSummary.value ?: ReminderSummary(
            expiredItems = emptyList(),
            expiringItems = emptyList(),
            lowStockItems = emptyList(),
            customRuleMatches = emptyList()
        )
        
        return ReminderSummaryStats(
            expiredCount = summary.expiredItems.size,
            todayExpiringCount = 0, // 兼容旧接口
            upcomingExpiringCount = summary.expiringItems.size,
            lowStockCount = summary.lowStockItems.size,
            customRuleCount = summary.customRuleMatches.size,
            totalUrgentCount = summary.getUrgentCount(),
            totalCount = summary.getTotalCount()
        )
    }

    /**
     * 标记自定义规则为已触发
     */
    fun markCustomRuleTriggered(ruleId: Long) {
        viewModelScope.launch {
            try {
                // 获取规则并更新触发状态
                val rule = settingsRepository.getRuleById(ruleId)
                if (rule != null) {
                    // 更新规则的最后触发时间
                    settingsRepository.updateRule(rule.copy(lastTriggeredAt = java.util.Date()))
                }
                // 重新加载数据以反映变化
                refreshData()
            } catch (e: Exception) {
                _errorMessage.value = "更新规则状态失败：${e.message}"
            }
        }
    }

    /**
     * 获取库存状态描述
     */
    fun getStockStatusDescription(item: LowStockItem): String {
        return if (item.isUrgent()) "已缺货" else "库存不足"
    }

    /**
     * 获取优先级描述
     */
    fun getPriorityDescription(priority: ReminderPriority): String {
        return when (priority) {
            ReminderPriority.URGENT -> "紧急"
            ReminderPriority.IMPORTANT -> "重要"
            ReminderPriority.NORMAL -> "普通"
        }
    }

    /**
     * 获取提醒类型描述
     */
    fun getReminderTypeDescription(type: ReminderType): String {
        return when (type) {
            ReminderType.EXPIRED -> "已过期"
            ReminderType.EXPIRING -> "即将到期"
            ReminderType.WARRANTY_EXPIRING -> "保修到期"
            ReminderType.LOW_STOCK -> "库存不足"
            ReminderType.OUT_OF_STOCK -> "已缺货"
            ReminderType.CUSTOM_RULE -> "自定义规则"
        }
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * 提醒统计数据类（兼容旧接口）
     */
    data class ReminderSummaryStats(
        val expiredCount: Int,
        val todayExpiringCount: Int,
        val upcomingExpiringCount: Int,
        val lowStockCount: Int,
        val customRuleCount: Int,
        val totalUrgentCount: Int,
        val totalCount: Int
    )
}