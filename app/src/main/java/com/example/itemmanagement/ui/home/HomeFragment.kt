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
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.Location
import com.example.itemmanagement.data.model.OpenStatus
import com.example.itemmanagement.data.model.ItemStatus
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

        // 设置物品点击事件
        itemAdapter.setOnItemClickListener { item ->
            val action = HomeFragmentDirections.actionNavigationHomeToAddItemFragment(item)
            findNavController().navigate(action)
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
                id = 1L,
                name = "MacBook Pro 14",
                quantity = 5.0,
                unit = "台",
                location = Location(1, "书房", "桌面", null),
                category = "电子设备",
                productionDate = null,
                expirationDate = null,
                openStatus = OpenStatus.UNOPENED,
                openDate = null,
                brand = "Apple",
                specification = "M2 Pro",
                status = ItemStatus.IN_STOCK,
                stockWarningThreshold = 1,
                price = 14999.0,
                purchaseChannel = "Apple Store",
                storeName = "Apple 官方旗舰店",
                subCategory = "笔记本电脑",
                customNote = "工作用",
                season = null,
                capacity = null,
                rating = 5.0,
                totalPrice = 14999.0,
                purchaseDate = Date(),
                shelfLife = null,
                warrantyPeriod = 365,
                warrantyEndDate = Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L),
                serialNumber = "FVFXC2023ABCD",
                photos = emptyList(),
                tags = emptyList()
            ),
            Item(
                id = 2L,
                name = "iPhone 15 Pro",
                quantity = 2.0,
                unit = "部",
                location = Location(2, "卧室", "抽屉", null),
                category = "电子设备",
                productionDate = null,
                expirationDate = null,
                openStatus = OpenStatus.UNOPENED,
                openDate = null,
                brand = "Apple",
                specification = "256GB",
                status = ItemStatus.IN_STOCK,
                stockWarningThreshold = 1,
                price = 8999.0,
                purchaseChannel = "京东",
                storeName = "Apple 京东自营旗舰店",
                subCategory = "手机",
                customNote = null,
                season = null,
                capacity = 256.0,
                rating = 4.5,
                totalPrice = 8999.0 * 2,
                purchaseDate = Date(),
                shelfLife = null,
                warrantyPeriod = 365,
                warrantyEndDate = Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L),
                serialNumber = "IP15PRO2023XYZ",
                photos = emptyList(),
                tags = emptyList()
            ),
            Item(
                id = 3L,
                name = "iPad Pro 12.9",
                quantity = 1.0,
                unit = "台",
                location = Location(3, "客厅", "茶几", null),
                category = "电子设备",
                productionDate = null,
                expirationDate = null,
                openStatus = OpenStatus.UNOPENED,
                openDate = null,
                brand = "Apple",
                specification = "M2 1TB",
                status = ItemStatus.IN_STOCK,
                stockWarningThreshold = 1,
                price = 12999.0,
                purchaseChannel = "天猫",
                storeName = "Apple 天猫旗舰店",
                subCategory = "平板电脑",
                customNote = "画图用",
                season = null,
                capacity = 1024.0,
                rating = 4.8,
                totalPrice = 12999.0,
                purchaseDate = Date(),
                shelfLife = null,
                warrantyPeriod = 365,
                warrantyEndDate = Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L),
                serialNumber = "IPADPRO2023ABC",
                photos = emptyList(),
                tags = emptyList()
            )
        )
        itemAdapter.submitList(testItems)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}