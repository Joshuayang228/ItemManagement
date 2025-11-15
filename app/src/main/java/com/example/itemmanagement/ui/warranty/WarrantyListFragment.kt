package com.example.itemmanagement.ui.warranty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.data.dao.WarrantyWithItemInfo
import com.example.itemmanagement.data.entity.WarrantyStatus
import com.example.itemmanagement.databinding.FragmentWarrantyListBinding
import com.example.itemmanagement.utils.SnackbarHelper
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 保修列表Fragment
 * 显示所有保修记录，支持状态筛选和基本操作
 */
class WarrantyListFragment : Fragment() {

    private var _binding: FragmentWarrantyListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: WarrantyListViewModel by viewModels {
        WarrantyListViewModelFactory(
            (requireActivity().application as ItemManagementApplication).warrantyRepository,
            requireContext()
        )
    }
    
    private lateinit var warrantyAdapter: WarrantyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarrantyListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        hideBottomNavigation()
        setupRecyclerView()
        setupStatusChips()
        setupButtons()
        observeData()
        
        // 初始加载数据
        viewModel.loadWarrantyOverview()

        binding.root.post {
            val layoutParams = binding.addWarrantyFab.layoutParams
            if (layoutParams is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                Log.d("FabDebugWarranty", "fabMarginBottom=${layoutParams.bottomMargin}")
            }
            Log.d("FabDebugWarranty", "recyclerPaddingBottom=${binding.warrantyRecyclerView.paddingBottom}")
            val insets = ViewCompat.getRootWindowInsets(binding.root)?.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.d("FabDebugWarranty", "systemBarsBottom=${insets?.bottom ?: -1}")
        }
    }

    /**
     * 设置RecyclerView和适配器
     */
    private fun setupRecyclerView() {
        warrantyAdapter = WarrantyAdapter(
            onItemClick = { warranty ->
                // 跳转到编辑页面
                findNavController().navigate(
                    R.id.action_warranty_list_to_add_edit_warranty,
                    bundleOf("warrantyId" to warranty.id)
                )
            },
            onDeleteClick = { warranty ->
                showDeleteConfirmDialog(warranty)
            }
        )
        
        binding.warrantyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = warrantyAdapter
        }
    }

    /**
     * 设置状态筛选 Chips
     */
    private fun setupStatusChips() {
        val statusOptions = listOf(
            "全部" to null,
            "保修中" to WarrantyStatus.ACTIVE,
            "已过期" to WarrantyStatus.EXPIRED
        )
        
        statusOptions.forEach { (label, status) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = status == null // "全部" 默认选中
                
                // 使用 Material 3 颜色状态
                val colorStateList = android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),  // 选中状态
                        intArrayOf(-android.R.attr.state_checked)  // 未选中状态
                    ),
                    intArrayOf(
                        androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            com.google.android.material.R.color.material_dynamic_secondary90
                        ),  // 选中背景色（M3 浅色）
                        android.graphics.Color.TRANSPARENT  // 未选中透明
                    )
                )
                chipBackgroundColor = colorStateList
                
                // 描边样式
                val strokeColorStateList = android.content.res.ColorStateList(
                    arrayOf(
                        intArrayOf(android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_checked)
                    ),
                    intArrayOf(
                        android.graphics.Color.TRANSPARENT,  // 选中时无描边
                        androidx.core.content.ContextCompat.getColor(
                            requireContext(),
                            com.google.android.material.R.color.material_dynamic_neutral_variant50
                        )  // 未选中时描边
                    )
                )
                chipStrokeColor = strokeColorStateList
                chipStrokeWidth = 2f
                
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                
                setOnClickListener {
                    if (status == null) {
                        // 点击"全部"，清除所有筛选
                        viewModel.clearAllFilters()
                        updateChipStates(null)
                    } else {
                        // 切换状态筛选
                        val currentStatuses = viewModel.filterStatuses.value?.toMutableSet() ?: mutableSetOf()
                        if (isChecked) {
                            currentStatuses.add(status)
                        } else {
                            currentStatuses.remove(status)
                        }
                        viewModel.setStatusFilters(currentStatuses)
                        updateChipStates(currentStatuses)
                    }
                }
            }
            binding.statusChipGroup.addView(chip)
        }
    }
    
    /**
     * 更新 Chip 选中状态
     */
    private fun updateChipStates(selectedStatuses: Set<WarrantyStatus>?) {
        val statusOptions = listOf(
            null, // 全部
            WarrantyStatus.ACTIVE,
            WarrantyStatus.EXPIRED
        )
        
        for (i in 0 until binding.statusChipGroup.childCount) {
            val chip = binding.statusChipGroup.getChildAt(i) as? Chip ?: continue
            val status = statusOptions.getOrNull(i)
            
            chip.isChecked = if (selectedStatuses == null || selectedStatuses.isEmpty()) {
                status == null // 没有筛选时，"全部" 选中
            } else {
                status != null && status in selectedStatuses
            }
        }
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setupButtons() {
        // 添加保修按钮
        binding.addWarrantyFab.setOnClickListener {
            findNavController().navigate(R.id.action_warranty_list_to_add_edit_warranty)
        }
    }

    /**
     * 观察数据变化
     */
    private fun observeData() {
        // 观察筛选后的保修列表
        viewModel.filteredWarranties.observe(viewLifecycleOwner) { warranties ->
            warrantyAdapter.submitList(warranties)
            updateEmptyState(warranties.isEmpty())
        }
        
        // 观察保修概览数据
        viewModel.warrantyOverview.observe(viewLifecycleOwner) { overview ->
            binding.totalWarrantyCount.text = overview.total.toString()
            binding.activeWarrantyCount.text = overview.active.toString()
            binding.nearExpirationCount.text = overview.nearExpiration.toString()
            binding.expiredWarrantyCount.text = overview.expired.toString()
        }
        
        // 观察加载状态
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                SnackbarHelper.showError(requireView(), it)
                viewModel.clearErrorMessage()
            }
        }
        
        // 观察删除结果
        viewModel.deleteResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                SnackbarHelper.showSuccess(requireView(), "删除成功")
            } else {
                SnackbarHelper.showError(requireView(), "删除失败")
            }
        }
        
        // 观察当前筛选状态，更新 Chip 状态
        viewModel.filterStatuses.observe(viewLifecycleOwner) { statuses ->
            updateChipStates(statuses)
        }
    }

    /**
     * 更新空状态显示
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.warrantyRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    /**
     * 显示更多选项对话框
     */
    private fun showMoreOptionsDialog(warranty: WarrantyWithItemInfo) {
        val options = arrayOf("编辑", "删除")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(warranty.itemName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // 编辑
                        findNavController().navigate(
                            R.id.action_warranty_list_to_add_edit_warranty,
                            bundleOf("warrantyId" to warranty.id)
                        )
                    }
                    1 -> {
                        // 删除
                        showDeleteConfirmDialog(warranty)
                    }
                }
            }
            .show()
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(warranty: WarrantyWithItemInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除保修记录")
            .setMessage("确定要删除「${warranty.itemName}」的保修记录吗？此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                val warrantyEntity = com.example.itemmanagement.data.entity.WarrantyEntity(
                    id = warranty.id,
                    itemId = warranty.itemId,
                    purchaseDate = java.util.Date(warranty.purchaseDate),
                    warrantyPeriodMonths = warranty.warrantyPeriodMonths,
                    warrantyEndDate = java.util.Date(warranty.warrantyEndDate),
                    receiptImageUris = warranty.receiptImageUris,
                    notes = warranty.notes,
                    status = warranty.status,
                    warrantyProvider = warranty.warrantyProvider,
                    contactInfo = warranty.contactInfo,
                    createdDate = java.util.Date(warranty.createdDate),
                    updatedDate = java.util.Date(warranty.updatedDate)
                )
                viewModel.deleteWarranty(warrantyEntity)
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
        // 页面重新显示时刷新数据
        viewModel.refreshData()
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
