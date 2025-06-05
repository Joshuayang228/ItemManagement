package com.example.itemmanagement.data

import java.util.Date

data class Item(
    val id: Long,
    val name: String,
    val imageUrl: String,
    val quantity: Int,
    val addDate: Date
)