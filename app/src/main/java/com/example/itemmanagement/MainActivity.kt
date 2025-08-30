package com.example.itemmanagement

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置工具栏
        setSupportActionBar(binding.toolbar)

        // 初始化导航组件
        drawerLayout = binding.drawerLayout
        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_item_list,
                R.id.navigation_warehouse,
                R.id.navigation_function,
                R.id.navigation_profile,
                R.id.nav_category,
                R.id.nav_settings
            ),
            drawerLayout
        )

        // 设置ActionBar和NavigationView
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // 处理通知点击导航
        handleNotificationNavigation()
        
        // 设置侧边栏导航
        binding.navDrawerView.setupWithNavController(navController)
        
        // 设置底部导航栏
        binding.navView.setupWithNavController(navController)
        
        // 检查并申请通知权限
        checkAndRequestNotificationPermission()
        
        // 注意：新架构不再需要Activity级别的ViewModel和导航监听器
        // 每个Fragment都有自己独立的ViewModel，避免数据污染
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
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