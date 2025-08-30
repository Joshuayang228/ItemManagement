package com.example.itemmanagement.ui.borrow

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.dao.BorrowStatistics
import com.example.itemmanagement.data.dao.BorrowWithItemInfo
import com.example.itemmanagement.data.entity.BorrowStatus
import com.example.itemmanagement.data.repository.BorrowRepository
import com.example.itemmanagement.reminder.ReminderScheduler
import kotlinx.coroutines.launch

/**
 * 借还列表ViewModel
 * 管理借还列表页面的数据和业务逻辑
 */
class BorrowListViewModel(
    private val borrowRepository: BorrowRepository,
    private val context: Context // 用于提醒功能测试
) : ViewModel() {

    // ==================== 数据状态 ====================
    
    /**
     * 当前选中的筛选状态
     */
    private val _selectedStatus = MutableLiveData<BorrowStatus?>(null)
    val selectedStatus: LiveData<BorrowStatus?> = _selectedStatus
    
    /**
     * 借还记录列表（根据筛选状态）
     */
    private val _borrowList = MediatorLiveData<List<BorrowWithItemInfo>>()
    val borrowList: LiveData<List<BorrowWithItemInfo>> = _borrowList
    
    /**
     * 统计信息
     */
    private val _statistics = MutableLiveData<BorrowStatistics>()
    val statistics: LiveData<BorrowStatistics> = _statistics
    
    /**
     * 错误消息
     */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    /**
     * 成功消息
     */
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    /**
     * 加载状态
     */
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        // 初始化借还记录列表监听
        setupBorrowListListener()
        
        // 初始加载统计信息
        loadStatistics()
        
        // 更新逾期状态
        updateOverdueRecords()
    }
    
    /**
     * 设置借还记录列表的响应式监听
     */
    private fun setupBorrowListListener() {
        _selectedStatus.observeForever { status ->
            val source = if (status == null) {
                borrowRepository.getAllBorrowsWithItemInfoFlow().asLiveData()
            } else {
                borrowRepository.getBorrowsWithItemInfoByStatusFlow(status).asLiveData()
            }
            
            _borrowList.removeSource(_borrowList)
            _borrowList.addSource(source) { newList ->
                _borrowList.value = newList
            }
        }
    }

    // ==================== 数据操作 ====================
    
    /**
     * 加载统计信息
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val stats = borrowRepository.getStatistics()
                _statistics.postValue(stats)
            } catch (e: Exception) {
                _errorMessage.postValue("加载统计信息失败: ${e.message}")
            }
        }
    }
    
    /**
     * 更新逾期记录状态
     */
    private fun updateOverdueRecords() {
        viewModelScope.launch {
            try {
                borrowRepository.updateOverdueRecords()
            } catch (e: Exception) {
                // 静默处理，不显示错误
            }
        }
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        loadStatistics()
        updateOverdueRecords()
    }

    // ==================== 筛选功能 ====================
    
    /**
     * 设置状态筛选
     * @param status 筛选的状态，null表示显示全部
     */
    fun setStatusFilter(status: BorrowStatus?) {
        if (_selectedStatus.value != status) {
            _selectedStatus.value = status
        }
    }
    
    /**
     * 清除筛选
     */
    fun clearFilter() {
        setStatusFilter(null)
    }
    
    /**
     * 获取筛选选项列表
     */
    fun getFilterOptions(): List<FilterOption> {
        return listOf(
            FilterOption("全部", null, true),
            FilterOption("已借出", BorrowStatus.BORROWED, false),
            FilterOption("已归还", BorrowStatus.RETURNED, false),
            FilterOption("已逾期", BorrowStatus.OVERDUE, false)
        )
    }

    // ==================== 借还操作 ====================
    
    /**
     * 归还物品
     * @param borrowId 借还记录ID
     */
    fun returnItem(borrowId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                val success = borrowRepository.returnItem(borrowId)
                
                if (success) {
                    _successMessage.postValue("归还成功")
                    // 刷新数据
                    loadStatistics()
                } else {
                    _errorMessage.postValue("归还失败，该记录可能已经归还")
                }
            } catch (e: Exception) {
                _errorMessage.postValue("归还失败: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
    
    /**
     * 删除借还记录
     * @param borrowId 借还记录ID
     */
    fun deleteBorrow(borrowId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                borrowRepository.deleteBorrowById(borrowId)
                _successMessage.postValue("删除成功")
                // 刷新数据
                loadStatistics()
            } catch (e: Exception) {
                _errorMessage.postValue("删除失败: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // ==================== 测试功能 ====================
    
    /**
     * 测试借还提醒功能
     */
    fun testBorrowReminders() {
        viewModelScope.launch {
            try {
                val reminderScheduler = ReminderScheduler(context)
                reminderScheduler.sendImmediateReminder()
                _successMessage.postValue("借还提醒测试完成，请检查通知栏")
            } catch (e: Exception) {
                _errorMessage.postValue("测试提醒功能失败: ${e.message}")
            }
        }
    }

    // ==================== 消息处理 ====================
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 清除成功消息
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}

/**
 * 筛选选项数据类
 */
data class FilterOption(
    val name: String,
    val status: BorrowStatus?,
    val isDefault: Boolean = false
)
