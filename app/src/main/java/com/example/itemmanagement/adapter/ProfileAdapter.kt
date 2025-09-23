package com.example.itemmanagement.adapter

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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
            binding.tvNickname.text = profile.nickname
            binding.tvLevelTitle.text = "LV.${profile.achievementLevel} ${getLevelTitle(profile.achievementLevel)}"
            binding.tvExperience.text = "${profile.experiencePoints}/1500 EXP"
            
            // 计算经验进度
            val progress = (profile.experiencePoints * 100 / 1500).coerceIn(0, 100)
            binding.progressExperience.progress = progress
        }

        private fun getLevelTitle(level: Int): String {
            return when (level) {
                in 1..2 -> "新手"
                in 3..4 -> "入门"
                in 5..7 -> "管理高手"
                in 8..10 -> "专家"
                else -> "大师"
            }
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
