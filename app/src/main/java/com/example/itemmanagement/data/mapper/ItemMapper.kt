package com.example.itemmanagement.data.mapper

import com.example.itemmanagement.data.entity.*
import com.example.itemmanagement.data.model.*
import com.example.itemmanagement.data.relation.ItemWithDetails

/**
 * 将ItemEntity转换为Item领域模型
 * 保持所有字段的映射关系一致
 */
fun ItemEntity.toItem(): Item {
    return Item(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        location = null, // 注意：这里不能直接访问location，因为它是通过关系查询获得的
        category = category,
        addDate = addDate,
        productionDate = productionDate,
        expirationDate = expirationDate,
        openStatus = openStatus,
        openDate = openDate,
        brand = brand,
        specification = specification,
        status = status,
        stockWarningThreshold = stockWarningThreshold,
        price = price,
        priceUnit = priceUnit,
        purchaseChannel = purchaseChannel,
        storeName = storeName,
        subCategory = subCategory,
        customNote = customNote,
        season = season,
        capacity = capacity,
        capacityUnit = capacityUnit,
        rating = rating,
        totalPrice = totalPrice,
        totalPriceUnit = totalPriceUnit,
        purchaseDate = purchaseDate,
        shelfLife = shelfLife,
        warrantyPeriod = warrantyPeriod,
        warrantyEndDate = warrantyEndDate,
        serialNumber = serialNumber,
        photos = emptyList(), // 注意：这里不能直接访问photos，因为它是通过关系查询获得的
        tags = emptyList() // 注意：这里不能直接访问tags，因为它是通过关系查询获得的
    )
}

/**
 * 将Item领域模型转换为ItemEntity数据库实体
 * 保持所有字段的映射关系一致
 */
fun Item.toItemEntity(locationId: Long? = null): ItemEntity {
    return ItemEntity(
        id = id,
        name = name,
        quantity = quantity,
        unit = unit,
        locationId = locationId, // 需要外部提供locationId
        category = category,
        addDate = addDate,
        productionDate = productionDate,
        expirationDate = expirationDate,
        openStatus = openStatus,
        openDate = openDate,
        brand = brand,
        specification = specification,
        status = status,
        stockWarningThreshold = stockWarningThreshold,
        price = price,
        priceUnit = priceUnit,
        purchaseChannel = purchaseChannel,
        storeName = storeName,
        subCategory = subCategory,
        customNote = customNote,
        season = season,
        capacity = capacity,
        capacityUnit = capacityUnit,
        rating = rating,
        totalPrice = totalPrice,
        totalPriceUnit = totalPriceUnit,
        purchaseDate = purchaseDate,
        shelfLife = shelfLife,
        warrantyPeriod = warrantyPeriod,
        warrantyEndDate = warrantyEndDate,
        serialNumber = serialNumber
    )
}

/**
 * 将ItemWithDetails转换为Item领域模型
 */
fun ItemWithDetails.toItem(): Item {
    return Item(
        id = item.id,
        name = item.name,
        quantity = item.quantity,
        unit = item.unit,
        location = location?.toLocation(),
        category = item.category,
        addDate = item.addDate,
        productionDate = item.productionDate,
        expirationDate = item.expirationDate,
        openStatus = item.openStatus,
        openDate = item.openDate,
        brand = item.brand,
        specification = item.specification,
        status = item.status,
        stockWarningThreshold = item.stockWarningThreshold,
        price = item.price,
        priceUnit = item.priceUnit,
        purchaseChannel = item.purchaseChannel,
        storeName = item.storeName,
        subCategory = item.subCategory,
        customNote = item.customNote,
        season = item.season,
        capacity = item.capacity,
        capacityUnit = item.capacityUnit,
        rating = item.rating,
        totalPrice = item.totalPrice,
        totalPriceUnit = item.totalPriceUnit,
        purchaseDate = item.purchaseDate,
        shelfLife = item.shelfLife,
        warrantyPeriod = item.warrantyPeriod,
        warrantyEndDate = item.warrantyEndDate,
        serialNumber = item.serialNumber,
        photos = photos.map { it.toPhoto() },
        tags = tags.map { it.toTag() }
    )
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