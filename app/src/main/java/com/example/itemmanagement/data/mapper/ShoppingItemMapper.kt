package com.example.itemmanagement.data.mapper

import com.example.itemmanagement.data.entity.ItemEntity
import com.example.itemmanagement.data.entity.ShoppingItemEntity
import com.example.itemmanagement.data.entity.ShoppingItemPriority
import com.example.itemmanagement.data.entity.UrgencyLevel
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.OpenStatus
import java.util.Date

/**
 * 购物物品与库存物品的转换工具类
 * 确保字段完美对应，支持双向转换
 */
object ShoppingItemMapper {
    
    /**
     * 将购物物品转换为库存物品实体（用于入库）
     */
    fun shoppingItemToItemEntity(
        shoppingItem: ShoppingItemEntity,
        locationId: Long? = null
    ): ItemEntity {
        return ItemEntity(
            id = 0, // 新物品，自动生成ID
            name = shoppingItem.name,
            quantity = shoppingItem.quantity,
            unit = "个", // 默认单位，因为购物物品的数量已包含单位信息
            locationId = locationId,
            category = shoppingItem.category,
            addDate = Date(), // 当前日期作为添加日期
            productionDate = null, // 需要用户后续补充
            expirationDate = null, // 需要用户后续补充
            openStatus = OpenStatus.UNOPENED, // 默认未开封
            openDate = null,
            brand = shoppingItem.brand,
            specification = shoppingItem.specification,
            status = ItemStatus.IN_STOCK, // 默认在库
            stockWarningThreshold = null, // 需要用户后续设置
            price = shoppingItem.actualPrice ?: shoppingItem.estimatedPrice,
            priceUnit = shoppingItem.priceUnit,
            purchaseChannel = shoppingItem.purchaseChannel,
            storeName = shoppingItem.storeName,
            subCategory = shoppingItem.subCategory,
            customNote = shoppingItem.customNote,
            season = shoppingItem.season,
            capacity = shoppingItem.capacity,
            capacityUnit = shoppingItem.capacityUnit,
            rating = shoppingItem.rating,
            totalPrice = shoppingItem.totalPrice ?: shoppingItem.actualPrice,
            totalPriceUnit = shoppingItem.priceUnit,
            purchaseDate = shoppingItem.purchaseDate ?: Date(),
            shelfLife = null, // 需要用户后续补充
            warrantyPeriod = null, // 需要用户后续补充
            warrantyEndDate = null,
            serialNumber = shoppingItem.serialNumber,
            isWishlistItem = false,
            isHighTurnover = false
        )
    }
    
    /**
     * 将库存物品转换为购物物品（用于添加到购物清单）
     */
    fun itemEntityToShoppingItem(
        item: ItemEntity,
        listId: Long,
        quantity: Double = 1.0,
        priority: ShoppingItemPriority = ShoppingItemPriority.NORMAL
    ): ShoppingItemEntity {
        return ShoppingItemEntity(
            id = 0, // 新购物物品，自动生成ID
            listId = listId,
            name = item.name,
            quantity = quantity, // 可以自定义购买数量
            category = item.category,
            brand = item.brand,
            specification = item.specification,
            subCategory = item.subCategory,
            customNote = "从库存物品「${item.name}」添加",
            estimatedPrice = item.price, // 使用原价格作为预估价格
            priceUnit = item.priceUnit,
            totalPrice = item.totalPrice,
            actualPrice = null, // 实际价格待购买时填写
            purchaseChannel = item.purchaseChannel,
            storeName = item.storeName,
            purchaseDate = null, // 购买后填写
            capacity = item.capacity,
            capacityUnit = item.capacityUnit,
            rating = item.rating,
            isPurchased = false,
            priority = priority,
            createdDate = Date(),
            sourceItemId = item.id,
            serialNumber = item.serialNumber,
            season = item.season
        )
    }
    
    /**
     * 将购物物品的字段值提取为Map，用于BaseItemFragment的预填充
     */
    fun shoppingItemToFieldMap(shoppingItem: ShoppingItemEntity): Map<String, Any?> {
        return mapOf(
            // 基础信息字段
            "名称" to shoppingItem.name,
            "数量" to shoppingItem.quantity,
            "分类" to shoppingItem.category,
            "子分类" to shoppingItem.subCategory,
            "品牌" to shoppingItem.brand,
            "规格" to shoppingItem.specification,
            "备注" to shoppingItem.customNote,
            
            // 购物特有价格字段
            "预估价格" to shoppingItem.estimatedPrice,
            "实际价格" to shoppingItem.actualPrice,
            "预算上限" to shoppingItem.budgetLimit,
            "总价" to shoppingItem.totalPrice,
            
            // 购买相关字段
            "购买渠道" to shoppingItem.purchaseChannel,
            "商家名称" to shoppingItem.storeName,
            "首选商店" to shoppingItem.preferredStore,
            "购买日期" to shoppingItem.purchaseDate,
            
            // 优先级和时间管理
            "紧急程度" to shoppingItem.urgencyLevel.name,
            "截止日期" to shoppingItem.deadline,
            "提醒日期" to shoppingItem.remindDate,
            
            // 特殊设置
            "周期性购买" to if (shoppingItem.isRecurring) "是" else "否",
            "周期间隔" to shoppingItem.recurringInterval,
            "推荐原因" to shoppingItem.recommendationReason,
            
            // 其他字段
            "容量" to shoppingItem.capacity,
            "评分" to shoppingItem.rating,
            "序列号" to shoppingItem.serialNumber,
            "季节" to shoppingItem.season,
            "标签" to shoppingItem.tags
        )
    }
    
    /**
     * 从BaseItemFragment的字段值创建购物物品
     */
    fun fieldMapToShoppingItem(
        fieldMap: Map<String, Any?>,
        listId: Long,
        originalId: Long = 0
    ): ShoppingItemEntity {
        return ShoppingItemEntity(
            id = originalId,
            listId = listId,
            
            // 基础信息字段
            name = fieldMap["名称"] as? String ?: "",
            quantity = (fieldMap["数量"] as? Number)?.toDouble() ?: 1.0,
            category = fieldMap["分类"] as? String ?: "",
            subCategory = fieldMap["子分类"] as? String,
            brand = fieldMap["品牌"] as? String,
            specification = fieldMap["规格"] as? String,
            customNote = fieldMap["备注"] as? String,
            
            // 购物特有价格字段
            estimatedPrice = (fieldMap["预估价格"] as? Number)?.toDouble(),
            actualPrice = (fieldMap["实际价格"] as? Number)?.toDouble(),
            budgetLimit = (fieldMap["预算上限"] as? Number)?.toDouble(),
            priceUnit = "元", // 默认价格单位
            totalPrice = (fieldMap["总价"] as? Number)?.toDouble(),
            
            // 购买相关字段
            purchaseChannel = fieldMap["购买渠道"] as? String,
            storeName = fieldMap["商家名称"] as? String,
            preferredStore = fieldMap["首选商店"] as? String,
            purchaseDate = fieldMap["购买日期"] as? Date,
            isPurchased = false, // 默认未购买
            
            // 优先级和时间管理
            priority = ShoppingItemPriority.NORMAL, // 默认优先级
            urgencyLevel = try {
                when (fieldMap["紧急程度"] as? String) {
                    "不急" -> UrgencyLevel.NOT_URGENT
                    "普通" -> UrgencyLevel.NORMAL
                    "急需" -> UrgencyLevel.URGENT
                    "非常急需" -> UrgencyLevel.CRITICAL
                    else -> UrgencyLevel.NORMAL
                }
            } catch (e: Exception) {
                UrgencyLevel.NORMAL
            },
            deadline = fieldMap["截止日期"] as? Date,
            remindDate = fieldMap["提醒日期"] as? Date,
            
            // 特殊设置
            isRecurring = (fieldMap["周期性购买"] as? String) == "是",
            recurringInterval = (fieldMap["周期间隔"] as? Number)?.toInt(),
            recommendationReason = fieldMap["推荐原因"] as? String,
            addedReason = "USER_MANUAL",
            
            // 时间字段
            createdDate = Date(),
            completedDate = null,
            
            // 其他字段
            capacity = (fieldMap["容量"] as? Number)?.toDouble(),
            capacityUnit = fieldMap["容量单位"] as? String,
            rating = (fieldMap["评分"] as? Number)?.toDouble(),
            serialNumber = fieldMap["序列号"] as? String,
            season = fieldMap["季节"] as? String,
            tags = fieldMap["标签"] as? String,
            sourceItemId = null
        )
    }
} 