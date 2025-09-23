package com.example.itemmanagement.ui.wishlist.model

import com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity
import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.entity.wishlist.WishlistUrgency

/**
 * 心愿单相关的UI模型类
 * 包含筛选、排序、导航等状态模型
 */

/**
 * 心愿单筛选条件数据类
 */
data class WishlistFilter(
    val categories: Set<String> = emptySet(),
    val priorities: Set<WishlistPriority> = emptySet(),
    val urgencies: Set<WishlistUrgency> = emptySet(),
    val priceRange: Pair<Double, Double>? = null,
    val onlyWithPriceTracking: Boolean = false,
    val onlyPriceDropped: Boolean = false,
    val onlyReachedTarget: Boolean = false,
    val onlyHighPriority: Boolean = false,
    val brands: Set<String> = emptySet(),
    val hasBudgetLimit: Boolean = false,
    val onlyPurchased: Boolean = false,
    val onlyUnpurchased: Boolean = false
) {
    
    /**
     * 检查物品是否匹配筛选条件
     */
    fun matches(item: WishlistItemEntity): Boolean {
        // 分类筛选
        if (categories.isNotEmpty() && !categories.contains(item.category)) {
            return false
        }
        
        // 优先级筛选
        if (priorities.isNotEmpty() && !priorities.contains(item.priority)) {
            return false
        }
        
        // 紧急程度筛选
        if (urgencies.isNotEmpty() && !urgencies.contains(item.urgency)) {
            return false
        }
        
        // 价格范围筛选
        if (priceRange != null && item.currentPrice != null) {
            if (item.currentPrice < priceRange.first || item.currentPrice > priceRange.second) {
                return false
            }
        }
        
        // 品牌筛选
        if (brands.isNotEmpty() && (item.brand == null || !brands.contains(item.brand))) {
            return false
        }
        
        // 价格跟踪筛选
        if (onlyWithPriceTracking && !item.isPriceTrackingEnabled) {
            return false
        }
        
        // 降价筛选
        if (onlyPriceDropped && !item.hasPriceDrop()) {
            return false
        }
        
        // 达到目标价格筛选
        if (onlyReachedTarget && !item.hasReachedTargetPrice()) {
            return false
        }
        
        // 高优先级筛选
        if (onlyHighPriority && item.priority.level < WishlistPriority.HIGH.level) {
            return false
        }
        
        // 预算限制筛选
        if (hasBudgetLimit && item.budgetLimit == null) {
            return false
        }
        
        // 购买状态筛选
        if (onlyPurchased && item.achievedDate == null) {
            return false
        }
        
        if (onlyUnpurchased && item.achievedDate != null) {
            return false
        }
        
        return true
    }
    
    /**
     * 检查是否有任何筛选条件被应用
     */
    fun hasActiveFilters(): Boolean {
        return categories.isNotEmpty() ||
                priorities.isNotEmpty() ||
                urgencies.isNotEmpty() ||
                priceRange != null ||
                onlyWithPriceTracking ||
                onlyPriceDropped ||
                onlyReachedTarget ||
                onlyHighPriority ||
                brands.isNotEmpty() ||
                hasBudgetLimit ||
                onlyPurchased ||
                onlyUnpurchased
    }
    
    /**
     * 获取活跃筛选条件的数量
     */
    fun getActiveFilterCount(): Int {
        var count = 0
        if (categories.isNotEmpty()) count++
        if (priorities.isNotEmpty()) count++
        if (urgencies.isNotEmpty()) count++
        if (priceRange != null) count++
        if (onlyWithPriceTracking) count++
        if (onlyPriceDropped) count++
        if (onlyReachedTarget) count++
        if (onlyHighPriority) count++
        if (brands.isNotEmpty()) count++
        if (hasBudgetLimit) count++
        if (onlyPurchased) count++
        if (onlyUnpurchased) count++
        return count
    }
}

/**
 * 心愿单排序方式枚举
 */
enum class WishlistSortOrder(val displayName: String, val description: String) {
    PRIORITY_DESC("优先级降序", "高优先级在前"),
    PRIORITY_ASC("优先级升序", "低优先级在前"),
    PRICE_DESC("价格降序", "高价格在前"),
    PRICE_ASC("价格升序", "低价格在前"),
    DATE_DESC("最新添加", "最近添加在前"),
    DATE_ASC("最早添加", "最早添加在前"),
    NAME_ASC("名称升序", "A-Z排序"),
    NAME_DESC("名称降序", "Z-A排序"),
    URGENCY_DESC("紧急程度降序", "最急需在前");
    
    companion object {
        /**
         * 获取默认排序方式
         */
        fun getDefault() = PRIORITY_DESC
        
        /**
         * 获取所有排序选项
         */
        fun getAllOptions() = values().toList()
    }
}

/**
 * 心愿单导航事件密封类
 */
sealed class WishlistNavigationEvent {
    object AddItem : WishlistNavigationEvent()
    data class ItemDetail(val itemId: Long) : WishlistNavigationEvent()
    data class EditItem(val itemId: Long) : WishlistNavigationEvent()
    object Filter : WishlistNavigationEvent()
    object Stats : WishlistNavigationEvent()
    object PriceTracking : WishlistNavigationEvent()
    object Settings : WishlistNavigationEvent()
    object SelectionMode : WishlistNavigationEvent()
    data class ShareItem(val itemId: Long) : WishlistNavigationEvent()
    data class AddToCart(val itemId: Long) : WishlistNavigationEvent()
}

/**
 * 心愿单显示模式枚举
 */
enum class WishlistDisplayMode(val displayName: String) {
    LIST("列表视图"),
    GRID("网格视图"),
    COMPACT("紧凑视图");
    
    companion object {
        fun getDefault() = LIST
    }
}

/**
 * 心愿单快速筛选枚举
 */
enum class WishlistQuickFilter(val displayName: String, val description: String) {
    ALL("全部", "显示所有心愿单物品"),
    UNPURCHASED("未购买", "显示尚未购买的心愿单物品"),
    PURCHASED("已购买", "显示已购买实现的物品"),
    HIGH_PRIORITY("高优先级", "只显示高优先级和紧急物品"),
    PRICE_DROPPED("降价提醒", "显示价格下降的物品"),
    TARGET_REACHED("达到目标价", "显示达到目标价格的物品"),
    RECENTLY_ADDED("最近添加", "显示最近7天添加的物品"),
    PRICE_TRACKING("价格跟踪", "显示启用价格跟踪的物品"),
    NO_PRICE("无价格信息", "显示没有价格信息的物品");
    
    /**
     * 将快速筛选转换为筛选条件
     */
    fun toFilter(): WishlistFilter {
        return when (this) {
            ALL -> WishlistFilter()
            UNPURCHASED -> WishlistFilter(onlyUnpurchased = true)
            PURCHASED -> WishlistFilter(onlyPurchased = true)
            HIGH_PRIORITY -> WishlistFilter(onlyHighPriority = true)
            PRICE_DROPPED -> WishlistFilter(onlyPriceDropped = true)
            TARGET_REACHED -> WishlistFilter(onlyReachedTarget = true)
            RECENTLY_ADDED -> WishlistFilter() // 需要结合时间筛选
            PRICE_TRACKING -> WishlistFilter(onlyWithPriceTracking = true)
            NO_PRICE -> WishlistFilter() // 需要特殊处理
        }
    }
}

/**
 * 价格状态枚举
 */
enum class PriceStatus(val displayName: String, val colorResource: String) {
    NO_PRICE("无价格", "text_secondary"),
    PRICE_UP("价格上涨", "error"),
    PRICE_DOWN("价格下降", "success"),
    PRICE_STABLE("价格稳定", "text_primary"),
    TARGET_REACHED("达到目标价", "success"),
    UNDER_BUDGET("在预算内", "success"),
    OVER_BUDGET("超出预算", "warning");
    
    companion object {
        /**
         * 根据物品状态获取价格状态
         */
        fun fromItem(item: WishlistItemEntity): PriceStatus {
            return when {
                item.currentPrice == null -> NO_PRICE
                item.hasReachedTargetPrice() -> TARGET_REACHED
                item.hasPriceDrop() -> PRICE_DOWN
                item.budgetLimit != null && item.currentPrice != null -> {
                    if (item.currentPrice <= item.budgetLimit) UNDER_BUDGET else OVER_BUDGET
                }
                else -> PRICE_STABLE
            }
        }
    }
}

/**
 * 心愿单物品UI状态数据类
 */
data class WishlistItemUiState(
    val item: WishlistItemEntity,
    val priceStatus: PriceStatus = PriceStatus.fromItem(item),
    val isSelected: Boolean = false,
    val showPriceHistory: Boolean = false,
    val isExpanded: Boolean = false
) {
    
    /**
     * 获取显示用的价格文本
     */
    fun getPriceDisplayText(): String {
        return when {
            item.currentPrice != null -> "¥${String.format("%.0f", item.currentPrice)}"
            item.price != null -> "预估 ¥${String.format("%.0f", item.price)}"
            else -> "暂无价格"
        }
    }
    
    /**
     * 获取优先级显示颜色
     */
    fun getPriorityColor(): String {
        return item.priority.colorCode
    }
    
    /**
     * 是否显示价格变化指示器
     */
    fun shouldShowPriceIndicator(): Boolean {
        return priceStatus in listOf(
            PriceStatus.PRICE_UP,
            PriceStatus.PRICE_DOWN,
            PriceStatus.TARGET_REACHED
        )
    }
    
    /**
     * 获取价格变化百分比文本
     */
    fun getPriceChangeText(): String? {
        val percentage = item.getPriceChangePercentage()
        return if (percentage != null) {
            val sign = if (percentage >= 0) "+" else ""
            "$sign${String.format("%.1f", percentage)}%"
        } else null
    }
}

/**
 * 心愿单统计卡片数据类
 */
data class WishlistStatsCard(
    val title: String,
    val value: String,
    val subtitle: String? = null,
    val trend: String? = null,
    val trendType: TrendType = TrendType.NEUTRAL,
    val icon: String? = null,
    val colorTheme: String = "primary"
)

/**
 * 趋势类型枚举
 */
enum class TrendType {
    UP, DOWN, NEUTRAL
}

