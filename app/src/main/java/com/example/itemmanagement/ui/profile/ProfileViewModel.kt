package com.example.itemmanagement.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.R
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.model.ProfileItem
import com.example.itemmanagement.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

/**
 * "我的"页面ViewModel
 * 管理个人资料、统计信息和各种功能入口
 */
class ProfileViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val itemRepository: UnifiedItemRepository
) : ViewModel() {
    
    // ==================== 数据 ====================
    
    
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
    
    /**
     * Profile列表项数据
     */
    private val _profileItems = MutableLiveData<List<ProfileItem>>()
    val profileItems: LiveData<List<ProfileItem>> = _profileItems
    
    // ==================== 初始化 ====================
    
    init {
        loadUserData()
        loadProfileItems()
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
                
            } catch (e: Exception) {
                _operationResult.value = "数据加载失败：${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
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
    
    /**
     * 加载Profile列表项
     */
    private fun loadProfileItems() {
        val items = mutableListOf<ProfileItem>().apply {
            // 用户信息卡片
            add(ProfileItem.UserInfo)
            
            // 第一个间隔
            add(ProfileItem.MenuSpacer)
            
            // 工具卡片（回收站、应用设置、数据导出合并）
            add(ProfileItem.MenuItem("recycle_bin", "回收站", R.drawable.ic_delete_colorful))
            add(ProfileItem.MenuItem("app_settings", "应用设置", R.drawable.ic_tune_colorful))
            add(ProfileItem.MenuItem("data_export", "数据导出", R.drawable.ic_save_colorful, showDivider = false))
            
            // 第二个间隔
            add(ProfileItem.MenuSpacer)
            
            // 联系我们和关于应用（合并卡片）
            add(ProfileItem.MenuItem("donation", "联系我们", R.drawable.ic_favorite_colorful))
            add(ProfileItem.MenuItem("about_app", "关于应用", R.drawable.ic_info_colorful, showDivider = false))
        }
        _profileItems.value = items
    }
}
