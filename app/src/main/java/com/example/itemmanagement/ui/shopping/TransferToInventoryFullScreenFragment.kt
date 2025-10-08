package com.example.itemmanagement.ui.shopping

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.appcompat.app.AppCompatActivity
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.ui.add.Field
import com.example.itemmanagement.ui.base.BaseItemFragment

/**
 * 购物清单转入库存的全屏界面
 * 
 * 设计理念：
 * - 继承自 BaseItemFragment，复用所有字段和照片功能
 * - 使用 activityViewModels 与半屏对话框共享 TransferToInventoryViewModel 实例
 * - 数据自动同步，无需手动传递
 * - 返回购物清单时保持导航栏隐藏
 */
class TransferToInventoryFullScreenFragment : BaseItemFragment<TransferToInventoryViewModel>() {

    private var sourceItemId: Long = 0L
    private var itemName: String = ""

    // ⭐ 使用 activityViewModels 与半屏共享 ViewModel
    override val viewModel: TransferToInventoryViewModel by activityViewModels {
        val app = (requireActivity().application as ItemManagementApplication)
        TransferToInventoryViewModelFactory(
            app.repository,
            cacheViewModel,
            sourceItemId
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 从参数中获取源购物物品ID
        arguments?.let { args ->
            sourceItemId = args.getLong("itemId", 0L)
            itemName = args.getString("itemName", "")
        }
        
        android.util.Log.d("TransferFullScreen", "onCreate: sourceItemId=$sourceItemId, itemName=$itemName")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("TransferFullScreen", "onViewCreated")
        
        // 设置标题
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close)
            actionBar.title = if (itemName.isNotEmpty()) {
                "入库：$itemName"
            } else {
                "购物物品入库"
            }
        }
        
        // ⭐ 隐藏底部导航栏（购物清单专用）
        hideBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // 确保底部导航栏隐藏
        hideBottomNavigation()
        android.util.Log.d("TransferFullScreen", "onResume: 隐藏导航栏")
    }

    override fun onViewModelReady() {
        android.util.Log.d("TransferFullScreen", "onViewModelReady")
        
        // ViewModel 已准备就绪
        // 数据已经在半屏时加载并保存到缓存了
        // 这里不需要重新加载，因为使用的是同一个 ViewModel 实例
        
        // 初始化库存专用字段（如果还没有初始化）
        initializeInventoryFields()
        
        // 启用菜单
        setHasOptionsMenu(true)
    }

    /**
     * 初始化库存专用字段
     */
    private fun initializeInventoryFields() {
        android.util.Log.d("TransferFullScreen", "初始化库存字段")
        
        val inventoryFields = setOf(
            // 基础信息（已从购物详情预填充）
            Field("基础信息", "名称", true),
            Field("基础信息", "数量", true),
            
            // 位置信息（库存必需）
            Field("位置", "位置", true),
            
            // 分类信息
            Field("分类", "分类", true),
            Field("分类", "子分类", false),
            Field("分类", "品牌", false),
            Field("分类", "规格", false),
            
            // 价格信息（从购物详情转换）
            Field("价格", "单价", true),
            Field("价格", "总价", false),
            
            // 日期信息
            Field("日期类", "购买日期", true),
            Field("日期类", "生产日期", false),
            Field("日期类", "保质过期时间", false),
            
            // 保修信息
            Field("保修", "保修期", false),
            Field("保修", "保修到期时间", false),
            
            // 购买信息
            Field("购买信息", "购买渠道", false),
            Field("购买信息", "商家名称", false),
            
            // 其他信息
            Field("其他", "备注", false),
            Field("其他", "开封状态", false),
            Field("其他", "评分", false)
        )
        
        // 设置字段（只有在没有选中字段时才设置）
        val currentFields = viewModel.selectedFields.value
        if (currentFields.isNullOrEmpty()) {
            android.util.Log.d("TransferFullScreen", "当前无选中字段，设置默认字段")
            inventoryFields.forEach { field ->
                viewModel.updateFieldSelection(field, field.isSelected)
            }
        } else {
            android.util.Log.d("TransferFullScreen", "已有选中字段: ${currentFields.size} 个")
        }
    }

    override fun setupTitleAndButtons() {
        // 设置按钮文本
        binding.saveButton.text = "入库"
        binding.editFieldsButton.text = "编辑字段"
        
        android.util.Log.d("TransferFullScreen", "按钮文本已设置")
    }

    override fun setupButtons() {
        // 入库按钮
        binding.saveButton.setOnClickListener {
            performSaveToInventory()
        }
        
        // 编辑字段按钮
        binding.editFieldsButton.setOnClickListener {
            showEditFieldsDialog()
        }
        
        android.util.Log.d("TransferFullScreen", "按钮监听器已设置")
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
     * 执行入库操作（状态转换）
     */
    private fun performSaveToInventory() {
        android.util.Log.d("TransferFullScreen", "========== 开始入库流程 ==========")
        
        // 保存当前字段值
        if (fieldViews.isNotEmpty()) {
            fieldValueManager.saveFieldValues(fieldViews)
        }
        android.util.Log.d("TransferFullScreen", "字段值已保存")
        
        // 显示确认对话框
        dialogFactory.createConfirmDialog(
            title = "确认入库",
            message = "确定要将「${viewModel.getFieldValue("名称") ?: "此物品"}」转入库存吗？\n\n" +
                    "注意：\n" +
                    "• 物品将从购物清单中移除\n" +
                    "• 购物记录将被保留用于数据分析\n" +
                    "• 物品将以库存状态保存",
            positiveButtonText = "确认入库",
            negativeButtonText = "取消",
            onPositiveClick = {
                android.util.Log.d("TransferFullScreen", "用户确认入库")
                
                // 调用ViewModel的保存方法
                viewModel.performSave()
            }
        )
    }

    /**
     * 重写 onDestroyView，确保返回购物清单时导航栏保持隐藏
     * 
     * 三层防护机制：
     * 1. super调用前隐藏
     * 2. 调用super（父类BaseItemFragment会调用showBottomNavigation）
     * 3. super调用后再次隐藏（覆盖父类行为）
     */
    override fun onDestroyView() {
        // 第一层：super调用前隐藏
        hideBottomNavigation()
        android.util.Log.d("TransferFullScreen", "返回购物清单，保持导航栏隐藏")
        
        // 调用父类方法（父类会调用showBottomNavigation）
        super.onDestroyView()
        
        // 第三层：super调用后再次隐藏（覆盖父类）
        hideBottomNavigation()
    }

    companion object {
        /**
         * 创建Fragment实例的工厂方法
         */
        fun newInstance(itemId: Long, itemName: String = ""): TransferToInventoryFullScreenFragment {
            return TransferToInventoryFullScreenFragment().apply {
                arguments = Bundle().apply {
                    putLong("itemId", itemId)
                    putString("itemName", itemName)
                }
            }
        }
    }
}
