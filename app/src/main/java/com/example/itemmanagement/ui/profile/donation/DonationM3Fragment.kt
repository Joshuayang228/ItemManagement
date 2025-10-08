package com.example.itemmanagement.ui.profile.donation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import java.io.OutputStream

/**
 * Material 3联系我们页面Fragment
 * 展示微信和支付宝收款码，提供联系方式
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
        
        hideBottomNavigation()
        
        // Material 3 进入动画
        // view.fadeIn(150)
        
        setupClickListeners()
        setupCardOptimization()
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
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

        // 复制微信号按钮
        binding.btnCopyWechat.setOnClickListener {
            copyWechatId()
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
            // 根据prefix确定保存哪个二维码
            val resourceId = when (prefix) {
                "wechat_qr" -> R.drawable.wechat_qr
                "alipay_qr" -> R.drawable.alipay_qr
                else -> return
            }
            
            val description = when (prefix) {
                "wechat_qr" -> "微信二维码"
                "alipay_qr" -> "支付宝二维码"
                else -> "二维码"
            }
            
            // 使用 Glide 加载图片资源并转换为 Bitmap
            Glide.with(this)
                .asBitmap()
                .load(resourceId)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        saveImageToGallery(resource, "${prefix}_code.png", description)
                    }
                    
                    override fun onLoadCleared(placeholder: Drawable?) {
                        // 清理资源
                    }
                })
            
        } catch (e: Exception) {
            Material3Feedback.showError(binding.root, "保存失败：${e.message}")
        }
    }
    
    /**
     * 将图片保存到系统相册
     */
    private fun saveImageToGallery(bitmap: Bitmap, fileName: String, description: String) {
        try {
            val outputStream: OutputStream?
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore API
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ItemManagement")
                }
                
                val uri = requireContext().contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                outputStream = uri?.let { requireContext().contentResolver.openOutputStream(it) }
            } else {
                // Android 10 以下保存到 Pictures 目录
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(imagesDir, "ItemManagement")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                
                val imageFile = File(appDir, fileName)
                outputStream = FileOutputStream(imageFile)
                
                // 通知系统扫描新文件
                @Suppress("DEPRECATION")
                requireContext().sendBroadcast(
                    android.content.Intent(
                        android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        android.net.Uri.fromFile(imageFile)
                    )
                )
            }
            
            outputStream?.use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.flush()
            }
            
            // 通知用户保存成功
            Material3Feedback.showSuccess(binding.root, "${description}已保存到相册")
            
        } catch (e: Exception) {
            Material3Feedback.showError(binding.root, "保存失败：${e.localizedMessage}")
        }
    }

    private fun copyWechatId() {
        try {
            val wechatId = "Joshuayang2228"
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("微信号", wechatId)
            clipboard.setPrimaryClip(clip)
            
            // 成功反馈
            Material3Feedback.showSuccess(binding.root, "微信号已复制")
            
        } catch (e: Exception) {
            Material3Feedback.showError(binding.root, "复制失败：${e.message}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }

    /**
     * 隐藏底部导航栏
     */
    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    /**
     * 显示底部导航栏
     */
    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }
}
