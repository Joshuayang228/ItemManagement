package com.example.itemmanagement.ui.profile.edit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.itemmanagement.ItemManagementApplication
import com.example.itemmanagement.R
import com.example.itemmanagement.databinding.FragmentEditProfileBinding
import com.example.itemmanagement.utils.ImageCompressor
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 个人资料编辑Fragment
 * 包含头像上传、昵称修改等功能
 */
class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EditProfileViewModel
    private var currentAvatarUri: Uri? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showImagePickerDialog()
        } else {
            Toast.makeText(context, "需要相册权限才能选择头像", Toast.LENGTH_SHORT).show()
        }
    }

    // 选择图片启动器
    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedImage(it) }
    }

    // 拍照启动器  
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentAvatarUri != null) {
            handleSelectedImage(currentAvatarUri!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        val application = requireActivity().application as ItemManagementApplication
        val factory = EditProfileViewModelFactory(application.userProfileRepository)
        viewModel = ViewModelProvider(this, factory).get(EditProfileViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideBottomNavigation()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        hideBottomNavigation()
    }

    private fun setupClickListeners() {
        binding.apply {
            // 头像容器点击
            avatarContainer.setOnClickListener {
                it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                checkPermissionAndPickImage()
            }
            
            // 用户ID点击复制
            layoutUserId.setOnClickListener {
                copyUserIdToClipboard()
            }

            // 保存按钮
            btnSave.setOnClickListener {
                saveProfile()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            profile?.let { updateUI(it) }
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "个人资料已保存", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun updateUI(profile: com.example.itemmanagement.data.entity.UserProfileEntity) {
        binding.apply {
            // 设置昵称
            etNickname.setText(profile.nickname)
            
            // 设置个性签名
            etSignature.setText(profile.signature ?: "")

            // 设置头像
            if (!profile.avatarUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(profile.avatarUri)
                    Glide.with(this@EditProfileFragment)
                        .load(uri)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .circleCrop()
                        .into(ivAvatar)
                } catch (e: Exception) {
                    setDefaultAvatar()
                }
            } else {
                setDefaultAvatar()
            }
            
            // 设置用户ID（9位数字）
            tvUserId.text = profile.userId

            // 设置加入日期
            tvJoinDate.text = dateFormat.format(profile.joinDate)
        }
    }

    private fun setDefaultAvatar() {
        binding.apply {
            Glide.with(this@EditProfileFragment)
                .load(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(ivAvatar)
        }
    }

    private fun checkPermissionAndPickImage() {
        // Android 13 (API 33) 及以上使用新的图片权限
        val permission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED -> {
                showImagePickerDialog()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // 用户之前拒绝过，显示说明
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("需要相册权限")
                    .setMessage("为了选择头像，需要访问您的相册")
                    .setPositiveButton("授权") { _, _ ->
                        requestPermissionLauncher.launch(permission)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            else -> {
                // 首次请求权限
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("从相册选择", "拍照", "取消")
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("选择头像")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectImageLauncher.launch("image/*")
                    1 -> takePicture()
                    // 2 是取消，什么都不做
                }
            }
            .show()
    }

    private fun takePicture() {
        try {
            val photoFile = File(
                requireContext().getExternalFilesDir(null),
                "avatar_${System.currentTimeMillis()}.jpg"
            )
            currentAvatarUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(currentAvatarUri)
        } catch (e: Exception) {
            Toast.makeText(context, "拍照功能暂时不可用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                // 压缩图片
                val compressedUri = ImageCompressor.compressImage(requireContext(), uri, 512, 512, 80)
                
                // 更新UI
                Glide.with(this@EditProfileFragment)
                    .load(compressedUri)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .error(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(binding.ivAvatar)
                
                // 保存URI
                viewModel.updateAvatarUri(compressedUri.toString())
                
                Toast.makeText(context, "头像已更新", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "处理图片失败: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyUserIdToClipboard() {
        val userId = binding.tvUserId.text.toString()
        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("用户ID", userId)
        clipboard.setPrimaryClip(clip)
        
        // 触觉反馈
        binding.layoutUserId.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        
        Toast.makeText(context, "用户ID已复制: $userId", Toast.LENGTH_SHORT).show()
    }
    
    private fun saveProfile() {
        val nickname = binding.etNickname.text.toString().trim()
        val signature = binding.etSignature.text.toString().trim()
        
        if (nickname.isEmpty()) {
            Toast.makeText(context, "请输入昵称", Toast.LENGTH_SHORT).show()
            return
        }

        if (nickname.length > 20) {
            Toast.makeText(context, "昵称不能超过20个字符", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (signature.length > 50) {
            Toast.makeText(context, "个性签名不能超过50个字符", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.updateProfile(nickname, signature.ifEmpty { null })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        showBottomNavigation()
        _binding = null
    }

    /**
     * 隐藏底部导航栏
     */
    private fun hideBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.GONE
    }

    /**
     * 显示底部导航栏
     */
    private fun showBottomNavigation() {
        activity?.findViewById<View>(R.id.nav_view)?.visibility = View.VISIBLE
    }
}
