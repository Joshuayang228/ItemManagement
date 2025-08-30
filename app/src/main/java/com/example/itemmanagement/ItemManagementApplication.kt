package com.example.itemmanagement

import android.app.Application
import com.example.itemmanagement.data.AppDatabase
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.repository.ReminderSettingsRepository
import com.example.itemmanagement.data.repository.WarrantyRepository
import com.example.itemmanagement.data.repository.BorrowRepository
import com.example.itemmanagement.data.repository.RecycleBinRepository
import com.example.itemmanagement.data.repository.UserProfileRepository
import com.example.itemmanagement.notification.EnhancedNotificationManager
import com.example.itemmanagement.reminder.ReminderManager
import com.example.itemmanagement.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ItemManagementApplication : Application() {
    // 数据库和基础仓库
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ItemRepository(database.itemDao(), database) }
    
    // 保修管理仓库
    val warrantyRepository by lazy { WarrantyRepository(database.warrantyDao()) }
    
    // 借还管理仓库
    val borrowRepository by lazy { BorrowRepository(database.borrowDao()) }
    
    // 回收站仓库
    val recycleBinRepository by lazy { RecycleBinRepository(database.recycleBinDao()) }
    
    // 用户资料仓库
    val userProfileRepository by lazy { UserProfileRepository(database.userProfileDao()) }
    
    // 提醒系统组件
    val reminderSettingsRepository by lazy { ReminderSettingsRepository.getInstance(database) }
    val reminderManager by lazy { ReminderManager.getInstance(repository, reminderSettingsRepository, warrantyRepository, borrowRepository) }
    
    // 统一的增强版通知管理器
    val notificationManager by lazy { EnhancedNotificationManager(this) }
    
    // 提醒调度器
    val reminderScheduler by lazy { ReminderScheduler(this) }
    
    override fun onCreate() {
        super.onCreate()
        // 初始化增强版通知渠道
        notificationManager.createNotificationChannels()
        
        // 异步初始化提醒系统默认数据和检查提醒
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 初始化默认数据
                reminderSettingsRepository.initializeDefaultData()
                
                // 检查是否需要发送提醒（应用启动时）
                reminderScheduler.checkAndSendReminders()
            } catch (e: Exception) {
                // 初始化失败不影响应用启动
                e.printStackTrace()
            }
        }
    }
} 