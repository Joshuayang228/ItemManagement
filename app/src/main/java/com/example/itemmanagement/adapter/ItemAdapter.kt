package com.example.itemmanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.Item
import java.text.SimpleDateFormat
import java.util.Locale

class ItemAdapter : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private var items: List<Item> = emptyList()

    fun submitList(newItems: List<Item>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.itemImage)
        private val nameText: TextView = itemView.findViewById(R.id.itemName)
        private val dateText: TextView = itemView.findViewById(R.id.itemDate)
        private val quantityText: TextView = itemView.findViewById(R.id.itemQuantity)
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(item: Item) {
            nameText.text = item.name
            dateText.text = dateFormat.format(item.addDate)
            quantityText.text = "数量: ${item.quantity}"

            Glide.with(itemView.context)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .centerCrop()
                .into(imageView)
        }
    }
}