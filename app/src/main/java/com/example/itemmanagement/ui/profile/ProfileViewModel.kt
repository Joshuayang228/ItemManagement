package com.example.itemmanagement.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

/**
 * "我的"页面ViewModel
 * 管理个人资料、统计信息和各种功能入口
 */
class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {
    
    // ==================== 数据 ====================
    
    /**
     * 用户统计摘要
     */
    private val _userStatsSummary = MutableLiveData<UserStatsSummary>()
    val userStatsSummary: LiveData<UserStatsSummary> = _userStatsSummary
    
    /**
     * 库存概览数据
     */
    private val _inventoryOverview = MutableLiveData<InventoryOverview>()
    val inventoryOverview: LiveData<InventoryOverview> = _inventoryOverview
    
    /**
     * 加载状态
     */
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    /**
     * 操作结果
     */
    private val _operationResult = MutableLiveData<String>()
    val operationResult: LiveData<String> = _operationResult
    
    /**
     * 用户资料Flow
     */
    val userProfile = userProfileRepository.getUserProfileFlow().asLiveData()
    
    // ==================== 初始化 ====================
    
    init {
        loadUserData()
    }
    
    // ==================== 数据加载 ====================
    
    /**
     * 加载用户数据
     */
    fun loadUserData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 记录用户活跃
                userProfileRepository.recordDailyActivity()
                
                // 加载用户统计摘要
                val summary = userProfileRepository.getUserStatsSummary()
                _userStatsSummary.value = summary
                
                // 加载库存概览
                loadInventoryOverview()
                
            } catch (e: Exception) {
                _operationResult.value = "数据加载失败：${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 加载库存概览数据
     */
    private suspend fun loadInventoryOverview() {
        try {
            // 获取所有物品数据
            val allItemsFlow = itemRepository.getAllItemsWithDetails()
            allItemsFlow.collect { allItems ->
                val totalItems = allItems.size
                val totalValue = allItems.sumOf { itemWithDetails -> 
                    (itemWithDetails.item.price ?: 0.0) * (itemWithDetails.item.quantity ?: 1.0) 
                }
                
                // 计算即将过期的物品（30天内）
                val now = System.currentTimeMillis()
                val thirtyDaysLater = now + (30 * 24 * 60 * 60 * 1000)
                val expiringSoon = allItems.count { itemWithDetails ->
                    itemWithDetails.item.expirationDate?.time?.let { expirationTime -> 
                        expirationTime <= thirtyDaysLater 
                    } == true
                }
                
                // 计算低库存物品（数量小于等于预警阈值）
                val lowStock = allItems.count { itemWithDetails ->
                    val threshold = itemWithDetails.item.stockWarningThreshold ?: 1
                    (itemWithDetails.item.quantity ?: 0.0) <= threshold
                }
                
                // 分类统计
                val categoryStats = allItems.groupBy { itemWithDetails -> 
                        itemWithDetails.item.category 
                    }
                    .map { (category, items) ->
                        CategoryStat(category, items.size)
                    }
                    .sortedByDescending { categoryStat -> categoryStat.count }
                    .take(5) // 只显示前5个分类
                
                _inventoryOverview.value = InventoryOverview(
                    totalItems = totalItems,
                    totalValue = totalValue,
                    expiringSoon = expiringSoon,
                    lowStock = lowStock,
                    categoryStats = categoryStats
                )
                
                // 更新用户资料中的当前物品数量
                userProfileRepository.updateCurrentItemCount(totalItems)
            }
            
        } catch (e: Exception) {
            _operationResult.value = "库存数据加载失败"
        }
    }
    
    // ==================== 个人信息操作 ====================
    
    /**
     * 更新用户昵称
     */
    fun updateNickname(newNickname: String) {
        viewModelScope.launch {
            try {
                userProfileRepository.updateNickname(newNickname)
                _operationResult.value = "昵称更新成功"
                loadUserData() // 刷新数据
            } catch (e: Exception) {
                _operationResult.value = "昵称更新失败：${e.localizedMessage}"
            }
        }
    }
    
    /**
     * 更新用户头像
     */
    fun updateAvatar(avatarUri: String?) {
        viewModelScope.launch {
            try {
                userProfileRepository.updateAvatar(avatarUri)
                _operationResult.value = "头像更新成功"
                loadUserData() // 刷新数据
            } catch (e: Exception) {
                _operationResult.value = "头像更新失败：${e.localizedMessage}"
            }
        }
    }
    
    // ==================== 设置操作 ====================
    
    /**
     * 更新主题设置
     */
    fun updateTheme(theme: String) {
        viewModelScope.launch {
            try {
                userProfileRepository.updateTheme(theme)
                _operationResult.value = "主题设置已更新"
            } catch (e: Exception) {
                _operationResult.value = "主题设置失败：${e.localizedMessage}"
            }
        }
    }
    
    /**
     * 更新通知设置
     */
    fun updateNotificationSettings(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userProfileRepository.updateNotificationSettings(enabled)
                _operationResult.value = "通知设置已更新"
            } catch (e: Exception) {
                _operationResult.value = "通知设置失败：${e.localizedMessage}"
            }
        }
    }
    
    /**
     * 更新应用锁设置
     */
    fun updateAppLockSettings(enabled: Boolean, lockType: String = "NONE") {
        viewModelScope.launch {
            try {
                userProfileRepository.updateAppLockSettings(enabled, lockType)
                _operationResult.value = "应用锁设置已更新"
            } catch (e: Exception) {
                _operationResult.value = "应用锁设置失败：${e.localizedMessage}"
            }
        }
    }
    
    // ==================== 徽章和成就 ====================
    
    /**
     * 获取用户徽章
     */
    fun getUserBadges() {
        viewModelScope.launch {
            try {
                val badges = userProfileRepository.getUnlockedBadges()
                // 这里可以进一步处理徽章数据
            } catch (e: Exception) {
                _operationResult.value = "徽章数据加载失败"
            }
        }
    }
    
    // ==================== 其他操作 ====================
    
    /**
     * 清除操作结果
     */
    fun clearOperationResult() {
        _operationResult.value = null
    }
    
    /**
     * 手动刷新数据
     */
    fun refreshData() {
        loadUserData()
    }
}
