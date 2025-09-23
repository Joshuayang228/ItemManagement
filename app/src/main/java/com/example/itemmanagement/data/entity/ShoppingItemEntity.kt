package com.example.itemmanagement.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * 购物物品优先级枚举
 */
enum class ShoppingItemPriority {
    LOW,     // 低优先级
    NORMAL,  // 普通
    HIGH,    // 高优先级
    URGENT   // 紧急
}

/**
 * 购物物品紧急程度枚举
 */
enum class UrgencyLevel {
    NOT_URGENT,     // 不急
    NORMAL,         // 普通
    URGENT,         // 急需
    CRITICAL        // 非常急需
}

/**
 * 购物物品实体 - 参考ItemEntity和Field定义，专为购物清单优化
 * 移除了不必要的unit字段，添加了购物特有的字段
 */
@Parcelize
@Entity(
    tableName = "shopping_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,                                         // 外键：属于哪个购物清单
    
    // === 基础信息字段（参考ItemEntity）===
    val name: String,                                         // 物品名称 *必需
    val quantity: Double,                                     // 数量（包含单位信息，如"2瓶"、"1公斤"）
    val category: String,                                     // 分类
    val subCategory: String? = null,                          // 子分类
    val brand: String? = null,                                // 品牌
    val specification: String? = null,                        // 规格
    val customNote: String? = null,                           // 备注
    
    // === 价格相关字段（购物特有）===
    val price: Double? = null,                                // 预估价格 *购物特有
    val actualPrice: Double? = null,                          // 实际价格 *购物特有
    val priceUnit: String? = "元",                            // 价格单位
    val budgetLimit: Double? = null,                          // 预算上限 *购物特有
    val totalPrice: Double? = null,                           // 总价（数量×单价）
    
    // === 购买相关字段 ===
    val purchaseChannel: String? = null,                      // 购买渠道
    val storeName: String? = null,                            // 商店名称
    val preferredStore: String? = null,                       // 首选商店 *购物特有
    val purchaseDate: Date? = null,                           // 购买日期
    val isPurchased: Boolean = false,                         // 是否已购买 *购物特有
    
    // === 优先级和紧急程度（购物特有）===
    val priority: ShoppingItemPriority = ShoppingItemPriority.NORMAL, // 优先级
    val urgencyLevel: UrgencyLevel = UrgencyLevel.NORMAL,     // 紧急程度 *购物特有
    val deadline: Date? = null,                               // 截止日期 *购物特有
    
    // === 物品属性字段 ===
    val capacity: Double? = null,                             // 容量
    val capacityUnit: String? = null,                         // 容量单位
    val rating: Double? = null,                               // 评分
    val season: String? = null,                               // 季节
    
    // === 关联和来源字段 ===
    val sourceItemId: Long? = null,                           // 来源库存物品ID（从库存添加时）
    val recommendationReason: String? = null,                 // 推荐原因 *购物特有
    val addedReason: String? = "USER_MANUAL",                 // 添加原因 *购物特有
    
    // === 时间字段 ===
    val addDate: Date = Date(),                               // 创建日期
    val completedDate: Date? = null,                          // 完成日期 *购物特有
    val remindDate: Date? = null,                             // 提醒日期 *购物特有
    
    // === 其他字段 ===
    val serialNumber: String? = null,                         // 序列号
    val isRecurring: Boolean = false,                         // 是否为周期性购买 *购物特有
    val recurringInterval: Int? = null,                       // 周期间隔（天数） *购物特有
    val tags: String? = null                                  // 标签（逗号分隔）
) : Parcelable 