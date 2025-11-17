package com.st10321779.rootedwealth.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.st10321779.rootedwealth.repository.FirebaseRepository
import com.st10321779.rootedwealth.util.PrefsManager
import kotlinx.coroutines.launch

class RewardsViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseRepository = FirebaseRepository()

    val purchasedThemes: LiveData<List<String>> = firebaseRepository.getPurchasedThemesLiveData()

    fun purchaseTheme(themeId: String, price: Int) {
        val currentCoins = PrefsManager.getCoinBalance(getApplication())
        if (currentCoins >= price) {
            // Deduct coins locally
            PrefsManager.spendCoins(getApplication(), price)
            // Save the purchase to Firebase
            firebaseRepository.purchaseTheme(themeId)
        }
    }
}