package com.example.itemmanagement.ui.feed

import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.model.*
import kotlinx.coroutines.flow.first
import kotlin.random.Random

/**
 * 智能信息流算法 - 整合所有算法组件的主协调器
 */
class SmartFeedAlgorithm(private val repository: ItemRepository) {
    
    /**
     * 评分后的物品
     */
    data class ScoredItem(
        val item: Item,
        val algorithmScore: Float,      // 原始算法分数
        val displayScore: Float,        // 最终展示分数（含惩罚和恢复）
        val browseDistance: Int,        // 浏览距离
        val displayState: AdaptiveDisplayManager.ItemDisplayState? = null
    )
    
    /**
     * 最终展示的物品
     */
    data class DisplayItem(
        val item: Item,
        val showReason: Boolean,        // 是否显示推荐理由
        val reasonText: String?,        // 推荐理由文本
        val reasonType: FeedType?,      // 推荐理由类型
        val algorithmScore: Float,      // 算法分数（调试用）
        val displayScore: Float         // 展示分数（调试用）
    )
    
    // 算法组件
    private val itemScorer = ItemScorer(repository)
    private val displayManager = AdaptiveDisplayManager()
    private val antiLoopManager = AntiLoopManager()
    
    /**
     * 生成信息流主算法
     */
    suspend fun generateFeed(count: Int = 10): List<DisplayItem> {
        val allItems = repository.getAllItems().first()
        
        if (allItems.isEmpty()) {
            return emptyList()
        }
        
        // 1. 为每个物品计算展示分数
        val itemsWithScores = calculateAllItemScores(allItems)
        
        // 2. 按展示分数排序，并应用智能洗牌避免固定顺序
        val scoredPairs = itemsWithScores.map { it.item.id to it.displayScore }
        val shuffledPairs = antiLoopManager.intelligentShuffle(scoredPairs)
        val rankedItems = shuffledPairs.map { (itemId, _) ->
            itemsWithScores.first { it.item.id == itemId }
        }
        
        // 3. 应用9:1全覆盖规则
        val selectedItems = applyFullCoverageRule(rankedItems, count)
        
        // 4. 应用1/10推荐理由规则
        val itemsWithReasons = applyReasonDisplayRule(selectedItems)
        
        // 5. 记录展示历史（同时更新反循环管理器）
        selectedItems.forEach { 
            displayManager.recordDisplay(it.item.id)
            antiLoopManager.recordDisplay(it.item.id)
        }
        
        return itemsWithReasons
    }
    
    /**
     * 为所有物品计算展示分数（包含反循环机制）
     */
    private suspend fun calculateAllItemScores(allItems: List<Item>): List<ScoredItem> {
        return allItems.map { item ->
            val algorithmScore = itemScorer.calculateItemScore(item)
            val displayScoreResult = displayManager.calculateDisplayScoreDetailed(item, algorithmScore)
            
            // 应用反循环机制
            val antiLoopScore = antiLoopManager.applyAntiLoopMechanisms(
                itemId = item.id,
                baseScore = displayScoreResult.finalScore,
                algorithmScore = algorithmScore
            )
            
            val browseDistance = displayManager.getBrowseDistance(item.id)
            val displayState = displayManager.getDisplayState(item.id)
            
            ScoredItem(
                item = item,
                algorithmScore = algorithmScore,
                displayScore = antiLoopScore,  // 使用应用反循环后的分数
                browseDistance = browseDistance,
                displayState = displayState
            )
        }
    }
    
    /**
     * 应用9:1全覆盖规则 - 确保低分物品也有展示机会
     */
    private fun applyFullCoverageRule(rankedItems: List<ScoredItem>, count: Int): List<ScoredItem> {
        if (rankedItems.size <= count) {
            return rankedItems // 物品总数不够，全部返回
        }
        
        val scoreThreshold = 0.3f
        val highScoreItems = rankedItems.filter { it.displayScore >= scoreThreshold }
        val lowScoreItems = rankedItems.filter { it.displayScore < scoreThreshold }
        
        // 如果没有低分物品，按排序取前N个
        if (lowScoreItems.isEmpty()) {
            return rankedItems.take(count)
        }
        
        // 如果全都是低分物品，按排序取前N个
        if (highScoreItems.isEmpty()) {
            return rankedItems.take(count)
        }
        
        // 应用9:1比例分配
        val highScoreCount = (count * 0.9).toInt()
        val lowScoreCount = count - highScoreCount
        
        val selectedHighScore = highScoreItems.take(highScoreCount)
        val selectedLowScore = lowScoreItems.take(lowScoreCount)
        
        // 混合并随机排序，避免明显的分层
        return (selectedHighScore + selectedLowScore).shuffled()
    }
    
    /**
     * 应用严格1/10推荐理由规则 - 每个物品有10%的独立概率显示推荐理由
     */
    private fun applyReasonDisplayRule(items: List<ScoredItem>): List<DisplayItem> {
        val result = mutableListOf<DisplayItem>()
        
        // 为每个物品独立计算是否显示推荐理由（10%概率）
        items.forEach { scoredItem ->
            val showReason = Random.nextFloat() < 0.1f // 10%概率显示推荐理由
            val reasonType = if (showReason) getRandomReasonType() else null
            val reasonText = if (showReason) {
                generateReasonText(scoredItem, reasonType!!)
            } else null
            
            result.add(DisplayItem(
                item = scoredItem.item,
                showReason = showReason,
                reasonText = reasonText,
                reasonType = reasonType,
                algorithmScore = scoredItem.algorithmScore,
                displayScore = scoredItem.displayScore
            ))
        }
        
        return result
    }
    
    /**
     * 随机选择推荐理由类型
     */
    private fun getRandomReasonType(): FeedType {
        val types = listOf(
            FeedType.MEMORY,     // 记忆类推荐
            FeedType.INSIGHT,    // 洞察类推荐
            FeedType.DISCOVERY,  // 发现类推荐
            FeedType.SUGGESTION  // 建议类推荐
        )
        return types.random()
    }
    
    /**
     * 根据推荐类型生成推荐理由文本
     */
    private fun generateReasonText(scoredItem: ScoredItem, reasonType: FeedType): String {
        val item = scoredItem.item
        
        return when (reasonType) {
            FeedType.MEMORY -> generateMemoryReason(item)
            FeedType.INSIGHT -> generateInsightReason(item)
            FeedType.DISCOVERY -> generateDiscoveryReason(item)
            FeedType.SUGGESTION -> generateSuggestionReason(item)
        }
    }
    
    /**
     * 生成记忆类推荐理由
     */
    private fun generateMemoryReason(item: Item): String {
        val daysSinceAdded = (System.currentTimeMillis() - item.addDate.time) / (24 * 60 * 60 * 1000)
        
        return when {
            item.openStatus == OpenStatus.UNOPENED -> "还没有开封过，要不要试试？"
            daysSinceAdded > 90 -> "已经${daysSinceAdded}天了，还记得当初为什么买它吗？"
            daysSinceAdded > 30 -> "好久没关注了，检查一下状态吧"
            item.price != null && item.price >= 500 -> "这可是个贵重物品，值得关注"
            else -> "想起这个物品了吗？"
        }
    }
    
    /**
     * 生成洞察类推荐理由
     */
    private fun generateInsightReason(item: Item): String {
        return when {
            !item.brand.isNullOrBlank() -> "你似乎很喜欢${item.brand}这个品牌"
            item.category.isNotBlank() -> "这是你在${item.category}分类中的收藏"
            item.price != null && item.price >= 1000 -> "高价值物品，值得好好保养"
            else -> "这个物品很有特色"
        }
    }
    
    /**
     * 生成发现类推荐理由
     */
    private fun generateDiscoveryReason(item: Item): String {
        return when {
            item.location != null -> "在${item.location.area}发现的宝贝"
            !item.brand.isNullOrBlank() -> "与你的${item.brand}产品很配"
            item.tags.isNotEmpty() -> "标签显示这很${item.tags.first().name}"
            else -> "这个发现很有趣"
        }
    }
    
    /**
     * 生成建议类推荐理由
     */
    private fun generateSuggestionReason(item: Item): String {
        return when {
            item.status == ItemStatus.EXPIRED -> "已过期，建议处理一下"
            item.status == ItemStatus.USED_UP -> "用完了，需要买新的吗？"
            item.status == ItemStatus.DISCARDED -> "已丢弃，可以清理记录了"
            item.status == ItemStatus.GIVEN_AWAY -> "已转赠，记录一下去向吧"
            item.openStatus == OpenStatus.UNOPENED -> "建议开封使用"
            else -> "可能需要关注一下"
        }
    }
    
    /**
     * 获取算法统计信息
     */
    suspend fun getAlgorithmStatistics(): Map<String, Any> {
        val allItems = repository.getAllItems().first()
        val displayStats = displayManager.getDisplayStatistics()
        val antiLoopStats = antiLoopManager.getStatusInfo()
        
        val scoredItems = calculateAllItemScores(allItems)
        val averageAlgorithmScore = scoredItems.map { it.algorithmScore }.average()
        val averageDisplayScore = scoredItems.map { it.displayScore }.average()
        
        return mapOf(
            "totalItems" to allItems.size,
            "averageAlgorithmScore" to averageAlgorithmScore,
            "averageDisplayScore" to averageDisplayScore,
            "displayManager" to displayStats,
            "antiLoopManager" to antiLoopStats,
            "highScoreItems" to scoredItems.count { it.displayScore >= 0.3f },
            "lowScoreItems" to scoredItems.count { it.displayScore < 0.3f }
        )
    }
    
    /**
     * 重置算法状态（用于测试或重新开始）
     */
    fun resetAlgorithmState() {
        displayManager.resetAllStates()
        antiLoopManager.reset()
    }
    
    /**
     * 获取物品的详细调试信息
     */
    suspend fun getItemDebugInfo(itemId: Long): String? {
        val allItems = repository.getAllItems().first()
        val item = allItems.find { it.id == itemId } ?: return null
        
        val algorithmScore = itemScorer.calculateItemScore(item)
        return displayManager.getDebugInfo(itemId, item, algorithmScore)
    }
}
