package com.example.itemmanagement.ui.wishlist.fragment

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentWishlistItemDetailBinding
import com.example.itemmanagement.ui.wishlist.viewmodel.WishlistItemDetailViewModel
import com.example.itemmanagement.ui.wishlist.viewmodel.WishlistViewModelFactory

/**
 * 心愿单物品详情Fragment
 * 展示心愿单物品的完整信息
 * 
 * 使用用户喜欢的"图片在上，信息在下"布局模式 [[memory:4615211]]
 * 
 * 核心功能：
 * 1. 展示心愿单物品的完整信息
 * 2. 价格信息和购买计划的可视化展示
 * 3. 快速操作按钮（编辑、删除、标记已购买）
 */
class WishlistDetailFragment : Fragment() {
    
    private var _binding: FragmentWishlistItemDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: WishlistDetailFragmentArgs by navArgs()
    
    private val viewModel: WishlistItemDetailViewModel by viewModels {
        val app = (requireActivity().application as ItemManagementApplication)
        WishlistViewModelFactory.forDetail(app.wishlistRepository, args.itemId)
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
        _binding = FragmentWishlistItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        
        android.util.Log.d("WishlistDetailFragment", "心愿单详情界面初始化完成 - 物品ID: ${args.itemId}")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_wishlist_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                navigateToEdit()
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmation()
                true
            }
            R.id.action_mark_purchased -> {
                markAsPurchased()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupUI() {
        // 设置快速操作按钮
        binding.buttonEdit?.setOnClickListener {
            navigateToEdit()
        }
        
        binding.buttonDelete?.setOnClickListener {
            showDeleteConfirmation()
        }
        
        binding.buttonMarkPurchased?.setOnClickListener {
            markAsPurchased()
        }
        
        binding.buttonUpdatePrice?.setOnClickListener {
            showPriceUpdateDialog()
        }
    }

    private fun observeViewModel() {
        // 观察心愿单物品数据
        viewModel.wishlistItem.observe(viewLifecycleOwner) { item ->
            item?.let { updateUI(it) }
        }

        // 观察加载状态
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // TODO: 可以添加加载指示器
            android.util.Log.d("WishlistDetailFragment", "加载状态: $isLoading")
        }

        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrBlank()) {
                showErrorMessage(errorMessage)
            }
        }

        // 观察操作成功消息
        viewModel.operationSuccess.observe(viewLifecycleOwner) { successMessage ->
            if (!successMessage.isNullOrBlank()) {
                showSuccessMessage(successMessage)
            }
        }

        // 观察导航事件
        viewModel.navigateBack.observe(viewLifecycleOwner) { shouldNavigate ->
            if (shouldNavigate) {
                findNavController().navigateUp()
                viewModel.onNavigationComplete()
            }
        }
    }

    private fun navigateToEdit() {
        findNavController().navigate(
            R.id.action_wishlist_detail_to_edit,
            android.os.Bundle().apply {
                putLong("itemId", args.itemId)
            }
        )
    }

    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("删除心愿单物品")
            .setMessage("确定要从心愿单中删除此物品吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteWishlistItem()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 更新UI显示
     */
    private fun updateUI(item: com.example.itemmanagement.data.entity.wishlist.WishlistItemEntity) {
        with(binding) {
            // 基础信息
            itemName.text = item.name
            itemCategory.text = buildString {
                append(item.category)
                if (!item.subCategory.isNullOrBlank()) {
                    append(" > ${item.subCategory}")
                }
            }
            itemSpecification.text = item.specification ?: "无规格信息"

            // 价格信息
            targetPrice.text = item.targetPrice?.let { "¥${"%.0f".format(it)}" } ?: "未设置"
            currentPrice.text = item.currentPrice?.let { "¥${"%.0f".format(it)}" } ?: "暂无价格"

            // 购买计划
            priority.text = item.priority.displayName
            urgency.text = item.urgency.displayName

            // 设置工具栏标题
            toolbar.title = item.name
        }
    }

    /**
     * 显示错误消息
     */
    private fun showErrorMessage(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
    }

    /**
     * 显示成功消息
     */
    private fun showSuccessMessage(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun deleteWishlistItem() {
        viewModel.deleteWishlistItem()
    }

    private fun markAsPurchased() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("标记为已购买")
            .setMessage("将此物品标记为已购买？")
            .setPositiveButton("确认") { _, _ ->
                viewModel.markAsPurchased()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showPriceUpdateDialog() {
        val editText = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "请输入新价格"
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("更新价格")
            .setMessage("输入当前市场价格：")
            .setView(editText)
            .setPositiveButton("更新") { _, _ ->
                val priceText = editText.text.toString()
                if (priceText.isNotBlank()) {
                    val newPrice = priceText.toDoubleOrNull()
                    if (newPrice != null && newPrice > 0) {
                        viewModel.updatePrice(newPrice)
                    } else {
                        showErrorMessage("请输入有效的价格")
                    }
                } else {
                    showErrorMessage("价格不能为空")
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
