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
import androidx.fragment.app.Fragment
import com.example.itemmanagement.ui.utils.Material3Feedback
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentDonationM3Binding
import com.example.itemmanagement.utils.SnackbarHelper
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * 联系我们页面Fragment
 * 展示微信和支付宝收款码，提供联系方式
 */
class DonationFragment : Fragment() {
    
    private var _binding: FragmentDonationM3Binding? = null
    private val binding get() = _binding!!
    
    // 微信号
    private val wechatId = "Joshuayang2228"
    
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
        
        setupQRCodeInteractions()
        setupCopyWechatId()
    }
    
    // Toolbar功能移除，导航由MainActivity统一管理
    
    private fun setupQRCodeInteractions() {
        // 微信收款码长按保存
        binding.ivWechatQR.setOnLongClickListener {
            saveQRCodeImage("wechat", R.drawable.wechat_qr, "微信收款码")
            true
        }
        
        // 微信卡片点击
        binding.cardWechat.setOnClickListener {
            copyWechatInfo()
        }
        
        // 支付宝收款码长按保存
        binding.ivAlipayQR.setOnLongClickListener {
            saveQRCodeImage("alipay", R.drawable.alipay_qr, "支付宝收款码")
            true
        }
        
        // 支付宝卡片点击
        binding.cardAlipay.setOnClickListener {
            copyAlipayInfo()
        }
    }
    
    /**
     * 设置复制微信号功能
     */
    private fun setupCopyWechatId() {
        binding.btnCopyWechat.setOnClickListener {
            copyToClipboard("微信号", wechatId)
            view?.let { 
                Material3Feedback.showSuccess(it, "微信号已复制")
            }
        }
    }
    
    /**
     * 保存二维码图片到相册
     */
    private fun saveQRCodeImage(platform: String, resourceId: Int, description: String) {
        try {
            Glide.with(this)
                .asBitmap()
                .load(resourceId)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        saveImageToGallery(resource, "${platform}_qr_code.png", description)
                    }
                    
                    override fun onLoadCleared(placeholder: Drawable?) {
                        // 清理资源
                    }
                })
        } catch (e: Exception) {
            SnackbarHelper.showError(requireView(), "保存失败，请重试")
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
            SnackbarHelper.showSuccess(requireView(), "${description}已保存到相册")
            
        } catch (e: Exception) {
            SnackbarHelper.showError(requireView(), "保存失败：${e.localizedMessage}")
        }
    }
    
    /**
     * 复制微信相关信息
     */
    private fun copyWechatInfo() {
        val wechatInfo = "微信请喝咖啡\n感谢您的支持！"
        copyToClipboard("微信请喝咖啡", wechatInfo)
        SnackbarHelper.showSuccess(requireView(), "微信信息已复制")
    }
    
    /**
     * 复制支付宝相关信息  
     */
    private fun copyAlipayInfo() {
        val alipayInfo = "支付宝请喝咖啡\n感谢您的支持！"
        copyToClipboard("支付宝请喝咖啡", alipayInfo)
        SnackbarHelper.showSuccess(requireView(), "支付宝信息已复制")
    }
    
    /**
     * 复制文本到剪贴板
     */
    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
