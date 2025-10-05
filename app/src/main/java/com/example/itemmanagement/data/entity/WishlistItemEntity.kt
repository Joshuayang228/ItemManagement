package com.example.itemmanagement.data.entity

import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import java.util.Date

/**
 * 临时的WishlistItemEntity类，用于向后兼容
 * TODO: 在统一架构完全迁移后删除此类
 * @deprecated 使用UnifiedItemEntity + WishlistDetailEntity替代
 */
@Deprecated("使用UnifiedItemEntity + WishlistDetailEntity替代")
data class WishlistItemEntity(
    val id: Long = 0,
    val name: String,
    val category: String,
    val subCategory: String? = null,
    val brand: String? = null,
    val specification: String? = null,
    val customNote: String? = null,
    val price: Double? = null,
    val targetPrice: Double? = null,
    val currentPrice: Double? = null,
    val priority: WishlistPriority = WishlistPriority.NORMAL,
    val urgency: WishlistUrgency = WishlistUrgency.NORMAL,
    val quantity: Double = 1.0,
    val quantityUnit: String = "个",
    val budgetLimit: Double? = null,
    val purchaseChannel: String? = null,
    val isPriceTrackingEnabled: Boolean = false,
    val sourceUrl: String? = null,
    val imageUrl: String? = null,
    val addedReason: String? = null,
    val isPaused: Boolean = false,
    val addDate: Date = Date(),
    val lastModified: Date = Date()
) {
    /**
     * 检查是否有价格下降
     */
    fun hasPriceDrop(): Boolean {
        return isPriceTrackingEnabled && 
               price != null && 
               currentPrice != null && 
               currentPrice!! < price!!
    }
    
    /**
     * 检查是否达到目标价格
     */
    fun hasReachedTargetPrice(): Boolean {
        return targetPrice != null && 
               currentPrice != null && 
               currentPrice!! <= targetPrice!!
    }
}
