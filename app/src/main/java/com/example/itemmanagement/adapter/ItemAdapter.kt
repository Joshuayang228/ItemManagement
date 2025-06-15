package com.example.itemmanagement.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
            .inflate(R.layout.item_product, parent, false)
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
        private val dateText: TextView = itemView.findViewById(R.id.itemDate)
        private val quantityText: TextView = itemView.findViewById(R.id.itemQuantity)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(item: Item) {
            nameText.text = item.name
            dateText.text = dateFormat.format(item.addDate)
            quantityText.text = "${item.quantity}${item.unit?.let { " $it" } ?: ""}"

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

            // 设置删除按钮点击事件
            deleteButton.setOnClickListener {
                showDeleteConfirmationDialog(item)
            }

            // 加载图片
            if (item.photos.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(item.photos[0].uri)
                    .centerCrop()
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.ic_image_placeholder)
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