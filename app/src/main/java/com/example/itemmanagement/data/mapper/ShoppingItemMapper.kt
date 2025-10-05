package com.example.itemmanagement.data.mapper

import com.example.itemmanagement.data.entity.*
import com.example.itemmanagement.data.entity.unified.*
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.OpenStatus
import java.util.Date

/**
 * 统一架构下的购物物品转换工具类
 * 支持在购物清单和库存之间进行状态流转
 */
object ShoppingItemMapper {
    
    /**
     * 将购物详情转换为库存（购买完成时使用）
     * 返回UnifiedItemEntity和InventoryDetailEntity对
     */
    fun shoppingItemToInventory(
        unifiedItem: UnifiedItemEntity,
        shoppingDetail: ShoppingDetailEntity,
        locationId: Long? = null
    ): Pair<UnifiedItemEntity, InventoryDetailEntity> {
        
        // 更新统一项目的基础信息
        val updatedUnifiedItem = unifiedItem.copy(
            updatedDate = Date()
        )
        
        // 创建库存详情
        val inventoryDetail = InventoryDetailEntity(
            itemId = unifiedItem.id,
            quantity = shoppingDetail.quantity,
            unit = shoppingDetail.quantityUnit,
            locationId = locationId,
            productionDate = null, // 需要用户后续补充
            expirationDate = null, // 需要用户后续补充
            openStatus = OpenStatus.UNOPENED, // 默认未开封
            openDate = null,
            status = ItemStatus.IN_STOCK, // 默认在库
            stockWarningThreshold = null, // 需要用户后续设置
            price = shoppingDetail.actualPrice ?: shoppingDetail.estimatedPrice,
            priceUnit = shoppingDetail.actualPriceUnit.takeIf { shoppingDetail.actualPrice != null } 
                ?: shoppingDetail.estimatedPriceUnit,  // ✅ 优先使用实际价格单位，否则使用预估价格单位
            purchaseChannel = null, // 可从shoppingDetail获取
            storeName = shoppingDetail.storeName,
            totalPrice = shoppingDetail.actualPrice,
            totalPriceUnit = shoppingDetail.actualPriceUnit.takeIf { shoppingDetail.actualPrice != null } 
                ?: shoppingDetail.estimatedPriceUnit,  // ✅ 总价单位与价格单位保持一致

            purchaseDate = shoppingDetail.purchaseDate ?: Date(),
            shelfLife = null, // 需要用户后续补充
            warrantyPeriod = null, // 需要用户后续补充
            warrantyEndDate = null,
            isHighTurnover = false,
            wasteDate = null
            // 注意：capacity, rating, season, serialNumber 已移至 UnifiedItemEntity
        )
        
        return Pair(updatedUnifiedItem, inventoryDetail)
    }
    
    /**
     * 将库存物品转换为购物物品（用于添加到购物清单）
     * 基于统一架构的转换
     */
    fun inventoryToShoppingItem(
        unifiedItem: UnifiedItemEntity,
        inventoryDetail: InventoryDetailEntity,
        shoppingListId: Long,
        quantity: Double = 1.0,
        priority: ShoppingItemPriority = ShoppingItemPriority.NORMAL
    ): Pair<UnifiedItemEntity, ShoppingDetailEntity> {
        
        // 统一项目保持不变（除了更新时间）
        val updatedUnifiedItem = unifiedItem.copy(
            updatedDate = Date()
        )
        
        // 创建购物详情
        val shoppingDetail = ShoppingDetailEntity(
            itemId = unifiedItem.id,
            shoppingListId = shoppingListId,
            quantity = quantity, // 可以自定义购买数量
            quantityUnit = inventoryDetail.unit,
            priority = priority,
            urgencyLevel = UrgencyLevel.NORMAL,
            estimatedPrice = inventoryDetail.price,
            estimatedPriceUnit = inventoryDetail.priceUnit ?: "元",  // ✅ 预估价格单位
            actualPrice = null, // 实际价格待购买时填写
            actualPriceUnit = "元",  // ✅ 实际价格单位（默认值）
            budgetLimit = null,
            budgetLimitUnit = "元",  // ✅ 预算上限单位（默认值）

            storeName = inventoryDetail.storeName,
            deadline = null,
            isPurchased = false,
            purchaseDate = null, // 购买后填写
            isRecurring = false,
            recurringInterval = null,
            // 注意：capacity, rating, season, serialNumber 已移至 UnifiedItemEntity
            recommendationReason = "从库存物品「${unifiedItem.name}」添加",
            addedReason = "USER_MANUAL",
            completedDate = null,
            remindDate = null,
            tags = null
        )
        
        return Pair(updatedUnifiedItem, shoppingDetail)
    }
    
    /**
     * 将购物物品的字段值提取为Map，用于BaseItemFragment的预填充
     * 基于统一架构的实现
     */
    fun shoppingItemToFieldMap(
        unifiedItem: UnifiedItemEntity,
        shoppingDetail: ShoppingDetailEntity
    ): Map<String, Any?> {
        return mapOf(
            // 基础信息字段
            "名称" to unifiedItem.name,
            "数量" to shoppingDetail.quantity,
            "分类" to unifiedItem.category,
            "子分类" to unifiedItem.subCategory,
            "品牌" to unifiedItem.brand,
            "规格" to unifiedItem.specification,
            "备注" to unifiedItem.customNote,
            
            // 购物特有价格字段
            "预估价格" to shoppingDetail.estimatedPrice,
            "实际价格" to shoppingDetail.actualPrice,
            "预算上限" to shoppingDetail.budgetLimit,
            
            // 购买相关字段
            "购买商店" to shoppingDetail.storeName,
            "购买日期" to shoppingDetail.purchaseDate,
            
            // 优先级和时间管理
            "优先级" to shoppingDetail.priority.name,
            "紧急程度" to shoppingDetail.urgencyLevel.name,
            "截止日期" to shoppingDetail.deadline,
            "提醒日期" to shoppingDetail.remindDate,
            
            // 特殊设置
            "周期性购买" to if (shoppingDetail.isRecurring) "是" else "否",
            "周期间隔" to shoppingDetail.recurringInterval,
            "推荐原因" to shoppingDetail.recommendationReason,
            
            // 其他字段（从UnifiedItemEntity读取）
            "容量" to unifiedItem.capacity,
            "评分" to unifiedItem.rating,
            "序列号" to unifiedItem.serialNumber,
            "季节" to unifiedItem.season,
            "标签" to shoppingDetail.tags
        )
    }
    
    /**
     * 从BaseItemFragment的字段值创建购物物品
     * 基于统一架构的实现
     */
    fun fieldMapToShoppingItem(
        fieldMap: Map<String, Any?>,
        shoppingListId: Long,
        originalItemId: Long = 0
    ): Pair<UnifiedItemEntity, ShoppingDetailEntity> {
        
        // 创建或更新统一项目
        val unifiedItem = UnifiedItemEntity(
            id = originalItemId,
            name = fieldMap["名称"] as? String ?: "",
            category = fieldMap["分类"] as? String ?: "",
            subCategory = fieldMap["子分类"] as? String,
            brand = fieldMap["品牌"] as? String,
            specification = fieldMap["规格"] as? String,
            customNote = fieldMap["备注"] as? String,
            capacity = (fieldMap["容量"] as? Number)?.toDouble(),
            capacityUnit = fieldMap["容量单位"] as? String,
            rating = (fieldMap["评分"] as? Number)?.toDouble(),
            season = fieldMap["季节"] as? String,
            serialNumber = fieldMap["序列号"] as? String,
            createdDate = Date(),
            updatedDate = Date()
        )
        
        // 创建购物详情
        val shoppingDetail = ShoppingDetailEntity(
            itemId = originalItemId,
            shoppingListId = shoppingListId,
            quantity = (fieldMap["数量"] as? Number)?.toDouble() ?: 1.0,
            quantityUnit = "个", // 默认单位
            priority = try {
                val priorityStr = (fieldMap["重要程度"] ?: fieldMap["优先级"]) as? String  // ✅ 兼容新旧字段
                ShoppingItemPriority.fromDisplayName(priorityStr ?: "") ?: when (priorityStr) {
                    "LOW", "低", "次要" -> ShoppingItemPriority.LOW
                    "NORMAL", "普通", "一般" -> ShoppingItemPriority.NORMAL
                    "HIGH", "高", "重要" -> ShoppingItemPriority.HIGH
                    "URGENT", "CRITICAL", "紧急", "关键" -> ShoppingItemPriority.CRITICAL
                    else -> ShoppingItemPriority.NORMAL
                }
            } catch (e: Exception) {
                ShoppingItemPriority.NORMAL
            },
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
            estimatedPrice = (fieldMap["预估价格"] as? Number)?.toDouble(),
            estimatedPriceUnit = "元",  // ✅ 默认预估价格单位
            actualPrice = (fieldMap["实际价格"] as? Number)?.toDouble(),
            actualPriceUnit = "元",  // ✅ 默认实际价格单位
            budgetLimit = (fieldMap["预算上限"] as? Number)?.toDouble(),
            budgetLimitUnit = "元",  // ✅ 默认预算上限单位

            storeName = (fieldMap["购买商店"] ?: fieldMap["首选商店"]) as? String,
            deadline = fieldMap["截止日期"] as? Date,
            isPurchased = false, // 默认未购买
            purchaseDate = fieldMap["购买日期"] as? Date,
            isRecurring = (fieldMap["周期性购买"] as? String) == "是",
            recurringInterval = (fieldMap["周期间隔"] as? Number)?.toInt(),
            // 注意：capacity, rating, season, serialNumber 已移至 UnifiedItemEntity
            recommendationReason = fieldMap["推荐原因"] as? String,
            addedReason = "USER_MANUAL",
            completedDate = null,
            remindDate = fieldMap["提醒日期"] as? Date,
            tags = fieldMap["标签"] as? String
        )
        
        return Pair(unifiedItem, shoppingDetail)
    }
} 