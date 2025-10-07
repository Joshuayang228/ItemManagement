package com.example.itemmanagement.ui.profile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

/**
 * 应用设置页面的ViewModel
 * 简化版：不使用LiveData/Flow，由Fragment主动调用
 */
class AppSettingsViewModel(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    /**
     * 获取当前主题
     * @return 主题字符串：LIGHT, DARK, 或 AUTO
     */
    suspend fun getTheme(): String {
        return try {
            val profile = userProfileRepository.getUserProfile()
            profile.preferredTheme
        } catch (e: Exception) {
            "AUTO"  // 默认值
        }
    }
    
    /**
     * 保存主题设置（同步等待，确保保存完成）
     * @param theme 主题字符串：LIGHT, DARK, 或 AUTO
     */
    suspend fun saveThemeSync(theme: String) {
        try {
            val profile = userProfileRepository.getUserProfile()
            val updated = profile.copy(preferredTheme = theme)
            userProfileRepository.updateUserProfile(updated)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 重置设置
     */
    fun resetSettings() {
        viewModelScope.launch {
            try {
                val profile = userProfileRepository.getUserProfile()
                val reset = profile.copy(preferredTheme = "AUTO")
                userProfileRepository.updateUserProfile(reset)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
