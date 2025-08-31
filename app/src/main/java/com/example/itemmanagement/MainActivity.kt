package com.example.itemmanagement

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
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

        // 初始化导航组件
        setupNavigation()
        
        // 处理通知点击导航
        handleNotificationNavigation()
        
        // 检查并申请通知权限
        checkAndRequestNotificationPermission()
        
        // 设置现代返回键处理
        setupBackPressedCallback()
        
        // 注意：新架构不再需要Activity级别的ViewModel和导航监听器
        // 每个Fragment都有自己独立的ViewModel，避免数据污染
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
    private fun setupNavigation() {
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
        // 标准导航设置
        binding.navView.setupWithNavController(navController)
        
        // Material 3 特定优化
        with(binding.navView) {
            // 确保使用selected模式 - 只有选中的项显示文字
            labelVisibilityMode = com.google.android.material.bottomnavigation.BottomNavigationView.LABEL_VISIBILITY_SELECTED
            
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
                }
            }
            
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
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                // 主要导航页面 - 隐藏TopBar
                R.id.navigation_home,
                R.id.navigation_warehouse,
                R.id.navigation_function,
                R.id.navigation_profile -> {
                    hideTopBar()
                }
                // 其他页面 - 显示TopBar
                else -> {
                    showTopBar()
                }
            }
        }
    }
    
    /**
     * 显示TopBar
     */
    private fun showTopBar() {
        binding.appBarLayout.visibility = android.view.View.VISIBLE
        supportActionBar?.setDisplayShowTitleEnabled(true)
        // 重新调整Fragment约束
        updateFragmentConstraints(true)
    }
    
    /**
     * 隐藏TopBar
     */
    private fun hideTopBar() {
        binding.appBarLayout.visibility = android.view.View.GONE
        supportActionBar?.setDisplayShowTitleEnabled(false)
        // 重新调整Fragment约束
        updateFragmentConstraints(false)
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
            // 导航到到期提醒页面
            navController.navigate(R.id.navigation_expiration_reminder)
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
} 