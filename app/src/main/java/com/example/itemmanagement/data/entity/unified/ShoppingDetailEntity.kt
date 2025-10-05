package com.example.itemmanagement.data.entity.unified

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.itemmanagement.data.entity.ShoppingListEntity
import com.example.itemmanagement.data.entity.ShoppingItemPriority
import com.example.itemmanagement.data.entity.UrgencyLevel
import java.util.Date

/**
 * 购物详情实体
 * 存储物品在购物清单状态下的专用字段
 * 与unified_items通过itemId关联
 */
@Entity(
    tableName = "shopping_details",
    foreignKeys = [
        ForeignKey(
            entity = UnifiedItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["shoppingListId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["itemId"], unique = false),  // 不设置unique，因为同一物品可能多次加入购物清单
        Index("shoppingListId"),
        Index("priority"),
        Index("urgencyLevel"),
        Index("isPurchased"),
        Index("deadline"),
        Index(value = ["shoppingListId", "isPurchased"])  // 组合索引
    ]
)
data class ShoppingDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                                    // 自增主键（支持同一物品多次购买历史）
    
    /** 关联的物品ID */
    val itemId: Long,                                    // 外键 -> unified_items.id
    
    /** 所属购物清单ID */
    val shoppingListId: Long,                            // 外键 -> shopping_lists.id
    
    // === 数量和单位 ===
    val quantity: Double,                                // 购买数量
    val quantityUnit: String = "个",                     // 数量单位
    
    // === 价格相关字段（购物特有）===
    val estimatedPrice: Double? = null,                  // 预估价格
    val estimatedPriceUnit: String = "元",               // 预估价格单位
    val actualPrice: Double? = null,                     // 实际价格
    val actualPriceUnit: String = "元",                  // 实际价格单位
    val budgetLimit: Double? = null,                     // 预算上限
    val budgetLimitUnit: String = "元",                  // 预算上限单位
    val totalPrice: Double? = null,                      // 总价（数量×单价）
    val totalPriceUnit: String? = null,                  // 总价单位
    
    // === 购买相关字段 ===
    val purchaseChannel: String? = null,                 // 购买渠道
    val storeName: String? = null,                       // 购买商店（统一字段，合并了原preferredStore）
    val purchaseDate: Date? = null,                      // 购买日期
    val isPurchased: Boolean = false,                    // 是否已购买
    
    // === 优先级和紧急程度（购物特有）===
    val priority: ShoppingItemPriority = ShoppingItemPriority.NORMAL, // 优先级
    val urgencyLevel: UrgencyLevel = UrgencyLevel.NORMAL,     // 紧急程度
    val deadline: Date? = null,                               // 截止日期
    
    // === 物品属性字段（已废弃，迁移到UnifiedItemEntity）===
    // val capacity: Double? = null,                             // 容量 -> 已移至UnifiedItemEntity
    // val capacityUnit: String? = null,                         // 容量单位 -> 已移至UnifiedItemEntity
    // val rating: Double? = null,                               // 评分 -> 已移至UnifiedItemEntity
    // val season: String? = null,                               // 季节 -> 已移至UnifiedItemEntity
    // val serialNumber: String? = null,                         // 序列号 -> 已移至UnifiedItemEntity
    
    // === 关联和来源字段 ===
    val sourceItemId: Long? = null,                           // 来源库存物品ID（从库存添加时）
    val recommendationReason: String? = null,                 // 推荐原因
    val addedReason: String = "USER_MANUAL",                  // 添加原因
    
    // === 时间字段 ===
    val addDate: Date = Date(),                               // 创建日期
    val completedDate: Date? = null,                          // 完成日期
    val remindDate: Date? = null,                             // 提醒日期
    
    // === 周期性购买 ===
    val isRecurring: Boolean = false,                         // 是否为周期性购买
    val recurringInterval: Int? = null,                       // 周期间隔（天数）
    
    // === 标签（临时存储，最终会迁移到tags表关联）===
    val tags: String? = null                                  // 标签（逗号分隔）
) {
    /**
     * 获取综合购买优先级（艾森豪威尔矩阵）
     * 返回值：1-10，数值越大越优先
     */
    fun getOverallPriority(): Int {
        val importanceScore = priority.level * 2  // 重要性权重为2
        val urgencyScore = urgencyLevel.level * 1 // 紧急性权重为1
        val deadlineBonus = if (isNearDeadline()) 2 else 0
        return importanceScore + urgencyScore + deadlineBonus
    }
    
    /**
     * 检查是否需要立即购买
     * 使用艾森豪威尔矩阵第一象限：重要且紧急
     */
    fun needsImmediatePurchase(): Boolean {
        return (priority == ShoppingItemPriority.CRITICAL && urgencyLevel.level >= UrgencyLevel.NORMAL.level) ||
               (urgencyLevel == UrgencyLevel.CRITICAL) ||
               isOverdue()
    }
    
    /**
     * 检查是否需要紧急购买（兼容旧方法）
     * @deprecated 使用 needsImmediatePurchase() 或 getOverallPriority()
     */
    @Deprecated("使用 needsImmediatePurchase() 或 getOverallPriority()", ReplaceWith("needsImmediatePurchase()"))
    fun needsUrgentPurchase(): Boolean {
        return needsImmediatePurchase()
    }
    
    /**
     * 检查是否接近截止日期（3天内）
     */
    fun isNearDeadline(): Boolean {
        if (deadline == null) return false
        
        val now = System.currentTimeMillis()
        val deadlineTime = deadline.time
        val daysUntilDeadline = (deadlineTime - now) / (1000 * 60 * 60 * 24)
        
        return daysUntilDeadline <= 3
    }
    
    /**
     * 检查是否已超过截止日期
     */
    fun isOverdue(): Boolean {
        return deadline != null && Date().after(deadline) && !isPurchased
    }
    
    /**
     * 获取预算使用率
     */
    fun getBudgetUsageRate(): Double? {
        return if (budgetLimit != null && totalPrice != null && budgetLimit > 0) {
            (totalPrice / budgetLimit).coerceAtMost(1.0)
        } else null
    }
    
    /**
     * 获取完整的购买计划描述
     */
    fun getPurchasePlanDescription(): String {
        return buildString {
            append("购买 $quantity $quantityUnit")
            if (storeName != null) {
                append("，商店 $storeName")
            }
            if (deadline != null) {
                append("，截止 ${deadline}")
            }
            if (budgetLimit != null) {
                append("，预算 $budgetLimit $budgetLimitUnit")
            }
        }
    }
    
    /**
     * 计算实际花费与预算的差异
     */
    fun getBudgetVariance(): Double? {
        return if (budgetLimit != null && actualPrice != null) {
            actualPrice - budgetLimit
        } else null
    }
    
    /**
     * 检查是否超出预算
     */
    fun isOverBudget(): Boolean {
        val variance = getBudgetVariance()
        return variance != null && variance > 0
    }
}
