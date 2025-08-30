package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "custom_rules")
data class CustomRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    
    // 规则基本信息
    val name: String,                             // 规则名称
    val description: String,                      // 规则描述
    
    // 目标范围（可选，为空则适用于所有）
    val targetCategory: String? = null,           // 目标分类
    val targetItemName: String? = null,           // 目标物品名称（支持模糊匹配）
    
    // 触发条件
    val ruleType: String,                         // 规则类型：EXPIRATION, STOCK, WARRANTY
    val advanceDays: Int? = null,                 // 提前天数（用于到期和保修规则）
    val minQuantity: Double? = null,              // 最小数量（用于库存规则）
    val maxQuantity: Double? = null,              // 最大数量（用于过量提醒）
    
    // 提醒设置
    val customMessage: String = "",               // 自定义提醒消息
    val priority: String = "NORMAL",              // 优先级：URGENT, IMPORTANT, NORMAL
    
    // 状态和元数据
    val enabled: Boolean = true,                  // 是否启用
    val createdAt: Date = Date(),                 // 创建时间
    val updatedAt: Date = Date(),                 // 更新时间
    val lastTriggeredAt: Date? = null             // 最后触发时间
)

// 规则类型枚举
enum class RuleType {
    EXPIRATION,    // 到期提醒
    STOCK,         // 库存提醒  
    WARRANTY       // 保修提醒
}

// 优先级枚举
enum class ReminderPriority {
    URGENT,        // 紧急
    IMPORTANT,     // 重要
    NORMAL         // 普通
}
