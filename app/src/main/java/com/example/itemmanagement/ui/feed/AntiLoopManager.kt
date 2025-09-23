package com.example.itemmanagement.ui.feed

import kotlin.random.Random
import kotlin.math.sin

/**
 * 反循环管理器 - 防止信息流产生固定循环模式
 */
class AntiLoopManager {
    
    // 最近展示历史（用于多样性检测）
    private val recentDisplayHistory = mutableListOf<Long>()
    
    // 全局展示计数器（用于定期抖动）
    private var globalDisplayCount = 0
    
    /**
     * 应用反循环机制到物品分数
     */
    fun applyAntiLoopMechanisms(
        itemId: Long,
        baseScore: Float,
        algorithmScore: Float
    ): Float {
        var finalScore = baseScore
        
        // 1. 随机因子 (0-0.2分随机波动)
        finalScore += getRandomFactor()
        
        // 2. 时间动态因子 (基于当前时间的周期性变化)
        finalScore += getTimeDynamicFactor(itemId)
        
        // 3. 多样性惩罚 (如果最近频繁展示则降分)
        finalScore += getDiversityPenalty(itemId)
        
        // 4. 分数抖动 (定期对所有分数进行微调)
        finalScore += getScoreJitter(algorithmScore)
        
        return finalScore.coerceAtLeast(0f)
    }
    
    /**
     * 智能洗牌排序 - 对相似分数的物品进行随机排序
     */
    fun intelligentShuffle(scoredItems: List<Pair<Long, Float>>): List<Pair<Long, Float>> {
        // 按分数分组，相似分数的物品归为一组
        val groupedItems = mutableListOf<List<Pair<Long, Float>>>()
        val sortedItems = scoredItems.sortedByDescending { it.second }
        
        var currentGroup = mutableListOf<Pair<Long, Float>>()
        var lastScore = Float.MAX_VALUE
        
        for (item in sortedItems) {
            // 如果分数差异大于0.1，开始新组
            if (lastScore - item.second > 0.1f) {
                if (currentGroup.isNotEmpty()) {
                    groupedItems.add(currentGroup.toList())
                    currentGroup.clear()
                }
            }
            currentGroup.add(item)
            lastScore = item.second
        }
        
        // 添加最后一组
        if (currentGroup.isNotEmpty()) {
            groupedItems.add(currentGroup.toList())
        }
        
        // 对每组内部进行随机排序
        val result = mutableListOf<Pair<Long, Float>>()
        for (group in groupedItems) {
            result.addAll(group.shuffled())
        }
        
        return result
    }
    
    /**
     * 记录展示历史
     */
    fun recordDisplay(itemId: Long) {
        recentDisplayHistory.add(itemId)
        globalDisplayCount++
        
        // 只保留最近20个展示记录
        if (recentDisplayHistory.size > 20) {
            recentDisplayHistory.removeAt(0)
        }
    }
    
    /**
     * 获取随机因子 (0-0.2)
     */
    private fun getRandomFactor(): Float {
        return Random.nextFloat() * 0.2f
    }
    
    /**
     * 获取时间动态因子 (-0.1 到 +0.1)
     * 基于当前时间创建周期性变化，让同一物品在不同时间有不同的微调
     */
    private fun getTimeDynamicFactor(itemId: Long): Float {
        val currentTime = System.currentTimeMillis()
        
        // 基于小时的周期性变化
        val hourCycle = (currentTime / (60 * 60 * 1000)) % 24
        val hourFactor = sin((hourCycle * 2 * Math.PI / 24).toDouble()).toFloat() * 0.05f
        
        // 基于物品ID的个性化时间因子
        val itemTimeFactor = sin(((itemId * currentTime / (30 * 60 * 1000)) % 100).toDouble()).toFloat() * 0.05f
        
        return hourFactor + itemTimeFactor
    }
    
    /**
     * 获取多样性惩罚 (最近展示过的物品降分)
     */
    private fun getDiversityPenalty(itemId: Long): Float {
        val recentCount = recentDisplayHistory.count { it == itemId }
        
        return when {
            recentCount >= 3 -> -0.3f  // 最近展示3次以上，重度惩罚
            recentCount >= 2 -> -0.2f  // 最近展示2次，中度惩罚
            recentCount >= 1 -> -0.1f  // 最近展示1次，轻度惩罚
            else -> 0f                 // 没有展示过，无惩罚
        }
    }
    
    /**
     * 获取分数抖动 (定期微调)
     */
    private fun getScoreJitter(algorithmScore: Float): Float {
        // 每展示100个物品，进行一次全局抖动
        val jitterCycle = globalDisplayCount / 100
        
        if (jitterCycle > 0) {
            // 基于周期和算法分数的微调
            val jitterSeed = (jitterCycle * algorithmScore * 1000).toInt()
            val localRandom = Random(jitterSeed)
            return (localRandom.nextFloat() - 0.5f) * 0.1f  // -0.05 到 +0.05
        }
        
        return 0f
    }
    
    /**
     * 检测是否存在循环模式
     */
    fun detectLoopPattern(): Boolean {
        if (recentDisplayHistory.size < 10) return false
        
        // 检查最近10个展示中是否有重复模式
        val recent10 = recentDisplayHistory.takeLast(10)
        val unique = recent10.toSet()
        
        // 如果10个展示中只有3-4个不同物品，可能存在循环
        return unique.size <= 4
    }
    
    /**
     * 获取当前状态信息（用于调试）
     */
    fun getStatusInfo(): Map<String, Any> {
        return mapOf(
            "recentDisplayCount" to recentDisplayHistory.size,
            "globalDisplayCount" to globalDisplayCount,
            "uniqueRecentItems" to recentDisplayHistory.toSet().size,
            "potentialLoop" to detectLoopPattern(),
            "recentHistory" to recentDisplayHistory.takeLast(10)
        )
    }
    
    /**
     * 重置状态（测试用）
     */
    fun reset() {
        recentDisplayHistory.clear()
        globalDisplayCount = 0
    }
}
