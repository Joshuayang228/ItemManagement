package com.example.itemmanagement.data.model

data class FunctionSection(
    val id: String,
    val title: String,
    val description: String,
    val iconResId: Int,
    val functions: List<FunctionCard>
) 