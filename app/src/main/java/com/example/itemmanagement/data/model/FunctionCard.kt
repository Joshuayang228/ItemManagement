package com.example.itemmanagement.data.model

data class FunctionCard(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: Int,
    val type: Type,
    val badgeCount: Int? = null,
    val isEnabled: Boolean = true
) {
    enum class Type {
        // 数据洞察
        ANALYTICS,
        CALENDAR,
        WASTE_REPORT,
        
        // 智能助手
        WISHLIST,
        REMINDER,
        WARRANTY,
        
        // 实用工具
        LENDING,
        BACKUP,
        UTILITY
    }
} 