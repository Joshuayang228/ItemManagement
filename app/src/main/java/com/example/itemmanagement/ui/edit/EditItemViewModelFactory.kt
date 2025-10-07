package com.example.itemmanagement.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.UnifiedItemRepository
import com.example.itemmanagement.data.repository.WarrantyRepository
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel

/**
 * EditItemViewModel 的工厂类
 */
class EditItemViewModelFactory(
    private val repository: UnifiedItemRepository,
    private val cacheViewModel: ItemStateCacheViewModel,
    private val itemId: Long,
    private val warrantyRepository: WarrantyRepository? = null  // ✅ 添加warrantyRepository参数
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditItemViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditItemViewModel(repository, cacheViewModel, itemId, warrantyRepository) as T  // ✅ 传入warrantyRepository
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}