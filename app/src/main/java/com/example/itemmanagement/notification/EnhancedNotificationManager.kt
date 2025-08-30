package com.example.itemmanagement.notification

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.itemmanagement.MainActivity
import com.example.itemmanagement.R
import com.example.itemmanagement.reminder.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 增强版通知管理器 - 支持完整的周期提醒功能
 */
class EnhancedNotificationManager(private val context: Context) {
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        // 通知渠道
        const val CHANNEL_GROUP_ID = "reminder_group"
        const val CHANNEL_URGENT_ID = "urgent_reminders"
        const val CHANNEL_IMPORTANT_ID = "important_reminders" 
        const val CHANNEL_NORMAL_ID = "normal_reminders"
        
        // 通知ID基础值
        const val NOTIFICATION_ID_URGENT_BASE = 1000
        const val NOTIFICATION_ID_IMPORTANT_BASE = 2000
        const val NOTIFICATION_ID_NORMAL_BASE = 3000
        const val NOTIFICATION_ID_SUMMARY = 999
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * 创建通知渠道和渠道组
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 创建渠道组
            val channelGroup = NotificationChannelGroup(
                CHANNEL_GROUP_ID,
                "物品提醒"
            )
            notificationManager.createNotificationChannelGroup(channelGroup)
            
            // 紧急通知渠道 - 高优先级，有声音和震动
            val urgentChannel = NotificationChannel(
                CHANNEL_URGENT_ID,
                "紧急提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "已过期、已缺货等紧急情况提醒"
                group = CHANNEL_GROUP_ID
                enableVibration(true)
                setShowBadge(true)
            }
            
            // 重要通知渠道 - 默认优先级，有声音
            val importantChannel = NotificationChannel(
                CHANNEL_IMPORTANT_ID,
                "重要提醒", 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "即将到期、库存不足等重要提醒"
                group = CHANNEL_GROUP_ID
                setShowBadge(true)
            }
            
            // 普通通知渠道 - 低优先级，无声音
            val normalChannel = NotificationChannel(
                CHANNEL_NORMAL_ID,
                "日常提醒",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "提前提醒、自定义规则等普通提醒"
                group = CHANNEL_GROUP_ID
                setShowBadge(false)
            }
            
            // 注册所有渠道
            notificationManager.createNotificationChannels(
                listOf(urgentChannel, importantChannel, normalChannel)
            )
        }
    }
    
    /**
     * 发送周期提醒汇总通知
     */
    fun sendPeriodicReminderNotification(summary: ReminderSummary) {
        if (!summary.hasItems()) return
        
        val urgentCount = summary.getUrgentCount()
        val totalCount = summary.getTotalCount()
        
        val title = if (urgentCount > 0) {
            "⚠️ 紧急提醒：$urgentCount 项需要立即处理"
        } else {
            "📋 物品提醒：共 $totalCount 项需要关注"
        }
        
        val summaryText = buildSummaryText(summary)
        val detailedText = buildDetailedText(summary)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "expiration_reminder")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SUMMARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = if (urgentCount > 0) CHANNEL_URGENT_ID else CHANNEL_IMPORTANT_ID
        val priority = if (urgentCount > 0) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setNumber(totalCount)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(detailedText)
                    .setBigContentTitle(title)
            )
        
        if (urgentCount > 0) {
            builder.setColor(context.getColor(android.R.color.holo_red_dark))
        }
        
        notificationManager.notify(NOTIFICATION_ID_SUMMARY, builder.build())
    }
    
    /**
     * 发送单个提醒通知
     */
    fun sendIndividualReminderNotification(reminderItem: ReminderItem) {
        val channelId = getChannelIdForPriority(reminderItem.priority)
        val notificationId = generateNotificationId(reminderItem)
        
        val intent = createNavigationIntent(reminderItem)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getIconForType(reminderItem.type))
            .setContentTitle(reminderItem.title)
            .setContentText(reminderItem.message)
            .setPriority(getPriorityForNotification(reminderItem.priority))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(getCategoryForType(reminderItem.type))
            .setWhen(reminderItem.createdAt.time)
            .setShowWhen(true)
        
        // 紧急通知添加特殊样式
        if (reminderItem.priority == ReminderPriority.URGENT) {
            builder.setColor(context.getColor(android.R.color.holo_red_dark))
        }
        
        notificationManager.notify(notificationId, builder.build())
    }
    
    /**
     * 检查通知权限和勿扰时间
     */
    fun canSendNotification(quietHourStart: String, quietHourEnd: String): Boolean {
        return areNotificationsEnabled() && !isInQuietHours(quietHourStart, quietHourEnd)
    }
    
    /**
     * 检查是否为周末（如果设置了周末暂停）
     */
    fun isWeekend(): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
    
    /**
     * 取消所有提醒通知
     */
    fun cancelAllReminderNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_SUMMARY)
        // 取消各个类别的通知（需要记录已发送的通知ID）
    }
    
    /**
     * 检查通知是否启用
     */
    private fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true
        }
    }
    
    /**
     * 检查是否在勿扰时间内
     */
    private fun isInQuietHours(startTime: String, endTime: String): Boolean {
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val current = timeToMinutes(currentTime)
        val start = timeToMinutes(startTime)
        val end = timeToMinutes(endTime)
        
        return if (start < end) {
            current in start..end
        } else {
            // 跨日情况（如 22:00 - 08:00）
            current >= start || current <= end
        }
    }
    
    /**
     * 构建汇总文本
     */
    private fun buildSummaryText(summary: ReminderSummary): String {
        val parts = mutableListOf<String>()
        
        if (summary.expiredItems.isNotEmpty()) {
            parts.add("${summary.expiredItems.size}个已过期")
        }
        
        if (summary.expiringItems.isNotEmpty()) {
            parts.add("${summary.expiringItems.size}个即将到期")
        }
        
        if (summary.lowStockItems.isNotEmpty()) {
            val urgentStock = summary.lowStockItems.count { it.isUrgent() }
            if (urgentStock > 0) {
                parts.add("${urgentStock}个已缺货")
            } else {
                parts.add("${summary.lowStockItems.size}个库存不足")
            }
        }
        
        if (summary.customRuleMatches.isNotEmpty()) {
            parts.add("${summary.customRuleMatches.size}个自定义提醒")
        }
        
        if (summary.warrantyExpiringItems.isNotEmpty()) {
            val urgentWarranty = summary.warrantyExpiringItems.count { it.isUrgent() }
            if (urgentWarranty > 0) {
                parts.add("${urgentWarranty}个保修已过期")
            } else {
                parts.add("${summary.warrantyExpiringItems.size}个保修即将到期")
            }
        }
        
        if (summary.borrowExpiringItems.isNotEmpty()) {
            val urgentBorrow = summary.borrowExpiringItems.count { it.isUrgent() }
            if (urgentBorrow > 0) {
                parts.add("${urgentBorrow}个借还已逾期")
            } else {
                parts.add("${summary.borrowExpiringItems.size}个借还即将到期")
            }
        }
        
        return parts.joinToString("，")
    }
    
    /**
     * 构建详细文本
     */
    private fun buildDetailedText(summary: ReminderSummary): String {
        return buildString {
            if (summary.expiredItems.isNotEmpty()) {
                appendLine("❌ 已过期物品:")
                summary.expiredItems.take(3).forEach { item ->
                    appendLine("• ${item.item.name}")
                }
                if (summary.expiredItems.size > 3) {
                    appendLine("• 还有 ${summary.expiredItems.size - 3} 个...")
                }
                appendLine()
            }
            
            if (summary.expiringItems.isNotEmpty()) {
                appendLine("⏰ 即将到期:")
                summary.expiringItems.take(3).forEach { item ->
                    appendLine("• ${item.item.name}")
                }
                if (summary.expiringItems.size > 3) {
                    appendLine("• 还有 ${summary.expiringItems.size - 3} 个...")
                }
                appendLine()
            }
            
            if (summary.lowStockItems.isNotEmpty()) {
                appendLine("📦 库存提醒:")
                summary.lowStockItems.take(3).forEach { item ->
                    val status = if (item.isUrgent()) "已缺货" else "库存不足"
                    appendLine("• ${item.item.item.name} ($status)")
                }
                if (summary.lowStockItems.size > 3) {
                    appendLine("• 还有 ${summary.lowStockItems.size - 3} 个...")
                }
                appendLine()
            }
            
            if (summary.warrantyExpiringItems.isNotEmpty()) {
                appendLine("🛡️ 保修提醒:")
                summary.warrantyExpiringItems.take(3).forEach { warranty ->
                    val status = warranty.getStatusDescription()
                    appendLine("• ${warranty.itemName} ($status)")
                }
                if (summary.warrantyExpiringItems.size > 3) {
                    appendLine("• 还有 ${summary.warrantyExpiringItems.size - 3} 个...")
                }
                appendLine()
            }
            
            if (summary.borrowExpiringItems.isNotEmpty()) {
                appendLine("🔄 借还提醒:")
                summary.borrowExpiringItems.take(3).forEach { borrow ->
                    val status = borrow.getStatusDescription()
                    appendLine("• ${borrow.itemName} - ${borrow.borrowerName} ($status)")
                }
                if (summary.borrowExpiringItems.size > 3) {
                    appendLine("• 还有 ${summary.borrowExpiringItems.size - 3} 个...")
                }
                appendLine()
            }
            
            if (summary.customRuleMatches.isNotEmpty()) {
                appendLine("⚙️ 自定义规则:")
                summary.customRuleMatches.take(3).forEach { match ->
                    appendLine("• ${match.item.item.name} (${match.rule.name})")
                }
                if (summary.customRuleMatches.size > 3) {
                    appendLine("• 还有 ${summary.customRuleMatches.size - 3} 个...")
                }
            }
        }
    }
    
    // 工具方法
    private fun timeToMinutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
    
    private fun getChannelIdForPriority(priority: ReminderPriority): String {
        return when (priority) {
            ReminderPriority.URGENT -> CHANNEL_URGENT_ID
            ReminderPriority.IMPORTANT -> CHANNEL_IMPORTANT_ID
            ReminderPriority.NORMAL -> CHANNEL_NORMAL_ID
        }
    }
    
    private fun generateNotificationId(item: ReminderItem): Int {
        val base = when (item.priority) {
            ReminderPriority.URGENT -> NOTIFICATION_ID_URGENT_BASE
            ReminderPriority.IMPORTANT -> NOTIFICATION_ID_IMPORTANT_BASE
            ReminderPriority.NORMAL -> NOTIFICATION_ID_NORMAL_BASE
        }
        return base + (item.itemId?.toInt() ?: item.hashCode()) % 1000
    }
    
    private fun getIconForType(type: ReminderType): Int {
        return when (type) {
            ReminderType.EXPIRED, ReminderType.EXPIRING -> R.drawable.ic_warning
            ReminderType.WARRANTY_EXPIRING -> R.drawable.ic_info
            ReminderType.LOW_STOCK, ReminderType.OUT_OF_STOCK -> R.drawable.ic_inventory
            ReminderType.CUSTOM_RULE -> R.drawable.ic_rule
        }
    }
    
    private fun getPriorityForNotification(priority: ReminderPriority): Int {
        return when (priority) {
            ReminderPriority.URGENT -> NotificationCompat.PRIORITY_HIGH
            ReminderPriority.IMPORTANT -> NotificationCompat.PRIORITY_DEFAULT
            ReminderPriority.NORMAL -> NotificationCompat.PRIORITY_LOW
        }
    }
    
    private fun getCategoryForType(type: ReminderType): String {
        return when (type) {
            ReminderType.EXPIRED, ReminderType.EXPIRING -> NotificationCompat.CATEGORY_REMINDER
            ReminderType.WARRANTY_EXPIRING -> NotificationCompat.CATEGORY_EVENT
            ReminderType.LOW_STOCK, ReminderType.OUT_OF_STOCK -> NotificationCompat.CATEGORY_STATUS
            ReminderType.CUSTOM_RULE -> NotificationCompat.CATEGORY_REMINDER
        }
    }
    
    private fun createNavigationIntent(item: ReminderItem): Intent {
        return Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to", "expiration_reminder")
            putExtra("highlight_item_id", item.itemId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }
    
    // ==================== 兼容旧版API ====================
    
    /**
     * 兼容旧版本的到期通知方法
     */
    fun sendExpirationNotification(
        todayExpiringItems: List<com.example.itemmanagement.data.relation.ItemWithDetails>,
        upcomingExpiringItems: List<com.example.itemmanagement.data.relation.ItemWithDetails>,
        expiredItems: List<com.example.itemmanagement.data.relation.ItemWithDetails>
    ) {
        // 将旧格式数据转换为新的ReminderSummary格式
        val summary = ReminderSummary(
            expiredItems = expiredItems,
            expiringItems = upcomingExpiringItems + todayExpiringItems,
            lowStockItems = emptyList(),
            customRuleMatches = emptyList()
        )
        
        sendPeriodicReminderNotification(summary)
    }
    
    /**
     * 兼容方法：创建单一通知渠道（保持接口一致性）
     */
    fun createNotificationChannel() {
        createNotificationChannels()
    }
}
