package com.example.itemmanagement.ui.export

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.repository.BorrowRepository
import com.example.itemmanagement.data.repository.WarrantyRepository

/**
 * DataExportViewModel的工厂类
 */
class DataExportViewModelFactory(
    private val application: Application,
    private val itemRepository: ItemRepository,
    private val warrantyRepository: WarrantyRepository? = null,
    private val borrowRepository: BorrowRepository? = null
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataExportViewModel::class.java)) {
            return DataExportViewModel(
                application,
                itemRepository,
                warrantyRepository,
                borrowRepository
            ) as T
        }
        throw IllegalArgumentException("未知的ViewModel类: ${modelClass.name}")
    }
}
