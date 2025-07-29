package com.example.itemmanagement

import android.app.Application
import com.example.itemmanagement.data.AppDatabase
import com.example.itemmanagement.data.ItemRepository
class ItemManagementApplication : Application() {
    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ItemRepository(database.itemDao(), database) }
} 