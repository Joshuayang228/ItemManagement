package com.example.itemmanagement.ui.animation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

/**
 * 搜索框动画管理器
 * 提供统一的搜索框动画效果，确保首页和仓库界面的一致性
 */
object SearchBoxAnimator {

    /**
     * 搜索框进入动画
     * 从上方滑入 + 淡入效果
     */
    fun animateSearchBoxEnter(searchContainer: View, delay: Long = 0L) {
        // 初始状态：向上偏移50dp，完全透明
        searchContainer.translationY = -50f
        searchContainer.alpha = 0f
        
        // 创建滑入动画
        val slideIn = ObjectAnimator.ofFloat(searchContainer, "translationY", -50f, 0f).apply {
            duration = 350
            interpolator = DecelerateInterpolator()
        }
        
        // 创建淡入动画
        val fadeIn = ObjectAnimator.ofFloat(searchContainer, "alpha", 0f, 1f).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
        }
        
        // 组合动画
        AnimatorSet().apply {
            playTogether(slideIn, fadeIn)
            startDelay = delay
            start()
        }
    }

    /**
     * 搜索框获得焦点动画
     * 轻微放大 + 阴影增强
     */
    fun animateSearchBoxFocusGained(searchCard: MaterialCardView, context: Context) {
        // 缩放动画
        val scaleX = ObjectAnimator.ofFloat(searchCard, "scaleX", 1.0f, 1.02f)
        val scaleY = ObjectAnimator.ofFloat(searchCard, "scaleY", 1.0f, 1.02f)
        
        // 阴影增强动画
        val elevation = ObjectAnimator.ofFloat(searchCard, "cardElevation", 1f, 4f)
        
        // 边框颜色变化（模拟效果，通过stroke颜色变化）
        val primaryColor = ContextCompat.getColor(context, com.google.android.material.R.color.design_default_color_primary)
        val originalColor = ContextCompat.getColor(context, android.R.color.darker_gray)
        
        val colorAnimator = ValueAnimator.ofArgb(originalColor, primaryColor).apply {
            addUpdateListener { animator ->
                searchCard.strokeColor = animator.animatedValue as Int
            }
        }
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, elevation, colorAnimator)
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * 搜索框失去焦点动画
     * 恢复原始状态
     */
    fun animateSearchBoxFocusLost(searchCard: MaterialCardView, context: Context) {
        // 缩放动画
        val scaleX = ObjectAnimator.ofFloat(searchCard, "scaleX", 1.02f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(searchCard, "scaleY", 1.02f, 1.0f)
        
        // 阴影恢复动画
        val elevation = ObjectAnimator.ofFloat(searchCard, "cardElevation", 4f, 1f)
        
        // 边框颜色恢复
        val primaryColor = ContextCompat.getColor(context, com.google.android.material.R.color.design_default_color_primary)
        val originalColor = ContextCompat.getColor(context, android.R.color.darker_gray)
        
        val colorAnimator = ValueAnimator.ofArgb(primaryColor, originalColor).apply {
            addUpdateListener { animator ->
                searchCard.strokeColor = animator.animatedValue as Int
            }
        }
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, elevation, colorAnimator)
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * 清除按钮出现动画
     * 旋转 + 淡入 + 缩放
     */
    fun animateClearButtonShow(clearButton: ImageView) {
        if (clearButton.visibility == View.VISIBLE) return
        
        // 设置初始状态
        clearButton.alpha = 0f
        clearButton.scaleX = 0.5f
        clearButton.scaleY = 0.5f
        clearButton.rotation = -180f
        clearButton.visibility = View.VISIBLE
        
        // 创建动画
        val fadeIn = ObjectAnimator.ofFloat(clearButton, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(clearButton, "scaleX", 0.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(clearButton, "scaleY", 0.5f, 1f)
        val rotation = ObjectAnimator.ofFloat(clearButton, "rotation", -180f, 0f)
        
        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY, rotation)
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * 清除按钮消失动画
     * 旋转 + 淡出 + 缩放
     */
    fun animateClearButtonHide(clearButton: ImageView) {
        if (clearButton.visibility != View.VISIBLE) return
        
        // 创建动画
        val fadeOut = ObjectAnimator.ofFloat(clearButton, "alpha", 1f, 0f)
        val scaleX = ObjectAnimator.ofFloat(clearButton, "scaleX", 1f, 0.5f)
        val scaleY = ObjectAnimator.ofFloat(clearButton, "scaleY", 1f, 0.5f)
        val rotation = ObjectAnimator.ofFloat(clearButton, "rotation", 0f, 180f)
        
        AnimatorSet().apply {
            playTogether(fadeOut, scaleX, scaleY, rotation)
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    clearButton.visibility = View.GONE
                }
            })
            start()
        }
    }

    /**
     * 搜索框退出动画
     * 向上滑出 + 淡出效果
     */
    fun animateSearchBoxExit(searchContainer: View, onComplete: (() -> Unit)? = null) {
        // 创建滑出动画
        val slideOut = ObjectAnimator.ofFloat(searchContainer, "translationY", 0f, -50f).apply {
            duration = 250
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // 创建淡出动画
        val fadeOut = ObjectAnimator.ofFloat(searchContainer, "alpha", 1f, 0f).apply {
            duration = 200
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // 组合动画
        AnimatorSet().apply {
            playTogether(slideOut, fadeOut)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    /**
     * 搜索文本变化时的脉冲动画
     * 轻微的缩放脉冲效果
     */
    fun animateSearchPulse(editText: EditText) {
        val pulse = ObjectAnimator.ofFloat(editText, "scaleX", 1f, 1.01f, 1f).apply {
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
        }
        pulse.start()
    }
}

