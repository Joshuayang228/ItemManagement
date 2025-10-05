package com.example.itemmanagement.data.entity.wishlist

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 心愿单价格历史实体
 * 用于跟踪和记录心愿单物品的价格变化历史
 */
@Entity(
    tableName = "wishlist_price_history",
    foreignKeys = [
        ForeignKey(
            entity = com.example.itemmanagement.data.entity.unified.WishlistDetailEntity::class,
            parentColumns = ["itemId"],
            childColumns = ["wishlistItemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("wishlistItemId"),
        Index("recordDate")
    ]
)
data class WishlistPriceHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val wishlistItemId: Long,                   // 外键：对应的心愿单物品ID
    val price: Double,                          // 记录的价格
    val priceUnit: String = "元",               // 价格单位
    val currency: String = "CNY",               // 货币代码
    
    // === 价格来源信息 ===
    val source: String,                         // 价格来源（manual/auto/api/web等）
    val sourceUrl: String? = null,              // 价格来源链接
    val storeName: String? = null,              // 商店名称
    val platform: String? = null,              // 平台名称（淘宝、京东等）
    
    // === 记录信息 ===
    val recordDate: Date = Date(),              // 价格记录时间
    val isManual: Boolean = false,              // 是否手动录入
    val isPromotional: Boolean = false,         // 是否促销价格
    val promotionalInfo: String? = null,        // 促销信息（满减、折扣等）
    
    // === 价格变动信息 ===
    val previousPrice: Double? = null,          // 上一次记录的价格
    val priceChange: Double? = null,            // 价格变动金额（正数为涨价，负数为降价）
    val changePercentage: Double? = null,       // 价格变动百分比
    
    // === 质量和可信度 ===
    val confidence: Double = 1.0,               // 价格可信度（0.0-1.0）
    val verificationStatus: PriceVerificationStatus = PriceVerificationStatus.UNVERIFIED,
    val notes: String? = null                   // 备注（如：限时特价、会员价等）
) {
    /**
     * 检查是否为降价
     */
    fun isPriceDown(): Boolean {
        return priceChange != null && priceChange!! < 0
    }
    
    /**
     * 检查是否为涨价
     */
    fun isPriceUp(): Boolean {
        return priceChange != null && priceChange!! > 0
    }
    
    /**
     * 获取价格变动描述
     */
    fun getPriceChangeDescription(): String? {
        return when {
            priceChange == null -> null
            isPriceDown() -> "降价 ${Math.abs(priceChange!!)} $priceUnit"
            isPriceUp() -> "涨价 ${priceChange!!} $priceUnit"
            else -> "价格不变"
        }
    }
    
    /**
     * 获取变动百分比文本
     */
    fun getChangePercentageText(): String? {
        return changePercentage?.let {
            val sign = if (it >= 0) "+" else ""
            "$sign${String.format("%.1f", it)}%"
        }
    }
}

/**
 * 价格验证状态枚举
 */
enum class PriceVerificationStatus(val displayName: String) {
    UNVERIFIED("未验证"),
    VERIFIED("已验证"),
    SUSPICIOUS("可疑"),
    INVALID("无效");
    
    companion object {
        fun fromString(status: String): PriceVerificationStatus {
            return values().firstOrNull { it.name == status } ?: UNVERIFIED
        }
    }
}
