package com.example.itemmanagement.ui.warranty

import androidx.lifecycle.*
import com.example.itemmanagement.data.repository.WarrantyRepository
import com.example.itemmanagement.data.dao.WarrantyWithItemInfo
import com.example.itemmanagement.reminder.ReminderScheduler
import android.content.Context
import com.example.itemmanagement.data.entity.WarrantyEntity
import com.example.itemmanagement.data.entity.WarrantyStatus
import kotlinx.coroutines.launch
/**
 * 保修列表页面的ViewModel
 * 负责管理保修列表的数据和状态
 */
class WarrantyListViewModel(
    private val warrantyRepository: WarrantyRepository,
    private val context: Context
) : ViewModel() {

    // ==================== UI状态管理 ====================
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _deleteResult = MutableLiveData<Boolean>()
    val deleteResult: LiveData<Boolean> = _deleteResult

    // ==================== 保修数据 ====================
    
    /**
     * 包含物品信息的保修记录列表
     * 实时更新的数据流
     */
    val warrantiesWithItemInfo: LiveData<List<WarrantyWithItemInfo>> = 
        warrantyRepository.getWarrantiesWithItemInfoStream().asLiveData()
    
    /**
     * 保修概览统计数据
     * Triple<总数, 有效数, 即将到期数>
     */
    private val _warrantyOverview = MutableLiveData<com.example.itemmanagement.data.repository.WarrantyOverview>()
    val warrantyOverview: LiveData<com.example.itemmanagement.data.repository.WarrantyOverview> = _warrantyOverview
    
    /**
     * 搜索结果
     */
    private val _searchResults = MutableLiveData<List<WarrantyEntity>>()
    val searchResults: LiveData<List<WarrantyEntity>> = _searchResults
    
    /**
     * 搜索状态
     */
    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching

    // ==================== 筛选状态 ====================
    
    /**
     * 当前筛选的状态集合（支持多选）
     */
    private val _filterStatuses = MutableLiveData<Set<WarrantyStatus>>(emptySet())
    val filterStatuses: LiveData<Set<WarrantyStatus>> = _filterStatuses
    
    /**
     * 当前筛选的状态（单选模式，向后兼容）
     */
    val filterStatus: LiveData<WarrantyStatus?> = _filterStatuses.map { statuses ->
        statuses.firstOrNull()
    }
    
    /**
     * 筛选后的保修列表
     */
    val filteredWarranties: MediatorLiveData<List<WarrantyWithItemInfo>> = MediatorLiveData()

    init {
        // 初始化筛选逻辑
        setupFilterLogic()
        
        // 加载概览数据
        loadWarrantyOverview()
        
        // 定期更新过期状态
        updateExpiredWarranties()
    }

    // ==================== 数据操作方法 ====================
    
    /**
     * 加载保修概览数据
     */
    fun loadWarrantyOverview() {
        viewModelScope.launch {
            try {
                _loading.value = true
                val overview = warrantyRepository.getWarrantyOverview()
                _warrantyOverview.value = overview
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "加载数据失败：${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 搜索保修记录
     * @param query 搜索关键词（物品名称）
     */
    fun searchWarranties(query: String) {
        if (query.trim().isEmpty()) {
            clearSearch()
            return
        }
        
        viewModelScope.launch {
            try {
                _isSearching.value = true
                val results = warrantyRepository.searchWarrantiesByItemName(query.trim())
                _searchResults.value = results
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "搜索失败：${e.message}"
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    /**
     * 清除搜索结果
     */
    fun clearSearch() {
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
    
    /**
     * 设置状态筛选（单选模式）
     */
    fun setStatusFilter(status: WarrantyStatus?) {
        _filterStatuses.value = if (status != null) setOf(status) else emptySet()
    }
    
    /**
     * 设置多个状态筛选（多选模式）
     */
    fun setStatusFilters(statuses: Set<WarrantyStatus>) {
        _filterStatuses.value = statuses
    }
    
    /**
     * 清除所有筛选
     */
    fun clearAllFilters() {
        _filterStatuses.value = emptySet()
    }
    
    /**
     * 删除保修记录
     */
    fun deleteWarranty(warranty: WarrantyEntity) {
        viewModelScope.launch {
            try {
                _loading.value = true
                warrantyRepository.deleteWarranty(warranty)
                _deleteResult.value = true
                _errorMessage.value = null
                
                // 重新加载概览数据
                loadWarrantyOverview()
            } catch (e: Exception) {
                _deleteResult.value = false
                _errorMessage.value = "删除失败：${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * 批量更新过期的保修状态
     */
    fun updateExpiredWarranties() {
        viewModelScope.launch {
            try {
                val updatedCount = warrantyRepository.updateExpiredWarranties()
                if (updatedCount > 0) {
                    // 如果有状态更新，刷新概览数据
                    loadWarrantyOverview()
                }
            } catch (e: Exception) {
                // 静默处理错误，不影响主要功能
                android.util.Log.w("WarrantyListViewModel", "更新过期状态失败: ${e.message}")
            }
        }
    }
    
    /**
     * 获取即将到期的保修记录
     * @param days 提前天数，默认30天
     */
    fun getWarrantiesNearingExpiration(days: Int = 30) {
        viewModelScope.launch {
            try {
                val nearExpiration = warrantyRepository.getWarrantiesNearingExpiration(days)
                // 可以通过其他LiveData暴露这个数据，或者直接用于通知
                android.util.Log.d("WarrantyListViewModel", "即将到期的保修记录数量: ${nearExpiration.size}")
            } catch (e: Exception) {
                android.util.Log.e("WarrantyListViewModel", "获取即将到期保修失败", e)
            }
        }
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 设置筛选逻辑
     * 监听原始数据和筛选条件的变化，自动更新筛选结果
     */
    private fun setupFilterLogic() {
        filteredWarranties.addSource(warrantiesWithItemInfo) { warranties ->
            applyFilters(warranties)
        }
        
        filteredWarranties.addSource(_filterStatuses) { _ ->
            warrantiesWithItemInfo.value?.let { warranties ->
                applyFilters(warranties)
            }
        }
    }
    
    /**
     * 应用筛选条件
     */
    private fun applyFilters(originalList: List<WarrantyWithItemInfo>) {
        var filtered = originalList
        
        // 按状态筛选（多选）
        val statuses = _filterStatuses.value
        if (!statuses.isNullOrEmpty()) {
            filtered = filtered.filter { it.status in statuses }
        }
        
        filteredWarranties.value = filtered
    }
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 重新加载所有数据
     */
    fun refreshData() {
        loadWarrantyOverview()
        updateExpiredWarranties()
    }
    
    /**
     * 测试保修提醒功能（立即触发）
     */
    fun testWarrantyReminders() {
        viewModelScope.launch {
            try {
                // 获取应用实例并立即发送提醒
                val reminderScheduler = ReminderScheduler(context)
                reminderScheduler.sendImmediateReminder()
                _errorMessage.postValue("保修提醒测试完成，请检查通知栏")
            } catch (e: Exception) {
                _errorMessage.postValue("测试提醒功能失败: ${e.message}")
            }
        }
    }
}
