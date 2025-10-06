package com.example.itemmanagement.data.entity.unified

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 物品状态管理实体
 * 管理物品在不同状态之间的流转
 * 一个物品可以同时拥有多个状态（如同时在心愿单和购物清单中）
 */
@Entity(
    tableName = "item_states",
    foreignKeys = [
        ForeignKey(
            entity = UnifiedItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("itemId"),
        Index("stateType"),
        Index("isActive"),
        Index(value = ["itemId", "stateType", "isActive"]),  // 组合索引，优化常用查询
        Index("activatedDate"),
        Index("contextId")
    ]
)
data class ItemStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 关联的统一物品ID */
    val itemId: Long,
    
    /** 状态类型 */
    val stateType: ItemStateType,
    
    /** 是否激活（软删除标记） */
    val isActive: Boolean = true,
    
    /** 状态激活时间 */
    val activatedDate: Date = Date(),
    
    /** 状态停用时间（null表示仍激活） */
    val deactivatedDate: Date? = null,
    
    /** 上下文ID（可选）
     * - 对于SHOPPING状态：存储shopping_list_id
     * - 对于其他状态：通常为null
     */
    val contextId: Long? = null,
    
    /** 状态元数据（JSON格式存储额外信息） */
    val metadata: String? = null,
    
    /** 状态转换原因/备注 */
    val notes: String? = null,
    
    /** 创建时间 */
    val createdDate: Date = Date()
) {
    /**
     * 停用当前状态
     */
    fun deactivate(reason: String? = null): ItemStateEntity {
        return this.copy(
            isActive = false,
            deactivatedDate = Date(),
            notes = reason
        )
    }
    
    /**
     * 检查状态是否过期（激活超过30天且已停用）
     */
    fun isExpired(): Boolean {
        if (isActive || deactivatedDate == null) return false
        
        val now = System.currentTimeMillis()
        val deactivatedTime = deactivatedDate.time
        val daysPassed = (now - deactivatedTime) / (1000 * 60 * 60 * 24)
        
        return daysPassed > 30
    }
    
    /**
     * 获取状态持续时间（天数）
     */
    fun getDurationDays(): Int {
        val endTime = deactivatedDate?.time ?: System.currentTimeMillis()
        val startTime = activatedDate.time
        return ((endTime - startTime) / (1000 * 60 * 60 * 24)).toInt()
    }
    
    /**
     * 检查是否为购物清单状态
     */
    fun isShoppingState(): Boolean {
        return stateType == ItemStateType.SHOPPING && contextId != null
    }
    
    /**
     * 获取状态显示标签
     */
    fun getStateDisplayLabel(): String {
        return when (stateType) {
            ItemStateType.SHOPPING -> if (contextId != null) "购物清单" else "待购买"
            ItemStateType.INVENTORY -> "库存"
            ItemStateType.DELETED -> "已删除"
        }
    }
}