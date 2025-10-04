package com.st10321779.rootedwealth.theme

import com.st10321779.rootedwealth.R

object ThemeRepository {

    val DEFAULT = AppTheme(
        id = "default",
        displayName = "Professional",
        primaryColorRes = R.color.primary_default,
        accentColorRes = R.color.accent_default,
        textColorRes = R.color.text_default,
        panelDrawableRes = null,
        panelUsesImage = false,
        darkOverlayAlpha = 0.15f
    )

    val ROSY = AppTheme(
        id = "rosy",
        displayName = "Rosy",
        primaryColorRes = R.color.rosy_panel,
        accentColorRes = R.color.rosy_accent,
        textColorRes = R.color.text_on_rosy,
        panelDrawableRes = null,
        panelUsesImage = false,
        darkOverlayAlpha = 0.12f
    )

    val SPIDERMAN = AppTheme(
        id = "spiderman",
        displayName = "Spider-Man",
        primaryColorRes = R.color.spiderman_fallback,
        accentColorRes = R.color.spiderman_accent,
        textColorRes = R.color.text_on_spiderman,
        panelDrawableRes = R.drawable.spiderman_panel_bg, // image in drawable
        panelUsesImage = true,
        darkOverlayAlpha = 0.22f
    )

    val all = listOf(DEFAULT, ROSY, SPIDERMAN)

    fun findById(id: String): AppTheme {
        return all.firstOrNull { it.id == id } ?: DEFAULT
    }
}
