package com.example.itemmanagement.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity
import java.util.Date

@Parcelize
data class Item(
    val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val location: Location?,
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
    val priceUnit: String? = "元",
    val purchaseChannel: String?,
    val storeName: String?,
    val subCategory: String?,
    val customNote: String?,
    val season: String?,                // 季节
    val capacity: Double?,              // 容量
    val capacityUnit: String?,          // 容量单位
    val rating: Double?,                // 评分
    val totalPrice: Double?,            // 总价
    val totalPriceUnit: String? = "元", // 总价单位
    val purchaseDate: Date?,            // 购买日期
    val shelfLife: Int?,                // 保质期（天数）
    val warrantyPeriod: Int?,           // 保修期（天数）
    val warrantyEndDate: Date?,         // 保修到期时间
    val serialNumber: String?,          // 序列号
    val isHighTurnover: Boolean = false, // 是否高周转
    val photos: List<Photo> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val shoppingDetail: @RawValue ShoppingDetailEntity? = null // 购物详情（仅购物清单物品有）
) : Parcelable

@Parcelize
data class Location(
    val id: Long = 0,
    val area: String,
    val container: String?,
    val sublocation: String?
) : Parcelable {
    fun getFullLocationString(): String {
        return buildString {
            append(area)
            if (!container.isNullOrBlank()) {
                append(" > ").append(container)
                if (!sublocation.isNullOrBlank()) {
                    append(" > ").append(sublocation)
                }
            }
        }
    }
}

@Parcelize
data class Photo(
    val id: Long = 0,
    val uri: String,
    val isMain: Boolean = false,
    val displayOrder: Int = 0
) : Parcelable

@Parcelize
data class Tag(
    val id: Long = 0,
    val name: String,
    val color: String? = null
) : Parcelable
