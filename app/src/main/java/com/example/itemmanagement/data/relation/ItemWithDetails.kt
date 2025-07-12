package com.example.itemmanagement.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Junction
import com.example.itemmanagement.data.entity.*

data class ItemWithDetails(
    @Embedded val item: ItemEntity,
    
    @Relation(
        parentColumn = "locationId",
        entityColumn = "id"
    )
    val location: LocationEntity?,
    
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