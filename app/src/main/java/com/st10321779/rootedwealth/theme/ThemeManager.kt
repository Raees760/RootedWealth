package com.st10321779.rootedwealth.theme

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
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

    /**
     * Apply theme to the provided root view. Views must be tagged with logical tags:
     * "themed_panel", "themed_accent", "themed_text", "themed_background_image".
     */

    fun applyTheme(context: Context, theme: AppTheme, root: View) {
        var primary = ContextCompat.getColor(context, theme.primaryColorRes)
        var accent = ContextCompat.getColor(context, theme.accentColorRes)
        var text = ContextCompat.getColor(context, theme.textColorRes)
        val darkMode = isDarkMode(context)

        if (darkMode && !theme.panelUsesImage) {
            // Apply generic dark mode effect: darken colors
            primary = Color.rgb((Color.red(primary) * 0.2f).toInt(), (Color.green(primary) * 0.2f).toInt(), (Color.blue(primary) * 0.2f).toInt())
            accent = Color.rgb((Color.red(accent) * 0.4f).toInt(), (Color.green(accent) * 0.4f).toInt(), (Color.blue(accent) * 0.4f).toInt())
            text = Color.WHITE // Assume white text on dark backgrounds
        }

        // A semi-transparent version of the primary color for panels
        val panelColor = Color.argb(230, Color.red(primary), Color.green(primary), Color.blue(primary))

        fun applyToView(v: View) {
            val tag = v.tag as? String ?: return
                when (tag) {
                    "themed_background" -> {
                        // This view is the main container for the screen.
                        // Apply a solid color, never an image.
                        v.setBackgroundColor(primary)
                    }

                    "themed_panel" -> {
                        // For cards and interactive panels.
                        if (v is com.google.android.material.card.MaterialCardView) {
                            // Use MaterialCardView for stroke properties
                            v.setBackgroundColor(panelColor)
                            v.strokeWidth = 2 // This will now resolve
                            v.strokeColor = accent // This will now resolve
                        } else {
                            // Fallback for regular views/layouts
                            v.setBackgroundColor(panelColor)
                        }
                    }

                    "themed_accent" -> v.setBackgroundColor(accent)
                    "themed_text" -> if (v is TextView) v.setTextColor(text)
                    "themed_input" -> {
                        // For EditText, Spinners etc.
                        if (v is EditText) {
                            v.setTextColor(text)
                            v.setHintTextColor(
                                Color.argb(
                                    150,
                                    Color.red(text),
                                    Color.green(text),
                                    Color.blue(text)
                                )
                            )
                        }
                    }

                    "themed_background_image" -> {
                        // This is ONLY for a dedicated ImageView that sits behind everything.
                        if (theme.panelUsesImage && theme.panelDrawableRes != null && v is ImageView) {
                            v.visibility = View.VISIBLE
                            v.setImageResource(theme.panelDrawableRes)
                            v.scaleType = ImageView.ScaleType.CENTER_CROP // To fill the screen
                            if (darkMode && theme.darkOverlayAlpha > 0f) {
                                v.setColorFilter(
                                    Color.argb((255 * theme.darkOverlayAlpha).toInt(), 0, 0, 0),
                                    PorterDuff.Mode.SRC_ATOP
                                )
                            } else {
                                v.clearColorFilter()
                            }
                        } else if (v is ImageView) {
                            v.visibility = View.GONE
                            v.setImageDrawable(null)
                        }
                    }
                }

                // BFS traversal of view tree
                val queue = ArrayDeque<View>()
                queue.add(root)
                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    applyToView(current)
                    if (current is ViewGroup) {
                        for (i in 0 until current.childCount) {
                            queue.add(current.getChildAt(i))
                        }
                    }
                }
            }
    }
}
