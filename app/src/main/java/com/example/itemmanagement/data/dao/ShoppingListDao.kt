package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.ShoppingListEntity
import com.example.itemmanagement.data.entity.ShoppingListStatus
import com.example.itemmanagement.data.entity.ShoppingListType
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    
    // 获取所有购物清单
    @Query("SELECT * FROM shopping_lists ORDER BY createdDate DESC")
    fun getAllShoppingLists(): Flow<List<ShoppingListEntity>>
    
    // 获取活跃的购物清单
    @Query("SELECT * FROM shopping_lists WHERE status = 'ACTIVE' ORDER BY createdDate DESC")
    fun getActiveShoppingLists(): Flow<List<ShoppingListEntity>>
    
    // 获取已完成的购物清单
    @Query("SELECT * FROM shopping_lists WHERE status = 'COMPLETED' ORDER BY createdDate DESC")
    fun getCompletedShoppingLists(): Flow<List<ShoppingListEntity>>
    
    // 根据类型获取购物清单
    @Query("SELECT * FROM shopping_lists WHERE type = :type ORDER BY createdDate DESC")
    fun getShoppingListsByType(type: ShoppingListType): Flow<List<ShoppingListEntity>>
    
    // 根据ID获取购物清单
    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    suspend fun getShoppingListById(id: Long): ShoppingListEntity?
    
    // 插入购物清单
    @Insert
    suspend fun insertShoppingList(shoppingList: ShoppingListEntity): Long
    
    // 更新购物清单
    @Update
    suspend fun updateShoppingList(shoppingList: ShoppingListEntity)
    
    // 删除购物清单
    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingListEntity)
    
    // 根据ID删除购物清单
    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteShoppingListById(id: Long)
    
    // 更新购物清单状态
    @Query("UPDATE shopping_lists SET status = :status WHERE id = :id")
    suspend fun updateShoppingListStatus(id: Long, status: ShoppingListStatus)
    
    // 获取购物清单数量统计
    @Query("SELECT COUNT(*) FROM shopping_lists")
    fun getShoppingListsCount(): Flow<Int>
    
    // 获取活跃清单数量
    @Query("SELECT COUNT(*) FROM shopping_lists WHERE status = 'ACTIVE'")
    fun getActiveListsCount(): Flow<Int>
} 