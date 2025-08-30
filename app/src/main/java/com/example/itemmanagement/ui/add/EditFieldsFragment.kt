package com.example.itemmanagement.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.itemmanagement.databinding.FragmentEditFieldsBinding
import com.example.itemmanagement.ui.base.BaseItemViewModel
import com.example.itemmanagement.ui.base.FieldInteractionViewModel
import com.example.itemmanagement.ui.shopping.ShoppingFieldManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator
import android.util.Log

/**
 * 新架构的字段编辑Fragment
 * 保持与原版完全相同的UI和功能，但使用BaseItemViewModel架构
 */
class EditFieldsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEditFieldsBinding? = null
    private val binding get() = _binding!!
    private lateinit var baseViewModel: BaseItemViewModel
    private var isShoppingMode: Boolean = false
    private lateinit var tabs: List<String>
    private var currentAdapter: FieldsPagerAdapter? = null
    
    companion object {
        private const val ARG_IS_SHOPPING_MODE = "is_shopping_mode"
        
        fun newInstance(
            fieldViewModel: FieldInteractionViewModel,
            isShoppingMode: Boolean = false
        ): EditFieldsFragment {
            return EditFieldsFragment().apply {
                this.baseViewModel = fieldViewModel as BaseItemViewModel
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_SHOPPING_MODE, isShoppingMode)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取购物模式参数
        isShoppingMode = arguments?.getBoolean(ARG_IS_SHOPPING_MODE, false) ?: false
        
        // 根据模式设置tabs - 与原版完全一致
        tabs = if (isShoppingMode) {
            listOf("全部", "基础信息", "购物字段", "数字类", "日期类", "状态类", "分类", "商业类", "其他")
        } else {
            listOf("全部", "基础信息", "数字类", "日期类", "状态类", "分类", "商业类", "其他")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditFieldsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("EditFieldsFragment", "=== 编辑字段Fragment创建 ===")
        Log.d("EditFieldsFragment", "购物模式: $isShoppingMode")
        Log.d("EditFieldsFragment", "标签页: $tabs")
        
        // 记录当前选中的字段
        val currentSelectedFields = baseViewModel.selectedFields.value ?: emptySet()
        Log.d("EditFieldsFragment", "当前已选中字段数量: ${currentSelectedFields.size}")
        currentSelectedFields.sortedBy { it.order }.forEach { field ->
            Log.d("EditFieldsFragment", "已选中字段: ${field.name} (组: ${field.group}, 顺序: ${field.order})")
        }
        
        setupViews()
    }

    private fun setupViews() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }
        
        // 全选按钮点击事件
        binding.selectAllButton.setOnClickListener {
            selectAllFields()
        }

        setupViewPager()
        setupTabs()
    }

    private fun setupViewPager() {
        currentAdapter = FieldsPagerAdapter()
        binding.viewPager.adapter = currentAdapter

        // 添加页面切换监听器 - 与原版一致
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val tabName = if (position < tabs.size) tabs[position] else "未知"
                Log.d("EditFieldsFragment", "=== 切换到标签页: $tabName (位置: $position) ===")
                
                // 记录当前选中字段状态
                val currentSelectedFields = baseViewModel.selectedFields.value ?: emptySet()
                Log.d("EditFieldsFragment", "当前选中字段数量: ${currentSelectedFields.size}")
                currentSelectedFields.sortedBy { it.order }.forEach { field ->
                    Log.d("EditFieldsFragment", "选中字段: ${field.name} (顺序: ${field.order})")
                }
            }
        })
    }

    private fun setupTabs() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()
    }

    private inner class FieldsPagerAdapter : FragmentStateAdapter(this) {
        override fun getItemCount() = tabs.size

        override fun createFragment(position: Int): Fragment {
            val tabName = tabs[position]

            val fields = when (tabName) {
                "全部" -> getAllFields()
                "基础信息" -> getBasicFields()
                "购物字段" -> getShoppingFields()
                "数字类" -> getNumberFields()
                "日期类" -> getDateFields()
                "状态类" -> getStatusFields()
                "分类" -> getCategoryFields()
                "商业类" -> getCommercialFields()
                "其他" -> getOtherFields()
                else -> emptyList()
            }
            
            // 记录每个标签页的字段内容和顺序
            Log.d("EditFieldsFragment", "=== 创建标签页: $tabName ===")
            Log.d("EditFieldsFragment", "字段数量: ${fields.size}")
            
            // 检查order的连续性
            val orders = fields.map { it.order }.sorted()
            Log.d("EditFieldsFragment", "所有order值: $orders")
            
            // 检查是否有重复的order
            val duplicateOrders = orders.groupBy { it }.filter { it.value.size > 1 }
            if (duplicateOrders.isNotEmpty()) {
                Log.w("EditFieldsFragment", "发现重复的order: $duplicateOrders")
            }
            
            // 按order排序后记录
            val sortedFieldsForLog = fields.sortedBy { it.order }
            sortedFieldsForLog.forEachIndexed { index, field ->
                Log.d("EditFieldsFragment", "[$index] ${field.name} (组: ${field.group}, 顺序: ${field.order}, 选中: ${field.isSelected})")
            }
            
            // 记录实际传递给FieldListFragment的字段顺序
            Log.d("EditFieldsFragment", "传递给FieldListFragment的字段顺序:")
            fields.forEachIndexed { index, field ->
                Log.d("EditFieldsFragment", "原始[$index] ${field.name} (order: ${field.order})")
            }

            // 确保字段按order排序后传递给FieldListFragment
            val sortedFields = fields.sortedBy { it.order }
            Log.d("EditFieldsFragment", "最终排序后传递的字段:")
            sortedFields.forEachIndexed { index, field ->
                Log.d("EditFieldsFragment", "最终[$index] ${field.name} (order: ${field.order})")
            }
            
            return FieldListFragment.newInstance(sortedFields) { field, isSelected ->
                Log.d("EditFieldsFragment", "字段选择变化: ${field.name} -> $isSelected")
                baseViewModel.updateFieldSelection(field, isSelected)
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            return itemId in 0 until itemCount
        }
    }

    // ===== 字段分类方法 - 与原版完全一致 =====
    
    private fun getAllFields(): List<Field> {
        Log.d("EditFieldsFragment", "=== 构建全部字段列表 ===")
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        Log.d("EditFieldsFragment", "ViewModel中选中字段数量: ${selectedFields.size}")
        selectedFields.forEach { field ->
            Log.d("EditFieldsFragment", "ViewModel选中字段: ${field.name} (order: ${field.order})")
        }
        
        // 获取所有可能的字段名称
        val allFieldNames = setOf(
            "名称", "数量", "位置", "加入心愿单", "高周转",
            "单价", "总价", "容量", "评分",
            "添加日期", "购买日期", "生产日期", "保修期", "保修到期时间", 
            "保质期", "保质过期时间", "开封时间",
            "开封状态",
            "分类", "子分类", "标签", "季节",
            "购买渠道", "商家名称", "品牌", "序列号",
            "备注"
        )
        
        // 字段组映射
        val fieldGroupMap = mapOf(
            "名称" to "基础信息", "数量" to "基础信息", "位置" to "基础信息", 
            "加入心愿单" to "基础信息", "高周转" to "基础信息",
            "单价" to "数字类", "总价" to "数字类", "容量" to "数字类", "评分" to "数字类",
            "添加日期" to "日期类", "购买日期" to "日期类", "生产日期" to "日期类", 
            "保修期" to "日期类", "保修到期时间" to "日期类", "保质期" to "日期类", 
            "保质过期时间" to "日期类", "开封时间" to "日期类",
            "开封状态" to "状态类",
            "分类" to "分类", "子分类" to "分类", "标签" to "分类", "季节" to "分类",
            "购买渠道" to "商业类", "商家名称" to "商业类", "品牌" to "商业类", "序列号" to "商业类",
            "备注" to "其他"
        )
        
        val commonFields = mutableListOf<Field>()
        
        // 首先添加已选中的字段，保持它们的原有order
        selectedFields.forEach { selectedField ->
            if (allFieldNames.contains(selectedField.name)) {
                val group = fieldGroupMap[selectedField.name] ?: "其他"
                commonFields.add(Field(group, selectedField.name, true, selectedField.order))
                Log.d("EditFieldsFragment", "添加已选中字段: ${selectedField.name} (order: ${selectedField.order})")
            }
        }
        
        // 获取已使用的order值，避免冲突
        val usedOrders = selectedFields.map { it.order }.toSet()
        Log.d("EditFieldsFragment", "已使用的order值: $usedOrders")
        
        // 然后添加未选中的字段，使用默认order，但避免冲突
        allFieldNames.forEach { fieldName ->
            if (!selectedFields.any { it.name == fieldName }) {
                val group = fieldGroupMap[fieldName] ?: "其他"
                var defaultOrder = Field.getDefaultOrder(fieldName)
                
                // 如果默认order与已选中字段冲突，则找一个可用的order
                while (usedOrders.contains(defaultOrder)) {
                    defaultOrder += 100  // 加100确保不会与现有order冲突
                    Log.d("EditFieldsFragment", "字段 $fieldName 的order冲突，调整为: $defaultOrder")
                }
                
                commonFields.add(Field(group, fieldName, false, defaultOrder))
                Log.d("EditFieldsFragment", "添加未选中字段: $fieldName (order: $defaultOrder)")
            }
        }
        
        Log.d("EditFieldsFragment", "创建的通用字段数量: ${commonFields.size}")
        commonFields.sortedBy { it.order }.forEachIndexed { index, field ->
            Log.d("EditFieldsFragment", "通用字段[$index]: ${field.name} (order: ${field.order}, selected: ${field.isSelected})")
        }
        
        // 如果是购物模式，添加购物专用字段
        val finalFields = if (isShoppingMode) {
            val shoppingFields = getShoppingFields()
            Log.d("EditFieldsFragment", "购物字段数量: ${shoppingFields.size}")
            commonFields + shoppingFields
        } else {
            commonFields
        }
        
        Log.d("EditFieldsFragment", "最终字段列表数量: ${finalFields.size}")
        return finalFields
    }

    private fun getBasicFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("基础信息", "名称", selectedFields.any { it.name == "名称" }),
            Field("基础信息", "数量", selectedFields.any { it.name == "数量" }),
            Field("基础信息", "位置", selectedFields.any { it.name == "位置" }),
            Field("基础信息", "加入心愿单", selectedFields.any { it.name == "加入心愿单" }),
            Field("基础信息", "高周转", selectedFields.any { it.name == "高周转" })
        )
    }

    private fun getNumberFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("数字类", "单价", selectedFields.any { it.name == "单价" }),
            Field("数字类", "总价", selectedFields.any { it.name == "总价" }),
            Field("数字类", "容量", selectedFields.any { it.name == "容量" }),
            Field("数字类", "评分", selectedFields.any { it.name == "评分" }),
            Field("数字类", "数量", selectedFields.any { it.name == "数量" })
        )
    }

    private fun getDateFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("日期类", "添加日期", selectedFields.any { it.name == "添加日期" }),
            Field("日期类", "购买日期", selectedFields.any { it.name == "购买日期" }),
            Field("日期类", "生产日期", selectedFields.any { it.name == "生产日期" }),
            Field("日期类", "保修期", selectedFields.any { it.name == "保修期" }),
            Field("日期类", "保修到期时间", selectedFields.any { it.name == "保修到期时间" }),
            Field("日期类", "保质期", selectedFields.any { it.name == "保质期" }),
            Field("日期类", "保质过期时间", selectedFields.any { it.name == "保质过期时间" }),
            Field("日期类", "开封时间", selectedFields.any { it.name == "开封时间" })
        )
    }

    private fun getStatusFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("状态类", "开封状态", selectedFields.any { it.name == "开封状态" })
        )
    }

    private fun getCategoryFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("分类", "分类", selectedFields.any { it.name == "分类" }),
            Field("分类", "子分类", selectedFields.any { it.name == "子分类" }),
            Field("分类", "标签", selectedFields.any { it.name == "标签" }),
            Field("分类", "季节", selectedFields.any { it.name == "季节" })
        )
    }

    private fun getCommercialFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("商业类", "单价", selectedFields.any { it.name == "单价" }),
            Field("商业类", "购买渠道", selectedFields.any { it.name == "购买渠道" }),
            Field("商业类", "商家名称", selectedFields.any { it.name == "商家名称" }),
            Field("商业类", "品牌", selectedFields.any { it.name == "品牌" }),
            Field("商业类", "序列号", selectedFields.any { it.name == "序列号" })
        )
    }

    private fun getOtherFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("其他", "备注", selectedFields.any { it.name == "备注" })
        )
    }
    
    private fun getShoppingFields(): List<Field> {
        if (!isShoppingMode) return emptyList()
        
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        val shoppingFieldNames = ShoppingFieldManager.getDefaultShoppingFields().filter { fieldName ->
            // 过滤掉基础信息字段，因为它们在"基础信息"tab中
            !setOf("名称", "数量", "分类", "品牌", "备注").contains(fieldName)
        }
        
        return shoppingFieldNames.map { fieldName ->
            val group = ShoppingFieldManager.getShoppingFieldGroup(fieldName)
            Field(group, fieldName, selectedFields.any { it.name == fieldName })
        }.sortedBy { ShoppingFieldManager.getShoppingFieldOrder(it.name) }
    }

    /**
     * 全选所有字段
     */
    private fun selectAllFields() {
        Log.d("EditFieldsFragment", "=== 开始全选字段 ===")
        
        try {
            // 获取所有字段
            val allFields = getAllFields()
            Log.d("EditFieldsFragment", "获取到所有字段数量: ${allFields.size}")
            
            if (allFields.isEmpty()) {
                Log.w("EditFieldsFragment", "没有可选择的字段")
                return
            }
            
            // 更新全选按钮状态，防止重复点击
            binding.selectAllButton.isEnabled = false
            binding.selectAllButton.text = "全选中..."
            
            // 批量更新字段选择状态
            allFields.forEach { field ->
                val selectedField = Field(field.group, field.name, true, field.order)
                baseViewModel.updateFieldSelection(selectedField, true)
            }
            
            Log.d("EditFieldsFragment", "设置选中字段数量: ${allFields.size}")
            
            // 延迟刷新UI，确保数据更新完成
            binding.root.post {
                try {
                    // 刷新所有页面的UI
                    currentAdapter?.notifyDataSetChanged()
                    
                    // 恢复按钮状态
                    binding.selectAllButton.isEnabled = true
                    binding.selectAllButton.text = "全选"
                    
                    // 显示完成提示
                    android.widget.Toast.makeText(
                        requireContext(), 
                        "已全选 ${allFields.size} 个字段", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    
                    Log.d("EditFieldsFragment", "全选完成，UI已刷新")
                } catch (e: Exception) {
                    Log.e("EditFieldsFragment", "刷新UI时出错: ${e.message}")
                    // 确保按钮状态恢复
                    binding.selectAllButton.isEnabled = true
                    binding.selectAllButton.text = "全选"
                }
            }
            
        } catch (e: Exception) {
            Log.e("EditFieldsFragment", "全选字段时出错: ${e.message}")
            // 确保按钮状态恢复
            binding.selectAllButton.isEnabled = true
            binding.selectAllButton.text = "全选"
            
            android.widget.Toast.makeText(
                requireContext(), 
                "全选失败: ${e.message}", 
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onDestroyView() {
        currentAdapter = null
        _binding = null
        super.onDestroyView()
    }
}