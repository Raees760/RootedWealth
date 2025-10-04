package com.st10321779.rootedwealth.util

import android.content.Context
import android.content.SharedPreferences

object PrefsManager {
    private const val PREFS_NAME = "app_prefs"

    private const val KEY_MONTHLY_BUDGET = "monthly_budget"
    private const val KEY_COIN_BALANCE = "coin_balance"
    private const val KEY_STREAK_COUNT = "streak_count"
    private const val KEY_LAST_LOG_DATE = "last_log_date"
    private const val KEY_BANK_LINKED = "bank_linked"
    private const val KEY_PURCHASED_THEMES = "purchased_themes"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Budget
    fun saveMonthlyBudget(context: Context, budget: Float) {
        prefs(context).edit().putFloat(KEY_MONTHLY_BUDGET, budget).apply()
    }
    fun getMonthlyBudget(context: Context): Float = prefs(context).getFloat(KEY_MONTHLY_BUDGET, 20000.0f)

    // Bank Link
    fun setBankLinked(context: Context, isLinked: Boolean) {
        prefs(context).edit().putBoolean(KEY_BANK_LINKED, isLinked).apply()
    }
    fun isBankLinked(context: Context): Boolean = prefs(context).getBoolean(KEY_BANK_LINKED, false)

    // Coins
    fun getCoinBalance(context: Context): Int = prefs(context).getInt(KEY_COIN_BALANCE, 100)
    fun addCoins(context: Context, amount: Int) {
        val current = getCoinBalance(context)
        prefs(context).edit().putInt(KEY_COIN_BALANCE, current + amount).apply()
    }
    fun spendCoins(context: Context, amount: Int): Boolean {
        val current = getCoinBalance(context)
        return if (current >= amount) {
            prefs(context).edit().putInt(KEY_COIN_BALANCE, current - amount).apply()
            true
        } else {
            false
        }
    }

    // Themes
    fun getPurchasedThemes(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_PURCHASED_THEMES, setOf("default")) ?: setOf("default")
    }

    fun purchaseTheme(context: Context, themeId: String) {
        val purchased = getPurchasedThemes(context).toMutableSet()
        purchased.add(themeId)
        prefs(context).edit().putStringSet(KEY_PURCHASED_THEMES, purchased).apply()
    }
}