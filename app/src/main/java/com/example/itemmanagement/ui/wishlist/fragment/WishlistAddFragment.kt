package com.example.itemmanagement.ui.wishlist.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.base.BaseItemFragment
import com.example.itemmanagement.ui.wishlist.viewmodel.WishlistAddViewModel
import com.example.itemmanagement.ui.wishlist.viewmodel.WishlistViewModelFactory
import kotlinx.coroutines.launch

/**
 * 心愿单添加Fragment - 简化版本
 * 基于BaseItemFragment，专门用于添加新的心愿单物品
 * 已更新为使用统一架构
 */
class WishlistAddFragment : BaseItemFragment<WishlistAddViewModel>() {

    override val viewModel: WishlistAddViewModel by viewModels {
        val app = (requireActivity().application as ItemManagementApplication)
        WishlistViewModelFactory.forAdd(
            app.wishlistRepository,
            app.repository,
            cacheViewModel
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 设置心愿单专用的标题和图标
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close)
            actionBar.title = "添加到心愿单"
        }
    }

    override fun onViewModelReady() {
        // 初始化字段属性（确保在设置默认字段之前完成）
        viewModel.initializeDefaultFieldProperties()
        
        // ViewModel 已准备就绪，使用ViewModel内置的初始化方法
        viewModel.initializeWishlistDefaultFields()
        
        // 启用菜单
        setHasOptionsMenu(true)
    }

    override fun setupTitleAndButtons() {
        // 设置标题
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = "添加到心愿单"
    }

    override fun setupButtons() {
        // 设置编辑字段按钮
        binding.editFieldsButton.setOnClickListener {
            showEditFieldsDialog()
        }
        
        // 设置保存按钮
        binding.saveButton.setOnClickListener {
            saveWishlistItem()
        }
    }
    
    /**
     * 显示编辑字段对话框
     */
    private fun showEditFieldsDialog() {
        try {
            // 在显示编辑字段对话框前，先保存当前字段的值
            if (fieldViews.isNotEmpty()) {
                fieldValueManager.saveFieldValues(fieldViews)
            }
            
            // 导航到编辑字段界面
            // 这里简化实现，显示一个消息提示
            android.widget.Toast.makeText(context, "编辑字段功能开发中", android.widget.Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            android.util.Log.e("WishlistAddFragment", "显示编辑字段对话框失败", e)
            android.widget.Toast.makeText(context, "无法打开编辑字段：${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveWishlistItem() {
        lifecycleScope.launch {
            try {
                viewModel.saveOrUpdateItem()
                // 保存成功，返回上一页
                findNavController().popBackStack()
            } catch (e: Exception) {
                // 处理错误
                android.util.Log.e("WishlistAddFragment", "保存失败", e)
            }
        }
    }
}
