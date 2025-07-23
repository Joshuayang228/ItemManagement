package com.example.itemmanagement.ui.add

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentAddItemBinding
import com.example.itemmanagement.data.model.Item
import com.example.itemmanagement.data.model.ItemStatus
import com.example.itemmanagement.data.model.Location
import com.example.itemmanagement.data.model.OpenStatus
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.viewModelScope
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.GridLayoutManager.LayoutParams
import com.bumptech.glide.Glide
import android.graphics.Rect
import android.widget.ImageView
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import android.util.Log
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.example.itemmanagement.ui.base.BaseItemFragment
import com.example.itemmanagement.ui.add.ItemMode

/**
 * 添加物品Fragment
 * 继承自BaseItemFragment，专门用于添加新物品
 */
class AddItemFragment : BaseItemFragment() {

    private lateinit var photoManager: PhotoManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化照片管理器
        photoManager = PhotoManager(this, viewModel, dialogFactory)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 确保在添加模式下，并且有默认字段
        if (!viewModel.isViewModelInitialized() || viewModel.mode.value != ItemMode.ADD) {
            viewModel.prepareForAddMode()
        }
        
        // 如果没有选中的字段，则初始化默认字段
        if (viewModel.getSelectedFieldsValue().isEmpty()) {
            viewModel.prepareForAddMode()
        }
    }

    override fun initializeMode(savedInstanceState: Bundle?) {
        // 处理导航参数
        arguments?.let { args ->
            // 获取模式参数
            val mode = args.getString("mode") ?: "add"
            // 设置模式
            viewModel.setMode(mode)
        } ?: run {
            // 如果没有参数，默认为添加模式
            viewModel.setMode("add")
        }
    }

    override fun setupTitleAndButtons() {
        // 设置标题
        requireActivity().title = getString(R.string.title_add_item)
        
        // 设置保存按钮文本
                binding.saveButton.text = "保存物品"
            }

    override fun observeSaveResult() {
        // 观察保存结果
        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
                
                // 清除所有数据和草稿，确保下次进入是全新状态
                viewModel.clearAllData()
                
                // 导航回上一个页面
                findNavController().navigateUp()
                // 重置保存结果的状态，防止再次进入时触发
                viewModel.onSaveResultConsumed()
            }
        }
    }

    override fun getItemId(): Long {
        // 添加模式下返回0，表示新物品
        return 0L
    }

    // 重写照片相关方法，使用PhotoManager
    override fun showPhotoSelectionDialog() {
        photoManager.showPhotoSelectionDialog(
            checkAndRequestCameraPermission = { checkAndRequestCameraPermission() },
            checkAndRequestStoragePermission = { checkAndRequestStoragePermission() }
        )
    }

    override fun launchCamera() {
        photoManager.launchCamera { uri ->
            takePicture.launch(uri)
        }
    }

    override fun showPhotoViewDialog(uri: android.net.Uri) {
        photoManager.showPhotoViewDialog(uri)
    }

    // 重写权限请求结果处理
    override val takePicture = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        photoManager.handleCameraResult(success, isFragmentActive = { isFragmentActive() })
    }

    override val pickImage = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        photoManager.handleGalleryResult(uri, isFragmentActive = { isFragmentActive() })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_add_item, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_scan -> {
                Toast.makeText(context, "扫描功能将在后续版本实现", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_camera -> {
                showPhotoSelectionDialog()
                true
            }
            R.id.action_clear -> {
                showClearConfirmDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showClearConfirmDialog() {
        dialogFactory.createConfirmDialog(
            title = "清除数据",
            message = "确定要清除所有已填写的数据吗？",
            positiveButtonText = "确定",
            negativeButtonText = "取消",
            onPositiveClick = {
                viewModel.clearAllData()
                viewModel.prepareForAddMode()
                Toast.makeText(context, "已清除所有数据", Toast.LENGTH_SHORT).show()
            }
        )
    }
}