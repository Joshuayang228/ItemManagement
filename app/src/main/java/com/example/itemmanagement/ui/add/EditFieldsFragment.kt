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
        
        // 添加日志，帮助排查问题
        Log.d("EditFieldsFragment", "=== 📦 初始化编辑字段Fragment ===")
        Log.d("EditFieldsFragment", "购物模式参数: isShoppingMode = $isShoppingMode")
        
        // 根据模式设置tabs - 优化后的分类
        tabs = if (isShoppingMode) {
            // 购物模式：购物管理紧跟基础信息
            Log.d("EditFieldsFragment", "✅ 使用购物模式 - 显示「购物管理」tab")
            listOf("全部", "基础信息", "购物管理", "数字类", "日期类", "商业类", "其他")
        } else {
            // 库存模式：库存管理紧跟基础信息
            Log.d("EditFieldsFragment", "✅ 使用库存模式 - 显示「库存管理」tab")
            listOf("全部", "基础信息", "库存管理", "数字类", "日期类", "商业类", "其他")
        }
        
        Log.d("EditFieldsFragment", "标签页列表: ${tabs.joinToString(", ")}")
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
                "购物管理" -> getShoppingFields()
                "数字类" -> getNumberFields()
                "日期类" -> getDateFields()
                "库存管理" -> getInventoryFields()
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
        
        // 获取所有可能的字段名称（区分库存和购物模式）
        
        // === 通用字段（两种模式都需要）===
        val commonFieldNames = setOf(
            // 基础信息
            "名称", "数量", "分类", "子分类", "品牌", "标签", "季节", "备注",
            // 数字类
            "单价", "总价", "容量", "规格", "评分",
            // 日期类（通用）
            "添加日期",
            // 商业类（通用）
            "购买渠道", "商家名称", "序列号"
        )
        
        // === 库存专有字段（仅库存模式）===
        val inventoryOnlyFields = setOf(
            "位置", "开封状态",
            // 食品相关日期
            "生产日期", "保质期", "保质过期时间", "购买日期",
            // 电子产品相关日期
            "保修期", "保修到期时间"
        )
        
        // === 购物专有字段（仅购物模式）===
        val shoppingOnlyFields = setOf(
            "预估价格", "预算上限", "购买商店",
            "重要程度", "紧急程度", "截止日期", "购买原因"
        )
        
        // 根据模式选择字段
        val allFieldNames = if (isShoppingMode) {
            Log.d("EditFieldsFragment", "🛒 购物模式：通用字段(${commonFieldNames.size}) + 购物专有(${shoppingOnlyFields.size})")
            commonFieldNames + shoppingOnlyFields
        } else {
            Log.d("EditFieldsFragment", "📦 库存模式：通用字段(${commonFieldNames.size}) + 库存专有(${inventoryOnlyFields.size})")
            commonFieldNames + inventoryOnlyFields
        }
        
        Log.d("EditFieldsFragment", "总字段数: ${allFieldNames.size}")
        
        // 字段组映射（优化后的分组）
        val fieldGroupMap = mutableMapOf<String, String>().apply {
            // 基础信息：物品核心属性
            put("名称", "基础信息")
            put("数量", "基础信息")
            put("分类", "基础信息")
            put("子分类", "基础信息")
            put("品牌", "基础信息")
            put("标签", "基础信息")
            put("季节", "基础信息")
            put("单价", "基础信息")
            put("备注", "基础信息")
            
            // 数字类
            put("总价", "数字类")
            put("容量", "数字类")
            put("评分", "数字类")
            put("规格", "数字类")
            
            // 日期类（通用）
            put("添加日期", "日期类")
            put("购买日期", "日期类")
            
            // 商业类
            put("商家名称", "商业类")
            put("序列号", "商业类")
            
            if (isShoppingMode) {
                // === 购物模式独有映射 ===
                // 购物管理
                put("预估价格", "购物管理")
                put("购买渠道", "购物管理")
                put("购买商店", "购物管理")
                put("预算上限", "购物管理")
                put("重要程度", "购物管理")
                put("紧急程度", "购物管理")
                put("截止日期", "购物管理")
                put("购买原因", "购物管理")
                
                // 日期类（购物模式专有）
                put("截止日期", "日期类")
            } else {
                // === 库存模式独有映射 ===
                // 库存管理
                put("位置", "库存管理")
                put("开封状态", "库存管理")
                put("生产日期", "库存管理")
                put("保质期", "库存管理")
                put("保质过期时间", "库存管理")
                
                // 日期类（库存专有）
                put("生产日期", "日期类")
                put("保修期", "日期类")
                put("保修到期时间", "日期类")
                put("保质期", "日期类")
                put("保质过期时间", "日期类")
                
                // 商业类（库存模式下"购买渠道"在这里）
                put("购买渠道", "商业类")
            }
        }
        
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
        
        Log.d("EditFieldsFragment", "创建的字段数量: ${commonFields.size}")
        commonFields.sortedBy { it.order }.forEachIndexed { index, field ->
            Log.d("EditFieldsFragment", "字段[$index]: ${field.name} (order: ${field.order}, selected: ${field.isSelected})")
        }
        
        // 所有字段已经在 commonFields 中（包括购物/库存专有字段），无需额外添加
        Log.d("EditFieldsFragment", "最终字段列表数量: ${commonFields.size}")
        return commonFields
    }

    private fun getBasicFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("基础信息", "名称", selectedFields.any { it.name == "名称" }),
            Field("基础信息", "数量", selectedFields.any { it.name == "数量" }),
            Field("基础信息", "分类", selectedFields.any { it.name == "分类" }),
            Field("基础信息", "子分类", selectedFields.any { it.name == "子分类" }),
            Field("基础信息", "品牌", selectedFields.any { it.name == "品牌" }),
            Field("基础信息", "标签", selectedFields.any { it.name == "标签" }),
            Field("基础信息", "季节", selectedFields.any { it.name == "季节" }),
            Field("基础信息", "单价", selectedFields.any { it.name == "单价" }),
            Field("基础信息", "备注", selectedFields.any { it.name == "备注" })
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

    /**
     * 日期类字段（根据模式返回不同字段）
     */
    private fun getDateFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        
        return if (isShoppingMode) {
            // 购物模式：只显示购物相关日期（2个）
            listOf(
                Field("日期类", "添加日期", selectedFields.any { it.name == "添加日期" }),
                Field("日期类", "截止日期", selectedFields.any { it.name == "截止日期" })
            )
        } else {
            // 库存模式：显示所有日期字段（7个）
            listOf(
                Field("日期类", "添加日期", selectedFields.any { it.name == "添加日期" }),
                Field("日期类", "购买日期", selectedFields.any { it.name == "购买日期" }),
                Field("日期类", "生产日期", selectedFields.any { it.name == "生产日期" }),
                Field("日期类", "保修期", selectedFields.any { it.name == "保修期" }),
                Field("日期类", "保修到期时间", selectedFields.any { it.name == "保修到期时间" }),
                Field("日期类", "保质期", selectedFields.any { it.name == "保质期" }),
                Field("日期类", "保质过期时间", selectedFields.any { it.name == "保质过期时间" })
            )
        }
    }

    private fun getInventoryFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("库存管理", "数量", selectedFields.any { it.name == "数量" }),
            Field("库存管理", "位置", selectedFields.any { it.name == "位置" }),
            Field("库存管理", "开封状态", selectedFields.any { it.name == "开封状态" }),
            Field("库存管理", "生产日期", selectedFields.any { it.name == "生产日期" }),
            Field("库存管理", "保质期", selectedFields.any { it.name == "保质期" }),
            Field("库存管理", "保质过期时间", selectedFields.any { it.name == "保质过期时间" })
        )
    }

    /**
     * 商业类字段（购物模式下不重复显示"购买渠道"）
     */
    private fun getCommercialFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        
        val fields = mutableListOf<Field>()
        fields.add(Field("商业类", "单价", selectedFields.any { it.name == "单价" }))
        
        // 购买渠道：购物模式下只在"购物管理"tab显示，库存模式下在"商业类"显示
        if (!isShoppingMode) {
            fields.add(Field("商业类", "购买渠道", selectedFields.any { it.name == "购买渠道" }))
        }
        
        fields.add(Field("商业类", "商家名称", selectedFields.any { it.name == "商家名称" }))
        fields.add(Field("商业类", "品牌", selectedFields.any { it.name == "品牌" }))
        fields.add(Field("商业类", "序列号", selectedFields.any { it.name == "序列号" }))
        
        return fields
    }

    private fun getOtherFields(): List<Field> {
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        return listOf(
            Field("其他", "备注", selectedFields.any { it.name == "备注" }),
            Field("其他", "规格", selectedFields.any { it.name == "规格" }),
            Field("其他", "序列号", selectedFields.any { it.name == "序列号" })
        )
    }
    
    /**
     * 购物管理字段（购物清单专用）
     * 8个字段，覆盖购物计划和执行
     */
    private fun getShoppingFields(): List<Field> {
        if (!isShoppingMode) return emptyList()
        
        val selectedFields = baseViewModel.selectedFields.value ?: emptySet()
        
        // 按逻辑分组定义购物管理字段
        return listOf(
            // === 价格预算（4个）===
            Field("购物管理", "预估价格", selectedFields.any { it.name == "预估价格" }),
            Field("购物管理", "购买渠道", selectedFields.any { it.name == "购买渠道" }),
            Field("购物管理", "购买商店", selectedFields.any { it.name == "购买商店" }),
            Field("购物管理", "预算上限", selectedFields.any { it.name == "预算上限" }),
            
            // === 优先级规划（3个）===
            Field("购物管理", "重要程度", selectedFields.any { it.name == "重要程度" }),
            Field("购物管理", "紧急程度", selectedFields.any { it.name == "紧急程度" }),
            Field("购物管理", "截止日期", selectedFields.any { it.name == "截止日期" }),
            
            // === 备注（1个）===
            Field("购物管理", "购买原因", selectedFields.any { it.name == "购买原因" })
        )
    }

    /**
     * 全选当前tab的字段 - 只选中当前tab，保留其他tab已选字段
     */
    private fun selectAllFields() {
        Log.d("EditFieldsFragment", "=== 开始全选当前tab字段 ===")
        
        try {
            // 获取当前tab位置
            val currentPosition = binding.viewPager.currentItem
            val currentTabName = if (currentPosition < tabs.size) tabs[currentPosition] else "未知"
            
            Log.d("EditFieldsFragment", "当前tab: $currentTabName (位置: $currentPosition)")
            
            // 获取当前tab的字段
            val currentTabFields = when (currentTabName) {
                "全部" -> getAllFields()
                "基础信息" -> getBasicFields()
                "购物管理" -> getShoppingFields()
                "数字类" -> getNumberFields()
                "日期类" -> getDateFields()
                "库存管理" -> getInventoryFields()
                "商业类" -> getCommercialFields()
                "其他" -> getOtherFields()
                else -> emptyList()
            }
            
            Log.d("EditFieldsFragment", "当前tab有 ${currentTabFields.size} 个字段")
            
            if (currentTabFields.isEmpty()) {
                Log.w("EditFieldsFragment", "当前tab没有可选择的字段")
                android.widget.Toast.makeText(
                    requireContext(), 
                    "当前tab没有可选字段", 
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }
            
            // 更新全选按钮状态，防止重复点击
            binding.selectAllButton.isEnabled = false
            binding.selectAllButton.text = "选中中..."
            
            // 获取当前已选中的字段（转为可变集合）
            val currentSelectedFields = (baseViewModel.selectedFields.value ?: emptySet()).toMutableSet()
            
            // 将当前tab的所有字段标记为选中
            val fieldsToSelect = currentTabFields.map { field ->
                Field(field.group, field.name, true, field.order)
            }
            
            // 合并到已选中的字段集合中（去重）
            fieldsToSelect.forEach { field ->
                // 移除旧的同名字段，添加新的选中状态
                currentSelectedFields.removeIf { it.name == field.name }
                currentSelectedFields.add(field)
            }
            
            Log.d("EditFieldsFragment", "准备选中当前tab的 ${fieldsToSelect.size} 个字段，总共 ${currentSelectedFields.size} 个已选字段")
            
            // 批量更新字段选择状态
            baseViewModel.setSelectedFields(currentSelectedFields)
            
            // 延迟刷新UI，确保数据更新完成
            binding.root.post {
                try {
                    // 获取当前显示的Fragment并更新其UI
                    val currentFragment = childFragmentManager.findFragmentByTag("f$currentPosition")
                    if (currentFragment is FieldListFragment) {
                        // 重新获取当前tab的字段（已包含更新后的选中状态）
                        val updatedFields = when (currentTabName) {
                            "全部" -> getAllFields()
                            "基础信息" -> getBasicFields()
                            "购物管理" -> getShoppingFields()
                            "数字类" -> getNumberFields()
                            "日期类" -> getDateFields()
                            "库存管理" -> getInventoryFields()
                            "商业类" -> getCommercialFields()
                            "其他" -> getOtherFields()
                            else -> emptyList()
                        }
                        // 直接更新当前Fragment的字段列表
                        currentFragment.updateFields(updatedFields)
                        Log.d("EditFieldsFragment", "已更新当前Fragment的UI，字段数: ${updatedFields.size}")
                    }
                    
                    // 恢复按钮状态
                    binding.selectAllButton.isEnabled = true
                    binding.selectAllButton.text = "全选"
                    
                    // 显示完成提示
                    android.widget.Toast.makeText(
                        requireContext(), 
                        "已全选「$currentTabName」的 ${fieldsToSelect.size} 个字段", 
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    
                    Log.d("EditFieldsFragment", "全选当前tab完成，UI已刷新")
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