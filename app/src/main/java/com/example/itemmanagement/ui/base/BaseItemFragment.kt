package com.example.itemmanagement.ui.base

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentAddItemBinding
import com.example.itemmanagement.ui.add.DialogFactory
import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.add.FieldViewFactory
import com.example.itemmanagement.ui.add.FieldValueManager
import com.example.itemmanagement.ui.add.PhotoAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.ImageView
import android.widget.LinearLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 新的物品管理基础Fragment
 * 
 * 这个抽象类与新的 BaseItemViewModel 架构配套工作，支持：
 * 1. 泛型 ViewModel 支持（AddItemViewModel, EditItemViewModel, ShoppingItemViewModel等）
 * 2. 自动的状态保存和恢复
 * 3. 通用的UI组件管理（照片、字段、标签等）
 * 4. 智能的缓存机制
 */
abstract class BaseItemFragment<T : BaseItemViewModel> : Fragment() {

    protected var _binding: FragmentAddItemBinding? = null
    protected val binding get() = _binding!!
    
    // 缓存 ViewModel - 与导航图绑定
    protected val cacheViewModel: ItemStateCacheViewModel by lazy {
        ViewModelProvider(requireActivity())[ItemStateCacheViewModel::class.java]
    }
    
    // 具体的 ViewModel - 由子类提供
    protected abstract val viewModel: T
    
    // UI工厂专用的AddItemViewModel（用于保持UI兼容性）
    // 注释掉旧的 uiViewModel - 现在直接使用 viewModel（它实现了FieldInteractionViewModel接口）
    // protected lateinit var uiViewModel: AddItemViewModel
    
    // UI组件
    protected lateinit var photoAdapter: PhotoAdapter
    protected lateinit var fieldViewFactory: FieldViewFactory
    protected lateinit var fieldValueManager: FieldValueManager
    protected lateinit var dialogFactory: DialogFactory
    
    // 字段视图映射
    protected val fieldViews = mutableMapOf<String, View>()
    
    // 照片相关
    protected var currentPhotoUri: Uri? = null
    protected var currentPhotoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        
        // 初始化基础工具类（不依赖 viewModel）
        dialogFactory = DialogFactory(requireContext())
        
        // 子类特定的初始化
        onViewModelReady()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化UI工厂专用的AddItemViewModel（使用简单工厂避免SavedState冲突）
        val repository = (requireActivity().application as ItemManagementApplication).repository
        
        // 初始化UI工厂组件（现在直接使用viewModel，它实现了FieldInteractionViewModel接口）
        fieldViewFactory = FieldViewFactory(requireContext(), viewModel, dialogFactory, resources, parentFragmentManager)
        fieldValueManager = FieldValueManager(requireContext(), viewModel, dialogFactory)
        
        setupUI()
        setupObservers()
        setupButtons()
        hideBottomNavigation()
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        setupTitleAndButtons()
        setupPhotoRecyclerView()
        setupFields()
    }

    /**
     * 设置观察者
     */
    private fun setupObservers() {
        observeSelectedFields()
        observePhotoUris()
        observeTags()
        observeSaveResult()
        observeErrorMessages()
    }

    /**
     * 设置照片RecyclerView
     */
    protected fun setupPhotoRecyclerView() {
        photoAdapter = PhotoAdapter().apply {
            setOnDeleteClickListener { position ->
                removePhoto(position)
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
            
            // 关键：设置item的宽度（与原有BaseItemFragment完全一致）
            post {
                val totalSpacing = (spanCount - 1) * spacing
                val itemWidth = (width - totalSpacing) / spanCount
                photoAdapter.setItemSize(itemWidth)
            }
        }
    }

    /**
     * 观察照片URI变化
     */
    private fun observePhotoUris() {
        viewModel.photoUris.observe(viewLifecycleOwner) { uris ->
            photoAdapter.setPhotos(uris)
        }
    }

    /**
     * 设置字段
     */
    protected fun setupFields() {
        // 清空现有字段
        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()
        
        // 观察选中字段的变化
        viewModel.selectedFields.observe(viewLifecycleOwner) { fields ->
            updateFieldsUI(fields)
        }
    }

    /**
     * 更新字段UI（完全按照原有BaseItemFragment的方式）
     */
    private fun updateFieldsUI(fields: Set<Field>) {
        // 清空现有字段
        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()
        
        // 使用Field类中定义的order属性进行排序（与原有方式完全相同）
        val sortedFields = fields.sortedBy { it.order }
        
        sortedFields.forEach { field ->
            android.util.Log.d("BaseItemFragment", "开始处理字段: ${field.name}")
            
            // 使用原有的FieldViewFactory创建复杂UI字段视图
            val fieldView = fieldViewFactory.createFieldView(field)
            android.util.Log.d("BaseItemFragment", "字段 ${field.name} 视图创建结果: ${if (fieldView != null) "成功 (${fieldView.javaClass.simpleName})" else "失败 (null)"}")
            
            binding.fieldsContainer.addView(fieldView)
            fieldViews[field.name] = fieldView
            android.util.Log.d("BaseItemFragment", "字段 ${field.name} 已添加到容器和fieldViews映射")
        }
        
        // 恢复已保存的字段值（现在直接使用viewModel，不需要同步）
        android.util.Log.d("BaseItemFragment", "开始UI数据恢复流程")
        android.util.Log.d("BaseItemFragment", "恢复前 - ViewModel字段值: ${viewModel.getAllFieldValues()}")
        android.util.Log.d("BaseItemFragment", "开始恢复UI字段值，字段数量: ${fieldViews.size}")
        fieldViews.forEach { (name, view) ->
            android.util.Log.d("BaseItemFragment", "fieldViews映射 - 字段: $name, 视图: ${view?.javaClass?.simpleName ?: "null"}")
        }
        
        fieldValueManager.restoreFieldValues(fieldViews)
        android.util.Log.d("BaseItemFragment", "UI数据恢复流程完成")
    }

    // syncDataToUIViewModel 方法已删除 - 不再需要同步，直接使用viewModel
    
    // syncDataFromUIViewModel 方法已删除 - 不再需要同步，直接使用viewModel

    /**
     * 观察选中字段变化
     */
    private fun observeSelectedFields() {
        viewModel.selectedFields.observe(viewLifecycleOwner) { fields ->
            // 字段变化在setupFields中已经处理
        }
    }

    /**
     * 观察标签变化
     */
    private fun observeTags() {
        viewModel.selectedTags.observe(viewLifecycleOwner) { tagsMap ->
            // 更新标签显示（使用原有的复杂标签更新逻辑）
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
                                    // 从 ViewModel 中移除标签
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
    }

    /**
     * 更新ChipGroup
     */
    private fun updateChipGroup(chipGroup: ChipGroup, tags: List<String>) {
        chipGroup.removeAllViews()
        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = true
                isChecked = true
            }
            chipGroup.addView(chip)
        }
    }

    /**
     * 观察保存结果
     */
    private fun observeSaveResult() {
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            success?.let {
                if (it) {
                    onSaveSuccess()
                } else {
                    onSaveFailure()
                }
                viewModel.onSaveResultConsumed()
            }
        }
    }

    /**
     * 观察错误消息
     */
    private fun observeErrorMessages() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 保存操作
     */
    protected fun performSave() {
        // 保存当前字段值（使用原有的精美逻辑）
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        // 不再需要同步 - 现在直接使用viewModel，UI组件直接与viewModel交互
        
        // 执行保存
        viewModel.performSave()
    }

    /**
     * 照片选择对话框
     */
    protected open fun showPhotoSelectionDialog() {
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
    protected fun showPhotoViewDialog(uri: Uri) {
        // 创建图片查看对话框
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            setImageURI(uri)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(imageView)
            .setNegativeButton("关闭") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // === 权限相关方法 ===
    
    protected fun checkAndRequestCameraPermission() {
        val cameraPermission = Manifest.permission.CAMERA
        when {
            ContextCompat.checkSelfPermission(requireContext(), cameraPermission) == PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                showPermissionRationaleDialog("相机") {
                    requestCameraPermissions.launch(arrayOf(cameraPermission))
                }
            }
            else -> {
                requestCameraPermissions.launch(arrayOf(cameraPermission))
            }
        }
    }

    protected fun checkAndRequestStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRationaleDialog("存储") {
                    requestStoragePermission.launch(arrayOf(permission))
                }
            }
            else -> {
                requestStoragePermission.launch(arrayOf(permission))
            }
        }
    }

    private fun showPermissionRationaleDialog(permissionType: String, onConfirm: () -> Unit) {
        dialogFactory.createConfirmDialog(
            title = "需要${permissionType}权限",
            message = "我们需要${permissionType}权限来完成操作。请在接下来的对话框中允许。",
            positiveButtonText = "确定",
            onPositiveClick = onConfirm
        )
    }

    // 权限请求结果处理
    private val requestCameraPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) {
            launchCamera()
        } else {
            showPermissionDeniedDialog("相机")
        }
    }

    private val requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) {
            openGallery()
        } else {
            showPermissionDeniedDialog("存储")
        }
    }

    private fun showPermissionDeniedDialog(permissionType: String) {
        dialogFactory.createConfirmDialog(
            title = "${permissionType}权限被拒绝",
            message = "没有${permissionType}权限，无法完成操作。请在设置中手动授予权限。",
            positiveButtonText = "确定"
        )
    }

    // === 相机和相册相关方法 ===
    
    private fun launchCamera() {
        try {
            currentPhotoFile = createTempImageFile("CAMERA")
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                currentPhotoFile!!
            )
            
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            }
            cameraLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "无法启动相机：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            currentPhotoUri?.let { uri ->
                lifecycleScope.launch {
                    val compressedUri = withContext(Dispatchers.IO) {
                        compressImage(uri)
                    }
                    compressedUri?.let {
                        photoAdapter.addPhoto(it)
                        viewModel.addPhotoUri(it)
                    }
                }
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                lifecycleScope.launch {
                    val compressedUri = withContext(Dispatchers.IO) {
                        compressImage(uri)
                    }
                    compressedUri?.let {
                        photoAdapter.addPhoto(it)
                        viewModel.addPhotoUri(it)
                    }
                }
            }
        }
    }

    // === 工具方法 ===
    
    private fun createTempImageFile(prefix: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "${prefix}_${timeStamp}"
        val storageDir = requireContext().getExternalFilesDir("Photos")
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    private fun compressImage(uri: Uri): Uri? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val maxDimension = 1024
            var sampleSize = 1

            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val heightRatio = Math.round(options.outHeight.toFloat() / maxDimension.toFloat())
                val widthRatio = Math.round(options.outWidth.toFloat() / maxDimension.toFloat())
                sampleSize = if (heightRatio < widthRatio) widthRatio else heightRatio
            }

            val compressOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            val bitmap = requireContext().contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, compressOptions)
            } ?: throw Exception("无法加载图片")

            val compressedFile = createTempImageFile("COMPRESSED")
            FileOutputStream(compressedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            bitmap.recycle()

            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                compressedFile
            )
        } catch (e: Exception) {
            null
        }
    }

    protected fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    protected fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        // 保存当前字段值（使用原有的精美逻辑）
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
            // 不再需要同步 - 现在直接使用viewModel，UI组件直接与viewModel交互
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }

    

    /**
     * 显示字段选择对话框（使用原有的精美样式）
     * 这个方法现在由EditFieldsFragment处理，不再需要在这里实现
     */



    // === 抽象方法，由子类实现 ===

    /**
     * ViewModel 准备就绪时调用
     */
    protected abstract fun onViewModelReady()
    
    /**
     * 设置标题和按钮
     */
    protected abstract fun setupTitleAndButtons()
    
    /**
     * 设置按钮点击事件
     */
    protected abstract fun setupButtons()
    
    /**
     * 保存成功回调
     */
    protected open fun onSaveSuccess() {
        Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
        // 默认行为：返回上一页
        activity?.onBackPressed()
    }
    
    /**
     * 保存失败回调
     */
    protected open fun onSaveFailure() {
        // 默认行为：错误消息已在observeErrorMessages中处理
    }
} 