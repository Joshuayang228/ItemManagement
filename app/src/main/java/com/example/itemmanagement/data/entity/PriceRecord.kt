package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 价格记录实体
 * 用于记录购物清单物品的价格变化历史
 */
@Entity(
    tableName = "price_records",
    indices = [
        Index(value = ["itemId"]),
        Index(value = ["recordDate"])
    ]
)
data class PriceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val itemId: Long,              // 关联的物品ID
    val recordDate: Date,          // 记录日期
    val price: Double,             // 价格
    val purchaseChannel: String,   // 购买渠道（京东、天猫、拼多多等）
    val notes: String? = null      // 备注（可选）
)

/**
 * 购买渠道常量
 */
object PurchaseChannel {
    const val JD = "京东"
    const val TMALL = "天猫"
    const val PDD = "拼多多"
    const val STORE = "实体店"
    const val OTHER = "其他"
    
    fun getAll() = listOf(JD, TMALL, PDD, STORE, OTHER)
}

