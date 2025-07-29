package com.example.itemmanagement.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * 购物清单类型枚举
 */
enum class ShoppingListType {
    DAILY,      // 日常补充
    WEEKLY,     // 周采购
    PARTY,      // 聚会准备
    TRAVEL,     // 旅行购物
    SPECIAL,    // 特殊场合
    CUSTOM      // 自定义
}

/**
 * 购物清单状态枚举
 */
enum class ShoppingListStatus {
    ACTIVE,     // 进行中
    COMPLETED,  // 已完成
    ARCHIVED    // 已归档
}

/**
 * 购物清单实体
 */
@Parcelize
@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                                    // 清单名称
    val description: String? = null,                     // 清单描述
    val type: ShoppingListType = ShoppingListType.DAILY, // 清单类型
    val status: ShoppingListStatus = ShoppingListStatus.ACTIVE, // 状态
    val createdDate: Date = Date(),                      // 创建日期
    val targetDate: Date? = null,                        // 目标完成日期
    val estimatedBudget: Double? = null,                 // 预算
    val actualSpent: Double? = null,                     // 实际花费
    val notes: String? = null                            // 备注
) : Parcelable 