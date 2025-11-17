package com.st10321779.rootedwealth.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.st10321779.rootedwealth.Login
import com.st10321779.rootedwealth.MainActivity
import com.st10321779.rootedwealth.R
import com.st10321779.rootedwealth.databinding.ActivitySettingsBinding
import com.st10321779.rootedwealth.gamification.GamificationEngine
import com.st10321779.rootedwealth.theme.AppTheme
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.ui.categories.CategoryManagerActivity
import com.st10321779.rootedwealth.util.PrefsManager
import com.st10321779.rootedwealth.viewmodels.HomeViewModel
import com.st10321779.rootedwealth.viewmodels.SettingsViewModel
import androidx.lifecycle.lifecycleScope
import com.st10321779.rootedwealth.repository.FirebaseRepository
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    private val homeViewModel: HomeViewModel by viewModels()

    // This list will be populated by the ViewModel with only the themes the user owns
    private var availableThemes: List<AppTheme> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply the currently active theme for consistent visuals
        ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)

        loadNonThemeSettings()
        observeViewModel()
        setupClickListeners()
    }

    private fun observeViewModel() {
        viewModel.themeSettingsState.observe(this) { state ->
            // The ViewModel gives us the final list of owned themes and which one to pre-select
            availableThemes = state.availableThemes

            val adapter = ArrayAdapter(
                this,
                R.layout.spinner_item_themed,
                availableThemes.map { it.displayName }
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerThemes.adapter = adapter

            // Set the spinner to the correct pre-selected theme
            if (state.selectedThemeIndex < availableThemes.size) {
                binding.spinnerThemes.setSelection(state.selectedThemeIndex)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveSettings.setOnClickListener {
            saveAndApplySettings()
        }

        binding.switchDark.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.saveDarkMode(this, isChecked)
            // Re-apply the current theme with the new dark mode setting for instant feedback
            ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, Login::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }

        binding.btnManageCategories.setOnClickListener {
            startActivity(Intent(this, CategoryManagerActivity::class.java))
        }

        binding.btnAddCoins.setOnClickListener {
            PrefsManager.addCoins(this, 500)
            Toast.makeText(this, "Added 500 coins!", Toast.LENGTH_SHORT).show()
        }

        binding.btnRunEndOfMonth.setOnClickListener {
            viewModel.runEndOfMonthChecks()
        }
    }

    // This function only loads settings NOT related to the theme spinner
    private fun loadNonThemeSettings() {
        binding.switchDark.isChecked = ThemeManager.isDarkMode(this)
        binding.etMinimumBudget.setText(PrefsManager.getMinimumMonthlyBudget(this).toString())
        binding.etMaximumBudget.setText(PrefsManager.getMaximumMonthlyBudget(this).toString())
        binding.switchLinkBank.isChecked = PrefsManager.isBankLinked(this)
    }

    private fun saveAndApplySettings() {
        if (availableThemes.isEmpty()) {
            Toast.makeText(this, "No themes available to save.", Toast.LENGTH_SHORT).show()
            return
        }

        // Save Theme Selection
        val selectedTheme = availableThemes[binding.spinnerThemes.selectedItemPosition]
        ThemeManager.saveSelectedTheme(this, selectedTheme.id)

        //Save Budget Settings
        val minBudget = binding.etMinimumBudget.text.toString().toFloatOrNull() ?: 0.0f
        val maxBudget = binding.etMaximumBudget.text.toString().toFloatOrNull() ?: 0.0f
        PrefsManager.saveMinimumMonthlyBudget(this, minBudget)
        PrefsManager.saveMaximumMonthlyBudget(this, maxBudget)

        // Save Bank Link Status

        val isNowLinked = binding.switchLinkBank.isChecked
        val wasPreviouslyLinked = PrefsManager.isBankLinked(this)

        PrefsManager.setBankLinked(this, isNowLinked)

        if (isNowLinked && !wasPreviouslyLinked) {
            Toast.makeText(this, "Linking account and seeding data...", Toast.LENGTH_LONG).show()

            // Use the activity's own lifecycleScope to launch the coroutine.
            // This scope will stay alive until the activity is truly destroyed.
            lifecycleScope.launch {
                val repository = FirebaseRepository()
                repository.seedBankLinkData()

                // Set the flag AFTER the seeding is complete.
                PrefsManager.setBankDataSeeded(this@SettingsActivity, true)

                // Now, restart the app.
                restartApp()
            }
        } else {
            // If no seeding is needed, just restart immediately.
            restartApp()
        }
    }
    private fun restartApp() {
        Toast.makeText(this, "Settings Saved! Restarting...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}