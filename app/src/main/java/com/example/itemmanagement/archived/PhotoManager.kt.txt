package com.example.itemmanagement.ui.add

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.itemmanagement.ui.base.FieldInteractionViewModel

/**
 * 照片管理器类
 * 负责处理照片相关的所有操作，包括拍照、从相册选择、压缩等
 * 
 * 现在使用FieldInteractionViewModel接口，与具体实现解耦
 */
class PhotoManager(
    private val fragment: Fragment,
    private val viewModel: FieldInteractionViewModel,
    private val dialogFactory: DialogFactory
) {
    private var currentPhotoUri: Uri? = null
    private var currentPhotoFile: File? = null
    
    /**
     * 显示照片选择对话框
     */
    fun showPhotoSelectionDialog(
        checkAndRequestCameraPermission: () -> Unit,
        checkAndRequestStoragePermission: () -> Unit
    ) {
        val items = arrayOf("拍照", "从相册选择")
        dialogFactory.createDialog(
            title = "选择照片来源",
            items = items
        ) { which ->
            when (which) {
                0 -> checkAndRequestCameraPermission()
                1 -> checkAndRequestStoragePermission()
            }
        }
    }
    
    /**
     * 启动相机
     */
    fun launchCamera(takePictureAction: (Uri) -> Unit) {
        try {
            currentPhotoFile = createTempImageFile("CAMERA")
            currentPhotoUri = FileProvider.getUriForFile(
                fragment.requireContext(),
                "${fragment.requireContext().packageName}.provider",
                currentPhotoFile!!
            )
            takePictureAction(currentPhotoUri!!)
        } catch (e: IOException) {
            Toast.makeText(fragment.context, "创建临时文件失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 处理相机拍照结果
     */
    fun handleCameraResult(success: Boolean, isFragmentActive: () -> Boolean) {
        if (!isFragmentActive()) {
            cleanupTempFiles()
            return
        }

        if (success) {
            currentPhotoUri?.let { uri ->
                fragment.lifecycleScope.launch {
                    try {
                        val isValid = withContext(Dispatchers.IO) {
                            currentPhotoFile?.let { file ->
                                if (file.exists() && file.length() > 0) {
                                    // 压缩图片
                                    val compressedUri = compressImage(uri)
                                    if (compressedUri != null) {
                                        // 验证压缩后的URI
                                        val isUriValid = isUriValid(compressedUri)
                                        if (!isUriValid) {
                                            false
                                        } else {
                                            // 更新当前URI为压缩后的URI
                                            currentPhotoUri = compressedUri
                                            true
                                        }
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            } ?: false
                        }

                        withContext(Dispatchers.Main) {
                            if (!isFragmentActive()) {
                                cleanupTempFiles()
                                return@withContext
                            }

                            if (isValid) {
                                try {
                                    currentPhotoUri?.let { validUri ->
                                        viewModel.addPhotoUri(validUri)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(fragment.context, "添加照片失败", Toast.LENGTH_SHORT).show()
                                    cleanupTempFiles()
                                }
                            } else {
                                Toast.makeText(fragment.context, "照片保存失败", Toast.LENGTH_SHORT).show()
                                cleanupTempFiles()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if (isFragmentActive()) {
                                Toast.makeText(fragment.context, "处理照片时出错", Toast.LENGTH_SHORT).show()
                            }
                            cleanupTempFiles()
                        }
                    }
                }
            }
        } else {
            cleanupTempFiles()
        }
    }
    
    /**
     * 处理从相册选择的图片
     */
    fun handleGalleryResult(uri: Uri?, isFragmentActive: () -> Boolean) {
        if (!isFragmentActive() || uri == null) {
            return
        }

        fragment.lifecycleScope.launch {
            try {
                val copiedUri = withContext(Dispatchers.IO) {
                    // 先复制到私有存储
                    val tempUri = copyUriToPrivateStorage(uri)

                    // 压缩复制后的图片
                    tempUri?.let { compressImage(it) }
                }

                withContext(Dispatchers.Main) {
                    if (!isFragmentActive()) {
                        return@withContext
                    }

                    if (copiedUri != null) {
                        viewModel.addPhotoUri(copiedUri)
                    } else {
                        Toast.makeText(fragment.context, "处理图片失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isFragmentActive()) {
                        Toast.makeText(fragment.context, "处理图片时出错", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    /**
     * 创建临时图片文件
     */
    private fun createTempImageFile(prefix: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "${prefix}_${timeStamp}"
        val storageDir = fragment.requireContext().getExternalFilesDir("Photos")
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",        /* suffix */
            storageDir     /* directory */
        )
    }
    
    /**
     * 图片压缩工具方法
     */
    private fun compressImage(uri: Uri): Uri? {
        return try {
            val context = fragment.requireContext()
            // 获取图片的原始尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            // 计算采样率
            val maxDimension = 1024 // 最大尺寸
            var sampleSize = 1

            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val heightRatio = Math.round(options.outHeight.toFloat() / maxDimension.toFloat())
                val widthRatio = Math.round(options.outWidth.toFloat() / maxDimension.toFloat())
                sampleSize = if (heightRatio < widthRatio) widthRatio else heightRatio
            }

            // 使用采样率加载图片
            val compressOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            // 加载并压缩图片
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, compressOptions)
            } ?: throw Exception("无法加载图片")

            // 创建压缩后的文件
            val compressedFile = createTempImageFile("COMPRESSED")
            FileOutputStream(compressedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            // 释放Bitmap
            bitmap.recycle()

            // 生成新的URI
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                compressedFile
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 将URI复制到私有存储
     */
    private fun copyUriToPrivateStorage(sourceUri: Uri): Uri? {
        return try {
            val context = fragment.requireContext()
            val tempFile = createTempImageFile("GALLERY")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 验证URI是否有效
     */
    private fun isUriValid(uri: Uri): Boolean {
        return try {
            fragment.requireContext().contentResolver.openInputStream(uri)?.use {
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 清理临时文件
     */
    fun cleanupTempFiles() {
        currentPhotoFile?.let {
            if (it.exists()) {
                it.delete()
            }
        }
        currentPhotoFile = null
        currentPhotoUri = null
    }
    
    /**
     * 显示照片查看对话框
     */
    fun showPhotoViewDialog(uri: Uri) {
        val context = fragment.requireContext()
        val dialog = android.app.Dialog(context)
        dialog.setContentView(com.example.itemmanagement.R.layout.dialog_photo_view)
        
        val photoView = dialog.findViewById<android.widget.ImageView>(com.example.itemmanagement.R.id.photoView)
        
        com.bumptech.glide.Glide.with(context)
            .load(uri)
            .error(com.example.itemmanagement.R.drawable.ic_image_error)
            .into(photoView)
        
        // 点击图片关闭对话框
        photoView.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
} 