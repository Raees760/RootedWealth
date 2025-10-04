package com.st10321779.rootedwealth.ui.rewards

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.st10321779.rootedwealth.databinding.ActivityRewardsBinding
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.util.PrefsManager

class RewardsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRewardsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)

        setupViewPager()
        updateCoinBalance()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = RewardsPagerAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Themes"
                1 -> "Coupons"
                else -> null
            }
        }.attach()
    }

    fun updateCoinBalance() {
        val balance = PrefsManager.getCoinBalance(this)
        binding.tvCoinBalance.text = "💰 $balance Coins"
    }
}