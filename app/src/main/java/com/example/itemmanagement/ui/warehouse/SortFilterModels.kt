package com.example.itemmanagement.ui.warehouse

/**
 * 排序选项枚举
 */
enum class SortOption(val displayName: String) {
    COMPREHENSIVE("综合"),
    QUANTITY("数量"),
    PRICE("单价"),
    RATING("评分"),
    REMAINING_SHELF_LIFE("剩余保质期"),
    UPDATE_TIME("添加时间")
}

/**
 * 排序方向枚举
 */
enum class SortDirection(val displayName: String) {
    ASC("升序"),
    DESC("降序")
}

/**
 * 季节枚举
 */
enum class Season(val displayName: String) {
    SPRING("春"),
    SUMMER("夏"),
    AUTUMN("秋"),
    WINTER("冬")
}

/**
 * 日期类型枚举 - 保留用于其他地方的兼容性
 */
enum class DateType(val displayName: String, val fieldName: String) {
    EXPIRATION("过期日期", "expirationDate"),
    CREATION("添加日期", "createdAt"),
    PURCHASE("购买日期", "purchaseDate"),
    PRODUCTION("生产日期", "productionDate")
}

/**
 * 筛选导航类别
 */
enum class FilterCategory(val displayName: String) {
    CORE("核心分类"),
    LOCATION("位置筛选"),
    STATUS_RATING("状态与评级"),
    VALUE_RANGE("数值范围"),
    DATE("日期筛选")
}

/**
 * 筛选状态数据类，包含所有筛选和排序条件
 */
data class FilterState(
    val searchTerm: String = "",
    
    // 核心分类
    val category: String = "",
    val subCategory: String = "",
    val brand: String = "",
    
    // 位置筛选
    val locationArea: String = "",
    val container: String = "",
    val sublocation: String = "",
    
    // 状态与评级
    val openStatus: Boolean? = null, // 向后兼容
    val openStatuses: Set<Boolean> = emptySet(), // 多选开封状态
    val minRating: Float? = null,
    val maxRating: Float? = null,
    val ratings: Set<Float> = emptySet(), // 多选评分
    val seasons: Set<String> = emptySet(), // 多选季节（支持动态）
    val tags: Set<String> = emptySet(), // 多选标签
    
    // 数值范围
    val minQuantity: Int? = null,
    val maxQuantity: Int? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    
    // 日期筛选 - 拆分为独立的日期范围
    val expirationStartDate: Long? = null,
    val expirationEndDate: Long? = null,
    val creationStartDate: Long? = null,
    val creationEndDate: Long? = null,
    val purchaseStartDate: Long? = null,
    val purchaseEndDate: Long? = null,
    val productionStartDate: Long? = null,
    val productionEndDate: Long? = null,
    
    // 排序
    val sortOption: SortOption = SortOption.COMPREHENSIVE,
    val sortDirection: SortDirection = SortDirection.DESC
) {
    // 兼容性方法 - 保持对原有代码的兼容
    @Deprecated("使用具体的日期范围字段", ReplaceWith("expirationStartDate"))
    val startDate: Long? get() = expirationStartDate
    
    @Deprecated("使用具体的日期范围字段", ReplaceWith("expirationEndDate"))
    val endDate: Long? get() = expirationEndDate
    
    @Deprecated("使用具体的日期范围字段", ReplaceWith("DateType.EXPIRATION"))
    val dateType: DateType? get() = if (expirationStartDate != null || expirationEndDate != null) DateType.EXPIRATION else null
} 