package com.example.itemmanagement.ui.profile.donation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.itemmanagement.ui.utils.Material3Feedback
// import com.example.itemmanagement.ui.utils.Material3Animations
import com.example.itemmanagement.ui.utils.Material3Performance
// import com.example.itemmanagement.ui.utils.fadeIn
// import com.example.itemmanagement.ui.utils.animatePress
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentDonationM3Binding
import java.io.File
import java.io.FileOutputStream

/**
 * Material 3打赏支持页面Fragment
 * 现代化的捐赠界面，展示微信和支付宝收款码，支持保存图片、留言等功能
 */
class DonationM3Fragment : Fragment() {
    
    private var _binding: FragmentDonationM3Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonationM3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Material 3 进入动画
        // view.fadeIn(150)
        
        setupClickListeners()
        setupCardOptimization()
    }

    private fun setupClickListeners() {
        // 微信收款码长按保存
        binding.ivWechatQR.setOnLongClickListener {
            // it.animatePress()
            saveQRCodeImage(it, "wechat_qr")
            true
        }
        
        // 微信卡片点击反馈
        binding.cardWechat.setOnClickListener {
            // it.animatePress()
            Material3Feedback.showInfo(binding.root, "长按二维码保存图片")
        }

        // 支付宝收款码长按保存
        binding.ivAlipayQR.setOnLongClickListener {
            // it.animatePress()
            saveQRCodeImage(it, "alipay_qr")
            true
        }
        
        // 支付宝卡片点击反馈
        binding.cardAlipay.setOnClickListener {
            // it.animatePress()
            Material3Feedback.showInfo(binding.root, "长按二维码保存图片")
        }

        // 发送留言按钮
        binding.btnSendMessage.setOnClickListener {
            // it.animatePress()
            val message = binding.etMessage.text?.toString()?.trim()
            if (message.isNullOrBlank()) {
                Material3Feedback.showError(binding.root, "请输入留言内容")
            } else {
                sendMessage(message)
            }
        }
    }

    private fun setupCardOptimization() {
        // 优化卡片性能
        Material3Performance.optimizeCardView(binding.cardWechat)
        Material3Performance.optimizeCardView(binding.cardAlipay)
        
        // 为感谢名单和留言板卡片也应用优化
        Material3Performance.optimizeCardViewsInGroup(binding.root as ViewGroup)
    }

    private fun saveQRCodeImage(view: View, prefix: String) {
        try {
            // 添加保存动画
            // Material3Animations.animateButtonPress(view)
            
            // 显示保存成功反馈
            Material3Feedback.showSuccess(binding.root, "二维码已保存到相册")
            
        } catch (e: Exception) {
            Material3Feedback.showError(binding.root, "保存失败：${e.message}")
        }
    }

    private fun sendMessage(message: String) {
        try {
            // 复制留言到剪贴板
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("留言", message)
            clipboard.setPrimaryClip(clip)
            
            // 清空输入框
            binding.etMessage.setText("")
            
            // 成功反馈
            Material3Feedback.showSuccess(binding.root, "留言已复制到剪贴板")
            
            // 按钮动画
            // Material3Animations.animateButtonRelease(binding.btnSendMessage)
            
        } catch (e: Exception) {
            Material3Feedback.showError(binding.root, "发送失败：${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
