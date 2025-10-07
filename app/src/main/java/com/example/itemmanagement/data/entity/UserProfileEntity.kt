package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 用户资料实体类
 * 存储用户的个人信息、偏好设置和使用统计
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Long = 1L, // 单用户应用，固定ID为1
    
    // ==================== 基本信息 ====================
    val userId: String = generateUserId(), // 9位数字用户ID
    val nickname: String = "物品管家用户",
    val signature: String? = null,          // 个性签名
    val avatarUri: String? = null,
    val joinDate: Date = Date(),
    
    // ==================== 使用统计 ====================
    val totalItemsManaged: Int = 0,        // 累计管理物品数
    val currentItemCount: Int = 0,          // 当前物品数量
    val expiredItemsAvoided: Int = 0,       // 成功避免过期的物品数
    val totalSavedValue: Double = 0.0,      // 估算节约金额
    val consecutiveDays: Int = 0,           // 连续使用天数
    val lastActiveDate: Date = Date(),      // 最后活跃日期
    
    // ==================== 成就统计 ====================
    val achievementLevel: Int = 1,          // 成就等级 (1-10)
    val experiencePoints: Int = 0,          // 经验值
    val unlockedBadges: String = "",        // 已解锁徽章 (JSON格式)
    
    // ==================== 偏好设置 ====================
    val preferredTheme: String = "AUTO",    // 主题偏好: LIGHT, DARK, AUTO
    val preferredLanguage: String = "zh",   // 语言偏好
    val enableNotifications: Boolean = true, // 是否启用通知
    val enableSoundEffects: Boolean = true,  // 是否启用音效
    val defaultCategory: String? = null,     // 默认分类
    val defaultUnit: String = "个",          // 默认单位
    
    // ==================== 隐私设置 ====================
    val enableAppLock: Boolean = false,     // 是否启用应用锁
    val lockType: String = "NONE",          // 锁类型: NONE, PIN, FINGERPRINT, FACE
    val showStatsInProfile: Boolean = true, // 是否在资料中显示统计
    val dataBackupEnabled: Boolean = true,  // 是否启用数据备份
    
    // ==================== 应用设置 ====================
    val autoBackupFreq: String = "WEEKLY",  // 自动备份频率: DAILY, WEEKLY, MONTHLY, NEVER
    val reminderFreq: String = "DAILY",     // 提醒频率: DAILY, WEEKLY, NEVER
    val compactModeEnabled: Boolean = false, // 紧凑模式
    val showTutorialTips: Boolean = true,    // 显示教程提示
    
    // ==================== 元数据 ====================
    val createdDate: Date = Date(),
    val updatedDate: Date = Date(),
    val appVersion: String = "1.0.0",       // 创建时的应用版本
    val profileVersion: Int = 1              // 资料结构版本
) {
    /**
     * 计算使用天数
     */
    fun getUsageDays(): Int {
        val now = System.currentTimeMillis()
        val joinTime = joinDate.time
        return ((now - joinTime) / (1000 * 60 * 60 * 24)).toInt() + 1
    }
    
    /**
     * 获取等级称号
     */
    fun getLevelTitle(): String {
        return when (achievementLevel) {
            1 -> "新手管家"
            2 -> "整理达人"
            3 -> "收纳专家"
            4 -> "物品大师"
            5 -> "管理高手"
            6 -> "组织之王"
            7 -> "效率专家"
            8 -> "超级管家"
            9 -> "收纳大神"
            10 -> "终极管家"
            else -> "神秘管家"
        }
    }
    
    /**
     * 计算下一级所需经验
     */
    fun getExpToNextLevel(): Int {
        return when (achievementLevel) {
            1 -> 100 - experiencePoints
            2 -> 300 - experiencePoints
            3 -> 600 - experiencePoints
            4 -> 1000 - experiencePoints
            5 -> 1500 - experiencePoints
            6 -> 2200 - experiencePoints
            7 -> 3000 - experiencePoints
            8 -> 4000 - experiencePoints
            9 -> 5500 - experiencePoints
            10 -> 0 // 最高级
            else -> 0
        }
    }
    
    /**
     * 获取成就进度百分比
     */
    fun getAchievementProgress(): Float {
        val currentLevelMin = when (achievementLevel) {
            1 -> 0
            2 -> 100
            3 -> 300
            4 -> 600
            5 -> 1000
            6 -> 1500
            7 -> 2200
            8 -> 3000
            9 -> 4000
            10 -> 5500
            else -> 0
        }
        
        val nextLevelMin = when (achievementLevel) {
            1 -> 100
            2 -> 300
            3 -> 600
            4 -> 1000
            5 -> 1500
            6 -> 2200
            7 -> 3000
            8 -> 4000
            9 -> 5500
            10 -> 5500 // 最高级
            else -> 100
        }
        
        if (achievementLevel >= 10) return 1.0f
        
        val progress = (experiencePoints - currentLevelMin).toFloat()
        val total = (nextLevelMin - currentLevelMin).toFloat()
        
        return if (total > 0) (progress / total).coerceIn(0f, 1f) else 0f
    }
    
    /**
     * 是否为活跃用户（最近7天内活跃）
     */
    fun isActiveUser(): Boolean {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7 * 24 * 60 * 60 * 1000)
        return lastActiveDate.time > sevenDaysAgo
    }
    
    /**
     * 获取用户级别颜色
     */
    fun getLevelColor(): String {
        return when (achievementLevel) {
            1, 2 -> "#4CAF50"      // 绿色 - 新手
            3, 4 -> "#2196F3"      // 蓝色 - 中级
            5, 6 -> "#9C27B0"      // 紫色 - 高级
            7, 8 -> "#FF9800"      // 橙色 - 专家
            9, 10 -> "#F44336"     // 红色 - 大师
            else -> "#607D8B"      // 灰色 - 默认
        }
    }
    
    companion object {
        /**
         * 生成9位数字的用户ID
         * 算法：使用时间戳的后6位 + 3位随机数，确保唯一性
         */
        fun generateUserId(): String {
            val timestamp = System.currentTimeMillis()
            // 取时间戳的后6位数字
            val timePart = (timestamp % 1000000).toString().padStart(6, '0')
            // 生成3位随机数
            val randomPart = (100..999).random().toString()
            // 组合成9位数字
            return timePart + randomPart
        }
    }
}
