package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "reminder_settings")
data class ReminderSettingsEntity(
    @PrimaryKey val id: Long = 1,
    
    // 到期提醒设置
    val expirationAdvanceDays: Int = 7,           // 过期提前天数
    val includeWarranty: Boolean = true,          // 包含保修期
    val notificationTime: String = "09:00",       // 通知时间
    
    // 库存提醒设置
    val stockReminderEnabled: Boolean = true,     // 库存提醒开关
    
    // 通知设置
    val pushNotificationEnabled: Boolean = true,  // 推送通知
    val inAppReminderEnabled: Boolean = true,     // 应用内提醒
    val quietHourStart: String = "22:00",         // 勿扰开始
    val quietHourEnd: String = "08:00",           // 勿扰结束
    val weekendPause: Boolean = false,            // 周末暂停
    
    // 系统字段
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
