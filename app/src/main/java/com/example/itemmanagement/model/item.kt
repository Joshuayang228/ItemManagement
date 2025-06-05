package com.example.itemmanagement.data.model

import java.util.Date

// 位置信息数据类
data class Location(
    val room: String,           // 房间（必填）
    val container: String? = null,     // 容器（如冰箱/柜子）
    val specificLocation: String? = null // 具体位置
)

// 开封状态枚举
enum class OpenStatus {
    UNOPENED,   // 未开封
    OPENED      // 已开封
}

// 物品状态枚举
enum class ItemStatus {
    IN_STOCK,   // 在库
    USED_UP,    // 已用完
    EXPIRED,    // 已过期
    GIVEN_AWAY  // 已转赠
}

// 主数据类
data class Item(
    // 核心属性（必填项）
    val id: Long = 0,                // 数据库主键
    val name: String,                // 物品名称
    val quantity: Double,            // 数量
    val unit: String,               // 单位
    val location: Location,          // 位置信息
    val category: String,            // 基础分类
    val addDate: Date = Date(),      // 添加日期

    // 时效管理属性
    val productionDate: Date? = null,    // 生产日期
    val expirationDate: Date? = null,    // 到期日期
    val openStatus: OpenStatus = OpenStatus.UNOPENED,  // 开封状态
    val openDate: Date? = null,          // 开封日期

    // 规格属性
    val brand: String? = null,           // 品牌
    val specification: String? = null,    // 规格描述
    val imageUrl: String? = null,        // 图片URL

    // 状态与管理属性
    val status: ItemStatus = ItemStatus.IN_STOCK,  // 物品状态
    val stockWarningThreshold: Int? = null,        // 库存预警值

    // 商业属性
    val price: Double? = null,           // 价格
    val purchaseChannel: String? = null, // 购买渠道
    val storeName: String? = null,       // 商家名称

    // 分类与标识
    val tags: List<String> = emptyList(),    // 标签
    val subCategory: String? = null,         // 子分类

    // 高级属性
    val serialNumber: String? = null,    // 批号/序列号
    val warrantyDate: Date? = null,      // 保修日期
    val targetUser: String? = null,      // 适用人群
    val lastUsedDate: Date? = null,      // 最后使用时间
    val usageFrequency: String? = null,  // 消耗频率

    // 自由信息
    val customNote: String? = null       // 自定义补充说明
)