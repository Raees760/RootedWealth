package com.st10321779.rootedwealth.theme

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class AppTheme(
    val id: String,
    val displayName: String,
    @ColorRes val primaryColorRes: Int,
    @ColorRes val accentColorRes: Int,
    @ColorRes val textColorRes: Int,
    @DrawableRes val panelDrawableRes: Int?, // if null use plain color
    val panelUsesImage: Boolean = false,
    val darkOverlayAlpha: Float = 0f // 0-1 overlay black alpha applied in dark mode
)
