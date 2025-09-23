package com.example.itemmanagement.ui.utils

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/**
 * Material 3性能优化工具类
 * 提供统一的性能优化方法，提升应用流畅度
 */
object Material3Performance {

    /**
     * 优化RecyclerView性能
     * @param recyclerView 要优化的RecyclerView
     */
    fun optimizeRecyclerView(recyclerView: RecyclerView) {
        recyclerView.apply {
            // 启用硬件加速
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // 设置固定大小以避免重新测量
            setHasFixedSize(true)
            
            // 优化滚动性能
            isNestedScrollingEnabled = false
            
            // 启用缓存
            setItemViewCacheSize(20)
            
            // 预加载
            recycledViewPool.setMaxRecycledViews(0, 10)
        }
    }

    /**
     * 优化MaterialCardView性能
     * @param cardView 要优化的MaterialCardView
     */
    fun optimizeCardView(cardView: MaterialCardView) {
        cardView.apply {
            // 启用硬件加速
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // 优化阴影渲染
            useCompatPadding = true
            
            // 缓存阴影
            setWillNotDraw(false)
        }
    }

    /**
     * 批量优化ViewGroup下的所有MaterialCardView
     * @param viewGroup 父ViewGroup
     */
    fun optimizeCardViewsInGroup(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            when (child) {
                is MaterialCardView -> optimizeCardView(child)
                is ViewGroup -> optimizeCardViewsInGroup(child)
            }
        }
    }

    /**
     * 优化布局层次，减少过度绘制
     * @param view 要优化的视图
     */
    fun optimizeLayoutHierarchy(view: View) {
        view.apply {
            // 启用裁剪以避免过度绘制
            clipToOutline = true
            
            // 优化绘制
            setWillNotDraw(true)
            
            // 硬件加速
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
    }

    /**
     * 内存优化 - 清理视图缓存
     * @param recyclerView RecyclerView实例
     */
    fun clearViewCache(recyclerView: RecyclerView) {
        recyclerView.recycledViewPool.clear()
    }

    /**
     * 启用视图回收优化
     * @param recyclerView RecyclerView实例
     */
    fun enableViewRecycling(recyclerView: RecyclerView) {
        recyclerView.apply {
            // 多种view type的缓存优化
            recycledViewPool.setMaxRecycledViews(0, 15) // Header类型
            recycledViewPool.setMaxRecycledViews(1, 20) // 普通Item类型
            recycledViewPool.setMaxRecycledViews(2, 10) // Footer类型
        }
    }

    /**
     * Material 3动画性能优化
     * @param view 应用动画的视图
     */
    fun optimizeAnimationPerformance(view: View) {
        view.apply {
            // 启用硬件加速以获得更好的动画性能
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            
            // 优化动画缓存 (使用现代API)
            isDrawingCacheEnabled = true
        }
    }
}
