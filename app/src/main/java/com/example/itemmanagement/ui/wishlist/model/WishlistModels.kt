package com.example.itemmanagement.ui.wishlist.model

import com.example.itemmanagement.data.entity.wishlist.WishlistPriority
import com.example.itemmanagement.data.view.WishlistItemView

/**
 * 心愿单显示模式
 */
enum class WishlistDisplayMode {
    LIST,    // 列表模式
    GRID,    // 网格模式  
    COMPACT  // 紧凑模式
}

/**
 * 心愿单UI状态
 */
sealed class WishlistUiState {
    object Loading : WishlistUiState()
    object Success : WishlistUiState()
    data class Error(val message: String) : WishlistUiState()
}

/**
 * 心愿单导航事件
 */
sealed class WishlistNavigationEvent {
    object AddItem : WishlistNavigationEvent()
    data class ItemDetail(val itemId: Long) : WishlistNavigationEvent()
    object Filter : WishlistNavigationEvent()
    object Stats : WishlistNavigationEvent()
    object PriceTracking : WishlistNavigationEvent()
    object Settings : WishlistNavigationEvent()
}

/**
 * 心愿单筛选状态
 */
data class WishlistFilterState(
    val showAll: Boolean = true,
    val showHighPriority: Boolean = false,
    val showPriceAlerts: Boolean = false
)

/**
 * 选择操作结果
 */
sealed class SelectionOperationResult {
    data class Success(val message: String) : SelectionOperationResult()
    data class Error(val message: String) : SelectionOperationResult()
}

/**
 * 价格状态
 */
enum class PriceStatus {
    NORMAL,      // 正常价格
    DROPPED,     // 价格下降
    INCREASED,   // 价格上涨
    TARGET_MET   // 达到目标价格
}

/**
 * 心愿单物品UI状态
 */
data class WishlistItemUiState(
    val itemId: Long,
    val name: String,
    val currentPrice: Double?,
    val targetPrice: Double?,
    val priority: WishlistPriority,
    val priceStatus: PriceStatus,
    val isSelected: Boolean = false
)

