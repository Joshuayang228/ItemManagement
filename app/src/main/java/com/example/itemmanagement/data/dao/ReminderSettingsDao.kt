package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.ReminderSettingsEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ReminderSettingsDao {
    
    /**
     * 获取提醒设置（Flow版本，用于UI观察）
     */
    @Query("SELECT * FROM reminder_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<ReminderSettingsEntity?>
    
    /**
     * 获取提醒设置（一次性获取）
     */
    @Query("SELECT * FROM reminder_settings WHERE id = 1")
    suspend fun getSettings(): ReminderSettingsEntity?
    
    /**
     * 插入或更新设置
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: ReminderSettingsEntity)
    
    /**
     * 更新特定字段 - 到期提醒设置
     */
    @Query("UPDATE reminder_settings SET expirationAdvanceDays = :days, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateExpirationAdvanceDays(days: Int, updatedAt: Date = Date())
    
    /**
     * 更新特定字段 - 通知时间
     */
    @Query("UPDATE reminder_settings SET notificationTime = :time, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateNotificationTime(time: String, updatedAt: Date = Date())
    
    /**
     * 更新特定字段 - 库存提醒开关
     */
    @Query("UPDATE reminder_settings SET stockReminderEnabled = :enabled, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateStockReminderEnabled(enabled: Boolean, updatedAt: Date = Date())
    
    /**
     * 更新特定字段 - 推送通知开关
     */
    @Query("UPDATE reminder_settings SET pushNotificationEnabled = :enabled, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updatePushNotificationEnabled(enabled: Boolean, updatedAt: Date = Date())
    
    /**
     * 更新特定字段 - 勿扰时间
     */
    @Query("UPDATE reminder_settings SET quietHourStart = :start, quietHourEnd = :end, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateQuietHours(start: String, end: String, updatedAt: Date = Date())
    
    /**
     * 初始化默认设置（如果不存在）
     */
    @Query("INSERT OR IGNORE INTO reminder_settings (id) VALUES (1)")
    suspend fun initializeDefaultSettings()
}
