package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.st10321779.rootedwealth.repository.FirebaseRepository
import com.st10321779.rootedwealth.theme.AppTheme
import com.st10321779.rootedwealth.theme.ThemeManager
import com.st10321779.rootedwealth.theme.ThemeRepository
import com.st10321779.rootedwealth.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

// Data class to hold the final state for the UI
data class ThemeSettingsState(
    val availableThemes: List<AppTheme> = emptyList(),
    val selectedThemeIndex: Int = 0
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepository = FirebaseRepository()

    // Raw data from Firebase
    private val purchasedThemeIds: LiveData<List<String>> = firebaseRepository.getPurchasedThemesLiveData()

    // The final, processed LiveData that the UI will observe
    val themeSettingsState = MediatorLiveData<ThemeSettingsState>()

    init {
        // When the purchased themes arrives from Firebase, process it
        themeSettingsState.addSource(purchasedThemeIds) { ids ->
            //Filter the master list to get only the themes the user owns
            val availableThemes = ThemeRepository.all.filter { it.id in ids }

            //default to the first available theme if the current one isn't owned
            val currentThemeId = "default" // A placeholder, this should be the user's saved preference
            var selectedIndex = availableThemes.indexOfFirst { it.id == currentThemeId }
            if (selectedIndex == -1) {
                selectedIndex = 0 // Default to the first item if the saved theme isn't owned
            }

            //Post the final state to the UI
            if(availableThemes.isNotEmpty()){
                themeSettingsState.value = ThemeSettingsState(availableThemes, selectedIndex)
            } else {
                // Handle the case where the user somehow owns no themes (not even default)
                themeSettingsState.value = ThemeSettingsState(emptyList(), 0)
            }
        }
    }
    fun runEndOfMonthChecks() = viewModelScope.launch {
        if (!PrefsManager.isBankLinked(getApplication())) {
            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Bank account must be linked for this feature.", Toast.LENGTH_SHORT).show()
            }
            return@launch
        }

        // Get expenses and budget
        val expenses = firebaseRepository.getExpensesOneTime() // We need a new repository function
        val maxBudget = PrefsManager.getMaximumMonthlyBudget(getApplication())

        // Calculate date range for LAST month
        val endCal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); add(Calendar.DATE, -1) }
        val startCal = Calendar.getInstance().apply { time = endCal.time; set(Calendar.DAY_OF_MONTH, 1) }

        val lastMonthExpenses = expenses.filter { it.date in startCal.time..endCal.time }
        val totalSpentLastMonth = lastMonthExpenses.sumOf { it.amount }

        val achievementId = "under_budget_1"
        if (totalSpentLastMonth < maxBudget && !PrefsManager.hasAchievement(getApplication(), achievementId)) {
            awardAchievement(getApplication(), achievementId, 100, "Achievement: Under Budget!")
        } else if (totalSpentLastMonth >= maxBudget) {
            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Did not meet 'Under Budget' goal last month.", Toast.LENGTH_SHORT).show()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Already earned 'Under Budget' achievement.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun awardAchievement(context: Context, id: String, coins: Int, message: String) {
        PrefsManager.addCoins(context, coins)
        PrefsManager.setAchievementUnlocked(context, id)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
