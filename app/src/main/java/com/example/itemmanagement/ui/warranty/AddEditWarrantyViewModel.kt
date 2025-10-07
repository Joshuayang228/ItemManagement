package com.example.itemmanagement.ui.warranty

import android.net.Uri
import androidx.lifecycle.*
import com.example.itemmanagement.data.repository.UnifiedItemRepository
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
    private val itemRepository: UnifiedItemRepository
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
        android.util.Log.d("AddEditWarrantyVM", "setSelectedItem: ${item?.item?.name}")
        _selectedItem.value = item
        android.util.Log.d("AddEditWarrantyVM", "setSelectedItem AFTER: ${_selectedItem.value?.item?.name}")
        // 触发表单验证
        _isFormValid.value = validateForm()
    }
    
    /**
     * 设置购买日期
     */
    fun setPurchaseDate(date: Date) {
        android.util.Log.d("AddEditWarrantyVM", "setPurchaseDate: $date")
        _purchaseDate.value = date
        // 重新计算到期日期
        calculateWarrantyEndDate()
        // 触发表单验证
        _isFormValid.value = validateForm()
    }
    
    /**
     * 设置保修期
     */
    fun setWarrantyPeriodMonths(months: Int) {
        android.util.Log.d("AddEditWarrantyVM", "setWarrantyPeriodMonths: $months")
        _warrantyPeriodMonths.value = months
        android.util.Log.d("AddEditWarrantyVM", "setWarrantyPeriodMonths AFTER: ${_warrantyPeriodMonths.value}")
        // 重新计算到期日期
        calculateWarrantyEndDate()
        // 触发表单验证
        _isFormValid.value = validateForm()
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
                itemRepository.getActiveItemsWithDetails().collect { items ->
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
     * 注意：现在改为在 setter 中直接触发验证，不再使用 MediatorLiveData
     */
    private fun setupFormValidation() {
        android.util.Log.d("AddEditWarrantyVM", "setupFormValidation: 初始化验证")
        // 初始化时验证一次
        _isFormValid.value = validateForm()
    }
    
    /**
     * 验证表单是否有效
     */
    private fun validateForm(): Boolean {
        val hasItem = _selectedItem.value != null
        val hasDate = _purchaseDate.value != null
        val hasPeriod = _warrantyPeriodMonths.value != null && _warrantyPeriodMonths.value!! > 0
        
        val result = hasItem && hasDate && hasPeriod
        
        android.util.Log.d("AddEditWarrantyVM", "========== validateForm ==========")
        android.util.Log.d("AddEditWarrantyVM", "hasItem=$hasItem (${_selectedItem.value?.item?.name})")
        android.util.Log.d("AddEditWarrantyVM", "hasDate=$hasDate (${_purchaseDate.value})")
        android.util.Log.d("AddEditWarrantyVM", "hasPeriod=$hasPeriod (${_warrantyPeriodMonths.value})")
        android.util.Log.d("AddEditWarrantyVM", "结果: $result")
        android.util.Log.d("AddEditWarrantyVM", "isFormValid.value 将被设置为: $result")
        android.util.Log.d("AddEditWarrantyVM", "==================================")
        
        return result
    }
    
    /**
     * 计算保修到期日期
     */
    private fun calculateWarrantyEndDate() {
        val purchaseDate = _purchaseDate.value
        val period = _warrantyPeriodMonths.value
        
        android.util.Log.d("AddEditWarrantyVM", "计算保修到期日期: purchaseDate=$purchaseDate, period=$period")
        
        if (purchaseDate != null && period != null && period > 0) {
            val calendar = Calendar.getInstance().apply {
                time = purchaseDate
                add(Calendar.MONTH, period)
            }
            val endDate = calendar.time
            android.util.Log.d("AddEditWarrantyVM", "计算结果: $endDate")
            _warrantyEndDate.value = endDate
        } else {
            android.util.Log.d("AddEditWarrantyVM", "计算条件不满足，无法计算")
        }
    }
    
    /**
     * 设置保修到期日期自动计算
     */
    private fun setupWarrantyEndDateCalculation() {
        android.util.Log.d("AddEditWarrantyVM", "setupWarrantyEndDateCalculation: 初始化")
        // 初始化时计算一次
        calculateWarrantyEndDate()
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
