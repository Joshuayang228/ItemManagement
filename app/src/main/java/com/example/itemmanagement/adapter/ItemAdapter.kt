package com.example.itemmanagement.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.Item
import java.text.SimpleDateFormat
import java.util.Locale

class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: List<Item> = emptyList()
    private var onItemClickListener: ((Item) -> Unit)? = null
    private var onDeleteClickListener: ((Item) -> Unit)? = null

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home, parent, false)
        return ItemViewHolder(view, onItemClickListener, onDeleteClickListener)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ItemViewHolder(
        itemView: View,
        private val onItemClickListener: ((Item) -> Unit)?,
        private val onDeleteClickListener: ((Item) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.itemImage)
        private val nameText: TextView = itemView.findViewById(R.id.itemName)
        private val priceAndDateLayout: LinearLayout = itemView.findViewById(R.id.priceAndDateLayout)
        private val priceText: TextView = itemView.findViewById(R.id.itemPrice)
        private val dateText: TextView = itemView.findViewById(R.id.itemDate)
        private val noteText: TextView = itemView.findViewById(R.id.itemNote)
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(item: Item) {
            // 设置名称（必须显示）
            nameText.text = item.name
            
            // 设置单价和日期行
            var showPriceAndDateLayout = false
            
            // 设置单价（如果有）
            if (item.price != null) {
                priceText.text = "¥${item.price}"
                priceText.visibility = View.VISIBLE
                showPriceAndDateLayout = true
            } else {
                priceText.visibility = View.GONE
            }
            
            // 设置添加日期
            dateText.text = dateFormat.format(item.addDate)
            showPriceAndDateLayout = true
            
            // 控制整行的可见性
            priceAndDateLayout.visibility = if (showPriceAndDateLayout) View.VISIBLE else View.GONE
            
            // 设置备注（如果有）
            if (!item.customNote.isNullOrEmpty()) {
                noteText.text = item.customNote
                noteText.visibility = View.VISIBLE
            } else {
                noteText.visibility = View.GONE
            }

            // 设置点击事件 - 导航到详情页面
            itemView.setOnClickListener {
                val bundle = androidx.core.os.bundleOf("itemId" to item.id)
                itemView.findNavController().navigate(R.id.navigation_item_detail, bundle)
            }

            // 设置长按事件 - 导航到编辑页面
            itemView.setOnLongClickListener {
                onItemClickListener?.invoke(item)
                true
            }

            // 加载图片 - 使用适合瀑布流的加载方式
            if (item.photos.isNotEmpty()) {
                val requestOptions = RequestOptions()
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_error)
                    .fitCenter()
                
                Glide.with(itemView.context)
                    .load(item.photos[0].uri)
                    .apply(requestOptions)
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_image_placeholder)
                // 为空图片设置一个默认高度
                imageView.layoutParams.height = itemView.resources.getDimensionPixelSize(R.dimen.default_image_height)
                imageView.requestLayout()
            }
        }

        private fun showDeleteConfirmationDialog(item: Item) {
            AlertDialog.Builder(itemView.context)
                .setTitle("确认删除")
                .setMessage("确定要删除「${item.name}」吗？")
                .setPositiveButton("删除") { _, _ ->
                    onDeleteClickListener?.invoke(item)
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }
}