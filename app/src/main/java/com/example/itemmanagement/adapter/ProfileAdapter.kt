package com.example.itemmanagement.adapter

import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.itemmanagement.R
import com.example.itemmanagement.data.entity.UserProfileEntity
import com.example.itemmanagement.data.model.ProfileItem
import com.example.itemmanagement.databinding.ItemProfileMenuRowBinding
import com.example.itemmanagement.databinding.ItemProfileSpacerBinding
import com.example.itemmanagement.databinding.ItemProfileUserInfoBinding

class ProfileAdapter(
    private val onMenuItemClick: (String) -> Unit,
    private val onUserInfoClick: () -> Unit
) : ListAdapter<ProfileItem, RecyclerView.ViewHolder>(ProfileDiffCallback()) {

    companion object {
        private const val TYPE_USER_INFO = 0
        private const val TYPE_MENU_ITEM = 1
        private const val TYPE_SPACER = 2
    }

    private var userProfile: UserProfileEntity? = null

    fun updateUserProfile(profile: UserProfileEntity) {
        userProfile = profile
        // 找到UserInfo项并刷新
        val userInfoIndex = currentList.indexOfFirst { it is ProfileItem.UserInfo }
        if (userInfoIndex != -1) {
            notifyItemChanged(userInfoIndex)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ProfileItem.UserInfo -> TYPE_USER_INFO
            is ProfileItem.MenuItem -> TYPE_MENU_ITEM
            is ProfileItem.MenuSpacer -> TYPE_SPACER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER_INFO -> {
                val binding = ItemProfileUserInfoBinding.inflate(inflater, parent, false)
                UserInfoViewHolder(binding)
            }
            TYPE_MENU_ITEM -> {
                val binding = ItemProfileMenuRowBinding.inflate(inflater, parent, false)
                MenuItemViewHolder(binding)
            }
            TYPE_SPACER -> {
                val binding = ItemProfileSpacerBinding.inflate(inflater, parent, false)
                SpacerViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserInfoViewHolder -> {
                userProfile?.let { holder.bind(it) }
            }
            is MenuItemViewHolder -> {
                val item = getItem(position) as ProfileItem.MenuItem
                holder.bind(item)
            }
            is SpacerViewHolder -> {
                // Spacer不需要绑定数据
            }
        }
    }

    inner class UserInfoViewHolder(
        private val binding: ItemProfileUserInfoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onUserInfoClick()
            }
        }

        fun bind(profile: UserProfileEntity) {
            // 设置昵称
            binding.tvNickname.text = profile.nickname
            
            // 设置用户ID（9位数字，不带#号）
            binding.tvUserId.text = "用户ID：${profile.userId}"
            
            // 设置个性签名（直接显示内容，不加前缀）
            if (!profile.signature.isNullOrEmpty()) {
                binding.tvSignature.text = profile.signature
                binding.tvSignature.visibility = View.VISIBLE
            } else {
                binding.tvSignature.visibility = View.GONE
            }
            
            // 设置头像
            if (!profile.avatarUri.isNullOrEmpty()) {
                try {
                    val uri = Uri.parse(profile.avatarUri)
                    Glide.with(binding.root.context)
                        .load(uri)
                        .placeholder(R.drawable.ic_person_placeholder)
                        .error(R.drawable.ic_person_placeholder)
                        .circleCrop()
                        .into(binding.ivUserAvatar)
                } catch (e: Exception) {
                    setDefaultAvatar()
                }
            } else {
                setDefaultAvatar()
            }
        }
        
        private fun setDefaultAvatar() {
            Glide.with(binding.root.context)
                .load(R.drawable.ic_person_placeholder)
                .circleCrop()
                .into(binding.ivUserAvatar)
        }
    }

    inner class MenuItemViewHolder(
        private val binding: ItemProfileMenuRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ProfileItem.MenuItem) {
            binding.menuIcon.setImageResource(item.iconRes)
            binding.menuTitle.text = item.title
            binding.divider.visibility = if (item.showDivider) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onMenuItemClick(item.id)
            }
        }
    }

    inner class SpacerViewHolder(
        binding: ItemProfileSpacerBinding
    ) : RecyclerView.ViewHolder(binding.root)
}

class ProfileDiffCallback : DiffUtil.ItemCallback<ProfileItem>() {
    override fun areItemsTheSame(oldItem: ProfileItem, newItem: ProfileItem): Boolean {
        return when {
            oldItem is ProfileItem.UserInfo && newItem is ProfileItem.UserInfo -> true
            oldItem is ProfileItem.MenuSpacer && newItem is ProfileItem.MenuSpacer -> true
            oldItem is ProfileItem.MenuItem && newItem is ProfileItem.MenuItem -> 
                oldItem.id == newItem.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: ProfileItem, newItem: ProfileItem): Boolean {
        return oldItem == newItem
    }
}
