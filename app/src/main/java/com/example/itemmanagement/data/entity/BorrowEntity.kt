package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 借还记录实体
 * 记录物品的借出和归还信息
 */
@Entity(
    tableName = "borrows",
    foreignKeys = [
        ForeignKey(
            entity = com.example.itemmanagement.data.entity.unified.UnifiedItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("status"), Index("borrowDate")]
)
data class BorrowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 关联的物品ID */
    val itemId: Long,
    
    /** 借用人姓名 */
    val borrowerName: String,
    
    /** 借用人联系方式（可选） */
    val borrowerContact: String? = null,
    
    /** 借出时间戳 */
    val borrowDate: Long,
    
    /** 预计归还时间戳 */
    val expectedReturnDate: Long,
    
    /** 实际归还时间戳（未归还时为null） */
    val actualReturnDate: Long? = null,
    
    /** 借还状态 */
    val status: BorrowStatus,
    
    /** 备注信息 */
    val notes: String? = null,
    
    /** 创建时间戳 */
    val createdDate: Long,
    
    /** 更新时间戳 */
    val updatedDate: Long
)
