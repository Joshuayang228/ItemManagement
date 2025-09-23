package com.example.itemmanagement.ui.utils

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller

/**
 * 自定义滑动速度控制器
 * 
 * 功能：
 * - 控制RecyclerView的滚动速度
 * - 提供更舒适的滑动体验
 * - 支持可调节的速度参数
 * 
 * @param context 上下文
 * @param speedFactor 速度因子，默认50f（数值越大越慢）
 */
class CustomSmoothScroller(
    context: Context,
    private val speedFactor: Float = 50f
) : LinearSmoothScroller(context) {

    /**
     * 计算滚动速度
     * 
     * @param displayMetrics 显示指标
     * @return 每像素的滚动时间（毫秒）
     */
    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
        // 默认是25f / densityDpi，我们调整为50f来减慢速度
        return speedFactor / displayMetrics.densityDpi
    }

    /**
     * 计算到达目标位置的时间
     * 
     * @param distance 滚动距离
     * @return 滚动时间（毫秒）
     */
    override fun calculateTimeForScrolling(distance: Int): Int {
        // 限制最大滚动时间为2秒，确保不会过慢
        return minOf(super.calculateTimeForScrolling(distance), 2000)
    }

    companion object {
        /**
         * 预设速度常量
         */
        const val SPEED_FAST = 25f      // 快速滚动
        const val SPEED_NORMAL = 50f    // 正常滚动（默认）
        const val SPEED_SLOW = 75f      // 慢速滚动
        const val SPEED_VERY_SLOW = 100f // 很慢滚动
    }
}
