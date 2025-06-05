package com.example.itemmanagement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.itemmanagement.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置工具栏
        setSupportActionBar(binding.toolbar)

        // 获取导航控制器
        val navController = findNavController(R.id.nav_host_fragment)

        // 配置顶部应用栏，设置哪些页面显示抽屉图标
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_warehouse,
                R.id.navigation_statistics,
                R.id.navigation_profile,
                R.id.nav_category,
                R.id.nav_settings
            ),
            binding.drawerLayout
        )

        // 设置ActionBar与导航控制器的联动
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 设置底部导航与导航控制器的联动
        binding.navView.setupWithNavController(navController)

        // 设置侧边导航与导航控制器的联动
        binding.navDrawerView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}