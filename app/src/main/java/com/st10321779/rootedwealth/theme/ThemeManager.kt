package com.st10321779.rootedwealth.theme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.st10321779.rootedwealth.R

object ThemeManager {
    private const val PREFS = "theme_prefs"
    private const val KEY_THEME = "selected_theme"
    private const val KEY_DARK = "dark_mode"

    fun saveSelectedTheme(context: Context, themeId: String) {
        prefs(context).edit().putString(KEY_THEME, themeId).apply()
    }
    fun getSelectedTheme(context: Context): AppTheme = ThemeRepository.findById(prefs(context).getString(KEY_THEME, ThemeRepository.DEFAULT.id)!!)

    fun saveDarkMode(context: Context, dark: Boolean){
        prefs(context).edit().putBoolean(KEY_DARK, dark).apply()
    }
    fun isDarkMode(context: Context): Boolean = prefs(context).getBoolean(KEY_DARK, false)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun applyTheme(context: Context, theme: AppTheme, root: View) {
        var primary = ContextCompat.getColor(context, theme.primaryColorRes)
        var accent = ContextCompat.getColor(context, theme.accentColorRes)
        var text = ContextCompat.getColor(context, theme.textColorRes)
        val darkMode = isDarkMode(context)

        if (darkMode) {
            // Apply a darkening effect for dark mode.
            // for image themes, this is handled by the overlay on the image view.
            if (!theme.panelUsesImage) {
                primary = Color.rgb((Color.red(primary) * 0.2f).toInt(), (Color.green(primary) * 0.2f).toInt(), (Color.blue(primary) * 0.2f).toInt())
                accent = Color.rgb((Color.red(accent) * 0.4f).toInt(), (Color.green(accent) * 0.4f).toInt(), (Color.blue(accent) * 0.4f).toInt())
            }
            // in dark mode, text should generally be white for contrast, unless the theme is an image theme.
            if (!theme.panelUsesImage) {
                text = Color.WHITE
            }
        }

        //use a more transparent panel color for a "frosted glass" effect on image themes
        val panelAlpha = if (theme.panelUsesImage) 150 else 230 // +-60% transparent for images
        val panelColor = Color.argb(panelAlpha, Color.red(primary), Color.green(primary), Color.blue(primary))

        //this is a helper function. called for each view.
        fun applyToView(v: View) {
            val tag = v.tag as? String ?: return
            when (tag) {
                "themed_background" -> {
                    // if the theme uses an image, make the main background transparent
                    // so the ImageView behind it can be seen. Otherwise, set the solid color.
                    if (theme.panelUsesImage) {
                        v.setBackgroundColor(Color.TRANSPARENT)
                    } else {
                        v.setBackgroundColor(primary)
                    }
                }
                "themed_panel" -> {
                    //For carfs, BottomAppBar, etc.
                    //instead of checking for MaterialCardView, we apply a consistent background.
                    //linearLayouts and other views also use this tag.
                    v.setBackgroundColor(panelColor)
                }
                "themed_accent" -> {
                    v.setBackgroundColor(accent)
                    if (v is TextView) {
                        //Ensure text on accent buttons is readable
                        v.setTextColor(text)
                    }
                }
                "themed_text" -> if (v is TextView) v.setTextColor(text)
                "themed_input" -> {
                    if (v is EditText) {
                        v.setTextColor(text)
                        v.setHintTextColor(Color.argb(150, Color.red(text), Color.green(text), Color.blue(text)))
                    }
                }
                "themed_background_image" -> {
                    if (theme.panelUsesImage && theme.panelDrawableRes != null && v is ImageView) {
                        v.visibility = View.VISIBLE
                        v.setImageResource(theme.panelDrawableRes)
                        v.scaleType = ImageView.ScaleType.CENTER_CROP
                        if (darkMode && theme.darkOverlayAlpha > 0f) {
                            v.setColorFilter(Color.argb((255 * theme.darkOverlayAlpha).toInt(), 0, 0, 0), PorterDuff.Mode.SRC_ATOP)
                        } else {
                            v.clearColorFilter()
                        }
                    } else if (v is ImageView) {
                        v.visibility = View.GONE
                        v.setImageDrawable(null)
                    }
                }
            }
        }

        //This is the main logic that goes through every view and applies the theme.
        val queue = ArrayDeque<View>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            applyToView(current) //call the helper function for the current view
            if (current is ViewGroup) {
                for (i in 0 until current.childCount) {
                    queue.add(current.getChildAt(i))
                }
            }
        }
        // ---------------------------------------------------------
    }
}