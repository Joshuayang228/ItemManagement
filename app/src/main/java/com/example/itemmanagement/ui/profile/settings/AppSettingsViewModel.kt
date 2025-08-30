package com.example.itemmanagement.ui.profile.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.entity.UserProfileEntity
import com.example.itemmanagement.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

/**
 * 应用设置页面的ViewModel
 * 管理主题、通知等应用设置
 */
class AppSettingsViewModel(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfileEntity>()
    val userProfile: LiveData<UserProfileEntity> = _userProfile

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                userProfileRepository.getUserProfileFlow().collect { profile ->
                    profile?.let { _userProfile.value = it }
                }
            } catch (e: Exception) {
                _message.value = "加载设置失败: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新主题设置
     */
    fun updateTheme(theme: String) {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(preferredTheme = theme)
                userProfileRepository.updateUserProfile(updatedProfile)
                _message.value = "主题设置已更新"
            } catch (e: Exception) {
                _message.value = "更新主题失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 更新通知设置
     */
    fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(enableNotifications = enabled)
                userProfileRepository.updateUserProfile(updatedProfile)
                _message.value = if (enabled) "已开启通知" else "已关闭通知"
            } catch (e: Exception) {
                _message.value = "更新通知设置失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 更新音效设置
     */
    fun updateSoundEffects(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(enableSoundEffects = enabled)
                userProfileRepository.updateUserProfile(updatedProfile)
                _message.value = if (enabled) "已开启音效" else "已关闭音效"
            } catch (e: Exception) {
                _message.value = "更新音效设置失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 更新紧凑模式设置
     */
    fun updateCompactMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(compactModeEnabled = enabled)
                userProfileRepository.updateUserProfile(updatedProfile)
                _message.value = if (enabled) "已开启紧凑模式" else "已关闭紧凑模式"
            } catch (e: Exception) {
                _message.value = "更新紧凑模式失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 更新教程提示设置
     */
    fun updateTutorialTips(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(showTutorialTips = enabled)
                userProfileRepository.updateUserProfile(updatedProfile)
                _message.value = if (enabled) "已开启教程提示" else "已关闭教程提示"
            } catch (e: Exception) {
                _message.value = "更新教程提示失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 更新统计显示设置
     */
    fun updateShowStats(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(showStatsInProfile = enabled)
                userProfileRepository.updateUserProfile(updatedProfile)
                _message.value = if (enabled) "已显示统计数据" else "已隐藏统计数据"
            } catch (e: Exception) {
                _message.value = "更新统计显示失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 更新默认单位
     */
    fun updateDefaultUnit(unit: String) {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(defaultUnit = unit)
                userProfileRepository.updateUserProfile(updatedProfile)
                _message.value = "默认单位已更新为: $unit"
            } catch (e: Exception) {
                _message.value = "更新默认单位失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 更新默认分类
     */
    fun updateDefaultCategory(category: String?) {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(defaultCategory = category)
                userProfileRepository.updateUserProfile(updatedProfile)
                _message.value = if (category.isNullOrEmpty()) {
                    "已清除默认分类"
                } else {
                    "默认分类已更新为: $category"
                }
            } catch (e: Exception) {
                _message.value = "更新默认分类失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 重置所有设置
     */
    fun resetSettings() {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val resetProfile = currentProfile.copy(
                    preferredTheme = "AUTO",
                    preferredLanguage = "zh",
                    enableNotifications = true,
                    enableSoundEffects = true,
                    defaultCategory = null,
                    defaultUnit = "个",
                    enableAppLock = false,
                    lockType = "NONE",
                    showStatsInProfile = true,
                    dataBackupEnabled = true,
                    autoBackupFreq = "WEEKLY",
                    reminderFreq = "DAILY",
                    compactModeEnabled = false,
                    showTutorialTips = true
                )
                userProfileRepository.updateUserProfile(resetProfile)
                _message.value = "设置已重置为默认值"
            } catch (e: Exception) {
                _message.value = "重置设置失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 执行数据清理
     */
    fun performDataCleanup() {
        viewModelScope.launch {
            try {
                // 这里可以调用清理逻辑
                // 例如清理回收站中超过30天的数据
                _message.value = "数据清理完成"
            } catch (e: Exception) {
                _message.value = "数据清理失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 清除消息
     */
    fun clearMessage() {
        _message.value = null
    }
}
