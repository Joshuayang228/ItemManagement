package com.example.itemmanagement.data.dao

import androidx.room.*
import com.example.itemmanagement.data.entity.ItemTagCrossRef
import com.example.itemmanagement.data.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    // === 基本操作 ===
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tags: List<TagEntity>): List<Long>
    
    @Update
    suspend fun update(tag: TagEntity)
    
    @Delete
    suspend fun delete(tag: TagEntity)
    
    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    // === 查询操作 ===
    
    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getById(id: Long): TagEntity?
    
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>
    
    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsSync(): List<TagEntity>
    
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): TagEntity?
    
    // === 物品标签关联查询 ===
    
    /**
     * 获取指定物品的所有标签
     */
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN item_tag_cross_ref itcr ON t.id = itcr.tagId
        WHERE itcr.itemId = :itemId
        ORDER BY t.name ASC
    """)
    suspend fun getTagsByItemId(itemId: Long): List<TagEntity>
    
    /**
     * 获取指定物品的所有标签（Flow版本）
     */
    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN item_tag_cross_ref itcr ON t.id = itcr.tagId
        WHERE itcr.itemId = :itemId
        ORDER BY t.name ASC
    """)
    fun getTagsByItemIdFlow(itemId: Long): Flow<List<TagEntity>>
    
    /**
     * 获取有指定标签的所有物品ID
     */
    @Query("""
        SELECT DISTINCT itcr.itemId FROM item_tag_cross_ref itcr
        INNER JOIN tags t ON itcr.tagId = t.id
        WHERE t.name = :tagName
    """)
    suspend fun getItemIdsByTagName(tagName: String): List<Long>
    
    // === 搜索查询 ===
    
    /**
     * 搜索标签（模糊查询）
     */
    @Query("""
        SELECT * FROM tags 
        WHERE name LIKE '%' || :query || '%'
        ORDER BY name ASC
    """)
    suspend fun searchTags(query: String): List<TagEntity>
    
    /**
     * 获取所有标签名称（去重，用于自动完成）
     */
    @Query("SELECT DISTINCT name FROM tags WHERE name IS NOT NULL AND name != '' ORDER BY name ASC")
    suspend fun getAllTagNames(): List<String>
    
    // === 统计查询 ===
    
    @Query("SELECT COUNT(*) FROM tags")
    suspend fun getTagCount(): Int
    
    /**
     * 获取使用中的标签（有物品关联的标签）
     */
    @Query("""
        SELECT DISTINCT t.* FROM tags t
        INNER JOIN item_tag_cross_ref itcr ON t.id = itcr.tagId
        ORDER BY t.name ASC
    """)
    suspend fun getUsedTags(): List<TagEntity>
    
    /**
     * 获取未使用的标签
     */
    @Query("""
        SELECT t.* FROM tags t
        LEFT JOIN item_tag_cross_ref itcr ON t.id = itcr.tagId
        WHERE itcr.tagId IS NULL
        ORDER BY t.name ASC
    """)
    suspend fun getUnusedTags(): List<TagEntity>
    
    /**
     * 获取标签使用统计
     */
    @Query("""
        SELECT t.name, COUNT(itcr.itemId) as usage_count
        FROM tags t
        LEFT JOIN item_tag_cross_ref itcr ON t.id = itcr.tagId
        GROUP BY t.id, t.name
        ORDER BY usage_count DESC, t.name ASC
    """)
    suspend fun getTagUsageStats(): List<TagUsageStat>
    
    // === ItemTagCrossRef 操作 ===
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTagCrossRef(crossRef: ItemTagCrossRef)
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItemTagCrossRefs(crossRefs: List<ItemTagCrossRef>)
    
    @Delete
    suspend fun deleteItemTagCrossRef(crossRef: ItemTagCrossRef)
    
    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId")
    suspend fun deleteAllItemTagsByItemId(itemId: Long)
    
    @Query("DELETE FROM item_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteAllItemTagsByTagId(tagId: Long)
    
    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId AND tagId = :tagId")
    suspend fun deleteItemTagCrossRef(itemId: Long, tagId: Long)
    
    // === 事务操作 ===
    
    /**
     * 为物品设置标签（替换现有标签）
     */
    @Transaction
    suspend fun setItemTags(itemId: Long, tagNames: List<String>) {
        // 删除现有关联
        deleteAllItemTagsByItemId(itemId)
        
        // 为每个标签名创建或获取标签，并建立关联
        tagNames.forEach { tagName ->
            val tagId = findOrCreateTag(tagName)
            insertItemTagCrossRef(ItemTagCrossRef(itemId, tagId))
        }
    }
    
    /**
     * 为物品添加标签（保留现有标签）
     */
    @Transaction
    suspend fun addItemTags(itemId: Long, tagNames: List<String>) {
        tagNames.forEach { tagName ->
            val tagId = findOrCreateTag(tagName)
            insertItemTagCrossRef(ItemTagCrossRef(itemId, tagId))
        }
    }
    
    /**
     * 查找或创建标签
     */
    @Transaction
    suspend fun findOrCreateTag(name: String, color: String? = null): Long {
        // 先尝试查找现有标签
        val existing = getByName(name)
        if (existing != null) {
            return existing.id
        }
        
        // 创建新标签
        val newTag = TagEntity(
            name = name,
            color = color
        )
        return insert(newTag)
    }
    
    /**
     * 清理未使用的标签
     */
    @Query("""
        DELETE FROM tags 
        WHERE id NOT IN (SELECT DISTINCT tagId FROM item_tag_cross_ref)
    """)
    suspend fun cleanUnusedTags(): Int
}

/**
 * 标签使用统计数据类
 */
data class TagUsageStat(
    val name: String,
    val usage_count: Int
)
