package com.example.itemmanagement.ui.add

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.databinding.FragmentEditFieldsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayoutMediator

class EditFieldsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEditFieldsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AddItemViewModel
    private val tabs = listOf("全部", "基础信息", "数字类", "日期类", "状态类", "分类", "商业类", "其他")
    private var currentAdapter: FieldsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        
        // 初始化ViewModel
        val repository = (requireActivity().application as ItemManagementApplication).repository
        val factory = AddItemViewModelFactory(repository, requireActivity())
        viewModel = ViewModelProvider(requireActivity(), factory)[AddItemViewModel::class.java]
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

        setupViews()
    }

    private fun setupViews() {
        binding.closeButton.setOnClickListener {
            dismiss()
        }

        setupViewPager()
        setupTabs()
    }

    private fun setupViewPager() {
        currentAdapter = FieldsPagerAdapter()
        binding.viewPager.adapter = currentAdapter

        // 添加页面切换监听器
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 页面切换时不需要特殊处理，因为每个Fragment都会观察ViewModel的变化
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
                "数字类" -> getNumberFields()
                "日期类" -> getDateFields()
                "状态类" -> getStatusFields()
                "分类" -> getCategoryFields()
                "商业类" -> getCommercialFields()
                "其他" -> getOtherFields()
                else -> emptyList()
            }

            return FieldListFragment.newInstance(fields) { field, isSelected ->
                viewModel.updateFieldSelection(field, isSelected)
            }
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun containsItem(itemId: Long): Boolean {
            return itemId in 0 until itemCount
        }
    }

    private fun getAllFields(): List<Field> {
        val selectedFields = viewModel.getSelectedFieldsValue()
        return listOf(
            Field("基础信息", "名称", selectedFields.any { it.name == "名称" }),
            Field("基础信息", "数量", selectedFields.any { it.name == "数量" }),
            Field("基础信息", "位置", selectedFields.any { it.name == "位置" }),
            Field("数字类", "单价", selectedFields.any { it.name == "单价" }),
            Field("数字类", "总价", selectedFields.any { it.name == "总价" }),
            Field("数字类", "容量", selectedFields.any { it.name == "容量" }),
            Field("数字类", "评分", selectedFields.any { it.name == "评分" }),
            Field("日期类", "添加日期", selectedFields.any { it.name == "添加日期" }),
            Field("日期类", "购买日期", selectedFields.any { it.name == "购买日期" }),
            Field("日期类", "生产日期", selectedFields.any { it.name == "生产日期" }),
            Field("日期类", "保修期", selectedFields.any { it.name == "保修期" }),
            Field("日期类", "保修到期时间", selectedFields.any { it.name == "保修到期时间" }),
            Field("日期类", "保质期", selectedFields.any { it.name == "保质期" }),
            Field("日期类", "保质过期时间", selectedFields.any { it.name == "保质过期时间" }),
            Field("日期类", "开封时间", selectedFields.any { it.name == "开封时间" }),
            Field("状态类", "开封状态", selectedFields.any { it.name == "开封状态" }),
            Field("分类", "分类", selectedFields.any { it.name == "分类" }),
            Field("分类", "子分类", selectedFields.any { it.name == "子分类" }),
            Field("分类", "标签", selectedFields.any { it.name == "标签" }),
            Field("分类", "季节", selectedFields.any { it.name == "季节" }),
            Field("商业类", "购买渠道", selectedFields.any { it.name == "购买渠道" }),
            Field("商业类", "商家名称", selectedFields.any { it.name == "商家名称" }),
            Field("商业类", "品牌", selectedFields.any { it.name == "品牌" }),
            Field("商业类", "序列号", selectedFields.any { it.name == "序列号" }),
            Field("其他", "备注", selectedFields.any { it.name == "备注" })
        )
    }

    private fun getBasicFields(): List<Field> {
        val selectedFields = viewModel.getSelectedFieldsValue()
        return listOf(
            Field("基础信息", "名称", selectedFields.any { it.name == "名称" }),
            Field("基础信息", "数量", selectedFields.any { it.name == "数量" }),
            Field("基础信息", "位置", selectedFields.any { it.name == "位置" })
        )
    }

    private fun getNumberFields(): List<Field> {
        val selectedFields = viewModel.getSelectedFieldsValue()
        return listOf(
            Field("数字类", "单价", selectedFields.any { it.name == "单价" }),
            Field("数字类", "总价", selectedFields.any { it.name == "总价" }),
            Field("数字类", "容量", selectedFields.any { it.name == "容量" }),
            Field("数字类", "评分", selectedFields.any { it.name == "评分" }),
            Field("数字类", "数量", selectedFields.any { it.name == "数量" })
        )
    }

    private fun getDateFields(): List<Field> {
        val selectedFields = viewModel.getSelectedFieldsValue()
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
        val selectedFields = viewModel.getSelectedFieldsValue()
        return listOf(
            Field("状态类", "开封状态", selectedFields.any { it.name == "开封状态" })
        )
    }

    private fun getCategoryFields(): List<Field> {
        val selectedFields = viewModel.getSelectedFieldsValue()
        return listOf(
            Field("分类", "分类", selectedFields.any { it.name == "分类" }),
            Field("分类", "子分类", selectedFields.any { it.name == "子分类" }),
            Field("分类", "标签", selectedFields.any { it.name == "标签" }),
            Field("分类", "季节", selectedFields.any { it.name == "季节" })
        )
    }

    private fun getCommercialFields(): List<Field> {
        val selectedFields = viewModel.getSelectedFieldsValue()
        return listOf(
            Field("商业类", "单价", selectedFields.any { it.name == "单价" }),
            Field("商业类", "购买渠道", selectedFields.any { it.name == "购买渠道" }),
            Field("商业类", "商家名称", selectedFields.any { it.name == "商家名称" }),
            Field("商业类", "品牌", selectedFields.any { it.name == "品牌" }),
            Field("商业类", "序列号", selectedFields.any { it.name == "序列号" })
        )
    }

    private fun getOtherFields(): List<Field> {
        val selectedFields = viewModel.getSelectedFieldsValue()
        return listOf(
            Field("其他", "备注", selectedFields.any { it.name == "备注" })
        )
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager.unregisterOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {})
        currentAdapter = null
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        fun newInstance() = EditFieldsFragment()
    }
}