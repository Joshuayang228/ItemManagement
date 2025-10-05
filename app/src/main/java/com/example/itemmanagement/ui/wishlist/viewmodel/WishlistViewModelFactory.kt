package com.example.itemmanagement.ui.wishlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.WishlistRepository
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel
import com.example.itemmanagement.ui.wishlist.WishlistViewModel
import kotlinx.coroutines.launch

/**
 * 心愿单ViewModel工厂类
 * 遵循MVVM架构，负责创建各种心愿单相关的ViewModel实例
 * 支持依赖注入模式，兼容基于BaseItemViewModel的新架构
 */
class WishlistViewModelFactory(
    private val wishlistRepository: WishlistRepository,
    private val itemRepository: UnifiedItemRepository? = null,  // 用于基于BaseItemViewModel的子类
    private val cacheViewModel: ItemStateCacheViewModel? = null,  // 用于基于BaseItemViewModel的子类
    private val itemId: Long? = null  // 用于编辑和详情模式
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(WishlistMainViewModel::class.java) -> {
                require(itemRepository != null) {
                    "WishlistMainViewModel requires itemRepository"
                }
                WishlistMainViewModel(itemRepository) as T
            }
            
            modelClass.isAssignableFrom(WishlistSelectionViewModel::class.java) -> {
                WishlistSelectionViewModel() as T
            }
            
            modelClass.isAssignableFrom(WishlistAddViewModel::class.java) -> {
                require(itemRepository != null && cacheViewModel != null) {
                    "WishlistAddViewModel requires itemRepository and cacheViewModel"
                }
                WishlistAddViewModel(itemRepository, cacheViewModel, wishlistRepository) as T
            }
            
            modelClass.isAssignableFrom(WishlistEditViewModel::class.java) -> {
                require(itemRepository != null && cacheViewModel != null && itemId != null) {
                    "WishlistEditViewModel requires itemRepository, cacheViewModel and itemId"
                }
                WishlistEditViewModel(itemRepository, cacheViewModel, wishlistRepository, itemId) as T
            }
            
            modelClass.isAssignableFrom(WishlistViewModel::class.java) -> {
                WishlistViewModel(wishlistRepository) as T
            }
            
            modelClass.isAssignableFrom(WishlistItemDetailViewModel::class.java) -> {
                require(itemId != null) { "WishlistItemDetailViewModel requires itemId" }
                WishlistItemDetailViewModel(wishlistRepository, itemId) as T
            }
            
            modelClass.isAssignableFrom(WishlistStatsViewModel::class.java) -> {
                WishlistStatsViewModel(wishlistRepository) as T
            }
            
            modelClass.isAssignableFrom(WishlistPriceTrackingViewModel::class.java) -> {
                WishlistPriceTrackingViewModel(wishlistRepository) as T
            }
            
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    
    companion object {
        /**
         * 创建用于添加心愿单物品的工厂
         */
        fun forAdd(
            wishlistRepository: WishlistRepository,
            itemRepository: UnifiedItemRepository,
            cacheViewModel: ItemStateCacheViewModel
        ): WishlistViewModelFactory {
            return WishlistViewModelFactory(
                wishlistRepository = wishlistRepository,
                itemRepository = itemRepository,
                cacheViewModel = cacheViewModel
            )
        }
        
        /**
         * 创建用于编辑心愿单物品的工厂
         */
        fun forEdit(
            wishlistRepository: WishlistRepository,
            itemRepository: UnifiedItemRepository,
            cacheViewModel: ItemStateCacheViewModel,
            itemId: Long
        ): WishlistViewModelFactory {
            return WishlistViewModelFactory(
                wishlistRepository = wishlistRepository,
                itemRepository = itemRepository,
                cacheViewModel = cacheViewModel,
                itemId = itemId
            )
        }
        
        /**
         * 创建用于心愿单详情的工厂
         */
        fun forDetail(
            wishlistRepository: WishlistRepository,
            itemId: Long
        ): WishlistViewModelFactory {
            return WishlistViewModelFactory(
                wishlistRepository = wishlistRepository,
                itemId = itemId
            )
        }
        
        /**
         * 创建用于心愿单主界面的工厂
         */
        fun forMain(
            wishlistRepository: WishlistRepository
        ): WishlistViewModelFactory {
            return WishlistViewModelFactory(
                wishlistRepository = wishlistRepository
            )
        }
    }
}

/**
 * 心愿单物品详情ViewModel
 * 负责管理心愿单物品详情页面的数据和业务逻辑
 */
class WishlistItemDetailViewModel(
    private val wishlistRepository: WishlistRepository,
    private val itemId: Long
) : ViewModel() {
    
    private val _wishlistItem = MutableLiveData<com.example.itemmanagement.data.view.WishlistItemView>()
    val wishlistItem: LiveData<com.example.itemmanagement.data.view.WishlistItemView> = _wishlistItem

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _operationSuccess = MutableLiveData<String>()
    val operationSuccess: LiveData<String> = _operationSuccess

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    init {
        loadWishlistItem()
    }

    /**
     * 加载心愿单物品详情
     */
    fun loadWishlistItem() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val item = wishlistRepository.getWishlistItemById(itemId)
                if (item != null) {
                    _wishlistItem.value = item
                } else {
                    _errorMessage.value = "心愿单物品不存在"
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载心愿单物品失败: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新价格
     */
    fun updatePrice(newPrice: Double, source: String = "manual") {
        viewModelScope.launch {
            try {
                wishlistRepository.updatePrice(itemId, newPrice)
                _operationSuccess.value = "价格已更新"
                // 重新加载物品数据以获取最新价格
                loadWishlistItem()
            } catch (e: Exception) {
                _errorMessage.value = "更新价格失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 标记为已购买
     */
    fun markAsPurchased() {
        viewModelScope.launch {
            try {
                wishlistRepository.markAsAchieved(itemId)
                _operationSuccess.value = "已标记为已购买"
                _navigateBack.value = true
            } catch (e: Exception) {
                _errorMessage.value = "标记已购买失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 删除心愿单物品
     */
    fun deleteWishlistItem() {
        viewModelScope.launch {
            try {
                wishlistRepository.deleteWishlistItem(itemId)
                _operationSuccess.value = "已从心愿单中删除"
                _navigateBack.value = true
            } catch (e: Exception) {
                _errorMessage.value = "删除失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 清除导航事件
     */
    fun onNavigationComplete() {
        _navigateBack.value = false
    }
}

/**
 * 心愿单统计ViewModel（占位符实现）
 */
class WishlistStatsViewModel(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {
    // TODO: 实现统计页面的ViewModel逻辑
}

/**
 * 心愿单价格跟踪ViewModel（占位符实现）
 */
class WishlistPriceTrackingViewModel(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {
    // TODO: 实现价格跟踪页面的ViewModel逻辑
}

