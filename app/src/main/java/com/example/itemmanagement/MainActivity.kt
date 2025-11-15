package com.example.itemmanagement

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.itemmanagement.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    
    // 🎯 跟踪TopBar当前状态，避免重复操作导致的闪现
    private var isTopBarVisible: Boolean = false
    private var isTopBarTitleEnabled: Boolean = false
    
    // 通知权限申请器
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Snackbar.make(binding.root, "通知权限已授权", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, "通知权限被拒绝，可能无法收到提醒通知", Snackbar.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("MainActivity", "📍 onCreate called, savedInstanceState=${savedInstanceState != null}")
        
        // 启用 Material You 动态颜色（Android 12+）
        DynamicColors.applyToActivityIfAvailable(this)
        
        // 启用 Edge-to-Edge 显示
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 处理系统窗口内边距
        setupWindowInsets()

        // 设置 Material 3 工具栏
        setSupportActionBar(binding.toolbar)
        
        // 🎯 恢复或初始化TopBar状态
        isTopBarVisible = savedInstanceState?.getBoolean("isTopBarVisible", true) ?: true
        isTopBarTitleEnabled = savedInstanceState?.getBoolean("isTopBarTitleEnabled", true) ?: true
        android.util.Log.d("MainActivity", "🔧 初始TopBar状态: visible=$isTopBarVisible, titleEnabled=$isTopBarTitleEnabled")

        // 初始化导航组件
        setupNavigation(savedInstanceState)
        
        // 处理通知点击导航
        handleNotificationNavigation()
        
        // 检查并申请通知权限
        checkAndRequestNotificationPermission()
        
        // 检查并显示版本更新日志
        checkAndShowUpdateLog()
        
        // 设置现代返回键处理
        setupBackPressedCallback()
        
        // 注意：新架构不再需要Activity级别的ViewModel和导航监听器
        // 每个Fragment都有自己独立的ViewModel，避免数据污染
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isTopBarVisible", isTopBarVisible)
        outState.putBoolean("isTopBarTitleEnabled", isTopBarTitleEnabled)
    }
    
    /**
     * 启用 Edge-to-Edge 显示
     */
    private fun enableEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // 设置系统栏颜色为透明，避免白条
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        
        // Android 11+ 更精确的控制
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
    }
    
    /**
     * 处理系统窗口内边距
     */
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            // 给AppBarLayout设置顶部内边距（当它可见时）
            if (binding.appBarLayout.visibility == android.view.View.VISIBLE) {
                binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            }
            
            // 根据TopBar可见性动态调整Fragment容器内边距
            adjustFragmentPadding(systemBars.top)
            
            // 底部导航栏的内边距设置 - 精简优化
            binding.navView.setPadding(
                systemBars.left, 
                0, 
                systemBars.right, 
                navigationBars.bottom  // 直接设置底部内边距，避免margin增加高度
            )
            
            // 侧边栏适配系统UI
            binding.navDrawerView.setPadding(0, systemBars.top, 0, 0)
            
            insets
        }
    }
    
    /**
     * 设置导航组件
     */
    private fun setupNavigation(savedInstanceState: Bundle?) {
        drawerLayout = binding.drawerLayout
        navController = findNavController(R.id.nav_host_fragment)
        
        // 禁用侧边栏滑动手势
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_warehouse,
                R.id.navigation_function,
                R.id.navigation_profile,
                R.id.nav_category,
                R.id.nav_settings
            )
            // 移除drawerLayout参数，不再支持侧边栏导航
            // 移除 R.id.navigation_item_list 以显示返回按钮
        )

        // 设置ActionBar和NavigationView
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // 设置导航监听器，动态显示/隐藏TopBar
        setupNavigationListener()
        
        // 🔧 修复主题切换后ActionBar消失的问题：手动触发当前目的地的ActionBar状态
        navController.currentDestination?.let { destination ->
            android.util.Log.d("MainActivity", "🎯 setupNavigation: 当前目的地=${resources.getResourceEntryName(destination.id)}")
            // 检测是否是Activity重建（从savedInstanceState恢复）
            val isRecreated = savedInstanceState != null
            android.util.Log.d("MainActivity", "🎯 setupNavigation: isRecreated=$isRecreated")
            
            // 根据当前目的地设置ActionBar状态
            when (destination.id) {
                R.id.navigation_home,
                R.id.navigation_warehouse,
                R.id.navigation_profile,
                R.id.navigation_function -> {
                    android.util.Log.d("MainActivity", "📍 setupNavigation: 主页面，隐藏TopBar")
                    hideTopBar()
                }
                R.id.addItemFragment -> {
                    android.util.Log.d("MainActivity", "📍 setupNavigation: 添加页面，显示TopBar")
                    showTopBar()
                }
                else -> {
                    android.util.Log.d("MainActivity", "📍 setupNavigation: 其他页面，显示TopBar")
                    // 🔧 Activity重建时强制刷新，确保ActionBar正确显示
                    showTopBar(forceRefresh = isRecreated)
                }
            }
        }
        
        // 保留侧边栏设置代码但不激活
        // binding.navDrawerView.setupWithNavController(navController)
        
        // 设置Material 3底部导航栏
        setupMaterial3BottomNavigation()
        
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * 设置Material 3底部导航栏
     */
    private fun setupMaterial3BottomNavigation() {
        // Material 3 特定优化
        with(binding.navView) {
            // 确保使用selected模式 - 只有选中的项显示文字
            labelVisibilityMode = com.google.android.material.bottomnavigation.BottomNavigationView.LABEL_VISIBILITY_SELECTED
            
            // 自定义项目选择监听器，处理加号按钮的特殊行为
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_add_item -> {
                        // 点击加号按钮，使用默认模板导航到添加物品页面
                        val defaultTemplateId = com.example.itemmanagement.utils.TemplatePreferences.getDefaultTemplateId(this@MainActivity)
                        val bundle = androidx.core.os.bundleOf("templateId" to defaultTemplateId)
                        navController.navigate(R.id.addItemFragment, bundle)
                        // 返回false，不让底部导航栏切换选中状态
                        false
                    }
                    else -> {
                        // 其他导航项使用Navigation Component的标准行为
                        androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                    }
                }
            }
            
            // 首次使用时显示气泡提示
            showFirstTimeTipIfNeeded()
            
            // 启用项目重选监听（点击当前选中项的行为）
            setOnItemReselectedListener { item ->
                // 在当前页面时点击导航项，可以滚动到顶部或刷新
                when (item.itemId) {
                    R.id.navigation_home -> {
                        // 首页重选：可以滚动到顶部
                        navController.popBackStack(R.id.navigation_home, false)
                    }
                    R.id.navigation_warehouse -> {
                        // 仓库重选：可以重置筛选
                        navController.popBackStack(R.id.navigation_warehouse, false)
                    }
                    R.id.navigation_function -> {
                        // 功能重选：返回功能首页
                        navController.popBackStack(R.id.navigation_function, false)
                    }
                    R.id.navigation_profile -> {
                        // 我的重选：返回个人页面顶部
                        navController.popBackStack(R.id.navigation_profile, false)
                    }
                    // 加号按钮重选时什么都不做
                    R.id.navigation_add_item -> { }
                }
            }
            
            // 🎯 为"添加"按钮添加长按支持
            setupAddButtonLongPress()
            
            // Material 3 动画优化
            itemRippleColor = androidx.core.content.ContextCompat.getColorStateList(
                this@MainActivity, 
                R.color.material3_nav_item_color
            )
        }
    }
    
    /**
     * 设置现代返回键处理
     */
    private fun setupBackPressedCallback() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // 使用现代导航处理
                    if (!navController.navigateUp(appBarConfiguration)) {
                        finish()
                    }
                }
            }
        })
    }

    /**
     * 设置导航监听器，动态显示/隐藏TopBar
     */
    private fun setupNavigationListener() {
        // 追踪上一个目的地，用于检测是否从添加/编辑页面返回
        var previousDestinationId: Int? = null
        
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val destName = try { 
                resources.getResourceEntryName(destination.id) 
            } catch (e: Exception) { 
                "unknown_${destination.id}" 
            }
            android.util.Log.d("MainActivity", "════════════════════════════════════════")
            android.util.Log.d("MainActivity", "🧭 导航监听器触发 - 导航到: $destName")
            
            // 检查底部导航栏状态
            val navView = binding.navView
            android.util.Log.d("MainActivity", "   📊 底部导航栏当前状态: ${visibilityToString(navView.visibility)}")
            
            // 检查是否从添加/编辑/详情页面返回到首页，如果是则刷新首页
            if (destination.id == R.id.navigation_home && previousDestinationId != null) {
                when (previousDestinationId) {
                    R.id.addItemFragment,
                    R.id.editItemFragment,
                    R.id.navigation_item_detail -> {
                        android.util.Log.d("MainActivity", "  🔄 从物品操作页面返回首页，触发刷新")
                        // 获取HomeFragment并刷新数据
                        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                        val currentFragment = navHostFragment?.childFragmentManager?.primaryNavigationFragment
                        if (currentFragment is com.example.itemmanagement.ui.home.HomeFragment) {
                            currentFragment.refreshData()
                        }
                    }
                }
            }
            
            when (destination.id) {
                // 主要导航页面 - 隐藏TopBar
                R.id.navigation_home,
                R.id.navigation_warehouse,
                R.id.navigation_profile -> {
                    android.util.Log.d("MainActivity", "  ➡️ 主页面，隐藏TopBar")
                    hideTopBar()
                }
                // 功能页面 - 隐藏TopBar（像首页一样）
                R.id.navigation_function -> {
                    android.util.Log.d("MainActivity", "  ➡️ 功能页面，隐藏TopBar")
                    hideTopBar()
                }
                // 添加物品页面 - 显示TopBar
                R.id.addItemFragment -> {
                    android.util.Log.d("MainActivity", "  ➡️ 添加页面，显示TopBar")
                    showTopBar()
                }
                // 地图查看页面 - 显示TopBar，Fragment自己会隐藏底部导航
                R.id.navigation_map_viewer -> {
                    android.util.Log.d("MainActivity", "  ➡️ 地图查看页面，显示TopBar")
                    android.util.Log.d("MainActivity", "     （底部导航栏由 MapViewerFragment 自行控制）")
                    showTopBar()
                }
                // 地图选点页面 - 显示TopBar，Fragment自己会隐藏底部导航
                R.id.navigation_map_picker -> {
                    android.util.Log.d("MainActivity", "  ➡️ 地图选点页面，显示TopBar")
                    android.util.Log.d("MainActivity", "     （底部导航栏由 MapPickerFragment 自行控制）")
                    showTopBar()
                }
                // 其他页面 - 显示TopBar
                else -> {
                    android.util.Log.d("MainActivity", "  ➡️ 其他页面($destName)，显示TopBar")
                    showTopBar()
                }
            }
            
            // 延迟检查底部导航栏状态
            binding.navView.postDelayed({
                android.util.Log.d("MainActivity", "   🔍 [100ms后检查] 底部导航栏状态: ${visibilityToString(binding.navView.visibility)}")
            }, 100)
            
            binding.navView.postDelayed({
                android.util.Log.d("MainActivity", "   🔍 [300ms后检查] 底部导航栏状态: ${visibilityToString(binding.navView.visibility)}")
            }, 300)
            
            // 记录当前目的地，作为下次的previous
            previousDestinationId = destination.id
            android.util.Log.d("MainActivity", "════════════════════════════════════════")
        }
    }
    
    private fun visibilityToString(visibility: Int): String {
        return when (visibility) {
            android.view.View.VISIBLE -> "VISIBLE"
            android.view.View.INVISIBLE -> "INVISIBLE"
            android.view.View.GONE -> "GONE"
            else -> "UNKNOWN($visibility)"
        }
    }
    
    /**
     * 显示TopBar
     */
    private fun showTopBar(forceRefresh: Boolean = false) {
        android.util.Log.d("MainActivity", "👁️ showTopBar called, 当前状态: visible=$isTopBarVisible, forceRefresh=$forceRefresh")
        // ✅ 只在状态发生变化时才执行操作，避免重复导致的闪现
        if (!isTopBarVisible || forceRefresh) {
            if (forceRefresh) {
                android.util.Log.d("MainActivity", "  🔄 强制刷新TopBar状态")
            } else {
                android.util.Log.d("MainActivity", "  ✅ TopBar从隐藏变为可见")
            }
            binding.appBarLayout.visibility = android.view.View.VISIBLE
            isTopBarVisible = true
            updateFragmentConstraints(true)
            
            // 🔧 强制显示ActionBar（修复Activity重建后ActionBar消失的问题）
            supportActionBar?.show()
        } else {
            android.util.Log.d("MainActivity", "  ⏭️ TopBar已经可见，跳过")
        }
        
        // 标题状态单独管理
        if (!isTopBarTitleEnabled || forceRefresh) {
            android.util.Log.d("MainActivity", "  📝 启用TopBar标题")
            supportActionBar?.setDisplayShowTitleEnabled(true)
            isTopBarTitleEnabled = true
        }
        
        // 恢复默认背景色（从透明状态恢复）
        restoreDefaultTopBarBackground()
    }
    
    /**
     * 显示TopBar并设置标题
     */
    private fun showTopBarWithTitle(title: String) {
        // ✅ 只在状态发生变化时才执行操作，避免重复导致的闪现
        if (!isTopBarVisible) {
            binding.appBarLayout.visibility = android.view.View.VISIBLE
            isTopBarVisible = true
            updateFragmentConstraints(true)
        }
        
        // 标题状态和内容管理
        if (!isTopBarTitleEnabled) {
            supportActionBar?.setDisplayShowTitleEnabled(true)
            isTopBarTitleEnabled = true
        }
        supportActionBar?.title = title
        
        // 恢复默认背景色（从透明状态恢复）
        restoreDefaultTopBarBackground()
    }
    
    /**
     * 显示透明TopBar并设置标题（专用于功能界面）
     */
    private fun showTransparentTopBarWithTitle(title: String) {
        // ✅ 只在状态发生变化时才执行操作，避免重复导致的闪现
        if (!isTopBarVisible) {
            binding.appBarLayout.visibility = android.view.View.VISIBLE
            isTopBarVisible = true
            updateFragmentConstraints(true)
        }
        
        // 标题状态和内容管理
        if (!isTopBarTitleEnabled) {
            supportActionBar?.setDisplayShowTitleEnabled(true)
            isTopBarTitleEnabled = true
        }
        supportActionBar?.title = title
        
        // 设置透明背景 - 只影响当前TopBar
        binding.toolbar.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        binding.appBarLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }
    
    /**
     * 恢复TopBar的默认背景色
     */
    private fun restoreDefaultTopBarBackground() {
        // 恢复Toolbar的默认背景色（从样式中获取）
        val typedArray = theme.obtainStyledAttributes(intArrayOf(R.attr.colorSurfaceContainerHigh))
        val defaultColor = typedArray.getColor(0, android.graphics.Color.WHITE)
        typedArray.recycle()
        
        binding.toolbar.setBackgroundColor(defaultColor)
        binding.appBarLayout.setBackgroundColor(defaultColor)
    }
    
    /**
     * 显示TopBar但禁用标题（用于添加物品页面）
     */
    private fun showTopBarWithoutTitle() {
        // ✅ 只在TopBar不可见时才显示，避免重复操作
        if (!isTopBarVisible) {
            binding.appBarLayout.visibility = android.view.View.VISIBLE
            isTopBarVisible = true
            updateFragmentConstraints(true)
        }
        
        // 标题状态管理
        if (isTopBarTitleEnabled) {
            supportActionBar?.setDisplayShowTitleEnabled(false)
            isTopBarTitleEnabled = false
        }
        // 立即清空标题，防止闪现
        supportActionBar?.title = ""
        
        // 恢复默认背景色（从透明状态恢复）
        restoreDefaultTopBarBackground()
    }
    
    /**
     * 隐藏TopBar
     */
    private fun hideTopBar() {
        android.util.Log.d("MainActivity", "🙈 hideTopBar called, 当前状态: visible=$isTopBarVisible")
        // ✅ 只在TopBar可见时才隐藏，避免重复操作导致的闪现
        if (isTopBarVisible) {
            android.util.Log.d("MainActivity", "  ✅ TopBar从可见变为隐藏")
            // 立即清空标题，防止隐藏过程中的闪现
            supportActionBar?.title = ""
            supportActionBar?.setDisplayShowTitleEnabled(false)
            binding.appBarLayout.visibility = android.view.View.GONE
            
            // 更新状态
            isTopBarVisible = false
            isTopBarTitleEnabled = false
            
            // 重新调整Fragment约束
            updateFragmentConstraints(false)
        } else {
            android.util.Log.d("MainActivity", "  ⏭️ TopBar已经隐藏，跳过")
        }
    }
    
    /**
     * 更新Fragment约束
     */
    private fun updateFragmentConstraints(showTopBar: Boolean) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val fragmentView = navHostFragment?.view
        val layoutParams = fragmentView?.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        layoutParams?.let { params ->
            if (showTopBar) {
                params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.topToBottom = R.id.appBarLayout
            } else {
                params.topToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            }
            fragmentView.layoutParams = params
        }
    }
    
    /**
     * 动态调整Fragment容器内边距
     */
    private fun adjustFragmentPadding(statusBarHeight: Int) {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val topPadding = if (binding.appBarLayout.visibility == android.view.View.VISIBLE) 0 else statusBarHeight
        navHostFragment?.view?.setPadding(0, topPadding, 0, 0)
    }

    /**
     * 处理通知点击导航
     */
    private fun handleNotificationNavigation() {
        val navigateTo = intent.getStringExtra("navigate_to")
        if (navigateTo == "expiration_reminder") {
            // 导航到物品日历页面
            navController.navigate(R.id.navigation_item_calendar)
        }
    }
    
    /**
     * 检查并申请通知权限
     */
    fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 权限已获取，无需操作
                }
                else -> {
                    // 申请权限
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    /**
     * 检查并显示版本更新日志
     */
    private fun checkAndShowUpdateLog() {
        // 延迟1秒显示，确保界面已完全加载
        binding.root.postDelayed({
            if (!isFinishing && !isDestroyed) {
                // 先检查在线更新
                checkOnlineUpdate()
                
                // 然后检查本地更新日志（首次安装或更新后）
                val shouldShow = com.example.itemmanagement.utils.VersionUpdateManager.shouldShowUpdateDialog(this)
                if (shouldShow) {
                    val dialog = com.example.itemmanagement.ui.dialog.UpdateLogDialog.newInstance(isFirstLaunch = true)
                    dialog.show(supportFragmentManager, "UpdateLogDialog")
                }
            }
        }, 1000)
    }
    
    /**
     * 检查在线更新
     */
    private fun checkOnlineUpdate() {
        lifecycleScope.launch {
            try {
                val updateInfo = com.example.itemmanagement.utils.OnlineUpdateChecker.checkForUpdate(this@MainActivity)
                if (updateInfo != null) {
                    if (!isFinishing && !isDestroyed) {
                        // 发现新版本，显示更新对话框
                        val dialog = com.example.itemmanagement.ui.dialog.OnlineUpdateDialog.newInstance(updateInfo)
                        dialog.show(supportFragmentManager, "OnlineUpdateDialog")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "检查在线更新失败", e)
                // 静默失败，不影响用户体验
            }
        }
    }
    
    /**
     * 检查通知权限是否已获取
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13以下版本不需要权限
        }
    }
    
    /**
     * 为底部导航栏的"添加"按钮添加长按支持
     */
    private fun setupAddButtonLongPress() {
        binding.navView.post {
            // BottomNavigationView内部结构：
            // BottomNavigationView -> BottomNavigationMenuView -> BottomNavigationItemView[]
            val menuView = binding.navView.getChildAt(0) as? android.view.ViewGroup
            
            menuView?.let { menu ->
                // 遍历所有item，找到"添加"按钮（第3个item，索引为2）
                if (menu.childCount >= 3) {
                    val addItemView = menu.getChildAt(2) as? android.view.ViewGroup
                    
                    // 设置长按监听
                    addItemView?.setOnLongClickListener {
                        // 震动反馈
                        vibrateDevice(50)
                        
                        // 显示模板选择对话框
                        showTemplateSelectionDialog()
                        true
                    }
                    
                    // 🎯 放大"添加"按钮的图标（2倍大小）
                    addItemView?.let { itemView ->
                        // BottomNavigationItemView 内部结构：包含 ImageView (图标)
                        for (i in 0 until itemView.childCount) {
                            val child = itemView.getChildAt(i)
                            if (child is android.widget.ImageView) {
                                // 找到图标，放大2倍
                                child.scaleX = 2.0f
                                child.scaleY = 2.0f
                                android.util.Log.d("MainActivity", "✅ 成功放大添加按钮图标 (2倍)")
                                break
                            }
                        }
                    }
                    
                    android.util.Log.d("MainActivity", "✅ 成功为添加按钮设置长按监听")
                } else {
                    android.util.Log.w("MainActivity", "⚠️ 无法找到添加按钮，childCount=${menu.childCount}")
                }
            } ?: android.util.Log.w("MainActivity", "⚠️ 无法获取BottomNavigationMenuView")
        }
    }
    
    /**
     * 震动反馈
     */
    private fun vibrateDevice(milliseconds: Long) {
        try {
            val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        milliseconds,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(milliseconds)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "震动失败", e)
        }
    }
    
    /**
     * 显示模板选择对话框
     */
    private fun showTemplateSelectionDialog() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
        
        // 创建模板选择对话框
        val dialog = com.example.itemmanagement.ui.template.TemplateSelectionBottomSheet(
            onTemplateSelected = { template ->
                // 用户选择了模板，跳转到添加界面并传递模板ID
                val bundle = androidx.core.os.bundleOf("templateId" to template.id)
                navController.navigate(R.id.addItemFragment, bundle)
            },
            onManageTemplates = {
                // 跳转到模板管理界面
                try {
                    navController.navigate(R.id.action_home_to_template_management)
                } catch (e: Exception) {
                    // 如果当前不在home，直接导航到模板管理
                    navController.navigate(R.id.navigation_template_management)
                }
            }
        )
        
        // 显示对话框
        currentFragment?.childFragmentManager?.let {
            dialog.show(it, "TemplateSelection")
        } ?: run {
            // 如果无法获取当前Fragment，使用Activity的FragmentManager
            dialog.show(supportFragmentManager, "TemplateSelection")
        }
    }
    
    /**
     * 每次打开APP显示气泡提示（除非用户点击了"不再显示"）
     */
    private fun showFirstTimeTipIfNeeded() {
        val prefs = getSharedPreferences("app_tips", MODE_PRIVATE)
        val neverShowAgain = prefs.getBoolean("never_show_add_button_tip", false)
        
        if (!neverShowAgain) {
            // 延迟显示，等待权限请求完成
            binding.navView.postDelayed({
                try {
                    // 查找添加按钮
                    var addItemView: View? = null
                    val menuView = binding.navView.getChildAt(0) as? android.view.ViewGroup
                    if (menuView != null) {
                        for (i in 0 until menuView.childCount) {
                            val itemView = menuView.getChildAt(i)
                            if (itemView.id == R.id.navigation_add_item) {
                                addItemView = itemView
                                break
                            }
                        }
                    }
                    
                    if (addItemView != null) {
                        showTooltipPopover(addItemView)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "显示气泡提示失败", e)
                }
            }, 3000) // 延迟3秒，等待权限请求完成
        }
    }
    
    /**
     * 显示气泡提示框（带"不再显示"按钮）
     */
    private fun showTooltipPopover(anchorView: View) {
        // 创建自定义气泡视图（使用LinearLayout容纳文本和按钮）
        val tooltipView = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(48, 32, 48, 32)
            gravity = android.view.Gravity.CENTER_VERTICAL
            
            // 设置圆角背景
            background = resources.getDrawable(R.drawable.bg_dialog_rounded, theme).apply {
                setTint(getColor(com.google.android.material.R.color.material_blue_grey_900))
            }
            elevation = 16f
            
            // 提示文本
            addView(android.widget.TextView(this@MainActivity).apply {
                text = "💡 长按「+」可编辑添加物品模板"
                setTextColor(getColor(android.R.color.white))
                textSize = 14f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })
            
            // 不再显示按钮
            addView(android.widget.TextView(this@MainActivity).apply {
                text = "不再显示"
                setTextColor(getColor(com.google.android.material.R.color.design_default_color_secondary))
                textSize = 12f
                setPadding(32, 0, 0, 0)
                gravity = android.view.Gravity.CENTER
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                // 添加点击效果
                isClickable = true
                isFocusable = true
                background = android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(getColor(android.R.color.white)),
                    null,
                    null
                )
            })
        }
        
        // 创建 PopupWindow
        val popupWindow = android.widget.PopupWindow(
            tooltipView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            isOutsideTouchable = true
            isFocusable = false // 不抢焦点，允许用户继续操作
        }
        
        // 设置"不再显示"按钮点击事件
        tooltipView.getChildAt(1).setOnClickListener {
            // 保存"不再显示"设置
            val prefs = getSharedPreferences("app_tips", MODE_PRIVATE)
            prefs.edit().putBoolean("never_show_add_button_tip", true).apply()
            android.util.Log.d("MainActivity", "用户选择不再显示模板提示")
            
            // 关闭气泡
            popupWindow.dismiss()
        }
        
        // 计算显示位置（在按钮上方）
        anchorView.post {
            val location = IntArray(2)
            anchorView.getLocationOnScreen(location)
            
            // 测量 tooltip 大小
            tooltipView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            
            val xOffset = (anchorView.width - tooltipView.measuredWidth) / 2
            val yOffset = -tooltipView.measuredHeight - anchorView.height - 48 // 在按钮上方更高的位置
            
            popupWindow.showAsDropDown(anchorView, xOffset, yOffset)
            
            // 5秒后自动消失
            anchorView.postDelayed({
                if (popupWindow.isShowing) {
                    popupWindow.dismiss()
                }
            }, 5000)
        }
    }
    
} 