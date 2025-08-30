package com.example.itemmanagement.data.repository

import com.example.itemmanagement.data.dao.UserProfileDao
import com.example.itemmanagement.data.entity.UserProfileEntity
import com.example.itemmanagement.ui.profile.UserStatsSummary
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.Date

/**
 * 用户资料仓库
 * 提供用户资料相关的业务逻辑和数据操作
 */
class UserProfileRepository(
    private val userProfileDao: UserProfileDao
) {
    
    // ==================== 数据流 ====================
    
    /**
     * 获取用户资料的实时数据流
     */
    fun getUserProfileFlow(): Flow<UserProfileEntity?> {
        return userProfileDao.getUserProfileFlow()
    }
    
    // ==================== 基本操作 ====================
    
    /**
     * 获取用户资料，如果不存在则创建默认资料
     */
    suspend fun getUserProfile(): UserProfileEntity {
        var profile = userProfileDao.getUserProfile()
        if (profile == null) {
            profile = createDefaultProfile()
            userProfileDao.insertOrUpdate(profile)
        }
        return profile
    }
    
    /**
     * 创建默认用户资料
     */
    private fun createDefaultProfile(): UserProfileEntity {
        return UserProfileEntity(
            nickname = "物品管家用户",
            joinDate = Date(),
            createdDate = Date(),
            updatedDate = Date()
        )
    }
    
    /**
     * 更新用户资料
     */
    suspend fun updateUserProfile(profile: UserProfileEntity) {
        userProfileDao.insertOrUpdate(profile.copy(updatedDate = Date()))
    }
    
    // ==================== 基本信息更新 ====================
    
    /**
     * 更新用户昵称
     */
    suspend fun updateNickname(nickname: String) {
        if (nickname.isBlank() || nickname.length > 20) {
            throw IllegalArgumentException("昵称不能为空且不能超过20个字符")
        }
        userProfileDao.updateNickname(nickname)
    }
    
    /**
     * 更新用户头像
     */
    suspend fun updateAvatar(avatarUri: String?) {
        userProfileDao.updateAvatar(avatarUri)
    }
    
    // ==================== 偏好设置 ====================
    
    /**
     * 更新主题偏好
     */
    suspend fun updateTheme(theme: String) {
        if (theme !in listOf("LIGHT", "DARK", "AUTO")) {
            throw IllegalArgumentException("无效的主题设置")
        }
        userProfileDao.updateTheme(theme)
    }
    
    /**
     * 更新通知设置
     */
    suspend fun updateNotificationSettings(enabled: Boolean) {
        userProfileDao.updateNotificationSettings(enabled)
    }
    
    /**
     * 更新应用锁设置
     */
    suspend fun updateAppLockSettings(enabled: Boolean, lockType: String = "NONE") {
        if (enabled && lockType !in listOf("PIN", "FINGERPRINT", "FACE")) {
            throw IllegalArgumentException("启用应用锁时必须指定有效的锁类型")
        }
        userProfileDao.updateAppLockSettings(enabled, if (enabled) lockType else "NONE")
    }
    
    /**
     * 更新默认设置
     */
    suspend fun updateDefaultSettings(category: String?, unit: String) {
        if (unit.isBlank()) {
            throw IllegalArgumentException("默认单位不能为空")
        }
        userProfileDao.updateDefaultCategory(category)
        userProfileDao.updateDefaultUnit(unit)
    }
    
    // ==================== 使用统计更新 ====================
    
    /**
     * 记录用户活跃（每日调用）
     */
    suspend fun recordDailyActivity() {
        val profile = getUserProfile()
        val today = Date()
        val yesterday = Date(today.time - 24 * 60 * 60 * 1000)
        
        // 计算连续天数
        val consecutiveDays = if (isSameDay(profile.lastActiveDate, yesterday) || 
                                 isSameDay(profile.lastActiveDate, today)) {
            if (isSameDay(profile.lastActiveDate, yesterday)) {
                profile.consecutiveDays + 1
            } else {
                profile.consecutiveDays // 今天已经记录过
            }
        } else {
            1 // 重新开始计算
        }
        
        userProfileDao.updateUserActivity(today, consecutiveDays)
        
        // 根据连续天数给予经验奖励
        if (consecutiveDays % 7 == 0) { // 每连续7天奖励
            addExperience(50, "连续使用${consecutiveDays}天")
        } else if (!isSameDay(profile.lastActiveDate, today)) { // 每日首次活跃
            addExperience(10, "每日活跃")
        }
    }
    
    /**
     * 增加管理物品数量
     */
    suspend fun addItemManaged(count: Int = 1) {
        userProfileDao.incrementItemsManaged(count)
        // 每管理10个物品奖励经验
        if (count >= 10 || (getUserProfile().totalItemsManaged + count) % 10 == 0) {
            addExperience(20, "管理物品数量达到里程碑")
        } else {
            addExperience(5 * count, "管理物品")
        }
    }
    
    /**
     * 更新当前物品数量
     */
    suspend fun updateCurrentItemCount(count: Int) {
        userProfileDao.updateCurrentItemCount(count)
    }
    
    /**
     * 记录避免过期的物品
     */
    suspend fun recordExpiredItemAvoided(count: Int = 1, estimatedValue: Double = 0.0) {
        userProfileDao.incrementExpiredItemsAvoided(count)
        if (estimatedValue > 0) {
            userProfileDao.addSavedValue(estimatedValue)
        }
        addExperience(15 * count, "避免物品过期")
    }
    
    // ==================== 成就系统 ====================
    
    /**
     * 增加经验值并检查等级提升
     */
    suspend fun addExperience(points: Int, reason: String = "") {
        if (points <= 0) return
        
        val profile = getUserProfile()
        val newExp = profile.experiencePoints + points
        userProfileDao.addExperiencePoints(points)
        
        // 检查等级提升
        val newLevel = calculateLevel(newExp)
        if (newLevel > profile.achievementLevel) {
            userProfileDao.updateAchievementLevel(newLevel)
            // 可以在这里触发等级提升通知
        }
    }
    
    /**
     * 根据经验值计算等级
     */
    private fun calculateLevel(experience: Int): Int {
        return when {
            experience < 100 -> 1
            experience < 300 -> 2
            experience < 600 -> 3
            experience < 1000 -> 4
            experience < 1500 -> 5
            experience < 2200 -> 6
            experience < 3000 -> 7
            experience < 4000 -> 8
            experience < 5500 -> 9
            else -> 10
        }
    }
    
    /**
     * 解锁徽章
     */
    suspend fun unlockBadge(badgeId: String, badgeName: String) {
        val profile = getUserProfile()
        val currentBadges = try {
            if (profile.unlockedBadges.isNotEmpty()) {
                JSONArray(profile.unlockedBadges)
            } else {
                JSONArray()
            }
        } catch (e: Exception) {
            JSONArray()
        }
        
        // 检查是否已经解锁
        val badges = mutableListOf<String>()
        for (i in 0 until currentBadges.length()) {
            badges.add(currentBadges.getString(i))
        }
        
        if (badgeId !in badges) {
            badges.add(badgeId)
            val newBadgesJson = JSONArray(badges).toString()
            userProfileDao.updateUnlockedBadges(newBadgesJson)
            addExperience(30, "解锁徽章：$badgeName")
        }
    }
    
    /**
     * 获取已解锁的徽章列表
     */
    suspend fun getUnlockedBadges(): List<String> {
        val profile = getUserProfile()
        return try {
            if (profile.unlockedBadges.isNotEmpty()) {
                val jsonArray = JSONArray(profile.unlockedBadges)
                (0 until jsonArray.length()).map { jsonArray.getString(it) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ==================== 统计分析 ====================
    
    /**
     * 获取用户统计摘要
     */
    suspend fun getUserStatsSummary(): UserStatsSummary {
        val profile = getUserProfile()
        return UserStatsSummary(
            nickname = profile.nickname,
            level = profile.achievementLevel,
            levelTitle = profile.getLevelTitle(),
            experiencePoints = profile.experiencePoints,
            expToNextLevel = profile.getExpToNextLevel(),
            progress = profile.getAchievementProgress(),
            usageDays = profile.getUsageDays(),
            consecutiveDays = profile.consecutiveDays,
            totalItemsManaged = profile.totalItemsManaged,
            currentItemCount = profile.currentItemCount,
            expiredItemsAvoided = profile.expiredItemsAvoided,
            totalSavedValue = profile.totalSavedValue,
            unlockedBadgeCount = getUnlockedBadges().size,
            isActiveUser = profile.isActiveUser(),
            joinDate = profile.joinDate,
            levelColor = profile.getLevelColor()
        )
    }
    
    /**
     * 重置用户数据（慎用）
     */
    suspend fun resetUserProfile() {
        val newProfile = createDefaultProfile()
        userProfileDao.insertOrUpdate(newProfile)
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查两个日期是否为同一天
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = java.util.Calendar.getInstance()
        val cal2 = java.util.Calendar.getInstance()
        cal1.time = date1
        cal2.time = date2
        
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
