package com.example.itemmanagement.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min

/**
 * 图片压缩工具类
 * 用于压缩上传的图片，节省存储空间和提高加载速度
 */
object ImageCompressor {

    /**
     * 压缩图片
     * @param context 上下文
     * @param uri 原图片URI
     * @param maxWidth 最大宽度
     * @param maxHeight 最大高度
     * @param quality 压缩质量 (0-100)
     * @return 压缩后的图片URI
     */
    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 800,
        maxHeight: Int = 800,
        quality: Int = 80
    ): Uri = withContext(Dispatchers.IO) {
        try {
            // 读取原图片
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) {
                throw IOException("无法解码图片")
            }

            // 处理图片旋转
            val rotatedBitmap = rotateImageIfRequired(context, originalBitmap, uri)

            // 计算压缩比例
            val scale = calculateScale(rotatedBitmap.width, rotatedBitmap.height, maxWidth, maxHeight)
            val newWidth = (rotatedBitmap.width * scale).toInt()
            val newHeight = (rotatedBitmap.height * scale).toInt()

            // 压缩图片
            val compressedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, newWidth, newHeight, true)

            // 保存压缩后的图片
            val compressedFile = File(
                context.getExternalFilesDir("compressed_images"),
                "compressed_${System.currentTimeMillis()}.jpg"
            )

            // 确保目录存在
            compressedFile.parentFile?.mkdirs()

            FileOutputStream(compressedFile).use { out ->
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            // 清理内存
            if (rotatedBitmap != originalBitmap) {
                rotatedBitmap.recycle()
            }
            originalBitmap.recycle()
            compressedBitmap.recycle()

            Uri.fromFile(compressedFile)
        } catch (e: Exception) {
            throw IOException("图片压缩失败: ${e.message}", e)
        }
    }

    /**
     * 计算压缩比例
     */
    private fun calculateScale(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Float {
        val widthScale = maxWidth.toFloat() / width
        val heightScale = maxHeight.toFloat() / height
        return min(min(widthScale, heightScale), 1.0f)
    }

    /**
     * 根据EXIF信息旋转图片
     */
    private fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = ExifInterface(inputStream!!)
            inputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * 旋转图片
     */
    private fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * 清理压缩文件缓存
     */
    suspend fun cleanupCompressedImages(context: Context) = withContext(Dispatchers.IO) {
        try {
            val compressedDir = File(context.getExternalFilesDir("compressed_images"), "")
            if (compressedDir.exists()) {
                val files = compressedDir.listFiles()
                val currentTime = System.currentTimeMillis()
                files?.forEach { file ->
                    // 删除超过7天的文件
                    if (currentTime - file.lastModified() > 7 * 24 * 60 * 60 * 1000) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }
}
