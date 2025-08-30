package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.CustomRuleEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface CustomRuleDao {
    
    /**
     * 获取所有自定义规则（Flow版本）
     */
    @Query("SELECT * FROM custom_rules ORDER BY createdAt DESC")
    fun getAllRulesFlow(): Flow<List<CustomRuleEntity>>
    
    /**
     * 获取所有自定义规则（一次性获取）
     */
    @Query("SELECT * FROM custom_rules ORDER BY createdAt DESC")
    suspend fun getAllRules(): List<CustomRuleEntity>
    
    /**
     * 获取所有启用的自定义规则
     */
    @Query("SELECT * FROM custom_rules WHERE enabled = 1 ORDER BY createdAt DESC")
    suspend fun getActiveRules(): List<CustomRuleEntity>
    
    /**
     * 根据规则类型获取规则
     */
    @Query("SELECT * FROM custom_rules WHERE ruleType = :type AND enabled = 1 ORDER BY createdAt DESC")
    suspend fun getRulesByType(type: String): List<CustomRuleEntity>
    
    /**
     * 根据ID获取规则
     */
    @Query("SELECT * FROM custom_rules WHERE id = :id")
    suspend fun getRuleById(id: Long): CustomRuleEntity?
    
    /**
     * 插入新规则
     */
    @Insert
    suspend fun insertRule(rule: CustomRuleEntity): Long
    
    /**
     * 更新规则
     */
    @Update
    suspend fun updateRule(rule: CustomRuleEntity)
    
    /**
     * 删除规则
     */
    @Delete
    suspend fun deleteRule(rule: CustomRuleEntity)
    
    /**
     * 根据ID删除规则
     */
    @Query("DELETE FROM custom_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)
    
    /**
     * 启用/禁用规则
     */
    @Query("UPDATE custom_rules SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateRuleEnabled(id: Long, enabled: Boolean, updatedAt: Date = Date())
    
    /**
     * 更新规则最后触发时间
     */
    @Query("UPDATE custom_rules SET lastTriggeredAt = :triggeredAt WHERE id = :id")
    suspend fun updateLastTriggeredAt(id: Long, triggeredAt: Date)
    
    /**
     * 根据分类获取相关规则
     */
    @Query("SELECT * FROM custom_rules WHERE (targetCategory IS NULL OR targetCategory = :category) AND enabled = 1")
    suspend fun getRulesForCategory(category: String): List<CustomRuleEntity>
    
    /**
     * 根据物品名称获取相关规则（模糊匹配）
     */
    @Query("SELECT * FROM custom_rules WHERE (targetItemName IS NULL OR :itemName LIKE '%' || targetItemName || '%') AND enabled = 1")
    suspend fun getRulesForItem(itemName: String): List<CustomRuleEntity>
    
    /**
     * 获取规则统计信息
     */
    @Query("SELECT COUNT(*) FROM custom_rules WHERE enabled = 1")
    suspend fun getActiveRulesCount(): Int
    
    @Query("SELECT COUNT(*) FROM custom_rules WHERE ruleType = :type AND enabled = 1")
    suspend fun getActiveRulesCountByType(type: String): Int
}
