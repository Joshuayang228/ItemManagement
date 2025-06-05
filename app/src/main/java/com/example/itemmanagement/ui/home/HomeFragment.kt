package com.example.itemmanagement.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.ItemAdapter
import com.example.itemmanagement.data.Item
import com.example.itemmanagement.databinding.FragmentHomeBinding
import java.util.Date

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val itemAdapter = ItemAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        setupButtons()
        loadTestData()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = itemAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(v.text.toString())
                return@setOnEditorActionListener true
            }
            false
        }

        binding.searchIcon.setOnClickListener {
            performSearch(binding.searchEditText.text.toString())
        }
    }

    private fun setupButtons() {
        // 设置悬浮添加按钮点击事件
        binding.addButton.setOnClickListener {
            onAddButtonClick()
        }

        // 设置顶部添加按钮点击事件
        binding.topAddButton.setOnClickListener {
            onAddButtonClick()
        }
    }

    private fun onAddButtonClick() {
        // 导航到添加物品页面
        findNavController().navigate(R.id.action_navigation_home_to_addItemFragment)
    }

    private fun performSearch(query: String) {
        // TODO: 实现搜索功能
        Toast.makeText(context, "搜索: $query", Toast.LENGTH_SHORT).show()
    }

    private fun loadTestData() {
        val testItems = listOf(
            Item(
                1L,
                "MacBook Pro 14",
                "https://picsum.photos/300/200?random=1",
                5,
                Date()
            ),
            Item(
                2L,
                "iPhone 15 Pro",
                "https://picsum.photos/300/200?random=2",
                10,
                Date()
            ),
            Item(
                3L,
                "iPad Pro 12.9",
                "https://picsum.photos/300/200?random=3",
                8,
                Date()
            ),
            Item(
                4L,
                "AirPods Pro",
                "https://picsum.photos/300/200?random=4",
                15,
                Date()
            ),
            Item(
                5L,
                "Apple Watch Series 9",
                "https://picsum.photos/300/200?random=5",
                12,
                Date()
            ),
            Item(
                6L,
                "Magic Keyboard",
                "https://picsum.photos/300/200?random=6",
                20,
                Date()
            ),
            Item(
                7L,
                "Magic Mouse",
                "https://picsum.photos/300/200?random=7",
                25,
                Date()
            ),
            Item(
                8L,
                "HomePod mini",
                "https://picsum.photos/300/200?random=8",
                18,
                Date()
            ),
            Item(
                9L,
                "Apple TV 4K",
                "https://picsum.photos/300/200?random=9",
                7,
                Date()
            ),
            Item(
                10L,
                "Mac mini",
                "https://picsum.photos/300/200?random=10",
                3,
                Date()
            )
        )
        itemAdapter.submitList(testItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}