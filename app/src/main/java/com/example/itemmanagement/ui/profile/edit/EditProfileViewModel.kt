package com.example.itemmanagement.ui.profile.edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.entity.UserProfileEntity
import com.example.itemmanagement.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

/**
 * 个人资料编辑页面的ViewModel
 * 管理头像上传、昵称修改等功能
 */
class EditProfileViewModel(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _userProfile = MutableLiveData<UserProfileEntity>()
    val userProfile: LiveData<UserProfileEntity> = _userProfile

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _saveSuccess = MutableLiveData<Boolean>()
    val saveSuccess: LiveData<Boolean> = _saveSuccess

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
                _message.value = "加载个人资料失败: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新昵称
     */
    fun updateNickname(nickname: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(nickname = nickname)
                userProfileRepository.updateUserProfile(updatedProfile)
                _userProfile.value = updatedProfile
                _saveSuccess.value = true
            } catch (e: Exception) {
                _message.value = "更新昵称失败: ${e.localizedMessage}"
                _saveSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新头像URI
     */
    fun updateAvatarUri(avatarUri: String) {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(avatarUri = avatarUri)
                userProfileRepository.updateUserProfile(updatedProfile)
                _userProfile.value = updatedProfile
                _message.value = "头像已更新"
            } catch (e: Exception) {
                _message.value = "更新头像失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 移除头像
     */
    fun removeAvatar() {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val updatedProfile = currentProfile.copy(avatarUri = null)
                userProfileRepository.updateUserProfile(updatedProfile)
                _userProfile.value = updatedProfile
                _message.value = "头像已移除"
            } catch (e: Exception) {
                _message.value = "移除头像失败: ${e.localizedMessage}"
            }
        }
    }

    /**
     * 验证输入
     */
    fun validateInput(nickname: String): String? {
        return when {
            nickname.trim().isEmpty() -> "昵称不能为空"
            nickname.trim().length > 20 -> "昵称不能超过20个字符"
            nickname.trim().length < 2 -> "昵称至少需要2个字符"
            else -> null
        }
    }

    /**
     * 重置个人资料统计数据（危险操作）
     */
    fun resetProfileStats() {
        viewModelScope.launch {
            try {
                val currentProfile = userProfileRepository.getUserProfile()
                val resetProfile = currentProfile.copy(
                    totalItemsManaged = 0,
                    currentItemCount = 0,
                    expiredItemsAvoided = 0,
                    totalSavedValue = 0.0,
                    consecutiveDays = 0,
                    achievementLevel = 1,
                    experiencePoints = 0,
                    unlockedBadges = ""
                )
                userProfileRepository.updateUserProfile(resetProfile)
                _userProfile.value = resetProfile
                _message.value = "统计数据已重置"
            } catch (e: Exception) {
                _message.value = "重置统计数据失败: ${e.localizedMessage}"
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
