package com.st10321779.rootedwealth.ui.rewards

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.st10321779.rootedwealth.databinding.ItemRewardsThemeBinding
import com.st10321779.rootedwealth.theme.ThemeRepository

class ThemeAdapter(
    private val themes: List<RewardTheme>,
    private var purchasedThemeIds: List<String>, // Now a mutable property
    private val onBuyClicked: (RewardTheme) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    inner class ThemeViewHolder(val binding: ItemRewardsThemeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemRewardsThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeViewHolder(binding)
    }

    override fun getItemCount(): Int = themes.size

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themes[position]

        with(holder.binding) {
            tvThemeName.text = theme.name

            val themeData = ThemeRepository.findById(theme.id)
            if (themeData.panelUsesImage && themeData.panelDrawableRes != null) {
                ivThemePreview.setImageResource(themeData.panelDrawableRes)
            } else {
                ivThemePreview.setImageResource(themeData.primaryColorRes)
            }

            // The check is now against the property passed into the adapter
            if (theme.id in purchasedThemeIds) {
                btnBuyTheme.isEnabled = false
                btnBuyTheme.text = "Owned"
                tvThemePrice.visibility = View.GONE
            } else {
                btnBuyTheme.isEnabled = true
                btnBuyTheme.text = "Buy"
                tvThemePrice.visibility = View.VISIBLE
                tvThemePrice.text = "💰 ${theme.price} Coins"
                btnBuyTheme.setOnClickListener { onBuyClicked(theme) }
            }
        }
    }

    // New function to update the list from the Fragment's observer
    fun updatePurchasedThemes(newPurchasedIds: List<String>) {
        this.purchasedThemeIds = newPurchasedIds
        notifyDataSetChanged() // Redraw the entire list
    }
}