package com.example.itemmanagement.data.entity.unified

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * 统一物品实体 - 统一架构的核心表
 * 存储所有物品的基础信息，不区分状态
 */
@Entity(tableName = "unified_items")
data class UnifiedItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** 物品名称 */
    val name: String,
    
    /** 分类 */
    val category: String,
    
    /** 子分类（可选）*/
    val subCategory: String? = null,
    
    /** 品牌（可选）*/
    val brand: String? = null,
    
    /** 规格说明（可选）*/
    val specification: String? = null,
    
    /** 自定义备注（可选）*/
    val customNote: String? = null,
    
    // === 物品固有属性（从详情表提升）===
    /** 容量/规格数值 */
    val capacity: Double? = null,
    
    /** 容量单位 */
    val capacityUnit: String? = null,
    
    /** 物品评分（0-5星）*/
    val rating: Double? = null,
    
    /** 适用季节 */
    val season: String? = null,
    
    /** 序列号/SKU */
    val serialNumber: String? = null,
    
    /** 地点地址 */
    val locationAddress: String? = null,
    
    /** 地点纬度 */
    val locationLatitude: Double? = null,
    
    /** 地点经度 */
    val locationLongitude: Double? = null,
    
    /** 创建时间 */
    val createdDate: Date = Date(),
    
    /** 最后更新时间 */
    val updatedDate: Date = Date()
)

