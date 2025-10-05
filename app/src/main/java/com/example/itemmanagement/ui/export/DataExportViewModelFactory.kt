package com.example.itemmanagement.ui.export

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.BorrowRepository
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.WarrantyRepository

/**
 * 数据导出ViewModel工厂类（基于统一架构）
 */
class DataExportViewModelFactory(
    private val application: Application,
    private val itemRepository: UnifiedItemRepository,
    private val warrantyRepository: WarrantyRepository? = null,
    private val borrowRepository: BorrowRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataExportViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DataExportViewModel(application, itemRepository, warrantyRepository, borrowRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

