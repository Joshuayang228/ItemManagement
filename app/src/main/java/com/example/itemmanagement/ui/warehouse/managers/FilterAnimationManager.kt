package com.example.itemmanagement.ui.warehouse.managers

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.example.itemmanagement.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.util.concurrent.ConcurrentHashMap

/**
 * 筛选动画管理器
 * 
 * 功能：
 * 1. 统一管理ChipGroup展开/收起动画
 * 2. 处理状态切换动画
 * 3. 优化动画性能
 * 4. 提供流畅的用户体验
 */
class FilterAnimationManager {
    
    // 正在进行的动画集合，用于取消和管理
    private val runningAnimations = ConcurrentHashMap<String, ValueAnimator>()
    
    // 动画配置
    companion object {
        private const val EXPAND_DURATION = 300L
        private const val COLLAPSE_DURATION = 250L
        private const val FADE_DURATION = 150L
        private const val MAX_LINES = 3
    }
    
    /**
     * 设置ChipGroup的展开/收起功能
     * 
     * @param chipGroup 目标ChipGroup
     * @param expandButton 展开按钮
     * @param animationKey 动画唯一标识符
     */
    fun setupChipGroupExpandCollapse(
        chipGroup: ChipGroup,
        expandButton: Chip,
        animationKey: String
    ) {
        // 延迟到布局完成后执行
        chipGroup.post {
            val shouldShowExpandButton = checkIfNeedsExpandButton(chipGroup)
            
            if (shouldShowExpandButton) {
                val maxHeight = calculateMaxHeight(chipGroup)
                
                // 显示展开按钮
                expandButton.visibility = View.VISIBLE
                
                // 设置初始折叠状态
                chipGroup.layoutParams.height = maxHeight
                chipGroup.requestLayout()
                
                // 设置点击监听器
                setupExpandButtonClick(chipGroup, expandButton, maxHeight, animationKey)
            } else {
                expandButton.visibility = View.GONE
            }
        }
    }
    
    /**
     * 检查是否需要展开按钮
     */
    private fun checkIfNeedsExpandButton(chipGroup: ChipGroup): Boolean {
        if (chipGroup.childCount == 0) return false
        
        val chipHeight = getChipHeight(chipGroup)
        val maxHeight = chipHeight * MAX_LINES
        
        return chipGroup.height > maxHeight
    }
    
    /**
     * 计算ChipGroup的最大折叠高度
     */
    private fun calculateMaxHeight(chipGroup: ChipGroup): Int {
        val chipHeight = getChipHeight(chipGroup)
        return chipHeight * MAX_LINES
    }
    
    /**
     * 获取Chip的高度（包括间距）
     */
    private fun getChipHeight(chipGroup: ChipGroup): Int {
        return if (chipGroup.childCount > 0) {
            val firstChip = chipGroup.getChildAt(0)
            firstChip.height + chipGroup.chipSpacingVertical
        } else {
            0
        }
    }
    
    /**
     * 设置展开按钮的点击监听器
     */
    private fun setupExpandButtonClick(
        chipGroup: ChipGroup,
        expandButton: Chip,
        maxHeight: Int,
        animationKey: String
    ) {
        var isExpanded = false
        
        expandButton.setOnClickListener {
            // 取消之前的动画
            cancelAnimation(animationKey)
            
            if (isExpanded) {
                // 收起动画
                animateCollapse(chipGroup, expandButton, maxHeight, animationKey) {
                    isExpanded = false
                }
            } else {
                // 展开动画
                animateExpand(chipGroup, expandButton, animationKey) {
                    isExpanded = true
                }
            }
        }
    }
    
    /**
     * 展开动画
     */
    private fun animateExpand(
        chipGroup: ChipGroup,
        expandButton: Chip,
        animationKey: String,
        onAnimationEnd: () -> Unit
    ) {
        val currentHeight = chipGroup.height
        val targetHeight = measureWrapContentHeight(chipGroup)
        
        val animator = ValueAnimator.ofInt(currentHeight, targetHeight).apply {
            duration = EXPAND_DURATION
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                chipGroup.layoutParams.height = animator.animatedValue as Int
                chipGroup.requestLayout()
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    updateExpandButtonState(expandButton, true)
                    onAnimationEnd()
                    runningAnimations.remove(animationKey)
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    runningAnimations.remove(animationKey)
                }
            })
        }
        
        runningAnimations[animationKey] = animator
        animator.start()
    }
    
    /**
     * 收起动画
     */
    private fun animateCollapse(
        chipGroup: ChipGroup,
        expandButton: Chip,
        maxHeight: Int,
        animationKey: String,
        onAnimationEnd: () -> Unit
    ) {
        val currentHeight = chipGroup.height
        
        val animator = ValueAnimator.ofInt(currentHeight, maxHeight).apply {
            duration = COLLAPSE_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { animator ->
                chipGroup.layoutParams.height = animator.animatedValue as Int
                chipGroup.requestLayout()
            }
            
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    updateExpandButtonState(expandButton, false)
                    onAnimationEnd()
                    runningAnimations.remove(animationKey)
                }
                
                override fun onAnimationCancel(animation: Animator) {
                    runningAnimations.remove(animationKey)
                }
            })
        }
        
        runningAnimations[animationKey] = animator
        animator.start()
    }
    
    /**
     * 测量ChipGroup的wrap_content高度
     */
    private fun measureWrapContentHeight(chipGroup: ChipGroup): Int {
        val widthSpec = View.MeasureSpec.makeMeasureSpec(chipGroup.width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        
        chipGroup.measure(widthSpec, heightSpec)
        return chipGroup.measuredHeight
    }
    
    /**
     * 更新展开按钮状态
     */
    private fun updateExpandButtonState(expandButton: Chip, isExpanded: Boolean) {
        if (isExpanded) {
            expandButton.text = "收起"
            expandButton.chipIcon = ContextCompat.getDrawable(
                expandButton.context, 
                R.drawable.ic_expand_less
            )
        } else {
            expandButton.text = "展示全部"
            expandButton.chipIcon = ContextCompat.getDrawable(
                expandButton.context, 
                R.drawable.ic_expand_more
            )
        }
    }
    
    /**
     * 视图淡入动画
     */
    fun fadeIn(view: View, duration: Long = FADE_DURATION) {
        view.alpha = 0f
        view.visibility = View.VISIBLE
        view.animate()
            .alpha(1f)
            .setDuration(duration)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }
    
    /**
     * 视图淡出动画
     */
    fun fadeOut(view: View, duration: Long = FADE_DURATION, hideAfterAnimation: Boolean = true) {
        view.animate()
            .alpha(0f)
            .setDuration(duration)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                if (hideAfterAnimation) {
                    view.visibility = View.GONE
                }
            }
            .start()
    }
    
    /**
     * 高度变化动画
     */
    fun animateHeightChange(
        view: View,
        targetHeight: Int,
        duration: Long = EXPAND_DURATION,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        val currentHeight = view.height
        
        ValueAnimator.ofInt(currentHeight, targetHeight).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                view.layoutParams.height = animator.animatedValue as Int
                view.requestLayout()
            }
            
            if (onAnimationEnd != null) {
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        onAnimationEnd()
                    }
                })
            }
        }.start()
    }
    
    /**
     * 批量展开多个ChipGroup
     */
    fun expandAllChipGroups(chipGroupsWithButtons: List<Pair<ChipGroup, Chip>>) {
        chipGroupsWithButtons.forEachIndexed { index, (chipGroup, expandButton) ->
            val animationKey = "batch_expand_$index"
            
            // 延迟执行，创建瀑布效果
            expandButton.postDelayed({
                if (checkIfNeedsExpandButton(chipGroup)) {
                    animateExpand(chipGroup, expandButton, animationKey) {}
                }
            }, index * 50L) // 50ms间隔
        }
    }
    
    /**
     * 批量收起多个ChipGroup
     */
    fun collapseAllChipGroups(chipGroupsWithButtons: List<Pair<ChipGroup, Chip>>) {
        chipGroupsWithButtons.forEachIndexed { index, (chipGroup, expandButton) ->
            val animationKey = "batch_collapse_$index"
            val maxHeight = calculateMaxHeight(chipGroup)
            
            // 延迟执行，创建瀑布效果
            expandButton.postDelayed({
                animateCollapse(chipGroup, expandButton, maxHeight, animationKey) {}
            }, index * 50L) // 50ms间隔
        }
    }
    
    /**
     * 取消指定的动画
     */
    private fun cancelAnimation(animationKey: String) {
        runningAnimations[animationKey]?.let { animator ->
            if (animator.isRunning) {
                animator.cancel()
            }
            runningAnimations.remove(animationKey)
        }
    }
    
    /**
     * 取消所有正在进行的动画
     */
    fun cancelAllAnimations() {
        runningAnimations.values.forEach { animator ->
            if (animator.isRunning) {
                animator.cancel()
            }
        }
        runningAnimations.clear()
    }
    
    /**
     * 获取正在运行的动画数量
     */
    fun getRunningAnimationCount(): Int {
        return runningAnimations.size
    }
    
    /**
     * 检查是否有动画正在运行
     */
    fun hasRunningAnimations(): Boolean {
        return runningAnimations.isNotEmpty()
    }
    
    /**
     * 等待所有动画完成
     */
    suspend fun waitForAnimationsToComplete() {
        while (hasRunningAnimations()) {
            kotlinx.coroutines.delay(50)
        }
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        cancelAllAnimations()
    }
}
