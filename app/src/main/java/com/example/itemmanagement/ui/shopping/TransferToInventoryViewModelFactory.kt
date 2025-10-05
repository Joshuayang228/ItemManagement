package com.example.itemmanagement.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel

/**
 * TransferToInventoryViewModel 的工厂类
 * 
 * 用于创建带参数的 TransferToInventoryViewModel 实例
 */
class TransferToInventoryViewModelFactory(
    private val repository: UnifiedItemRepository,
    private val cacheViewModel: ItemStateCacheViewModel,
    private val sourceItemId: Long
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransferToInventoryViewModel::class.java)) {
            return TransferToInventoryViewModel(repository, cacheViewModel, sourceItemId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
