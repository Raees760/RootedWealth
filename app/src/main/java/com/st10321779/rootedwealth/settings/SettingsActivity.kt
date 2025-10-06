package com.st10321779.rootedwealth.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.st10321779.rootedwealth.MainActivity
import com.st10321779.rootedwealth.databinding.ActivitySettingsBinding
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.theme.ThemeRepository
import com.st10321779.rootedwealth.util.PrefsManager
import com.st10321779.rootedwealth.viewmodels.HomeViewModel
import androidx.lifecycle.lifecycleScope
import com.st10321779.rootedwealth.gamification.GamificationEngine
import com.st10321779.rootedwealth.ui.categories.CategoryManagerActivity
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.st10321779.rootedwealth.Login

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)

        setupThemeSpinner()
        loadCurrentSettings()

        binding.btnManageCategories.setOnClickListener {
            startActivity(Intent(this, CategoryManagerActivity::class.java))
        }

        binding.btnAddCoins.setOnClickListener {
            PrefsManager.addCoins(this, 500)
            Toast.makeText(this, "Added 500 coins!", Toast.LENGTH_SHORT).show()
        }

        binding.btnRunEndOfMonth.setOnClickListener {
            // launch a coroutine to call the suspend function
            lifecycleScope.launch {
                GamificationEngine.processEndOfMonth(this@SettingsActivity)
            }
        }

        binding.btnSaveSettings.setOnClickListener {
            saveAndApplySettings()
        }

        //apply dark mode instantly for better UX
        binding.switchDark.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.saveDarkMode(this, isChecked)
            ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)
        }
        // logout Listener
        binding.btnLogout.setOnClickListener {
            //sign out from Firebase
            FirebaseAuth.getInstance().signOut()

            //go to the login screen
            val intent = Intent(this, Login::class.java)
            // =these flags clear the entire task stack, so the user can't press "back" to get into the app again
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish() //close the settings activity
        }
    }

    private fun setupThemeSpinner() {
        val themes = ThemeRepository.all
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes.map { it.displayName })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerThemes.adapter = adapter
    }

    private fun loadCurrentSettings() {
        //Theme
        val themes = ThemeRepository.all
        val selectedTheme = ThemeManager.getSelectedTheme(this)
        binding.spinnerThemes.setSelection(themes.indexOfFirst { it.id == selectedTheme.id })

        //Dark Mode
        binding.switchDark.isChecked = ThemeManager.isDarkMode(this)

        //Budget
        binding.etMonthlyBudget.setText(PrefsManager.getMonthlyBudget(this).toString())

        //Bank Link
        binding.switchLinkBank.isChecked = PrefsManager.isBankLinked(this)
    }

    private fun saveAndApplySettings() {
        //Theme
        val themes = ThemeRepository.all
        val selectedTheme = themes[binding.spinnerThemes.selectedItemPosition]
        ThemeManager.saveSelectedTheme(this, selectedTheme.id)

        //Budget
        val budget = binding.etMonthlyBudget.text.toString().toFloatOrNull() ?: 0.0f
        if (budget > 0) {
            PrefsManager.saveMonthlyBudget(this, budget)
        }

        //Bank Link
        val isBankLinked = binding.switchLinkBank.isChecked
        PrefsManager.setBankLinked(this, isBankLinked)
        //notify the ViewModel to run the seeder if needed
        val homeViewModel: HomeViewModel by viewModels()
        homeViewModel.onBankLinkStatusChanged(isBankLinked)

        Toast.makeText(this, "Settings Saved! Applying changes...", Toast.LENGTH_SHORT).show()

        // force a full restart to apply theme correctly
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}