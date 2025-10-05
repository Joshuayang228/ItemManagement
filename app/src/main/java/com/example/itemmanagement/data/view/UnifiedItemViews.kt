package com.example.itemmanagement.data.view

import androidx.room.Embedded
import com.example.itemmanagement.data.entity.LocationEntity
import com.example.itemmanagement.data.entity.unified.InventoryDetailEntity
import com.example.itemmanagement.data.entity.unified.ItemStateEntity
import com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity
import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
import com.example.itemmanagement.data.entity.unified.WishlistDetailEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import java.util.Date

/**
 * 统一物品视图数据类
 * 用于组合不同状态下的物品信息，完全兼容备份功能
 */

/**
 * 心愿单完整视图 - 基于统一架构的完整心愿单功能
 * 
 * 集成来源：
 * - UnifiedItemEntity: 基础物品信息 (id, name, category, brand等)
 * - WishlistDetailEntity: 心愿单专用字段 (price, priority, urgency等)  
 * - ItemStateEntity: 状态信息 (添加时间, 活跃状态等)
 * 
 * 兼容备份中WishlistItemEntity的所有功能和业务逻辑
 */
data class WishlistItemView(
    // === 基础物品信息 (来自UnifiedItemEntity) ===
    val id: Long,                                   // 统一ID
    val name: String,                               // 物品名称
    val category: String,                           // 分类
    val subCategory: String? = null,                // 子分类
    val brand: String? = null,                      // 品牌
    val specification: String? = null,              // 规格描述
    val customNote: String? = null,                 // 备注说明
    
    // === 心愿单专用信息 (来自WishlistDetailEntity) ===
    val price: Double? = null,                      // 预估价格
    val targetPrice: Double? = null,                // 目标价格
    val priceUnit: String = "元",                   // 价格单位
    val currentPrice: Double? = null,               // 当前市场价格
    val lowestPrice: Double? = null,                // 历史最低价格
    val highestPrice: Double? = null,               // 历史最高价格
    
    // === 购买计划 ===
    val priority: WishlistPriority = WishlistPriority.NORMAL,  // 优先级
    val urgency: WishlistUrgency = WishlistUrgency.NORMAL,     // 紧急程度
    val quantity: Double = 1.0,                     // 购买数量
    val quantityUnit: String = "个",                // 数量单位
    val budgetLimit: Double? = null,                // 预算上限
    val purchaseChannel: String? = null,            // 首选购买渠道
    
    // === 价格跟踪设置 ===
    val isPriceTrackingEnabled: Boolean = false,    // 是否启用价格跟踪
    val priceDropThreshold: Double? = null,         // 降价提醒阈值
    val lastPriceCheck: Date? = null,               // 最后价格检查时间
    
    // === 状态信息 (来自ItemStateEntity) ===
    val isActive: Boolean = true,                   // 是否活跃
    val isPaused: Boolean = false,                  // 是否暂停跟踪
    val addedToWishlistDate: Date,                  // 添加到心愿单时间
    val lastModified: Date,                         // 最后修改时间
    val achievedDate: Date? = null,                 // 实现愿望日期
    
    // === 扩展信息 ===
    val sourceUrl: String? = null,                  // 商品链接
    val imageUrl: String? = null,                   // 商品图片
    val relatedInventoryItemId: Long? = null,       // 关联库存物品ID
    val addedReason: String? = null,                // 添加原因
    
    // === 统计信息 ===
    val viewCount: Int = 0,                         // 查看次数
    val lastViewDate: Date? = null,                 // 最后查看时间
    val priceChangeCount: Int = 0                   // 价格变动次数
) {
    
    // === 业务逻辑方法 (完全兼容备份功能) ===
    
    /**
     * 检查是否需要立即关注
     */
    fun needsImmediateAttention(): Boolean {
        return priority == WishlistPriority.URGENT || 
               urgency.level >= 4 // 高紧急程度
    }
    
    /**
     * 检查是否有价格下降
     */
    fun hasPriceDrop(): Boolean {
        return currentPrice != null && 
               lowestPrice != null && 
               currentPrice!! <= lowestPrice!!
    }
    
    /**
     * 获取价格变化百分比
     */
    fun getPriceChangePercentage(): Double? {
        return if (price != null && currentPrice != null && price != 0.0) {
            ((currentPrice!! - price!!) / price!!) * 100
        } else null
    }
    
    /**
     * 检查是否达到目标价格
     */
    fun hasReachedTargetPrice(): Boolean {
        return currentPrice != null && 
               targetPrice != null && 
               currentPrice!! <= targetPrice!!
    }
    
    /**
     * 获取显示用的完整名称
     */
    fun getDisplayName(): String {
        return if (brand != null) {
            "$brand $name"
        } else {
            name
        }
    }
    
    /**
     * 检查是否已实现愿望
     */
    fun isAchieved(): Boolean {
        return achievedDate != null
    }
    
    /**
     * 检查是否超出预算
     */
    fun isOverBudget(): Boolean {
        return currentPrice != null && 
               budgetLimit != null && 
               currentPrice!! > budgetLimit!!
    }
    
    /**
     * 获取价格状态显示文本
     */
    fun getPriceStatusText(): String {
        return when {
            currentPrice == null -> "暂无价格"
            hasReachedTargetPrice() -> "已达目标价"
            hasPriceDrop() -> "价格下降"
            isOverBudget() -> "超出预算"
            else -> "¥${String.format("%.0f", currentPrice!!)}"
        }
    }
    
    /**
     * 获取优先级颜色代码
     */
    fun getPriorityColorCode(): String {
        return priority.colorCode
    }
    
    /**
     * 检查是否需要价格更新
     */
    fun needsPriceUpdate(): Boolean {
        if (!isPriceTrackingEnabled || lastPriceCheck == null) return false
        
        val now = System.currentTimeMillis()
        val lastCheck = lastPriceCheck!!.time
        val daysSinceLastCheck = (now - lastCheck) / (1000 * 60 * 60 * 24)
        
        return daysSinceLastCheck >= 7 // 7天未更新价格
    }
}

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
    val deletedReason: String? // 从 ItemStateEntity 获取
)

// 用于物品生命周期追踪
data class ItemLifecycle(
    val unifiedItem: UnifiedItemEntity,
    val states: List<ItemStateEntity>,
    val wishlistDetails: WishlistDetailEntity?,
    val shoppingHistory: List<ShoppingDetailEntity>, // 可能有多个购物记录
    val inventoryDetails: InventoryDetailEntity?
    // 可以添加其他组件信息，如 photos, tags, warranties, borrows
)

