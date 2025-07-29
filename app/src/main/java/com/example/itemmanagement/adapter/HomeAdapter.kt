package com.example.itemmanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.databinding.ItemHomeBinding
import com.example.itemmanagement.databinding.ItemHomeFunctionHeaderBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.Locale

class HomeAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    private var items: List<Item> = emptyList()
    private var onItemClickListener: ((Item) -> Unit)? = null
    private var onDeleteClickListener: ((Item) -> Unit)? = null
    private var onFunctionClickListener: ((String) -> Unit)? = null

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Item) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnDeleteClickListener(listener: (Item) -> Unit) {
        onDeleteClickListener = listener
    }

    fun setOnFunctionClickListener(listener: (String) -> Unit) {
        onFunctionClickListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_ITEM
    }

    override fun getItemCount(): Int = items.size + 1 // +1 for header

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemHomeFunctionHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                FunctionHeaderViewHolder(binding, onFunctionClickListener)
            }
            TYPE_ITEM -> {
                val binding = ItemHomeBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ItemViewHolder(binding, onItemClickListener, onDeleteClickListener)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FunctionHeaderViewHolder -> {
                // Header不需要绑定数据
            }
            is ItemViewHolder -> {
                holder.bind(items[position - 1]) // -1 because of header
            }
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        // 让header占据整行
        if (holder is FunctionHeaderViewHolder) {
            val lp = holder.itemView.layoutParams
            if (lp is StaggeredGridLayoutManager.LayoutParams) {
                lp.isFullSpan = true
            }
        }
    }

    class FunctionHeaderViewHolder(
        private val binding: ItemHomeFunctionHeaderBinding,
        private val onFunctionClickListener: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.expiringItemsCard.setOnClickListener {
                onFunctionClickListener?.invoke("expiring")
            }
            binding.expiredItemsCard.setOnClickListener {
                onFunctionClickListener?.invoke("expired")
            }
            binding.lowStockCard.setOnClickListener {
                onFunctionClickListener?.invoke("low_stock")
            }
            binding.shoppingListCard.setOnClickListener {
                onFunctionClickListener?.invoke("shopping_list")
            }
        }
    }

    class ItemViewHolder(
        private val binding: ItemHomeBinding,
        private val onItemClickListener: ((Item) -> Unit)?,
        private val onDeleteClickListener: ((Item) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(item: Item) {
            // 设置名称（必须显示）
            binding.itemName.text = item.name
            
            // 设置单价和日期行
            var showPriceAndDateLayout = false
            
            // 设置单价（如果有）
            if (item.price != null) {
                binding.itemPrice.text = "¥${item.price}"
                binding.itemPrice.visibility = View.VISIBLE
                showPriceAndDateLayout = true
            } else {
                binding.itemPrice.visibility = View.GONE
            }
            
            // 设置添加日期
            binding.itemDate.text = dateFormat.format(item.addDate)
            showPriceAndDateLayout = true
            
            // 控制整行的可见性
            binding.priceAndDateLayout.visibility = if (showPriceAndDateLayout) View.VISIBLE else View.GONE
            
            // 设置备注（如果有）
            if (!item.customNote.isNullOrEmpty()) {
                binding.itemNote.text = item.customNote
                binding.itemNote.visibility = View.VISIBLE
            } else {
                binding.itemNote.visibility = View.GONE
            }

            // 设置点击事件
            itemView.setOnClickListener {
                onItemClickListener?.invoke(item)
            }

            // 设置长按事件
            itemView.setOnLongClickListener {
                onItemClickListener?.invoke(item)
                true
            }

            // 加载图片
            if (item.photos.isNotEmpty()) {
                val requestOptions = RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .fitCenter()
                
                Glide.with(itemView.context)
                    .load(item.photos[0].uri)
                    .apply(requestOptions)
                    .into(binding.itemImage)
            } else {
                binding.itemImage.setImageResource(R.drawable.ic_image_placeholder)
                // 为空图片设置一个默认高度
                binding.itemImage.layoutParams.height = 
                    itemView.resources.getDimensionPixelSize(R.dimen.default_image_height)
                binding.itemImage.requestLayout()
            }
        }
    }
} 