package com.example.itemmanagement.test

import android.content.Context
import android.util.Log
import com.example.itemmanagement.data.AppDatabase
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 测试数据插入器
 * 用于将生成的测试数据插入到数据库中
 */
object TestDataInserter {
    
    private const val TAG = "TestDataInserter"
    
    /**
     * 插入全字段测试数据到数据库
     * @param context 应用上下文
     * @param onComplete 完成回调
     */
    fun insertTestData(context: Context, onComplete: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "开始插入测试数据...")
                
                // 获取数据库和Repository
                val database = AppDatabase.getDatabase(context)
                val repository = UnifiedItemRepository(
                    database,
                    database.unifiedItemDao(),
                    database.itemStateDao(),
                    database.shoppingDetailDao(),
                    database.shoppingListDao(),
                    database.inventoryDetailDao(),
                    database.locationDao(),
                    database.tagDao(),
                    database.photoDao(),
                    database.priceRecordDao()
                )
                
                // 生成测试数据
                val testItems = TestDataGenerator.generateFullFieldTestItems()
                Log.d(TAG, "生成了 ${testItems.size} 个测试物品")
                
                var successCount = 0
                var errorCount = 0
                
                // 逐个插入测试数据
                testItems.forEachIndexed { index, testData ->
                    try {
                        Log.d(TAG, "正在插入第 ${index + 1} 个物品: ${testData.unifiedItem.name}")
                        
                        repository.addInventoryItem(
                            unifiedItem = testData.unifiedItem,
                            inventoryDetail = testData.inventoryDetail
                        )
                        
                        Log.d(TAG, "成功插入物品: ${testData.unifiedItem.name}")
                        successCount++
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "插入物品失败: ${testData.unifiedItem.name}", e)
                        errorCount++
                    }
                }
                
                // 回调结果
                withContext(Dispatchers.Main) {
                    val message = "测试数据插入完成！\n成功: $successCount 个\n失败: $errorCount 个"
                    Log.i(TAG, message)
                    onComplete(errorCount == 0, message)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "插入测试数据时发生错误", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "插入测试数据失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 清除所有测试数据（可选功能）
     * 注意：这会删除所有物品数据，请谨慎使用！
     */
    fun clearAllData(context: Context, onComplete: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.w(TAG, "开始清除所有数据...")
                
                val database = AppDatabase.getDatabase(context)
                
                // 清除所有表的数据
                database.clearAllTables()
                
                withContext(Dispatchers.Main) {
                    val message = "所有数据已清除！"
                    Log.i(TAG, message)
                    onComplete(true, message)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "清除数据时发生错误", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "清除数据失败: ${e.message}")
                }
            }
        }
    }
}
