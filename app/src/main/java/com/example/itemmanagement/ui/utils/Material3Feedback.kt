package com.example.itemmanagement.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.example.itemmanagement.R

/**
 * Material Design 3 反馈工具类
 * 提供符合M3规范的触觉反馈和视觉反馈
 */
object Material3Feedback {
    
    /**
     * 执行触觉反馈
     * 根据Android版本提供最佳的触觉反馈体验
     */
    fun performHapticFeedback(view: View, feedbackType: Int = HapticFeedbackConstants.CONTEXT_CLICK) {
        try {
            // 使用View的内建触觉反馈
            view.performHapticFeedback(feedbackType, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING)
            
            // 对于支持的设备，提供额外的振动反馈
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                performVibration(view.context, feedbackType)
            }
        } catch (e: Exception) {
            // 忽略触觉反馈的异常，不影响主要功能
        }
    }
    
    /**
     * 根据反馈类型执行相应的振动
     */
    private fun performVibration(context: Context, feedbackType: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            val vibrationEffect = when (feedbackType) {
                HapticFeedbackConstants.CONTEXT_CLICK -> {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
                HapticFeedbackConstants.LONG_PRESS -> {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                }
                HapticFeedbackConstants.KEYBOARD_TAP -> {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                }
                else -> {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                }
            }
            
            if (vibrator.hasVibrator()) {
                vibrator.vibrate(vibrationEffect)
            }
        } catch (e: Exception) {
            // 忽略振动异常
        }
    }
    
    /**
     * 轻触反馈 - 用于按钮点击
     */
    fun performLightClick(view: View) {
        performHapticFeedback(view, HapticFeedbackConstants.CONTEXT_CLICK)
    }
    
    /**
     * 重触反馈 - 用于长按或重要操作
     */
    fun performHeavyClick(view: View) {
        performHapticFeedback(view, HapticFeedbackConstants.LONG_PRESS)
    }
    
    /**
     * 键盘触摸反馈 - 用于文本输入
     */
    fun performKeyboardTap(view: View) {
        performHapticFeedback(view, HapticFeedbackConstants.KEYBOARD_TAP)
    }
    
    /**
     * 选择反馈 - 用于列表项选择
     */
    fun performSelectionClick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHapticFeedback(view, HapticFeedbackConstants.CONFIRM)
        } else {
            performHapticFeedback(view, HapticFeedbackConstants.CONTEXT_CLICK)
        }
    }
    
    /**
     * 拒绝反馈 - 用于无效操作
     */
    fun performRejectFeedback(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            performHapticFeedback(view, HapticFeedbackConstants.REJECT)
        } else {
            // 对于旧版本，执行双击反馈来表示拒绝
            performHapticFeedback(view, HapticFeedbackConstants.CONTEXT_CLICK)
            view.postDelayed({
                performHapticFeedback(view, HapticFeedbackConstants.CONTEXT_CLICK)
            }, 50)
        }
    }
    
    // =================== 视觉反馈方法 ===================
    
    /**
     * 显示信息提示
     */
    fun showInfo(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, R.color.primary))
        snackbar.setTextColor(ContextCompat.getColor(view.context, R.color.on_primary))
        snackbar.show()
        
        // 添加轻微的触觉反馈
        performLightClick(view)
    }
    
    /**
     * 显示成功提示
     */
    fun showSuccess(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, R.color.success))
        snackbar.setTextColor(ContextCompat.getColor(view.context, R.color.white))
        snackbar.show()
        
        // 添加确认触觉反馈
        performSelectionClick(view)
    }
    
    /**
     * 显示错误提示
     */
    fun showError(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, R.color.error))
        snackbar.setTextColor(ContextCompat.getColor(view.context, R.color.white))
        snackbar.show()
        
        // 添加拒绝触觉反馈
        performRejectFeedback(view)
    }
    
    /**
     * 显示警告提示
     */
    fun showWarning(view: View, message: String, duration: Int = Snackbar.LENGTH_LONG) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, R.color.orange))
        snackbar.setTextColor(ContextCompat.getColor(view.context, R.color.on_orange_container))
        snackbar.show()
        
        // 添加轻微的触觉反馈
        performLightClick(view)
    }
}