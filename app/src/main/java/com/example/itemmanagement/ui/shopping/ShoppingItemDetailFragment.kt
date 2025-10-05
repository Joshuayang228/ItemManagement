package com.example.itemmanagement.ui.shopping

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentShoppingItemDetailBinding
import com.example.itemmanagement.data.entity.ShoppingItemPriority
import com.example.itemmanagement.data.entity.UrgencyLevel
import com.example.itemmanagement.ui.detail.adapter.PhotoAdapter
import com.example.itemmanagement.adapter.PriceRecordAdapter
import com.example.itemmanagement.adapter.ChannelPriceAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartModel
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartType
import com.github.aachartmodel.aainfographics.aachartcreator.AASeriesElement
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartAnimationType
import com.github.aachartmodel.aainfographics.aaoptionsmodel.AAStyle
import com.github.aachartmodel.aainfographics.aachartcreator.AAChartView
import android.widget.FrameLayout
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 购物物品详情Fragment
 * 显示购物物品的完整信息，支持标记已购买、删除、转入库存等操作
 */
class ShoppingItemDetailFragment : Fragment() {

    private var _binding: FragmentShoppingItemDetailBinding? = null
    private val binding get() = _binding!!

    private val args: ShoppingItemDetailFragmentArgs by navArgs()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var photoAdapter: PhotoAdapter

    private val viewModel: ShoppingItemDetailViewModel by viewModels {
        ShoppingItemDetailViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository,
            args.itemId,
            args.listId
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShoppingItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        android.util.Log.d("ShoppingDetail", "========== Fragment.onViewCreated ==========")
        android.util.Log.d("ShoppingDetail", "itemId from args: ${args.itemId}")
        android.util.Log.d("ShoppingDetail", "listId from args: ${args.listId}")
        android.util.Log.d("ShoppingDetail", "listName from args: ${args.listName}")

        hideBottomNavigation()
        setupPhotoViewPager()
        setupButtonListeners()
        setupPriceTracking()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        view?.post { hideBottomNavigation() }
    }

    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_item_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                navigateToEdit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupPhotoViewPager() {
        photoAdapter = PhotoAdapter()
        binding.photoViewPager.adapter = photoAdapter
    }

    private fun setupButtonListeners() {
        // 标记已购买/取消购买
        binding.buttonTogglePurchased.setOnClickListener {
            viewModel.togglePurchaseStatus()
        }

        // 转入库存
        binding.buttonTransferToInventory.setOnClickListener {
            showTransferConfirmDialog()
        }
    }

    private fun observeViewModel() {
        android.util.Log.d("ShoppingDetail", "开始监听ViewModel...")
        viewModel.itemDetail.observe(viewLifecycleOwner) { item ->
            android.util.Log.d("ShoppingDetail", "LiveData更新: item = ${item?.name ?: "null"}")
            if (item != null) {
                bindItemData(item)
            } else {
                android.util.Log.w("ShoppingDetail", "接收到空物品数据")
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                viewModel.onErrorHandled()
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                    .setAnchorView(binding.bottomActionBar)
                    .show()
                viewModel.onMessageShown()
            }
        }

        viewModel.navigateBack.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate == true) {
                findNavController().navigateUp()
                viewModel.onNavigationComplete()
            }
        }
    }

    private fun bindItemData(item: com.example.itemmanagement.data.model.Item) {
        val shoppingDetail = item.shoppingDetail ?: return

        android.util.Log.d("ShoppingDetail", "========== 绑定物品数据 ==========")
        android.util.Log.d("ShoppingDetail", "物品名称: ${item.name}")
        android.util.Log.d("ShoppingDetail", "预估价格: ${shoppingDetail.estimatedPrice}")
        android.util.Log.d("ShoppingDetail", "预算上限: ${shoppingDetail.budgetLimit}")
        android.util.Log.d("ShoppingDetail", "重要程度: ${shoppingDetail.priority}")
        android.util.Log.d("ShoppingDetail", "紧急程度: ${shoppingDetail.urgencyLevel}")
        android.util.Log.d("ShoppingDetail", "截止日期: ${shoppingDetail.deadline}")

        // 物品名称
        binding.nameTextView.text = item.name

        // 照片
        if (item.photos.isNotEmpty()) {
            val photoEntities = item.photos.map { photo ->
                com.example.itemmanagement.data.entity.PhotoEntity(
                    id = photo.id,
                    itemId = item.id,
                    uri = photo.uri,
                    isMain = photo.isMain,
                    displayOrder = photo.displayOrder
                )
            }
            photoAdapter.submitList(photoEntities)
            binding.noPhotoPlaceholder.visibility = View.GONE
            binding.photoViewPager.visibility = View.VISIBLE
        } else {
            binding.noPhotoPlaceholder.visibility = View.VISIBLE
            binding.photoViewPager.visibility = View.GONE
        }

        // 基本信息
        binding.quantityTextView.text = "${shoppingDetail.quantity} ${shoppingDetail.quantityUnit}"
        binding.categoryTextView.text = item.category ?: "未分类"

        // 价格信息
        binding.estimatedPriceTextView.text = if (shoppingDetail.estimatedPrice != null) {
            "¥${String.format("%.2f", shoppingDetail.estimatedPrice)}"
        } else {
            "未设置"
        }

        binding.budgetLimitTextView.text = if (shoppingDetail.budgetLimit != null) {
            "¥${String.format("%.2f", shoppingDetail.budgetLimit)}"
        } else {
            "未设置"
        }

        binding.actualPriceTextView.text = if (shoppingDetail.actualPrice != null) {
            "¥${String.format("%.2f", shoppingDetail.actualPrice)}"
        } else {
            if (shoppingDetail.isPurchased) "已购买（未记录价格）" else "未购买"
        }

        // 购物计划
        setupPriorityChip(shoppingDetail.priority)
        setupUrgencyChip(shoppingDetail.urgencyLevel)

        if (shoppingDetail.deadline != null) {
            binding.deadlineLayout.visibility = View.VISIBLE
            binding.deadlineTextView.text = dateFormat.format(shoppingDetail.deadline)
        } else {
            binding.deadlineLayout.visibility = View.GONE
        }

        // 提醒日期 - 暂时隐藏（字段未实现）
        binding.reminderLayout.visibility = View.GONE

        // 购买信息
        if (!shoppingDetail.storeName.isNullOrBlank()) {
            binding.storeLayout.visibility = View.VISIBLE
            binding.storeTextView.text = shoppingDetail.storeName
        } else {
            binding.storeLayout.visibility = View.GONE
        }

        // 推荐原因 - 暂时隐藏（字段未实现）
        binding.recommendLayout.visibility = View.GONE

        // 备注
        if (!item.customNote.isNullOrBlank()) {
            binding.noteCard.visibility = View.VISIBLE
            binding.noteTextView.text = item.customNote
        } else {
            binding.noteCard.visibility = View.GONE
        }

        // 标签
        setupTags(item.tags)

        // 底部按钮状态
        updatePurchaseButton(shoppingDetail.isPurchased)
    }

    private fun setupPriorityChip(priority: ShoppingItemPriority) {
        binding.priorityChip.text = priority.displayName
        
        val colorAttr = when (priority.level) {
            4 -> com.google.android.material.R.attr.colorError
            3 -> com.google.android.material.R.attr.colorPrimary
            2 -> com.google.android.material.R.attr.colorTertiary
            else -> com.google.android.material.R.attr.colorSecondary
        }
        
        val color = getColorFromAttr(colorAttr)
        binding.priorityChip.chipBackgroundColor = ColorStateList.valueOf(color)
        binding.priorityChip.setTextColor(
            ContextCompat.getColor(requireContext(), android.R.color.white)
        )
    }

    private fun setupUrgencyChip(urgency: UrgencyLevel) {
        binding.urgencyChip.text = urgency.displayName
        
        when (urgency) {
            UrgencyLevel.CRITICAL -> {
                val color = getColorFromAttr(com.google.android.material.R.attr.colorError)
                binding.urgencyChip.chipBackgroundColor = ColorStateList.valueOf(color)
                binding.urgencyChip.setTextColor(
                    ContextCompat.getColor(requireContext(), android.R.color.white)
                )
            }
            UrgencyLevel.URGENT -> {
                val color = getColorFromAttr(com.google.android.material.R.attr.colorErrorContainer)
                binding.urgencyChip.chipBackgroundColor = ColorStateList.valueOf(color)
                binding.urgencyChip.setTextColor(
                    getColorFromAttr(com.google.android.material.R.attr.colorOnErrorContainer)
                )
            }
            else -> {
                val color = getColorFromAttr(com.google.android.material.R.attr.colorTertiaryContainer)
                binding.urgencyChip.chipBackgroundColor = ColorStateList.valueOf(color)
                binding.urgencyChip.setTextColor(
                    getColorFromAttr(com.google.android.material.R.attr.colorOnTertiaryContainer)
                )
            }
        }
    }

    private fun setupTags(tags: List<com.example.itemmanagement.data.model.Tag>) {
        if (tags.isEmpty()) {
            binding.tagsCard.visibility = View.GONE
            return
        }

        binding.tagsCard.visibility = View.VISIBLE
        binding.tagsChipGroup.removeAllViews()

        tags.forEach { tag ->
            val chip = Chip(requireContext()).apply {
                text = tag.name
                isClickable = false
                isCheckable = false
                chipBackgroundColor = ColorStateList.valueOf(
                    getColorFromAttr(com.google.android.material.R.attr.colorSecondaryContainer)
                )
                setTextColor(
                    getColorFromAttr(com.google.android.material.R.attr.colorOnSecondaryContainer)
                )
            }
            binding.tagsChipGroup.addView(chip)
        }
    }

    private fun updatePurchaseButton(isPurchased: Boolean) {
        if (isPurchased) {
            binding.buttonTogglePurchased.text = "取消购买"
        } else {
            binding.buttonTogglePurchased.text = "标记已购买"
        }
    }

    private fun showDeleteConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除物品")
            .setMessage("确定要删除此物品吗？物品将移至回收站。")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteItem()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showTransferConfirmDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("转入库存")
            .setMessage("确定要将此物品转入库存吗？")
            .setPositiveButton("转入") { _, _ ->
                viewModel.transferToInventory()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun navigateToEdit() {
        try {
            val action = ShoppingItemDetailFragmentDirections
                .actionShoppingItemDetailToEditShoppingItem(
                    itemId = args.itemId,
                    listId = args.listId,
                    listName = args.listName ?: "购物清单"
                )
            findNavController().navigate(action)
        } catch (e: Exception) {
            Snackbar.make(binding.root, "打开编辑页面失败", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun getColorFromAttr(attr: Int): Int {
        val typedValue = android.util.TypedValue()
        requireContext().theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    /**
     * 设置价格跟踪卡片
     */
    private fun setupPriceTracking() {
        val priceTrackingCard = binding.root.findViewById<View>(R.id.priceTrackingCardInclude) ?: return
        
        // 初始化视图
        val recordCountText = priceTrackingCard.findViewById<android.widget.TextView>(R.id.recordCountText)
        val btnAddRecord = priceTrackingCard.findViewById<android.widget.ImageButton>(R.id.btnAddRecord)
        
        // 图表相关
        val emptyChartText = priceTrackingCard.findViewById<android.widget.TextView>(R.id.emptyChartText)
        val statsLayout = priceTrackingCard.findViewById<View>(R.id.statsLayout)
        val recentRecordsLayout = priceTrackingCard.findViewById<View>(R.id.recentRecordsLayout)
        
        // 价格统计
        val maxPriceText = priceTrackingCard.findViewById<android.widget.TextView>(R.id.maxPriceText)
        val avgPriceText = priceTrackingCard.findViewById<android.widget.TextView>(R.id.avgPriceText)
        val minPriceText = priceTrackingCard.findViewById<android.widget.TextView>(R.id.minPriceText)
        
        // RecyclerView
        val recentRecordsRecyclerView = priceTrackingCard.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recentRecordsRecyclerView)
        
        // 初始化适配器
        val priceRecordAdapter = PriceRecordAdapter(
            onDeleteClick = { record ->
                showDeletePriceRecordDialog(record)
            }
        )
        recentRecordsRecyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = priceRecordAdapter
        }
        
        // 添加记录按钮点击
        val onAddClick = {
            viewModel.itemDetail.value?.let { item ->
                showRecordPriceDialog(item)
            }
        }
        btnAddRecord?.setOnClickListener { onAddClick() }
        
        // 展示全部按钮
        val btnShowAllRecords = priceTrackingCard.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShowAllRecords)
        var isShowingAll = false
        
        // 观察价格记录（始终显示内容区域）
        viewModel.priceRecords.observe(viewLifecycleOwner) { records ->
            recordCountText?.text = "(${records.size}条)"
            
            if (records.isEmpty()) {
                // 空状态：显示提示文本，隐藏统计和列表
                emptyChartText?.visibility = View.VISIBLE
                statsLayout?.visibility = View.GONE
                recentRecordsLayout?.visibility = View.GONE
                btnShowAllRecords?.visibility = View.GONE
            } else {
                // 有数据：显示折线图、统计和列表
                emptyChartText?.visibility = View.GONE
                statsLayout?.visibility = View.VISIBLE
                recentRecordsLayout?.visibility = View.VISIBLE
                
                // 根据状态显示记录（传递总记录数用于序号计算）
                val displayRecords = if (isShowingAll) records else records.take(3)
                priceRecordAdapter.submitList(displayRecords, records.size)
                
                // 控制按钮显示
                if (records.size > 3) {
                    btnShowAllRecords?.visibility = View.VISIBLE
                    btnShowAllRecords?.text = if (isShowingAll) "收起" else "展示全部 (${records.size})"
                } else {
                    btnShowAllRecords?.visibility = View.GONE
                }
                
                // 更新折线图
                updatePriceChart(priceTrackingCard, records)
            }
        }
        
        // 展示全部/收起按钮点击事件
        btnShowAllRecords?.setOnClickListener {
            isShowingAll = !isShowingAll
            val records = viewModel.priceRecords.value ?: emptyList()
            val displayRecords = if (isShowingAll) records else records.take(3)
            priceRecordAdapter.submitList(displayRecords, records.size)
            btnShowAllRecords.text = if (isShowingAll) "收起" else "展示全部 (${records.size})"
        }
        
        // 观察价格统计
        viewModel.priceStatistics.observe(viewLifecycleOwner) { statistics ->
            if (statistics != null && statistics.recordCount > 0) {
                maxPriceText?.text = "¥${statistics.maxPrice.toInt()}"
                avgPriceText?.text = "¥${statistics.avgPrice.toInt()}"
                minPriceText?.text = "¥${statistics.minPrice.toInt()}"
            }
        }
    }
    
    /**
     * 显示记录价格对话框（完整版）
     */
    private fun showRecordPriceDialog(item: com.example.itemmanagement.data.model.Item) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_record_price, null)
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        
        // 初始化视图
        val itemNameText = dialogView.findViewById<android.widget.TextView>(R.id.itemNameText)
        val priceInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.priceInput)
        val priceUnitInput = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.priceUnitInput)
        val channelChipGroup = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.channelChipGroup)
        val channelInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.channelInput)
        val dateInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dateInput)
        val notesInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.notesInput)
        val btnSave = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSave)
        
        // 设置物品名称
        itemNameText.text = item.name
        
        // 预填充价格（如果有预估价格）
        item.shoppingDetail?.estimatedPrice?.let { price ->
            priceInput.setText(price.toInt().toString())
        }
        
        // 价格单位选项（与库存字段单价单位一致）
        val priceUnits = arrayOf("元", "美元", "日元", "欧元")
        val unitAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, priceUnits)
        priceUnitInput?.setAdapter(unitAdapter)
        priceUnitInput?.setText("元", false)
        
        // Chip点击事件：填充到自定义输入框
        val chipJD = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.channelJD)
        val chipTmall = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.channelTmall)
        val chipPDD = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.channelPDD)
        val chipStore = dialogView.findViewById<com.google.android.material.chip.Chip>(R.id.channelStore)
        
        chipJD?.setOnClickListener { channelInput?.setText("京东") }
        chipTmall?.setOnClickListener { channelInput?.setText("天猫") }
        chipPDD?.setOnClickListener { channelInput?.setText("拼多多") }
        chipStore?.setOnClickListener { channelInput?.setText("实体店") }
        
        // 日期选择器
        var selectedDate = java.util.Date()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateInput?.setText(dateFormatter.format(selectedDate))
        dateInput?.setOnClickListener {
            val datePicker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择日期")
                .setSelection(selectedDate.time)
                .build()
            
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = java.util.Date(selection)
                dateInput.setText(dateFormatter.format(selectedDate))
            }
            
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
        
        // 保存按钮点击事件
        btnSave.setOnClickListener {
            val priceText = priceInput?.text?.toString()
            if (priceText.isNullOrBlank()) {
                priceInput?.error = "请输入价格"
                return@setOnClickListener
            }
            
            val price = priceText.toDoubleOrNull()
            if (price == null || price <= 0) {
                priceInput?.error = "请输入有效的价格"
                return@setOnClickListener
            }
            
            // 获取渠道（优先使用自定义输入）
            val channel = channelInput?.text?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: "其他"
            
            // 获取备注
            val notes = notesInput?.text?.toString()?.trim()?.takeIf { it.isNotBlank() }
            
            // 创建价格记录
            val priceRecord = com.example.itemmanagement.data.entity.PriceRecord(
                itemId = item.id,
                recordDate = selectedDate,
                price = price,
                purchaseChannel = channel,
                notes = notes
            )
            
            // 通过 ViewModel 保存
            viewModel.addPriceRecord(priceRecord)
            dialog.dismiss()
            
            Snackbar.make(binding.root, "价格已记录", Snackbar.LENGTH_SHORT).show()
        }
        
        dialog.show()
    }
    
    /**
     * 更新价格折线图（简化版，不区分渠道）
     */
    private fun updatePriceChart(priceTrackingCard: View, records: List<com.example.itemmanagement.data.entity.PriceRecord>) {
        val chartContainer = priceTrackingCard.findViewById<FrameLayout>(R.id.chartContainer) ?: return
        
        // 按日期排序（从旧到新）
        val sortedRecords = records.sortedBy { it.recordDate }
        
        // 准备图表数据
        val priceData = sortedRecords.map { it.price }.toTypedArray()
        val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
        val dateCategories = sortedRecords.map { dateFormat.format(it.recordDate) }.toTypedArray()
        
        // 创建折线图配置（单条线，蓝色主题）
        val aaChartModel = AAChartModel()
            .chartType(AAChartType.Line)
            .title("")
            .titleStyle(AAStyle().color("transparent"))
            .subtitle("")
            .backgroundColor("#F5F5F5")
            .dataLabelsEnabled(false)
            .categories(dateCategories)
            .yAxisTitle("价格 (¥)")
            .markerRadius(5)
            .legendEnabled(false)  // 不显示图例（因为只有一条线）
            .touchEventEnabled(true)
            .animationType(AAChartAnimationType.EaseOutQuart)
            .animationDuration(800)
            .series(arrayOf(
                AASeriesElement()
                    .name("价格")
                    .data(priceData as Array<Any>)
                    .color("#1976D2")  // Material Blue 600
                    .lineWidth(3)
            ))
        
        // 创建或更新 AAChartView
        var aaChartView = chartContainer.findViewWithTag<AAChartView>("priceChartView")
        if (aaChartView == null) {
            aaChartView = AAChartView(requireContext())
            aaChartView.tag = "priceChartView"
            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            chartContainer.removeAllViews()
            chartContainer.addView(aaChartView, layoutParams)
        }
        
        aaChartView.aa_drawChartWithChartModel(aaChartModel)
    }
    
    /**
     * 显示删除价格记录确认对话框
     */
    private fun showDeletePriceRecordDialog(record: com.example.itemmanagement.data.entity.PriceRecord) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除价格记录")
            .setMessage("确定要删除这条价格记录吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deletePriceRecord(record)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }
}

