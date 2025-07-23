package com.example.itemmanagement.ui.warehouse

/**
 * 排序选项枚举
 */
enum class SortOption(val displayName: String) {
    NAME("名称"),
    EXPIRATION_DATE("保质期"),
    UPDATE_TIME("添加时间"),
    QUANTITY("数量"),
    PRICE("价格"),
    RATING("评分")
}

/**
 * 排序方向枚举
 */
enum class SortDirection(val displayName: String) {
    ASC("升序"),
    DESC("降序")
}

/**
 * 筛选状态数据类，包含所有筛选和排序条件
 */
data class FilterState(
    val searchTerm: String = "",
    val category: String = "",
    val subCategory: String = "",
    val brand: String = "",
    val locationArea: String = "",
    val container: String = "",
    val sublocation: String = "",
    val minQuantity: Int? = null,
    val maxQuantity: Int? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minRating: Float? = null,
    val maxRating: Float? = null,
    val openStatus: Boolean? = null,
    val sortOption: SortOption = SortOption.UPDATE_TIME,
    val sortDirection: SortDirection = SortDirection.DESC
) 