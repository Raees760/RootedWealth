package com.st10321779.rootedwealth.ui.rewards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.st10321779.rootedwealth.R
import com.st10321779.rootedwealth.databinding.ItemRewardsThemeBinding
import com.st10321779.rootedwealth.theme.ThemeRepository
import com.st10321779.rootedwealth.util.PrefsManager

class ThemeAdapter(
    private val themes: List<RewardTheme>,
    private val onBuyClicked: (RewardTheme) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    inner class ThemeViewHolder(val binding: ItemRewardsThemeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemRewardsThemeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThemeViewHolder(binding)
    }

    override fun getItemCount(): Int = themes.size

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themes[position]
        val context = holder.itemView.context
        val purchasedThemes = PrefsManager.getPurchasedThemes(context)

        with(holder.binding) {
            tvThemeName.text = theme.name

            // Set a representative image for each theme
            val themeData = ThemeRepository.findById(theme.id)
            if (themeData.panelUsesImage && themeData.panelDrawableRes != null) {
                ivThemePreview.setImageResource(themeData.panelDrawableRes)
            } else {
                ivThemePreview.setImageResource(themeData.primaryColorRes)
            }


            if (theme.id in purchasedThemes) {
                btnBuyTheme.isEnabled = false
                btnBuyTheme.text = "Owned"
                tvThemePrice.visibility = View.GONE
            } else {
                btnBuyTheme.isEnabled = true
                btnBuyTheme.text = "Buy"
                tvThemePrice.visibility = View.VISIBLE
                tvThemePrice.text = "💰 ${theme.price} Coins"
            }

            btnBuyTheme.setOnClickListener {
                onBuyClicked(theme)
            }
        }
    }
}