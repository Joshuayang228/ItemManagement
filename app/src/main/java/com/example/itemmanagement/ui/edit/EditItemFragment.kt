package com.example.itemmanagement.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import android.graphics.Rect
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentAddItemBinding
import com.example.itemmanagement.ui.add.AddItemViewModel
import com.example.itemmanagement.ui.add.AddItemViewModelFactory
import com.example.itemmanagement.ui.add.DialogFactory
import com.example.itemmanagement.ui.add.FieldValueManager
import com.example.itemmanagement.ui.add.FieldViewFactory
import com.example.itemmanagement.ui.add.PhotoAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.itemmanagement.ui.add.TagManager
import com.example.itemmanagement.ui.add.Field
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri
import android.widget.RadioButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.content.FileProvider
import android.content.Intent
import android.provider.MediaStore
import android.content.pm.PackageManager
import androidx.lifecycle.lifecycleScope

/**
 * 编辑物品Fragment
 * 这个Fragment使用与AddItemFragment相同的布局和ViewModel，但专门用于编辑物品
 */
class EditItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AddItemViewModel
    private lateinit var photoAdapter: PhotoAdapter

    // 工具类实例
    private lateinit var fieldViewFactory: FieldViewFactory
    private lateinit var dialogFactory: DialogFactory
    private lateinit var fieldValueManager: FieldValueManager

    // 字段视图映射
    private val fieldViews = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // 初始化ViewModel
        val repository = (requireActivity().application as ItemManagementApplication).repository
        val factory = AddItemViewModelFactory(repository, requireActivity())
        viewModel = ViewModelProvider(requireActivity(), factory)[AddItemViewModel::class.java]

        // 初始化工具类
        dialogFactory = DialogFactory(requireContext())
        fieldViewFactory = FieldViewFactory(requireContext(), viewModel, dialogFactory, resources)
        fieldValueManager = FieldValueManager(requireContext(), viewModel, dialogFactory)
        
        // 处理导航参数
        arguments?.let { args ->
            // 设置为编辑模式
            viewModel.setMode("edit")
            
            // 获取itemId并加载物品
            val itemId = args.getLong("itemId", -1L)
            if (itemId != -1L) {
                // 根据ID加载物品
                viewModel.loadItemById(itemId)
            }
        }
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
        
        // 强制设置标题为"编辑物品"
        requireActivity().title = getString(R.string.title_edit_item)
        
        // 设置保存按钮文本
        binding.saveButton.text = "更新物品"

        // 初始化照片适配器
        photoAdapter = PhotoAdapter().apply {
            setOnDeleteClickListener { position ->
                photoAdapter.removePhoto(position)
                viewModel.removePhotoUri(position)
            }
            setOnAddPhotoClickListener {
                showPhotoSelectionDialog()
            }
            setOnPhotoClickListener { uri ->
                showPhotoViewDialog(uri)
            }
        }

        // 设置照片RecyclerView
        setupPhotoRecyclerView()

        // 观察ViewModel中的图片URI列表变化
        viewModel.photoUris.observe(viewLifecycleOwner) { uris ->
            photoAdapter.setPhotos(uris)
        }

        // 恢复已保存的图片
        val savedPhotos = viewModel.getPhotoUris()
        if (savedPhotos.isNotEmpty()) {
            photoAdapter.setPhotos(savedPhotos)
        }

        // 设置字段
        setupFields()
        observeSelectedFields()
        setupButtons()
        hideBottomNavigation()
        
        // 观察保存结果
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Toast.makeText(context, "更新成功", Toast.LENGTH_SHORT).show()
                
                // 清除所有数据和草稿，确保下次进入是全新状态
                viewModel.clearAllData()
                
                // 导航回上一个页面
                findNavController().navigateUp()
                // 重置保存结果的状态，防止再次进入时触发
                viewModel.onSaveResultConsumed()
            }
        }
        
        // 观察错误信息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }

        // 观察标签变化
        observeTags()
    }

    private fun setupPhotoRecyclerView() {
        binding.photoRecyclerView.apply {
            val spanCount = 3
            val spacing = resources.getDimensionPixelSize(R.dimen.photo_grid_spacing)

            // 设置内边距
            setPadding(spacing, spacing, spacing, spacing)
            clipToPadding = false

            layoutManager = GridLayoutManager(requireContext(), spanCount).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int = 1
                }
            }

            // 添加间距装饰器
            if (itemDecorationCount == 0) {
                addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.set(spacing, spacing, spacing, spacing)
                    }
                })
            }

            adapter = photoAdapter

            // 设置item的高度等于宽度
            post {
                val itemWidth = (width - (paddingLeft + paddingRight) - (spacing * (spanCount - 1))) / spanCount
                photoAdapter.setItemSize(itemWidth)
            }
        }
    }

    private fun setupFields() {
        // 观察字段变化，动态创建字段视图
        viewModel.selectedFields.observe(viewLifecycleOwner) { fields ->
            binding.fieldsContainer.removeAllViews()
            fieldViews.clear()
            
            // 使用Field类中定义的order属性进行排序
            val sortedFields = fields.sortedBy { it.order }
            
            sortedFields.forEach { field ->
                val fieldView = fieldViewFactory.createFieldView(field)
                binding.fieldsContainer.addView(fieldView)
                fieldViews[field.name] = fieldView
            }
            
            // 恢复已保存的字段值
            fieldValueManager.restoreFieldValues(fieldViews)
        }
    }

    private fun observeSelectedFields() {
        // 观察已选择的字段变化
        viewModel.selectedFields.observe(viewLifecycleOwner) { fields ->
            // 已在setupFields中处理
        }
    }

    private fun observeTags() {
        // 观察标签变化
        viewModel.selectedTags.observe(viewLifecycleOwner) { selectedTagsMap ->
            selectedTagsMap.forEach { (fieldName, selectedTags) ->
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
                        if (tagsToRemove.isNotEmpty()) {
                            for (i in chipGroup.childCount - 1 downTo 0) {
                                val chip = chipGroup.getChildAt(i) as? Chip
                                if (chip != null && chip.text.toString() in tagsToRemove) {
                                    chipGroup.removeView(chip)
                                }
                            }
                        }

                        // 添加新的标签
                        val tagsToAdd = selectedTags - currentTags
                        if (tagsToAdd.isNotEmpty()) {
                            val tagManager = TagManager(requireContext(), dialogFactory, viewModel, fieldName)
                            tagsToAdd.forEach { tag ->
                                tagManager.addChipToGroup(chipGroup, tag)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        binding.editFieldsButton.setOnClickListener {
            // 在显示编辑字段对话框前，先保存当前字段的值
            if (fieldViews.isNotEmpty()) {
                fieldValueManager.saveFieldValues(fieldViews)
            }
            showEditFieldsDialog()
        }

        binding.saveButton.setOnClickListener {
            saveItem()
        }
    }

    private fun showEditFieldsDialog() {
        com.example.itemmanagement.ui.add.EditFieldsFragment.newInstance().show(
            childFragmentManager,
            "EditFieldsFragment"
        )
    }

    private fun saveItem() {
        // 保存字段值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        // 获取所有字段的值
        val values = fieldValueManager.getFieldValues(fieldViews)

        // 验证必填字段
        val nameValue = values["名称"] as? String
        if (nameValue.isNullOrBlank()) {
            Toast.makeText(context, "请输入物品名称", Toast.LENGTH_SHORT).show()
            return
        }

        // 将Uri列表转换为Photo列表
        val photoList = photoAdapter.getPhotos().map { uri ->
            com.example.itemmanagement.data.model.Photo(
                uri = uri.toString(),
                isMain = false,
                displayOrder = 0
            )
        }
        
        // 处理标签数据
        val tagsList = mutableListOf<com.example.itemmanagement.data.model.Tag>()
        (values["标签"] as? Set<String>)?.forEach { tagName ->
            tagsList.add(com.example.itemmanagement.data.model.Tag(name = tagName))
        }

        // 处理季节数据
        val seasonValue = (values["季节"] as? Set<String>)?.joinToString(", ")

        // 处理单位
        val quantityUnit = values["数量_unit"] as? String ?: "个"
        val priceUnit = values["单价_unit"] as? String ?: "元"
        val capacityUnit = values["容量_unit"] as? String
        val totalPriceUnit = values["总价_unit"] as? String ?: "元"

        // 处理保质期和保修期数据
        val shelfLifeValue = when (val shelfLife = values["保质期"]) {
            is Pair<*, *> -> {
                val number = (shelfLife.first as? String)?.toIntOrNull()
                val unit = shelfLife.second as? String
                
                when (unit) {
                    "年" -> number?.times(365)
                    "月" -> number?.times(30)
                    "日" -> number
                    else -> null
                }
            }
            is String -> shelfLife.toIntOrNull()
            else -> null
        }
        
        val warrantyPeriodValue = when (val warranty = values["保修期"]) {
            is Pair<*, *> -> {
                val number = (warranty.first as? String)?.toIntOrNull()
                val unit = warranty.second as? String
                
                when (unit) {
                    "年" -> number?.times(365)
                    "月" -> number?.times(30)
                    "日" -> number
                    else -> null
                }
            }
            is String -> warranty.toIntOrNull()
            else -> null
        }

        // 解析日期
        fun parseDate(dateStr: String?): Date? {
            if (dateStr.isNullOrBlank()) return null
            return try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            } catch (e: Exception) {
                null
            }
        }

        // 创建Item对象
        val item = com.example.itemmanagement.data.model.Item(
            id = viewModel.getEditingItemId() ?: 0,
            name = nameValue,
            quantity = (values["数量"] as? String)?.toDoubleOrNull() ?: 0.0,
            unit = quantityUnit,
            location = com.example.itemmanagement.data.model.Location(
                area = values["位置_area"] as? String ?: "未指定",
                container = values["位置_container"] as? String,
                sublocation = values["位置_sublocation"] as? String
            ),
            category = values["分类"] as? String ?: "未指定",
            productionDate = parseDate(values["生产日期"] as? String),
            expirationDate = parseDate(values["保质过期时间"] as? String),
            openStatus = when(values["开封状态"]) {
                "已开封" -> com.example.itemmanagement.data.model.OpenStatus.OPENED
                "未开封" -> com.example.itemmanagement.data.model.OpenStatus.UNOPENED
                else -> null
            },
            openDate = parseDate(values["开封时间"] as? String),
            brand = values["品牌"] as? String,
            specification = values["规格"] as? String,
            stockWarningThreshold = (values["库存预警值"] as? String)?.toIntOrNull(),
            price = (values["单价"] as? String)?.toDoubleOrNull(),
            priceUnit = priceUnit,
            purchaseChannel = values["购买渠道"] as? String,
            storeName = values["商家名称"] as? String,
            subCategory = values["子分类"] as? String,
            customNote = values["备注"] as? String,
            season = seasonValue,
            capacity = (values["容量"] as? String)?.toDoubleOrNull(),
            capacityUnit = capacityUnit,
            rating = (values["评分"] as? String)?.toDoubleOrNull() ?: (values["评分"] as? Float)?.toDouble(),
            totalPrice = (values["总价"] as? String)?.toDoubleOrNull(),
            totalPriceUnit = totalPriceUnit,
            purchaseDate = parseDate(values["购买日期"] as? String),
            shelfLife = shelfLifeValue,
            warrantyPeriod = warrantyPeriodValue,
            warrantyEndDate = parseDate(values["保修到期时间"] as? String),
            serialNumber = values["序列号"] as? String,
            photos = photoList,
            addDate = parseDate(values["添加日期"] as? String) ?: Date(),
            status = com.example.itemmanagement.data.model.ItemStatus.IN_STOCK,
            tags = tagsList
        )

        // 保存物品
        viewModel.saveItem(item)
    }

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

    private fun checkAndRequestCameraPermission() {
        val cameraPermission = android.Manifest.permission.CAMERA

        when {
            // 检查相机权限
            androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), cameraPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                launchCamera()
            }
            // 如果应该显示权限说明
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                dialogFactory.createConfirmDialog(
                    title = "需要相机权限",
                    message = "我们需要相机权限来为物品拍照。请在接下来的对话框中允许使用相机。",
                    positiveButtonText = "确定",
                    onPositiveClick = {
                        requestCameraPermissions.launch(arrayOf(cameraPermission))
                    }
                )
            }
            else -> {
                requestCameraPermissions.launch(arrayOf(cameraPermission))
            }
        }
    }

    private fun checkAndRequestStoragePermission() {
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), permission) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                dialogFactory.createConfirmDialog(
                    title = "需要存储权限",
                    message = "需要存储权限才能访问相册。",
                    positiveButtonText = "确定",
                    onPositiveClick = {
                        requestStoragePermission.launch(arrayOf(permission))
                    }
                )
            }
            else -> {
                requestStoragePermission.launch(arrayOf(permission))
            }
        }
    }

    private val requestCameraPermissions = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            launchCamera()
        } else {
            showPermissionDeniedDialog("相机")
        }
    }

    private val requestStoragePermission = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            openGallery()
        } else {
            showPermissionDeniedDialog("存储")
        }
    }

    private fun showPermissionDeniedDialog(permissionType: String) {
        dialogFactory.createConfirmDialog(
            title = "权限请求",
            message = "需要${permissionType}权限才能继续操作。请在设置中开启相关权限。",
            positiveButtonText = "去设置",
            negativeButtonText = "取消",
            onPositiveClick = {
                // 跳转到应用设置页面
                startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", requireContext().packageName, null)
                })
            }
        )
    }

    private var currentPhotoUri: android.net.Uri? = null
    private var currentPhotoFile: java.io.File? = null
    
    private val takePicture = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoUri?.let { uri ->
                lifecycleScope.launch {
                    try {
                        val isValid = withContext(Dispatchers.IO) {
                            currentPhotoFile?.let { file ->
                                if (file.exists() && file.length() > 0) {
                                    // 压缩图片
                                    val compressedUri = compressImage(uri)
                                    if (compressedUri != null) {
                                        // 验证压缩后的URI
                                        val isUriValid = isUriValid(compressedUri)
                                        if (!isUriValid) {
                                            false
                                        } else {
                                            // 更新当前URI为压缩后的URI
                                            currentPhotoUri = compressedUri
                                            true
                                        }
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            } ?: false
                        }

                        withContext(Dispatchers.Main) {
                            if (isValid) {
                                try {
                                    currentPhotoUri?.let { validUri ->
                                        viewModel.addPhotoUri(validUri)
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "添加照片失败", Toast.LENGTH_SHORT).show()
                                    cleanupTempFiles()
                                }
                            } else {
                                Toast.makeText(context, "照片保存失败", Toast.LENGTH_SHORT).show()
                                cleanupTempFiles()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "处理照片时出错", Toast.LENGTH_SHORT).show()
                            cleanupTempFiles()
                        }
                    }
                }
            }
        } else {
            cleanupTempFiles()
        }
    }

    private val pickImage = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        uri?.let { sourceUri ->
            lifecycleScope.launch {
                try {
                    val copiedUri = withContext(Dispatchers.IO) {
                        // 先复制到私有存储
                        val tempUri = copyUriToPrivateStorage(sourceUri)

                        // 压缩复制后的图片
                        tempUri?.let { compressImage(it) }
                    }

                    withContext(Dispatchers.Main) {
                        copiedUri?.let { newUri ->
                            val isValid = withContext(Dispatchers.IO) {
                                isUriValid(newUri)
                            }

                            if (isValid) {
                                try {
                                    viewModel.addPhotoUri(newUri)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "添加照片失败", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "无法加载照片", Toast.LENGTH_SHORT).show()
                            }
                        } ?: run {
                            Toast.makeText(context, "无法加载照片", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "处理照片时出错", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun launchCamera() {
        try {
            // 确保存储目录存在
            val storageDir = requireContext().getExternalFilesDir("Photos")
            if (storageDir == null) {
                Toast.makeText(context, "无法创建存储目录", Toast.LENGTH_SHORT).show()
                return
            }

            if (!storageDir.exists()) {
                val dirCreated = storageDir.mkdirs()
                if (!dirCreated) {
                    Toast.makeText(context, "无法创建存储目录", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            // 创建临时文件
            val photoFile = createImageFile()
            if (photoFile == null) {
                Toast.makeText(context, "无法创建临时文件", Toast.LENGTH_SHORT).show()
                return
            }

            currentPhotoFile = photoFile

            // 生成URI
            try {
                val providerAuthority = "${requireContext().packageName}.provider"
                currentPhotoUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    providerAuthority,
                    photoFile
                )
            } catch (e: Exception) {
                Toast.makeText(context, "无法生成文件URI", Toast.LENGTH_SHORT).show()
                cleanupTempFiles()
                return
            }

            // 授予相机应用临时权限
            val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            intent.flags = android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION

            // 授予所有可能处理此Intent的应用权限
            val resInfoList = requireContext().packageManager.queryIntentActivities(
                intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                requireContext().grantUriPermission(
                    packageName,
                    currentPhotoUri,
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            takePicture.launch(currentPhotoUri)
        } catch (e: Exception) {
            Toast.makeText(context, "无法启动相机", Toast.LENGTH_SHORT).show()
            cleanupTempFiles()
        }
    }

    private fun openGallery() {
        try {
            pickImage.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开相册", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): java.io.File? {
        return try {
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val storageDir = requireContext().getExternalFilesDir("Photos")

            java.io.File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
        } catch (e: java.io.IOException) {
            null
        }
    }

    private fun cleanupTempFiles() {
        currentPhotoFile?.let {
            if (it.exists()) {
                try {
                    it.delete()
                } catch (e: Exception) {
                    // 忽略异常
                }
            }
        }
        currentPhotoFile = null
        currentPhotoUri = null
    }

    private fun compressImage(uri: android.net.Uri): android.net.Uri? {
        return try {
            // 获取图片的原始尺寸
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input, null, options)
            }

            // 计算采样率
            val maxDimension = 1024 // 最大尺寸
            var sampleSize = 1

            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val heightRatio = Math.round(options.outHeight.toFloat() / maxDimension.toFloat())
                val widthRatio = Math.round(options.outWidth.toFloat() / maxDimension.toFloat())
                sampleSize = if (heightRatio < widthRatio) widthRatio else heightRatio
            }

            // 使用采样率加载图片
            val compressOptions = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            // 加载并压缩图片
            val bitmap = requireContext().contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input, null, compressOptions)
            } ?: throw Exception("无法加载图片")

            // 创建压缩后的文件
            val compressedFile = createTempImageFile("COMPRESSED")
            java.io.FileOutputStream(compressedFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
            }

            // 释放Bitmap
            bitmap.recycle()

            // 生成新的URI
            androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                compressedFile
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun createTempImageFile(prefix: String): java.io.File {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        val imageFileName = "${prefix}_${timeStamp}"
        val storageDir = requireContext().getExternalFilesDir("Photos")
        return java.io.File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",        /* suffix */
            storageDir     /* directory */
        )
    }

    private fun copyUriToPrivateStorage(sourceUri: android.net.Uri): android.net.Uri? {
        return try {
            // 创建目标文件
            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val fileName = "IMG_${timeStamp}.jpg"
            val storageDir = requireContext().getExternalFilesDir("Photos")

            // 确保目录存在
            if (storageDir == null) {
                return null
            }

            if (!storageDir.exists()) {
                val dirCreated = storageDir.mkdirs()
                if (!dirCreated) {
                    return null
                }
            }

            val destinationFile = java.io.File(storageDir, fileName)

            // 复制文件内容
            requireContext().contentResolver.openInputStream(sourceUri)?.use { input ->
                java.io.FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 生成新的URI
            androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                destinationFile
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun isUriValid(uri: android.net.Uri): Boolean {
        return try {
            // 尝试打开输入流
            requireContext().contentResolver.openInputStream(uri)?.use { stream ->
                // 检查是否可以读取数据
                stream.available() > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理所有临时文件
        try {
            requireContext().getExternalFilesDir("Photos")?.listFiles()?.forEach { file ->
                if (file.name.startsWith("JPEG_") && file.name.endsWith(".jpg")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // 忽略异常
        }
    }

    private fun showPhotoViewDialog(uri: android.net.Uri) {
        // 显示照片对话框
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = android.widget.ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }

        // 加载图片
        com.bumptech.glide.Glide.with(this)
            .load(uri)
            .into(imageView)

        // 设置点击关闭
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(imageView)
        dialog.show()
    }

    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
        _binding = null
    }
} 