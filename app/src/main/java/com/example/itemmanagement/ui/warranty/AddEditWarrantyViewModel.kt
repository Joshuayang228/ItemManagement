package com.example.itemmanagement.ui.warranty

import android.net.Uri
import androidx.lifecycle.*
import com.example.itemmanagement.data.ItemRepository
import com.example.itemmanagement.data.repository.WarrantyRepository
import com.example.itemmanagement.data.entity.WarrantyEntity
import com.example.itemmanagement.data.entity.WarrantyStatus
import com.example.itemmanagement.data.relation.ItemWithDetails
import kotlinx.coroutines.launch
import java.util.*

/**
 * 添加/编辑保修信息页面的ViewModel
 * 支持新建保修记录和编辑现有保修记录
 */
class AddEditWarrantyViewModel(
    private val warrantyRepository: WarrantyRepository,
    private val itemRepository: ItemRepository
) : ViewModel() {

    // ==================== 编辑模式管理 ====================
    
    /**
     * 当前编辑的保修ID（新建时为null）
     */
    private var currentWarrantyId: Long? = null
    
    /**
     * 是否为编辑模式
     */
    private val _isEditMode = MutableLiveData<Boolean>(false)
    val isEditMode: LiveData<Boolean> = _isEditMode

    // ==================== UI状态管理 ====================
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // ==================== 表单数据 ====================
    
    /**
     * 选中的物品
     */
    private val _selectedItem = MutableLiveData<ItemWithDetails?>()
    val selectedItem: LiveData<ItemWithDetails?> = _selectedItem
    
    /**
     * 购买日期
     */
    private val _purchaseDate = MutableLiveData<Date>()
    val purchaseDate: LiveData<Date> = _purchaseDate
    
    /**
     * 保修期（月数）
     */
    private val _warrantyPeriodMonths = MutableLiveData<Int>()
    val warrantyPeriodMonths: LiveData<Int> = _warrantyPeriodMonths
    
    /**
     * 保修到期日期（自动计算）
     */
    private val _warrantyEndDate = MutableLiveData<Date>()
    val warrantyEndDate: LiveData<Date> = _warrantyEndDate
    
    /**
     * 凭证图片URI列表
     */
    private val _receiptImageUris = MutableLiveData<List<Uri>>(emptyList())
    val receiptImageUris: LiveData<List<Uri>> = _receiptImageUris
    
    /**
     * 备注
     */
    private val _notes = MutableLiveData<String?>()
    val notes: LiveData<String?> = _notes
    
    /**
     * 保修状态
     */
    private val _status = MutableLiveData<WarrantyStatus>(WarrantyStatus.ACTIVE)
    val status: LiveData<WarrantyStatus> = _status
    
    /**
     * 保修服务提供商
     */
    private val _warrantyProvider = MutableLiveData<String?>()
    val warrantyProvider: LiveData<String?> = _warrantyProvider
    
    /**
     * 联系方式
     */
    private val _contactInfo = MutableLiveData<String?>()
    val contactInfo: LiveData<String?> = _contactInfo

    // ==================== 辅助数据 ====================
    
    /**
     * 可选择的物品列表
     */
    private val _availableItems = MutableLiveData<List<ItemWithDetails>>()
    val availableItems: LiveData<List<ItemWithDetails>> = _availableItems
    
    /**
     * 表单验证状态
     */
    private val _isFormValid = MutableLiveData<Boolean>(false)
    val isFormValid: LiveData<Boolean> = _isFormValid

    init {
        // 设置默认值
        _purchaseDate.value = Date()
        _warrantyPeriodMonths.value = 12 // 默认12个月
        
        // 加载可选择的物品
        loadAvailableItems()
        
        // 设置表单验证监听
        setupFormValidation()
        
        // 设置保修到期日期自动计算
        setupWarrantyEndDateCalculation()
    }

    // ==================== 数据初始化方法 ====================
    
    /**
     * 初始化新建模式
     * @param preSelectedItemId 预选择的物品ID（可选）
     */
    fun initializeForAdd(preSelectedItemId: Long? = null) {
        _isEditMode.value = false
        currentWarrantyId = null
        
        preSelectedItemId?.let { itemId ->
            // 如果有预选择的物品，设置为选中状态
            viewModelScope.launch {
                try {
                    val item = itemRepository.getItemWithDetailsById(itemId)
                    if (item != null) {
                        _selectedItem.value = item
                    }
                } catch (e: Exception) {
                    _errorMessage.value = "加载物品信息失败：${e.message}"
                }
            }
        }
    }
    
    /**
     * 初始化编辑模式
     * @param warrantyId 要编辑的保修记录ID
     */
    fun initializeForEdit(warrantyId: Long) {
        _isEditMode.value = true
        currentWarrantyId = warrantyId
        
        viewModelScope.launch {
            try {
                _loading.value = true
                val warranty = warrantyRepository.getWarrantyById(warrantyId)
                if (warranty != null) {
                    loadWarrantyDataToForm(warranty)
                } else {
                    _errorMessage.value = "保修记录不存在"
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载保修信息失败：${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // ==================== 表单数据设置方法 ====================
    
    /**
     * 设置选中的物品
     */
    fun setSelectedItem(item: ItemWithDetails?) {
        _selectedItem.value = item
    }
    
    /**
     * 设置购买日期
     */
    fun setPurchaseDate(date: Date) {
        _purchaseDate.value = date
    }
    
    /**
     * 设置保修期
     */
    fun setWarrantyPeriodMonths(months: Int) {
        _warrantyPeriodMonths.value = months
    }
    
    /**
     * 设置备注
     */
    fun setNotes(notes: String?) {
        _notes.value = notes
    }
    
    /**
     * 设置保修状态
     */
    fun setStatus(status: WarrantyStatus) {
        _status.value = status
    }
    
    /**
     * 设置保修服务提供商
     */
    fun setWarrantyProvider(provider: String?) {
        _warrantyProvider.value = provider
    }
    
    /**
     * 设置联系方式
     */
    fun setContactInfo(contactInfo: String?) {
        _contactInfo.value = contactInfo
    }
    
    /**
     * 添加凭证图片
     */
    fun addReceiptImage(uri: Uri) {
        val current = _receiptImageUris.value ?: emptyList()
        _receiptImageUris.value = current + uri
    }
    
    /**
     * 移除凭证图片
     */
    fun removeReceiptImage(uri: Uri) {
        val current = _receiptImageUris.value ?: emptyList()
        _receiptImageUris.value = current.filter { it != uri }
    }
    
    /**
     * 设置凭证图片列表
     */
    fun setReceiptImages(uris: List<Uri>) {
        _receiptImageUris.value = uris
    }

    // ==================== 保存操作 ====================
    
    /**
     * 保存保修记录
     */
    fun saveWarranty() {
        if (!isFormValid.value!!) {
            _errorMessage.value = "请填写完整的保修信息"
            return
        }
        
        viewModelScope.launch {
            try {
                _loading.value = true
                
                val selectedItem = _selectedItem.value!!
                val purchaseDate = _purchaseDate.value!!
                val warrantyPeriod = _warrantyPeriodMonths.value!!
                val warrantyEndDate = _warrantyEndDate.value!!
                
                // 转换图片URI列表为JSON字符串
                val receiptUrisJson = _receiptImageUris.value?.takeIf { it.isNotEmpty() }?.let { uris ->
                    com.google.gson.Gson().toJson(uris.map { it.toString() })
                }
                
                if (_isEditMode.value == true && currentWarrantyId != null) {
                    // 编辑模式：更新现有记录
                    val updatedWarranty = WarrantyEntity(
                        id = currentWarrantyId!!,
                        itemId = selectedItem.item.id,
                        purchaseDate = purchaseDate,
                        warrantyPeriodMonths = warrantyPeriod,
                        warrantyEndDate = warrantyEndDate,
                        receiptImageUris = receiptUrisJson,
                        notes = _notes.value,
                        status = _status.value!!,
                        warrantyProvider = _warrantyProvider.value,
                        contactInfo = _contactInfo.value,
                        createdDate = Date(), // 这个字段在更新时会被忽略
                        updatedDate = Date()
                    )
                    
                    warrantyRepository.updateWarranty(updatedWarranty)
                } else {
                    // 新建模式：创建新记录
                    warrantyRepository.createWarranty(
                        itemId = selectedItem.item.id,
                        purchaseDate = purchaseDate,
                        warrantyPeriodMonths = warrantyPeriod,
                        receiptImageUris = _receiptImageUris.value?.map { it.toString() },
                        notes = _notes.value,
                        warrantyProvider = _warrantyProvider.value,
                        contactInfo = _contactInfo.value
                    )
                }
                
                _saveResult.value = true
                _errorMessage.value = null
            } catch (e: Exception) {
                _saveResult.value = false
                _errorMessage.value = "保存失败：${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    // ==================== 私有辅助方法 ====================
    
    /**
     * 加载可选择的物品列表
     */
    private fun loadAvailableItems() {
        viewModelScope.launch {
            try {
                itemRepository.getAllItemsWithDetails().collect { items ->
                    _availableItems.value = items
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载物品列表失败：${e.message}"
            }
        }
    }
    
    /**
     * 从保修记录加载数据到表单
     */
    private suspend fun loadWarrantyDataToForm(warranty: WarrantyEntity) {
        // 加载关联的物品信息
        val item = itemRepository.getItemWithDetailsById(warranty.itemId)
        _selectedItem.value = item
        
        // 设置表单数据
        _purchaseDate.value = warranty.purchaseDate
        _warrantyPeriodMonths.value = warranty.warrantyPeriodMonths
        _warrantyEndDate.value = warranty.warrantyEndDate
        _notes.value = warranty.notes
        _status.value = warranty.status
        _warrantyProvider.value = warranty.warrantyProvider
        _contactInfo.value = warranty.contactInfo
        
        // 解析并设置凭证图片
        val imageUris = warrantyRepository.parseReceiptImageUris(warranty.receiptImageUris)
            .map { Uri.parse(it) }
        _receiptImageUris.value = imageUris
    }
    
    /**
     * 设置表单验证逻辑
     */
    private fun setupFormValidation() {
        val mediator = MediatorLiveData<Boolean>()
        
        mediator.addSource(_selectedItem) { _isFormValid.value = validateForm() }
        mediator.addSource(_purchaseDate) { _isFormValid.value = validateForm() }
        mediator.addSource(_warrantyPeriodMonths) { _isFormValid.value = validateForm() }
    }
    
    /**
     * 验证表单是否有效
     */
    private fun validateForm(): Boolean {
        return _selectedItem.value != null &&
               _purchaseDate.value != null &&
               _warrantyPeriodMonths.value != null &&
               _warrantyPeriodMonths.value!! > 0
    }
    
    /**
     * 设置保修到期日期自动计算
     */
    private fun setupWarrantyEndDateCalculation() {
        val mediator = MediatorLiveData<Date>()
        
        val calculation = {
            val purchaseDate = _purchaseDate.value
            val period = _warrantyPeriodMonths.value
            
            if (purchaseDate != null && period != null && period > 0) {
                val calendar = Calendar.getInstance().apply {
                    time = purchaseDate
                    add(Calendar.MONTH, period)
                }
                calendar.time
            } else {
                null
            }
        }
        
        mediator.addSource(_purchaseDate) { 
            calculation()?.let { _warrantyEndDate.value = it }
        }
        mediator.addSource(_warrantyPeriodMonths) { 
            calculation()?.let { _warrantyEndDate.value = it }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    /**
     * 重置表单
     */
    fun resetForm() {
        _selectedItem.value = null
        _purchaseDate.value = Date()
        _warrantyPeriodMonths.value = 12
        _receiptImageUris.value = emptyList()
        _notes.value = null
        _status.value = WarrantyStatus.ACTIVE
        _warrantyProvider.value = null
        _contactInfo.value = null
    }
}
