package com.example.itemmanagement.data.model

/**
 * 轻量级数据类，专门用于仓库列表页显示，避免过度获取数据
 */
data class WarehouseItem(
    val id: Long,
    val name: String,
    val primaryPhotoUri: String?, // 列表只显示主图
    val quantity: Int,
    val expirationDate: Long?,    // 用于计算保质期状态
    
    // 位置信息 - 分开存储各级位置
    val locationArea: String?,    // 位置区域，如"客厅"
    val locationContainer: String?, // 位置容器，如"收纳柜"
    val locationSublocation: String?, // 位置子位置，如"第二层"
    
    val category: String?,        // 分类
    val subCategory: String?,     // 子分类
    val brand: String?,           // 品牌
    
    // 新增字段
    val rating: Float?,           // 评分
    val price: Double?,           // 价格
    val priceUnit: String?,       // 价格单位
    val openStatus: Boolean?,     // 开封状态
    val addDate: Long?,           // 添加日期
    
    // 标签 - 存储为逗号分隔的字符串，最多显示3个
    val tagsList: String? = null, // 从数据库查询的标签列表字符串
    
    // 其他可能需要的字段
    val customNote: String? = null, // 自定义备注
    val season: String? = null // 季节信息，用于筛选
) 