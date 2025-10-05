package com.example.itemmanagement.data.entity.unified

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency
import java.util.Date

/**
 * 心愿单详情实体
 * 存储物品在心愿单状态下的专用字段
 * 与unified_items通过itemId关联
 */
@Entity(
    tableName = "wishlist_details",
    foreignKeys = [
        ForeignKey(
            entity = UnifiedItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["itemId"], unique = true),  // 添加唯一约束，因为有外键引用
        Index("priority"),
        Index("urgency"),
        Index("targetPrice"),
        Index("isPriceTrackingEnabled")
    ]
)
data class WishlistDetailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,                        // 自增主键（支持同一物品多次加入心愿单历史）
    
    /** 关联的物品ID */
    val itemId: Long,                        // 外键 -> unified_items.id
    
    // === 价格相关 ===
    val price: Double? = null,               // 参考价格
    val targetPrice: Double? = null,         // 目标价格（期望的购买价格）
    val priceUnit: String = "元",            // 价格单位
    val currentPrice: Double? = null,        // 当前市场价格
    val lowestPrice: Double? = null,         // 历史最低价格
    val highestPrice: Double? = null,        // 历史最高价格
    
    // === 购买计划 ===
    val priority: WishlistPriority = WishlistPriority.NORMAL,  // 优先级
    val urgency: WishlistUrgency = WishlistUrgency.NORMAL,     // 紧急程度
    val quantity: Double = 1.0,              // 期望购买数量
    val quantityUnit: String = "个",         // 数量单位
    val budgetLimit: Double? = null,         // 个人预算上限
    val purchaseChannel: String? = null,     // 首选购买渠道
    val preferredBrand: String? = null,      // 首选品牌（可能与基础brand不同）
    
    // === 跟踪设置 ===
    val isPriceTrackingEnabled: Boolean = true, // 是否启用价格跟踪
    val priceDropThreshold: Double? = null,     // 降价提醒阈值(百分比，如5.0表示5%)
    val lastPriceCheck: Date? = null,           // 最后一次价格检查时间
    val priceCheckInterval: Int = 7,            // 价格检查间隔（天数）
    
    // === 状态管理 ===
    val isPaused: Boolean = false,              // 是否暂停跟踪
    val achievedDate: Date? = null,             // 实现愿望的日期（购买时设置）
    
    // === 关联信息 ===
    val sourceUrl: String? = null,              // 商品链接
    val imageUrl: String? = null,               // 商品图片链接
    val relatedInventoryItemId: Long? = null,   // 关联的库存物品ID（保持向后兼容）
    val addedReason: String? = null,            // 添加原因（手动添加/推荐/缺货补充等）
    
    // === 统计信息 ===
    val viewCount: Int = 0,                     // 查看次数
    val lastViewDate: Date? = null,             // 最后查看时间
    val priceChangeCount: Int = 0,              // 价格变动次数
    
    // === 时间字段 ===
    val createdDate: Date = Date(),             // 加入心愿单时间
    val lastModified: Date = Date()             // 最后修改时间
) {
    /**
     * 检查是否需要立即关注
     */
    fun needsImmediateAttention(): Boolean {
        return priority == WishlistPriority.URGENT || 
               WishlistUrgency.isHighUrgency(urgency)
    }
    
    /**
     * 检查是否有价格下降
     */
    fun hasPriceDrop(): Boolean {
        return currentPrice != null && 
               lowestPrice != null && 
               currentPrice!! <= lowestPrice!!
    }
    
    /**
     * 获取价格变化百分比
     */
    fun getPriceChangePercentage(): Double? {
        return if (price != null && currentPrice != null && price != 0.0) {
            ((currentPrice!! - price!!) / price!!) * 100
        } else null
    }
    
    /**
     * 检查是否达到目标价格
     */
    fun hasReachedTargetPrice(): Boolean {
        return currentPrice != null && 
               targetPrice != null && 
               currentPrice!! <= targetPrice!!
    }
    
    /**
     * 获取期望总价
     */
    fun getEstimatedTotalPrice(): Double? {
        return targetPrice?.let { it * quantity }
    }
    
    /**
     * 检查是否适合购买（达到目标价格或低于预算）
     */
    fun isGoodTimeToBuy(): Boolean {
        return hasReachedTargetPrice() || 
               (budgetLimit != null && currentPrice != null && currentPrice!! <= budgetLimit!!)
    }
}
