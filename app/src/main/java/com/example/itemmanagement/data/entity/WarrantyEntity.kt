package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 保修信息实体类
 * 为物品提供完整的保修管理功能
 */
@Entity(
    tableName = "warranties",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("itemId"),
        Index("warrantyEndDate") // 为到期提醒优化查询
    ]
)
data class WarrantyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * 关联的物品ID
     */
    val itemId: Long,
    
    /**
     * 购买日期
     * 作为保修计算的起始日期
     */
    val purchaseDate: Date,
    
    /**
     * 保修期长度（月数）
     * 例如：12表示12个月保修期
     */
    val warrantyPeriodMonths: Int,
    
    /**
     * 保修到期日期
     * 由purchaseDate + warrantyPeriodMonths自动计算
     */
    val warrantyEndDate: Date,
    
    /**
     * 保修凭证图片URI列表
     * 存储为JSON字符串格式，例如：["uri1", "uri2", "uri3"]
     * 可以存储发票、收据、保修卡等图片
     */
    val receiptImageUris: String? = null,
    
    /**
     * 保修备注
     * 可以记录保修条款、注意事项等
     */
    val notes: String? = null,
    
    /**
     * 保修状态
     * ACTIVE: 保修期内
     * EXPIRED: 已过期
     * CLAIMED: 已报修
     * VOID: 保修作废
     */
    val status: WarrantyStatus = WarrantyStatus.ACTIVE,
    
    /**
     * 保修服务提供商
     * 例如：官方售后、第三方维修等
     */
    val warrantyProvider: String? = null,
    
    /**
     * 保修联系方式
     * 售后电话、客服微信等
     */
    val contactInfo: String? = null,
    
    /**
     * 创建日期
     */
    val createdDate: Date = Date(),
    
    /**
     * 最后更新日期
     */
    val updatedDate: Date = Date()
)

/**
 * 保修状态枚举
 */
enum class WarrantyStatus {
    ACTIVE,    // 保修期内
    EXPIRED,   // 已过期
    CLAIMED,   // 已报修
    VOID       // 保修作废
}
