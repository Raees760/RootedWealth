package com.st10321779.rootedwealth.ui.rewards

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class RewardsPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ThemesFragment()
            1 -> CouponsFragment()
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}