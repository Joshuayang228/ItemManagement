package com.example.itemmanagement.ui.profile.recycle

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.DeletedItemEntity
import com.example.itemmanagement.databinding.FragmentRecycleBinBinding

/**
 * 回收站页面Fragment
 * 显示已删除的物品，支持恢复和彻底删除操作
 */
class RecycleBinFragment : Fragment() {
    
    private var _binding: FragmentRecycleBinBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var viewModel: RecycleBinViewModel
    private lateinit var recycleBinAdapter: RecycleBinAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecycleBinBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViewModel()
        setupRecyclerView()
        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupViewModel() {
        val app = requireActivity().application as ItemManagementApplication
        val factory = RecycleBinViewModelFactory(app.recycleBinRepository)
        viewModel = ViewModelProvider(this, factory)[RecycleBinViewModel::class.java]
    }
    
    private fun setupRecyclerView() {
        recycleBinAdapter = RecycleBinAdapter(
            onItemClick = { item ->
                // 点击物品 - 可以显示详情或预览
                showItemDetailDialog(item)
            },
            onRestoreClick = { item ->
                showRestoreConfirmDialog(item)
            },
            onDeleteClick = { item ->
                showDeleteConfirmDialog(item)
            },
            onSelectionChange = { item, isSelected ->
                if (isSelected) {
                    viewModel.toggleSelectMode()
                }
                viewModel.toggleItemSelection(item.originalId)
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recycleBinAdapter
        }
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        // 设置搜索功能
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchDeletedItems(newText ?: "")
                return true
            }
        })
    }
    
    private fun setupClickListeners() {
        // 选择模式按钮
        binding.btnSelectMode.setOnClickListener {
            viewModel.toggleSelectMode()
        }
        
        // 自动清理按钮
        binding.btnAutoClean.setOnClickListener {
            showAutoCleanConfirmDialog()
        }
        
        // 清空回收站按钮
        binding.btnClearAll.setOnClickListener {
            showClearAllConfirmDialog()
        }
        
        // 批量恢复
        binding.btnBatchRestore.setOnClickListener {
            showBatchRestoreConfirmDialog()
        }
        
        // 批量删除
        binding.btnBatchDelete.setOnClickListener {
            showBatchDeleteConfirmDialog()
        }
    }
    
    private fun observeViewModel() {
        // 观察已删除物品列表
        viewModel.deletedItems.observe(viewLifecycleOwner) { items ->
            updateRecyclerView(items)
            updateEmptyState(items.isEmpty())
        }
        
        // 观察搜索结果
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (viewModel.searchQuery.value?.isNotBlank() == true) {
                updateRecyclerView(results)
                updateEmptyState(results.isEmpty())
            }
        }
        
        // 观察物品数量
        viewModel.deletedItemCount.observe(viewLifecycleOwner) { count ->
            binding.tvTotalCount.text = count.toString()
        }
        
        // 观察统计信息
        viewModel.recycleBinStats.observe(viewLifecycleOwner) { stats ->
            binding.tvTotalCount.text = stats.totalCount.toString()
            binding.tvNearCleanCount.text = stats.nearAutoCleanCount.toString()
        }
        
        // 观察选择模式
        viewModel.isSelectMode.observe(viewLifecycleOwner) { isSelectMode ->
            recycleBinAdapter.isSelectMode = isSelectMode
            binding.layoutBatchActions.visibility = if (isSelectMode) View.VISIBLE else View.GONE
            
            // 更新选择模式按钮文本
            binding.btnSelectMode.text = if (isSelectMode) "取消" else "选择"
        }
        
        // 观察选中物品
        viewModel.selectedItems.observe(viewLifecycleOwner) { selectedItems ->
            recycleBinAdapter.selectedItems = selectedItems
            
            val selectedCount = selectedItems.size
            binding.tvSelectedCount.text = "已选择 $selectedCount 项"
            
            // 更新批量操作按钮状态
            val hasSelection = selectedCount > 0
            binding.btnBatchRestore.isEnabled = hasSelection
            binding.btnBatchDelete.isEnabled = hasSelection
        }
        
        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // 观察操作结果
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.success) {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                    
                    // 如果有恢复的物品，需要调用实际的恢复逻辑
                    handleRestoredItems(it)
                } else {
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_LONG).show()
                }
                viewModel.clearOperationResult()
            }
        }
    }
    
    private fun updateRecyclerView(items: List<DeletedItemEntity>) {
        recycleBinAdapter.submitList(items)
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun handleRestoredItems(result: OperationResult) {
        // TODO: 这里需要调用ItemRepository来实际恢复物品到数据库
        // 现在先简单提示用户
        val restoredItems = result.restoredItems
        if (restoredItems.isNotEmpty()) {
            // 这里应该调用ItemRepository的插入方法
            // 由于RecycleBinFragment无法直接访问ItemRepository，
            // 需要通过回调或其他方式通知上级处理
            
            // 临时方案：通过Toast告知用户需要手动处理
            Toast.makeText(
                requireContext(), 
                "物品已从回收站移除，请手动重新添加到库存", 
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // ==================== 对话框方法 ====================
    
    private fun showItemDetailDialog(item: DeletedItemEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("物品详情")
            .setMessage(buildItemDetailMessage(item))
            .setPositiveButton("恢复") { _, _ ->
                viewModel.restoreItem(item.originalId)
            }
            .setNegativeButton("删除") { _, _ ->
                showDeleteConfirmDialog(item)
            }
            .setNeutralButton("关闭", null)
            .show()
    }
    
    private fun buildItemDetailMessage(item: DeletedItemEntity): String {
        return buildString {
            appendLine("名称：${item.name}")
            appendLine("分类：${item.category}")
            if (!item.brand.isNullOrBlank()) {
                appendLine("品牌：${item.brand}")
            }
            if (item.quantity != null && item.unit != null) {
                appendLine("数量：${item.quantity}${item.unit}")
            }
            val locationText = item.getFullLocationString()
            if (!locationText.isNullOrBlank()) {
                appendLine("位置：$locationText")
            }
            appendLine("删除时间：${item.deletedDate}")
            appendLine("剩余天数：${item.getDaysUntilAutoClean()}天")
        }
    }
    
    private fun showRestoreConfirmDialog(item: DeletedItemEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("恢复物品")
            .setMessage("确定要恢复「${item.name}」吗？")
            .setPositiveButton("恢复") { _, _ ->
                viewModel.restoreItem(item.originalId)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showDeleteConfirmDialog(item: DeletedItemEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("彻底删除")
            .setMessage("确定要彻底删除「${item.name}」吗？\n\n此操作不可撤销！")
            .setPositiveButton("删除") { _, _ ->
                viewModel.permanentDeleteItem(item.originalId)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showBatchRestoreConfirmDialog() {
        val selectedCount = viewModel.selectedItems.value?.size ?: 0
        AlertDialog.Builder(requireContext())
            .setTitle("批量恢复")
            .setMessage("确定要恢复选中的 $selectedCount 个物品吗？")
            .setPositiveButton("恢复") { _, _ ->
                viewModel.restoreSelectedItems()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showBatchDeleteConfirmDialog() {
        val selectedCount = viewModel.selectedItems.value?.size ?: 0
        AlertDialog.Builder(requireContext())
            .setTitle("批量删除")
            .setMessage("确定要彻底删除选中的 $selectedCount 个物品吗？\n\n此操作不可撤销！")
            .setPositiveButton("删除") { _, _ ->
                viewModel.permanentDeleteSelectedItems()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showAutoCleanConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("自动清理")
            .setMessage("自动清理将删除超过30天的物品。\n\n确定继续吗？")
            .setPositiveButton("清理") { _, _ ->
                viewModel.performAutoClean()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showClearAllConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("清空回收站")
            .setMessage("确定要清空整个回收站吗？\n\n此操作将删除所有物品且不可撤销！")
            .setPositiveButton("清空") { _, _ ->
                viewModel.clearRecycleBin()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
