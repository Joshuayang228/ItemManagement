package com.example.itemmanagement.data.model

import androidx.annotation.DrawableRes

/**
 * Profile界面的列表项数据模型
 */
sealed class ProfileItem {
    /**
     * 用户信息卡片
     */
    object UserInfo : ProfileItem()
    
    /**
     * 间隔项
     */
    object MenuSpacer : ProfileItem()
    
    /**
     * 菜单项
     */
    data class MenuItem(
        val id: String,
        val title: String,
        @DrawableRes val iconRes: Int,
        val showDivider: Boolean = true
    ) : ProfileItem()
}
