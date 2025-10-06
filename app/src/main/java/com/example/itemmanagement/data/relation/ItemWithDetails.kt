package com.example.itemmanagement.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Junction
import com.example.itemmanagement.data.entity.*
import com.example.itemmanagement.data.entity.unified.*

/**
 * 统一架构的物品详情关系
 * 基于UnifiedItemEntity的完整物品数据
 */
data class UnifiedItemWithDetails(
    @Embedded val unifiedItem: UnifiedItemEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val itemStates: List<ItemStateEntity>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val shoppingDetails: List<ShoppingDetailEntity>,
    
    @Relation(
        parentColumn = "id", 
        entityColumn = "itemId"
    )
    val inventoryDetail: InventoryDetailEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val photos: List<PhotoEntity>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)

/**
 * 保留旧的ItemWithDetails作为兼容性接口
 * 内部使用UnifiedItemWithDetails实现
 * @deprecated 使用UnifiedItemWithDetails替代
 */
@Deprecated("使用UnifiedItemWithDetails替代")
data class ItemWithDetails(
    @Embedded val unifiedItem: UnifiedItemEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val inventoryDetail: InventoryDetailEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "itemId"
    )
    val photos: List<PhotoEntity>,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ItemTagCrossRef::class,
            parentColumn = "itemId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
) {
    // 为向后兼容提供item属性
    val item: UnifiedItemEntity get() = unifiedItem
    
    // 为向后兼容提供location属性
    val location: LocationEntity? get() = locationEntity // 从Repository查询得到的位置信息
    
    // 临时存储位置信息的属性（由Repository设置）
    var locationEntity: LocationEntity? = null
} 