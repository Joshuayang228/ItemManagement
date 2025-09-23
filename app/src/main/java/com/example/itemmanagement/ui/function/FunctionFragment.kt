package com.example.itemmanagement.ui.function

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.adapter.FunctionAdapter
import com.example.itemmanagement.databinding.FragmentFunctionBinding

class FunctionFragment : Fragment() {

    private var _binding: FragmentFunctionBinding? = null
    private val binding get() = _binding!!
    private val functionAdapter = FunctionAdapter()
    
    private val viewModel: FunctionViewModel by viewModels {
        FunctionViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFunctionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = functionAdapter
        }
        
        functionAdapter.setOnFunctionClickListener { functionType ->
            // 处理功能卡片点击事件
            viewModel.handleFunctionClick(functionType)
        }
    }

    private fun observeData() {
        viewModel.functionGroupItems.observe(viewLifecycleOwner) { functionGroupItems ->
            functionAdapter.submitFunctionGroupItems(functionGroupItems)
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { actionId ->
            actionId?.let {
                findNavController().navigate(it)
                viewModel.clearNavigationEvent()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 