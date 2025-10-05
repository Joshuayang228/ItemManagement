package com.example.itemmanagement.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.example.itemmanagement.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.example.itemmanagement.data.entity.ShoppingListEntity
import com.example.itemmanagement.data.entity.ShoppingListStatus
import com.example.itemmanagement.data.entity.ShoppingListType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 购物清单进度数据类
 */
data class ShoppingListProgress(
    val totalItems: Int,
    val completedItems: Int,
    val progress: Int
) {
    val progressText: String get() = if (totalItems > 0) "$completedItems/$totalItems" else "0/0"
}

/**
 * 购物清单管理适配器
 * 显示购物清单列表，支持点击查看详情、编辑、删除等操作
 */
class ShoppingListManagementAdapter(
    private val onItemClick: (ShoppingListEntity) -> Unit,
    private val onEditClick: (ShoppingListEntity) -> Unit,
    private val onDeleteClick: (ShoppingListEntity) -> Unit,
    private val onCompleteClick: (ShoppingListEntity) -> Unit,
    private val getProgressData: (Long) -> ShoppingListProgress
) : ListAdapter<ShoppingListEntity, ShoppingListManagementAdapter.ShoppingListViewHolder>(ShoppingListDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list_management_m3, parent, false)
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
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        private val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
        private val constraintLayout: ConstraintLayout = itemView.findViewById(R.id.constraintLayout)

        fun bind(shoppingList: ShoppingListEntity) {
            // 设置基本信息
            listName.text = shoppingList.name
            
            // 设置描述并调整标题对齐
            if (shoppingList.description.isNullOrBlank()) {
                listDescription.visibility = View.GONE
                // 无描述时，让标题垂直居中
                adjustNameConstraints(centerVertically = true)
            } else {
                listDescription.visibility = View.VISIBLE
                listDescription.text = shoppingList.description
                // 有描述时，让标题偏上
                adjustNameConstraints(centerVertically = false)
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
                listBudget.text = "¥${String.format("%.2f", shoppingList.estimatedBudget)}"
            } else {
                listBudget.visibility = View.GONE
            }
            
            // 设置进度信息 - M3新增功能
            setupProgress(shoppingList)
            
            // 设置点击事件
            itemView.setOnClickListener { onItemClick(shoppingList) }
            btnMore.setOnClickListener { showMoreOptions(shoppingList) }
        }
        
        private fun setupProgress(shoppingList: ShoppingListEntity) {
            // 获取真实进度数据
            val progressData = getProgressData(shoppingList.id)
            
            val (progress, progressText) = when (shoppingList.status) {
                ShoppingListStatus.ACTIVE -> {
                    // 活跃状态：使用真实数据计算进度
                    val progress = if (progressData.totalItems > 0) {
                        (progressData.completedItems * 100) / progressData.totalItems
                    } else {
                        0
                    }
                    Pair(progress, progressData.progressText)
                }
                ShoppingListStatus.COMPLETED -> {
                    // 已完成状态：100%
                    Pair(100, progressData.progressText)
                }
                ShoppingListStatus.ARCHIVED -> {
                    // 已归档状态：显示完成状态
                    Pair(100, "已完成")
                }
            }
            
            progressBar.progress = progress
            tvProgress.text = progressText
        }
        
        
        /**
         * 动态调整清单名称的约束关系
         */
        private fun adjustNameConstraints(centerVertically: Boolean) {
            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)
            
            if (centerVertically) {
                // 无描述时：垂直居中对齐
                constraintSet.connect(R.id.tvListName, ConstraintSet.TOP, R.id.listIcon, ConstraintSet.TOP)
                constraintSet.connect(R.id.tvListName, ConstraintSet.BOTTOM, R.id.listIcon, ConstraintSet.BOTTOM)
            } else {
                // 有描述时：只约束到顶部，移除底部约束
                constraintSet.connect(R.id.tvListName, ConstraintSet.TOP, R.id.listIcon, ConstraintSet.TOP)
                constraintSet.clear(R.id.tvListName, ConstraintSet.BOTTOM)
            }
            
            constraintSet.applyTo(constraintLayout)
        }
        
        private fun showMoreOptions(shoppingList: ShoppingListEntity) {
            val popup = androidx.appcompat.widget.PopupMenu(itemView.context, btnMore)
            
            // 根据状态显示不同选项
            when (shoppingList.status) {
                ShoppingListStatus.ACTIVE -> {
                    popup.menu.add(0, 1, 0, "编辑清单")
                    popup.menu.add(0, 2, 1, "结束清单")
                    popup.menu.add(0, 3, 2, "删除清单")
                }
                ShoppingListStatus.COMPLETED -> {
                    popup.menu.add(0, 4, 0, "重新激活")
                    popup.menu.add(0, 3, 1, "删除清单")
                }
                ShoppingListStatus.ARCHIVED -> {
                    popup.menu.add(0, 3, 0, "删除清单")
                }
            }
            
            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    1 -> {
                        // 编辑清单
                        onEditClick(shoppingList)
                        true
                    }
                    2 -> {
                        // 结束清单
                        showCompleteConfirmDialog(shoppingList)
                        true
                    }
                    3 -> {
                        // 删除清单
                        showDeleteConfirmDialog(shoppingList)
                        true
                    }
                    4 -> {
                        // 重新激活
                        onCompleteClick(shoppingList)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
        
        private fun showCompleteConfirmDialog(shoppingList: ShoppingListEntity) {
            MaterialAlertDialogBuilder(itemView.context)
                .setTitle("结束清单")
                .setMessage("确定要结束清单 \"${shoppingList.name}\" 吗？\n结束后清单状态将变为已完成。")
                .setPositiveButton("结束") { _, _ ->
                    onCompleteClick(shoppingList)
                }
                .setNegativeButton("取消", null)
                .show()
        }
        
        private fun showDeleteConfirmDialog(shoppingList: ShoppingListEntity) {
            MaterialAlertDialogBuilder(itemView.context)
                .setTitle("删除清单")
                .setMessage("确定要删除清单 \"${shoppingList.name}\" 吗？\n此操作不可恢复，清单中的所有商品也将被删除。")
                .setPositiveButton("删除") { _, _ ->
                    onDeleteClick(shoppingList)
                }
                .setNegativeButton("取消", null)
                .show()
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
                ShoppingListStatus.ACTIVE -> itemView.context.getColor(R.color.shopping_primary)
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