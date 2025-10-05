package com.example.itemmanagement.ui.add

import com.example.itemmanagement.data.entity.unified.UnifiedItemEntity
import com.example.itemmanagement.data.entity.unified.WishlistDetailEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import java.util.Date

/**
 * 心愿单字段管理器
 * 处理心愿单相关字段的映射和转换
 */
object WishlistFieldManager {

    /**
     * 将心愿单物品转换为字段映射
     */
    fun wishlistItemToFieldMap(
        unifiedItem: UnifiedItemEntity,
        wishlistDetail: WishlistDetailEntity
    ): Map<String, Any> {
        return mapOf(
            "name" to unifiedItem.name,
            "category" to unifiedItem.category,
            "subCategory" to (unifiedItem.subCategory ?: ""),
            "brand" to (unifiedItem.brand ?: ""),
            "specification" to (unifiedItem.specification ?: ""),
            "customNote" to (unifiedItem.customNote ?: ""),
            "price" to (wishlistDetail.price ?: 0.0),
            "targetPrice" to (wishlistDetail.targetPrice ?: 0.0),
            "currentPrice" to (wishlistDetail.currentPrice ?: 0.0),
            "priority" to wishlistDetail.priority.name,
            "urgency" to wishlistDetail.urgency.name,
            "quantity" to wishlistDetail.quantity,
            "quantityUnit" to wishlistDetail.quantityUnit,
            "budgetLimit" to (wishlistDetail.budgetLimit ?: 0.0),
            "purchaseChannel" to (wishlistDetail.purchaseChannel ?: ""),
            "isPriceTrackingEnabled" to wishlistDetail.isPriceTrackingEnabled,
            "sourceUrl" to (wishlistDetail.sourceUrl ?: ""),
            "imageUrl" to (wishlistDetail.imageUrl ?: ""),
            "addedReason" to (wishlistDetail.addedReason ?: ""),
            "isPaused" to wishlistDetail.isPaused
        )
    }

    /**
     * 将字段映射转换为心愿单物品
     */
    fun fieldMapToWishlistItem(
        fieldMap: Map<String, Any>,
        existingItemId: Long? = null
    ): Pair<UnifiedItemEntity, WishlistDetailEntity> {
        
        val unifiedItem = UnifiedItemEntity(
            id = existingItemId ?: 0L,
            name = fieldMap["name"] as? String ?: "",
            category = fieldMap["category"] as? String ?: "",
            subCategory = fieldMap["subCategory"] as? String,
            brand = fieldMap["brand"] as? String,
            specification = fieldMap["specification"] as? String,
            customNote = fieldMap["customNote"] as? String,
            createdDate = Date(),
            updatedDate = Date()
        )

        val wishlistDetail = WishlistDetailEntity(
            itemId = existingItemId ?: 0L,
            price = fieldMap["price"] as? Double,
            targetPrice = fieldMap["targetPrice"] as? Double,
            currentPrice = fieldMap["currentPrice"] as? Double,
            priority = try {
                WishlistPriority.valueOf(fieldMap["priority"] as? String ?: "NORMAL")
            } catch (e: Exception) {
                WishlistPriority.NORMAL
            },
            urgency = try {
                WishlistUrgency.valueOf(fieldMap["urgency"] as? String ?: "NORMAL")
            } catch (e: Exception) {
                WishlistUrgency.NORMAL
            },
            quantity = fieldMap["quantity"] as? Double ?: 1.0,
            quantityUnit = fieldMap["quantityUnit"] as? String ?: "个",
            budgetLimit = fieldMap["budgetLimit"] as? Double,
            purchaseChannel = fieldMap["purchaseChannel"] as? String,
            isPriceTrackingEnabled = fieldMap["isPriceTrackingEnabled"] as? Boolean ?: false,
            sourceUrl = fieldMap["sourceUrl"] as? String,
            imageUrl = fieldMap["imageUrl"] as? String,
            addedReason = fieldMap["addedReason"] as? String,
            isPaused = fieldMap["isPaused"] as? Boolean ?: false,
            createdDate = Date(),
            lastModified = Date()
        )

        return Pair(unifiedItem, wishlistDetail)
    }

    /**
     * 获取默认字段值
     */
    fun getDefaultFields(): Map<String, Any> {
        return mapOf(
            "name" to "",
            "category" to "",
            "subCategory" to "",
            "brand" to "",
            "specification" to "",
            "customNote" to "",
            "price" to 0.0,
            "targetPrice" to 0.0,
            "currentPrice" to 0.0,
            "priority" to WishlistPriority.NORMAL.name,
            "urgency" to WishlistUrgency.NORMAL.name,
            "quantity" to 1.0,
            "quantityUnit" to "个",
            "budgetLimit" to 0.0,
            "purchaseChannel" to "",
            "isPriceTrackingEnabled" to false,
            "sourceUrl" to "",
            "imageUrl" to "",
            "addedReason" to "",
            "isPaused" to false
        )
    }
}
