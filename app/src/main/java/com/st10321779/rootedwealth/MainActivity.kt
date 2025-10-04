package com.st10321779.rootedwealth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.st10321779.rootedwealth.add.AddExpenseActivity
import com.st10321779.rootedwealth.databinding.ActivityMainBinding
import com.st10321779.rootedwealth.settings.SettingsActivity
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.ui.history.HistoryFragment
import com.st10321779.rootedwealth.ui.home.HomeFragment
import com.st10321779.rootedwealth.ui.rewards.RewardsActivity
import com.st10321779.rootedwealth.ui.tutorial.TutorialActivity

class MainActivity : AppCompatActivity() {

    // Declare the binding variable
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using the binding class
        binding = ActivityMainBinding.inflate(layoutInflater)
        // Set the content view to the root of the binding
        setContentView(binding.root)

        // Set initial fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Access views through the binding object
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.navigation_history -> {
                    loadFragment(HistoryFragment())
                    true
                }
                R.id.navigation_tutorial -> {
                    startActivity(Intent(this, TutorialActivity::class.java))
                    false // Return false so the navigation item doesn't stay selected
                }
                R.id.navigation_rewards -> {
                    startActivity(Intent(this, RewardsActivity::class.java))
                    // Return false so the item doesn't stay selected.
                    // This is because we are navigating away to a new Activity.
                    // To keep it selected, might need to manage state, but false is simpler.
                    false
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false // Same reason as above
                }
                else -> false
            }
        }

        // This is important for the cradle effect of the BottomAppBar
        binding.navView.background = null

        // Set the click listener for the Floating Action Button
        binding.fabAddExpense.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        // Ensure theme is applied on resume, e.g., after returning from settings
        ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), binding.root)
    }
}