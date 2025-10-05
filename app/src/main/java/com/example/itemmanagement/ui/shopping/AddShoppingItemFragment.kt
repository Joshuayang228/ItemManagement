package com.example.itemmanagement.ui.shopping

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AppCompatActivity
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.base.BaseItemFragment

/**
 * 添加购物清单物品Fragment
 * 
 * 使用动态字段系统，继承自BaseItemFragment
 * 专门用于向购物清单添加物品
 */
class AddShoppingItemFragment : BaseItemFragment<AddShoppingItemViewModel>() {

    private var listId: Long = 0L
    private var listName: String = "购物清单"

    // 获取购物物品专用的ViewModel（延迟初始化）
    override val viewModel: AddShoppingItemViewModel by viewModels {
        // 先从arguments获取listId
        val actualListId = arguments?.getLong("listId", 1L) ?: 1L
        val actualListName = arguments?.getString("listName", "购物清单") ?: "购物清单"
        
        android.util.Log.d("AddShoppingItemFragment", "初始化ViewModel: listId=$actualListId, listName=$actualListName")
        
        val app = (requireActivity().application as ItemManagementApplication)
        val repository = app.repository
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(AddShoppingItemViewModel::class.java)) {
                    return AddShoppingItemViewModel(repository, cacheViewModel, actualListId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 从参数中获取购物清单ID和名称（用于UI显示）
        arguments?.let { args ->
            listId = args.getLong("listId", 1L)
            listName = args.getString("listName", "购物清单") ?: "购物清单"
            android.util.Log.d("AddShoppingItemFragment", "onCreate: listId=$listId, listName=$listName")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 🎨 设置关闭图标替代默认的返回箭头
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close)
            actionBar.title = "添加至 $listName"
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 确保底部导航栏隐藏（延迟执行确保生效）
        view?.post {
            hideBottomNavigation()
        }
    }

    override fun onViewModelReady() {
        // ViewModel已准备就绪，初始化默认字段
        initializeDefaultFields()
        
        // 启用菜单
        setHasOptionsMenu(true)
    }

    /**
     * 初始化购物清单专用的默认字段
     */
    private fun initializeDefaultFields() {
        val defaultFields = setOf(
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", true),
            Field("分类", "分类", true),
            Field("价格", "预估价格", true),
            Field("购买计划", "重要程度", true),  // ✅ 优化：优先级 -> 重要程度
            Field("基础信息", "备注", true)
        )
        
        // 设置默认字段
        defaultFields.forEach { field ->
            viewModel.updateFieldSelection(field, field.isSelected)
        }
    }

    override fun setupTitleAndButtons() {
        // 设置按钮文本
        binding.saveButton.text = "添加到清单"
        binding.editFieldsButton.text = "编辑字段"
    }

    override fun setupButtons() {
        // 保存按钮 - 使用自定义的保存逻辑
        binding.saveButton.setOnClickListener {
            performShoppingItemSave()
        }
        
        // 编辑字段按钮
        binding.editFieldsButton.setOnClickListener {
            showEditFieldsDialog()
        }
    }

    /**
     * 显示编辑字段对话框
     */
    private fun showEditFieldsDialog() {
        // 在显示编辑字段对话框前，先保存当前字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        // 使用EditFieldsFragment
        val editFieldsFragment = com.example.itemmanagement.ui.add.EditFieldsFragment.newInstance(viewModel, false)
        editFieldsFragment.show(childFragmentManager, "EditFieldsDialog")
    }

    /**
     * 执行保存购物物品操作
     */
    private fun performShoppingItemSave() {
        // 保存当前字段值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        // 调用ViewModel的保存方法
        viewModel.saveShoppingItem { success, message ->
            if (success) {
                // 显示成功消息并返回
                android.widget.Toast.makeText(
                    requireContext(),
                    message ?: "物品已添加到购物清单",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                
                // 延迟一下再返回，让用户看到消息
                view?.postDelayed({
                    activity?.onBackPressed()
                }, 500)
            } else {
                // 显示错误消息
                android.widget.Toast.makeText(
                    requireContext(),
                    message ?: "添加物品失败，请重试",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        /**
         * 创建Fragment实例的工厂方法
         */
        fun newInstance(listId: Long, listName: String): AddShoppingItemFragment {
            return AddShoppingItemFragment().apply {
                arguments = Bundle().apply {
                    putLong("listId", listId)
                    putString("listName", listName)
                }
            }
        }
    }
}

