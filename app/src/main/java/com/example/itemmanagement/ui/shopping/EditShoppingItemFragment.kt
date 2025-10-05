package com.example.itemmanagement.ui.shopping

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.base.BaseItemFragment
import com.example.itemmanagement.ui.add.Field

/**
 * 编辑购物物品 Fragment
 * 
 * 继承自 BaseItemFragment，复用动态字段系统
 * 专门用于编辑购物清单中的物品
 */
class EditShoppingItemFragment : BaseItemFragment<EditShoppingItemViewModel>() {

    private var itemId: Long = 0L
    private var listId: Long = 0L
    private var listName: String = "购物清单"

    override val viewModel: EditShoppingItemViewModel by viewModels {
        // 先从arguments获取参数
        val actualItemId = arguments?.getLong("itemId", 0L) ?: 0L
        val actualListId = arguments?.getLong("listId", 0L) ?: 0L
        
        android.util.Log.d("EditShoppingItem", "初始化Fragment ViewModel: itemId=$actualItemId, listId=$actualListId")
        
        val app = (requireActivity().application as ItemManagementApplication)
        val repository = app.repository
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return EditShoppingItemViewModel(repository, cacheViewModel, actualItemId, actualListId) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        arguments?.let {
            itemId = it.getLong("itemId", 0L)
            listId = it.getLong("listId", 0L)
            listName = it.getString("listName", "购物清单") ?: "购物清单"
            
            android.util.Log.d("EditShoppingItem", "Fragment onCreate: itemId=$itemId, listId=$listId, listName=$listName")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 隐藏底部导航栏
        hideBottomNavigation()
        
        // 设置关闭图标
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close)
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 确保底部导航栏隐藏
        view?.post { hideBottomNavigation() }
    }

    override fun onViewModelReady() {
        // ViewModel 已自动加载数据，无需额外操作
        android.util.Log.d("EditShoppingItem", "ViewModel已准备就绪")
    }

    override fun setupTitleAndButtons() {
        activity?.title = "编辑购物物品"
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

    /**
     * 显示编辑字段对话框
     */
    private fun showEditFieldsDialog() {
        // 在显示编辑字段对话框前，先保存当前字段的值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        
        // 使用EditFieldsFragment
        val editFieldsFragment = com.example.itemmanagement.ui.add.EditFieldsFragment
            .newInstance(viewModel, false)
        editFieldsFragment.show(childFragmentManager, "EditFieldsDialog")
    }

    companion object {
        /**
         * 创建Fragment实例的工厂方法
         */
        fun newInstance(itemId: Long, listId: Long, listName: String): EditShoppingItemFragment {
            return EditShoppingItemFragment().apply {
                arguments = Bundle().apply {
                    putLong("itemId", itemId)
                    putLong("listId", listId)
                    putString("listName", listName)
                }
            }
        }
    }
}


