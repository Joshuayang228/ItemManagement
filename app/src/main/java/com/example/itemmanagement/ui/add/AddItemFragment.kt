package com.example.itemmanagement.ui.add

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentAddItemBinding
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.Location
import com.example.itemmanagement.data.model.OpenStatus
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.GridLayoutManager.LayoutParams
import com.bumptech.glide.Glide
import android.graphics.Rect
import android.widget.ImageView
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import android.util.Log
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class AddItemFragment : Fragment() {

    companion object {
        private const val PHOTOS_DIR = "Photos"
    }

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AddItemViewModel

    private lateinit var photoAdapter: PhotoAdapter
    private var currentPhotoUri: Uri? = null
    private var currentPhotoFile: File? = null

    // 工具类实例
    private lateinit var fieldViewFactory: FieldViewFactory
    private lateinit var dialogFactory: DialogFactory
    private lateinit var fieldValueManager: FieldValueManager

    // 字段视图映射
    private val fieldViews = mutableMapOf<String, View>()

    // 预定义的选项数据
    private val units = arrayOf("个", "件", "包", "盒", "瓶", "袋", "箱", "克", "千克", "升", "毫升")
    private val rooms = arrayOf("客厅", "主卧", "次卧", "厨房", "卫生间", "阳台", "储物间")
    private val categories = arrayOf("食品", "药品", "日用品", "电子产品", "衣物", "文具", "其他")

    // 权限请求
    private val requestCameraPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            launchCamera()
        } else {
            showPermissionDeniedDialog("相机")
        }
    }

    private val requestStoragePermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            openGallery()
        } else {
            showPermissionDeniedDialog("存储")
        }
    }

    // 创建临时图片文件
    private fun createTempImageFile(prefix: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "${prefix}_${timeStamp}"
        val storageDir = requireContext().getExternalFilesDir("Photos")
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",        /* suffix */
            storageDir     /* directory */
        )
    }

    // 图片压缩工具方法
    private fun compressImage(uri: Uri): Uri? {
        return try {
            // 获取图片的原始尺寸
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
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
            val compressOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }

            // 加载并压缩图片
            val bitmap = requireContext().contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, compressOptions)
            } ?: throw Exception("无法加载图片")

            // 创建压缩后的文件
            val compressedFile = createTempImageFile("COMPRESSED")
            FileOutputStream(compressedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            // 释放Bitmap
            bitmap.recycle()

            // 生成新的URI
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                compressedFile
            )
        } catch (e: Exception) {
            null
        }
    }

    // 相机结果
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (!isFragmentActive()) {
            cleanupTempFiles()
            return@registerForActivityResult
        }

        if (success) {
            currentPhotoUri?.let { uri ->
                viewModel.viewModelScope.launch {
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
                            if (!isFragmentActive()) {
                                cleanupTempFiles()
                                return@withContext
                            }

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
                            if (isFragmentActive()) {
                                Toast.makeText(context, "处理照片时出错", Toast.LENGTH_SHORT).show()
                            }
                            cleanupTempFiles()
                        }
                    }
                }
            }
        } else {
            cleanupTempFiles()
        }
    }

    // 图库结果
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (!isFragmentActive()) {
            return@registerForActivityResult
        }

        uri?.let { sourceUri ->
            viewModel.viewModelScope.launch {
                try {
                    val copiedUri = withContext(Dispatchers.IO) {
                        // 先复制到私有存储
                        val tempUri = copyUriToPrivateStorage(sourceUri)

                        // 压缩复制后的图片
                        tempUri?.let { compressImage(it) }
                    }

                    withContext(Dispatchers.Main) {
                        if (!isFragmentActive()) {
                            return@withContext
                        }

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
                        if (isFragmentActive()) {
                            Toast.makeText(context, "处理照片时出错", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // 复制URI到应用私有存储
    private fun copyUriToPrivateStorage(sourceUri: Uri): Uri? {
        if (!isFragmentActive()) {
            return null
        }

        return try {
            // 创建目标文件
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_${timeStamp}.jpg"
            val storageDir = requireContext().getExternalFilesDir(PHOTOS_DIR)

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

            val destinationFile = File(storageDir, fileName)

            // 复制文件内容
            requireContext().contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 生成新的URI
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                destinationFile
            )
        } catch (e: Exception) {
            null
        }
    }

    // 检查URI是否有效
    private fun isUriValid(uri: Uri): Boolean {
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

    // 检查Fragment是否处于活动状态
    private fun isFragmentActive(): Boolean {
        return _binding != null && isAdded && !isDetached && !isRemoving
    }

    // 清理临时文件
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        // 初始化ViewModel
        val repository = (requireActivity().application as com.example.itemmanagement.ItemManagementApplication).repository
        val factory = AddItemViewModelFactory(repository, requireActivity())
        viewModel = ViewModelProvider(requireActivity(), factory)[AddItemViewModel::class.java]

        // 初始化工具类
        dialogFactory = DialogFactory(requireContext())
        fieldViewFactory = FieldViewFactory(requireContext(), viewModel, dialogFactory, resources)
        fieldValueManager = FieldValueManager(requireContext(), viewModel, dialogFactory)
        
        // 处理导航参数
        arguments?.let { args ->
            // 获取模式参数
            val mode = args.getString("mode") ?: "add"
            // 设置模式
            viewModel.setMode(mode)
            
            // 根据模式处理参数
            if (mode == "edit") {
                // 编辑模式：获取itemId并加载物品
                val itemId = args.getLong("itemId", -1L)
                if (itemId != -1L) {
                    // 根据ID加载物品
                    viewModel.loadItemById(itemId)
                } else {
                    // 无效的itemId，不执行任何操作
                    Log.w("AddItemFragment", "编辑模式收到无效的itemId: -1")
                }
            } else {
                // 添加模式：处理Item对象参数
                if (args.containsKey("item")) {
                    val item = args.getParcelable<Item>("item")
                    item?.let {
                        // 将物品数据加载到ViewModel
                        viewModel.loadItemData(it)
                    }
                } else {
                    // 正常的添加模式：调用prepareForAddMode准备全新的状态
                    viewModel.prepareForAddMode()
                }
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

        // 观察模式变化
        viewModel.mode.observe(viewLifecycleOwner) { mode ->
            // 根据模式设置标题和按钮文本
            if (mode == "edit") {
                requireActivity().title = "编辑物品"
                binding.saveButton.text = "更新物品"
            } else {
                requireActivity().title = "添加物品"
                binding.saveButton.text = "保存物品"
            }
        }

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

        // 观察ViewModel中的图片URI列表变化
        viewModel.photoUris.observe(viewLifecycleOwner) { uris ->
            photoAdapter.setPhotos(uris)
        }

        // 恢复已保存的图片
        val savedPhotos = viewModel.getPhotoUris()
        if (savedPhotos.isNotEmpty()) {
            photoAdapter.setPhotos(savedPhotos)
        }

        initializeDefaultFields()
        setupViews()
        observeSelectedFields()
        setupButtons()
        hideBottomNavigation()
        
        // 观察保存结果
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            // success可能为null，需要做null判断
            if (success == true) {
                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                
                // 清除所有数据和草稿，确保下次进入是全新状态
                viewModel.clearAllData()
                viewModel.clearAddDraftAfterSave()
                
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

    private fun initializeDefaultFields() {
        // 只在首次创建时且不是编辑模式时初始化默认字段
        if (viewModel.getSelectedFieldsValue().isEmpty() && viewModel.mode.value != "edit") {
            viewModel.initializeDefaultFieldProperties()
            // 设置默认选中的字段
            listOf(
                "名称",
                "数量",
                "位置",
                "分类",
                "生产日期",
                "保质过期时间"
            ).forEach { fieldName ->
                viewModel.updateFieldSelection(Field("", fieldName, fieldName == "名称"), true)
            }
        }
    }

    private fun setupViews() {
        // 移除了addPhotoButton的点击事件设置，因为现在使用RecyclerView中的添加项
    }

    private fun showPhotoSelectionDialog() {
        if (!isFragmentActive()) return

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
        if (!isFragmentActive()) return

        val cameraPermission = Manifest.permission.CAMERA

        when {
            // 检查相机权限
            ContextCompat.checkSelfPermission(requireContext(), cameraPermission) == PackageManager.PERMISSION_GRANTED -> {
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
        if (!isFragmentActive()) return

        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
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

    private fun launchCamera() {
        if (!isFragmentActive()) return

        try {
            // 确保存储目录存在
            val storageDir = requireContext().getExternalFilesDir(PHOTOS_DIR)
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
                currentPhotoUri = FileProvider.getUriForFile(
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
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION

            // 授予所有可能处理此Intent的应用权限
            val resInfoList = requireContext().packageManager.queryIntentActivities(
                intent, PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                requireContext().grantUriPermission(
                    packageName,
                    currentPhotoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            takePicture.launch(currentPhotoUri)
        } catch (e: Exception) {
            Toast.makeText(context, "无法启动相机", Toast.LENGTH_SHORT).show()
            cleanupTempFiles()
        }
    }

    private fun openGallery() {
        if (!isFragmentActive()) return

        try {
            pickImage.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(context, "无法打开相册", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = requireContext().getExternalFilesDir(PHOTOS_DIR)

            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            )
        } catch (e: IOException) {
            null
        }
    }

    private fun observeSelectedFields() {
        viewModel.selectedFields.observe(viewLifecycleOwner) { fields ->
            updateFields(fields)
        }
    }

    private fun updateFields(fields: Set<Field>) {
        // 保存当前字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }

        binding.fieldsContainer.removeAllViews()
        fieldViews.clear()

        // 使用Field类中定义的order属性进行排序
        val sortedFields = fields.sortedBy { it.order }

        sortedFields.forEach { field ->
            val fieldView = fieldViewFactory.createFieldView(field)
            binding.fieldsContainer.addView(fieldView)
            fieldViews[field.name] = fieldView
        }

        // 恢复保存的字段值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.restoreFieldValues(fieldViews)
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

    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    private fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        } catch (e: Exception) {
            null
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
                startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                })
            }
        )
    }

    override fun onPause() {
        super.onPause()
        // 在Fragment暂停时保存所有字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理所有临时文件
        try {
            requireContext().getExternalFilesDir(PHOTOS_DIR)?.listFiles()?.forEach { file ->
                if (file.name.startsWith("JPEG_") && file.name.endsWith(".jpg")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // 忽略异常
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_add_item, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                showClearConfirmDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showPhotoViewDialog(uri: Uri) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // 加载图片
        Glide.with(this)
            .load(uri)
            .into(imageView)

        // 设置点击关闭
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(imageView)
        dialog.show()
    }

    private fun showEditFieldsDialog() {
        EditFieldsFragment.newInstance().show(
            childFragmentManager,
            "EditFieldsFragment"
        )
    }

    private fun saveItem() {
        // 在保存之前确保所有字段的值都已保存
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

        // 处理保质期和保修期数据 - 这些字段通常保存为Pair<String, String>格式
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

        // 处理带单位的字段
        val quantityUnit = values["数量_unit"] as? String ?: ""
        val capacityUnit = values["容量_unit"] as? String ?: ""
        val priceUnit = values["单价_unit"] as? String ?: "元"
        val totalPriceUnit = values["总价_unit"] as? String ?: "元"

        // 创建物品对象
        val item = Item(
            // 如果是编辑模式，保留原有ID；否则使用默认值0
            id = if (viewModel.mode.value == "edit") viewModel.getEditingItemId() ?: 0 else 0,
            name = nameValue,
            quantity = (values["数量"] as? String)?.toDoubleOrNull() ?: 0.0,
            unit = quantityUnit,
            location = Location(
                area = values["位置_area"] as? String ?: "未指定",
                container = values["位置_container"] as? String,
                sublocation = values["位置_sublocation"] as? String
            ),
            category = values["分类"] as? String ?: "未指定",
            productionDate = parseDate(values["生产日期"] as? String),
            expirationDate = parseDate(values["保质过期时间"] as? String),
            // 确保开封状态有默认值
            openStatus = when(values["开封状态"]) {
                "已开封" -> OpenStatus.OPENED
                else -> OpenStatus.UNOPENED // 默认为未开封
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
            // 使用当前时间作为添加日期，或者使用用户输入的添加日期
            addDate = parseDate(values["添加日期"] as? String) ?: Date(),
            // 其他默认值
            status = ItemStatus.IN_STOCK,
            tags = tagsList
        )

        // 保存物品
        viewModel.saveItem(item)
    }

    private fun showClearConfirmDialog() {
        dialogFactory.createConfirmDialog(
            title = "确认清除",
            message = "确定要清除所有已输入的信息吗？",
            positiveButtonText = "确定",
            negativeButtonText = "取消",
            onPositiveClick = {
                clearAllFields()
            }
        )
    }

    private fun clearAllFields() {
        // 清除ViewModel中的字段值和照片，但保留已选中的字段
        viewModel.clearFieldValuesOnly()

        // 通过fieldValueManager恢复空值到UI
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.restoreFieldValues(fieldViews)
        }

        // 显示清除成功提示
        Toast.makeText(context, "已清除输入信息", Toast.LENGTH_SHORT).show()
    }

    // 添加一个方法来重置为添加模式
    private fun resetToAddMode() {
        // 清除所有数据并重置为添加模式
        viewModel.resetToAddMode()
        
        // 重新初始化默认字段
        initializeDefaultFields()
        
        // 重新设置视图
        setupViews()
    }
}