package com.example.itemmanagement.ui.warranty

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.data.dao.WarrantyWithItemInfo
import com.example.itemmanagement.data.entity.WarrantyStatus
import com.example.itemmanagement.databinding.FragmentWarrantyListBinding
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
        
        setupRecyclerView()
        setupButtons()
        observeData()
        
        // 初始加载数据
        viewModel.loadWarrantyOverview()
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
            onMoreClick = { warranty ->
                showMoreOptionsDialog(warranty)
            }
        )
        
        binding.warrantyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = warrantyAdapter
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
        
        // 筛选按钮
        binding.filterButton.setOnClickListener {
            showFilterDialog()
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
        viewModel.warrantyOverview.observe(viewLifecycleOwner) { (total, active, nearExpiration) ->
            binding.totalWarrantyCount.text = total.toString()
            binding.activeWarrantyCount.text = active.toString()
            binding.nearExpirationCount.text = nearExpiration.toString()
        }
        
        // 观察加载状态
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
        
        // 观察删除结果
        viewModel.deleteResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 观察当前筛选状态
        viewModel.filterStatus.observe(viewLifecycleOwner) { status ->
            updateFilterButtonText(status)
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
     * 更新筛选按钮文本
     */
    private fun updateFilterButtonText(status: WarrantyStatus?) {
        val buttonText = when (status) {
            null -> "筛选状态"
            WarrantyStatus.ACTIVE -> "保修中"
            WarrantyStatus.EXPIRED -> "已过期"
            WarrantyStatus.CLAIMED -> "已报修"
            WarrantyStatus.VOID -> "已作废"
        }
        binding.filterButton.text = buttonText
    }

    /**
     * 显示筛选对话框
     */
    private fun showFilterDialog() {
        val statusItems = arrayOf("全部", "保修中", "已过期", "已报修", "已作废")
        val statusValues = arrayOf(null, WarrantyStatus.ACTIVE, WarrantyStatus.EXPIRED, WarrantyStatus.CLAIMED, WarrantyStatus.VOID)
        
        val currentStatus = viewModel.filterStatus.value
        val currentIndex = statusValues.indexOf(currentStatus).takeIf { it >= 0 } ?: 0
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("筛选保修状态")
            .setSingleChoiceItems(statusItems, currentIndex) { dialog, which ->
                viewModel.setStatusFilter(statusValues[which])
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("清除筛选") { dialog, _ ->
                viewModel.clearAllFilters()
                dialog.dismiss()
            }
            .show()
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
        // 页面重新显示时刷新数据
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
