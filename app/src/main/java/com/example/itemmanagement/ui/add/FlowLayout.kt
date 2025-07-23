package com.example.itemmanagement.ui.add

import android.content.Context
import android.util.AttributeSet

import android.view.View
import android.view.ViewGroup
import kotlin.math.max

class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {

    private var lineHeight = 0
    private val maxItemsPerRow = 3
    private val horizontalSpacing = 8  // 标签之间的固定间距
    private val verticalSpacing = 8    // 行之间的固定间距

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var height = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom

        val count = childCount
        var totalHeight = 0
        lineHeight = 0

        // 收集所有可见的子视图
        val childrenToLayout = mutableListOf<View>()
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                childrenToLayout.add(child)
            }
        }

        // 调试日志


        // 如果没有子视图，设置高度为0
        if (childrenToLayout.isEmpty()) {
            setMeasuredDimension(
                MeasureSpec.getSize(widthMeasureSpec),
                paddingTop + paddingBottom
            )
            return
        }

        // 计算每行可用的实际宽度（考虑右侧预留空间）
        val effectiveWidth = if (paddingEnd > 40) {
            width - paddingEnd
        } else {
            width - 40 // 预留40dp给下拉箭头
        }

        // 按每行最多3个进行分组
        var currentLineStart = 0
        while (currentLineStart < childrenToLayout.size) {
            var xPos = 0
            var maxChildHeight = 0
            var itemsInThisRow = 0

            // 测量这一行能放下多少个子视图
            for (i in 0 until minOf(maxItemsPerRow, childrenToLayout.size - currentLineStart)) {
                val child = childrenToLayout[currentLineStart + i]
                measureChild(child, widthMeasureSpec, heightMeasureSpec)

                val childWidth = child.measuredWidth
                if (xPos + childWidth > effectiveWidth && itemsInThisRow > 0) {
                    break
                }

                maxChildHeight = max(maxChildHeight, child.measuredHeight)
                xPos += childWidth + horizontalSpacing
                itemsInThisRow++
            }

            totalHeight += maxChildHeight
            if (currentLineStart + itemsInThisRow < childrenToLayout.size) {
                totalHeight += verticalSpacing
            }
            currentLineStart += itemsInThisRow
        }

        height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> totalHeight.coerceAtMost(height)
            else -> totalHeight
        }

        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            height + paddingTop + paddingBottom
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        var yPos = paddingTop

        // 如果没有子视图，直接返回
        if (childCount == 0) {
            return
        }

        // 计算每行可用的实际宽度（考虑右侧预留空间）
        val effectiveWidth = if (paddingEnd > 40) {
            width - paddingLeft - paddingEnd
        } else {
            width - paddingLeft - 40 // 预留40dp给下拉箭头
        }

        // 收集所有可见的子视图
        val childrenToLayout = mutableListOf<View>()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                childrenToLayout.add(child)
                // 调试日志
    
            }
        }

        var currentLineStart = 0
        while (currentLineStart < childrenToLayout.size) {
            var xPos = paddingLeft
            var maxChildHeight = 0
            var itemsInThisRow = 0

            // 布局这一行的子视图
            for (i in 0 until minOf(maxItemsPerRow, childrenToLayout.size - currentLineStart)) {
                val child = childrenToLayout[currentLineStart + i]
                val childWidth = child.measuredWidth

                if (xPos - paddingLeft + childWidth > effectiveWidth && itemsInThisRow > 0) {
                    break
                }

                child.layout(xPos, yPos, xPos + childWidth, yPos + child.measuredHeight)
                maxChildHeight = max(maxChildHeight, child.measuredHeight)
                xPos += childWidth + horizontalSpacing
                itemsInThisRow++
            }

            // 调试日志


            // 只有当还有下一行时才添加行间距
            if (currentLineStart + itemsInThisRow < childrenToLayout.size) {
                yPos += maxChildHeight + verticalSpacing
            } else {
                yPos += maxChildHeight
            }

            currentLineStart += itemsInThisRow
        }
    }
}