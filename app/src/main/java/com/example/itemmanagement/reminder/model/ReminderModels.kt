package com.example.itemmanagement.reminder.model

import com.example.itemmanagement.data.entity.CustomRuleEntity
import com.example.itemmanagement.data.relation.ItemWithDetails
import java.util.Date

/**
 * 提醒汇总数据
 */
data class ReminderSummary(
    val expiredItems: List<ItemWithDetails> = emptyList(),
    val expiringItems: List<ItemWithDetails> = emptyList(),
    val lowStockItems: List<LowStockItem> = emptyList(),
    val customRuleMatches: List<CustomRuleMatch> = emptyList(),
    val warrantyExpiringItems: List<WarrantyExpiringItem> = emptyList(),  // 新增：独立保修到期提醒
    val borrowExpiringItems: List<BorrowExpiringItem> = emptyList(),  // 新增：借还到期提醒
    val generateTime: Date = Date()
) {
    /**
     * 获取总的需要关注的物品数量
     */
    fun getTotalCount(): Int {
        return expiredItems.size + expiringItems.size + lowStockItems.size + customRuleMatches.size + warrantyExpiringItems.size + borrowExpiringItems.size
    }
    
    /**
     * 获取紧急程度的物品数量
     */
    fun getUrgentCount(): Int {
        return expiredItems.size + lowStockItems.count { it.isUrgent() } + warrantyExpiringItems.count { it.isUrgent() } + borrowExpiringItems.count { it.isUrgent() }
    }
    
    /**
     * 是否有需要关注的物品
     */
    fun hasItems(): Boolean {
        return getTotalCount() > 0
    }
}

/**
 * 库存不足物品
 */
data class LowStockItem(
    val item: ItemWithDetails,
    val currentQuantity: Double,
    val requiredQuantity: Double,
    val shortfallQuantity: Double = requiredQuantity - currentQuantity
) {
    /**
     * 是否为紧急情况（库存为0或负数）
     */
    fun isUrgent(): Boolean {
        return currentQuantity <= 0
    }
    
    /**
     * 获取库存状态描述
     */
    fun getStatusDescription(): String {
        return when {
            currentQuantity <= 0 -> "已用完"
            currentQuantity < requiredQuantity * 0.3 -> "严重不足"
            currentQuantity < requiredQuantity * 0.5 -> "库存不足"
            else -> "即将不足"
        }
    }
}

/**
 * 自定义规则匹配结果
 */
data class CustomRuleMatch(
    val item: ItemWithDetails,
    val rule: CustomRuleEntity,
    val matchReason: String = "",
    val calculatedValue: Double? = null  // 如果是数量相关的规则，存储计算值
) {
    /**
     * 获取优先级
     */
    fun getPriority(): ReminderPriority {
        return when (rule.priority) {
            "URGENT" -> ReminderPriority.URGENT
            "IMPORTANT" -> ReminderPriority.IMPORTANT
            else -> ReminderPriority.NORMAL
        }
    }
    
    /**
     * 获取自定义消息，如果没有则生成默认消息
     */
    fun getMessage(): String {
        return if (rule.customMessage.isNotBlank()) {
            rule.customMessage.replace("{itemName}", item.item.name)
        } else {
            generateDefaultMessage()
        }
    }
    
    private fun generateDefaultMessage(): String {
        return when (rule.ruleType) {
            "EXPIRATION" -> "${item.item.name} 将在 ${rule.advanceDays} 天内到期"
            "WARRANTY" -> "${item.item.name} 的保修期将在 ${rule.advanceDays} 天内到期"
            "STOCK" -> "${item.item.name} 的库存量低于 ${rule.minQuantity}"
            else -> "${item.item.name} 符合自定义规则：${rule.name}"
        }
    }
}

/**
 * 提醒项目（通用）
 */
data class ReminderItem(
    val id: String = "${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}",
    val title: String,
    val message: String,
    val priority: ReminderPriority,
    val type: ReminderType,
    val itemId: Long? = null,
    val ruleId: Long? = null,
    val createdAt: Date = Date(),
    val daysUntilEvent: Int? = null,  // 距离事件发生的天数（正数表示未来，负数表示过去）
    val relatedQuantity: Double? = null  // 相关的数量信息
)

/**
 * 提醒类型
 */
enum class ReminderType {
    EXPIRED,           // 已过期
    EXPIRING,          // 即将到期
    WARRANTY_EXPIRING, // 保修即将到期
    LOW_STOCK,         // 库存不足
    OUT_OF_STOCK,      // 已缺货
    CUSTOM_RULE        // 自定义规则
}

/**
 * 提醒优先级
 */
enum class ReminderPriority {
    URGENT,    // 紧急：已过期、已缺货
    IMPORTANT, // 重要：即将到期、库存不足
    NORMAL     // 普通：提前提醒
}

/**
 * 提醒设置快照（用于UI显示）
 */
data class ReminderSettingsSnapshot(
    val expirationAdvanceDays: Int,
    val includeWarranty: Boolean,
    val notificationTime: String,
    val stockReminderEnabled: Boolean,
    val pushNotificationEnabled: Boolean,
    val quietHourStart: String,
    val quietHourEnd: String,
    val categoryThresholds: Map<String, Double>,
    val activeRulesCount: Int
) {
    /**
     * 格式化通知时间显示
     */
    fun getFormattedNotificationTime(): String {
        return "每日 $notificationTime"
    }
    
    /**
     * 格式化勿扰时间显示
     */
    fun getFormattedQuietHours(): String {
        return "$quietHourStart - $quietHourEnd"
    }
    
    /**
     * 获取总体启用状态
     */
    fun isEnabled(): Boolean {
        return pushNotificationEnabled && (stockReminderEnabled || expirationAdvanceDays > 0)
    }
}

/**
 * 保修即将到期物品
 */
data class WarrantyExpiringItem(
    val warrantyId: Long,
    val itemId: Long,
    val itemName: String,
    val itemCategory: String,
    val itemBrand: String?,
    val warrantyEndDate: Date,
    val warrantyProvider: String?,
    val contactInfo: String?,
    val daysUntilExpiration: Int
) {
    /**
     * 是否为紧急情况（已过期或今日到期）
     */
    fun isUrgent(): Boolean {
        return daysUntilExpiration <= 0
    }
    
    /**
     * 获取状态描述
     */
    fun getStatusDescription(): String {
        return when {
            daysUntilExpiration < 0 -> "已过期 ${-daysUntilExpiration} 天"
            daysUntilExpiration == 0 -> "今日到期"
            daysUntilExpiration == 1 -> "明日到期"
            daysUntilExpiration <= 7 -> "将在 $daysUntilExpiration 天后到期"
            daysUntilExpiration <= 30 -> "将在 $daysUntilExpiration 天后到期"
            else -> "还有 $daysUntilExpiration 天到期"
        }
    }
    
    /**
     * 获取优先级
     */
    fun getPriority(): ReminderPriority {
        return when {
            daysUntilExpiration <= 0 -> ReminderPriority.URGENT      // 已过期
            daysUntilExpiration <= 7 -> ReminderPriority.IMPORTANT   // 7天内到期
            else -> ReminderPriority.NORMAL                          // 其他情况
        }
    }
}

/**
 * 借还到期物品
 */
data class BorrowExpiringItem(
    val borrowId: Long,
    val itemId: Long,
    val itemName: String,
    val itemCategory: String,
    val itemBrand: String?,
    val borrowerName: String,
    val borrowerContact: String?,
    val expectedReturnDate: Date,
    val daysUntilReturn: Int
) {
    /**
     * 是否为紧急情况（已逾期或今日到期）
     */
    fun isUrgent(): Boolean {
        return daysUntilReturn <= 0
    }
    
    /**
     * 获取状态描述
     */
    fun getStatusDescription(): String {
        return when {
            daysUntilReturn < 0 -> "已逾期 ${-daysUntilReturn} 天"
            daysUntilReturn == 0 -> "今日到期"
            daysUntilReturn == 1 -> "明日到期"
            daysUntilReturn <= 3 -> "将在 $daysUntilReturn 天后到期"
            daysUntilReturn <= 7 -> "将在 $daysUntilReturn 天后到期"
            else -> "还有 $daysUntilReturn 天到期"
        }
    }
    
    /**
     * 获取优先级
     */
    fun getPriority(): ReminderPriority {
        return when {
            daysUntilReturn <= 0 -> ReminderPriority.URGENT      // 已逾期
            daysUntilReturn <= 3 -> ReminderPriority.IMPORTANT   // 3天内到期
            else -> ReminderPriority.NORMAL                      // 其他情况
        }
    }
}
