package com.example.itemmanagement.ui.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.databinding.FragmentFieldListBinding

class FieldListFragment : Fragment() {
    private var _binding: FragmentFieldListBinding? = null
    private val binding get() = _binding!!
    private var fields: List<Field> = emptyList()
    private var onFieldSelected: ((Field, Boolean) -> Unit)? = null
    private val viewModel: AddItemViewModel by activityViewModels()
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
        observeSelectedFields()
    }

    private fun setupRecyclerView() {
        adapter = FieldsAdapter(fields) { field, isSelected ->
            onFieldSelected?.invoke(field, isSelected)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@FieldListFragment.adapter
        }
    }

    private fun observeSelectedFields() {
        viewModel.selectedFields.observe(viewLifecycleOwner) { selectedFields ->
            val updatedFields = fields.map { field ->
                field.copy(isSelected = selectedFields.any { it.name == field.name })
            }
            if (updatedFields != fields) {
                fields = updatedFields
                adapter?.updateFields(fields)
            }
        }
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