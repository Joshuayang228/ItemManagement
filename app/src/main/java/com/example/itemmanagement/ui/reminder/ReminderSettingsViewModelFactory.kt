package com.example.itemmanagement.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.data.repository.ReminderSettingsRepository

/**
 * 提醒设置ViewModel工厂类
 */
class ReminderSettingsViewModelFactory(
    private val reminderSettingsRepository: ReminderSettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderSettingsViewModel(reminderSettingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

