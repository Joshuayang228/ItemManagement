package com.example.itemmanagement.ui.feed

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * 流畅滚动管理器 - 实现自然渐进式无限刷新
 */
class SmoothScrollManager {
    
    /**
     * 加载状态
     */
    sealed class LoadingState {
        object Idle : LoadingState()
        object Loading : LoadingState()
        object PreLoading : LoadingState()  // 预加载状态
    }
    
    /**
     * 分批配置 - 优化为更快的加载体验
     */
    data class BatchConfig(
        val batchSize: Int = 6,           // 每批加载数量（增加到6个）
        val preLoadThreshold: Int = 15,   // 预加载阈值（距底部15个时开始）
        val maxBatchInterval: Long = 80L, // 批次间最大间隔（减少到80ms）
        val minBatchInterval: Long = 40L  // 批次间最小间隔（减少到40ms）
    )
    
    private val config = BatchConfig()
    private var currentState: LoadingState = LoadingState.Idle
    private val loadingQueue = mutableListOf<suspend () -> Unit>()
    
    /**
     * 流畅加载更多内容
     */
    suspend fun loadMoreSmoothly(
        currentItemCount: Int,
        totalNeeded: Int,
        onBatchLoaded: suspend (items: List<Any>) -> Unit,
        itemGenerator: suspend (count: Int) -> List<Any>
    ) {
        if (currentState is LoadingState.Loading) return
        
        currentState = LoadingState.Loading
        
        try {
            val remainingCount = totalNeeded - currentItemCount
            if (remainingCount <= 0) {
                currentState = LoadingState.Idle
                return
            }
            
            // 分批加载
            loadInBatches(remainingCount, onBatchLoaded, itemGenerator)
            
        } finally {
            currentState = LoadingState.Idle
        }
    }
    
    /**
     * 分批渐进式加载
     */
    private suspend fun loadInBatches(
        totalCount: Int,
        onBatchLoaded: suspend (items: List<Any>) -> Unit,
        itemGenerator: suspend (count: Int) -> List<Any>
    ) = withContext(Dispatchers.Default) {
        
        var remainingCount = totalCount
        var loadedCount = 0
        
        while (remainingCount > 0) {
            val batchSize = minOf(config.batchSize, remainingCount)
            
            // 生成这一批的数据
            val batchItems = itemGenerator(batchSize)
            
            // 在主线程更新UI
            withContext(Dispatchers.Main) {
                onBatchLoaded(batchItems)
            }
            
            remainingCount -= batchSize
            loadedCount += batchSize
            
            // 如果还有更多数据，等待一段时间再加载下一批
            if (remainingCount > 0) {
                delay(calculateBatchDelay(loadedCount))
            }
        }
    }
    
    /**
     * 计算批次间延迟时间 - 优化为更快的加载速度
     */
    private fun calculateBatchDelay(loadedCount: Int): Long {
        // 随着加载进度增加，延迟时间减少（加速感）
        val progress = minOf(loadedCount / 12f, 1f) // 12个物品后达到最快速度（从20个降低）
        val delayRange = config.maxBatchInterval - config.minBatchInterval
        return config.minBatchInterval + (delayRange * (1f - progress)).toLong()
    }
    
    /**
     * 检查是否应该预加载
     */
    fun shouldPreLoad(
        visibleItemCount: Int,
        totalItemCount: Int,
        firstVisibleItem: Int
    ): Boolean {
        if (currentState is LoadingState.Loading) return false
        
        val itemsFromBottom = totalItemCount - (firstVisibleItem + visibleItemCount)
        return itemsFromBottom <= config.preLoadThreshold
    }
    
    /**
     * 启动预加载
     */
    fun startPreLoading() {
        if (currentState is LoadingState.Idle) {
            currentState = LoadingState.PreLoading
        }
    }
    
    /**
     * 获取当前加载状态
     */
    fun getCurrentState(): LoadingState = currentState
    
    /**
     * 重置状态
     */
    fun reset() {
        currentState = LoadingState.Idle
        loadingQueue.clear()
    }
}
