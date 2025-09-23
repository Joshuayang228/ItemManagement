package com.example.itemmanagement.ui.warehouse.managers

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.example.itemmanagement.databinding.FragmentFilterBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.util.concurrent.atomic.AtomicBoolean

/**
 * BottomSheet触摸事件管理器
 * 
 * 功能：统一管理BottomSheet的触摸事件处理逻辑，解决拖拽与滚动冲突
 * 
 * 处理层级：
 * 1. RootView层：区分拖拽区域和内容区域
 * 2. ContentContainer层：保护ScrollView滚动
 * 3. ScrollView层：处理特殊组件（如品牌输入框）
 * 4. ChildView层：递归保护所有子视图
 */
class BottomSheetTouchManager(
    private val binding: FragmentFilterBottomSheetBinding,
    private val behaviorProvider: () -> BottomSheetBehavior<View>?
) {
    
    // 触摸监听器集合，用于内存管理
    private val touchListeners = mutableSetOf<View.OnTouchListener>()
    private val isSetupComplete = AtomicBoolean(false)
    
    /**
     * 设置完整的触摸事件处理
     */
    fun setupTouchHandling() {
        if (isSetupComplete.compareAndSet(false, true)) {
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
        
        val scrollTouchListener = ScrollTouchListener()
        binding.contentScrollView.setOnTouchListener(scrollTouchListener)
        touchListeners.add(scrollTouchListener)
        
        // 递归保护所有子视图
        applyTouchProtectionToAllChildren(binding.contentScrollView)
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
     * 内容容器触摸监听器
     */
    private inner class ContentTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, 
                MotionEvent.ACTION_CANCEL -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            return false
        }
    }
    
    /**
     * ScrollView触摸监听器
     */
    private inner class ScrollTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            // 检查是否触摸在品牌输入框区域
            if (isTouchOnBrandDropdown(event)) {
                return false // 让AutoCompleteTextView正常处理
            }
            
            // 其他区域：保持滚动保护
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    view.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, 
                MotionEvent.ACTION_CANCEL -> {
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            return false
        }
        
        private fun isTouchOnBrandDropdown(event: MotionEvent): Boolean {
            return isTouchOnView(binding.coreSection.brandDropdown, event.rawX, event.rawY)
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
                    // 滚动到顶部：允许BottomSheet响应拖拽
                    behavior.isDraggable = true
                }
                scrollY > 0 -> {
                    // 在内容中滚动：确保拖拽不干扰内容滚动
                    binding.contentScrollView.parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
        }
    }
    
    /**
     * 递归地为所有子视图应用触摸保护
     */
    private fun applyTouchProtectionToAllChildren(parentView: View) {
        if (parentView is ViewGroup) {
            for (i in 0 until parentView.childCount) {
                val child = parentView.getChildAt(i)
                
                // 跳过品牌输入框，因为它有特殊的处理逻辑
                if (child.id == binding.coreSection.brandDropdown.id) {
                    continue
                }
                
                val childTouchListener = ChildViewTouchListener()
                child.setOnTouchListener(childTouchListener)
                touchListeners.add(childTouchListener)
                
                // 递归处理子视图
                applyTouchProtectionToAllChildren(child)
            }
        }
    }
    
    /**
     * 子视图触摸监听器
     */
    private inner class ChildViewTouchListener : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.contentScrollView.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    binding.contentScrollView.parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    binding.contentScrollView.parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            return false // 让子视图的原有功能正常工作
        }
    }
    
    /**
     * 检查触摸点是否在指定View的区域内
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
