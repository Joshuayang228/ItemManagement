package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * 用户资料数据访问对象
 * 管理用户资料的增删改查操作
 */
@Dao
interface UserProfileDao {
    
    // ==================== 基本CRUD操作 ====================
    
    /**
     * 创建或更新用户资料
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userProfile: UserProfileEntity)
    
    /**
     * 更新用户资料
     */
    @Update
    suspend fun update(userProfile: UserProfileEntity)
    
    /**
     * 删除用户资料
     */
    @Delete
    suspend fun delete(userProfile: UserProfileEntity)
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取用户资料（Flow）
     */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>
    
    /**
     * 获取用户资料（一次性）
     */
    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfile(): UserProfileEntity?
    
    /**
     * 检查用户资料是否存在
     */
    @Query("SELECT COUNT(*) > 0 FROM user_profile WHERE id = 1")
    suspend fun exists(): Boolean
    
    // ==================== 特定字段更新 ====================
    
    /**
     * 更新用户昵称
     */
    @Query("UPDATE user_profile SET nickname = :nickname, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateNickname(nickname: String, updateDate: Date = Date())
    
    /**
     * 更新用户头像
     */
    @Query("UPDATE user_profile SET avatarUri = :avatarUri, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateAvatar(avatarUri: String?, updateDate: Date = Date())
    
    /**
     * 更新主题偏好
     */
    @Query("UPDATE user_profile SET preferredTheme = :theme, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateTheme(theme: String, updateDate: Date = Date())
    
    /**
     * 更新通知设置
     */
    @Query("UPDATE user_profile SET enableNotifications = :enabled, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateNotificationSettings(enabled: Boolean, updateDate: Date = Date())
    
    /**
     * 更新应用锁设置
     */
    @Query("UPDATE user_profile SET enableAppLock = :enabled, lockType = :lockType, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateAppLockSettings(enabled: Boolean, lockType: String, updateDate: Date = Date())
    
    // ==================== 使用统计更新 ====================
    
    /**
     * 增加管理物品数量
     */
    @Query("UPDATE user_profile SET totalItemsManaged = totalItemsManaged + :count, updatedDate = :updateDate WHERE id = 1")
    suspend fun incrementItemsManaged(count: Int = 1, updateDate: Date = Date())
    
    /**
     * 更新当前物品数量
     */
    @Query("UPDATE user_profile SET currentItemCount = :count, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateCurrentItemCount(count: Int, updateDate: Date = Date())
    
    /**
     * 增加避免过期物品数
     */
    @Query("UPDATE user_profile SET expiredItemsAvoided = expiredItemsAvoided + :count, updatedDate = :updateDate WHERE id = 1")
    suspend fun incrementExpiredItemsAvoided(count: Int = 1, updateDate: Date = Date())
    
    /**
     * 更新节约金额
     */
    @Query("UPDATE user_profile SET totalSavedValue = totalSavedValue + :value, updatedDate = :updateDate WHERE id = 1")
    suspend fun addSavedValue(value: Double, updateDate: Date = Date())
    
    /**
     * 更新最后活跃时间
     */
    @Query("UPDATE user_profile SET lastActiveDate = :activeDate, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateLastActiveDate(activeDate: Date = Date(), updateDate: Date = Date())
    
    // ==================== 成就系统 ====================
    
    /**
     * 增加经验值
     */
    @Query("UPDATE user_profile SET experiencePoints = experiencePoints + :points, updatedDate = :updateDate WHERE id = 1")
    suspend fun addExperiencePoints(points: Int, updateDate: Date = Date())
    
    /**
     * 更新成就等级
     */
    @Query("UPDATE user_profile SET achievementLevel = :level, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateAchievementLevel(level: Int, updateDate: Date = Date())
    
    /**
     * 更新连续使用天数
     */
    @Query("UPDATE user_profile SET consecutiveDays = :days, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateConsecutiveDays(days: Int, updateDate: Date = Date())
    
    /**
     * 更新解锁的徽章
     */
    @Query("UPDATE user_profile SET unlockedBadges = :badges, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateUnlockedBadges(badges: String, updateDate: Date = Date())
    
    // ==================== 偏好设置 ====================
    
    /**
     * 更新默认分类
     */
    @Query("UPDATE user_profile SET defaultCategory = :category, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateDefaultCategory(category: String?, updateDate: Date = Date())
    
    /**
     * 更新默认单位
     */
    @Query("UPDATE user_profile SET defaultUnit = :unit, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateDefaultUnit(unit: String, updateDate: Date = Date())
    
    /**
     * 更新语言偏好
     */
    @Query("UPDATE user_profile SET preferredLanguage = :language, updatedDate = :updateDate WHERE id = 1")
    suspend fun updateLanguage(language: String, updateDate: Date = Date())
    
    // ==================== 复合更新操作 ====================
    
    /**
     * 更新用户活跃状态（活跃时间 + 连续天数）
     */
    @Query("""
        UPDATE user_profile 
        SET lastActiveDate = :activeDate, 
            consecutiveDays = :consecutiveDays,
            updatedDate = :updateDate 
        WHERE id = 1
    """)
    suspend fun updateUserActivity(
        activeDate: Date = Date(),
        consecutiveDays: Int,
        updateDate: Date = Date()
    )
    
    /**
     * 批量更新统计数据
     */
    @Query("""
        UPDATE user_profile 
        SET currentItemCount = :itemCount,
            totalItemsManaged = :totalManaged,
            expiredItemsAvoided = :expiredAvoided,
            totalSavedValue = :savedValue,
            experiencePoints = :expPoints,
            achievementLevel = :level,
            lastActiveDate = :activeDate,
            updatedDate = :updateDate
        WHERE id = 1
    """)
    suspend fun updateAllStats(
        itemCount: Int,
        totalManaged: Int,
        expiredAvoided: Int,
        savedValue: Double,
        expPoints: Int,
        level: Int,
        activeDate: Date = Date(),
        updateDate: Date = Date()
    )
}
