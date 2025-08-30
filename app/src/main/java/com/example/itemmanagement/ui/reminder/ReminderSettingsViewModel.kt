package com.example.itemmanagement.ui.reminder

import androidx.lifecycle.*
import com.example.itemmanagement.data.entity.ReminderSettingsEntity
import com.example.itemmanagement.data.entity.CategoryThresholdEntity
import com.example.itemmanagement.data.entity.CustomRuleEntity
import com.example.itemmanagement.data.repository.ReminderSettingsRepository
import com.example.itemmanagement.reminder.model.ReminderSettingsSnapshot
import kotlinx.coroutines.launch

class ReminderSettingsViewModel(
    private val repository: ReminderSettingsRepository
) : ViewModel() {
    
    // 提醒设置数据
    private val _settings = MutableLiveData<ReminderSettingsEntity>()
    val settings: LiveData<ReminderSettingsEntity> = _settings
    
    private val _categoryThresholds = MutableLiveData<List<CategoryThresholdEntity>>()
    val categoryThresholds: LiveData<List<CategoryThresholdEntity>> = _categoryThresholds
    
    private val _customRules = MutableLiveData<List<CustomRuleEntity>>()
    val customRules: LiveData<List<CustomRuleEntity>> = _customRules
    
    // UI状态
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess
    
    init {
        loadAllSettings()
    }
    
    /**
     * 加载所有设置数据
     */
    fun loadAllSettings() {
        viewModelScope.launch {
            _loading.value = true
            try {
                // 并行加载所有数据
                val settings = repository.getSettings()
                val thresholds = repository.getAllThresholds()
                val rules = repository.getActiveRules()
                
                _settings.value = settings
                _categoryThresholds.value = thresholds
                _customRules.value = rules
                
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "加载设置失败：${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    // ==================== 基础设置更新 ====================
    
    /**
     * 更新到期提前天数
     */
    fun updateExpirationAdvanceDays(days: Int) {
        viewModelScope.launch {
            try {
                repository.updateExpirationAdvanceDays(days)
                refreshCurrentSettings()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "更新失败：${e.message}"
            }
        }
    }
    
    /**
     * 更新通知时间
     */
    fun updateNotificationTime(time: String) {
        viewModelScope.launch {
            try {
                repository.updateNotificationTime(time)
                refreshCurrentSettings()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "更新失败：${e.message}"
            }
        }
    }
    
    /**
     * 更新库存提醒开关
     */
    fun updateStockReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateStockReminderEnabled(enabled)
                refreshCurrentSettings()
            } catch (e: Exception) {
                _errorMessage.value = "更新失败：${e.message}"
            }
        }
    }
    
    /**
     * 更新完整设置
     */
    fun updateSettings(newSettings: ReminderSettingsEntity) {
        viewModelScope.launch {
            try {
                repository.updateSettings(newSettings)
                _settings.value = newSettings
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "保存设置失败：${e.message}"
            }
        }
    }
    
    // ==================== 分类阈值管理 ====================
    
    /**
     * 更新分类阈值
     */
    fun updateCategoryThreshold(category: String, minQuantity: Double) {
        viewModelScope.launch {
            try {
                repository.updateCategoryMinQuantity(category, minQuantity)
                refreshCategoryThresholds()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "更新分类阈值失败：${e.message}"
            }
        }
    }
    
    /**
     * 添加新的分类阈值
     */
    fun addCategoryThreshold(threshold: CategoryThresholdEntity) {
        viewModelScope.launch {
            try {
                repository.updateCategoryThreshold(threshold)
                refreshCategoryThresholds()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "添加分类阈值失败：${e.message}"
            }
        }
    }
    
    /**
     * 删除分类阈值
     */
    fun deleteCategoryThreshold(category: String) {
        viewModelScope.launch {
            try {
                // 通过设置enabled为false来"软删除"
                val existing = repository.getThresholdByCategory(category)
                if (existing != null) {
                    repository.updateCategoryThreshold(existing.copy(enabled = false))
                }
                refreshCategoryThresholds()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "删除分类阈值失败：${e.message}"
            }
        }
    }
    
    // ==================== 自定义规则管理 ====================
    
    /**
     * 创建新的自定义规则
     */
    fun createCustomRule(rule: CustomRuleEntity) {
        viewModelScope.launch {
            try {
                repository.insertRule(rule)
                refreshCustomRules()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "创建自定义规则失败：${e.message}"
            }
        }
    }
    
    /**
     * 更新自定义规则
     */
    fun updateCustomRule(rule: CustomRuleEntity) {
        viewModelScope.launch {
            try {
                repository.updateRule(rule)
                refreshCustomRules()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "更新自定义规则失败：${e.message}"
            }
        }
    }
    
    /**
     * 删除自定义规则
     */
    fun deleteCustomRule(ruleId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteRule(ruleId)
                refreshCustomRules()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "删除自定义规则失败：${e.message}"
            }
        }
    }
    
    /**
     * 启用/禁用自定义规则
     */
    fun toggleCustomRule(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateRuleEnabled(ruleId, enabled)
                refreshCustomRules()
            } catch (e: Exception) {
                _errorMessage.value = "更新规则状态失败：${e.message}"
            }
        }
    }
    
    // ==================== 初始化和重置 ====================
    
    /**
     * 初始化默认数据
     */
    fun initializeDefaults() {
        viewModelScope.launch {
            try {
                repository.initializeDefaultData()
                loadAllSettings()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "初始化失败：${e.message}"
            }
        }
    }
    
    /**
     * 重置所有设置为默认值
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                val defaultSettings = ReminderSettingsEntity()
                repository.updateSettings(defaultSettings)
                repository.initializeDefaultCategoryThresholds()
                loadAllSettings()
                _saveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "重置失败：${e.message}"
            }
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取设置快照（用于其他页面显示）
     */
    fun getSettingsSnapshot(): ReminderSettingsSnapshot? {
        val settings = _settings.value ?: return null
        val thresholds = _categoryThresholds.value ?: emptyList()
        val activeRules = _customRules.value?.count { it.enabled } ?: 0
        
        return ReminderSettingsSnapshot(
            expirationAdvanceDays = settings.expirationAdvanceDays,
            includeWarranty = settings.includeWarranty,
            notificationTime = settings.notificationTime,
            stockReminderEnabled = settings.stockReminderEnabled,
            pushNotificationEnabled = settings.pushNotificationEnabled,
            quietHourStart = settings.quietHourStart,
            quietHourEnd = settings.quietHourEnd,
            categoryThresholds = thresholds.filter { it.enabled }.associate { it.category to it.minQuantity },
            activeRulesCount = activeRules
        )
    }
    
    /**
     * 刷新当前设置
     */
    private suspend fun refreshCurrentSettings() {
        try {
            val updated = repository.getSettings()
            _settings.value = updated
        } catch (e: Exception) {
            // 忽略刷新错误
        }
    }
    
    /**
     * 刷新分类阈值
     */
    private suspend fun refreshCategoryThresholds() {
        try {
            val updated = repository.getAllThresholds()
            _categoryThresholds.value = updated
        } catch (e: Exception) {
            // 忽略刷新错误
        }
    }
    
    /**
     * 刷新自定义规则
     */
    private suspend fun refreshCustomRules() {
        try {
            val updated = repository.getActiveRules()
            _customRules.value = updated
        } catch (e: Exception) {
            // 忽略刷新错误
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 清除保存成功标志
     */
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }
    
    /**
     * 验证设置有效性
     */
    fun validateSettings(settings: ReminderSettingsEntity): String? {
        return when {
            settings.expirationAdvanceDays < 0 || settings.expirationAdvanceDays > 365 ->
                "提前天数必须在0-365之间"
            
            !isValidTimeFormat(settings.notificationTime) ->
                "通知时间格式无效，请使用 HH:mm 格式"
                
            !isValidTimeFormat(settings.quietHourStart) || !isValidTimeFormat(settings.quietHourEnd) ->
                "勿扰时间格式无效，请使用 HH:mm 格式"
                
            else -> null
        }
    }
    
    /**
     * 验证时间格式
     */
    private fun isValidTimeFormat(time: String): Boolean {
        val timeRegex = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$".toRegex()
        return timeRegex.matches(time)
    }
}
