package com.example.itemmanagement.ui.shopping

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.itemmanagement.R
import com.example.itemmanagement.adapter.ShoppingListAdapter

/**
 * 在已购买和未购买物品之间显示分割线的ItemDecoration
 */
class PurchasedDividerDecoration(
    context: Context,
    private val adapter: ShoppingListAdapter
) : RecyclerView.ItemDecoration() {

    private val paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.divider_gray)
        strokeWidth = context.resources.getDimension(R.dimen.divider_height)
    }

    private val dividerPadding = context.resources.getDimensionPixelSize(R.dimen.divider_padding)

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val purchasedStartPosition = adapter.getPurchasedStartPosition()
        
        // 如果没有已购买物品或没有未购买物品，不绘制分割线
        if (purchasedStartPosition <= 0 || purchasedStartPosition >= adapter.itemCount) {
            return
        }

        // 找到已购买物品的第一个item的ViewHolder
        val viewHolder = parent.findViewHolderForAdapterPosition(purchasedStartPosition)
        
        if (viewHolder != null) {
            val view = viewHolder.itemView
            val left = parent.paddingLeft.toFloat() + dividerPadding
            val right = (parent.width - parent.paddingRight).toFloat() - dividerPadding
            val y = view.top.toFloat()
            
            // 绘制分割线
            c.drawLine(left, y, right, y, paint)
        }
    }
}


