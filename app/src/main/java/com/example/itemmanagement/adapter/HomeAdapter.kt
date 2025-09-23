package com.example.itemmanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.ui.home.HomeViewModel
import com.example.itemmanagement.databinding.ItemHomeBinding
import com.example.itemmanagement.databinding.ItemHomeFunctionHeaderBinding
import com.example.itemmanagement.databinding.ItemLoadingFooterBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 列表项数据类型
 */
sealed class HomeListItem {
    object Header : HomeListItem()
    data class ItemData(val displayItem: HomeViewModel.HomeDisplayItem) : HomeListItem()
    object LoadingFooter : HomeListItem()
}

/**
 * DiffUtil回调，用于高效的列表更新和动画
 */
class HomeDiffCallback : DiffUtil.ItemCallback<HomeListItem>() {
    override fun areItemsTheSame(oldItem: HomeListItem, newItem: HomeListItem): Boolean {
        return when {
            oldItem is HomeListItem.Header && newItem is HomeListItem.Header -> true
            oldItem is HomeListItem.ItemData && newItem is HomeListItem.ItemData -> 
                oldItem.displayItem.item.id == newItem.displayItem.item.id
            oldItem is HomeListItem.LoadingFooter && newItem is HomeListItem.LoadingFooter -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: HomeListItem, newItem: HomeListItem): Boolean {
        return when {
            oldItem is HomeListItem.Header && newItem is HomeListItem.Header -> true
            oldItem is HomeListItem.ItemData && newItem is HomeListItem.ItemData -> {
                oldItem.displayItem.item == newItem.displayItem.item &&
                oldItem.displayItem.showReason == newItem.displayItem.showReason &&
                oldItem.displayItem.reasonText == newItem.displayItem.reasonText
            }
            oldItem is HomeListItem.LoadingFooter && newItem is HomeListItem.LoadingFooter -> true
            else -> false
        }
    }
}

class HomeAdapter : ListAdapter<HomeListItem, RecyclerView.ViewHolder>(HomeDiffCallback()) {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
        const val TYPE_LOADING_FOOTER = 2
    }

    private var onItemClickListener: ((Item) -> Unit)? = null
    private var onDeleteClickListener: ((Item) -> Unit)? = null
    private var onFunctionClickListener: ((String) -> Unit)? = null

    /**
     * 提交新的展示物品列表
     */
    fun submitDisplayItems(newItems: List<HomeViewModel.HomeDisplayItem>, showLoading: Boolean = false) {
        val listItems = mutableListOf<HomeListItem>()
        listItems.add(HomeListItem.Header)
        listItems.addAll(newItems.map { HomeListItem.ItemData(it) })
        
        // 如果需要显示加载状态，添加footer
        if (showLoading) {
            listItems.add(HomeListItem.LoadingFooter)
        }
        
        submitList(listItems)
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
        return when (getItem(position)) {
            is HomeListItem.Header -> TYPE_HEADER
            is HomeListItem.ItemData -> TYPE_ITEM
            is HomeListItem.LoadingFooter -> TYPE_LOADING_FOOTER
        }
    }

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
            TYPE_LOADING_FOOTER -> {
                val binding = ItemLoadingFooterBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                LoadingFooterViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HomeListItem.Header -> {
                // Header不需要绑定数据
            }
            is HomeListItem.ItemData -> {
                (holder as ItemViewHolder).bind(item.displayItem)
            }
            is HomeListItem.LoadingFooter -> {
                // LoadingFooter不需要绑定数据，显示固定的loading状态
            }
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        // 让header和loading footer占据整行
        if (holder is FunctionHeaderViewHolder || holder is LoadingFooterViewHolder) {
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

        fun bind(displayItem: HomeViewModel.HomeDisplayItem) {
            val item = displayItem.item
            
            // 设置名称（必须显示）
            binding.itemName.text = item.name
            
            // 设置单价和日期行
            // 设置单价（如果有）
            if (item.price != null) {
                binding.itemPrice.text = "¥${item.price}"
                binding.itemPrice.visibility = View.VISIBLE
            } else {
                binding.itemPrice.visibility = View.GONE
            }
            
            // 设置添加日期（总是显示）
            binding.itemDate.text = dateFormat.format(item.addDate)
            
            // 价格和日期行总是可见（至少有日期）
            binding.priceAndDateLayout.visibility = View.VISIBLE
            
            // 设置备注和推荐理由
            setupNoteAndReason(item, displayItem)

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
            loadItemImage(item)
        }
        
        /**
         * 设置备注和推荐理由
         */
        private fun setupNoteAndReason(item: Item, displayItem: HomeViewModel.HomeDisplayItem) {
            val hasNote = !item.customNote.isNullOrEmpty()
            val hasReason = displayItem.showReason && !displayItem.reasonText.isNullOrEmpty()
            
            when {
                hasNote && hasReason -> {
                    // 都有：显示备注 + 推荐理由
                    binding.itemNote.text = buildString {
                        append(item.customNote)
                        append("\n")
                        append("💡 ")
                        append(displayItem.reasonText)
                    }
                    binding.itemNote.visibility = View.VISIBLE
                }
                hasNote && !hasReason -> {
                    // 只有备注
                    binding.itemNote.text = item.customNote
                    binding.itemNote.visibility = View.VISIBLE
                }
                !hasNote && hasReason -> {
                    // 只有推荐理由
                    binding.itemNote.text = "💡 ${displayItem.reasonText}"
                    binding.itemNote.visibility = View.VISIBLE
                }
                else -> {
                    // 都没有
                    binding.itemNote.visibility = View.GONE
                }
            }
        }
        
        /**
         * 加载物品图片
         */
        private fun loadItemImage(item: Item) {
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

    /**
     * Loading Footer ViewHolder - 显示底部加载状态
     */
    class LoadingFooterViewHolder(
        private val binding: ItemLoadingFooterBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        // LoadingFooter不需要特殊的绑定逻辑，布局中已经包含了旋转的ProgressBar
    }
} 