package com.example.itemmanagement.ui.borrow

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
import com.example.itemmanagement.data.dao.BorrowWithItemInfo
import com.example.itemmanagement.data.entity.BorrowStatus
import com.example.itemmanagement.databinding.FragmentBorrowListBinding
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
        
        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                // 点击查看详情（暂时显示Toast）
                Toast.makeText(requireContext(), "查看 ${borrow.itemName} 的借还详情", Toast.LENGTH_SHORT).show()
            },
            onReturnClick = { borrow ->
                showReturnConfirmDialog(borrow)
            },
            onMoreClick = { borrow ->
                showMoreOptionsDialog(borrow)
            }
        )
        
        binding.rvBorrowList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = borrowAdapter
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
        
        // 筛选按钮
        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }
        
        // 测试提醒按钮
        binding.btnTestReminder.setOnClickListener {
            viewModel.testBorrowReminders()
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
        
        // 观察筛选状态
        viewModel.selectedStatus.observe(viewLifecycleOwner) { status ->
            val filterText = when (status) {
                null -> "全部"
                BorrowStatus.BORROWED -> "已借出"
                BorrowStatus.RETURNED -> "已归还"
                BorrowStatus.OVERDUE -> "已逾期"
            }
            binding.btnFilter.text = filterText
        }
        
        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.clearErrorMessage()
            }
        }
        
        // 观察成功消息
        viewModel.successMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
    private fun showMoreOptionsDialog(borrow: BorrowWithItemInfo) {
        val options = mutableListOf<String>()
        
        // 根据状态显示不同选项
        when (borrow.status) {
            BorrowStatus.BORROWED, BorrowStatus.OVERDUE -> {
                options.add("归还物品")
            }
            BorrowStatus.RETURNED -> {
                // 已归还的记录暂时只能删除
            }
        }
        
        options.add("删除记录")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(borrow.itemName)
            .setItems(options.toTypedArray()) { _, which ->
                when {
                    // 归还物品
                    options[which] == "归还物品" -> {
                        showReturnConfirmDialog(borrow)
                    }
                    // 删除记录
                    options[which] == "删除记录" -> {
                        showDeleteConfirmDialog(borrow)
                    }
                }
            }
            .show()
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
     * 显示筛选对话框
     */
    private fun showFilterDialog() {
        val options = viewModel.getFilterOptions()
        val optionNames = options.map { it.name }.toTypedArray()
        val currentIndex = options.indexOfFirst { 
            it.status == viewModel.selectedStatus.value 
        }.takeIf { it >= 0 } ?: 0
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("筛选借还记录")
            .setSingleChoiceItems(optionNames, currentIndex) { dialog, which ->
                val selectedOption = options[which]
                viewModel.setStatusFilter(selectedOption.status)
                dialog.dismiss()
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
