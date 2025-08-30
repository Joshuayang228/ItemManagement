package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 回收站中的已删除物品实体
 * 保存被删除物品的完整信息，支持30天内恢复
 */
@Entity(tableName = "deleted_items")
data class DeletedItemEntity(
    @PrimaryKey
    val originalId: Long,  // 原物品的ID
    
    // ==================== 原物品信息 ====================
    val name: String,
    val brand: String? = null,
    val specification: String? = null,
    val category: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val price: Double? = null,
    val purchaseDate: Date? = null,
    val expirationDate: Date? = null,
    val warrantyEndDate: Date? = null,
    val customNote: String? = null,
    val addDate: Date = Date(),
    
    // ==================== 位置信息 ====================
    val locationId: Long? = null,
    val locationArea: String? = null,     // 冗余存储位置信息
    val locationContainer: String? = null,
    val locationSublocation: String? = null,
    
    // ==================== 照片信息 ====================
    val photoUris: String? = null,  // JSON格式存储照片URI列表
    
    // ==================== 标签信息 ====================
    val tagNames: String? = null,   // JSON格式存储标签名称列表
    
    // ==================== 回收站特有信息 ====================
    val deletedDate: Date = Date(),        // 删除时间
    val deletedReason: String? = null,     // 删除原因（可选）
    val canRestore: Boolean = true,        // 是否可以恢复
    
    // ==================== 关联数据 ====================
    val hasWarranty: Boolean = false,      // 是否有保修信息
    val hasBorrowRecord: Boolean = false   // 是否有借还记录
) {
    /**
     * 计算距离自动清理的剩余天数
     */
    fun getDaysUntilAutoClean(): Int {
        val now = System.currentTimeMillis()
        val deleteTime = deletedDate.time
        val passedDays = (now - deleteTime) / (1000 * 60 * 60 * 24)
        return (30 - passedDays).toInt().coerceAtLeast(0)
    }
    
    /**
     * 是否即将被自动清理（剩余3天内）
     */
    fun isNearAutoClean(): Boolean = getDaysUntilAutoClean() <= 3
    
    /**
     * 是否已超过保留期限
     */
    fun isExpiredForClean(): Boolean = getDaysUntilAutoClean() <= 0
    
    /**
     * 获取完整位置字符串
     */
    fun getFullLocationString(): String? {
        return if (!locationArea.isNullOrBlank()) {
            buildString {
                append(locationArea)
                if (!locationContainer.isNullOrBlank()) {
                    append(" > ").append(locationContainer)
                    if (!locationSublocation.isNullOrBlank()) {
                        append(" > ").append(locationSublocation)
                    }
                }
            }
        } else null
    }
}
