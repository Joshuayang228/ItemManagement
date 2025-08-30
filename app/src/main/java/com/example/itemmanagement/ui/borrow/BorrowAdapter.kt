package com.example.itemmanagement.ui.borrow

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.dao.BorrowWithItemInfo
import com.example.itemmanagement.data.entity.BorrowStatus
import com.example.itemmanagement.databinding.ItemBorrowBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 借还记录列表适配器
 */
class BorrowAdapter(
    private val onItemClick: (BorrowWithItemInfo) -> Unit,
    private val onReturnClick: (BorrowWithItemInfo) -> Unit,
    private val onMoreClick: (BorrowWithItemInfo) -> Unit
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
                tvItemCategory.text = borrow.category
                if (borrow.brand?.isNotBlank() == true) {
                    tvItemBrand.text = borrow.brand
                    tvItemBrand.visibility = View.VISIBLE
                } else {
                    tvItemBrand.visibility = View.GONE
                }

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

                // 备注信息
                if (borrow.notes?.isNotBlank() == true) {
                    tvNotes.text = borrow.notes
                    tvNotes.visibility = View.VISIBLE
                } else {
                    tvNotes.visibility = View.GONE
                }

                // 点击事件
                root.setOnClickListener { onItemClick(borrow) }
                btnMore.setOnClickListener { onMoreClick(borrow) }
            }
        }

        private fun setupBorrowedStatus(borrow: BorrowWithItemInfo) {
            with(binding) {
                // 计算剩余天数
                val currentTime = System.currentTimeMillis()
                val remainingDays = ((borrow.expectedReturnDate - currentTime) / (1000 * 60 * 60 * 24)).toInt()

                when {
                    remainingDays > 3 -> {
                        // 还有较多时间
                        tvStatus.text = "还剩 ${remainingDays} 天"
                        tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.color_success))
                        cardView.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.color_surface))
                    }
                    remainingDays in 1..3 -> {
                        // 即将到期
                        tvStatus.text = "还剩 ${remainingDays} 天"
                        tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.color_warning))
                        cardView.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.color_warning_surface))
                    }
                    remainingDays == 0 -> {
                        // 今天到期
                        tvStatus.text = "今日到期"
                        tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.color_error))
                        cardView.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.color_error_surface))
                    }
                    else -> {
                        // 应该是逾期状态，但这里防御性处理
                        tvStatus.text = "已逾期 ${-remainingDays} 天"
                        tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.color_error))
                        cardView.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.color_error_surface))
                    }
                }

                // 显示归还按钮
                btnReturn.visibility = View.VISIBLE
                btnReturn.setOnClickListener { onReturnClick(borrow) }
            }
        }

        private fun setupOverdueStatus(borrow: BorrowWithItemInfo) {
            with(binding) {
                val currentTime = System.currentTimeMillis()
                val overdueDays = ((currentTime - borrow.expectedReturnDate) / (1000 * 60 * 60 * 24)).toInt()

                tvStatus.text = "已逾期 ${overdueDays} 天"
                tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.color_error))
                cardView.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.color_error_surface))

                // 显示归还按钮
                btnReturn.visibility = View.VISIBLE
                btnReturn.setOnClickListener { onReturnClick(borrow) }
            }
        }

        private fun setupReturnedStatus(borrow: BorrowWithItemInfo) {
            with(binding) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val returnDate = if (borrow.actualReturnDate != null) {
                    dateFormat.format(Date(borrow.actualReturnDate))
                } else {
                    "未知"
                }

                tvStatus.text = "已归还 ($returnDate)"
                tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.color_success))
                cardView.setCardBackgroundColor(ContextCompat.getColor(root.context, R.color.color_surface))

                // 隐藏归还按钮
                btnReturn.visibility = View.GONE
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
