package com.example.itemmanagement.ui.wishlist.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.base.BaseItemFragment
import com.example.itemmanagement.ui.wishlist.WishlistFieldManager
import com.example.itemmanagement.ui.wishlist.viewmodel.WishlistAddViewModel
import com.example.itemmanagement.ui.wishlist.viewmodel.WishlistViewModelFactory

/**
 * 心愿单添加Fragment
 * 基于BaseItemFragment，专门用于添加新的心愿单物品
 * 
 * 核心特性：
 * 1. 完全复用BaseItemFragment的UI管理系统
 * 2. 使用心愿单专用的字段配置和ViewModel
 * 3. 保持与现有添加界面一致的用户体验
 * 4. 使用用户熟悉的界面风格 [[memory:4615211]]
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
        android.util.Log.d("WishlistAddFragment", "🚀 onViewCreated 开始")
        android.util.Log.d("WishlistAddFragment", "📦 savedInstanceState: $savedInstanceState")
        
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("WishlistAddFragment", "✅ 父类onViewCreated完成")
        
        // 🎨 设置心愿单专用的标题和图标
        android.util.Log.d("WishlistAddFragment", "🎨 设置心愿单专用标题")
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            android.util.Log.d("WishlistAddFragment", "🎯 设置 ActionBar 属性")
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close)
            actionBar.title = "添加到心愿单"
            android.util.Log.d("WishlistAddFragment", "   ✅ ActionBar 设置完成")
        } ?: android.util.Log.w("WishlistAddFragment", "⚠️ ActionBar 为空，无法设置标题")
        
        android.util.Log.d("WishlistAddFragment", "🎉 onViewCreated 完成")
    }

    override fun onViewModelReady() {
        android.util.Log.d("WishlistAddFragment", "🚀 onViewModelReady 开始")
        android.util.Log.d("WishlistAddFragment", "📁 ViewModel类型: ${viewModel.javaClass.simpleName}")
        android.util.Log.d("WishlistAddFragment", "🔍 ViewModel hashCode: ${viewModel.hashCode()}")
        
        // 检查ViewModel的初始状态
        android.util.Log.d("WishlistAddFragment", "📊 ViewModel初始状态检查:")
        android.util.Log.d("WishlistAddFragment", "   📋 当前选中字段数量: ${viewModel.selectedFields.value?.size}")
        android.util.Log.d("WishlistAddFragment", "   💾 当前字段值数量: [protected属性，无法访问]")
        android.util.Log.d("WishlistAddFragment", "   🗺️ 字段属性数量: ${viewModel.getAllFieldProperties().size}")
        
        // 初始化字段属性（确保在设置默认字段之前完成）
        android.util.Log.d("WishlistAddFragment", "🔧 先初始化字段属性")
        viewModel.initializeDefaultFieldProperties()
        android.util.Log.d("WishlistAddFragment", "✅ 字段属性初始化完成")
        
        // ViewModel 已准备就绪，使用ViewModel内置的初始化方法
        android.util.Log.d("WishlistAddFragment", "🎯 初始化心愿单默认字段")
        viewModel.initializeWishlistDefaultFields()
        android.util.Log.d("WishlistAddFragment", "✅ 默认字段初始化完成")
        
        // 检查初始化后的状态
        android.util.Log.d("WishlistAddFragment", "📊 初始化后的状态检查:")
        android.util.Log.d("WishlistAddFragment", "   📋 选中字段数量: ${viewModel.selectedFields.value?.size}")
        android.util.Log.d("WishlistAddFragment", "   💾 字段值数量: [protected属性，无法访问]")
        android.util.Log.d("WishlistAddFragment", "   🗺️ 字段属性数量: ${viewModel.getAllFieldProperties().size}")
        
        // 打印关键字段的属性
        val keyFields = listOf("优先级", "紧急程度", "添加日期", "购买计划")
        keyFields.forEach { fieldName ->
            val properties = viewModel.getFieldProperties(fieldName)
            android.util.Log.d("WishlistAddFragment", "🔍 关键字段 '$fieldName' 属性: $properties")
        }
        
        // 启用菜单
        android.util.Log.d("WishlistAddFragment", "📝 启用菜单")
        setHasOptionsMenu(true)
        
        // 注意：字段UI刷新由BaseItemFragment自动处理
        android.util.Log.d("WishlistAddFragment", "📝 字段UI将由BaseItemFragment自动刷新")
        
        android.util.Log.d("WishlistAddFragment", "🎉 心愿单添加界面初始化完成")
    }

    override fun setupTitleAndButtons() {
        android.util.Log.d("WishlistAddFragment", "🎯 setupTitleAndButtons 开始")
        
        // 设置心愿单专用的按钮文本
        android.util.Log.d("WishlistAddFragment", "📝 设置按钮文本")
        binding.saveButton.text = "添加到心愿单"
        binding.editFieldsButton.text = "编辑字段"
        android.util.Log.d("WishlistAddFragment", "✅ 按钮文本设置完成")
        
        android.util.Log.d("WishlistAddFragment", "🎉 setupTitleAndButtons 完成")
    }

    override fun setupButtons() {
        android.util.Log.d("WishlistAddFragment", "🔘 setupButtons 开始")
        
        // 保存按钮
        android.util.Log.d("WishlistAddFragment", "💾 设置保存按钮监听器")
        binding.saveButton.setOnClickListener {
            android.util.Log.d("WishlistAddFragment", "💾 保存按钮被点击")
            performSave()
        }
        
        // 编辑字段按钮
        android.util.Log.d("WishlistAddFragment", "✏️ 设置编辑字段按钮监听器")
        binding.editFieldsButton.setOnClickListener {
            android.util.Log.d("WishlistAddFragment", "✏️ 编辑字段按钮被点击")
            showEditFieldsDialog()
        }
        
        android.util.Log.d("WishlistAddFragment", "✅ setupButtons 完成")
    }

    
    override fun onResume() {
        super.onResume()
        // 确保标题正确显示
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title = "添加到心愿单"
    }
    
    /**
     * 处理保存成功事件
     */
    override fun onSaveSuccess() {
        android.util.Log.d("WishlistAddFragment", "🎉 onSaveSuccess 被调用")
        
        // 显示成功消息
        android.util.Log.d("WishlistAddFragment", "💬 显示成功消息")
        showSuccessMessage("已成功添加到心愿单")
        
        // 导航回心愿单主页面
        android.util.Log.d("WishlistAddFragment", "🔙 导航回心愿单主页面")
        try {
            findNavController().navigateUp()
            android.util.Log.d("WishlistAddFragment", "✅ 导航成功")
        } catch (e: Exception) {
            android.util.Log.e("WishlistAddFragment", "❌ 导航失败: ${e.message}", e)
        }
    }
    
    /**
     * 显示字段选择对话框
     */
    private fun showEditFieldsDialog() {
        android.util.Log.d("WishlistAddFragment", "📝 showEditFieldsDialog 开始")
        android.util.Log.d("WishlistAddFragment", "📋 当前fieldViews数量: ${fieldViews.size}")
        
        // 在显示编辑字段对话框前，先保存当前字段的值
        if (fieldViews.isNotEmpty()) {
            android.util.Log.d("WishlistAddFragment", "💾 保存当前字段值")
            fieldValueManager.saveFieldValues(fieldViews)
            android.util.Log.d("WishlistAddFragment", "✅ 字段值保存完成")
        } else {
            android.util.Log.w("WishlistAddFragment", "⚠️ fieldViews为空，跳过保存")
        }
        
        // 使用EditFieldsFragment
        android.util.Log.d("WishlistAddFragment", "🔍 创建EditFieldsFragment")
        val editFieldsFragment = com.example.itemmanagement.ui.add.EditFieldsFragment.newInstance(viewModel, false)
        android.util.Log.d("WishlistAddFragment", "💬 显示EditFieldsFragment")
        editFieldsFragment.show(childFragmentManager, "EditFieldsFragment")
        android.util.Log.d("WishlistAddFragment", "🎉 showEditFieldsDialog 完成")
    }

    /**
     * 显示成功消息
     */
    private fun showSuccessMessage(message: String) {
        // 可以使用现有的Material3Feedback或者简单的Toast
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
