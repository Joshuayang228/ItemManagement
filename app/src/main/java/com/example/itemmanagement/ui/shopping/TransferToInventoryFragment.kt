package com.example.itemmanagement.ui.shopping

import android.Manifest
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentTransferToInventoryBinding
import androidx.fragment.app.activityViewModels
import com.example.itemmanagement.ui.add.DialogFactory
import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.add.FieldViewFactory
import com.example.itemmanagement.ui.add.FieldValueManager
import com.example.itemmanagement.utils.SnackbarHelper
import com.example.itemmanagement.ui.add.PhotoAdapter
import com.example.itemmanagement.ui.base.ItemStateCacheViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 转入库存半屏对话框
 *
 * 核心特性：
 * - 继承 BottomSheetDialogFragment，默认支持向下拖拽关闭，禁止向上拖拽全屏
 * - 完整复用 BaseItemFragment 的照片和字段功能
 * - 使用 AddItemViewModel 实现数据预填充和保存
 */
class TransferToInventoryFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentTransferToInventoryBinding? = null
    private val binding get() = _binding!!

    private var itemId: Long = -1L

    // ViewModel
    private val cacheViewModel: ItemStateCacheViewModel by lazy {
        ViewModelProvider(requireActivity())[ItemStateCacheViewModel::class.java]
    }

    // ⭐ 改用 TransferToInventoryViewModel，使用 activityViewModels 与全屏共享
    private val viewModel: TransferToInventoryViewModel by activityViewModels {
        val app = (requireActivity().application as ItemManagementApplication)
        TransferToInventoryViewModelFactory(
            app.repository,
            cacheViewModel,
            itemId
        )
    }

    // UI组件（复用 BaseItemFragment 的组件）
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var fieldViewFactory: FieldViewFactory
    private lateinit var fieldValueManager: FieldValueManager
    private lateinit var dialogFactory: DialogFactory

    // 字段视图映射
    private val fieldViews = mutableMapOf<String, View>()

    // 照片相关
    private var currentPhotoUri: Uri? = null
    private var currentPhotoFile: File? = null

    companion object {
        private const val ARG_ITEM_ID = "item_id"

        fun newInstance(itemId: Long): TransferToInventoryFragment {
            return TransferToInventoryFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_ITEM_ID, itemId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = arguments?.getLong(ARG_ITEM_ID) ?: -1L
        if (itemId == -1L) {
            android.util.Log.e("TransferToInventory", "未接收到有效的物品ID")
            SnackbarHelper.showError(requireView(), "物品信息加载失败")
            dismiss()
            return
        }

        // 初始化工具类
        dialogFactory = DialogFactory(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferToInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        android.util.Log.d("TransferToInventory", "========== onViewCreated 开始 ==========")
        android.util.Log.d("TransferToInventory", "物品ID: $itemId")

        // 初始化UI工厂组件
        fieldViewFactory = FieldViewFactory(requireContext(), viewModel, dialogFactory, resources, parentFragmentManager)
        fieldValueManager = FieldValueManager(requireContext(), viewModel, dialogFactory)
        android.util.Log.d("TransferToInventory", "✓ UI工厂组件已初始化")

        setupUI()
        android.util.Log.d("TransferToInventory", "✓ UI已设置")
        
        setupObservers()
        android.util.Log.d("TransferToInventory", "✓ 观察者已设置")
        
        setupButtons()
        android.util.Log.d("TransferToInventory", "✓ 按钮已设置")

        // 先初始化默认字段
        initializeDefaultFields()
        android.util.Log.d("TransferToInventory", "✓ 默认字段已初始化")

        // ⭐ 数据加载已在 ViewModel 的 init 中自动完成
        // 这里不需要手动调用 loadFromShoppingItem()
        android.util.Log.d("TransferToInventory", "========== onViewCreated 完成 ==========")
    }
    
    override fun onStart() {
        super.onStart()
        
        // ⭐ 配置 BottomSheetBehavior：禁止向上拖拽，只允许向下拖拽关闭
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                
                // 设置为展开状态
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                
                // 禁用半展开状态（防止向上拖拽）
                behavior.isFitToContents = true
                behavior.skipCollapsed = true
                
                // 设置peekHeight为0，确保不会有额外的向上空间
                behavior.peekHeight = 0
                
                // 添加状态监听，防止用户向上拖拽
                behavior.addBottomSheetCallback(object : com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        // 只允许展开(EXPANDED)和隐藏(HIDDEN)状态
                        // 如果用户尝试拖拽到其他状态，强制回到展开状态
                        when (newState) {
                            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_DRAGGING,
                            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_SETTLING -> {
                                // 允许拖拽动画
                            }
                            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED,
                            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                                // 禁止折叠和半展开状态，强制回到展开状态
                                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                            }
                            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED -> {
                                // 展开状态 - 正常
                            }
                            com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN -> {
                                // 隐藏状态 - 关闭对话框
                                dismiss()
                            }
                        }
                    }
                    
                    override fun onSlide(bottomSheet: View, slideOffset: Float) {
                        // 监控滑动，只允许向下滑动（slideOffset < 1.0）
                        // slideOffset: 1.0 = 完全展开, 0.0 = 折叠, <0 = 隐藏
                        if (slideOffset > 1.0f) {
                            // 防止向上滑动超过展开状态
                            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                        }
                    }
                })
                
                android.util.Log.d("TransferToInventory", "✓ BottomSheet 行为已配置：禁止向上拖拽")
            }
        }
    }

    /**
     * 初始化默认字段
     */
    private fun initializeDefaultFields() {
        android.util.Log.d("TransferToInventory", ">>> 初始化默认字段")
        val defaultFields = setOf(
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", true),
            Field("基础信息", "位置", true),
            Field("其他", "备注", true),
            Field("分类", "分类", true),
            Field("日期类", "添加日期", true)
        )

        // 设置默认字段
        android.util.Log.d("TransferToInventory", "设置默认字段，共 ${defaultFields.size} 个")
        defaultFields.forEach { field ->
            android.util.Log.d("TransferToInventory", "  - 设置字段: ${field.name}, 选中: ${field.isSelected}")
            viewModel.updateFieldSelection(field, field.isSelected)
        }
        android.util.Log.d("TransferToInventory", "<<< 默认字段初始化完成")
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        setupPhotoRecyclerView()
        setupFields()
    }

    /**
     * 设置照片RecyclerView（完全复用 BaseItemFragment 的逻辑）
     */
    private fun setupPhotoRecyclerView() {
        photoAdapter = PhotoAdapter().apply {
            setOnDeleteClickListener { position ->
                viewModel.removePhotoUri(position)
            }
            setOnAddPhotoClickListener {
                showPhotoSelectionDialog()
            }
            setOnPhotoClickListener { uri ->
                showPhotoViewDialog(uri)
            }
        }

        binding.photoRecyclerView.apply {
            val spanCount = 3
            val spacing = resources.getDimensionPixelSize(R.dimen.photo_grid_spacing)

            setPadding(0, 0, 0, 0)
            clipToPadding = false
            layoutManager = GridLayoutManager(requireContext(), spanCount)

            // 移除旧的decoration
            if (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }

            // 添加间距decoration
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val position = parent.getChildAdapterPosition(view)
                    val column = position % spanCount

                    outRect.left = column * spacing / spanCount
                    outRect.right = spacing - (column + 1) * spacing / spanCount

                    if (position >= spanCount) {
                        outRect.top = spacing
                    }
                }
            })

            adapter = photoAdapter

            // 关键：设置item的宽度
            post {
                val totalSpacing = (spanCount - 1) * spacing
                val itemWidth = (width - totalSpacing) / spanCount
                photoAdapter.setItemSize(itemWidth)
            }
        }
    }

    /**
     * 设置字段（不再在这里初始化，由 initializeDefaultFields 处理）
     */
    private fun setupFields() {
        // 字段将通过 observeSelectedFields 自动渲染
    }

    /**
     * 设置观察者
     */
    private fun setupObservers() {
        android.util.Log.d("TransferToInventory", ">>> 设置观察者")
        
        // 观察照片变化
        viewModel.photoUris.observe(viewLifecycleOwner) { uris ->
            android.util.Log.d("TransferToInventory", "📷 照片变化: ${uris.size} 张照片")
            photoAdapter.setPhotos(uris)
        }

        // 观察字段变化
        viewModel.selectedFields.observe(viewLifecycleOwner) { fields ->
            android.util.Log.d("TransferToInventory", "📝 字段变化: ${fields.size} 个字段")
            fields.forEach { field ->
                android.util.Log.d("TransferToInventory", "  - 字段: ${field.group} -> ${field.name}")
            }
            updateFieldsUI(fields)
        }

        // 观察标签变化
        viewModel.selectedTags.observe(viewLifecycleOwner) { tagsMap ->
            android.util.Log.d("TransferToInventory", "🏷️ 标签变化: ${tagsMap.size} 组标签")
            updateTags(tagsMap)
        }

        // 观察保存结果
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            success?.let {
                android.util.Log.d("TransferToInventory", "💾 保存结果: ${if (it) "成功" else "失败"}")
                if (it) {
                    SnackbarHelper.showSuccess(requireView(), "已成功转入库存")
                    dismiss()
                } else {
                    SnackbarHelper.showError(requireView(), "转入失败，请检查必填字段")
                }
                viewModel.onSaveResultConsumed()
            }
        }

        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                android.util.Log.e("TransferToInventory", "❌ 错误消息: $message")
                SnackbarHelper.showError(requireView(), message)
            }
        }
        
        android.util.Log.d("TransferToInventory", "<<< 观察者设置完成")
    }

    /**
     * 更新字段UI
     */
    private fun updateFieldsUI(fields: Set<Field>) {
        android.util.Log.d("TransferToInventory", ">>> 更新字段UI")
        android.util.Log.d("TransferToInventory", "收到 ${fields.size} 个字段")
        
        // 清空现有字段
        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()
        android.util.Log.d("TransferToInventory", "✓ 已清空现有字段")

        // 按order排序
        val sortedFields = fields.sortedBy { it.order }
        android.util.Log.d("TransferToInventory", "✓ 字段已排序")

        sortedFields.forEach { field ->
            // 使用FieldViewFactory创建字段视图
            android.util.Log.d("TransferToInventory", "  创建字段视图: ${field.name}")
            val fieldView = fieldViewFactory.createFieldView(field)
            if (fieldView != null) {
                binding.fieldsContainer.addView(fieldView)
                fieldViews[field.name] = fieldView
                android.util.Log.d("TransferToInventory", "  ✓ 字段 ${field.name} 创建成功")
            } else {
                android.util.Log.e("TransferToInventory", "  ✗ 字段 ${field.name} 创建失败（返回null）")
            }
        }

        android.util.Log.d("TransferToInventory", "当前 fieldViews 映射大小: ${fieldViews.size}")
        android.util.Log.d("TransferToInventory", "准备恢复字段值...")
        
        // 获取 ViewModel 中的字段值
        val allFieldValues = viewModel.getAllFieldValues()
        android.util.Log.d("TransferToInventory", "ViewModel 中的字段值数量: ${allFieldValues.size}")
        allFieldValues.forEach { (key, value) ->
            android.util.Log.d("TransferToInventory", "  字段值: $key = $value")
        }
        
        // 恢复已保存的字段值
        fieldValueManager.restoreFieldValues(fieldViews)
        android.util.Log.d("TransferToInventory", "<<< 字段UI更新完成")
    }

    /**
     * 更新标签
     */
    private fun updateTags(tagsMap: Map<String, Set<String>>) {
        tagsMap.forEach { (fieldName, selectedTags) ->
            fieldViews[fieldName]?.let { fieldView ->
                val chipGroup = fieldView.findViewById<ChipGroup>(R.id.selected_tags_container)
                if (chipGroup != null) {
                    // 获取当前显示的标签
                    val currentTags = mutableSetOf<String>()
                    for (i in 0 until chipGroup.childCount) {
                        val chip = chipGroup.getChildAt(i) as? Chip
                        if (chip != null) {
                            currentTags.add(chip.text.toString())
                        }
                    }

                    // 移除已删除的标签
                    val tagsToRemove = currentTags - selectedTags
                    tagsToRemove.forEach { tagToRemove ->
                        for (i in 0 until chipGroup.childCount) {
                            val chip = chipGroup.getChildAt(i) as? Chip
                            if (chip != null && chip.text.toString() == tagToRemove) {
                                chipGroup.removeView(chip)
                                break
                            }
                        }
                    }

                    // 添加新的标签
                    val tagsToAdd = selectedTags - currentTags
                    tagsToAdd.forEach { tagToAdd ->
                        val chip = Chip(requireContext()).apply {
                            text = tagToAdd
                            isCloseIconVisible = true
                            setOnCloseIconClickListener {
                                chipGroup.removeView(this)
                                val updatedTags = selectedTags.toMutableSet()
                                updatedTags.remove(tagToAdd)
                                viewModel.updateSelectedTags(fieldName, updatedTags)
                            }
                        }
                        chipGroup.addView(chip)
                    }
                }
            }
        }
    }

    /**
     * 设置按钮
     */
    private fun setupButtons() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSaveToInventory.setOnClickListener {
            performSave()
        }

        binding.btnFullScreen.setOnClickListener {
            navigateToFullScreenMode()
        }
    }

    /**
     * 执行保存
     */
    private fun performSave() {
        // 保存当前字段值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }

        // 执行保存
        viewModel.performSave()
    }

    /**
     * 跳转到全屏模式（完整的添加物品界面）
     */
    private fun navigateToFullScreenMode() {
        // 先保存当前已填写的字段值到缓存
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }

        // 使用 Navigation 跳转到 AddItemFragment
        val bundle = Bundle().apply {
            putString("sourceType", "SHOPPING_LIST")
            putLong("sourceItemId", itemId)
            putString("mode", "add")
        }

        try {
            // 尝试多种方式获取 NavController
            val navController = when {
                // 方式1: 从父 Fragment 获取
                parentFragment != null -> parentFragment?.findNavController()
                // 方式2: 从目标 Fragment 获取
                targetFragment != null -> targetFragment?.findNavController()
                // 方式3: 从 Activity 的导航宿主获取
                else -> {
                    val navHostFragment = requireActivity().supportFragmentManager
                        .findFragmentById(R.id.nav_host_fragment)
                    navHostFragment?.findNavController()
                }
            }

            if (navController != null) {
                // ⭐ 修复：在导航前先确保导航栏隐藏
                activity?.findViewById<View>(com.example.itemmanagement.R.id.nav_view)?.visibility = View.GONE
                android.util.Log.d("TransferToInventory", "跳转全屏前确保导航栏隐藏")
                
                // 先关闭半屏对话框
                dismiss()
                
                // 延迟一小段时间后导航，确保对话框已完全关闭
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    // 导航前再次确保导航栏隐藏（双重保险）
                    activity?.findViewById<View>(com.example.itemmanagement.R.id.nav_view)?.visibility = View.GONE
                    
                    // ⭐ 导航到新的全屏Fragment
                    navController.navigate(
                        R.id.action_shopping_list_to_transfer_fullscreen,
                        Bundle().apply {
                            putLong("itemId", itemId)
                            putString("itemName", viewModel.getFieldValue("名称")?.toString() ?: "")
                        }
                    )
                }, 100)
            } else {
                SnackbarHelper.showError(requireView(), "导航失败，请重试")
                android.util.Log.e("TransferToInventory", "无法获取 NavController")
            }
        } catch (e: Exception) {
            android.util.Log.e("TransferToInventory", "导航到全屏模式失败", e)
            SnackbarHelper.showError(requireView(), "跳转失败: ${e.message}")
        }
    }

    /**
     * 显示照片选择对话框
     */
    private fun showPhotoSelectionDialog() {
        val items = arrayOf("拍照", "从相册选择")
        dialogFactory.createDialog(
            title = "选择照片来源",
            items = items
        ) { which ->
            when (which) {
                0 -> checkAndRequestCameraPermission()
                1 -> checkAndRequestStoragePermission()
            }
        }
    }

    /**
     * 显示照片查看对话框
     */
    private fun showPhotoViewDialog(uri: Uri) {
        // TODO: 实现照片查看功能（可以使用DialogFactory显示大图）
        SnackbarHelper.show(requireView(), "查看: ${uri.lastPathSegment}")
    }

    // ===== 权限和照片相关方法 =====

    private fun checkAndRequestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun checkAndRequestStoragePermission() {
        pickImageLauncher.launch("image/*")
    }

    private fun takePhoto() {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(null)
        val photoFile = File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
        currentPhotoFile = photoFile
        currentPhotoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        takePictureLauncher.launch(currentPhotoUri)
    }

    // ActivityResultLaunchers
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            SnackbarHelper.show(requireView(), "需要相机权限才能拍照")
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && currentPhotoUri != null) {
            lifecycleScope.launch {
                val compressedUri = compressImage(currentPhotoUri!!)
                if (compressedUri != null) {
                    viewModel.addPhotoUri(compressedUri)
                }
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            lifecycleScope.launch {
                val compressedUri = compressImage(it)
                if (compressedUri != null) {
                    viewModel.addPhotoUri(compressedUri)
                }
            }
        }
    }

    /**
     * 压缩图片
     */
    private suspend fun compressImage(uri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val maxSize = 1024
            val ratio = Math.min(
                maxSize.toFloat() / bitmap.width,
                maxSize.toFloat() / bitmap.height
            )
            val width = (ratio * bitmap.width).toInt()
            val height = (ratio * bitmap.height).toInt()

            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true)

            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir(null)
            val photoFile = File.createTempFile(
                "COMPRESSED_${timeStamp}_",
                ".jpg",
                storageDir
            )

            val outputStream = java.io.FileOutputStream(photoFile)
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            outputStream.close()

            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
        } catch (e: Exception) {
            android.util.Log.e("TransferToInventory", "压缩图片失败", e)
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
