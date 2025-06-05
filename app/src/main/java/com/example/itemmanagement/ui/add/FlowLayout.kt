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
    private val horizontalSpacing = 16
    private val verticalSpacing = 16

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var height = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom

        val count = childCount
        var lineWidth = 0
        var maxLineWidth = 0
        var totalHeight = 0
        lineHeight = 0

        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec)
                val childWidth = child.measuredWidth
                lineHeight = max(lineHeight, child.measuredHeight)

                if (lineWidth + childWidth > width) {
                    maxLineWidth = max(maxLineWidth, lineWidth)
                    lineWidth = childWidth
                    totalHeight += lineHeight + verticalSpacing
                } else {
                    lineWidth += if (lineWidth == 0) childWidth else childWidth + horizontalSpacing
                }
            }
        }

        totalHeight += lineHeight
        maxLineWidth = max(maxLineWidth, lineWidth)

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
        val count = childCount
        val width = r - l
        var xPos = paddingLeft
        var yPos = paddingTop
        var lineHeight = 0

        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight

                if (xPos + childWidth > width) {
                    xPos = paddingLeft
                    yPos += lineHeight + verticalSpacing
                    lineHeight = childHeight
                } else {
                    lineHeight = max(lineHeight, childHeight)
                }

                child.layout(xPos, yPos, xPos + childWidth, yPos + childHeight)
                xPos += childWidth + horizontalSpacing
            }
        }
    }
}