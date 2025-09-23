package com.example.itemmanagement.data.entity.wishlist

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 心愿单物品实体
 * 独立于库存物品的心愿单专用数据模型
 */
@Entity(tableName = "wishlist_items")
data class WishlistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // === 基础信息 ===
    val name: String,                           // 物品名称 *必需
    val category: String,                       // 分类 *必需
    val subCategory: String? = null,            // 子分类
    val brand: String? = null,                  // 品牌
    val specification: String? = null,          // 规格描述
    val customNote: String? = null,             // 备注说明
    
    // === 价格相关 ===
    val price: Double? = null,                  // 单价
    val targetPrice: Double? = null,            // 目标价格（期望的购买价格）
    val priceUnit: String = "元",               // 价格单位
    val currentPrice: Double? = null,           // 当前市场价格
    val lowestPrice: Double? = null,            // 历史最低价格
    val highestPrice: Double? = null,           // 历史最高价格
    
    // === 购买计划 ===
    val priority: WishlistPriority = WishlistPriority.NORMAL,  // 优先级
    val urgency: WishlistUrgency = WishlistUrgency.NORMAL,     // 紧急程度
    val quantity: Double = 1.0,                 // 购买数量
    val quantityUnit: String = "个",            // 数量单位
    val budgetLimit: Double? = null,            // 个人预算上限
    val purchaseChannel: String? = null,        // 首选购买渠道
    val preferredBrand: String? = null,         // 首选品牌（可能与brand不同）
    
    // === 跟踪设置 ===
    val isPriceTrackingEnabled: Boolean = true, // 是否启用价格跟踪
    val priceDropThreshold: Double? = null,     // 降价提醒阈值(百分比，如5.0表示5%)
    val lastPriceCheck: Date? = null,           // 最后一次价格检查时间
    val priceCheckInterval: Int = 7,            // 价格检查间隔（天数）
    
    // === 状态管理 ===
    val isActive: Boolean = true,               // 是否激活（软删除用）
    val isPaused: Boolean = false,              // 是否暂停跟踪
    val addDate: Date = Date(),                 // 创建时间
    val lastModified: Date = Date(),            // 最后修改时间
    val achievedDate: Date? = null,             // 实现愿望的日期（购买时设置）
    
    // === 关联信息 ===
    val sourceUrl: String? = null,              // 商品链接
    val imageUrl: String? = null,               // 商品图片链接
    val relatedItemId: Long? = null,            // 关联的库存物品ID（购买后）
    val addedReason: String? = null,            // 添加原因（手动添加/推荐/缺货补充等）
    
    // === 统计信息 ===
    val viewCount: Int = 0,                     // 查看次数
    val lastViewDate: Date? = null,             // 最后查看时间
    val priceChangeCount: Int = 0               // 价格变动次数
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
     * 获取显示用的完整名称
     */
    fun getDisplayName(): String {
        return if (brand != null) {
            "$brand $name"
        } else {
            name
        }
    }
}
