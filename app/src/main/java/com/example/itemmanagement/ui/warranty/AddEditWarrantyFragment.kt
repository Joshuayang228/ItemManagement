package com.example.itemmanagement.ui.warranty

import android.Manifest
import com.example.itemmanagement.ui.utils.showMaterial3DatePicker
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController

import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.WarrantyStatus
import com.example.itemmanagement.data.relation.ItemWithDetails
import com.example.itemmanagement.databinding.FragmentAddEditWarrantyBinding
import com.example.itemmanagement.utils.SnackbarHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

/**
 * 添加/编辑保修信息Fragment
 * 基于用户偏好，使用顶部图片+下面字段的UI风格
 */
class AddEditWarrantyFragment : Fragment() {

    private var _binding: FragmentAddEditWarrantyBinding? = null
    private val binding get() = _binding!!
    
    // 使用arguments替代navArgs，因为我们使用Bundle传递参数
    private fun getWarrantyId(): Long = arguments?.getLong("warrantyId", -1L) ?: -1L
    private fun getPreSelectedItemId(): Long = arguments?.getLong("preSelectedItemId", -1L) ?: -1L
    private val viewModel: AddEditWarrantyViewModel by viewModels {
        AddEditWarrantyViewModelFactory(
            (requireActivity().application as ItemManagementApplication).warrantyRepository,
            (requireActivity().application as ItemManagementApplication).repository
        )
    }
    
    private lateinit var receiptImageAdapter: ReceiptImageAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditWarrantyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        hideBottomNavigation()
        setupUI()
        setupImageRecyclerView()
        setupObservers()
        
        // 根据传入参数初始化模式
        val warrantyId = getWarrantyId()
        if (warrantyId > 0) {
            // 编辑模式
            viewModel.initializeForEdit(warrantyId)
        } else {
            // 新建模式
            val preSelectedItemId = getPreSelectedItemId()
            viewModel.initializeForAdd(if (preSelectedItemId > 0) preSelectedItemId else null)
        }
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        setupItemSelection()
        setupDatePickers()
        setupWarrantyPeriodInput()
        // setupWarrantyStatusDropdown() // 已移除保修详情卡片
        setupSaveButton()
    }

    /**
     * 设置物品选择
     */
    private fun setupItemSelection() {
        binding.itemSelectionLayout.setOnClickListener {
            showItemSelectionDialog()
        }
    }

    /**
     * 设置日期选择器
     */
    private fun setupDatePickers() {
        binding.purchaseDateLayout.setOnClickListener {
            showDatePicker { date ->
                viewModel.setPurchaseDate(date)
            }
        }
    }

    /**
     * 设置保修期输入
     */
    private fun setupWarrantyPeriodInput() {
        android.util.Log.d("AddEditWarrantyFrag", "setupWarrantyPeriodInput: 设置监听器")
        // 直接在EditText中输入，监听文本变化
        binding.warrantyPeriodEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                android.util.Log.d("AddEditWarrantyFrag", "warrantyPeriodEditText afterTextChanged: '$text'")
                if (text.isNotEmpty()) {
                    try {
                        val months = text.toInt()
                        android.util.Log.d("AddEditWarrantyFrag", "解析保修期: $months 个月")
                        if (months > 0) {
                            viewModel.setWarrantyPeriodMonths(months)
                        } else {
                            android.util.Log.d("AddEditWarrantyFrag", "保修期 <= 0，忽略")
                        }
                    } catch (e: NumberFormatException) {
                        android.util.Log.d("AddEditWarrantyFrag", "解析保修期失败: ${e.message}")
                    }
                } else {
                    android.util.Log.d("AddEditWarrantyFrag", "保修期文本为空")
                }
            }
        })
    }

    /**
     * 设置保修状态下拉菜单
     */
    private fun setupWarrantyStatusDropdown() {
        // 保修状态已移除，不再需要
        // binding.warrantyStatusLayout.setOnClickListener {
        //     showWarrantyStatusDialog()
        // }
    }

    /**
     * 设置保存按钮
     */
    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveFormData()
            viewModel.saveWarranty()
        }
    }

    /**
     * 设置图片RecyclerView
     */
    private fun setupImageRecyclerView() {
        receiptImageAdapter = ReceiptImageAdapter(
            onImageClick = { uri -> showImageViewDialog(uri) },
            onImageDelete = { uri -> viewModel.removeReceiptImage(uri) },
            onAddImageClick = { showImageSourceDialog() }
        )
        
        binding.receiptImageRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = receiptImageAdapter
        }
    }

    /**
     * 设置观察者
     */
    private fun setupObservers() {
        // 观察选中的物品
        viewModel.selectedItem.observe(viewLifecycleOwner) { item ->
            updateSelectedItemDisplay(item)
        }
        
        // 观察可选物品列表
        viewModel.availableItems.observe(viewLifecycleOwner) { items ->
            // 物品列表用于选择对话框
        }
        
        // 观察购买日期
        viewModel.purchaseDate.observe(viewLifecycleOwner) { date ->
            binding.purchaseDateEditText.text = dateFormat.format(date)
        }
        
        // 观察保修期
        viewModel.warrantyPeriodMonths.observe(viewLifecycleOwner) { months ->
            if (binding.warrantyPeriodEditText.text.toString() != months.toString()) {
                binding.warrantyPeriodEditText.setText(months.toString())
            }
        }
        
        // 观察到期日期（自动计算）
        viewModel.warrantyEndDate.observe(viewLifecycleOwner) { date ->
            if (date != null) {
                binding.warrantyEndDateEditText.text = dateFormat.format(date)
            } else {
                binding.warrantyEndDateEditText.text = ""
            }
        }
        
        // 观察保修状态 - 已移除保修详情卡片
        // viewModel.status.observe(viewLifecycleOwner) { status ->
        //     binding.warrantyStatusAutoComplete.text = when (status) {
        //         WarrantyStatus.ACTIVE -> "保修期内"
        //         WarrantyStatus.EXPIRED -> "已过期"
        //         WarrantyStatus.CLAIMED -> "已报修"
        //         WarrantyStatus.VOID -> "已作废"
        //     }
        // }
        
        // 观察保修服务商
        viewModel.warrantyProvider.observe(viewLifecycleOwner) { provider ->
            if (binding.warrantyProviderEditText.text.toString() != (provider ?: "")) {
                binding.warrantyProviderEditText.setText(provider ?: "")
            }
        }
        
        // 观察联系方式
        viewModel.contactInfo.observe(viewLifecycleOwner) { contact ->
            if (binding.contactInfoEditText.text.toString() != (contact ?: "")) {
                binding.contactInfoEditText.setText(contact ?: "")
            }
        }
        
        // 观察备注
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            if (binding.notesEditText.text.toString() != (notes ?: "")) {
                binding.notesEditText.setText(notes ?: "")
            }
        }
        
        // 观察凭证图片
        viewModel.receiptImageUris.observe(viewLifecycleOwner) { uris ->
            receiptImageAdapter.submitList(uris)
        }
        
        // 观察表单验证状态
        viewModel.isFormValid.observe(viewLifecycleOwner) { isValid ->
            android.util.Log.d("AddEditWarrantyFrag", "========== isFormValid 变化 ==========")
            android.util.Log.d("AddEditWarrantyFrag", "isValid: $isValid")
            android.util.Log.d("AddEditWarrantyFrag", "saveButton.isEnabled 之前: ${binding.saveButton.isEnabled}")
            binding.saveButton.isEnabled = isValid
            binding.saveButton.alpha = if (isValid) 1.0f else 0.5f
            android.util.Log.d("AddEditWarrantyFrag", "saveButton.isEnabled 之后: ${binding.saveButton.isEnabled}")
            android.util.Log.d("AddEditWarrantyFrag", "saveButton.alpha: ${binding.saveButton.alpha}")
            android.util.Log.d("AddEditWarrantyFrag", "=====================================")
        }
        
        // 观察编辑模式
        viewModel.isEditMode.observe(viewLifecycleOwner) { isEditMode ->
            // binding.warrantyStatusLayout.visibility = if (isEditMode) View.VISIBLE else View.GONE // 已移除保修详情卡片
            binding.saveButton.text = if (isEditMode) "更新保修信息" else "保存保修信息"
        }
        
        // 观察加载状态
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // 观察保存结果
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                SnackbarHelper.showSuccess(requireView(), "保存成功")
                findNavController().navigateUp()
            }
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                SnackbarHelper.showError(requireView(), it)
                viewModel.clearErrorMessage()
            }
        }
    }

    /**
     * 更新选中物品的显示
     */
    private fun updateSelectedItemDisplay(item: ItemWithDetails?) {
        if (item != null) {
            binding.itemSelectionAutoComplete.text = item.item.name
            // 不再显示预览卡片，已经隐藏
            // binding.selectedItemPreviewCard.visibility = View.VISIBLE
            // binding.selectedItemName.text = item.item.name
            // binding.selectedItemInfo.text = "${item.item.category} • ${item.item.brand ?: "未知品牌"}"
            
            // 加载物品图片
            val firstPhoto = item.photos.firstOrNull()
            if (firstPhoto != null) {
                // 这里需要根据项目实际的图片加载方式来实现
                // binding.selectedItemImage.setImageURI(Uri.parse(firstPhoto.uri))
            }
        } else {
            binding.itemSelectionAutoComplete.text = "请选择物品"
            // 预览卡片已隐藏
        }
    }

    /**
     * 显示物品选择对话框
     */
    private fun showItemSelectionDialog() {
        val items = viewModel.availableItems.value ?: return
        val itemNames = items.map { "${it.item.name} (${it.item.category})" }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择物品")
            .setItems(itemNames) { _, which ->
                viewModel.setSelectedItem(items[which])
            }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * 显示日期选择器
     */
    /**
     * 显示Material 3日期选择器
     */
    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val currentDate = viewModel.purchaseDate.value ?: Date()
        
        showMaterial3DatePicker(
            title = "选择购买日期",
            selectedDate = currentDate
        ) { selectedDate ->
            onDateSelected(selectedDate)
        }
    }

    /**
     * 显示保修期输入对话框
     */
    private fun showWarrantyPeriodInputDialog() {
        val currentValue = viewModel.warrantyPeriodMonths.value?.toString() ?: ""
        val editText = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(currentValue)
            hint = "请输入月数"
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("设置保修期")
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    try {
                        val months = text.toInt()
                        if (months > 0) {
                            viewModel.setWarrantyPeriodMonths(months)
                        } else {
                            SnackbarHelper.show(requireView(), "保修期必须大于0")
                        }
                    } catch (e: NumberFormatException) {
                        SnackbarHelper.show(requireView(), "请输入有效的数字")
                    }
                }
            }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    /**
     * 显示保修状态选择对话框
     */
    private fun showWarrantyStatusDialog() {
        val statusOptions = arrayOf("保修期内", "已过期", "已报修", "已作废")
        val statusValues = listOf(
            WarrantyStatus.ACTIVE,
            WarrantyStatus.EXPIRED,
            WarrantyStatus.CLAIMED,
            WarrantyStatus.VOID
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择保修状态")
            .setItems(statusOptions) { _, which ->
                viewModel.setStatus(statusValues[which])
            }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    
    /**
     * 显示文本输入对话框
     */
    private fun showTextInputDialog(
        title: String, 
        currentValue: String, 
        isMultiLine: Boolean = false,
        onConfirm: (String) -> Unit
    ) {
        val editText = EditText(requireContext()).apply {
            setText(currentValue)
            hint = "请输入$title"
            if (isMultiLine) {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                            android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                minLines = 3
                maxLines = 5
            }
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(editText)
            .setPositiveButton("确定") { _, _ ->
                onConfirm(editText.text.toString())
            }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    /**
     * 显示图片来源选择对话框
     */
    private fun showImageSourceDialog() {
        val options = arrayOf("拍照", "从相册选择")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("选择图片来源")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkAndRequestCameraPermission()
                    1 -> checkAndRequestStoragePermission()
                }
            }
            .show()
    }

    /**
     * 显示图片查看对话框
     */
    private fun showImageViewDialog(uri: Uri) {
        // 这里可以创建一个全屏的图片查看器
        // 暂时使用简单的Snackbar提示
        SnackbarHelper.show(requireView(), "查看图片: ${uri.lastPathSegment}")
    }

    /**
     * 保存表单数据到ViewModel
     */
    private fun saveFormData() {
        // 保存其他表单字段（现在都是EditText了）
        viewModel.setNotes(binding.notesEditText.text.toString().takeIf { it.isNotBlank() })
        viewModel.setWarrantyProvider(binding.warrantyProviderEditText.text.toString().takeIf { it.isNotBlank() })
        viewModel.setContactInfo(binding.contactInfoEditText.text.toString().takeIf { it.isNotBlank() })
    }

    // === 权限和图片选择相关方法 ===
    
    private fun checkAndRequestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showPermissionRationaleDialog("相机") {
                    requestCameraPermission.launch(Manifest.permission.CAMERA)
                }
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkAndRequestStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) 
                == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRationaleDialog("存储") {
                    requestStoragePermission.launch(permission)
                }
            }
            else -> {
                requestStoragePermission.launch(permission)
            }
        }
    }

    private fun showPermissionRationaleDialog(permissionType: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("需要${permissionType}权限")
            .setMessage("我们需要${permissionType}权限来完成操作。请在接下来的对话框中允许。")
            .setPositiveButton("确定") { _, _ -> onConfirm() }
            .setNegativeButton("取消") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            SnackbarHelper.show(requireView(), "需要相机权限才能拍照")
        }
    }

    private val requestStoragePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            SnackbarHelper.show(requireView(), "需要存储权限才能选择图片")
        }
    }

    private fun launchCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // 这里需要处理相机拍照的结果
            // 暂时使用模拟数据
            SnackbarHelper.show(requireView(), "拍照功能待完善")
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                viewModel.addReceiptImage(uri)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }

    /**
     * 隐藏底部导航栏
     */
    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    /**
     * 显示底部导航栏
     */
    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }
}
