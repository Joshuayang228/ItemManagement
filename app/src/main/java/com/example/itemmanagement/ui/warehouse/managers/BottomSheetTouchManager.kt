package com.example.itemmanagement.ui.warehouse.managers

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * BottomSheet触摸事件管理器
 * 
 * 功能：统一管理BottomSheet的触摸事件处理逻辑，解决拖拽与滚动冲突
 * 
 * 处理层级：
 * 1. RootView层：区分拖拽区域和内容区域
 * 2. ContentContainer层：保护ScrollView滚动
 * 3. ScrollView层：智能处理滚动、点击、失焦
 * 
 * 重构优化：
 * - 移除递归触摸保护，允许在任意区域滚动
 * - 集成失焦逻辑，点击空白区域失焦
 * - 智能区分点击和滚动意图
 */
class BottomSheetTouchManager(
    private val binding: FragmentFilterBottomSheetBinding,
    private val behaviorProvider: () -> BottomSheetBehavior<View>?,
    fragment: Fragment
) {
    
    // 触摸监听器集合，用于内存管理
    private val touchListeners = mutableSetOf<View.OnTouchListener>()
    private val isSetupComplete = AtomicBoolean(false)
    
    // Fragment弱引用，用于访问焦点和键盘
    private val fragmentReference = WeakReference(fragment)
    
    // 系统触摸阈值
    private val touchSlop: Int by lazy {
        ViewConfiguration.get(binding.root.context).scaledTouchSlop
    }
    
    /**
     * 设置完整的触摸事件处理
     */
    fun setupTouchHandling() {
        if (isSetupComplete.compareAndSet(false, true)) {
            // 🎯 禁用拖拽功能，筛选界面固定
            behaviorProvider()?.isDraggable = false
            android.util.Log.d("TouchManager", "🔒 已永久禁用 BottomSheet 拖拽功能")
            
            setupRootTouchDispatch()
            setupContentScrollProtection()
            setupDragZoneHandling()
            setupScrollStateListener()
        }
    }
    
    /**
     * 根视图触摸分发 - 最高层级的触摸事件路由
     */
    private fun setupRootTouchDispatch() {
        val rootTouchListener = RootTouchListener()
        binding.root.setOnTouchListener(rootTouchListener)
        touchListeners.add(rootTouchListener)
    }
    
    /**
     * 内容滚动保护 - 确保内容区域滚动优先
     */
    private fun setupContentScrollProtection() {
        val contentTouchListener = ContentTouchListener()
        binding.contentContainer.setOnTouchListener(contentTouchListener)
        touchListeners.add(contentTouchListener)
        
        val scrollTouchListener = SmartScrollTouchListener()
        binding.contentScrollView.setOnTouchListener(scrollTouchListener)
        touchListeners.add(scrollTouchListener)
        
        // 🎯 移除递归触摸保护，允许在任意区域滚动
        // applyTouchProtectionToAllChildren(binding.contentScrollView)
    }
    
    /**
     * 拖拽区域处理 - 提供视觉反馈
     */
    private fun setupDragZoneHandling() {
        val dragTouchListener = DragZoneTouchListener()
        binding.dragHandleContainer.setOnTouchListener(dragTouchListener)
        touchListeners.add(dragTouchListener)
    }
    
    /**
     * 滚动状态监听 - 协调滚动与拖拽
     * 注意：不要在这里设置滚动监听器，避免与NavigationSyncManager冲突
     * 由NavigationSyncManager统一管理滚动事件
     */
    private fun setupScrollStateListener() {
        // 移除独立的滚动监听器设置，避免覆盖NavigationSyncManager的监听器
        // binding.contentScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
        //     handleScrollStateChange(scrollY, oldScrollY)
        // }
    }
    
    /**
     * 根视图触摸监听器
     */
    private inner class RootTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            return when (getTouchZone(event)) {
                TouchZone.DRAG -> handleDragZoneTouch(event)
                TouchZone.CONTENT -> handleContentZoneTouch(event)
            }
        }
        
        private fun getTouchZone(event: MotionEvent): TouchZone {
            val dragZoneBottom = binding.dragHandleContainer.let { container ->
                val location = IntArray(2)
                container.getLocationInWindow(location)
                location[1] + container.height
            }
            
            return if (event.rawY <= dragZoneBottom) {
                TouchZone.DRAG
            } else {
                TouchZone.CONTENT
            }
        }
        
        private fun handleDragZoneTouch(event: MotionEvent): Boolean {
            // 拖拽区域：允许BottomSheet正常处理
            return false // 不拦截，让事件继续传递
        }
        
        private fun handleContentZoneTouch(event: MotionEvent): Boolean {
            return when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val canScroll = binding.contentScrollView.canScrollVertically(-1) || 
                                  binding.contentScrollView.canScrollVertically(1)
                    
                    if (canScroll) {
                        // 有滚动内容时：优先滚动，阻止BottomSheet拦截
                        binding.contentScrollView.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    false
                }
                else -> false
            }
        }
    }
    
    /**
     * 内容容器触摸监听器 - 简化版
     */
    private inner class ContentTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            // 只在 ACTION_DOWN 时请求不拦截，允许子View处理
            if (event.action == MotionEvent.ACTION_DOWN) {
                view.parent?.requestDisallowInterceptTouchEvent(true)
            }
            return false
        }
    }
    
    /**
     * 智能滚动触摸监听器 - 重构版
     * 
     * 功能：
     * 1. 在任意区域（控件或空白）都可以滚动
     * 2. 点击输入框外的区域失焦
     * 3. 智能区分点击和滚动意图
     */
    private inner class SmartScrollTouchListener : View.OnTouchListener {
        private var touchStartX = 0f
        private var touchStartY = 0f
        private var isScrolling = false
        private var hasMoved = false
        
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 记录触摸起始位置
                    touchStartX = event.x
                    touchStartY = event.y
                    isScrolling = false
                    hasMoved = false
                    
                    // 🎯 关键：ACTION_DOWN 时，先阻止 BottomSheet 拦截，让 ScrollView 有机会处理
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                    
                    return false  // 继续传递事件
                }
                
                MotionEvent.ACTION_MOVE -> {
                    // 计算移动距离
                    val deltaX = abs(event.x - touchStartX)
                    val deltaY = abs(event.y - touchStartY)
                    
                    // 判断是否开始滚动
                    if (!isScrolling && (deltaX > touchSlop || deltaY > touchSlop)) {
                        isScrolling = true
                        hasMoved = true
                        
                        // 🎯 关键：检测到滚动意图时
                        // 1. 如果当前有输入框焦点，失焦
                        val currentFocus = getCurrentFocus()
                        if (currentFocus != null && isInputView(currentFocus)) {
                            clearAllInputFocus()
                            hideKeyboard()
                            android.util.Log.d("TouchManager", "📜 滚动时失焦")
                        }
                        
                        // 2. 🎯 保持阻止 BottomSheet 拦截，让 ScrollView 滚动
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    
                    return false
                }
                
                MotionEvent.ACTION_UP -> {
                    if (!hasMoved || !isScrolling) {
                        // 🎯 这是一个点击事件（不是滚动）
                        val touchedView = findViewAtPosition(view, event.x, event.y)
                        
                        if (touchedView == null || !isInputView(touchedView)) {
                            // 点击在输入框外 -> 失焦
                            val currentFocus = getCurrentFocus()
                            if (currentFocus != null && isInputView(currentFocus)) {
                                clearAllInputFocus()
                                hideKeyboard()
                                android.util.Log.d("TouchManager", "🎯 点击空白区域失焦")
                            }
                        }
                        // 点击在输入框上 -> 不处理，让输入框正常聚焦
                    }
                    
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    isScrolling = false
                    hasMoved = false
                    return false
                }
                
                MotionEvent.ACTION_CANCEL -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    isScrolling = false
                    hasMoved = false
                    return false
                }
            }
            
            return false
        }
    }
    
    /**
     * 拖拽区域触摸监听器
     */
    private inner class DragZoneTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 按下时的视觉反馈
                    view.alpha = 0.8f
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    // 释放时恢复透明度
                    view.animate().alpha(1.0f).setDuration(150).start()
                }
            }
            return false // 让BottomSheetBehavior处理拖拽
        }
    }
    
    /**
     * 处理滚动状态变化（由NavigationSyncManager调用）
     */
    fun handleScrollStateChange(scrollY: Int, oldScrollY: Int) {
        val behavior = behaviorProvider()
        if (behavior != null) {
            when {
                scrollY == 0 && oldScrollY > 0 -> {
                    // 滚动到顶部：保持禁用拖拽（已永久禁用）
                    // behavior.isDraggable = false  // 已在 setupTouchHandling 中设置
                }
                scrollY > 0 -> {
                    // 在内容中滚动：确保拖拽不干扰内容滚动
                    binding.contentScrollView.parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
    }
    
    // 🎯 递归触摸保护已移除，以允许在任意区域滚动
    
    // ==================== 🎯 辅助方法 ====================
    
    /**
     * 在指定位置查找View
     */
    private fun findViewAtPosition(parent: View, x: Float, y: Float): View? {
        if (parent !is ViewGroup) {
            return parent
        }
        
        // 从后往前遍历（后添加的View在上层）
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i)
            
            // 检查点是否在child的边界内
            if (x >= child.left && x < child.right && 
                y >= child.top && y < child.bottom) {
                
                // 如果child是ViewGroup，递归查找
                if (child is ViewGroup && child.childCount > 0) {
                    val relativeX = x - child.left
                    val relativeY = y - child.top
                    val nestedView = findViewAtPosition(child, relativeX, relativeY)
                    if (nestedView != null) {
                        return nestedView
                    }
                }
                return child
            }
        }
        
        return null
    }
    
    /**
     * 检查是否是输入控件
     */
    private fun isInputView(view: View): Boolean {
        return view is android.widget.EditText ||
               view is com.google.android.material.textfield.TextInputEditText ||
               view is android.widget.AutoCompleteTextView ||
               view.parent is com.google.android.material.textfield.TextInputLayout
    }
    
    /**
     * 获取当前焦点
     */
    private fun getCurrentFocus(): View? {
        return fragmentReference.get()?.activity?.currentFocus
    }
    
    /**
     * 清除所有输入框焦点
     */
    private fun clearAllInputFocus() {
        try {
            // 方式1: 清除当前焦点
            fragmentReference.get()?.activity?.currentFocus?.clearFocus()
            
            // 方式2: 逐个清除所有输入框焦点（确保完整性）
            binding.valueRangeSection.minQuantityInput.clearFocus()
            binding.valueRangeSection.maxQuantityInput.clearFocus()
            binding.valueRangeSection.minPriceInput.clearFocus()
            binding.valueRangeSection.maxPriceInput.clearFocus()
            binding.coreSection.brandDropdown.clearFocus()
        } catch (e: Exception) {
            android.util.Log.e("TouchManager", "清除焦点失败: ${e.message}")
        }
    }
    
    /**
     * 隐藏软键盘
     */
    private fun hideKeyboard() {
        try {
            val fragment = fragmentReference.get() ?: return
            val imm = fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val view = fragment.view
            imm.hideSoftInputFromWindow(view?.windowToken, 0)
        } catch (e: Exception) {
            android.util.Log.e("TouchManager", "隐藏键盘失败: ${e.message}")
        }
    }
    
    /**
     * 检查触摸点是否在指定View的区域内（使用屏幕坐标）
     */
    private fun isTouchOnView(view: View, rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val viewLeft = location[0]
        val viewTop = location[1]
        val viewRight = viewLeft + view.width
        val viewBottom = viewTop + view.height
        
        return rawX >= viewLeft && rawX <= viewRight && rawY >= viewTop && rawY <= viewBottom
    }
    
    /**
     * 重新应用滚动保护机制
     */
    fun reapplyScrollProtection() {
        setupContentScrollProtection()
    }
    
    /**
     * 清理所有触摸监听器
     */
    fun cleanup() {
        touchListeners.clear()
        binding.root.setOnTouchListener(null)
        binding.contentContainer.setOnTouchListener(null)
        binding.contentScrollView.setOnTouchListener(null)
        binding.dragHandleContainer.setOnTouchListener(null)
        
        clearAllChildListeners(binding.root)
        isSetupComplete.set(false)
    }
    
    /**
     * 递归清理所有子视图监听器
     */
    private fun clearAllChildListeners(parentView: View) {
        if (parentView is ViewGroup) {
            for (i in 0 until parentView.childCount) {
                val child = parentView.getChildAt(i)
                child.setOnTouchListener(null)
                clearAllChildListeners(child)
            }
        }
    }
    
    /**
     * 触摸区域枚举
     */
    private enum class TouchZone {
        DRAG,    // 拖拽区域
        CONTENT  // 内容区域
    }
}
