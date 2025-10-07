package com.example.itemmanagement.ui.borrow

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.dao.BorrowWithItemInfo
import com.example.itemmanagement.data.entity.BorrowStatus
import com.example.itemmanagement.databinding.ItemBorrowBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 借还记录列表适配器
 */
class BorrowAdapter(
    private val onItemClick: (BorrowWithItemInfo) -> Unit,
    private val onMoreClick: (View, BorrowWithItemInfo) -> Unit
) : ListAdapter<BorrowWithItemInfo, BorrowAdapter.BorrowViewHolder>(BorrowDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BorrowViewHolder {
        val binding = ItemBorrowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BorrowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BorrowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BorrowViewHolder(
        private val binding: ItemBorrowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(borrow: BorrowWithItemInfo) {
            with(binding) {
                // 物品信息
                tvItemName.text = borrow.itemName

                // 加载物品图片
                if (borrow.photoUri?.isNotBlank() == true) {
                    try {
                        val uri = Uri.parse(borrow.photoUri)
                        Glide.with(root.context)
                            .load(uri)
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_broken_image_24dp)
                            .into(ivItemPhoto)
                    } catch (e: Exception) {
                        ivItemPhoto.setImageResource(R.drawable.ic_image_placeholder)
                    }
                } else {
                    ivItemPhoto.setImageResource(R.drawable.ic_image_placeholder)
                }

                // 借用人信息
                tvBorrowerName.text = borrow.borrowerName
                if (borrow.borrowerContact?.isNotBlank() == true) {
                    tvBorrowerContact.text = borrow.borrowerContact
                    tvBorrowerContact.visibility = View.VISIBLE
                } else {
                    tvBorrowerContact.visibility = View.GONE
                }

                // 时间信息
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                tvBorrowDate.text = "借出：${dateFormat.format(Date(borrow.borrowDate))}"
                tvExpectedReturnDate.text = "应还：${dateFormat.format(Date(borrow.expectedReturnDate))}"

                // 根据状态显示不同的信息和颜色
                when (borrow.status) {
                    BorrowStatus.BORROWED -> {
                        setupBorrowedStatus(borrow)
                    }
                    BorrowStatus.OVERDUE -> {
                        setupOverdueStatus(borrow)
                    }
                    BorrowStatus.RETURNED -> {
                        setupReturnedStatus(borrow)
                    }
                }

                // 点击事件
                root.setOnClickListener { onItemClick(borrow) }
                btnMore.setOnClickListener { view -> onMoreClick(view, borrow) }
            }
        }

        /**
         * 设置已借出状态显示
         */
        private fun setupBorrowedStatus(borrow: BorrowWithItemInfo) {
            with(binding) {
                // 计算剩余天数
                val currentTime = System.currentTimeMillis()
                val remainingDays = ((borrow.expectedReturnDate - currentTime) / (1000 * 60 * 60 * 24)).toInt()

                when {
                    remainingDays > 14 -> {
                        // 时间充足（> 14天）- 绿色
                        tvStatus.text = "已借出"
                        tvStatus.setTextColor(Color.parseColor("#4CAF50"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E8"))
                        
                        ivReturnDateIcon.setColorFilter(Color.parseColor("#4CAF50"))
                        tvExpectedReturnDate.setTextColor(Color.parseColor("#4CAF50"))
                        
                        tvRemainingDays.text = "还剩${remainingDays}天"
                        tvRemainingDays.setTextColor(Color.parseColor("#4CAF50"))
                        tvRemainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E8"))
                        tvRemainingDays.visibility = View.VISIBLE
                    }
                    remainingDays in 7..14 -> {
                        // 提前提醒（7-14天）- 浅橙色
                        tvStatus.text = "已借出"
                        tvStatus.setTextColor(Color.parseColor("#FFA726"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF8E1"))
                        
                        ivReturnDateIcon.setColorFilter(Color.parseColor("#FFA726"))
                        tvExpectedReturnDate.setTextColor(Color.parseColor("#FFA726"))
                        
                        tvRemainingDays.text = "还剩${remainingDays}天"
                        tvRemainingDays.setTextColor(Color.parseColor("#FFA726"))
                        tvRemainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF8E1"))
                        tvRemainingDays.visibility = View.VISIBLE
                    }
                    remainingDays in 4..6 -> {
                        // 注意提醒（4-6天）- 橙色
                        tvStatus.text = "即将到期"
                        tvStatus.setTextColor(Color.parseColor("#FF9800"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
                        
                        ivReturnDateIcon.setColorFilter(Color.parseColor("#FF9800"))
                        tvExpectedReturnDate.setTextColor(Color.parseColor("#FF9800"))
                        
                        tvRemainingDays.text = "还剩${remainingDays}天"
                        tvRemainingDays.setTextColor(Color.parseColor("#FF9800"))
                        tvRemainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF3E0"))
                        tvRemainingDays.visibility = View.VISIBLE
                    }
                    remainingDays in 1..3 -> {
                        // 即将到期（1-3天）- 深橙色
                        tvStatus.text = "即将到期"
                        tvStatus.setTextColor(Color.parseColor("#F57C00"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFE0B2"))
                        
                        ivReturnDateIcon.setColorFilter(Color.parseColor("#F57C00"))
                        tvExpectedReturnDate.setTextColor(Color.parseColor("#F57C00"))
                        
                        tvRemainingDays.text = "还剩${remainingDays}天"
                        tvRemainingDays.setTextColor(Color.parseColor("#F57C00"))
                        tvRemainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFE0B2"))
                        tvRemainingDays.visibility = View.VISIBLE
                    }
                    remainingDays == 0 -> {
                        // 今天到期 - 红色
                        tvStatus.text = "今日到期"
                        tvStatus.setTextColor(Color.parseColor("#F44336"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                        
                        ivReturnDateIcon.setColorFilter(Color.parseColor("#F44336"))
                        tvExpectedReturnDate.setTextColor(Color.parseColor("#F44336"))
                        
                        tvRemainingDays.text = "今日到期"
                        tvRemainingDays.setTextColor(Color.parseColor("#F44336"))
                        tvRemainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                        tvRemainingDays.visibility = View.VISIBLE
                    }
                    else -> {
                        // 已逾期 - 红色
                        val overdueDays = -remainingDays
                        tvStatus.text = "已逾期"
                        tvStatus.setTextColor(Color.parseColor("#F44336"))
                        tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                        
                        ivReturnDateIcon.setColorFilter(Color.parseColor("#F44336"))
                        tvExpectedReturnDate.setTextColor(Color.parseColor("#F44336"))
                        
                        tvRemainingDays.text = "逾期${overdueDays}天"
                        tvRemainingDays.setTextColor(Color.parseColor("#F44336"))
                        tvRemainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                        tvRemainingDays.visibility = View.VISIBLE
                    }
                }
            }
        }

        /**
         * 设置已逾期状态显示
         */
        private fun setupOverdueStatus(borrow: BorrowWithItemInfo) {
            with(binding) {
                val currentTime = System.currentTimeMillis()
                val overdueDays = ((currentTime - borrow.expectedReturnDate) / (1000 * 60 * 60 * 24)).toInt()

                tvStatus.text = "已逾期"
                tvStatus.setTextColor(Color.parseColor("#F44336"))
                tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                
                ivReturnDateIcon.setColorFilter(Color.parseColor("#F44336"))
                tvExpectedReturnDate.setTextColor(Color.parseColor("#F44336"))
                
                tvRemainingDays.text = "逾期${overdueDays}天"
                tvRemainingDays.setTextColor(Color.parseColor("#F44336"))
                tvRemainingDays.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                tvRemainingDays.visibility = View.VISIBLE
            }
        }

        /**
         * 设置已归还状态显示
         */
        private fun setupReturnedStatus(borrow: BorrowWithItemInfo) {
            with(binding) {
                tvStatus.text = "已归还"
                tvStatus.setTextColor(Color.parseColor("#9E9E9E"))
                tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F5F5F5"))
                
                ivReturnDateIcon.setColorFilter(Color.parseColor("#9E9E9E"))
                tvExpectedReturnDate.setTextColor(Color.parseColor("#9E9E9E"))
                
                // 已归还不显示剩余天数
                tvRemainingDays.visibility = View.GONE
            }
        }
    }
}

/**
 * DiffUtil回调用于计算列表差异
 */
class BorrowDiffCallback : DiffUtil.ItemCallback<BorrowWithItemInfo>() {
    override fun areItemsTheSame(oldItem: BorrowWithItemInfo, newItem: BorrowWithItemInfo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: BorrowWithItemInfo, newItem: BorrowWithItemInfo): Boolean {
        return oldItem == newItem
    }
}
