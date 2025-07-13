package com.example.itemmanagement

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
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
import com.example.itemmanagement.ui.add.AddItemViewModel
import com.example.itemmanagement.ui.add.AddItemViewModelFactory
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var viewModel: AddItemViewModel

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
                R.id.navigation_statistics,
                R.id.navigation_profile,
                R.id.nav_category,
                R.id.nav_settings
            ),
            drawerLayout
        )

        // 设置ActionBar和NavigationView
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // 设置侧边栏导航
        binding.navDrawerView.setupWithNavController(navController)
        
        // 设置底部导航栏
        binding.navView.setupWithNavController(navController)
        
        // 初始化ViewModel
        val repository = (application as ItemManagementApplication).repository
        val factory = AddItemViewModelFactory(repository, this)
        viewModel = ViewModelProvider(this, factory)[AddItemViewModel::class.java]
        
        // 监听导航变化
        setupNavigationListener()
    }
    
    private fun setupNavigationListener() {
        // 监听导航目的地变化
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            // 当导航到添加物品页面时，如果是从主页过来且当前ViewModel处于编辑模式
            if (destination.id == R.id.addItemFragment) {
                val mode = arguments?.getString("mode") ?: "add"
                
                // 如果导航到添加物品页面，但没有指定mode或mode是add，并且当前是编辑模式
                // 这表示用户从编辑页面返回到主页后又点击了添加按钮
                if (mode == "add" && viewModel.isInEditMode()) {
                    // 从编辑模式切换回添加模式，恢复之前的草稿
                    viewModel.returnFromEditToAdd()
                }
            }
        }
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
} 