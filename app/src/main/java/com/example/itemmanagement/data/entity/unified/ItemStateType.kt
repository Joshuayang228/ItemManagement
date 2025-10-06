package com.example.itemmanagement.data.entity.unified

/**
 * 物品状态类型枚举
 * 定义物品在系统中可能存在的状态
 */
enum class ItemStateType(
    val displayName: String,
    val description: String
) {
    SHOPPING("购物清单", "计划购买的物品"),
    INVENTORY("库存", "已拥有的物品"),
    DELETED("已删除", "已删除的物品（回收站）");
    
    companion object {
        /**
         * 获取所有显示名称
         */
        fun getDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
        
        /**
         * 根据显示名称获取状态类型
         */
        fun fromDisplayName(displayName: String): ItemStateType? {
            return values().firstOrNull { it.displayName == displayName }
        }
        
        /**
         * 检查状态是否为活跃业务状态（排除删除状态）
         */
        fun isActiveBusinessState(stateType: ItemStateType): Boolean {
            return stateType != DELETED
        }
        
        /**
         * 获取状态的默认顺序（用于UI显示）
         */
        fun getDefaultOrder(): List<ItemStateType> {
            return listOf(SHOPPING, INVENTORY, DELETED)
        }
    }
}

