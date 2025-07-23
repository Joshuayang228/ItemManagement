package com.example.itemmanagement.data.query

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.itemmanagement.ui.warehouse.SortDirection
import com.example.itemmanagement.ui.warehouse.SortOption

/**
 * 物品查询构建器，负责构建动态SQL查询
 * 使用建造者模式，支持链式调用
 */
class ItemQueryBuilder {

    // 基础查询，使用LEFT JOIN按需连接表，确保列名与WarehouseItem属性名匹配
    private val baseQuery = """
        SELECT 
            i.id, 
            i.name, 
            CAST(i.quantity AS INTEGER) AS quantity, 
            i.expirationDate, 
            i.category, 
            i.subCategory, 
            i.brand,
            CAST(i.rating AS REAL) AS rating, 
            i.price, 
            i.priceUnit, 
            i.openStatus, 
            i.addDate, 
            i.customNote,
            l.area AS locationArea, 
            l.container AS locationContainer, 
            l.sublocation AS locationSublocation,
            (SELECT uri FROM photos WHERE itemId = i.id ORDER BY displayOrder ASC, id ASC LIMIT 1) AS primaryPhotoUri,
            (SELECT GROUP_CONCAT(t.name) FROM tags t 
             INNER JOIN item_tag_cross_ref cr ON t.id = cr.tagId 
             WHERE cr.itemId = i.id) AS tagsList
        FROM items AS i
        LEFT JOIN locations AS l ON i.locationId = l.id
    """.trimIndent()

    private val conditions = mutableListOf<String>()
    private val arguments = mutableListOf<Any>()
    private var orderByClause: String = "ORDER BY i.addDate DESC" // 默认按添加日期降序

    /**
     * 添加搜索词条件
     */
    fun withSearchTerm(term: String): ItemQueryBuilder {
        if (term.isNotBlank()) {
            conditions.add("(i.name LIKE ? OR i.customNote LIKE ? OR i.brand LIKE ?)")
            val searchTerm = "%$term%"
            arguments.add(searchTerm)
            arguments.add(searchTerm)
            arguments.add(searchTerm)
        }
        return this
    }

    /**
     * 添加分类条件
     */
    fun withCategory(category: String): ItemQueryBuilder {
        if (category.isNotBlank()) {
            conditions.add("i.category = ?")
            arguments.add(category)
        }
        return this
    }

    /**
     * 添加子分类条件
     */
    fun withSubCategory(subCategory: String): ItemQueryBuilder {
        if (subCategory.isNotBlank()) {
            conditions.add("i.subCategory = ?")
            arguments.add(subCategory)
        }
        return this
    }

    /**
     * 添加品牌条件
     */
    fun withBrand(brand: String): ItemQueryBuilder {
        if (brand.isNotBlank()) {
            conditions.add("i.brand = ?")
            arguments.add(brand)
        }
        return this
    }

    /**
     * 添加位置区域条件
     */
    fun withLocationArea(area: String): ItemQueryBuilder {
        if (area.isNotBlank()) {
            conditions.add("l.area = ?")
            arguments.add(area)
        }
        return this
    }

    /**
     * 添加位置容器条件
     */
    fun withLocationContainer(container: String): ItemQueryBuilder {
        if (container.isNotBlank()) {
            conditions.add("l.container = ?")
            arguments.add(container)
        }
        return this
    }

    /**
     * 添加位置子位置条件
     */
    fun withLocationSublocation(sublocation: String): ItemQueryBuilder {
        if (sublocation.isNotBlank()) {
            conditions.add("l.sublocation = ?")
            arguments.add(sublocation)
        }
        return this
    }

    /**
     * 添加数量范围条件
     */
    fun withQuantityRange(min: Int?, max: Int?): ItemQueryBuilder {
        if (min != null) {
            conditions.add("i.quantity >= ?")
            arguments.add(min)
        }
        if (max != null) {
            conditions.add("i.quantity <= ?")
            arguments.add(max)
        }
        return this
    }

    /**
     * 添加评分范围条件
     */
    fun withRatingRange(min: Float?, max: Float?): ItemQueryBuilder {
        if (min != null) {
            conditions.add("i.rating >= ?")
            arguments.add(min)
        }
        if (max != null) {
            conditions.add("i.rating <= ?")
            arguments.add(max)
        }
        return this
    }

    /**
     * 添加价格范围条件
     */
    fun withPriceRange(min: Double?, max: Double?): ItemQueryBuilder {
        if (min != null) {
            conditions.add("i.price >= ?")
            arguments.add(min)
        }
        if (max != null) {
            conditions.add("i.price <= ?")
            arguments.add(max)
        }
        return this
    }

    /**
     * 添加开封状态条件
     */
    fun withOpenStatus(isOpened: Boolean?): ItemQueryBuilder {
        if (isOpened != null) {
            conditions.add("i.openStatus = ?")
            arguments.add(if (isOpened) 1 else 0)
        }
        return this
    }

    /**
     * 设置排序方式
     */
    fun sortBy(option: SortOption, direction: SortDirection): ItemQueryBuilder {
        val column = when (option) {
            SortOption.NAME -> "i.name"
            SortOption.EXPIRATION_DATE -> "i.expirationDate"
            SortOption.UPDATE_TIME -> "i.addDate" // 使用 addDate 代替 updateTime
            SortOption.QUANTITY -> "i.quantity"
            SortOption.PRICE -> "i.price"
            SortOption.RATING -> "i.rating"
        }
        val dir = direction.name
        orderByClause = "ORDER BY $column $dir"
        return this
    }

    /**
     * 构建最终的查询对象
     */
    fun build(): SupportSQLiteQuery {
        val whereClause = if (conditions.isNotEmpty()) {
            conditions.joinToString(separator = " AND ", prefix = "WHERE ")
        } else {
            ""
        }
        
        val finalSql = "$baseQuery $whereClause $orderByClause"
        
        return SimpleSQLiteQuery(finalSql, arguments.toTypedArray())
    }
} 