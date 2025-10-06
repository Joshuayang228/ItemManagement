package com.example.itemmanagement.data.view

import androidx.room.Embedded
import com.example.itemmanagement.data.entity.LocationEntity
import com.example.itemmanagement.data.entity.unified.InventoryDetailEntity
import com.example.itemmanagement.data.entity.unified.ItemStateEntity
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
    val deletedReason: String? // 从 ItemStateEntity 获取
)

// 用于物品生命周期追踪
data class ItemLifecycle(
    val unifiedItem: UnifiedItemEntity,
    val states: List<ItemStateEntity>,
    val shoppingHistory: List<ShoppingDetailEntity>, // 可能有多个购物记录
    val inventoryDetails: InventoryDetailEntity?
    // 可以添加其他组件信息，如 photos, tags, warranties, borrows
)

