package com.example.itemmanagement.data.model

import java.util.Date

data class CalendarEvent(
    val id: Long,
    val itemId: Long,
    val eventType: EventType,
    val title: String,
    val description: String,
    val eventDate: Date,
    val reminderDays: List<Int>, // 提前提醒天数 [1, 3, 7]
    val priority: Priority,
    val isCompleted: Boolean,
    val recurrenceType: RecurrenceType?, // 周期性事件
    val itemName: String = "",
    val category: String = ""
)

enum class EventType(val displayName: String, val icon: String) {
    EXPIRATION("过期提醒", "⏰"),
    WARRANTY("保修到期", "🔧"),
    MAINTENANCE("定期维护", "🛠️"),
    ANNIVERSARY("购买纪念", "🎉"),
    CUSTOM("自定义", "📝"),
    // 操作记录事件
    ITEM_ADDED("添加物品", "📦"),
    ITEM_EDITED("编辑物品", "✏️"),
    ITEM_DELETED("删除物品", "🗑️"),
    SHOPPING_TRANSFERRED("购物入库", "🛒")
}

enum class Priority(val displayName: String, val color: String) {
    URGENT("紧急", "#F44336"),      // 红色
    HIGH("重要", "#FF9800"),        // 橙色  
    NORMAL("正常", "#4CAF50"),      // 绿色
    LOW("低", "#9E9E9E")           // 灰色
}

enum class RecurrenceType(val displayName: String) {
    DAILY("每日"),
    WEEKLY("每周"),
    MONTHLY("每月"),
    YEARLY("每年")
}

data class CalendarDay(
    val date: Date,
    val events: List<CalendarEvent>,
    val isToday: Boolean = false,
    val isCurrentMonth: Boolean = true
)

data class CalendarMonth(
    val year: Int,
    val month: Int,
    val days: List<CalendarDay>
)

// 用于时间轴视图的数据
data class TimelineEvent(
    val event: CalendarEvent,
    val daysUntil: Int, // 距离事件的天数，负数表示已过期
    val urgencyLevel: UrgencyLevel
)

enum class UrgencyLevel(val displayName: String, val color: String) {
    OVERDUE("已过期", "#F44336"),
    URGENT("今日", "#FF5722"),
    SOON("3天内", "#FF9800"),
    UPCOMING("1周内", "#FFC107"),
    NORMAL("正常", "#4CAF50")
} 