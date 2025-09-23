package com.example.itemmanagement.ui.feed

import com.example.itemmanagement.data.model.Item

/**
 * 自适应展示管理器 - 管理物品展示历史、惩罚机制和恢复逻辑
 */
class AdaptiveDisplayManager {
    
    /**
     * 物品展示状态
     */
    data class ItemDisplayState(
        val itemId: Long,
        var displayPenalty: Float = 0f,        // 当前展示惩罚分数
        var lastDisplayPosition: Int = 0,      // 最后展示时的全局位置
        var displayCount: Int = 0,             // 累计展示次数
        var totalDisplayCount: Int = 0         // 历史总展示次数（包括重置前）
    )
    
    /**
     * 展示分数计算结果
     */
    data class DisplayScoreResult(
        val finalScore: Float,          // 最终展示分数
        val algorithmScore: Float,      // 原始算法分数
        val penalty: Float,             // 当前惩罚分数
        val recovery: Float,            // 当前恢复分数
        val browseDistance: Int,        // 浏览距离
        val requiredDistance: Int       // 需要的恢复距离
    )
    
    // 存储所有物品的展示状态
    private val displayStates = mutableMapOf<Long, ItemDisplayState>()
    
    // 全局展示位置计数器（用于计算浏览距离）
    private var globalDisplayPosition = 0
    
    // 个性化恢复管理器
    private val recoveryManager = PersonalizedRecoveryManager()
    
    /**
     * 计算物品的最终展示分数
     */
    fun calculateDisplayScore(item: Item, algorithmScore: Float): Float {
        val result = calculateDisplayScoreDetailed(item, algorithmScore)
        return result.finalScore
    }
    
    /**
     * 计算物品的详细展示分数（包含调试信息）
     */
    fun calculateDisplayScoreDetailed(item: Item, algorithmScore: Float): DisplayScoreResult {
        val state = getOrCreateDisplayState(item.id)
        
        // 计算浏览距离
        val browseDistance = globalDisplayPosition - state.lastDisplayPosition
        
        // 计算个性化恢复
        val recovery = recoveryManager.calculatePersonalizedRecovery(
            item = item,
            algorithmScore = algorithmScore,
            currentPenalty = state.displayPenalty,
            browseDistance = browseDistance
        )
        
        // 计算需要的恢复距离
        val requiredDistance = recoveryManager.calculateRequiredBrowseDistance(
            item = item,
            algorithmScore = algorithmScore,
            currentPenalty = state.displayPenalty
        )
        
        // 计算最终分数
        val finalScore = (algorithmScore - state.displayPenalty + recovery).coerceAtLeast(0f)
        
        return DisplayScoreResult(
            finalScore = finalScore,
            algorithmScore = algorithmScore,
            penalty = state.displayPenalty,
            recovery = recovery,
            browseDistance = browseDistance,
            requiredDistance = requiredDistance
        )
    }
    
    /**
     * 记录单个物品展示，应用惩罚
     */
    fun recordDisplay(itemId: Long) {
        val state = getOrCreateDisplayState(itemId)
        
        // 计算展示惩罚（渐进式惩罚）
        val penalty = calculateDisplayPenalty(state.displayCount)
        
        // 应用惩罚
        state.displayPenalty += penalty
        state.lastDisplayPosition = globalDisplayPosition
        state.displayCount++
        state.totalDisplayCount++
        
        // 更新全局展示位置
        globalDisplayPosition++
        
        // 定期清理，防止数值过大
        periodicCleanup()
    }
    
    /**
     * 批量记录多个物品展示
     */
    fun recordBatchDisplay(itemIds: List<Long>) {
        itemIds.forEach { itemId ->
            recordDisplay(itemId)
        }
    }
    
    /**
     * 计算展示惩罚分数
     */
    private fun calculateDisplayPenalty(currentDisplayCount: Int): Float {
        return when {
            currentDisplayCount == 0 -> 0.5f    // 第一次展示后，减0.5分
            currentDisplayCount == 1 -> 0.3f    // 第二次展示后，减0.3分  
            currentDisplayCount == 2 -> 0.25f   // 第三次展示后，减0.25分
            currentDisplayCount == 3 -> 0.2f    // 第四次展示后，减0.2分
            currentDisplayCount >= 4 -> 0.15f   // 之后每次减0.15分
            else -> 0.1f                        // 保底惩罚
        }
    }
    
    /**
     * 获取或创建物品展示状态
     */
    private fun getOrCreateDisplayState(itemId: Long): ItemDisplayState {
        return displayStates.getOrPut(itemId) { ItemDisplayState(itemId) }
    }
    
    /**
     * 获取物品的浏览距离
     */
    fun getBrowseDistance(itemId: Long): Int {
        val state = displayStates[itemId] ?: return Int.MAX_VALUE
        return globalDisplayPosition - state.lastDisplayPosition
    }
    
    /**
     * 获取物品的展示状态信息
     */
    fun getDisplayState(itemId: Long): ItemDisplayState? {
        return displayStates[itemId]
    }
    
    /**
     * 获取所有物品的展示统计
     */
    fun getDisplayStatistics(): Map<String, Any> {
        val states = displayStates.values
        
        return mapOf(
            "totalItems" to states.size,
            "totalDisplays" to states.sumOf { it.totalDisplayCount },
            "averagePenalty" to if (states.isNotEmpty()) states.map { it.displayPenalty }.average() else 0.0,
            "maxPenalty" to (states.maxOfOrNull { it.displayPenalty } ?: 0f),
            "globalPosition" to globalDisplayPosition,
            "itemsWithPenalty" to states.count { it.displayPenalty > 0f }
        )
    }
    
    /**
     * 重置特定物品的展示状态
     */
    fun resetItemState(itemId: Long) {
        displayStates[itemId]?.let { state ->
            state.displayPenalty = 0f
            state.displayCount = 0
            state.lastDisplayPosition = 0
        }
    }
    
    /**
     * 重置所有物品的展示状态
     */
    fun resetAllStates() {
        displayStates.clear()
        globalDisplayPosition = 0
    }
    
    /**
     * 部分重置展示状态（保留一定历史）
     */
    fun partialReset(keepRecentDisplays: Int = 50) {
        val recentThreshold = globalDisplayPosition - keepRecentDisplays
        
        displayStates.values.forEach { state ->
            // 如果很久没展示了，减少惩罚
            if (state.lastDisplayPosition < recentThreshold) {
                state.displayPenalty *= 0.5f // 减半惩罚
            }
            
            // 重置展示计数，但保留惩罚分数
            if (state.displayCount > 3) {
                state.displayCount = 1 // 重置为少量展示
            }
        }
    }
    
    /**
     * 定期清理，防止数值过大
     */
    private fun periodicCleanup() {
        // 当全局位置超过10000时，进行相对位置重置
        if (globalDisplayPosition > 10000) {
            val minPosition = displayStates.values.minOfOrNull { it.lastDisplayPosition } ?: 0
            
            // 所有记录相对位置保持不变，但重置基准点
            displayStates.values.forEach { state ->
                state.lastDisplayPosition -= minPosition
            }
            
            globalDisplayPosition -= minPosition
        }
        
        // 清理长时间未使用的状态（可选）
        val cleanupThreshold = globalDisplayPosition - 1000
        displayStates.entries.removeAll { (_, state) ->
            state.lastDisplayPosition < cleanupThreshold && state.displayPenalty <= 0.1f
        }
    }
    
    /**
     * 获取调试信息
     */
    fun getDebugInfo(itemId: Long, item: Item, algorithmScore: Float): String {
        val result = calculateDisplayScoreDetailed(item, algorithmScore)
        val state = getDisplayState(itemId)
        
        return """
            === 物品展示调试信息 ===
            物品: ${item.name}
            算法分数: %.3f
            最终分数: %.3f
            当前惩罚: %.3f
            恢复分数: %.3f
            浏览距离: ${result.browseDistance}
            需要距离: ${result.requiredDistance}
            展示次数: ${state?.displayCount ?: 0}
            总展示次数: ${state?.totalDisplayCount ?: 0}
            全局位置: $globalDisplayPosition
            最后展示位置: ${state?.lastDisplayPosition ?: 0}
        """.trimIndent().format(
            result.algorithmScore,
            result.finalScore,
            result.penalty,
            result.recovery
        )
    }
}

