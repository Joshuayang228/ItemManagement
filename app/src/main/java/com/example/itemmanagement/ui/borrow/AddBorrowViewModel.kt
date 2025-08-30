package com.example.itemmanagement.ui.borrow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.relation.ItemWithDetails
import com.example.itemmanagement.data.repository.BorrowRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import java.util.Calendar
import java.util.Date

/**
 * 添加借出记录ViewModel
 * 管理添加借出记录页面的数据和业务逻辑
 */
class AddBorrowViewModel(
    private val borrowRepository: BorrowRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    // ==================== 表单数据 ====================
    
    /**
     * 选择的物品
     */
    private val _selectedItem = MutableLiveData<ItemWithDetails?>()
    val selectedItem: LiveData<ItemWithDetails?> = _selectedItem
    
    /**
     * 借用人姓名
     */
    private val _borrowerName = MutableLiveData<String>("")
    val borrowerName: LiveData<String> = _borrowerName
    
    /**
     * 借用人联系方式
     */
    private val _borrowerContact = MutableLiveData<String>("")
    val borrowerContact: LiveData<String> = _borrowerContact
    
    /**
     * 预计归还日期
     */
    private val _expectedReturnDate = MutableLiveData<Date>()
    val expectedReturnDate: LiveData<Date> = _expectedReturnDate
    
    /**
     * 备注
     */
    private val _notes = MutableLiveData<String>("")
    val notes: LiveData<String> = _notes

    // ==================== 可选择的物品列表 ====================
    
    /**
     * 所有可选择的物品
     */
    val availableItems: LiveData<List<ItemWithDetails>> = 
        itemRepository.getAllItemsWithDetails().asLiveData()

    // ==================== 状态管理 ====================
    
    /**
     * 表单验证状态
     */
    private val _isFormValid = MutableLiveData<Boolean>(false)
    val isFormValid: LiveData<Boolean> = _isFormValid
    
    /**
     * 保存结果
     */
    private val _saveResult = MutableLiveData<Boolean?>()
    val saveResult: LiveData<Boolean?> = _saveResult
    
    /**
     * 错误消息
     */
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    /**
     * 加载状态
     */
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        // 设置默认的预计归还日期（7天后）
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        _expectedReturnDate.value = calendar.time
        
        // 初始表单验证
        validateForm()
    }

    // ==================== 表单数据设置 ====================
    
    /**
     * 设置选择的物品
     */
    fun setSelectedItem(item: ItemWithDetails?) {
        _selectedItem.value = item
        validateForm()
    }
    
    /**
     * 设置借用人姓名
     */
    fun setBorrowerName(name: String) {
        _borrowerName.value = name
        validateForm()
    }
    
    /**
     * 设置借用人联系方式
     */
    fun setBorrowerContact(contact: String) {
        _borrowerContact.value = contact
    }
    
    /**
     * 设置预计归还日期
     */
    fun setExpectedReturnDate(date: Date) {
        _expectedReturnDate.value = date
        validateForm()
    }
    
    /**
     * 设置备注
     */
    fun setNotes(notes: String) {
        _notes.value = notes
    }

    // ==================== 表单验证 ====================
    
    /**
     * 验证表单
     */
    private fun validateForm() {
        val isValid = _selectedItem.value != null &&
                     !_borrowerName.value.isNullOrBlank() &&
                     _expectedReturnDate.value != null &&
                     _expectedReturnDate.value!!.after(Date()) // 归还日期必须是未来时间
        
        _isFormValid.value = isValid
    }

    // ==================== 保存操作 ====================
    
    /**
     * 保存借出记录
     */
    fun saveBorrowRecord() {
        // 再次验证表单
        if (_isFormValid.value != true) {
            _errorMessage.value = "请检查表单信息是否完整"
            return
        }
        
        val selectedItem = _selectedItem.value!!
        val borrowerName = _borrowerName.value!!.trim()
        val expectedReturnDate = _expectedReturnDate.value!!
        
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)
                
                // 检查物品是否已经被借出
                val currentBorrow = borrowRepository.getCurrentBorrowRecordForItem(selectedItem.item.id)
                if (currentBorrow != null) {
                    _errorMessage.postValue("该物品已经借出给 ${currentBorrow.borrowerName}，无法重复借出")
                    return@launch
                }
                
                // 创建借出记录
                val borrowId = borrowRepository.createBorrowRecord(
                    itemId = selectedItem.item.id,
                    borrowerName = borrowerName,
                    borrowerContact = _borrowerContact.value?.trim()?.takeIf { it.isNotBlank() },
                    expectedReturnDate = expectedReturnDate,
                    notes = _notes.value?.trim()?.takeIf { it.isNotBlank() }
                )
                
                if (borrowId > 0) {
                    _saveResult.postValue(true)
                    _errorMessage.postValue("借出记录创建成功")
                } else {
                    _errorMessage.postValue("创建借出记录失败")
                    _saveResult.postValue(false)
                }
                
            } catch (e: Exception) {
                _errorMessage.postValue("保存失败: ${e.message}")
                _saveResult.postValue(false)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // ==================== 辅助方法 ====================
    
    /**
     * 清除表单
     */
    fun clearForm() {
        _selectedItem.value = null
        _borrowerName.value = ""
        _borrowerContact.value = ""
        _notes.value = ""
        
        // 重置预计归还日期为7天后
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        _expectedReturnDate.value = calendar.time
        
        validateForm()
    }
    
    /**
     * 预设置选中物品（从其他页面跳转过来时）
     * @param itemId 物品ID
     */
    fun preSelectItem(itemId: Long) {
        viewModelScope.launch {
            try {
                itemRepository.getAllItemsWithDetails().collect { items ->
                    val item = items.find { it.item.id == itemId }
                    if (item != null) {
                        setSelectedItem(item)
                    }
                }
            } catch (e: Exception) {
                // 忽略错误，用户可以手动选择
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 清除保存结果
     */
    fun clearSaveResult() {
        _saveResult.value = null
    }
}
