package com.st10321779.rootedwealth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.st10321779.rootedwealth.add.AddExpenseActivity
import com.google.firebase.auth.FirebaseAuth
import com.st10321779.rootedwealth.databinding.ActivityMainBinding
import com.st10321779.rootedwealth.settings.SettingsActivity
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.ui.history.HistoryFragment
import com.st10321779.rootedwealth.ui.home.HomeFragment
import com.st10321779.rootedwealth.ui.rewards.RewardsActivity
import com.st10321779.rootedwealth.ui.tutorial.TutorialActivity

class MainActivity : AppCompatActivity() {

    //declare the binding variable
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Firstt make sure user is logged in
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            // if no user is logged in, go to the Login screen and finish this activity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
            return //stop the rest of the onCreate method from running
        }

        // Inflate the layout using the binding class
        binding = ActivityMainBinding.inflate(layoutInflater)
        // set the content view to the root of the binding
        setContentView(binding.root)

        // set initial fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // access views throughthe  binding object
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    loadFragment(HomeFragment())
                    true //true because we stay here
                }
                R.id.navigation_history -> {
                    loadFragment(HistoryFragment())
                    true
                }
                R.id.navigation_tutorial -> {
                    startActivity(Intent(this, TutorialActivity::class.java))
                    false // return false so the navigation item doesnt stay selected
                }
                R.id.navigation_rewards -> {
                    startActivity(Intent(this, RewardsActivity::class.java))
                    // return false so the item doesnt stay selected.
                    // This is since we are navigating away to a new Activity.
                    // To keep it selected, might need to manage state, but false is easier...
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

        //set the click listener for the Floating Action Button
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