package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.st10321779.rootedwealth.repository.FirebaseRepository
import com.st10321779.rootedwealth.theme.AppTheme
import com.st10321779.rootedwealth.theme.ThemeRepository

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
}
