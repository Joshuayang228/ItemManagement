package com.example.itemmanagement.ui.add

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.itemmanagement.ui.base.BaseItemViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 新架构的照片管理器类
 * 负责处理照片相关的所有操作，基于BaseItemViewModel
 */
class NewPhotoManager(
    private val fragment: Fragment,
    private val viewModel: BaseItemViewModel
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
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("选择照片来源")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> checkAndRequestCameraPermission()
                    1 -> checkAndRequestStoragePermission()
                }
            }
            .show()
    }

    /**
     * 创建临时照片文件
     */
    fun createTempImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = File(fragment.requireContext().cacheDir, "images")
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }
            
            File.createTempFile(imageFileName, ".jpg", storageDir).also {
                currentPhotoFile = it
            }
        } catch (ex: IOException) {
            null
        }
    }

    /**
     * 获取照片URI用于拍照
     */
    fun getPhotoURI(): Uri? {
        val photoFile = createTempImageFile() ?: return null
        currentPhotoUri = try {
            FileProvider.getUriForFile(
                fragment.requireContext(),
                "${fragment.requireContext().packageName}.fileprovider",
                photoFile
            )
        } catch (e: Exception) {
            null
        }
        return currentPhotoUri
    }

    /**
     * 处理相机拍照结果
     */
    fun handleCameraResult(onPhotoAdded: (String) -> Unit) {
        currentPhotoFile?.let { file ->
            if (file.exists()) {
                fragment.lifecycleScope.launch {
                    val compressedPath = compressAndSaveImage(file.absolutePath)
                    if (compressedPath != null) {
                        // 保存照片路径到ViewModel
                        val currentPhotos = viewModel.getFieldValue("照片") as? MutableList<String> 
                            ?: mutableListOf()
                        currentPhotos.add(compressedPath)
                        viewModel.saveFieldValue("照片", currentPhotos)
                        
                        onPhotoAdded(compressedPath)
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(fragment.requireContext(), "照片已添加", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理从相册选择的照片
     */
    fun handleGalleryResult(uri: Uri, onPhotoAdded: (String) -> Unit) {
        fragment.lifecycleScope.launch {
            try {
                val inputStream = fragment.requireContext().contentResolver.openInputStream(uri)
                val tempFile = File(fragment.requireContext().cacheDir, "temp_${System.currentTimeMillis()}.jpg")
                
                inputStream?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                val compressedPath = compressAndSaveImage(tempFile.absolutePath)
                if (compressedPath != null) {
                    // 保存照片路径到ViewModel
                    val currentPhotos = viewModel.getFieldValue("照片") as? MutableList<String> 
                        ?: mutableListOf()
                    currentPhotos.add(compressedPath)
                    viewModel.saveFieldValue("照片", currentPhotos)
                    
                    onPhotoAdded(compressedPath)
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(fragment.requireContext(), "照片已添加", Toast.LENGTH_SHORT).show()
                    }
                }
                
                // 清理临时文件
                tempFile.delete()
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(fragment.requireContext(), "处理照片失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * 压缩并保存图片
     */
    private suspend fun compressAndSaveImage(imagePath: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(imagePath)
                    ?: return@withContext null

                // 压缩图片
                val compressedBitmap = compressBitmap(bitmap)
                
                // 保存压缩后的图片
                val fileName = "img_${System.currentTimeMillis()}.jpg"
                val outputFile = File(fragment.requireContext().filesDir, fileName)
                
                FileOutputStream(outputFile).use { out ->
                    compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                
                bitmap.recycle()
                compressedBitmap.recycle()
                
                outputFile.absolutePath
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * 压缩Bitmap
     */
    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        val maxSize = 1024 // 最大尺寸
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }
        
        val ratio = if (width > height) {
            maxSize.toFloat() / width
        } else {
            maxSize.toFloat() / height
        }
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * 删除照片
     */
    fun deletePhoto(photoPath: String, onPhotoDeleted: () -> Unit) {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("删除照片")
            .setMessage("确定要删除这张照片吗？")
            .setPositiveButton("删除") { _, _ ->
                // 从ViewModel中移除
                val currentPhotos = viewModel.getFieldValue("照片") as? MutableList<String> 
                    ?: mutableListOf()
                currentPhotos.remove(photoPath)
                viewModel.saveFieldValue("照片", currentPhotos)
                
                // 删除文件
                try {
                    File(photoPath).delete()
                } catch (e: Exception) {
                    // 忽略文件删除错误
                }
                
                onPhotoDeleted()
                Toast.makeText(fragment.requireContext(), "照片已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 获取所有照片路径
     */
    fun getAllPhotos(): List<String> {
        return viewModel.getFieldValue("照片") as? List<String> ?: emptyList()
    }

    /**
     * 清除所有照片
     */
    fun clearAllPhotos() {
        val currentPhotos = viewModel.getFieldValue("照片") as? List<String> ?: emptyList()
        
        // 删除所有照片文件
        currentPhotos.forEach { photoPath ->
            try {
                File(photoPath).delete()
            } catch (e: Exception) {
                // 忽略文件删除错误
            }
        }
        
        // 清空ViewModel中的照片列表
        viewModel.saveFieldValue("照片", emptyList<String>())
    }

    /**
     * 设置照片列表
     */
    fun setPhotos(photos: List<String>) {
        viewModel.saveFieldValue("照片", photos)
    }
}