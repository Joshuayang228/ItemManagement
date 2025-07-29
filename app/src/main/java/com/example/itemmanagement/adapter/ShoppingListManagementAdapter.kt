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
import com.example.itemmanagement.data.entity.ShoppingListEntity
import com.example.itemmanagement.data.entity.ShoppingListStatus
import com.example.itemmanagement.data.entity.ShoppingListType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 购物清单管理适配器
 * 显示购物清单列表，支持点击查看详情、编辑、删除等操作
 */
class ShoppingListManagementAdapter(
    private val onItemClick: (ShoppingListEntity) -> Unit,
    private val onEditClick: (ShoppingListEntity) -> Unit,
    private val onDeleteClick: (ShoppingListEntity) -> Unit,
    private val onCompleteClick: (ShoppingListEntity) -> Unit
) : ListAdapter<ShoppingListEntity, ShoppingListManagementAdapter.ShoppingListViewHolder>(ShoppingListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list_management, parent, false)
        return ShoppingListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ShoppingListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val listName: TextView = itemView.findViewById(R.id.tvListName)
        private val listDescription: TextView = itemView.findViewById(R.id.tvListDescription)
        private val listType: TextView = itemView.findViewById(R.id.tvListType)
        private val listStatus: TextView = itemView.findViewById(R.id.tvListStatus)
        private val listDate: TextView = itemView.findViewById(R.id.tvListDate)
        private val listBudget: TextView = itemView.findViewById(R.id.tvListBudget)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val btnComplete: ImageButton = itemView.findViewById(R.id.btnComplete)

        fun bind(shoppingList: ShoppingListEntity) {
            // 设置基本信息
            listName.text = shoppingList.name
            
            // 设置描述
            if (shoppingList.description.isNullOrBlank()) {
                listDescription.visibility = View.GONE
            } else {
                listDescription.visibility = View.VISIBLE
                listDescription.text = shoppingList.description
            }
            
            // 设置类型
            listType.text = getTypeDisplayName(shoppingList.type)
            
            // 设置状态
            listStatus.text = getStatusDisplayName(shoppingList.status)
            listStatus.setTextColor(getStatusColor(shoppingList.status))
            
            // 设置创建日期
            val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
            listDate.text = "创建于 ${dateFormat.format(shoppingList.createdDate)}"
            
            // 设置预算
            if (shoppingList.estimatedBudget != null && shoppingList.estimatedBudget > 0) {
                listBudget.visibility = View.VISIBLE
                listBudget.text = "预算: ¥${String.format("%.2f", shoppingList.estimatedBudget)}"
            } else {
                listBudget.visibility = View.GONE
            }
            
            // 设置按钮状态
            setupButtons(shoppingList)
            
            // 设置点击事件
            itemView.setOnClickListener { onItemClick(shoppingList) }
            btnEdit.setOnClickListener { onEditClick(shoppingList) }
            btnDelete.setOnClickListener { onDeleteClick(shoppingList) }
            btnComplete.setOnClickListener { onCompleteClick(shoppingList) }
        }
        
        private fun setupButtons(shoppingList: ShoppingListEntity) {
            when (shoppingList.status) {
                ShoppingListStatus.ACTIVE -> {
                    btnComplete.visibility = View.VISIBLE
                    btnComplete.setImageResource(R.drawable.ic_save)
                    btnEdit.visibility = View.VISIBLE
                    btnDelete.visibility = View.VISIBLE
                }
                ShoppingListStatus.COMPLETED -> {
                    btnComplete.visibility = View.VISIBLE
                    btnComplete.setImageResource(R.drawable.ic_arrow_back_24)
                    btnEdit.visibility = View.GONE
                    btnDelete.visibility = View.VISIBLE
                }
                ShoppingListStatus.ARCHIVED -> {
                    btnComplete.visibility = View.GONE
                    btnEdit.visibility = View.GONE
                    btnDelete.visibility = View.VISIBLE
                }
            }
        }
        
        private fun getTypeDisplayName(type: ShoppingListType): String {
            return when (type) {
                ShoppingListType.DAILY -> "日常补充"
                ShoppingListType.WEEKLY -> "周采购"
                ShoppingListType.PARTY -> "聚会准备"
                ShoppingListType.TRAVEL -> "旅行购物"
                ShoppingListType.SPECIAL -> "特殊场合"
                ShoppingListType.CUSTOM -> "自定义"
            }
        }
        
        private fun getStatusDisplayName(status: ShoppingListStatus): String {
            return when (status) {
                ShoppingListStatus.ACTIVE -> "进行中"
                ShoppingListStatus.COMPLETED -> "已完成"
                ShoppingListStatus.ARCHIVED -> "已归档"
            }
        }
        
        private fun getStatusColor(status: ShoppingListStatus): Int {
            return when (status) {
                ShoppingListStatus.ACTIVE -> itemView.context.getColor(R.color.primary)
                ShoppingListStatus.COMPLETED -> itemView.context.getColor(R.color.accent)
                ShoppingListStatus.ARCHIVED -> itemView.context.getColor(R.color.text_secondary)
            }
        }
    }

    class ShoppingListDiffCallback : DiffUtil.ItemCallback<ShoppingListEntity>() {
        override fun areItemsTheSame(oldItem: ShoppingListEntity, newItem: ShoppingListEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ShoppingListEntity, newItem: ShoppingListEntity): Boolean {
            return oldItem == newItem
        }
    }
} 