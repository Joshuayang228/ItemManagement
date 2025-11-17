package com.example.itemmanagement.data.entity.unified

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.itemmanagement.data.entity.LocationEntity
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.OpenStatus
import java.util.Date

/**
 * 库存详情实体
 * 存储物品在库存状态下的专用字段
 * 与unified_items通过itemId关联
 */
@Entity(
    tableName = "inventory_details",
    foreignKeys = [
        ForeignKey(
            entity = UnifiedItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["itemId"], unique = false),  // 不设置unique，因为同一物品可能多次入库
        Index("locationId"),
        Index("status"),
        Index("expirationDate"),
        Index("stockWarningThreshold"),
        Index(value = ["status", "expirationDate"])  // 组合索引，优化过期查询
    ]
)
data class InventoryDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                            // 自增主键（支持同一物品多次入库历史）
    
    /** 关联的物品ID */
    val itemId: Long,                            // 外键 -> unified_items.id
    
    // === 数量和位置 ===
    val quantity: Double,                        // 库存数量
    val unit: String,                           // 数量单位
    val isQuantityUserInput: Boolean = false,   // 数量是否由用户输入（用于区分默认值）
    val locationId: Long? = null,               // 存储位置ID
    
    // === 时间相关 ===
    val productionDate: Date? = null,           // 生产日期
    val expirationDate: Date? = null,           // 过期日期
    val openDate: Date? = null,                 // 开封日期
    val purchaseDate: Date? = null,             // 购买日期
    val wasteDate: Date? = null,                // 物品变成浪费状态的时间
    
    // === 开封状态 ===
    val openStatus: OpenStatus? = null,         // 开封状态
    
    // === 库存管理 ===
    val status: ItemStatus = ItemStatus.IN_STOCK, // 库存状态
    val stockWarningThreshold: Int? = null,      // 库存警告阈值
    val isHighTurnover: Boolean = false,         // 是否高周转
    
    // === 价格信息 ===
    val price: Double? = null,                  // 单价
    val priceUnit: String? = null,              // 价格单位
    val totalPrice: Double? = null,             // 总价
    val totalPriceUnit: String? = null,         // 总价单位
    
    // === 购买信息 ===
    val purchaseChannel: String? = null,        // 购买渠道
    val storeName: String? = null,              // 商店名称
    
    // === 物品属性（已废弃，迁移到UnifiedItemEntity）===
    // val capacity: Double? = null,               // 容量 -> 已移至UnifiedItemEntity
    // val capacityUnit: String? = null,           // 容量单位 -> 已移至UnifiedItemEntity
    // val rating: Double? = null,                 // 评分 -> 已移至UnifiedItemEntity
    // val season: String? = null,                 // 季节 -> 已移至UnifiedItemEntity
    // val serialNumber: String? = null,           // 序列号 -> 已移至UnifiedItemEntity
    
    // === 保修信息（已废弃，迁移到warranties表）===
    val shelfLife: Int? = null,                 // 保质期（天数）
    // warrantyPeriod 和 warrantyEndDate 已迁移至 WarrantyEntity
    
    // === 时间戳 ===
    val createdDate: Date = Date(),             // 创建时间
    val updatedDate: Date = Date()              // 更新时间
) {
    /**
     * 检查是否即将过期（7天内）
     */
    fun isNearExpiration(advanceDays: Int = 7): Boolean {
        if (expirationDate == null) return false
        
        val now = System.currentTimeMillis()
        val expTime = expirationDate.time
        val daysUntilExp = (expTime - now) / (1000 * 60 * 60 * 24)
        
        return daysUntilExp <= advanceDays && daysUntilExp >= 0
    }
    
    /**
     * 检查是否已过期
     */
    fun isExpired(): Boolean {
        return expirationDate != null && Date().after(expirationDate)
    }
    
    /**
     * 检查库存是否不足
     */
    fun isLowStock(): Boolean {
        return stockWarningThreshold != null && quantity <= stockWarningThreshold
    }
    
    /**
     * 获取剩余保质期天数
     */
    fun getRemainingShelfLifeDays(): Int? {
        return expirationDate?.let { expDate ->
            val now = System.currentTimeMillis()
            val expTime = expDate.time
            ((expTime - now) / (1000 * 60 * 60 * 24)).toInt()
        }
    }
    
    /**
     * 获取库存状态描述
     */
    fun getStockStatusDescription(): String {
        return when {
            isExpired() -> "已过期"
            isNearExpiration() -> "即将过期"
            isLowStock() -> "库存不足"
            status == ItemStatus.USED_UP -> "已用完"
            status == ItemStatus.IN_STOCK -> "正常"
            else -> status.name
        }
    }
    
    /**
     * 计算物品价值（数量×单价）
     */
    fun calculateCurrentValue(): Double? {
        return price?.let { it * quantity }
    }
    
    /**
     * 检查是否需要补货
     */
    fun needsRestocking(): Boolean {
        return isLowStock() || status == ItemStatus.USED_UP
    }
    
    /**
     * 获取开封后剩余使用时间（如果有保质期和开封日期）
     */
    fun getRemainingUsageTime(): Int? {
        return if (openDate != null && shelfLife != null) {
            val openTime = openDate.time
            val now = System.currentTimeMillis()
            val usedDays = ((now - openTime) / (1000 * 60 * 60 * 24)).toInt()
            (shelfLife - usedDays).coerceAtLeast(0)
        } else null
    }
    
    // isWarrantyNearExpiration 方法已移除，保修检查请使用 WarrantyEntity
}
