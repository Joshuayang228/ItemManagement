package com.example.itemmanagement.data.view

import androidx.room.Embedded
import com.example.itemmanagement.data.entity.LocationEntity
import com.example.itemmanagement.data.entity.unified.InventoryDetailEntity
import com.example.itemmanagement.data.entity.unified.ItemStateEntity
import com.example.itemmanagement.data.entity.unified.ItemStateType
import com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity
import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
import java.util.Date

/**
 * 统一物品视图数据类
 * 用于组合不同状态下的物品信息，完全兼容备份功能
 */

// 用于购物清单视图
data class ShoppingItemView(
    @Embedded val unifiedItem: UnifiedItemEntity,
    @Embedded val shoppingDetail: ShoppingDetailEntity,
    val addedToShoppingDate: Date // 从 ItemStateEntity 获取
)

// 用于库存视图
data class InventoryItemView(
    @Embedded val unifiedItem: UnifiedItemEntity,
    @Embedded val inventoryDetail: InventoryDetailEntity,
    @Embedded(prefix = "location_") val location: LocationEntity?, // 包含位置信息
    val addedToInventoryDate: Date // 从 ItemStateEntity 获取
)

// 用于回收站视图
data class DeletedItemView(
    @Embedded val unifiedItem: UnifiedItemEntity,
    val deletedDate: Date, // 从 ItemStateEntity 获取
    val deletedReason: String?, // 从 ItemStateEntity 获取
    val previousStateType: ItemStateType? = null // 删除前的状态（用于显示来源）
) {
    /**
     * 获取物品ID
     */
    val itemId: Long
        get() = unifiedItem.id
    
    /**
     * 获取物品名称
     */
    val name: String
        get() = unifiedItem.name
    
    /**
     * 获取分类
     */
    val category: String
        get() = unifiedItem.category
    
    /**
     * 获取子分类
     */
    val subCategory: String?
        get() = unifiedItem.subCategory
    
    /**
     * 获取品牌
     */
    val brand: String?
        get() = unifiedItem.brand
    
    /**
     * 获取容量
     */
    val capacity: Double?
        get() = unifiedItem.capacity
    
    /**
     * 获取容量单位
     */
    val capacityUnit: String?
        get() = unifiedItem.capacityUnit
    
    /**
     * 获取评分
     */
    val rating: Double?
        get() = unifiedItem.rating
    
    /**
     * 获取完整分类路径（分类 > 子分类）
     */
    fun getFullCategory(): String {
        return if (subCategory.isNullOrBlank()) {
            category
        } else {
            "$category > $subCategory"
        }
    }
    
    /**
     * 计算距离删除的天数
     */
    fun getDaysSinceDeleted(): Int {
        val now = System.currentTimeMillis()
        val deletedTime = deletedDate.time
        val diffMillis = now - deletedTime
        return java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
    }
    
    /**
     * 计算距离自动清理的剩余天数（30天后自动清理）
     */
    fun getDaysUntilAutoClean(): Int {
        val autoCleanDays = 30
        val daysSinceDeleted = getDaysSinceDeleted()
        return (autoCleanDays - daysSinceDeleted).coerceAtLeast(0)
    }
    
    /**
     * 检查是否即将被自动清理（少于3天）
     */
    fun isNearAutoClean(): Boolean {
        return getDaysUntilAutoClean() <= 3
    }
    
    /**
     * 检查是否已过期（超过30天）
     */
    fun isExpired(): Boolean {
        return getDaysUntilAutoClean() == 0
    }
    
    /**
     * 获取删除原因显示文本
     */
    fun getDeletedReasonDisplay(): String {
        return deletedReason ?: "用户删除"
    }
    
    /**
     * 获取格式化的删除时间
     */
    fun getFormattedDeletedDate(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return sdf.format(deletedDate)
    }
    
    /**
     * 获取简短的删除时间（如"2天前"）
     */
    fun getRelativeDeletedTime(): String {
        val days = getDaysSinceDeleted()
        return when {
            days == 0 -> "今天"
            days == 1 -> "昨天"
            days < 7 -> "${days}天前"
            days < 30 -> "${days / 7}周前"
            else -> getFormattedDeletedDate()
        }
    }
}

// 用于物品生命周期追踪
data class ItemLifecycle(
    val unifiedItem: UnifiedItemEntity,
    val states: List<ItemStateEntity>,
    val shoppingHistory: List<ShoppingDetailEntity>, // 可能有多个购物记录
    val inventoryDetails: InventoryDetailEntity?
    // 可以添加其他组件信息，如 photos, tags, warranties, borrows
)

