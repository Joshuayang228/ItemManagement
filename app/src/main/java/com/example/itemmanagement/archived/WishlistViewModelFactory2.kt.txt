package com.example.itemmanagement.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.WishlistRepository
/**
 * 心愿单ViewModel工厂类
 * 用于创建WishlistViewModel实例
 */
class WishlistViewModelFactory(
    private val wishlistRepository: WishlistRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WishlistViewModel::class.java)) {
            return WishlistViewModel(wishlistRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
