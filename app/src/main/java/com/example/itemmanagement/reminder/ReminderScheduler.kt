package com.example.itemmanagement.reminder

import android.content.Context
import com.example.itemmanagement.ItemManagementApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 简化版提醒调度器 - 不使用WorkManager，基于应用启动触发
 */
class ReminderScheduler(private val context: Context) {
    
    private val app = context.applicationContext as ItemManagementApplication
    private val reminderManager = app.reminderManager
    private val settingsRepository = app.reminderSettingsRepository
    private val notificationManager = app.notificationManager
    
    companion object {
        private const val PREFS_NAME = "reminder_scheduler"
        private const val KEY_LAST_CHECK = "last_check_date"
        private const val KEY_LAST_NOTIFICATION = "last_notification_time"
    }
    
    /**
     * 检查是否需要发送提醒（应用启动时调用）
     */
    fun checkAndSendReminders() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.getSettings()
                
                // 检查是否应该发送提醒
                if (shouldSendReminder(settings)) {
                    val summary = reminderManager.getAllReminders()
                    
                    // 检查勿扰时间和周末设置
                    val canSend = canSendNow(settings)
                    
                    if (canSend && summary.hasItems()) {
                        notificationManager.sendPeriodicReminderNotification(summary)
                        updateLastNotificationTime()
                    }
                    
                    // 更新检查时间
                    updateLastCheckDate()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 立即检查并发送提醒（测试用）
     */
    fun sendImmediateReminder() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = reminderManager.getAllReminders()
                if (summary.hasItems()) {
                    notificationManager.sendPeriodicReminderNotification(summary)
                    updateLastNotificationTime()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 判断是否应该发送提醒
     */
    private suspend fun shouldSendReminder(settings: com.example.itemmanagement.data.entity.ReminderSettingsEntity): Boolean {
        // 检查推送通知是否启用
        if (!settings.pushNotificationEnabled) {
            return false
        }
        
        val today = getTodayDateString()
        val lastCheck = getLastCheckDate()
        
        // 每天最多检查一次
        if (today == lastCheck) {
            return false
        }
        
        // 检查是否到了设定的通知时间
        val currentTime = getCurrentTimeString()
        val notificationTime = settings.notificationTime
        
        // 简单的时间比较（可以容错前后30分钟）
        return isTimeToNotify(currentTime, notificationTime)
    }
    
    /**
     * 检查当前是否可以发送通知
     */
    private suspend fun canSendNow(settings: com.example.itemmanagement.data.entity.ReminderSettingsEntity): Boolean {
        // 检查勿扰时间
        if (!notificationManager.canSendNotification(settings.quietHourStart, settings.quietHourEnd)) {
            return false
        }
        
        // 检查周末暂停
        if (settings.weekendPause && notificationManager.isWeekend()) {
            return false
        }
        
        return true
    }
    
    /**
     * 判断是否到了通知时间
     */
    private fun isTimeToNotify(currentTime: String, targetTime: String): Boolean {
        val current = timeToMinutes(currentTime)
        val target = timeToMinutes(targetTime)
        
        // 允许在目标时间前后30分钟内发送
        val tolerance = 30
        return Math.abs(current - target) <= tolerance
    }
    
    /**
     * 获取今日日期字符串
     */
    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    
    /**
     * 获取当前时间字符串
     */
    private fun getCurrentTimeString(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    
    /**
     * 将时间转换为分钟数
     */
    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
    
    /**
     * 获取上次检查日期
     */
    private fun getLastCheckDate(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_CHECK, null)
    }
    
    /**
     * 更新最后检查日期
     */
    private fun updateLastCheckDate() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_CHECK, getTodayDateString())
            .apply()
    }
    
    /**
     * 更新最后通知时间
     */
    private fun updateLastNotificationTime() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_NOTIFICATION, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * 获取最后通知时间
     */
    fun getLastNotificationTime(): Date? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong(KEY_LAST_NOTIFICATION, 0)
        return if (timestamp > 0) Date(timestamp) else null
    }
    
    /**
     * 清除调度数据（用于重置）
     */
    fun clearScheduleData() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
