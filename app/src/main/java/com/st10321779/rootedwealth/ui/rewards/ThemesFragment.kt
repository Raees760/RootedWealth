package com.st10321779.rootedwealth.ui.rewards

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.st10321779.rootedwealth.databinding.FragmentThemesBinding
import com.st10321779.rootedwealth.theme.ThemeRepository
import com.st10321779.rootedwealth.util.PrefsManager

class ThemesFragment : Fragment() {
    private var _binding: FragmentThemesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThemesBinding.inflate(inflater, container, false)

        val themes = ThemeRepository.all.map {
            RewardTheme(it.id, it.displayName, if (it.id == "default") 0 else 150)
        }

        val adapter = ThemeAdapter(themes) { theme ->
            // On Buy Clicked
            val purchased = PrefsManager.getPurchasedThemes(requireContext())
            if (theme.id in purchased) {
                Toast.makeText(context, "Theme already owned!", Toast.LENGTH_SHORT).show()
                return@ThemeAdapter
            }

            if (PrefsManager.spendCoins(requireContext(), theme.price)) {
                PrefsManager.purchaseTheme(requireContext(), theme.id)
                Toast.makeText(context, "${theme.name} purchased!", Toast.LENGTH_SHORT).show()
                (activity as? RewardsActivity)?.updateCoinBalance()
                // In a real app, you would notify the adapter to refresh its view
            } else {
                Toast.makeText(context, "Not enough coins!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvThemes.layoutManager = LinearLayoutManager(context)
        binding.rvThemes.adapter = adapter

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class RewardTheme(val id: String, val name: String, val price: Int)