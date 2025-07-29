package com.example.itemmanagement.data.model

data class InventoryStats(
    val totalItems: Int,
    val totalValue: Double,
    val expiringItems: Int,
    val expiredItems: Int,
    val lowStockItems: Int,
    val categoriesCount: Int = 0,
    val locationsCount: Int = 0,
    val wishlistItems: Int = 0,
    val recentlyAddedItems: Int = 0
) 