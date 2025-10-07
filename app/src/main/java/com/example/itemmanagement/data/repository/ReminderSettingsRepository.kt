package com.example.itemmanagement.data.repository

import com.example.itemmanagement.data.AppDatabase
import com.example.itemmanagement.data.entity.ReminderSettingsEntity
import com.example.itemmanagement.data.entity.CategoryThresholdEntity
import com.example.itemmanagement.data.entity.CustomRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date

class ReminderSettingsRepository(private val database: AppDatabase) {
    
    // ==================== 提醒设置相关 ====================
    
    /**
     * 获取提醒设置（Flow版本）
     */
    fun getSettingsFlow(): Flow<ReminderSettingsEntity?> {
        return database.reminderSettingsDao().getSettingsFlow()
    }
    
    /**
     * 获取提醒设置，如果不存在则返回默认设置
     */
    suspend fun getSettings(): ReminderSettingsEntity {
        var settings = database.reminderSettingsDao().getSettings()
        if (settings == null) {
            // 初始化默认设置
            val defaultSettings = ReminderSettingsEntity()
            database.reminderSettingsDao().insertOrUpdateSettings(defaultSettings)
            settings = defaultSettings
        }
        return settings
    }
    
    /**
     * 更新提醒设置
     */
    suspend fun updateSettings(settings: ReminderSettingsEntity) {
        database.reminderSettingsDao().insertOrUpdateSettings(
            settings.copy(updatedAt = Date())
        )
    }
    
    /**
     * 更新到期提前天数
     */
    suspend fun updateExpirationAdvanceDays(days: Int) {
        database.reminderSettingsDao().updateExpirationAdvanceDays(days)
    }
    
    /**
     * 更新通知时间
     */
    suspend fun updateNotificationTime(time: String) {
        database.reminderSettingsDao().updateNotificationTime(time)
    }
    
    /**
     * 更新库存提醒开关
     */
    suspend fun updateStockReminderEnabled(enabled: Boolean) {
        database.reminderSettingsDao().updateStockReminderEnabled(enabled)
    }
    
    /**
     * 更新保修期提醒开关
     */
    suspend fun updateIncludeWarranty(enabled: Boolean) {
        val settings = getSettings()
        updateSettings(settings.copy(includeWarranty = enabled))
    }
    
    /**
     * 更新推送通知开关
     */
    suspend fun updatePushNotification(enabled: Boolean) {
        val settings = getSettings()
        updateSettings(settings.copy(pushNotificationEnabled = enabled))
    }
    
    /**
     * 更新应用内提醒开关
     */
    suspend fun updateInAppReminder(enabled: Boolean) {
        val settings = getSettings()
        updateSettings(settings.copy(inAppReminderEnabled = enabled))
    }
    
    /**
     * 更新周末暂停开关
     */
    suspend fun updateWeekendPause(enabled: Boolean) {
        val settings = getSettings()
        updateSettings(settings.copy(weekendPause = enabled))
    }
    
    /**
     * 更新免打扰开始时间
     */
    suspend fun updateQuietHourStart(time: String) {
        val settings = getSettings()
        updateSettings(settings.copy(quietHourStart = time))
    }
    
    /**
     * 更新免打扰结束时间
     */
    suspend fun updateQuietHourEnd(time: String) {
        val settings = getSettings()
        updateSettings(settings.copy(quietHourEnd = time))
    }
    
    // ==================== 分类阈值相关 ====================
    
    /**
     * 获取所有分类阈值（Flow版本）
     */
    fun getAllThresholdsFlow(): Flow<List<CategoryThresholdEntity>> {
        return database.categoryThresholdDao().getAllThresholdsFlow()
    }
    
    /**
     * 获取所有分类阈值
     */
    suspend fun getAllThresholds(): List<CategoryThresholdEntity> {
        return database.categoryThresholdDao().getAllThresholds()
    }
    
    /**
     * 获取启用的分类阈值映射表
     */
    suspend fun getEnabledThresholdsMap(): Map<String, CategoryThresholdEntity> {
        return database.categoryThresholdDao().getEnabledThresholds()
            .associateBy { it.category }
    }
    
    /**
     * 获取特定分类的阈值
     */
    suspend fun getThresholdByCategory(category: String): CategoryThresholdEntity? {
        return database.categoryThresholdDao().getThresholdByCategory(category)
    }
    
    /**
     * 更新或插入分类阈值
     */
    suspend fun updateCategoryThreshold(threshold: CategoryThresholdEntity) {
        database.categoryThresholdDao().insertOrUpdateThreshold(
            threshold.copy(updatedAt = Date())
        )
    }
    
    /**
     * 更新分类的最小数量
     */
    suspend fun updateCategoryMinQuantity(category: String, minQuantity: Double) {
        database.categoryThresholdDao().updateMinQuantity(category, minQuantity)
    }
    
    /**
     * 初始化默认分类阈值
     */
    suspend fun initializeDefaultCategoryThresholds() {
        database.categoryThresholdDao().initializeDefaultThresholds()
    }
    
    // ==================== 自定义规则相关 ====================
    
    /**
     * 获取所有自定义规则（Flow版本）
     */
    fun getAllRulesFlow(): Flow<List<CustomRuleEntity>> {
        return database.customRuleDao().getAllRulesFlow()
    }
    
    /**
     * 获取所有启用的自定义规则
     */
    suspend fun getActiveRules(): List<CustomRuleEntity> {
        return database.customRuleDao().getActiveRules()
    }
    
    /**
     * 根据规则类型获取启用的规则
     */
    suspend fun getActiveRulesByType(type: String): List<CustomRuleEntity> {
        return database.customRuleDao().getRulesByType(type)
    }
    
    /**
     * 根据ID获取规则
     */
    suspend fun getRuleById(id: Long): CustomRuleEntity? {
        return database.customRuleDao().getRuleById(id)
    }
    
    /**
     * 插入新规则
     */
    suspend fun insertRule(rule: CustomRuleEntity): Long {
        return database.customRuleDao().insertRule(rule)
    }
    
    /**
     * 更新规则
     */
    suspend fun updateRule(rule: CustomRuleEntity) {
        database.customRuleDao().updateRule(
            rule.copy(updatedAt = Date())
        )
    }
    
    /**
     * 删除规则
     */
    suspend fun deleteRule(ruleId: Long) {
        database.customRuleDao().deleteRuleById(ruleId)
    }
    
    /**
     * 启用/禁用规则
     */
    suspend fun updateRuleEnabled(ruleId: Long, enabled: Boolean) {
        database.customRuleDao().updateRuleEnabled(ruleId, enabled)
    }
    
    /**
     * 更新规则最后触发时间
     */
    suspend fun updateRuleLastTriggered(ruleId: Long) {
        database.customRuleDao().updateLastTriggeredAt(ruleId, Date())
    }
    
    /**
     * 获取针对特定分类的规则
     */
    suspend fun getRulesForCategory(category: String): List<CustomRuleEntity> {
        return database.customRuleDao().getRulesForCategory(category)
    }
    
    /**
     * 获取针对特定物品的规则
     */
    suspend fun getRulesForItem(itemName: String): List<CustomRuleEntity> {
        return database.customRuleDao().getRulesForItem(itemName)
    }
    
    /**
     * 获取所有物品的分类列表（去重）
     */
    suspend fun getAllItemCategories(): List<String> {
        return database.unifiedItemDao().getAllCategories()
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 初始化默认数据
     */
    suspend fun initializeDefaultData() {
        // 初始化默认设置
        getSettings()
        
        // 初始化默认分类阈值
        val existingThresholds = getAllThresholds()
        if (existingThresholds.isEmpty()) {
            initializeDefaultCategoryThresholds()
        }
    }
    
    /**
     * 获取提醒统计信息
     */
    suspend fun getReminderStats(): ReminderStats {
        val settings = getSettings()
        val thresholdCount = database.categoryThresholdDao().getAllThresholds().size
        val activeRulesCount = database.customRuleDao().getActiveRulesCount()
        
        return ReminderStats(
            isReminderEnabled = settings.pushNotificationEnabled || settings.inAppReminderEnabled,
            expirationAdvanceDays = settings.expirationAdvanceDays,
            stockReminderEnabled = settings.stockReminderEnabled,
            categoryThresholdCount = thresholdCount,
            activeCustomRulesCount = activeRulesCount
        )
    }
    
    companion object {
        @Volatile
        private var INSTANCE: ReminderSettingsRepository? = null
        
        fun getInstance(database: AppDatabase): ReminderSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ReminderSettingsRepository(database)
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * 提醒统计数据
 */
data class ReminderStats(
    val isReminderEnabled: Boolean,
    val expirationAdvanceDays: Int,
    val stockReminderEnabled: Boolean,
    val categoryThresholdCount: Int,
    val activeCustomRulesCount: Int
)
