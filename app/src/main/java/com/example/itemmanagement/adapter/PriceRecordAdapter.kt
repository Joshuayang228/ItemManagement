package com.example.itemmanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.PriceRecord
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 价格记录 Adapter
 */
class PriceRecordAdapter(
    private val onItemClick: ((PriceRecord) -> Unit)? = null,
    private val onDeleteClick: ((PriceRecord) -> Unit)? = null
) : ListAdapter<PriceRecord, PriceRecordAdapter.ViewHolder>(DiffCallback) {

    // 总记录数（用于序号计算）
    private var totalRecordCount: Int = 0

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PriceRecord>() {
            override fun areItemsTheSame(oldItem: PriceRecord, newItem: PriceRecord): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PriceRecord, newItem: PriceRecord): Boolean {
                return oldItem == newItem
            }
        }
    }

    /**
     * 提交列表并设置总记录数
     */
    fun submitList(list: List<PriceRecord>?, totalCount: Int) {
        totalRecordCount = totalCount
        submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_price_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recordNumber: TextView = itemView.findViewById(R.id.recordNumber)
        private val recordDate: TextView = itemView.findViewById(R.id.recordDate)
        private val recordChannel: TextView = itemView.findViewById(R.id.recordChannel)
        private val recordPrice: TextView = itemView.findViewById(R.id.recordPrice)
        private val recordNotes: TextView = itemView.findViewById(R.id.recordNotes)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(record: PriceRecord, position: Int) {
            // 序号（倒序显示，最新的序号最大）
            recordNumber.text = "${totalRecordCount - position}."
            
            recordDate.text = dateFormat.format(record.recordDate)
            recordChannel.text = record.purchaseChannel
            recordPrice.text = "¥${record.price.toInt()}"
            
            // 备注
            if (!record.notes.isNullOrBlank()) {
                recordNotes.text = record.notes
                recordNotes.visibility = View.VISIBLE
            } else {
                recordNotes.visibility = View.GONE
            }

            // 点击事件
            itemView.setOnClickListener {
                onItemClick?.invoke(record)
            }

            // 删除按钮
            if (onDeleteClick != null) {
                btnDelete.visibility = View.VISIBLE
                btnDelete.setOnClickListener {
                    onDeleteClick.invoke(record)
                }
            } else {
                btnDelete.visibility = View.GONE
            }
        }
    }
}

/**
 * 各平台价格 Adapter
 */
class ChannelPriceAdapter : ListAdapter<PriceRecord, ChannelPriceAdapter.ViewHolder>(DiffCallback) {

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PriceRecord>() {
            override fun areItemsTheSame(oldItem: PriceRecord, newItem: PriceRecord): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PriceRecord, newItem: PriceRecord): Boolean {
                return oldItem == newItem
            }
        }
    }

    private var minPrice: Double = 0.0

    fun setMinPrice(price: Double) {
        minPrice = price
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel_price, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val channelName: TextView = itemView.findViewById(R.id.channelName)
        private val channelPrice: TextView = itemView.findViewById(R.id.channelPrice)
        private val channelDate: TextView = itemView.findViewById(R.id.channelDate)
        private val lowestBadge: TextView = itemView.findViewById(R.id.lowestBadge)

        private val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())

        fun bind(record: PriceRecord) {
            channelName.text = record.purchaseChannel
            channelPrice.text = "¥${record.price.toInt()}"
            channelDate.text = dateFormat.format(record.recordDate)

            // 如果是最低价，显示徽章
            if (record.price == minPrice) {
                lowestBadge.visibility = View.VISIBLE
            } else {
                lowestBadge.visibility = View.GONE
            }
        }
    }
}

