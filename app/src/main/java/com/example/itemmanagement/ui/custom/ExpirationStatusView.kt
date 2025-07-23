package com.example.itemmanagement.ui.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.itemmanagement.R
import com.example.itemmanagement.data.model.Item
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 自定义保质期进度条视图
 * 显示纯色填充的进度条和右侧的剩余天数文本
 */
class ExpirationStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 画笔
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // 矩形区域
    private val backgroundRect = RectF()
    private val progressRect = RectF()
    
    // 自定义属性
    private var barHeight = 0f
    private var cornerRadius = 0f
    private var goodColor = 0
    private var warningColor = 0
    private var errorColor = 0
    private var textSize = 0f
    private var textPadding = 0f
    
    // 状态数据
    private var progress = 0 // 0-100
    private var statusText = ""
    private var statusColor = 0
    
    // 是否需要显示文本
    private var showStatusText = true
    
    // 文本绘制位置
    private var barWidthRatio = 0.7f // 进度条占总宽度的比例
    
    // 文本高度
    private var textHeight = 0f

    init {
        // 初始化默认值
        backgroundPaint.color = ContextCompat.getColor(context, R.color.background_light)
        
        // 获取自定义属性
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ExpirationStatusView)
        try {
            barHeight = typedArray.getDimension(
                R.styleable.ExpirationStatusView_barHeight,
                context.resources.getDimension(R.dimen.default_bar_height)
            )
            cornerRadius = typedArray.getDimension(
                R.styleable.ExpirationStatusView_cornerRadius, 
                context.resources.getDimension(R.dimen.default_corner_radius)
            )
            goodColor = typedArray.getColor(
                R.styleable.ExpirationStatusView_goodColor,
                ContextCompat.getColor(context, R.color.status_good)
            )
            warningColor = typedArray.getColor(
                R.styleable.ExpirationStatusView_warningColor,
                ContextCompat.getColor(context, R.color.status_warning)
            )
            errorColor = typedArray.getColor(
                R.styleable.ExpirationStatusView_errorColor,
                ContextCompat.getColor(context, R.color.status_error)
            )
            textSize = typedArray.getDimension(
                R.styleable.ExpirationStatusView_textSize,
                context.resources.getDimension(R.dimen.default_text_size)
            )
            textPadding = typedArray.getDimension(
                R.styleable.ExpirationStatusView_textPadding,
                context.resources.getDimension(R.dimen.default_text_padding)
            )
            showStatusText = typedArray.getBoolean(
                R.styleable.ExpirationStatusView_showStatusText, 
                true
            )
            barWidthRatio = typedArray.getFloat(
                R.styleable.ExpirationStatusView_barWidthRatio,
                0.7f
            ).coerceIn(0.3f, 0.9f)
        } finally {
            typedArray.recycle()
        }
        
        // 设置画笔属性
        progressPaint.style = Paint.Style.FILL
        
        textPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.LEFT
        
        // 计算文本高度
        textHeight = textPaint.fontMetrics.bottom - textPaint.fontMetrics.top
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 计算所需的高度，确保足够放下文本和进度条
        val textHeightWithPadding = textHeight + paddingTop + paddingBottom
        val barHeightWithPadding = barHeight + paddingTop + paddingBottom
        val desiredHeight = max(textHeightWithPadding, barHeightWithPadding).roundToInt()
        
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize)
            else -> desiredHeight
        }
        
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width - paddingLeft - paddingRight
        val height = height - paddingTop - paddingBottom
        
        // 计算进度条宽度（占总宽度的一部分）
        val barWidth = width * barWidthRatio
        
        // 设置进度条背景矩形 (垂直居中)
        backgroundRect.set(
            paddingLeft.toFloat(),
            paddingTop + (height - barHeight) / 2,
            paddingLeft + barWidth,
            paddingTop + (height + barHeight) / 2
        )
        
        // 绘制背景
        canvas.drawRoundRect(backgroundRect, cornerRadius, cornerRadius, backgroundPaint)
        
        // 计算并绘制进度
        val progressWidth = barWidth * progress / 100
        if (progressWidth > 0) {
            progressRect.set(
                backgroundRect.left,
                backgroundRect.top,
                backgroundRect.left + progressWidth,
                backgroundRect.bottom
            )
            
            progressPaint.color = statusColor
            canvas.drawRoundRect(progressRect, cornerRadius, cornerRadius, progressPaint)
            
            // 如果进度不够长，裁剪掉右边的圆角
            if (progressWidth < cornerRadius * 2) {
                // 绘制一个矩形覆盖右边的圆角部分
                val clipRect = RectF(
                    progressRect.right - cornerRadius,
                    progressRect.top,
                    progressRect.right,
                    progressRect.bottom
                )
                canvas.drawRect(clipRect, backgroundPaint)
            }
        }
        
        // 绘制文本 (垂直居中)
        if (showStatusText && statusText.isNotEmpty()) {
            val textX = backgroundRect.right + textPadding
            // 文本垂直居中
            val textY = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2 + paddingTop
            
            // 调整文字颜色以匹配状态
            textPaint.color = statusColor
            canvas.drawText(statusText, textX, textY, textPaint)
        }
    }

    /**
     * 更新保质期状态
     * @param item 商品数据
     */
    fun updateStatus(item: Item) {
        if (item.expirationDate == null) {
            visibility = GONE
            return
        }
        
        visibility = VISIBLE
        
        val expirationDate = item.expirationDate
        val startDate = item.productionDate ?: item.addDate
        val currentDate = Date()
        
        // 验证日期有效性
        if (startDate.after(expirationDate)) {
            visibility = GONE
            return
        }
        
        // 计算进度
        val totalDuration = expirationDate.time - startDate.time
        val elapsedDuration = currentDate.time - startDate.time
        
        progress = if (totalDuration > 0) {
            (elapsedDuration * 100 / totalDuration).toInt().coerceIn(0, 100)
        } else {
            100
        }
        
        // 计算剩余天数
        val remainingMillis = expirationDate.time - currentDate.time
        val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingMillis)
        
        // 设置状态颜色和文本
        statusText = when {
            remainingDays > 30 -> {
                statusColor = goodColor
                "剩余 ${remainingDays} 天"
            }
            remainingDays in 1..30 -> {
                statusColor = warningColor
                "剩余 ${remainingDays} 天"
            }
            remainingDays == 0L -> {
                statusColor = errorColor
                "今天过期"
            }
            else -> {
                statusColor = errorColor
                "已过期 ${-remainingDays} 天"
            }
        }
        
        // 触发重绘
        invalidate()
    }
    
    /**
     * 手动设置进度和状态
     * @param progress 进度值(0-100)
     * @param statusText 状态文本
     * @param statusColor 状态颜色
     */
    fun setStatus(progress: Int, statusText: String, statusColor: Int) {
        this.progress = progress.coerceIn(0, 100)
        this.statusText = statusText
        this.statusColor = statusColor
        invalidate()
    }
} 