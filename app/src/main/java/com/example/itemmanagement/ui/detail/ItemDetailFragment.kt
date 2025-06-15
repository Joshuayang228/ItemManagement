package com.example.itemmanagement.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentItemDetailBinding
import com.example.itemmanagement.ui.detail.adapter.PhotoAdapter
import com.example.itemmanagement.ui.detail.adapter.TagAdapter
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Locale

class ItemDetailFragment : Fragment() {
    private var _binding: FragmentItemDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemDetailViewModel by viewModels {
        ItemDetailViewModelFactory(
            (requireActivity().application as ItemManagementApplication).repository
        )
    }

    private val args: ItemDetailFragmentArgs by navArgs()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var tagAdapter: TagAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupTagsRecyclerView()
        viewModel.loadItem(args.itemId)
        observeItem()
        observeError()
    }

    private fun setupViewPager() {
        // 设置照片ViewPager
        photoAdapter = PhotoAdapter()
        binding.photoViewPager.adapter = photoAdapter
    }
    
    private fun setupTagsRecyclerView() {
        // 设置标签RecyclerView
        tagAdapter = TagAdapter()
        binding.tagsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = tagAdapter
        }
    }

    private fun observeItem() {
        viewModel.item.observe(viewLifecycleOwner) { item ->
            binding.apply {
                // 基本信息
                nameTextView.text = item.name
                quantityTextView.text = getString(R.string.quantity_format, item.quantity, item.unit)
                categoryTextView.text = getString(R.string.category_format, item.category)
                // 子分类信息
                if (item.subCategory != null) {
                    subCategoryTextView.text = getString(R.string.subcategory_format, item.subCategory)
                    subCategoryTextView.visibility = View.VISIBLE
                } else {
                    subCategoryTextView.visibility = View.GONE
                }

                // 位置信息
                item.location?.let {
                    locationTextView.text = getString(R.string.location_format, it.getFullLocationString())
                    locationTextView.visibility = View.VISIBLE
                } ?: run {
                    locationTextView.visibility = View.GONE
                }

                // 时间信息
                addDateTextView.text = getString(R.string.add_date_format, dateFormat.format(item.addDate))
                item.productionDate?.let {
                    productionDateTextView.text = getString(R.string.production_date_format, dateFormat.format(it))
                    productionDateTextView.visibility = View.VISIBLE
                } ?: run {
                    productionDateTextView.visibility = View.GONE
                }
                item.expirationDate?.let {
                    expirationDateTextView.text = getString(R.string.expiration_date_format, dateFormat.format(it))
                    expirationDateTextView.visibility = View.VISIBLE
                } ?: run {
                    expirationDateTextView.visibility = View.GONE
                }

                // 开封状态
                openStatusTextView.text = getString(R.string.open_status_format, 
                    if (item.openStatus.toString() == "OPENED") "已开封" else "未开封")
                item.openDate?.let {
                    openDateTextView.text = getString(R.string.open_date_format, dateFormat.format(it))
                    openDateTextView.visibility = View.VISIBLE
                } ?: run {
                    openDateTextView.visibility = View.GONE
                }

                // 商品信息
                item.brand?.let {
                    brandTextView.text = getString(R.string.brand_format, it)
                    brandTextView.visibility = View.VISIBLE
                } ?: run {
                    brandTextView.visibility = View.GONE
                }
                item.specification?.let {
                    specificationTextView.text = getString(R.string.specification_format, it)
                    specificationTextView.visibility = View.VISIBLE
                } ?: run {
                    specificationTextView.visibility = View.GONE
                }

                // 库存状态
                statusTextView.text = getString(R.string.status_format, item.status.name)
                item.stockWarningThreshold?.let {
                    stockWarningThresholdTextView.text = getString(R.string.stock_warning_threshold_format, it)
                    stockWarningThresholdTextView.visibility = View.VISIBLE
                } ?: run {
                    stockWarningThresholdTextView.visibility = View.GONE
                }

                // 购买信息
                item.price?.let {
                    priceTextView.text = getString(R.string.price_format, it)
                    priceTextView.visibility = View.VISIBLE
                } ?: run {
                    priceTextView.visibility = View.GONE
                }
                item.purchaseChannel?.let {
                    purchaseChannelTextView.text = getString(R.string.purchase_channel_format, it)
                    purchaseChannelTextView.visibility = View.VISIBLE
                } ?: run {
                    purchaseChannelTextView.visibility = View.GONE
                }
                item.storeName?.let {
                    storeNameTextView.text = getString(R.string.store_name_format, it)
                    storeNameTextView.visibility = View.VISIBLE
                } ?: run {
                    storeNameTextView.visibility = View.GONE
                }

                // 备注
                item.customNote?.let {
                    customNoteTextView.text = getString(R.string.custom_note_format, it)
                    customNoteTextView.visibility = View.VISIBLE
                } ?: run {
                    customNoteTextView.visibility = View.GONE
                }

                // 照片
                if (item.photos.isNotEmpty()) {
                    photoAdapter.submitList(item.photos)
                    photoViewPager.visibility = View.VISIBLE
                } else {
                    photoViewPager.visibility = View.GONE
                }

                // 标签
                if (item.tags.isNotEmpty()) {
                    tagAdapter.submitList(item.tags)
                    tagsRecyclerView.visibility = View.VISIBLE
                    tagsLabel.visibility = View.VISIBLE
                } else {
                    tagsRecyclerView.visibility = View.GONE
                    tagsLabel.visibility = View.GONE
                }
            }
        }
    }

    private fun observeError() {
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 