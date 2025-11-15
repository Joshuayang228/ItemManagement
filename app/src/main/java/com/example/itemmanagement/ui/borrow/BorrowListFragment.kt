package com.example.itemmanagement.ui.borrow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
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
import com.example.itemmanagement.data.dao.BorrowWithItemInfo
import com.example.itemmanagement.data.entity.BorrowStatus
import com.example.itemmanagement.databinding.FragmentBorrowListBinding
import com.example.itemmanagement.utils.SnackbarHelper
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 借还列表Fragment
 * 显示所有借还记录，支持状态筛选和基本操作
 */
class BorrowListFragment : Fragment() {

    private var _binding: FragmentBorrowListBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: BorrowListViewModel by viewModels {
        BorrowListViewModelFactory(
            (requireActivity().application as ItemManagementApplication).borrowRepository,
            requireContext()
        )
    }
    
    private lateinit var borrowAdapter: BorrowAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBorrowListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        hideBottomNavigation()
        setupRecyclerView()
        setupStatusChips()
        setupButtons()
        observeViewModel()

        binding.root.post {
            val layoutParams = binding.fabAddBorrow.layoutParams
            if (layoutParams is androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams) {
                Log.d("FabDebugBorrow", "fabMarginBottom=${layoutParams.bottomMargin}")
            }
            Log.d("FabDebugBorrow", "recyclerPaddingBottom=${binding.rvBorrowList.paddingBottom}")
            val insets = ViewCompat.getRootWindowInsets(binding.root)?.getInsets(WindowInsetsCompat.Type.systemBars())
            Log.d("FabDebugBorrow", "systemBarsBottom=${insets?.bottom ?: -1}")
        }
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
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

    /**
     * 设置工具栏
     */
    // Toolbar功能移除，导航由MainActivity统一管理

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        borrowAdapter = BorrowAdapter(
            onItemClick = { borrow ->
                // 跳转到编辑页面
                findNavController().navigate(
                    R.id.action_borrow_list_to_add_borrow,
                    bundleOf("borrowId" to borrow.id)
                )
            },
            onMoreClick = { view, borrow ->
                showPopupMenu(view, borrow)
            }
        )
        
        binding.rvBorrowList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = borrowAdapter
        }
    }

    /**
     * 设置状态筛选 Chips
     */
    private fun setupStatusChips() {
        val statusOptions = listOf(
            "全部" to null,
            "已借出" to BorrowStatus.BORROWED,
            "已归还" to BorrowStatus.RETURNED,
            "已逾期" to BorrowStatus.OVERDUE
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
                        viewModel.clearFilter()
                        updateChipStates(null)
                    } else {
                        // 切换状态筛选
                        val currentStatuses = viewModel.selectedStatuses.value?.toMutableSet() ?: mutableSetOf()
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
    private fun updateChipStates(selectedStatuses: Set<BorrowStatus>?) {
        val statusOptions = listOf(
            null, // 全部
            BorrowStatus.BORROWED,
            BorrowStatus.RETURNED,
            BorrowStatus.OVERDUE
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
        // 添加借出记录
        binding.fabAddBorrow.setOnClickListener {
            findNavController().navigate(R.id.action_borrow_list_to_add_borrow)
        }
    }

    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        // 观察借还记录列表
        viewModel.borrowList.observe(viewLifecycleOwner) { borrows ->
            borrowAdapter.submitList(borrows)
            
            // 显示/隐藏空状态
            if (borrows.isEmpty()) {
                binding.rvBorrowList.visibility = View.GONE
                binding.layoutEmpty.visibility = View.VISIBLE
            } else {
                binding.rvBorrowList.visibility = View.VISIBLE
                binding.layoutEmpty.visibility = View.GONE
            }
        }
        
        // 观察统计信息
        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            with(binding) {
                tvTotalCount.text = stats.total.toString()
                tvBorrowedCount.text = stats.borrowed.toString()
                tvOverdueCount.text = stats.overdue.toString()
                tvReturnedCount.text = stats.returned.toString()
            }
        }
        
        // 观察筛选状态，更新 Chip 状态
        viewModel.selectedStatuses.observe(viewLifecycleOwner) { statuses ->
            updateChipStates(statuses)
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                SnackbarHelper.showError(requireView(), message)
                viewModel.clearErrorMessage()
            }
        }
        
        // 观察成功消息
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                SnackbarHelper.showSuccess(requireView(), message)
                viewModel.clearSuccessMessage()
            }
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 可以在这里显示/隐藏加载指示器
            // 暂时不实现，保持界面简洁
        }
    }

    /**
     * 显示归还确认对话框
     */
    private fun showReturnConfirmDialog(borrow: BorrowWithItemInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("确认归还")
            .setMessage("确认 ${borrow.borrowerName} 已归还 ${borrow.itemName} ？")
            .setPositiveButton("确认") { _, _ ->
                viewModel.returnItem(borrow.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示更多选项对话框
     */
    /**
     * 显示弹出菜单
     */
    private fun showPopupMenu(view: View, borrow: BorrowWithItemInfo) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_borrow_item, popup.menu)
        
        // 根据状态显示/隐藏"归还"选项
        val returnItem = popup.menu.findItem(R.id.action_return)
        returnItem.isVisible = borrow.status == BorrowStatus.BORROWED || borrow.status == BorrowStatus.OVERDUE
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_return -> {
                    showReturnConfirmDialog(borrow)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmDialog(borrow)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmDialog(borrow: BorrowWithItemInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除记录")
            .setMessage("确认删除 ${borrow.itemName} 的借还记录？\n此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteBorrow(borrow.id)
            }
            .setNegativeButton("取消", null)
            .show()
    }


    /**
     * 刷新数据
     */
    fun refreshData() {
        viewModel.refreshData()
    }
}
