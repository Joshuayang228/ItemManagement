package com.example.itemmanagement.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * 导出文件管理器
 * 负责文件的保存、分享和权限管理
 */
class ExportManager(private val context: Context) {
    
    companion object {
        private const val EXPORT_FOLDER = "ItemManagement_Export"
        private const val FILE_PROVIDER_AUTHORITY = "com.example.itemmanagement.provider"
    }
    
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    /**
     * 导出数据类型枚举
     */
    enum class ExportType(val displayName: String, val filePrefix: String) {
        ITEMS("物品清单", "items"),
        WARRANTIES("保修记录", "warranties"),
        BORROWS("借还记录", "borrows"),
        SHOPPING("购物清单", "shopping"),
        SUMMARY("数据摘要", "summary"),
        ALL("完整数据", "complete")
    }
    
    /**
     * 导出结果
     */
    data class ExportResult(
        val success: Boolean,
        val message: String,
        val filePath: String? = null,
        val fileUri: Uri? = null
    )
    
    /**
     * 保存CSV文件到Downloads目录
     * @param content CSV内容
     * @param exportType 导出类型
     * @param customName 自定义文件名（可选）
     */
    suspend fun saveCSVFile(
        content: String, 
        exportType: ExportType,
        customName: String? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        
        if (content.isEmpty() || content.startsWith("没有")) {
            return@withContext ExportResult(false, content)
        }
        
        try {
            val timestamp = dateFormat.format(Date())
            val fileName = customName ?: "${exportType.filePrefix}_${timestamp}.csv"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore
                saveWithMediaStore(content, fileName, exportType)
            } else {
                // Android 9及以下使用传统文件系统
                saveWithFileSystem(content, fileName, exportType)
            }
            
        } catch (e: Exception) {
            ExportResult(false, "保存文件失败: ${e.message}")
        }
    }
    
    /**
     * 分享CSV文件
     */
    fun shareCSVFile(fileUri: Uri, exportType: ExportType) {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "物品管理 - ${exportType.displayName}")
                putExtra(Intent.EXTRA_TEXT, "这是从物品管理应用导出的${exportType.displayName}数据")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "分享${exportType.displayName}")
            chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(chooserIntent)
            
        } catch (e: Exception) {
            Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 批量导出所有数据
     */
    suspend fun exportAllData(
        itemsContent: String,
        warrantiesContent: String,
        borrowsContent: String,
        shoppingContent: String,
        summaryContent: String
    ): List<ExportResult> = withContext(Dispatchers.IO) {
        
        val results = mutableListOf<ExportResult>()
        val timestamp = dateFormat.format(Date())
        
        // 导出各类数据
        if (itemsContent.isNotEmpty() && !itemsContent.startsWith("没有")) {
            results.add(saveCSVFile(itemsContent, ExportType.ITEMS, "物品清单_${timestamp}.csv"))
        }
        
        if (warrantiesContent.isNotEmpty() && !warrantiesContent.startsWith("没有")) {
            results.add(saveCSVFile(warrantiesContent, ExportType.WARRANTIES, "保修记录_${timestamp}.csv"))
        }
        
        if (borrowsContent.isNotEmpty() && !borrowsContent.startsWith("没有")) {
            results.add(saveCSVFile(borrowsContent, ExportType.BORROWS, "借还记录_${timestamp}.csv"))
        }
        
        if (shoppingContent.isNotEmpty() && !shoppingContent.startsWith("没有")) {
            results.add(saveCSVFile(shoppingContent, ExportType.SHOPPING, "购物清单_${timestamp}.csv"))
        }
        
        // 导出摘要
        results.add(saveCSVFile(summaryContent, ExportType.SUMMARY, "数据摘要_${timestamp}.txt"))
        
        results
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * Android 10+ 使用MediaStore保存文件
     */
    private suspend fun saveWithMediaStore(
        content: String, 
        fileName: String, 
        exportType: ExportType
    ): ExportResult = withContext(Dispatchers.IO) {
        
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, if (fileName.endsWith(".csv")) "text/csv" else "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/$EXPORT_FOLDER")
            }
            
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                }
                
                ExportResult(
                    success = true, 
                    message = "文件已保存到Downloads/${EXPORT_FOLDER}/${fileName}",
                    fileUri = uri,
                    filePath = "Downloads/${EXPORT_FOLDER}/${fileName}"
                )
            } else {
                ExportResult(false, "创建文件失败")
            }
            
        } catch (e: IOException) {
            ExportResult(false, "写入文件失败: ${e.message}")
        } catch (e: Exception) {
            ExportResult(false, "保存失败: ${e.message}")
        }
    }
    
    /**
     * Android 9及以下使用传统文件系统
     */
    private suspend fun saveWithFileSystem(
        content: String, 
        fileName: String, 
        exportType: ExportType
    ): ExportResult = withContext(Dispatchers.IO) {
        
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val exportDir = File(downloadsDir, EXPORT_FOLDER)
            
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            
            val file = File(exportDir, fileName)
            
            FileWriter(file, Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
            
            // 创建FileProvider URI用于分享
            val fileUri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                file
            )
            
            ExportResult(
                success = true,
                message = "文件已保存到${file.absolutePath}",
                fileUri = fileUri,
                filePath = file.absolutePath
            )
            
        } catch (e: IOException) {
            ExportResult(false, "写入文件失败: ${e.message}")
        } catch (e: Exception) {
            ExportResult(false, "保存失败: ${e.message}")
        }
    }
}
