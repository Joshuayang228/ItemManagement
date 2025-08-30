package com.example.itemmanagement.ui.profile.recycle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.DeletedItemEntity
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * 回收站物品列表的适配器
 */
class RecycleBinAdapter(
    private val onItemClick: (DeletedItemEntity) -> Unit,
    private val onRestoreClick: (DeletedItemEntity) -> Unit,
    private val onDeleteClick: (DeletedItemEntity) -> Unit,
    private val onSelectionChange: (DeletedItemEntity, Boolean) -> Unit
) : ListAdapter<DeletedItemEntity, RecycleBinAdapter.DeletedItemViewHolder>(DeletedItemDiffCallback()) {
    
    /**
     * 是否处于选择模式
     */
    var isSelectMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    
    /**
     * 选中的物品ID集合
     */
    var selectedItems: Set<Long> = emptySet()
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletedItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_deleted_item, parent, false)
        return DeletedItemViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: DeletedItemViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DeletedItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvBrand: TextView = itemView.findViewById(R.id.tvBrand)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvDeletedDate: TextView = itemView.findViewById(R.id.tvDeletedDate)
        private val tvDaysRemaining: TextView = itemView.findViewById(R.id.tvDaysRemaining)
        private val btnRestore: ImageButton = itemView.findViewById(R.id.btnRestore)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        private val cbSelect: CheckBox = itemView.findViewById(R.id.cbSelect)
        
        fun bind(item: DeletedItemEntity) {
            // 基本信息
            tvName.text = item.name
            tvCategory.text = item.category
            tvBrand.text = if (!item.brand.isNullOrBlank()) item.brand else "无品牌"
            
            // 数量信息
            val quantityText = if (item.quantity != null && item.unit != null) {
                "${item.quantity}${item.unit}"
            } else {
                "数量未知"
            }
            tvQuantity.text = quantityText
            
            // 位置信息
            val locationText = item.getFullLocationString() ?: "未知位置"
            tvLocation.text = locationText
            
            // 删除时间
            val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
            tvDeletedDate.text = "删除时间：${dateFormat.format(item.deletedDate)}"
            
            // 剩余天数
            val daysRemaining = item.getDaysUntilAutoClean()
            tvDaysRemaining.text = when {
                daysRemaining <= 0 -> "即将自动清理"
                daysRemaining <= 3 -> "$daysRemaining 天后清理"
                else -> "还有 $daysRemaining 天"
            }
            
            // 设置剩余天数的颜色
            val textColor = when {
                daysRemaining <= 0 -> ContextCompat.getColor(itemView.context, R.color.expired_color)
                daysRemaining <= 3 -> ContextCompat.getColor(itemView.context, R.color.warning_color)
                else -> ContextCompat.getColor(itemView.context, R.color.text_secondary)
            }
            tvDaysRemaining.setTextColor(textColor)
            
            // 加载照片
            loadItemPhoto(item)
            
            // 选择模式处理
            cbSelect.visibility = if (isSelectMode) View.VISIBLE else View.GONE
            cbSelect.isChecked = item.originalId in selectedItems
            cbSelect.setOnCheckedChangeListener { _, isChecked ->
                onSelectionChange(item, isChecked)
            }
            
            // 操作按钮
            btnRestore.visibility = if (isSelectMode) View.GONE else View.VISIBLE
            btnDelete.visibility = if (isSelectMode) View.GONE else View.VISIBLE
            
            btnRestore.setOnClickListener { onRestoreClick(item) }
            btnDelete.setOnClickListener { onDeleteClick(item) }
            
            // 整体点击
            itemView.setOnClickListener {
                if (isSelectMode) {
                    cbSelect.isChecked = !cbSelect.isChecked
                } else {
                    onItemClick(item)
                }
            }
            
            // 长按进入选择模式
            itemView.setOnLongClickListener {
                if (!isSelectMode) {
                    onSelectionChange(item, true)
                }
                true
            }
            
            // 设置是否可恢复的状态
            if (!item.canRestore) {
                btnRestore.isEnabled = false
                btnRestore.alpha = 0.5f
                tvName.alpha = 0.6f
            } else {
                btnRestore.isEnabled = true
                btnRestore.alpha = 1.0f
                tvName.alpha = 1.0f
            }
        }
        
        private fun loadItemPhoto(item: DeletedItemEntity) {
            val photoUris = try {
                if (!item.photoUris.isNullOrBlank()) {
                    val jsonArray = JSONArray(item.photoUris)
                    (0 until jsonArray.length()).map { jsonArray.getString(it) }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList()
            }
            
            if (photoUris.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(photoUris.first())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image_24dp)
                    .centerCrop()
                    .into(ivPhoto)
            } else {
                // 根据分类设置默认图标
                val defaultIcon = when (item.category.lowercase()) {
                    "食品", "食物" -> R.drawable.ic_inventory_24dp
                    "药品", "药物" -> R.drawable.ic_inventory_24dp
                    "电子产品", "数码" -> R.drawable.ic_inventory_24dp
                    else -> R.drawable.ic_inventory_24dp
                }
                ivPhoto.setImageResource(defaultIcon)
            }
        }
    }
}

/**
 * DiffUtil回调，用于高效更新列表
 */
class DeletedItemDiffCallback : DiffUtil.ItemCallback<DeletedItemEntity>() {
    override fun areItemsTheSame(oldItem: DeletedItemEntity, newItem: DeletedItemEntity): Boolean {
        return oldItem.originalId == newItem.originalId
    }
    
    override fun areContentsTheSame(oldItem: DeletedItemEntity, newItem: DeletedItemEntity): Boolean {
        return oldItem == newItem
    }
}
