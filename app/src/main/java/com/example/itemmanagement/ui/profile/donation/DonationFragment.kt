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
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentDonationBinding

import java.io.File
import java.io.FileOutputStream

/**
 * 打赏支持页面Fragment
 * 展示微信和支付宝收款码，支持保存图片、留言等功能
 */
class DonationFragment : Fragment() {
    
    private var _binding: FragmentDonationBinding? = null
    private val binding get() = _binding!!
    
    // 感谢名单数据（实际项目中可以从服务器获取）
    private val donorList = listOf(
        "感谢 *明 的支持 ❤️",
        "感谢 *华 的鼓励 🙏", 
        "感谢 *丽 的打赏 💝",
        "感谢所有默默支持的朋友们 🌟"
    )
    
    // 留言列表（实际项目中可以从服务器获取）
    private val messageList = mutableListOf<String>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupQRCodeInteractions()
        setupDonorList()
        setupMessageFunctionality()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
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
    
    private fun setupDonorList() {
        // 显示感谢名单
        val donorText = buildString {
            donorList.forEach { donor ->
                appendLine(donor)
            }
            if (donorList.isEmpty()) {
                append("暂无打赏记录\n期待您的第一份支持！")
            } else {
                append("\n🎉 排名不分先后，感谢每一份心意！")
            }
        }
        binding.tvDonorList.text = donorText
    }
    
    private fun setupMessageFunctionality() {
        binding.btnSendMessage.setOnClickListener {
            sendMessage()
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
            Toast.makeText(
                requireContext(),
                "保存失败，请重试",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * 将图片保存到相册
     */
    private fun saveImageToGallery(bitmap: Bitmap, fileName: String, description: String) {
        try {
            val file = File(requireContext().getExternalFilesDir(null), fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            // 通知用户保存成功
            Toast.makeText(
                requireContext(),
                "${description}已保存到本地",
                Toast.LENGTH_SHORT
            ).show()
            
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "保存失败：${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * 复制微信相关信息
     */
    private fun copyWechatInfo() {
        val wechatInfo = "微信打赏\n感谢您的支持！"
        copyToClipboard("微信打赏信息", wechatInfo)
        Toast.makeText(requireContext(), "微信打赏信息已复制", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 复制支付宝相关信息  
     */
    private fun copyAlipayInfo() {
        val alipayInfo = "支付宝打赏\n感谢您的支持！"
        copyToClipboard("支付宝打赏信息", alipayInfo)
        Toast.makeText(requireContext(), "支付宝打赏信息已复制", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 复制文本到剪贴板
     */
    private fun copyToClipboard(label: String, text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }
    
    /**
     * 发送留言
     */
    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()
        if (message.isBlank()) {
            Toast.makeText(requireContext(), "请输入留言内容", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (message.length > 200) {
            Toast.makeText(requireContext(), "留言内容过长，请控制在200字内", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 模拟发送留言
        sendMessageToServer(message)
    }
    
    /**
     * 模拟向服务器发送留言
     */
    private fun sendMessageToServer(message: String) {
        // 这里应该是实际的网络请求
        // 现在只是模拟本地添加
        messageList.add("用户留言：$message")
        binding.etMessage.text?.clear()
        
        Toast.makeText(
            requireContext(),
            "留言发送成功，感谢您的支持！",
            Toast.LENGTH_SHORT
        ).show()
        
        // 可以在这里更新留言显示区域
        updateMessageDisplay()
    }
    
    /**
     * 更新留言显示
     */
    private fun updateMessageDisplay() {
        // 如果有留言显示区域，可以在这里更新
        // 目前的布局中没有留言显示区域，可以后续添加
    }
    
    /**
     * 获取打赏统计信息（模拟数据）
     */
    private fun getDonationStats(): DonationStats {
        return DonationStats(
            totalDonors = donorList.size,
            totalAmount = "保密", // 实际项目中可以展示总金额
            lastDonationDate = "2024年1月15日",
            messageCount = messageList.size
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * 打赏统计信息数据类
 */
data class DonationStats(
    val totalDonors: Int,           // 总打赏人数
    val totalAmount: String,        // 总打赏金额
    val lastDonationDate: String,   // 最后一次打赏日期
    val messageCount: Int           // 留言数量
)
