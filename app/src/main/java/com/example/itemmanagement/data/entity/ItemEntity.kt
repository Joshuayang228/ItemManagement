package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.OpenStatus
import java.util.Date

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("locationId"),
        Index("category"),
        Index("status"),
        Index("expirationDate")
    ]
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val locationId: Long?,
    val category: String,
    val addDate: Date = Date(),
    val productionDate: Date?,
    val expirationDate: Date?,
    val openStatus: OpenStatus?,
    val openDate: Date?,
    val brand: String?,
    val specification: String?,
    val status: ItemStatus = ItemStatus.IN_STOCK,
    val stockWarningThreshold: Int?,
    val price: Double?,
    val priceUnit: String?,
    val purchaseChannel: String?,
    val storeName: String?,
    val subCategory: String?,
    val customNote: String?,
    val season: String?,
    val capacity: Double?,
    val capacityUnit: String?,
    val rating: Double?,
    val totalPrice: Double?,
    val totalPriceUnit: String?,
    val purchaseDate: Date?,
    val shelfLife: Int?,
    val warrantyPeriod: Int?,
    val warrantyEndDate: Date?,
    val serialNumber: String?,
    val isHighTurnover: Boolean = false,   // 是否高周转
    val wasteDate: Date? = null            // 物品变成浪费状态的时间
) 