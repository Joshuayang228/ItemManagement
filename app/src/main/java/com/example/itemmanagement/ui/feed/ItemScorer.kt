package com.example.itemmanagement.ui.feed

import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.model.*
import kotlinx.coroutines.flow.first
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min

/**
 * 物品评分器 - 为每个物品计算多维度基础分数
 */
class ItemScorer(private val repository: ItemRepository) {
    
    /**
     * 为物品计算综合基础分数 (0-1.0)
     */
    suspend fun calculateItemScore(item: Item): Float {
        val memoryScore = calculateMemoryScore(item)         // 记忆维度 (30%)
        val insightScore = calculateInsightScore(item)       // 洞察维度 (25%)
        val relationshipScore = calculateRelationshipScore(item) // 关系维度 (25%)
        val needsScore = calculateNeedsScore(item)           // 需求维度 (20%)
        
        return memoryScore * 0.3f + 
               insightScore * 0.25f + 
               relationshipScore * 0.25f + 
               needsScore * 0.2f
    }
    
    /**
     * 计算记忆维度分数
     * 考虑：遗忘度、价值度、开封状态、位置偏僻度
     */
    private fun calculateMemoryScore(item: Item): Float {
        var score = 0f
        
        // 1. 遗忘度评分：时间越久分数越高 (0-0.4)
        val daysSinceAdded = (System.currentTimeMillis() - item.addDate.time) / (24 * 60 * 60 * 1000)
        score += when {
            daysSinceAdded > 180 -> 0.4f   // 半年以上
            daysSinceAdded > 90  -> 0.35f  // 3-6个月
            daysSinceAdded > 30  -> 0.25f  // 1-3个月  
            daysSinceAdded > 7   -> 0.15f  // 1周-1个月
            else -> 0.05f                  // 1周内
        }
        
        // 2. 价值度评分：价格越高分数越高 (0-0.3)
        item.price?.let { price ->
            score += when {
                price >= 2000 -> 0.3f
                price >= 1000 -> 0.25f
                price >= 500  -> 0.2f
                price >= 200  -> 0.15f
                price >= 100  -> 0.1f
                price >= 50   -> 0.05f
                else -> 0.02f
            }
        } ?: run { score += 0.1f } // 无价格信息时给予中等分数
        
        // 3. 开封状态评分 (0-0.3)
        score += when(item.openStatus) {
            OpenStatus.UNOPENED -> 0.3f   // 未开封很重要，提醒用户
            OpenStatus.OPENED -> 0.1f     // 已开封一般关注
            else -> 0.02f                 // 其他状态基本不重要
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * 计算洞察维度分数
     * 考虑：品牌偏好、分类偏好、消费趋势
     */
    private suspend fun calculateInsightScore(item: Item): Float {
        val allItems = repository.getAllItems().first()
        var score = 0f
        
        // 1. 品牌偏好分析 (0-0.4)
        if (!item.brand.isNullOrBlank()) {
            val sameBrandItems = allItems.filter { 
                !it.brand.isNullOrBlank() && it.brand.equals(item.brand, ignoreCase = true)
            }
            val brandCount = sameBrandItems.size
            
            score += when {
                brandCount >= 5 -> 0.4f   // 品牌收藏家
                brandCount >= 3 -> 0.3f   // 品牌偏好明显
                brandCount >= 2 -> 0.2f   // 有一定偏好
                else -> 0.1f              // 独一无二
            }
        }
        
        // 2. 分类偏好分析 (0-0.3)
        val sameCategoryItems = allItems.filter { 
            it.category.equals(item.category, ignoreCase = true)
        }
        val categoryRatio = sameCategoryItems.size.toFloat() / allItems.size.toFloat()
        
        score += when {
            categoryRatio >= 0.3f -> 0.3f  // 主要分类
            categoryRatio >= 0.2f -> 0.2f  // 重要分类
            categoryRatio >= 0.1f -> 0.15f // 一般分类
            else -> 0.05f                  // 小众分类
        }
        
        // 3. 价格档次分析 (0-0.3)
        item.price?.let { price ->
            val averagePrice = allItems.mapNotNull { it.price }.average()
            val priceRatio = price / averagePrice
            
            score += when {
                priceRatio >= 2.0 -> 0.3f   // 高端产品
                priceRatio >= 1.5 -> 0.25f  // 中高端产品
                priceRatio >= 0.8 -> 0.2f   // 中等产品
                priceRatio >= 0.5 -> 0.15f  // 中低端产品
                else -> 0.1f                // 低端产品
            }
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * 计算关系维度分数
     * 考虑：配套关系、位置关联、使用关联
     */
    private suspend fun calculateRelationshipScore(item: Item): Float {
        val allItems = repository.getAllItems().first()
        var score = 0f
        
        // 1. 位置关联分析 (0-0.4)
        item.location?.let { location ->
            val sameLocationItems = allItems.filter { 
                it.location?.area?.equals(location.area, ignoreCase = true) == true
            }
            val locationDensity = sameLocationItems.size
            
            score += when {
                locationDensity >= 10 -> 0.4f  // 热点位置
                locationDensity >= 5  -> 0.3f  // 常用位置
                locationDensity >= 2  -> 0.2f  // 一般位置
                else -> 0.1f                   // 独立位置
            }
        }
        
        // 2. 配套关系分析 (0-0.3)
        val relatedKeywords = getRelatedKeywords(item)
        val relatedItems = allItems.filter { otherItem ->
            otherItem.id != item.id && hasRelationship(item, otherItem, relatedKeywords)
        }
        
        score += min(relatedItems.size * 0.05f, 0.3f)
        
        // 3. 标签相似度分析 (0-0.3)
        if (item.tags.isNotEmpty()) {
            val tagMatchCount = allItems.sumOf { otherItem ->
                if (otherItem.id != item.id) {
                    item.tags.intersect(otherItem.tags.toSet()).size
                } else 0
            }
            
            score += min(tagMatchCount * 0.02f, 0.3f)
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * 计算需求维度分数
     * 考虑：补货需求、配套需求、升级需求
     */
    private suspend fun calculateNeedsScore(item: Item): Float {
        var score = 0f
        
        // 1. 状态紧急度 (0-0.4)
        score += when(item.status) {
            ItemStatus.EXPIRED -> 0.4f     // 过期了，急需处理
            ItemStatus.USED_UP -> 0.3f     // 用完了，考虑补货
            ItemStatus.IN_STOCK -> 0.1f    // 在库正常状态
            ItemStatus.GIVEN_AWAY -> 0.05f // 已转赠，较低关注
            ItemStatus.DISCARDED -> 0.02f  // 已丢弃，最低关注
        }
        
        // 2. 开封状态需求 (0-0.3)
        score += when(item.openStatus) {
            OpenStatus.UNOPENED -> 0.3f    // 未开封，提醒使用
            OpenStatus.OPENED -> 0.15f     // 已开封，正常关注
            else -> 0.1f                   // 其他状态
        }
        
        // 3. 价值保护需求 (0-0.3)
        item.price?.let { price ->
            if (price >= 500) {
                score += 0.3f  // 贵重物品需要更多关注
            } else if (price >= 200) {
                score += 0.2f  // 中等价值物品
            } else if (price >= 50) {
                score += 0.1f  // 一般价值物品
            }
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * 获取物品相关关键词
     */
    private fun getRelatedKeywords(item: Item): Set<String> {
        val keywords = mutableSetOf<String>()
        
        // 从名称中提取关键词
        val name = item.name.lowercase()
        keywords.addAll(name.split(" ", "-", "_"))
        
        // 从分类中提取关键词
        keywords.add(item.category.lowercase())
        
        // 从品牌中提取关键词
        item.brand?.let { keywords.add(it.lowercase()) }
        
        // 从标签中提取关键词
        item.tags.forEach { tag -> keywords.add(tag.name.lowercase()) }
        
        return keywords.filter { it.length > 1 }.toSet()
    }
    
    /**
     * 判断两个物品是否有关联关系
     */
    private fun hasRelationship(item1: Item, item2: Item, keywords1: Set<String>): Boolean {
        val keywords2 = getRelatedKeywords(item2)
        
        // 关键词重合度
        val intersection = keywords1.intersect(keywords2)
        if (intersection.size >= 2) return true
        
        // 同品牌
        if (!item1.brand.isNullOrBlank() && 
            item1.brand.equals(item2.brand, ignoreCase = true)) return true
        
        // 同分类
        if (item1.category.equals(item2.category, ignoreCase = true)) return true
        
        // 相似价格档次
        val price1 = item1.price ?: 0.0
        val price2 = item2.price ?: 0.0
        if (price1 > 0 && price2 > 0) {
            val priceRatio = max(price1 / price2, price2 / price1)
            if (priceRatio <= 2.0) return true
        }
        
        return false
    }
}
