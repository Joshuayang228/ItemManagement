package com.example.itemmanagement.data.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 临时的ShoppingItemEntity类，用于解决Navigation Args的编译错误
 * TODO: 迁移Navigation参数到使用统一架构的类型
 */
@Parcelize
data class ShoppingItemEntity(
    val id: Long = 0,
    val name: String = "",
    val category: String = "",
    val quantity: Double = 1.0,
    val quantityUnit: String = "个",
    val priority: ShoppingItemPriority = ShoppingItemPriority.NORMAL,
    val urgencyLevel: UrgencyLevel = UrgencyLevel.NORMAL,
    val estimatedPrice: Double? = null,
    val actualPrice: Double? = null,
    val priceUnit: String = "元",
    val budgetLimit: Double? = null,
    val preferredStore: String? = null,
    val isPurchased: Boolean = false,
    val isRecurring: Boolean = false,
    val recurringInterval: Int? = null,
    val addedReason: String = "USER_MANUAL",
    val customNote: String? = null,
    val brand: String? = null
) : Parcelable
