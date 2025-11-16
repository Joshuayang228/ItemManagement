package com.example.itemmanagement.ui.detail

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentItemDetailBinding
import com.example.itemmanagement.ui.detail.adapter.PhotoAdapter
import com.example.itemmanagement.ui.photo.FullscreenPhotoActivity
import com.example.itemmanagement.ui.detail.adapter.TagAdapter
import com.example.itemmanagement.adapter.PriceRecordAdapter
import com.example.itemmanagement.data.model.OpenStatus
import com.example.itemmanagement.utils.SnackbarHelper
import com.google.android.material.chip.Chip
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartAnimationType
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Material Design 3 精美物品详情Fragment - 基于备份样式重新设计
 */
class ItemDetailFragment : Fragment() {
    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemDetailViewModel by viewModels {
        ItemDetailViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    private val args: ItemDetailFragmentArgs by navArgs()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var photoAdapter: PhotoAdapter
    // tagAdapter 已移除，现在使用动态创建的 Chip
    
    // 价格记录适配器（用于来源信息的价格跟踪）
    private lateinit var sourcePriceRecordAdapter: PriceRecordAdapter
    private var isShowingAllSourceRecords = false

    // 备注展开状态
    private var isNoteExpanded = false
    
    // 来源信息展开状态
    private var isSourceExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_item_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                navigateToEditItem()
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            R.id.action_add_to_shopping_list -> {
                showAddToShoppingListDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("删除物品")
            .setMessage("确定要删除此物品吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteItem(args.itemId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun navigateToEditItem() {
        try {
            val action = ItemDetailFragmentDirections.actionNavigationItemDetailToEditItemFragment(
                itemId = args.itemId
            )
            findNavController().navigate(action)
        } catch (e: Exception) {
            SnackbarHelper.show(requireView(), "编辑功能暂时不可用")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        hideBottomNavigation()
        
        setupAdapters()
        setupNoteExpandButton()
        setupSourceExpandButton()
        viewModel.loadItem(args.itemId)
        observeItem()
        observeError()
        observeNavigation()
        observeSourceInfo()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
        
        // 延迟检查并强制隐藏（防止被其他机制恢复）
        binding.root.postDelayed({
            activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
        }, 100)
        
        binding.root.postDelayed({
            activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
        }, 250)
        
        binding.root.postDelayed({
            activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
        }, 400)
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    private fun setupAdapters() {
        // 设置照片适配器
        photoAdapter = PhotoAdapter { photoUri, position ->
            // 点击照片时显示大图
            showFullScreenPhoto(photoUri, position)
        }
        binding.photoViewPager.adapter = photoAdapter
        
        // 添加页面变化监听器，更新照片计数文本
        binding.photoViewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 更新照片计数文本 (例如: "1/5")
                val totalPhotos = photoAdapter.itemCount
                if (totalPhotos > 0) {
                    binding.photoCountText.text = "${position + 1}/$totalPhotos"
                    binding.photoCountText.visibility = View.VISIBLE
                } else {
                    binding.photoCountText.visibility = View.GONE
                }
            }
        })

        // 标签现在使用HorizontalScrollView中的LinearLayout，不需要RecyclerView设置
    }

    private fun setupNoteExpandButton() {
        binding.expandButton.setOnClickListener {
            toggleNoteExpansion()
        }
    }

    private fun toggleNoteExpansion() {
        isNoteExpanded = !isNoteExpanded
        if (isNoteExpanded) {
            binding.customNoteTextView.maxLines = Int.MAX_VALUE
            binding.expandButton.text = "收起"
        } else {
            binding.customNoteTextView.maxLines = 5
            binding.expandButton.text = "展开"
        }
    }
    
    private fun setupSourceExpandButton() {
        // 找到来源信息卡片（整个卡片可点击）
        val sourceInfoCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(
            R.id.sourceInfoCard
        )
        sourceInfoCard?.setOnClickListener {
            toggleSourceExpansion()
        }
    }
    
    private fun toggleSourceExpansion() {
        isSourceExpanded = !isSourceExpanded
        
        val expandIcon = binding.root.findViewById<android.widget.ImageView>(
            R.id.expandIcon
        )
        val shoppingDetailsContainer = binding.root.findViewById<LinearLayout>(
            R.id.shoppingDetailsContainer
        )
        
        if (isSourceExpanded) {
            // 展开状态
            shoppingDetailsContainer?.visibility = View.VISIBLE
            // 旋转图标向上（180度）
            expandIcon?.animate()?.rotation(180f)?.setDuration(200)?.start()
        } else {
            // 收起状态
            shoppingDetailsContainer?.visibility = View.GONE
            // 旋转图标向下（0度）
            expandIcon?.animate()?.rotation(0f)?.setDuration(200)?.start()
        }
    }

    private fun observeNavigation() {
        viewModel.navigateBack.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                findNavController().navigateUp()
                viewModel.onNavigationComplete()
            }
        }
    }
    
    private fun observeSourceInfo() {
        viewModel.shoppingSource.observe(viewLifecycleOwner) { shoppingDetail ->
            android.util.Log.d("ItemDetailFragment", "🔍 来源信息变化: $shoppingDetail")
            updateSourceInfoCard(shoppingDetail)
        }
        
        // 观察价格记录
        viewModel.sourcePriceRecords.observe(viewLifecycleOwner) { records ->
            android.util.Log.d("ItemDetailFragment", "🔍 价格记录变化: ${records.size} 条")
            updateSourcePriceTracking(records)
        }
        
        // 观察保修信息
        viewModel.warranty.observe(viewLifecycleOwner) { warranty ->
            android.util.Log.d("ItemDetailFragment", "🔍 保修信息变化: $warranty")
            updateWarrantyInfo(warranty)
        }
    }

    private fun observeItem() {
        viewModel.item.observe(viewLifecycleOwner) { item ->
            binding.apply {
                // 基本信息
                nameTextView.text = item.name
                quantityTextView.text = "${formatNumber(item.quantity)} ${item.unit ?: "个"}"
                locationTextView.text = item.location?.getFullLocationString() ?: "未设置"
                
                // GPS地点信息
                android.util.Log.d("ItemDetailFragment", "📍 物品地点信息 - 地址: ${item.locationAddress}")
                if (!item.locationAddress.isNullOrBlank()) {
                    android.util.Log.d("ItemDetailFragment", "📍 显示地点信息")
                    gpsLocationContainer.visibility = View.VISIBLE
                    gpsLocationAddressTextView.text = item.locationAddress
                    
                    // 显示地图卡片（如果有GPS坐标）
                    if (item.locationLatitude != null && item.locationLongitude != null) {
                        setupMapToggle(item.name, item.locationAddress ?: "", item.locationLatitude!!, item.locationLongitude!!)
                    } else {
                        hideMapCard()
                    }
                } else {
                    android.util.Log.d("ItemDetailFragment", "📍 地点信息为空，隐藏地点容器")
                    gpsLocationContainer.visibility = View.GONE
                    hideMapCard()
                }
                
                ratingBar.rating = item.rating?.toFloat() ?: 0f

                // 分类 - 使用Material Design 3 Chips
                setupCategoryChips(item.category, item.subCategory)
                
                // 开封状态
                openStatusTextView.text = getOpenStatusText(item.openStatus)

                // 价格信息
                priceTextView.text = "${formatNumber(item.price ?: 0.0)} ${item.priceUnit ?: "元"}"
                totalPriceTextView.text = "${formatNumber(item.totalPrice ?: 0.0)} ${item.totalPriceUnit ?: "元"}"

                // 标签 - 转换domain模型为Entity模型
                // 更新标签显示 - 使用动态创建的 Chip
                setupTagChips(item.tags)

                // 日期信息
                addDateTextView.text = item.addDate?.let { dateFormat.format(it) } ?: "未设置"
                purchaseDateTextView.text = item.purchaseDate?.let { dateFormat.format(it) } ?: "未设置"
                productionDateTextView.text = item.productionDate?.let { dateFormat.format(it) } ?: "未设置"
                openDateTextView.text = item.openDate?.let { dateFormat.format(it) } ?: "未设置"
                expirationDateTextView.text = item.expirationDate?.let { dateFormat.format(it) } ?: "未设置"
                // 保修信息已移至 WarrantyEntity，由 observeSourceInfo() 中的 warranty 观察者更新
                // warrantyEndDateTextView.text = item.warrantyEndDate?.let { dateFormat.format(it) } ?: "未设置"

                // 详细信息
                brandTextView.text = item.brand ?: "未设置"
                capacityTextView.text = buildCapacityString(item.capacity, item.capacityUnit)
                seasonTextView.text = item.season ?: "未设置"
                purchaseChannelTextView.text = item.purchaseChannel ?: "未设置"
                storeNameTextView.text = item.storeName ?: "未设置"
                serialNumberTextView.text = item.serialNumber ?: "未设置"
                shelfLifeTextView.text = buildShelfLifeString(item.shelfLife)
                // 保修信息已移至 WarrantyEntity，由 observeSourceInfo() 中的 warranty 观察者更新
                // warrantyTextView.text = buildWarrantyString(item.warrantyPeriod)
                specificationTextView.text = item.specification ?: "未设置"
                customNoteTextView.text = item.customNote ?: "无备注"

                // 照片 - 转换domain模型为Entity模型
                val photoEntities = item.photos.map { photo ->
                    com.example.itemmanagement.data.entity.PhotoEntity(
                        id = photo.id,
                        itemId = item.id,
                        uri = photo.uri,
                        isMain = photo.isMain,
                        displayOrder = 0
                    )
                }
                photoAdapter.submitList(photoEntities)
                updatePhotoIndicator(photoEntities.size)

                // 状态标签
                updateStatusTag(item)

                // 保修进度条
                updateWarrantyProgress(item)

                // 备注展开按钮显示控制
                updateNoteExpandButton(item.customNote)

                // 卡片可见性控制
                updateCardVisibility(item)
            }
        }
    }

    private fun setupCategoryChips(category: String?, subCategory: String?) {
        binding.apply {
            // 主分类Chip
            if (!category.isNullOrBlank()) {
                categoryChip.text = category
                categoryChip.visibility = View.VISIBLE
            } else {
                categoryChip.visibility = View.GONE
            }

            // 子分类Chip
            if (!subCategory.isNullOrBlank()) {
                subCategoryChip.text = subCategory
                subCategoryChip.visibility = View.VISIBLE
            } else {
                subCategoryChip.visibility = View.GONE
            }
        }
    }

    private fun getOpenStatusText(openStatus: OpenStatus?): String {
        return when (openStatus) {
            OpenStatus.UNOPENED -> "未开封"
            OpenStatus.OPENED -> "已开封"
            null -> "未设置"
        }
    }

    private fun buildCapacityString(capacity: Double?, unit: String?): String {
        return if (capacity != null) {
            "${formatNumber(capacity)} ${unit ?: ""}"
        } else {
            "未设置"
        }
    }

    private fun buildShelfLifeString(shelfLife: Int?): String {
        return if (shelfLife != null && shelfLife > 0) {
            "${shelfLife}天"
        } else {
            "未设置"
        }
    }
    
    /**
     * 更新保修信息显示
     */
    private fun updateWarrantyInfo(warranty: com.example.itemmanagement.data.entity.WarrantyEntity?) {
        binding.apply {
            if (warranty != null) {
                // 保修期（转换为天数）
                val warrantyDays = warranty.warrantyPeriodMonths * 30
                warrantyTextView.text = "${warranty.warrantyPeriodMonths}个月（约${warrantyDays}天）"
                warrantyContainer.visibility = View.VISIBLE
                
                // 保修到期日期
                warrantyEndDateTextView.text = dateFormat.format(warranty.warrantyEndDate)
                warrantyEndContainer.visibility = View.VISIBLE
            } else {
                warrantyTextView.text = "未设置"
                warrantyContainer.visibility = View.GONE
                warrantyEndDateTextView.text = "未设置"
                warrantyEndContainer.visibility = View.GONE
            }
        }
    }

    private fun updatePhotoIndicator(photoCount: Int) {
        // 只更新照片计数文本
        if (photoCount > 0) {
            binding.photoCountText.text = "1/$photoCount"
            binding.photoCountText.visibility = View.VISIBLE
        } else {
            binding.photoCountText.visibility = View.GONE
        }
    }
    
    /**
     * 显示全屏照片对话框
     */
    private fun showFullScreenPhoto(photoUri: String, initialPosition: Int) {
        val photos = photoAdapter.currentList
        if (photos.isEmpty()) {
            return
        }

        val resolvedPosition = initialPosition.coerceIn(0, photos.lastIndex)
        val uriList = photos.map { it.uri }

        val intent = FullscreenPhotoActivity.createIntent(
            requireContext(),
            uriList,
            resolvedPosition
        )
        startActivity(intent)
    }

    private fun updateStatusTag(item: com.example.itemmanagement.data.model.Item) {
        android.util.Log.d("ItemDetailFragment", "━━━━━ updateStatusTag ━━━━━")
        val statusText = calculateItemStatus(item)
        android.util.Log.d("ItemDetailFragment", "计算得到的状态文本: $statusText")
        if (statusText != null) {
            android.util.Log.d("ItemDetailFragment", "显示状态标签: $statusText")
            binding.statusTagView.apply {
                text = statusText
                visibility = View.VISIBLE
                // 根据状态设置Material Design 3颜色
                when (statusText) {
                    "过期" -> {
                        chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_red_light)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    "临期" -> {
                        chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_orange_light)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    "已借出" -> {
                        chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_purple)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                    else -> {
                        chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), android.R.color.holo_blue_light)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    }
                }
            }
        } else {
            binding.statusTagView.visibility = View.GONE
        }
    }

    private fun calculateItemStatus(item: com.example.itemmanagement.data.model.Item): String? {
        android.util.Log.d("ItemDetailFragment", "━━━━━ calculateItemStatus ━━━━━")
        android.util.Log.d("ItemDetailFragment", "物品名称: ${item.name}")
        android.util.Log.d("ItemDetailFragment", "物品ID: ${item.id}")
        android.util.Log.d("ItemDetailFragment", "物品状态: ${item.status}")
        
        // 优先检查借出状态
        if (item.status == com.example.itemmanagement.data.model.ItemStatus.BORROWED) {
            android.util.Log.d("ItemDetailFragment", "✅ 状态匹配BORROWED，返回'已借出'")
            return "已借出"
        }
        android.util.Log.d("ItemDetailFragment", "状态不是BORROWED，检查过期状态...")
        
        // 检查过期状态
        val expirationDate = item.expirationDate ?: return null
        val now = Date()
        val diffInMillis = expirationDate.time - now.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return when {
            diffInDays < 0 -> "过期"
            diffInDays <= 7 -> "临期"
            diffInDays <= 30 -> "即将过期"
            else -> null
        }
    }

    private fun updateWarrantyProgress(item: com.example.itemmanagement.data.model.Item) {
        // 保修信息已移至 WarrantyEntity，保修进度通过保修管理查看
        // 此方法已废弃
    }

    private fun updateNoteExpandButton(customNote: String?) {
        if (customNote != null && customNote.length > 100) {
            binding.expandButton.visibility = View.VISIBLE
            binding.customNoteTextView.maxLines = 5
        } else {
            binding.expandButton.visibility = View.GONE
            binding.customNoteTextView.maxLines = Int.MAX_VALUE
        }
    }

    private fun updateCardVisibility(item: com.example.itemmanagement.data.model.Item) {
        binding.apply {
            // 基本信息卡片 - 总是显示
            basicInfoCard.visibility = View.VISIBLE
            
            // 状态卡片（保修信息已移至 WarrantyEntity）
            statusCard.visibility = if (item.rating != null || item.tags.isNotEmpty() || 
                                       item.shelfLife != null ||
                                       item.season != null || item.openStatus != null) View.VISIBLE else View.GONE
            
            // 日期信息卡片（保修日期已移至 WarrantyEntity）
            dateCard.visibility = if (item.addDate != null || item.purchaseDate != null || 
                                     item.productionDate != null || item.expirationDate != null) View.VISIBLE else View.GONE
            
            // 商业信息卡片
            commercialCard.visibility = if (item.brand != null || item.purchaseChannel != null || 
                                           item.storeName != null || item.serialNumber != null ||
                                           item.specification != null) View.VISIBLE else View.GONE

            // 备注卡片
            noteCard.visibility = if (item.customNote != null) View.VISIBLE else View.GONE

            // 具体字段的可见性 - 基本信息卡片
            capacityContainer.visibility = if (item.capacity != null) View.VISIBLE else View.GONE
            priceContainer.visibility = if (item.price != null) View.VISIBLE else View.GONE
            totalPriceContainer.visibility = if (item.totalPrice != null) View.VISIBLE else View.GONE
            categoryContainer.visibility = if (item.category != null || item.subCategory != null) View.VISIBLE else View.GONE
            locationContainer.visibility = if (item.location != null) View.VISIBLE else View.GONE
            
            // 状态卡片字段（保修信息已移至 WarrantyEntity）
            shelfLifeContainer.visibility = if (item.shelfLife != null && item.shelfLife > 0) View.VISIBLE else View.GONE
            // warrantyContainer.visibility = if (item.warrantyPeriod != null && item.warrantyPeriod > 0) View.VISIBLE else View.GONE
            seasonContainer.visibility = if (item.season != null) View.VISIBLE else View.GONE
            openStatusContainer.visibility = if (item.openStatus != null) View.VISIBLE else View.GONE
            ratingContainer.visibility = if (item.rating != null) View.VISIBLE else View.GONE
            tagsContainer.visibility = if (item.tags.isNotEmpty()) View.VISIBLE else View.GONE
            
            // 日期信息卡片字段
            purchaseDateContainer.visibility = if (item.purchaseDate != null) View.VISIBLE else View.GONE
            productionDateContainer.visibility = if (item.productionDate != null) View.VISIBLE else View.GONE
            openDateContainer.visibility = if (item.openDate != null) View.VISIBLE else View.GONE
            expirationDateContainer.visibility = if (item.expirationDate != null) View.VISIBLE else View.GONE
            // 保修日期已移至 WarrantyEntity
            // warrantyEndContainer.visibility = if (item.warrantyEndDate != null) View.VISIBLE else View.GONE
            
            // 商业信息卡片字段
            brandContainer.visibility = if (item.brand != null) View.VISIBLE else View.GONE
            purchaseChannelContainer.visibility = if (item.purchaseChannel != null) View.VISIBLE else View.GONE
            storeNameContainer.visibility = if (item.storeName != null) View.VISIBLE else View.GONE
            serialNumberContainer.visibility = if (item.serialNumber != null) View.VISIBLE else View.GONE
            specificationContainer.visibility = if (item.specification != null) View.VISIBLE else View.GONE
        }
    }

    private fun observeError() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                SnackbarHelper.showError(requireView(), errorMessage)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }

    /**
     * 设置标签Chips - 与分类Chip保持完全一致的样式
     */
    /**
     * 设置标签Chips - 统一粉色系
     * 🎨 简洁统一的视觉风格，与购物物品详情页保持一致
     */
    private fun setupTagChips(tags: List<com.example.itemmanagement.data.model.Tag>) {
        val tagsLayout = binding.root.findViewById<LinearLayout>(R.id.tagsLinearLayout)
        tagsLayout.removeAllViews()
        
        tags.forEachIndexed { index, tag ->
            val chip = com.google.android.material.chip.Chip(requireContext())
            chip.text = tag.name
            
            // 🎯 统一交互设置
            chip.isClickable = true
            chip.isFocusable = true
            chip.isCheckable = false
            
            // 🎨 统一绿色系背景 - 自然、标签专属
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#E8F5E8") // 🟢 浅绿
            )
            
            // 文字色使用主题色
            val typedValue = android.util.TypedValue()
            val theme = requireContext().theme
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSecondaryContainer, typedValue, true)
            chip.setTextColor(typedValue.data)
            
            chip.chipStrokeWidth = 0f
            chip.isCloseIconVisible = false
            chip.textSize = 12f
            
            // 📐 统一边距设置
            val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            // 右边距：最后一个chip不需要右边距，其他chip设置8dp右边距
            val rightMargin = if (index == tags.size - 1) 0 else 8.dpToPx()
            layoutParams.setMargins(0, -4.dpToPx(), rightMargin, -4.dpToPx())
            chip.layoutParams = layoutParams
            
            // 🖱️ 添加点击事件 - 提供触觉反馈
            chip.setOnClickListener {
                chip.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
            
            tagsLayout.addView(chip)
        }
    }

    /**
     * dp转px的扩展函数
     */
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun formatNumber(number: Double): String {
        return if (number == number.toInt().toDouble()) {
            number.toInt().toString()
        } else {
            String.format(Locale.getDefault(), "%.2f", number)
        }
    }
    
    /**
     * 更新来源信息卡片
     */
    private fun updateSourceInfoCard(shoppingDetail: com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity?) {
        val sourceInfoCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(
            R.id.sourceInfoCard
        )
        val sourceIcon = binding.root.findViewById<android.widget.ImageView>(
            R.id.sourceIcon
        )
        val sourceTypeTextView = binding.root.findViewById<TextView>(R.id.sourceTypeTextView)
        val expandIcon = binding.root.findViewById<android.widget.ImageView>(
            R.id.expandIcon
        )
        val shoppingDetailsContainer = binding.root.findViewById<LinearLayout>(R.id.shoppingDetailsContainer)
        
        if (shoppingDetail != null) {
            // 来自购物清单转入
            android.util.Log.d("ItemDetailFragment", "✅ 物品来自购物清单转入")
            sourceTypeTextView?.text = "来自购物清单转入"
            sourceIcon?.setImageResource(R.drawable.ic_shopping)
            expandIcon?.visibility = View.VISIBLE
            sourceInfoCard?.isClickable = true
            sourceInfoCard?.isFocusable = true
            sourceInfoCard?.visibility = View.VISIBLE
            
            // 填充购物详情数据
            updateShoppingDetailsCard(shoppingDetail)
        } else {
            // ⭐ 手动添加的物品，隐藏整个来源信息卡片
            android.util.Log.d("ItemDetailFragment", "📝 物品为手动添加，隐藏来源信息卡片")
            sourceInfoCard?.visibility = View.GONE
        }
    }
    
    /**
     * 更新购物详情卡片的数据
     */
    private fun updateShoppingDetailsCard(shoppingDetail: com.example.itemmanagement.data.entity.unified.ShoppingDetailEntity) {
        // 预估价格
        val estimatedPriceTextView = binding.root.findViewById<TextView>(R.id.estimatedPriceTextView)
        estimatedPriceTextView?.text = "¥${formatNumber(shoppingDetail.estimatedPrice ?: 0.0)}"
        
        // 商店
        val storeTextView = binding.root.findViewById<TextView>(R.id.storeTextView)
        storeTextView?.text = shoppingDetail.storeName ?: "未设置"
        
        // 优先级
        val priorityTextView = binding.root.findViewById<TextView>(R.id.priorityTextView)
        priorityTextView?.text = when(shoppingDetail.priority) {
            com.example.itemmanagement.data.entity.ShoppingItemPriority.CRITICAL -> "关键"
            com.example.itemmanagement.data.entity.ShoppingItemPriority.HIGH -> "重要"
            com.example.itemmanagement.data.entity.ShoppingItemPriority.NORMAL -> "一般"
            com.example.itemmanagement.data.entity.ShoppingItemPriority.LOW -> "次要"
            else -> "未设置"
        }
        
        // 紧急程度
        val urgencyTextView = binding.root.findViewById<TextView>(R.id.urgencyTextView)
        urgencyTextView?.text = when(shoppingDetail.urgencyLevel) {
            com.example.itemmanagement.data.entity.UrgencyLevel.URGENT -> "紧急"
            com.example.itemmanagement.data.entity.UrgencyLevel.NORMAL -> "普通"
            com.example.itemmanagement.data.entity.UrgencyLevel.NOT_URGENT -> "不紧急"
            else -> "未设置"
        }
        
        // 购买状态
        val purchaseStatusTextView = binding.root.findViewById<TextView>(R.id.purchaseStatusTextView)
        purchaseStatusTextView?.text = if (shoppingDetail.isPurchased) "已购买" else "未购买"
        
        // 购物备注（如果有）
        val shoppingNoteCard = binding.root.findViewById<com.google.android.material.card.MaterialCardView>(
            R.id.shoppingNoteCard
        )
        val shoppingNoteTextView = binding.root.findViewById<TextView>(R.id.shoppingNoteTextView)
        
        // 注意：购物备注应该从 UnifiedItemEntity 的 customNote 获取
        // 但这里我们在购物详情展开区域，可以显示购物时记录的备注
        // 实际上购物清单没有单独的备注字段，都使用 customNote
        shoppingNoteCard?.visibility = View.GONE // 隐藏备注卡片，因为已在主备注卡片显示
    }
    
    /**
     * 更新来源信息的价格跟踪卡片
     */
    private fun updateSourcePriceTracking(records: List<com.example.itemmanagement.data.entity.PriceRecord>) {
        val priceTrackingCard = binding.root.findViewById<View>(R.id.sourcePriceTrackingCardInclude) ?: return
        
        val emptyChartText = priceTrackingCard.findViewById<TextView>(R.id.emptyChartText)
        val chartContainer = priceTrackingCard.findViewById<FrameLayout>(R.id.chartContainer)
        val statsLayout = priceTrackingCard.findViewById<LinearLayout>(R.id.statsLayout)
        val recentRecordsLayout = priceTrackingCard.findViewById<LinearLayout>(R.id.recentRecordsLayout)
        val recordCountText = priceTrackingCard.findViewById<TextView>(R.id.recordCountText)
        val maxPriceText = priceTrackingCard.findViewById<TextView>(R.id.maxPriceText)
        val avgPriceText = priceTrackingCard.findViewById<TextView>(R.id.avgPriceText)
        val minPriceText = priceTrackingCard.findViewById<TextView>(R.id.minPriceText)
        val recentRecordsRecyclerView = priceTrackingCard.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recentRecordsRecyclerView)
        val btnShowAllRecords = priceTrackingCard.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShowAllRecords)
        
        // 初始化适配器（如果还没初始化）
        if (!::sourcePriceRecordAdapter.isInitialized) {
            sourcePriceRecordAdapter = PriceRecordAdapter(
                onDeleteClick = { /* 来源信息中的价格记录不允许删除 */ }
            )
            recentRecordsRecyclerView?.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = sourcePriceRecordAdapter
            }
        }
        
        recordCountText?.text = "(${records.size}条)"
        
        if (records.isEmpty()) {
            // 空状态
            emptyChartText?.visibility = View.VISIBLE
            chartContainer?.visibility = View.GONE
            statsLayout?.visibility = View.GONE
            recentRecordsLayout?.visibility = View.GONE
        } else {
            // 有数据状态
            emptyChartText?.visibility = View.GONE
            chartContainer?.visibility = View.VISIBLE
            statsLayout?.visibility = View.VISIBLE
            recentRecordsLayout?.visibility = View.VISIBLE
            
            // 更新统计
            val maxPrice = records.maxOfOrNull { it.price } ?: 0.0
            val minPrice = records.minOfOrNull { it.price } ?: 0.0
            val avgPrice = records.map { it.price }.average()
            
            maxPriceText?.text = "¥${maxPrice.toInt()}"
            avgPriceText?.text = "¥${avgPrice.toInt()}"
            minPriceText?.text = "¥${minPrice.toInt()}"
            
            // 显示记录
            val displayRecords = if (isShowingAllSourceRecords) records else records.take(3)
            sourcePriceRecordAdapter.submitList(displayRecords, records.size)
            
            // 控制展示全部按钮
            if (records.size > 3) {
                btnShowAllRecords?.visibility = View.VISIBLE
                btnShowAllRecords?.text = if (isShowingAllSourceRecords) "收起" else "展示全部 (${records.size})"
            } else {
                btnShowAllRecords?.visibility = View.GONE
            }
            
            // 更新折线图
            updateSourcePriceChart(priceTrackingCard, records)
        }
        
        // 展示全部/收起按钮
        btnShowAllRecords?.setOnClickListener {
            isShowingAllSourceRecords = !isShowingAllSourceRecords
            val displayRecords = if (isShowingAllSourceRecords) records else records.take(3)
            sourcePriceRecordAdapter.submitList(displayRecords, records.size)
            btnShowAllRecords.text = if (isShowingAllSourceRecords) "收起" else "展示全部 (${records.size})"
        }
    }
    
    /**
     * 更新价格折线图
     */
    private fun updateSourcePriceChart(priceTrackingCard: View, records: List<com.example.itemmanagement.data.entity.PriceRecord>) {
        val chartContainer = priceTrackingCard.findViewById<FrameLayout>(R.id.chartContainer) ?: return
        
        // 按日期排序
        val sortedRecords = records.sortedBy { it.recordDate }
        
        // 准备图表数据
        val priceData = sortedRecords.map { it.price as Any }.toTypedArray()
        val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
        val dateCategories = sortedRecords.map { dateFormat.format(it.recordDate) }.toTypedArray()
        
        // 配置图表模型
        val chartModel = AAChartModel()
            .chartType(AAChartType.Line)
            .animationType(AAChartAnimationType.EaseInCubic)
            .animationDuration(800)
            .backgroundColor("#FFFFFF")
            .dataLabelsEnabled(false)
            .legendEnabled(false)
            .categories(dateCategories)
            .yAxisTitle("")
            .series(
                arrayOf(
                    AASeriesElement()
                        .name("价格")
                        .data(priceData)
                        .color("#1976D2")
                        .lineWidth(3f)
                )
            )
        
        // 查找或创建图表视图
        var chartView = chartContainer.findViewWithTag<AAChartView>("chartView")
        if (chartView == null) {
            chartView = AAChartView(requireContext())
            chartView.tag = "chartView"
            chartContainer.removeAllViews()
            chartContainer.addView(
                chartView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
        
        // 绘制图表
        chartView.aa_drawChartWithChartModel(chartModel)
    }
    
    /**
     * 显示添加到购物清单的对话框
     */
    private fun showAddToShoppingListDialog() {
        viewModel.loadActiveShoppingLists { shoppingLists ->
            // 检查 Fragment 是否还在活动状态
            if (!isAdded || _binding == null) {
                return@loadActiveShoppingLists
            }
            
            if (shoppingLists.isEmpty()) {
                com.google.android.material.snackbar.Snackbar.make(
                    binding.root,
                    "暂无购物清单，请先创建购物清单",
                    com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                ).show()
                return@loadActiveShoppingLists
            }
            
            // 创建对话框视图
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_to_shopping_list, null)
            val radioGroup = dialogView.findViewById<android.widget.RadioGroup>(R.id.radioGroupShoppingLists)
            val etQuantity = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etQuantity)
            val etPurchaseReason = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPurchaseReason)
            
            // 动态添加购物清单选项
            shoppingLists.forEachIndexed { index, list ->
                val radioButton = android.widget.RadioButton(requireContext())
                radioButton.id = View.generateViewId()
                radioButton.text = list.name
                radioButton.tag = list.id
                if (index == 0) radioButton.isChecked = true
                radioGroup.addView(radioButton)
            }
            
            // 预填充数量（默认为1）
            etQuantity.setText("1")
            
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("加入购物清单")
                .setView(dialogView)
                .setPositiveButton("确定") { _, _ ->
                    val selectedRadioButtonId = radioGroup.checkedRadioButtonId
                    if (selectedRadioButtonId != -1) {
                        val selectedRadioButton = radioGroup.findViewById<android.widget.RadioButton>(selectedRadioButtonId)
                        val selectedListId = selectedRadioButton.tag as Long
                        val quantity = etQuantity.text.toString().toDoubleOrNull() ?: 1.0
                        val purchaseReason = etPurchaseReason.text.toString().trim()
                        
                        viewModel.addToShoppingList(
                            itemId = args.itemId,
                            shoppingListId = selectedListId,
                            quantity = quantity,
                            purchaseReason = purchaseReason
                        )
                    }
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    /**
     * 隐藏底部导航栏
     */
    private fun hideBottomNavigation() {
        android.util.Log.d("ItemDetailFragment", "🔽 hideBottomNavigation() 被调用")
        val navView = activity?.findViewById<View>(R.id.nav_view)
        
        if (navView == null) {
            android.util.Log.e("ItemDetailFragment", "   ❌ 错误：找不到 nav_view！")
            return
        }
        
        android.util.Log.d("ItemDetailFragment", "   ✓ 找到 nav_view，当前状态: ${visibilityToString(navView.visibility)}")
        android.util.Log.d("ItemDetailFragment", "   ⏩ 调用 post() 延迟隐藏")
        
        navView.post {
            android.util.Log.d("ItemDetailFragment", "   📌 post() 回调执行中...")
            android.util.Log.d("ItemDetailFragment", "      隐藏前状态: ${visibilityToString(navView.visibility)}")
            navView.visibility = View.GONE
            android.util.Log.d("ItemDetailFragment", "      ✅ 已设置为 GONE")
            android.util.Log.d("ItemDetailFragment", "      隐藏后状态: ${visibilityToString(navView.visibility)}")
        }
    }

    /**
     * 显示底部导航栏
     */
    private fun showBottomNavigation() {
        android.util.Log.d("ItemDetailFragment", "🔼 showBottomNavigation() 被调用")
        val navView = activity?.findViewById<View>(R.id.nav_view)
        
        if (navView == null) {
            android.util.Log.e("ItemDetailFragment", "   ❌ 错误：找不到 nav_view！")
            return
        }
        
        android.util.Log.d("ItemDetailFragment", "   ✓ 找到 nav_view，当前状态: ${visibilityToString(navView.visibility)}")
        android.util.Log.d("ItemDetailFragment", "   ⏩ 调用 post() 延迟显示")
        
        navView.post {
            android.util.Log.d("ItemDetailFragment", "   📌 post() 回调执行中...")
            android.util.Log.d("ItemDetailFragment", "      显示前状态: ${visibilityToString(navView.visibility)}")
            navView.visibility = View.VISIBLE
            android.util.Log.d("ItemDetailFragment", "      ✅ 已设置为 VISIBLE")
            android.util.Log.d("ItemDetailFragment", "      显示后状态: ${visibilityToString(navView.visibility)}")
        }
    }
    
    // ==================== 地图相关方法 ====================
    
    /**
     * 设置地图折叠/展开功能
     */
    private fun setupMapToggle(itemName: String, address: String, latitude: Double, longitude: Double) {
        var isMapExpanded = false
        
        // 初始状态：地图折叠
        hideMapCard()
        
        // 设置点击监听
        binding.gpsLocationHeader.setOnClickListener {
            isMapExpanded = !isMapExpanded
            
            if (isMapExpanded) {
                // 展开地图
                showMapCard(itemName, address, latitude, longitude)
                binding.gpsLocationExpandIcon.rotation = 180f
            } else {
                // 折叠地图
                hideMapCard()
                binding.gpsLocationExpandIcon.rotation = 0f
            }
        }
    }
    
    /**
     * 显示地图卡片
     */
    private fun showMapCard(itemName: String, address: String, latitude: Double, longitude: Double) {
        try {
            // 显示地图 Surface 容器
            binding.mapSurfaceContainer.visibility = View.VISIBLE
            
            // 动态加载地图嵌入式布局（无卡片版本）
            val mapCardContainer = binding.mapCardContainer
            mapCardContainer.removeAllViews()
            
            val mapCardView = layoutInflater.inflate(R.layout.card_location_map_embedded, mapCardContainer, false)
            mapCardContainer.addView(mapCardView)
            
            // 初始化地图
            val mapView = mapCardView.findViewById<com.amap.api.maps.MapView>(R.id.mapView)
            mapView.onCreate(null)
            
            val aMap = mapView.map
            
            // 设置地图为2D普通地图
            aMap.mapType = com.amap.api.maps.AMap.MAP_TYPE_NORMAL
            
            // 禁用所有交互（地图上方有遮罩层）
            aMap.uiSettings.apply {
                isZoomControlsEnabled = false
                isCompassEnabled = false
                isScaleControlsEnabled = false
                isScrollGesturesEnabled = false
                isZoomGesturesEnabled = false
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
            }
            
            // 设置为2D视角（俯视角度）
            aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.changeTilt(0f))
            
            // 添加标记
            val position = com.amap.api.maps.model.LatLng(latitude, longitude)
            aMap.addMarker(
                com.amap.api.maps.model.MarkerOptions()
                    .position(position)
                    .title(itemName)
                    .icon(com.amap.api.maps.model.BitmapDescriptorFactory.defaultMarker(
                        com.amap.api.maps.model.BitmapDescriptorFactory.HUE_RED
                    ))
            )
            
            // 移动相机到标记位置
            aMap.moveCamera(com.amap.api.maps.CameraUpdateFactory.newLatLngZoom(position, 15f))
            
            // 点击地图进入全屏大图
            mapCardView.findViewById<View>(R.id.mapOverlay).setOnClickListener {
                openFullScreenMap(itemName, address, latitude, longitude)
            }
            
            android.util.Log.d("ItemDetailFragment", "🗺️ 地图卡片显示成功")
        } catch (e: Exception) {
            android.util.Log.e("ItemDetailFragment", "显示地图卡片失败", e)
            hideMapCard()
        }
    }
    
    /**
     * 隐藏地图卡片
     */
    private fun hideMapCard() {
        binding.mapSurfaceContainer.visibility = View.GONE
        binding.mapCardContainer.removeAllViews()
    }
    
    /**
     * 打开全屏地图
     */
    private fun openFullScreenMap(itemName: String, address: String, latitude: Double, longitude: Double) {
        try {
            android.util.Log.d("ItemDetailFragment", "════════════════════════════════════════")
            android.util.Log.d("ItemDetailFragment", "🗺️ openFullScreenMap() 被调用")
            android.util.Log.d("ItemDetailFragment", "   参数: itemName=$itemName")
            android.util.Log.d("ItemDetailFragment", "   参数: latitude=$latitude")
            android.util.Log.d("ItemDetailFragment", "   参数: longitude=$longitude")
            android.util.Log.d("ItemDetailFragment", "   参数: address=$address")
            
            // 检查底部导航栏状态
            val navView = activity?.findViewById<View>(R.id.nav_view)
            android.util.Log.d("ItemDetailFragment", "   📊 导航前底部导航栏状态: ${if (navView != null) visibilityToString(navView.visibility) else "NULL"}")
            
            val action = ItemDetailFragmentDirections.actionNavigationItemDetailToMapViewer(
                latitude = latitude.toFloat(),
                longitude = longitude.toFloat(),
                itemName = itemName,
                address = address
            )
            
            android.util.Log.d("ItemDetailFragment", "   ⏩ 准备导航到 MapViewerFragment...")
            findNavController().navigate(action)
            android.util.Log.d("ItemDetailFragment", "   ✅ navigate() 调用完成")
            
            // 导航后立即检查状态
            navView?.let {
                android.util.Log.d("ItemDetailFragment", "   📊 导航后立即检查底部导航栏状态: ${visibilityToString(it.visibility)}")
                
                // 延迟100ms后再次检查
                it.postDelayed({
                    android.util.Log.d("ItemDetailFragment", "   🔍 [100ms后] 底部导航栏状态: ${visibilityToString(it.visibility)}")
                }, 100)
            }
            
            android.util.Log.d("ItemDetailFragment", "════════════════════════════════════════")
        } catch (e: Exception) {
            android.util.Log.e("ItemDetailFragment", "❌ 打开全屏地图失败", e)
        }
    }
    
    private fun visibilityToString(visibility: Int): String {
        return when (visibility) {
            View.VISIBLE -> "VISIBLE"
            View.INVISIBLE -> "INVISIBLE"
            View.GONE -> "GONE"
            else -> "UNKNOWN($visibility)"
        }
    }
    
    /**
     * 调用高德地图APP进行导航
     */
    private fun navigateToLocation(latitude: Double, longitude: Double, name: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                addCategory(android.content.Intent.CATEGORY_DEFAULT)
                data = android.net.Uri.parse("amapuri://route/plan/?dlat=$latitude&dlon=$longitude&dname=$name&dev=0&t=0")
            }
            
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                // 没有安装高德地图，使用浏览器打开网页版
                val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://uri.amap.com/navigation?to=$longitude,$latitude,$name&mode=car&policy=1&src=myapp&coordinate=gaode&callnative=0")
                }
                startActivity(webIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("ItemDetailFragment", "导航失败", e)
            android.widget.Toast.makeText(requireContext(), "导航失败", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}