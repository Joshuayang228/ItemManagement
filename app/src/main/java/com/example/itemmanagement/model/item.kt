package com.example.itemmanagement.data.model

import android.net.Uri
import java.util.Date

// 位置信息数据类
data class Location(
    val area: String,                  // 区域（如厨房、卧室等，必填）
    val container: String? = null,     // 容器（如冰箱、衣柜等，依赖于area）
    val sublocation: String? = null    // 子位置（如第三层、左侧抽屉等，依赖于container）
) {
    // 确保层级关系正确
    init {
        // 如果area为空，不允许container和sublocation有值
        require(area.isNotBlank()) { "区域(area)不能为空" }

        // 如果container为空，不允许sublocation有值
        if (container.isNullOrBlank()) {
            require(sublocation.isNullOrBlank()) { "当容器(container)为空时，子位置(sublocation)也必须为空" }
        }
    }

    // 获取完整位置字符串
    fun getFullLocationString(): String {
        val parts = mutableListOf<String>()
        parts.add(area)
        if (!container.isNullOrBlank()) {
            parts.add(container)
            if (!sublocation.isNullOrBlank()) {
                parts.add(sublocation)
            }
        }
        return parts.joinToString(" > ")
    }
}

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
    val photos: List<Uri> = emptyList(), // 物品照片列表

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