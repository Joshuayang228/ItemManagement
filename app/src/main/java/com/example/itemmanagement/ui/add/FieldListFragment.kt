package com.example.itemmanagement.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.databinding.FragmentFieldListBinding

/**
 * 新架构的字段列表Fragment
 * 保持与原版完全相同的UI和功能，但不依赖旧的AddItemViewModel
 */
class FieldListFragment : Fragment() {
    private var _binding: FragmentFieldListBinding? = null
    private val binding get() = _binding!!
    private var fields: List<Field> = emptyList()
    private var onFieldSelected: ((Field, Boolean) -> Unit)? = null
    private var adapter: FieldsAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFieldListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = FieldsAdapter(fields) { field, isSelected ->
            onFieldSelected?.invoke(field, isSelected)
            // 更新本地字段状态以保持UI同步
            fields = fields.map { 
                if (it.name == field.name && it.group == field.group) {
                    it.copy(isSelected = isSelected)
                } else {
                    it
                }
            }
            adapter?.updateFields(fields)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@FieldListFragment.adapter
        }
    }

    /**
     * 更新字段列表（从外部调用）
     */
    fun updateFields(newFields: List<Field>) {
        fields = newFields
        adapter?.updateFields(fields)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adapter = null
        _binding = null
    }

    companion object {
        fun newInstance(
            fields: List<Field>,
            onFieldSelected: ((Field, Boolean) -> Unit)? = null
        ) = FieldListFragment().apply {
            this.fields = fields
            this.onFieldSelected = onFieldSelected
        }
    }
}