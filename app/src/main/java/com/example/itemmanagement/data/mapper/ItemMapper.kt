package com.example.itemmanagement.data.mapper

import com.example.itemmanagement.data.entity.*
import com.example.itemmanagement.data.entity.unified.*
import com.example.itemmanagement.data.model.*
import com.example.itemmanagement.data.relation.ItemWithDetails
import com.example.itemmanagement.data.relation.UnifiedItemWithDetails
import java.util.Date

/**
 * 将UnifiedItemEntity转换为Item领域模型（基础信息）
 * @deprecated 使用UnifiedItemMapper.toItem()替代
 */
@Deprecated("使用UnifiedItemMapper.toItem()替代")
fun UnifiedItemEntity.toItem(): Item {
    return Item(
        id = id,
        name = name,
        quantity = 0.0, // 基础Entity不包含数量，需要从详情获取
        unit = "",      // 基础Entity不包含单位，需要从详情获取
        location = null, // 需要从InventoryDetailEntity获取
        category = category,
        addDate = createdDate,
        productionDate = null,
        expirationDate = null,
        openStatus = null,
        openDate = null,
        brand = brand,
        specification = specification,
        status = ItemStatus.IN_STOCK, // 默认状态
        stockWarningThreshold = null,
        price = null,
        priceUnit = null,
        purchaseChannel = null,
        storeName = null,
        subCategory = subCategory,
        customNote = customNote,
        season = season, // 从UnifiedItemEntity读取
        capacity = capacity, // 从UnifiedItemEntity读取
        capacityUnit = capacityUnit, // 从UnifiedItemEntity读取
        rating = rating, // 从UnifiedItemEntity读取
        totalPrice = null,
        totalPriceUnit = null,
        purchaseDate = null,
        shelfLife = null,
        warrantyPeriod = null,
        warrantyEndDate = null,
        serialNumber = serialNumber, // 从UnifiedItemEntity读取
        locationAddress = locationAddress, // GPS地址
        locationLatitude = locationLatitude, // GPS纬度
        locationLongitude = locationLongitude, // GPS经度
        isHighTurnover = false,
        photos = emptyList(),
        tags = emptyList()
    )
}

/**
 * 将Item领域模型转换为统一架构实体
 * @deprecated 使用UnifiedItemMapper.toInventoryEntities()替代
 */
@Deprecated("使用UnifiedItemMapper.toInventoryEntities()替代")
fun Item.toItemEntity(locationId: Long? = null): UnifiedItemEntity {
    return UnifiedItemEntity(
        id = id,
        name = name,
        category = category,
        subCategory = subCategory,
        brand = brand,
        specification = specification,
        customNote = customNote,
        // 迁移的字段（从InventoryDetail/ShoppingDetail迁移到UnifiedItem）
        capacity = capacity,
        capacityUnit = capacityUnit,
        rating = rating,
        season = season,
        serialNumber = serialNumber,
        // GPS地点信息
        locationAddress = locationAddress,
        locationLatitude = locationLatitude,
        locationLongitude = locationLongitude,
        createdDate = addDate,
        updatedDate = Date()
    )
}

/**
 * 将ItemWithDetails转换为Item领域模型（向后兼容）
 * @deprecated 使用UnifiedItemMapper.toItem()替代
 */
@Deprecated("使用UnifiedItemMapper.toItem()替代")
fun ItemWithDetails.toItem(): Item {
    android.util.Log.d("ItemMapper", "🔄 开始ItemWithDetails到Item的转换")
    android.util.Log.d("ItemMapper", "📋 UnifiedItem: $unifiedItem")
    android.util.Log.d("ItemMapper", "📦 InventoryDetail: $inventoryDetail")
    android.util.Log.d("ItemMapper", "📸 Photos: ${photos?.size}张")
    android.util.Log.d("ItemMapper", "🏷️ Tags: ${tags?.size}个")
    
    val inventoryDetail = this.inventoryDetail
    
    // 构建位置信息
    val location = this.location?.let { locationEntity ->
        android.util.Log.d("ItemMapper", "📍 找到LocationEntity: area='${locationEntity.area}', container='${locationEntity.container}', sublocation='${locationEntity.sublocation}'")
        Location(
            id = locationEntity.id,
            area = locationEntity.area,
            container = locationEntity.container,
            sublocation = locationEntity.sublocation
        )
    }
    android.util.Log.d("ItemMapper", "📍 转换后的Location: ${location?.let { "area='${it.area}', container='${it.container}', sublocation='${it.sublocation}'" } ?: "null"}")
    
    return Item(
        id = unifiedItem.id,
        name = unifiedItem.name,
        quantity = inventoryDetail?.quantity ?: 0.0,
        unit = inventoryDetail?.unit ?: "",
        isQuantityUserInput = inventoryDetail?.isQuantityUserInput ?: false,
        location = location, // 修复：使用构建的位置信息
        category = unifiedItem.category,
        addDate = unifiedItem.createdDate,
        productionDate = inventoryDetail?.productionDate,
        expirationDate = inventoryDetail?.expirationDate,
        openStatus = inventoryDetail?.openStatus,
        openDate = inventoryDetail?.openDate,
        brand = unifiedItem.brand,
        specification = unifiedItem.specification,
        status = inventoryDetail?.status ?: ItemStatus.IN_STOCK,
        stockWarningThreshold = inventoryDetail?.stockWarningThreshold,
        price = inventoryDetail?.price,
        priceUnit = inventoryDetail?.priceUnit,
        purchaseChannel = inventoryDetail?.purchaseChannel,
        storeName = inventoryDetail?.storeName,
        subCategory = unifiedItem.subCategory,
        customNote = unifiedItem.customNote,
        season = unifiedItem.season, // 从UnifiedItemEntity读取
        capacity = unifiedItem.capacity, // 从UnifiedItemEntity读取
        capacityUnit = unifiedItem.capacityUnit, // 从UnifiedItemEntity读取
        rating = unifiedItem.rating, // 从UnifiedItemEntity读取
        totalPrice = inventoryDetail?.totalPrice,
        totalPriceUnit = inventoryDetail?.totalPriceUnit,
        purchaseDate = inventoryDetail?.purchaseDate,
        shelfLife = inventoryDetail?.shelfLife,
        // warrantyPeriod 和 warrantyEndDate 已移至 WarrantyEntity
        warrantyPeriod = null,
        warrantyEndDate = null,
        serialNumber = unifiedItem.serialNumber, // 从UnifiedItemEntity读取
        locationAddress = unifiedItem.locationAddress, // GPS地址
        locationLatitude = unifiedItem.locationLatitude, // GPS纬度
        locationLongitude = unifiedItem.locationLongitude, // GPS经度
        isHighTurnover = inventoryDetail?.isHighTurnover ?: false,
        photos = photos?.map { 
            android.util.Log.d("ItemMapper", "📸 转换照片: ${it.uri}")
            it.toPhoto() 
        } ?: emptyList(),
        tags = tags?.map { 
            android.util.Log.d("ItemMapper", "🏷️ 转换标签: ${it.name}")
            it.toTag() 
        } ?: emptyList()
    ).also { item ->
        android.util.Log.d("ItemMapper", "✅ 转换完成的Item:")
        android.util.Log.d("ItemMapper", "  📋 名称: '${item.name}'")
        android.util.Log.d("ItemMapper", "  📍 位置: ${item.location}")
        android.util.Log.d("ItemMapper", "  🏷️ 标签: ${item.tags.map { it.name }}")
        android.util.Log.d("ItemMapper", "  📸 照片: ${item.photos.map { it.uri }}")
        android.util.Log.d("ItemMapper", "  ⭐ 评分: ${item.rating}")
    }
}

/**
 * 将Location领域模型转换为LocationEntity
 */
fun Location.toLocationEntity(): LocationEntity {
    return LocationEntity(
        id = id,
        area = area,
        container = container,
        sublocation = sublocation
    )
}

/**
 * 将LocationEntity转换为Location领域模型
 */
fun LocationEntity.toLocation(): Location {
    return Location(
        id = id,
        area = area,
        container = container,
        sublocation = sublocation
    )
}

/**
 * 将Photo领域模型转换为PhotoEntity
 */
fun Photo.toPhotoEntity(itemId: Long): PhotoEntity {
    return PhotoEntity(
        id = id,
        itemId = itemId,
        uri = uri,
        isMain = isMain,
        displayOrder = displayOrder
    )
}

/**
 * 将PhotoEntity转换为Photo领域模型
 */
fun PhotoEntity.toPhoto(): Photo {
    return Photo(
        id = id,
        uri = uri,
        isMain = isMain,
        displayOrder = displayOrder
    )
}

/**
 * 将Tag领域模型转换为TagEntity
 */
fun Tag.toTagEntity(): TagEntity {
    return TagEntity(
        id = id,
        name = name,
        color = color
    )
}

/**
 * 将TagEntity转换为Tag领域模型
 */
fun TagEntity.toTag(): Tag {
    return Tag(
        id = id,
        name = name,
        color = color
    )
}

/**
 * 将ItemWithDetails转换为WarehouseItem（用于周期提醒等列表显示）
 */
fun ItemWithDetails.toWarehouseItem(): com.example.itemmanagement.data.model.WarehouseItem {
    val inventoryDetail = this.inventoryDetail
    val location = this.location
    
    // 获取主图URI
    val primaryPhotoUri = photos?.firstOrNull { it.isMain }?.uri 
        ?: photos?.firstOrNull()?.uri
    
    // 获取标签列表字符串
    val tagsList = tags?.joinToString(", ") { it.name }
    
    return com.example.itemmanagement.data.model.WarehouseItem(
        id = unifiedItem.id,
        name = unifiedItem.name,
        primaryPhotoUri = primaryPhotoUri,
        quantity = inventoryDetail?.quantity?.toInt() ?: 0,
        expirationDate = inventoryDetail?.expirationDate?.time,
        locationArea = location?.area,
        locationContainer = location?.container,
        locationSublocation = location?.sublocation,
        category = unifiedItem.category,
        subCategory = unifiedItem.subCategory,
        brand = unifiedItem.brand,
        rating = unifiedItem.rating?.toFloat(),
        price = inventoryDetail?.price,
        priceUnit = inventoryDetail?.priceUnit,
        openStatus = when (inventoryDetail?.openStatus) {
            OpenStatus.OPENED -> true
            OpenStatus.UNOPENED -> false
            else -> null
        },
        addDate = unifiedItem.createdDate.time,
        tagsList = tagsList,
        customNote = unifiedItem.customNote,
        season = unifiedItem.season
    )
} 