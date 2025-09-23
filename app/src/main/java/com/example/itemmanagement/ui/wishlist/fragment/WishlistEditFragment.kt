package com.example.itemmanagement.ui.wishlist.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.base.BaseItemFragment
import com.example.itemmanagement.ui.wishlist.viewmodel.WishlistEditViewModel
import com.example.itemmanagement.ui.wishlist.viewmodel.WishlistViewModelFactory

/**
 * 心愿单编辑Fragment
 * 基于BaseItemFragment，专门用于编辑现有的心愿单物品
 * 
 * 核心特性：
 * 1. 完全复用BaseItemFragment的UI管理系统
 * 2. 每个物品ID有独立的ViewModel实例和缓存空间
 * 3. 智能的状态缓存和恢复
 * 4. 使用用户熟悉的界面风格 [[memory:4615211]]
 */
class WishlistEditFragment : BaseItemFragment<WishlistEditViewModel>() {

    // 获取导航参数
    private val args: WishlistEditFragmentArgs by navArgs()

    override val viewModel: WishlistEditViewModel by viewModels {
        val app = (requireActivity().application as ItemManagementApplication)
        WishlistViewModelFactory.forEdit(
            app.wishlistRepository,
            app.repository,
            cacheViewModel,
            args.itemId
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 🎨 设置心愿单编辑专用的标题和图标
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close)
            actionBar.title = "编辑心愿单"
        }
        
        // 观察额外的数据
        observeAdditionalData()
    }

    override fun onViewModelReady() {
        android.util.Log.d("WishlistEditFragment", "🚀 onViewModelReady 开始 (编辑模式)")
        android.util.Log.d("WishlistEditFragment", "📁 ViewModel类型: ${viewModel::class.simpleName}")
        android.util.Log.d("WishlistEditFragment", "🔍 ViewModel hashCode: ${viewModel.hashCode()}")
        android.util.Log.d("WishlistEditFragment", "🆔 物品ID: ${args.itemId}")
        
        // ViewModel 已准备就绪，加载现有物品数据
        android.util.Log.d("WishlistEditFragment", "📋 开始加载心愿单物品数据")
        viewModel.loadWishlistItem()
        setHasOptionsMenu(true)
        
        android.util.Log.d("WishlistEditFragment", "✅ 心愿单编辑界面初始化完成 - 物品ID: ${args.itemId}")
    }

    override fun setupTitleAndButtons() {
        // 设置心愿单编辑专用的按钮文本
        binding.saveButton.text = "保存修改"
        binding.editFieldsButton.text = "编辑字段"
    }

    override fun setupButtons() {
        // 保存按钮
        binding.saveButton.setOnClickListener {
            performSave()
        }
        
        // 编辑字段按钮
        binding.editFieldsButton.setOnClickListener {
            showEditFieldsDialog()
        }
    }

    private fun observeAdditionalData() {
        // 观察错误消息
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrBlank()) {
                showErrorMessage(errorMessage)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 确保标题正确显示
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = "编辑心愿单"
    }
    
    /**
     * 处理保存成功事件
     */
    override fun onSaveSuccess() {
        // 显示成功消息
        showSuccessMessage("心愿单物品更新成功")
        
        // 导航回心愿单主页面或详情页面
        findNavController().navigateUp()
    }

    /**
     * 显示字段选择对话框
     */
    private fun showEditFieldsDialog() {
        // 在显示编辑字段对话框前，先保存当前字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        // 使用EditFieldsFragment
        val editFieldsFragment = com.example.itemmanagement.ui.add.EditFieldsFragment.newInstance(viewModel, false)
        editFieldsFragment.show(childFragmentManager, "EditFieldsFragment")
    }
    
    /**
     * 显示成功消息
     */
    private fun showSuccessMessage(message: String) {
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 显示错误消息
     */
    private fun showErrorMessage(message: String) {
        android.widget.Toast.makeText(requireContext(), "错误: $message", android.widget.Toast.LENGTH_LONG).show()
        android.util.Log.e("WishlistEditFragment", "错误: $message")
    }
}
