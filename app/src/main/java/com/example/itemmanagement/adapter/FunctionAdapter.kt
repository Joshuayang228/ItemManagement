package com.example.itemmanagement.adapter

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.FunctionCard
import com.example.itemmanagement.data.model.FunctionGroupItem
import com.example.itemmanagement.data.model.FunctionGroupRow
import com.example.itemmanagement.data.model.CustomSpacerItem
import com.example.itemmanagement.databinding.ItemFunctionGroupRowBinding
import com.example.itemmanagement.databinding.ItemFunctionCustomSpacerBinding

class FunctionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FUNCTION_ROW = 0
        private const val TYPE_SPACER = 1
    }

    private var functionGroupItems: List<FunctionGroupItem> = emptyList()
    private var onFunctionClickListener: ((String) -> Unit)? = null

    /**
     * 提交分组功能列表项
     */
    fun submitFunctionGroupItems(newItems: List<FunctionGroupItem>) {
        functionGroupItems = newItems
        notifyDataSetChanged()
    }

    fun setOnFunctionClickListener(listener: (String) -> Unit) {
        onFunctionClickListener = listener
    }

    override fun getItemViewType(position: Int): Int {
        return when (functionGroupItems[position]) {
            is FunctionGroupRow -> TYPE_FUNCTION_ROW
            is CustomSpacerItem -> TYPE_SPACER
        }
    }

    override fun getItemCount(): Int = functionGroupItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FUNCTION_ROW -> {
                val binding = ItemFunctionGroupRowBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                FunctionRowViewHolder(binding, onFunctionClickListener)
            }
            TYPE_SPACER -> {
                val binding = ItemFunctionCustomSpacerBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                CustomSpacerViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FunctionRowViewHolder -> {
                val functionRow = functionGroupItems[position] as FunctionGroupRow
                holder.bind(functionRow)
            }
            is CustomSpacerViewHolder -> {
                val spacerItem = functionGroupItems[position] as CustomSpacerItem
                holder.bind(spacerItem)
            }
        }
    }

    /**
     * 功能行 ViewHolder
     */
    class FunctionRowViewHolder(
        private val binding: ItemFunctionGroupRowBinding,
        private val onFunctionClickListener: ((String) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(functionRow: FunctionGroupRow) {
            val card = functionRow.functionCard
            
            binding.apply {
                functionTitle.text = card.title
                functionIcon.setImageResource(card.iconResId)

                // 根据功能类型设置图标颜色
                val iconColor = when (card.type) {
                    FunctionCard.Type.ANALYTICS, 
                    FunctionCard.Type.CALENDAR, 
                    FunctionCard.Type.WASTE_REPORT -> {
                        itemView.context.getColor(R.color.data_insights_accent)
                    }
                    FunctionCard.Type.REMINDER, 
                    FunctionCard.Type.WARRANTY -> {
                        itemView.context.getColor(R.color.smart_assistant_accent)
                    }
                    FunctionCard.Type.LENDING, 
                    FunctionCard.Type.BACKUP, 
                    FunctionCard.Type.UTILITY -> {
                        itemView.context.getColor(R.color.utilities_accent)
                    }
                }

                // 设置图标颜色
                functionIcon.setColorFilter(iconColor)

                // 不再设置卡片背景，使用原生系统式风格

                // 控制分割线显示
                divider.visibility = if (functionRow.showDivider) View.VISIBLE else View.GONE

                // 设置启用状态
                root.isEnabled = card.isEnabled
                root.alpha = if (card.isEnabled) 1.0f else 0.5f

                // 设置点击事件
                root.setOnClickListener {
                    if (card.isEnabled) {
                        // 添加触感反馈
                        root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onFunctionClickListener?.invoke(card.id)
                    }
                }
            }
        }
    }

    /**
     * 自定义间距 ViewHolder
     */
    class CustomSpacerViewHolder(
        private val binding: ItemFunctionCustomSpacerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(spacerItem: CustomSpacerItem) {
            // 动态设置间距高度
            val layoutParams = binding.root.layoutParams
            layoutParams.height = (spacerItem.height * binding.root.context.resources.displayMetrics.density).toInt()
            binding.root.layoutParams = layoutParams
        }
    }
}