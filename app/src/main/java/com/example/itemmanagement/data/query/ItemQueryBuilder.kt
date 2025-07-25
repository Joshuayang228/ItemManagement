package com.example.itemmanagement.data.query

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.itemmanagement.ui.warehouse.SortDirection
import com.example.itemmanagement.ui.warehouse.SortOption
import com.example.itemmanagement.ui.warehouse.Season
import com.example.itemmanagement.ui.warehouse.DateType

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
    private var orderByClause: String = "ORDER BY (COALESCE(i.rating, 0) * 0.4 + CASE WHEN i.expirationDate IS NULL THEN 5.0 WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 0 THEN 0.0 WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 86400000 THEN 1.0 WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 604800000 THEN 2.0 WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 2592000000 THEN 3.0 WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 15552000000 THEN 4.0 ELSE 5.0 END * 0.3 + CASE WHEN i.quantity <= 0 THEN 0.0 WHEN i.quantity <= 3 THEN 1.0 WHEN i.quantity <= 10 THEN 3.0 WHEN i.quantity <= 50 THEN 4.0 ELSE 5.0 END * 0.2 + CASE WHEN (strftime('%s', 'now') * 1000 - i.addDate) <= 86400000 THEN 5.0 WHEN (strftime('%s', 'now') * 1000 - i.addDate) <= 604800000 THEN 4.0 WHEN (strftime('%s', 'now') * 1000 - i.addDate) <= 2592000000 THEN 3.0 WHEN (strftime('%s', 'now') * 1000 - i.addDate) <= 15552000000 THEN 2.0 ELSE 1.0 END * 0.1) DESC" // 默认按综合排序降序

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
     * 添加多选评分条件
     */
    fun withRatings(ratings: Set<Float>): ItemQueryBuilder {
        if (ratings.isNotEmpty()) {
            val placeholders = ratings.joinToString(",") { "?" }
            conditions.add("CAST(i.rating AS INTEGER) IN ($placeholders)")
            arguments.addAll(ratings.map { it.toInt() })
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
            arguments.add(if (isOpened) "OPENED" else "UNOPENED")
        }
        return this
    }

    /**
     * 添加多选开封状态条件
     */
    fun withOpenStatuses(openStatuses: Set<Boolean>): ItemQueryBuilder {
        if (openStatuses.isNotEmpty()) {
            val statusStrings = openStatuses.map { if (it) "OPENED" else "UNOPENED" }
            val placeholders = statusStrings.joinToString(",") { "?" }
            conditions.add("i.openStatus IN ($placeholders)")
            arguments.addAll(statusStrings)
        }
        return this
    }

    /**
     * 添加季节条件
     * @param seasons 选中的季节集合
     */
    fun withSeasons(seasons: Set<String>): ItemQueryBuilder {
        if (seasons.isNotEmpty()) {
            val placeholders = seasons.joinToString(",") { "?" }
            conditions.add("i.season IN ($placeholders)")
            arguments.addAll(seasons)
        }
        return this
    }

    /**
     * 添加标签条件
     * @param tags 选中的标签集合
     */
    fun withTags(tags: Set<String>): ItemQueryBuilder {
        if (tags.isNotEmpty()) {
            // 使用EXISTS子查询检查物品是否包含所有选中的标签
            val tagConditions = tags.map {
                "EXISTS (SELECT 1 FROM tags t INNER JOIN item_tag_cross_ref cr ON t.id = cr.tagId WHERE cr.itemId = i.id AND t.name = ?)"
            }
            if (tagConditions.isNotEmpty()) {
                conditions.add("(${tagConditions.joinToString(" AND ")})")
                arguments.addAll(tags)
            }
        }
        return this
    }

    /**
     * 添加日期范围条件
     * @param dateType 日期类型
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    fun withDateRange(dateType: DateType?, startDate: Long?, endDate: Long?): ItemQueryBuilder {
        if (dateType != null && (startDate != null || endDate != null)) {
            val dateColumn = when (dateType) {
                DateType.EXPIRATION -> "i.expirationDate"
                DateType.CREATION -> "i.addDate"
                DateType.PURCHASE -> "i.purchaseDate"  // 假设数据库中有此字段
                DateType.PRODUCTION -> "i.productionDate"  // 假设数据库中有此字段
            }
            
            if (startDate != null) {
                conditions.add("$dateColumn >= ?")
                arguments.add(startDate)
            }
            
            if (endDate != null) {
                conditions.add("$dateColumn <= ?")
                arguments.add(endDate)
            }
        }
        return this
    }
    
    /**
     * 添加过期日期范围条件
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    fun withExpirationDateRange(startDate: Long?, endDate: Long?): ItemQueryBuilder {
        if (startDate != null) {
            conditions.add("i.expirationDate >= ?")
            arguments.add(startDate)
        }
        if (endDate != null) {
            conditions.add("i.expirationDate <= ?")
            arguments.add(endDate)
        }
        return this
    }
    
    /**
     * 添加添加日期范围条件
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    fun withCreationDateRange(startDate: Long?, endDate: Long?): ItemQueryBuilder {
        if (startDate != null) {
            conditions.add("i.addDate >= ?")
            arguments.add(startDate)
        }
        if (endDate != null) {
            conditions.add("i.addDate <= ?")
            arguments.add(endDate)
        }
        return this
    }
    
    /**
     * 添加购买日期范围条件
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    fun withPurchaseDateRange(startDate: Long?, endDate: Long?): ItemQueryBuilder {
        if (startDate != null) {
            conditions.add("i.purchaseDate >= ?")
            arguments.add(startDate)
        }
        if (endDate != null) {
            conditions.add("i.purchaseDate <= ?")
            arguments.add(endDate)
        }
        return this
    }
    
    /**
     * 添加生产日期范围条件
     * @param startDate 开始日期时间戳
     * @param endDate 结束日期时间戳
     */
    fun withProductionDateRange(startDate: Long?, endDate: Long?): ItemQueryBuilder {
        if (startDate != null) {
            conditions.add("i.productionDate >= ?")
            arguments.add(startDate)
        }
        if (endDate != null) {
            conditions.add("i.productionDate <= ?")
            arguments.add(endDate)
        }
        return this
    }

    /**
     * 设置排序方式
     */
    fun sortBy(option: SortOption, direction: SortDirection): ItemQueryBuilder {
        val dir = direction.name
        
        orderByClause = when (option) {
            SortOption.COMPREHENSIVE -> {
                // 综合排序算法：评分(40%) + 剩余保质期(30%) + 数量(20%) + 添加时间(10%)
                val comprehensiveScore = """
                    (
                        COALESCE(i.rating, 0) * 0.4 +
                        CASE 
                            WHEN i.expirationDate IS NULL THEN 5.0
                            WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 0 THEN 0.0
                            WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 86400000 THEN 1.0
                            WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 604800000 THEN 2.0
                            WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 2592000000 THEN 3.0
                            WHEN (i.expirationDate - strftime('%s', 'now') * 1000) <= 15552000000 THEN 4.0
                            ELSE 5.0
                        END * 0.3 +
                        CASE 
                            WHEN i.quantity <= 0 THEN 0.0
                            WHEN i.quantity <= 3 THEN 1.0
                            WHEN i.quantity <= 10 THEN 3.0
                            WHEN i.quantity <= 50 THEN 4.0
                            ELSE 5.0
                        END * 0.2 +
                        CASE 
                            WHEN (strftime('%s', 'now') * 1000 - i.addDate) <= 86400000 THEN 5.0
                            WHEN (strftime('%s', 'now') * 1000 - i.addDate) <= 604800000 THEN 4.0
                            WHEN (strftime('%s', 'now') * 1000 - i.addDate) <= 2592000000 THEN 3.0
                            WHEN (strftime('%s', 'now') * 1000 - i.addDate) <= 15552000000 THEN 2.0
                            ELSE 1.0
                        END * 0.1
                    )
                """.trimIndent()
                "ORDER BY $comprehensiveScore $dir"
            }
            SortOption.QUANTITY -> "ORDER BY i.quantity $dir"
            SortOption.PRICE -> "ORDER BY i.price $dir"
            SortOption.RATING -> "ORDER BY i.rating $dir"
            SortOption.REMAINING_SHELF_LIFE -> {
                // 剩余保质期排序：计算剩余天数
                val remainingDays = """
                    CASE 
                        WHEN i.expirationDate IS NULL THEN 999999
                        ELSE (i.expirationDate - strftime('%s', 'now') * 1000) / 86400000
                    END
                """.trimIndent()
                "ORDER BY $remainingDays $dir"
            }
            SortOption.UPDATE_TIME -> "ORDER BY i.addDate $dir"
        }
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