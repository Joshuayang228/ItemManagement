package com.example.itemmanagement.ui.profile.recycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.RecycleBinRepository

/**
 * 回收站ViewModel的工厂类
 */
class RecycleBinViewModelFactory(
    private val recycleBinRepository: RecycleBinRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecycleBinViewModel::class.java)) {
            return RecycleBinViewModel(recycleBinRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
