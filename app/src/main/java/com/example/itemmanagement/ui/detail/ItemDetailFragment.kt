package com.example.itemmanagement.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentItemDetailBinding
import com.example.itemmanagement.ui.detail.adapter.PhotoAdapter
import com.example.itemmanagement.ui.detail.adapter.TagAdapter
import com.example.itemmanagement.data.model.OpenStatus
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.widget.LinearLayout

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
    private lateinit var tagAdapter: TagAdapter

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
        setupViewPager()
        setupTagsRecyclerView()
        setupButtons()
        viewModel.loadItem(args.itemId)
        observeItem()
        observeError()
    }

    private fun setupViewPager() {
        // 设置照片ViewPager
        photoAdapter = PhotoAdapter()
        binding.photoViewPager.adapter = photoAdapter
    }
    
    private fun setupTagsRecyclerView() {
        // 设置标签RecyclerView
        tagAdapter = TagAdapter()
        binding.tagsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = tagAdapter
        }
    }
    
    private fun setupButtons() {
        // 设置编辑按钮点击事件
        binding.editButton.setOnClickListener {
            navigateToEditItem()
        }
        
        // 设置底部操作按钮
        binding.modifyQuantityButton.setOnClickListener {
            // 实现修改数量的逻辑
            Toast.makeText(context, "修改数量功能将在后续版本实现", Toast.LENGTH_SHORT).show()
        }
        
        binding.markOpenedButton.setOnClickListener {
            // 实现标记开封的逻辑
            Toast.makeText(context, "标记开封功能将在后续版本实现", Toast.LENGTH_SHORT).show()
        }
        
        binding.consumeButton.setOnClickListener {
            // 实现消耗的逻辑
            Toast.makeText(context, "消耗功能将在后续版本实现", Toast.LENGTH_SHORT).show()
        }
        
        // 备注展开/收起按钮
        binding.expandButton.setOnClickListener {
            if (binding.customNoteTextView.maxLines == 5) {
                binding.customNoteTextView.maxLines = Integer.MAX_VALUE
                binding.expandButton.text = "收起"
            } else {
                binding.customNoteTextView.maxLines = 5
                binding.expandButton.text = "展开"
            }
        }
    }
    
    private fun navigateToEditItem() {
        // 导航到编辑物品页面
        val action = ItemDetailFragmentDirections.actionNavigationItemDetailToAddItemFragment(
            mode = "edit",
            itemId = args.itemId
        )
        findNavController().navigate(action)
    }

    private fun observeItem() {
        viewModel.item.observe(viewLifecycleOwner) { item ->
            binding.apply {
                // 基本信息
                nameTextView.text = item.name
                quantityTextView.text = "${formatNumber(item.quantity)} ${item.unit ?: "个"}"
                
                // 评分
                val ratingContainer = binding.root.findViewById<View>(R.id.ratingContainer)
                item.rating?.let {
                    binding.ratingBar.rating = it.toFloat()
                    ratingContainer?.visibility = View.VISIBLE
                } ?: run {
                    ratingContainer?.visibility = View.GONE
                }
                
                // 分类和子分类
                // 找到分类容器
                val categoryContainer = binding.root.findViewById<LinearLayout>(R.id.categoryContainer)
                
                if (item.category.isNullOrBlank() || item.category == "未指定") {
                    // 如果分类为空或未指定，隐藏整个分类容器
                    categoryContainer?.visibility = View.GONE
                } else {
                    setCategoryTag(categoryTextView, item.category, getCategoryColor(item.category))
                    categoryTextView.visibility = View.VISIBLE
                    
                    // 子分类处理
                    item.subCategory?.let {
                        if (it.isNotBlank()) {
                            setCategoryTag(subCategoryTextView, it, getCategoryColor(item.category, true))
                            subCategoryTextView.visibility = View.VISIBLE
                        } else {
                            subCategoryTextView.visibility = View.GONE
                        }
                    } ?: run {
                        subCategoryTextView.visibility = View.GONE
                    }
                    
                    // 显示分类容器
                    categoryContainer?.visibility = View.VISIBLE
                }
                
                // 位置信息
                item.location?.let {
                    if (it.area.isBlank() || it.area == "未指定") {
                        binding.locationContainer.visibility = View.GONE
                    } else {
                        locationTextView.text = it.getFullLocationString()
                        binding.locationContainer.visibility = View.VISIBLE
                    }
                } ?: run {
                    binding.locationContainer.visibility = View.GONE
                }

                // 单价
                item.price?.let {
                    val priceUnitText = item.priceUnit ?: "元"
                    priceTextView.text = "${formatNumber(it)} ${priceUnitText}"
                    priceContainer.visibility = View.VISIBLE
                } ?: run {
                    priceContainer.visibility = View.GONE
                }
                
                // 总价
                item.totalPrice?.let {
                    val totalPriceUnitText = item.totalPriceUnit ?: "元"
                    totalPriceTextView.text = "${formatNumber(it)} ${totalPriceUnitText}"
                    totalPriceContainer.visibility = View.VISIBLE
                } ?: run {
                    totalPriceContainer.visibility = View.GONE
                }
                
                // 添加日期
                addDateTextView.text = dateFormat.format(item.addDate)
                
                // 保修期
                item.warrantyPeriod?.let { period ->
                    val unitText = when {
                        period % 365 == 0 -> "${period / 365}年"
                        period % 30 == 0 -> "${period / 30}个月"
                        else -> "${period}天"
                    }
                    warrantyTextView.text = unitText
                    warrantyContainer.visibility = View.VISIBLE
                } ?: run {
                    warrantyContainer.visibility = View.GONE
                }
                
                // 过期日期
                item.expirationDate?.let {
                    expirationDateTextView.text = dateFormat.format(it)
                    expirationDateContainer.visibility = View.VISIBLE
                    
                    // 设置状态标签
                    setupExpirationStatus(it)
                } ?: run {
                    expirationDateContainer.visibility = View.GONE
                    statusTagView.visibility = View.GONE
                }

                // 保质期
                item.shelfLife?.let { period ->
                    val unitText = when {
                        period % 365 == 0 -> "${period / 365}年"
                        period % 30 == 0 -> "${period / 30}个月"
                        else -> "${period}天"
                    }
                    shelfLifeTextView.text = unitText
                    shelfLifeContainer.visibility = View.VISIBLE
                } ?: run {
                    shelfLifeContainer.visibility = View.GONE
                }

                // 生产日期
                item.productionDate?.let {
                    productionDateTextView.text = dateFormat.format(it)
                    productionDateContainer.visibility = View.VISIBLE
                } ?: run {
                    productionDateContainer.visibility = View.GONE
                }

                // 购买日期
                item.purchaseDate?.let {
                    purchaseDateTextView.text = dateFormat.format(it)
                    purchaseDateContainer.visibility = View.VISIBLE
                } ?: run {
                    purchaseDateContainer.visibility = View.GONE
                }

                // 开封状态
                item.openStatus?.let {
                    openStatusTextView.text = if (it == OpenStatus.OPENED) "已开封" else "未开封"
                    openStatusContainer.visibility = View.VISIBLE
                } ?: run {
                    openStatusContainer.visibility = View.GONE
                }
                
                // 开封日期
                item.openDate?.let {
                    openDateTextView.text = dateFormat.format(it)
                    openDateContainer.visibility = View.VISIBLE
                } ?: run {
                    openDateContainer.visibility = View.GONE
                }
                
                // 标签
                if (item.tags.isNotEmpty()) {
                    tagAdapter.submitList(item.tags.map { com.example.itemmanagement.data.entity.TagEntity(name = it.name) })
                    tagsRecyclerView.visibility = View.VISIBLE
                    binding.tagsContainer.visibility = View.VISIBLE
                } else {
                    // 如果没有标签，隐藏标签列表和标签容器
                    tagsRecyclerView.visibility = View.GONE
                    binding.tagsContainer.visibility = View.GONE
                }
                
                // 季节
                item.season?.let {
                    seasonTextView.text = it
                    seasonContainer.visibility = View.VISIBLE
                } ?: run {
                    seasonContainer.visibility = View.GONE
                }
                
                // 容量
                item.capacity?.let {
                    val unitText = item.capacityUnit ?: "ml"
                    capacityTextView.text = "${formatNumber(it)} $unitText"
                    capacityContainer.visibility = View.VISIBLE
                } ?: run {
                    capacityContainer.visibility = View.GONE
                }
                
                // 序列号
                item.serialNumber?.let {
                    serialNumberTextView.text = it
                    serialNumberContainer.visibility = View.VISIBLE
                } ?: run {
                    serialNumberContainer.visibility = View.GONE
                }
                
                // 品牌
                item.brand?.let {
                    brandTextView.text = it
                    brandContainer.visibility = View.VISIBLE
                } ?: run {
                    brandContainer.visibility = View.GONE
                }
                
                item.purchaseChannel?.let {
                    purchaseChannelTextView.text = it
                    purchaseChannelContainer.visibility = View.VISIBLE
                } ?: run {
                    purchaseChannelContainer.visibility = View.GONE
                }
                
                item.storeName?.let {
                    storeNameTextView.text = it
                    storeNameContainer.visibility = View.VISIBLE
                } ?: run {
                    storeNameContainer.visibility = View.GONE
                }
                
                // 保修信息
                item.warrantyEndDate?.let { endDate ->
                    warrantyEndDateTextView.text = dateFormat.format(endDate)
                    warrantyEndContainer.visibility = View.VISIBLE
                    
                    // 计算保修进度条
                    setupWarrantyProgress(item.purchaseDate ?: item.addDate, endDate)
                } ?: run {
                    warrantyEndContainer.visibility = View.GONE
                    warrantyProgressBar.visibility = View.GONE
                }

                // 规格与备注
                // 规格
                item.specification?.let {
                    specificationTextView.text = it
                    specificationContainer.visibility = View.VISIBLE
                } ?: run {
                    specificationContainer.visibility = View.GONE
                }
                
                // 备注
                item.customNote?.let {
                    customNoteTextView.text = it
                    // 如果备注较长，显示展开按钮
                    if (it.length > 200) {
                        customNoteTextView.maxLines = 5
                        expandButton.visibility = View.VISIBLE
                    } else {
                        customNoteTextView.maxLines = Integer.MAX_VALUE
                        expandButton.visibility = View.GONE
                    }
                    customNoteTextView.visibility = View.VISIBLE
                } ?: run {
                    customNoteTextView.visibility = View.GONE
                    expandButton.visibility = View.GONE
                }

                // 照片
                if (item.photos.isNotEmpty()) {
                    photoAdapter.submitList(item.photos.map { com.example.itemmanagement.data.entity.PhotoEntity(uri = it.uri, itemId = item.id) })
                    photoContainer.visibility = View.VISIBLE
                    
                    // 设置照片指示器
                    setupPhotoIndicators(item.photos.size)
                } else {
                    photoContainer.visibility = View.GONE
                }
                
                // 根据是否已开封，设置"标记开封"按钮状态
                markOpenedButton.isEnabled = item.openStatus != OpenStatus.OPENED
                markOpenedButton.text = if (item.openStatus == OpenStatus.OPENED) "已开封" else "标记开封"
                
                // 根据内容决定卡片的可见性
                updateCardVisibility()
            }
        }
    }
    
    private fun updateCardVisibility() {
        // 关键信息卡片：如果有数量、位置、评分、分类、标签或开封状态，则显示
        val hasKeyInfo = binding.quantityTextView.visibility == View.VISIBLE ||
                binding.locationContainer.visibility == View.VISIBLE ||
                binding.ratingContainer.visibility == View.VISIBLE ||
                binding.categoryTextView.visibility == View.VISIBLE ||
                binding.openStatusContainer.visibility == View.VISIBLE ||
                binding.shelfLifeContainer.visibility == View.VISIBLE ||
                binding.tagsRecyclerView.adapter?.itemCount ?: 0 > 0
        
        binding.keyInfoCard.visibility = if (hasKeyInfo) View.VISIBLE else View.GONE
        
        // 物品状态卡片：如果有保质期、保修期、季节、开封状态、评分或标签，则显示
        val hasStatusInfo = binding.shelfLifeContainer.visibility == View.VISIBLE ||
                binding.warrantyContainer.visibility == View.VISIBLE ||
                binding.seasonContainer.visibility == View.VISIBLE ||
                binding.openStatusContainer.visibility == View.VISIBLE ||
                binding.ratingContainer.visibility == View.VISIBLE ||
                binding.tagsContainer.visibility == View.VISIBLE
        
        binding.keyInfoCard2.visibility = if (hasStatusInfo) View.VISIBLE else View.GONE
        
        // 状态与日期卡片：如果有任何日期信息，则显示
        val hasDateInfo = binding.expirationDateContainer.visibility == View.VISIBLE ||
                binding.productionDateContainer.visibility == View.VISIBLE ||
                binding.purchaseDateContainer.visibility == View.VISIBLE ||
                binding.openDateContainer.visibility == View.VISIBLE ||
                binding.warrantyEndContainer.visibility == View.VISIBLE
        
        binding.dateCard.visibility = if (hasDateInfo) View.VISIBLE else View.GONE
        
        // 商业信息卡片：如果有品牌、价格、购买渠道等信息，则显示
        val hasCommercialInfo = binding.brandContainer.visibility == View.VISIBLE ||
                binding.priceContainer.visibility == View.VISIBLE ||
                binding.totalPriceContainer.visibility == View.VISIBLE ||
                binding.purchaseChannelContainer.visibility == View.VISIBLE ||
                binding.storeNameContainer.visibility == View.VISIBLE ||
                binding.serialNumberContainer.visibility == View.VISIBLE ||
                binding.warrantyContainer.visibility == View.VISIBLE
        
        binding.commercialInfoCard.visibility = if (hasCommercialInfo) View.VISIBLE else View.GONE
        
        // 规格与备注卡片：如果有规格、容量、季节或备注信息，则显示
        val hasSpecsOrNotes = binding.specificationContainer.visibility == View.VISIBLE ||
                binding.capacityContainer.visibility == View.VISIBLE ||
                binding.seasonContainer.visibility == View.VISIBLE ||
                binding.customNoteTextView.visibility == View.VISIBLE
        
        binding.specsAndNotesCard.visibility = if (hasSpecsOrNotes) View.VISIBLE else View.GONE
    }
    
    private fun setupExpirationStatus(expirationDate: Date) {
        val today = Calendar.getInstance().time
        val diffInDays = TimeUnit.DAYS.convert(
            expirationDate.time - today.time,
            TimeUnit.MILLISECONDS
        )
        
        // 设置状态标签
        binding.statusTagView.apply {
            when {
                diffInDays < 0 -> {
                    text = "已过期"
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_error)
                    visibility = View.VISIBLE
                }
                diffInDays < 7 -> {
                    text = "即将过期"
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_error)
                    visibility = View.VISIBLE
                }
                diffInDays < 30 -> {
                    text = "临期"
                    background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_status_warning)
                    visibility = View.VISIBLE
                }
                else -> {
                    visibility = View.GONE
                }
            }
        }
    }
    
    private fun setupWarrantyProgress(startDate: Date, endDate: Date) {
        val today = Calendar.getInstance().time
        val totalDays = TimeUnit.DAYS.convert(
            endDate.time - startDate.time,
            TimeUnit.MILLISECONDS
        )
        val passedDays = TimeUnit.DAYS.convert(
            today.time - startDate.time,
            TimeUnit.MILLISECONDS
        )
        
        binding.warrantyProgressBar.apply {
            if (totalDays > 0) {
                progress = ((passedDays.toFloat() / totalDays) * 100).toInt().coerceIn(0, 100)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }
    
    private fun setupPhotoIndicators(count: Int) {
        binding.photoIndicator.removeAllViews()
        
        if (count <= 1) {
            binding.photoIndicator.visibility = View.GONE
            return
        }
        
        binding.photoIndicator.visibility = View.VISIBLE
        
        for (i in 0 until count) {
            val dot = TextView(requireContext())
            dot.layoutParams = ViewGroup.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.detail_indicator_diameter),
                resources.getDimensionPixelSize(R.dimen.detail_indicator_diameter)
            )
            dot.background = ContextCompat.getDrawable(requireContext(), R.drawable.dot_indicator)
            dot.isSelected = i == 0
            
            val layoutParams = ViewGroup.MarginLayoutParams(
                resources.getDimensionPixelSize(R.dimen.detail_indicator_diameter),
                resources.getDimensionPixelSize(R.dimen.detail_indicator_diameter)
            )
            layoutParams.setMargins(
                resources.getDimensionPixelSize(R.dimen.detail_indicator_spacing),
                0,
                resources.getDimensionPixelSize(R.dimen.detail_indicator_spacing),
                0
            )
            dot.layoutParams = layoutParams
            
            binding.photoIndicator.addView(dot)
        }
        
        // 设置ViewPager的页面变化监听器
        binding.photoViewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                // 更新指示器状态
                for (i in 0 until binding.photoIndicator.childCount) {
                    binding.photoIndicator.getChildAt(i).isSelected = i == position
                }
            }
        })
    }
    
    private fun setCategoryTag(textView: TextView, text: String, color: Int) {
        textView.text = text
        textView.background.setTint(color)
    }
    
    private fun getCategoryColor(category: String, isSubCategory: Boolean = false): Int {
        val baseColorId = when (category.lowercase()) {
            "食品" -> R.color.category_food
            "药品" -> R.color.category_medicine
            "电子产品" -> R.color.category_electronics
            "衣物" -> R.color.category_clothing
            else -> R.color.category_other
        }
        
        return if (isSubCategory) {
            // 子分类颜色略深一点
            ContextCompat.getColor(requireContext(), baseColorId)
        } else {
            ContextCompat.getColor(requireContext(), baseColorId)
        }
    }
    
    private fun getStatusText(status: String): String {
        return when (status) {
            "IN_STOCK" -> "在库"
            "OUT_OF_STOCK" -> "缺货"
            "EXPIRED" -> "过期"
            "CONSUMED" -> "已消耗"
            "LENT" -> "已借出"
            "LOST" -> "丢失"
            "DAMAGED" -> "损坏"
            else -> status
        }
    }

    private fun observeError() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 格式化数字，如果是整数则不显示小数点，如果有小数则保留小数
     */
    private fun formatNumber(number: Double): String {
        return if (number == number.toInt().toDouble()) {
            number.toInt().toString()
        } else {
            number.toString()
        }
    }
} 