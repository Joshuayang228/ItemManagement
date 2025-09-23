package com.example.itemmanagement.test

import android.content.Context
import android.util.Log
import com.example.itemmanagement.data.AppDatabase
import com.example.itemmanagement.data.repository.WishlistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 心愿单测试数据插入器
 * 用于将生成的心愿单测试数据插入到数据库中
 */
object WishlistTestDataInserter {
    
    private const val TAG = "WishlistTestDataInserter"
    
    /**
     * 插入心愿单测试数据到数据库
     * @param context 应用上下文
     * @param onComplete 完成回调
     */
    fun insertWishlistTestData(context: Context, onComplete: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "开始插入心愿单测试数据...")
                
                // 获取数据库和Repository
                val database = AppDatabase.getDatabase(context)
                val wishlistRepository = WishlistRepository(
                    database = database,
                    wishlistDao = database.wishlistDao(),
                    priceHistoryDao = database.wishlistPriceHistoryDao()
                )
                
                // 生成测试数据
                val testItems = WishlistTestDataGenerator.generateWishlistTestItems()
                Log.d(TAG, "生成了 ${testItems.size} 个心愿单测试物品")
                
                var successCount = 0
                var errorCount = 0
                
                // 逐个插入测试数据
                testItems.forEachIndexed { index, testData ->
                    try {
                        Log.d(TAG, "正在插入第 ${index + 1} 个心愿单物品: ${testData.item.name}")
                        
                        // 插入心愿单物品
                        val itemId = database.wishlistDao().insert(testData.item)
                        Log.d(TAG, "成功插入心愿单物品: ${testData.item.name}, ID: $itemId")
                        
                        // 插入价格历史记录（如果有）
                        if (testData.priceHistory.isNotEmpty()) {
                            testData.priceHistory.forEach { priceHistory ->
                                val updatedPriceHistory = priceHistory.copy(wishlistItemId = itemId)
                                database.wishlistPriceHistoryDao().insert(updatedPriceHistory)
                                Log.d(TAG, "插入价格历史记录: ${priceHistory.price}${priceHistory.priceUnit}")
                            }
                        }
                        
                        successCount++
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "插入心愿单物品失败: ${testData.item.name}", e)
                        errorCount++
                    }
                }
                
                // 回调结果
                withContext(Dispatchers.Main) {
                    val message = "心愿单测试数据插入完成！\n成功: $successCount 个\n失败: $errorCount 个"
                    Log.i(TAG, message)
                    onComplete(errorCount == 0, message)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "插入心愿单测试数据时发生错误", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "插入心愿单测试数据失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 清除所有心愿单数据（可选功能）
     * 注意：这会删除所有心愿单数据，请谨慎使用！
     */
    fun clearAllWishlistData(context: Context, onComplete: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.w(TAG, "开始清除所有心愿单数据...")
                
                val database = AppDatabase.getDatabase(context)
                val wishlistRepository = WishlistRepository(
                    database = database,
                    wishlistDao = database.wishlistDao(),
                    priceHistoryDao = database.wishlistPriceHistoryDao()
                )
                
                // 清除所有心愿单数据
                // 由于没有直接的deleteAll方法，使用事务清理所有数据
                database.clearAllTables()
                
                withContext(Dispatchers.Main) {
                    val message = "所有心愿单数据已清除！"
                    Log.i(TAG, message)
                    onComplete(true, message)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "清除心愿单数据时发生错误", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "清除心愿单数据失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 插入库存和心愿单的组合测试数据
     * @param context 应用上下文
     * @param onComplete 完成回调
     */
    fun insertCombinedTestData(context: Context, onComplete: (Boolean, String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "开始插入组合测试数据（库存 + 心愿单）...")
                
                var totalSuccess = 0
                var totalError = 0
                val results = mutableListOf<String>()
                
                // 插入库存测试数据
                TestDataInserter.insertTestData(context) { success, message ->
                    if (success) {
                        totalSuccess++
                        results.add("✅ 库存数据插入成功")
                    } else {
                        totalError++
                        results.add("❌ 库存数据插入失败: $message")
                    }
                }
                
                // 等待库存数据插入完成，然后插入心愿单数据
                kotlinx.coroutines.delay(1000) // 等待1秒确保库存数据插入完成
                
                insertWishlistTestData(context) { success, message ->
                    if (success) {
                        totalSuccess++
                        results.add("✅ 心愿单数据插入成功")
                    } else {
                        totalError++
                        results.add("❌ 心愿单数据插入失败: $message")
                    }
                    
                    // 最终结果回调
                    val finalMessage = """
                        |组合测试数据插入完成！
                        |
                        |${results.joinToString("\n")}
                        |
                        |总计: 成功 $totalSuccess 项，失败 $totalError 项
                    """.trimMargin()
                    
                    onComplete(totalError == 0, finalMessage)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "插入组合测试数据时发生错误", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "插入组合测试数据失败: ${e.message}")
                }
            }
        }
    }
}
