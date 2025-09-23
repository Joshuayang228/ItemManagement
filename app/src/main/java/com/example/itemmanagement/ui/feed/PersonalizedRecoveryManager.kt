package com.example.itemmanagement.ui.feed

import com.example.itemmanagement.data.model.*

/**
 * 个性化恢复管理器 - 根据物品特征计算个性化恢复速度
 */
class PersonalizedRecoveryManager {
    
    /**
     * 根据物品特征计算个性化恢复速度
     * 返回值：恢复速度倍数 (0.3f - 3.0f)
     * - 数值越大，恢复越快，越容易重新出现
     * - 数值越小，恢复越慢，重新出现的频率越低
     */
    fun calculateRecoveryRate(item: Item, algorithmScore: Float): Float {
        var recoveryRate = 1.0f // 基础恢复速度
        
        // 1. 算法分数影响（核心因子）
        recoveryRate *= when {
            algorithmScore >= 0.8f -> 2.0f    // 高分物品：2倍恢复速度
            algorithmScore >= 0.6f -> 1.5f    // 中高分物品：1.5倍恢复速度  
            algorithmScore >= 0.4f -> 1.0f    // 中等物品：正常恢复速度
            algorithmScore >= 0.2f -> 0.7f    // 中低分物品：0.7倍恢复速度
            else -> 0.5f                      // 低分物品：0.5倍恢复速度
        }
        
        // 2. 物品价值影响
        item.price?.let { price ->
            recoveryRate *= when {
                price >= 2000 -> 1.4f   // 高价值物品恢复更快
                price >= 1000 -> 1.3f   // 中高价值物品
                price >= 500  -> 1.1f   // 中等价值物品
                price >= 100  -> 1.0f   // 普通价值物品
                price >= 50   -> 0.9f   // 中低价值物品
                else -> 0.8f            // 低价值物品恢复慢
            }
        } ?: run {
            recoveryRate *= 0.9f // 无价格信息的物品恢复稍慢
        }
        
        // 3. 使用状态影响
        recoveryRate *= when(item.openStatus) {
            OpenStatus.UNOPENED -> 1.3f   // 未开封的更重要，恢复快
            OpenStatus.OPENED -> 1.0f     // 已开封正常恢复
            else -> 0.8f                  // 其他状态恢复稍慢
        }
        
        // 4. 物品状态影响
        recoveryRate *= when(item.status) {
            ItemStatus.EXPIRED -> 1.4f        // 过期物品需要快速关注
            ItemStatus.USED_UP -> 1.2f        // 用完了需要关注补货
            ItemStatus.IN_STOCK -> 1.0f       // 在库正常恢复
            ItemStatus.GIVEN_AWAY -> 0.8f     // 已转赠，较少关注
            ItemStatus.DISCARDED -> 0.6f      // 已丢弃，最少关注
        }
        
        // 5. 分类重要性影响
        recoveryRate *= when(item.category.lowercase()) {
            "数码产品", "电子设备", "电脑配件" -> 1.2f  // 数码产品更重要
            "重要文件", "证件", "文档" -> 1.3f          // 重要文件最重要
            "贵重物品", "珠宝", "收藏品" -> 1.2f        // 贵重物品重要
            "药品", "医疗用品" -> 1.3f                 // 药品很重要
            "食品", "零食", "饮料" -> 0.8f             // 食品相对不重要
            "消耗品", "日用品", "清洁用品" -> 0.7f      // 消耗品不重要
            "玩具", "游戏", "娱乐" -> 0.6f             // 娱乐物品不重要
            else -> 1.0f                              // 其他分类正常
        }
        
        // 6. 品牌影响（可选）
        item.brand?.let { brand ->
            val premiumBrands = setOf(
                "apple", "iphone", "ipad", "macbook", "mac",
                "samsung", "华为", "huawei", "xiaomi", "小米",
                "sony", "canon", "nikon", "bose",
                "gucci", "louis vuitton", "hermes", "chanel",
                "nike", "adidas", "puma"
            )
            
            if (premiumBrands.any { brand.lowercase().contains(it) }) {
                recoveryRate *= 1.1f // 知名品牌恢复稍快
            }
        }
        
        // 限制在合理范围内
        return recoveryRate.coerceIn(0.3f, 3.0f)
    }
    
    /**
     * 计算物品需要的恢复距离（个性化）
     * 返回值：需要浏览多少个其他物品后开始恢复
     */
    fun calculateRequiredBrowseDistance(
        item: Item, 
        algorithmScore: Float, 
        currentPenalty: Float
    ): Int {
        val recoveryRate = calculateRecoveryRate(item, algorithmScore)
        
        // 基础恢复距离：20个物品
        val baseDistance = 20
        
        // 根据恢复速度调整距离
        val adjustedDistance = (baseDistance / recoveryRate).toInt()
        
        // 根据当前惩罚程度调整
        val penaltyFactor = when {
            currentPenalty >= 1.5f -> 1.8f  // 重度惩罚，需要更多距离
            currentPenalty >= 1.0f -> 1.5f  // 高度惩罚
            currentPenalty >= 0.5f -> 1.2f  // 中等惩罚
            currentPenalty >= 0.2f -> 1.0f  // 轻微惩罚
            else -> 0.8f                    // 很少惩罚，可以更快恢复
        }
        
        // 计算最终距离
        val finalDistance = (adjustedDistance * penaltyFactor).toInt()
        
        // 限制在合理范围内：最少5个，最多100个
        return finalDistance.coerceIn(5, 100)
    }
    
    /**
     * 计算基于个性化距离的恢复分数
     */
    fun calculatePersonalizedRecovery(
        item: Item,
        algorithmScore: Float,
        currentPenalty: Float,
        browseDistance: Int
    ): Float {
        if (currentPenalty <= 0f) return 0f
        
        val requiredDistance = calculateRequiredBrowseDistance(item, algorithmScore, currentPenalty)
        
        // 计算恢复进度 (0.0 - 1.0)
        val recoveryProgress = (browseDistance.toFloat() / requiredDistance).coerceAtMost(1.0f)
        
        // 渐进式恢复，使用平滑曲线而非线性恢复
        val recoveryAmount = when {
            recoveryProgress >= 1.0f -> currentPenalty           // 完全恢复
            recoveryProgress >= 0.9f -> currentPenalty * 0.9f    // 90%恢复
            recoveryProgress >= 0.8f -> currentPenalty * 0.8f    // 80%恢复
            recoveryProgress >= 0.6f -> currentPenalty * 0.6f    // 60%恢复
            recoveryProgress >= 0.4f -> currentPenalty * 0.4f    // 40%恢复
            recoveryProgress >= 0.2f -> currentPenalty * 0.2f    // 20%恢复
            recoveryProgress >= 0.1f -> currentPenalty * 0.1f    // 10%恢复
            else -> 0f                                           // 未开始恢复
        }
        
        return recoveryAmount
    }
    
    /**
     * 获取物品的恢复状态描述（用于调试）
     */
    fun getRecoveryStatusDescription(
        item: Item,
        algorithmScore: Float,
        currentPenalty: Float,
        browseDistance: Int
    ): String {
        val recoveryRate = calculateRecoveryRate(item, algorithmScore)
        val requiredDistance = calculateRequiredBrowseDistance(item, algorithmScore, currentPenalty)
        val recoveryAmount = calculatePersonalizedRecovery(item, algorithmScore, currentPenalty, browseDistance)
        
        return """
            物品: ${item.name}
            算法分数: %.2f
            恢复速度: %.1fx
            当前惩罚: %.2f
            需要距离: %d
            已浏览距离: %d
            恢复量: %.2f
        """.trimIndent().format(
            algorithmScore, recoveryRate, currentPenalty, 
            requiredDistance, browseDistance, recoveryAmount
        )
    }
}
