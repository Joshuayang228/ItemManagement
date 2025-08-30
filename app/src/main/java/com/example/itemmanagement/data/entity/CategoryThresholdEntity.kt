package com.example.itemmanagement.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "category_thresholds")
data class CategoryThresholdEntity(
    @PrimaryKey val category: String,             // 分类名称（主键）
    val minQuantity: Double = 1.0,                // 最小数量阈值
    val enabled: Boolean = true,                  // 是否启用此分类的库存提醒
    val unit: String = "个",                      // 推荐单位（用于显示）
    val description: String = "",                 // 分类描述
    val createdAt: Date = Date(),                 // 创建时间
    val updatedAt: Date = Date()                  // 更新时间
)
