package com.example.itemmanagement.ui.expiration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.UnifiedItemRepository

/**
 * 过期提醒ViewModel工厂类（基于统一架构）
 */
class ExpirationReminderViewModelFactory(
    private val repository: UnifiedItemRepository,
    private val settingsRepository: com.example.itemmanagement.data.repository.ReminderSettingsRepository,
    private val reminderManager: com.example.itemmanagement.reminder.ReminderManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpirationReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpirationReminderViewModel(repository, settingsRepository, reminderManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
