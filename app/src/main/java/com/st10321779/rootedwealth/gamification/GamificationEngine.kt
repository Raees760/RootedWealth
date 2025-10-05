package com.st10321779.rootedwealth.gamification

import android.content.Context
import android.widget.Toast
import com.st10321779.rootedwealth.data.local.AppDatabase
import com.st10321779.rootedwealth.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.max

object GamificationEngine {

    /**
     * Call this every time a new expense is added.
     */
    suspend fun processNewEntry(context: Context) {
        checkAndAwardFirstExpenseAchievement(context)
        updateStreak(context)
        checkWeeklyChallenges(context) // Check challenges on every new entry
    }

    /**
     * Call this manually (e.g., from a button) to simulate end-of-month calculations.
     */
    suspend fun processEndOfMonth(context: Context) {
        checkUnderBudgetAchievements(context)
        // Add more end-of-month coin rewards here if needed
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "End-of-month rewards calculated!", Toast.LENGTH_LONG).show()
        }
    }


    // --- ACHIEVEMENT & STREAK LOGIC ---
    private suspend fun checkAndAwardFirstExpenseAchievement(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val count = db.expenseDao().getExpenseCount()
        if (count == 1 && !PrefsManager.hasAchievement(context, "first_expense")) {
            awardAchievement(context, "first_expense", 50, "Achievement: Getting Started!")
        }
    }

    private suspend fun updateStreak(context: Context) {
        val lastLogMillis = PrefsManager.getLastLogDate(context)
        if (lastLogMillis == 0L) { // First ever log
            PrefsManager.saveStreakCount(context, 1)
            PrefsManager.saveLastLogDate(context, System.currentTimeMillis())
            return
        }

        val lastLogCal = Calendar.getInstance().apply { timeInMillis = lastLogMillis }
        val currentCal = Calendar.getInstance()

        if (isSameDay(lastLogCal, currentCal)) return // Already logged today

        val streak = if (isConsecutiveDay(lastLogCal, currentCal)) {
            PrefsManager.getStreakCount(context) + 1
        } else {
            1 // Streak broken, reset
        }

        PrefsManager.saveStreakCount(context, streak)
        PrefsManager.saveLastLogDate(context, System.currentTimeMillis())

        val bonus = when (streak) {
            3 -> 10
            7 -> 20
            14 -> 40
            30 -> 100
            else -> 0
        }

        if (bonus > 0) {
            PrefsManager.addCoins(context, bonus)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "🔥 $streak Day Streak! +$bonus Coins!", Toast.LENGTH_LONG).show()
            }
            if (streak == 7 || streak == 30) {
                val achievementId = "diligent_logger_$streak"
                if (!PrefsManager.hasAchievement(context, achievementId)) {
                    awardAchievement(context, achievementId, 50, "Achievement: Diligent Logger!")
                }
            }
        }
    }


    // --- CHALLENGE & REWARD LOGIC ---

    private suspend fun checkWeeklyChallenges(context: Context) {
        // Only run if a bank account is linked
        if (!PrefsManager.isBankLinked(context)) return

        val end = Calendar.getInstance().time
        val startCal = Calendar.getInstance()
        startCal.add(Calendar.DAY_OF_YEAR, -7)
        val start = startCal.time
        val db = AppDatabase.getDatabase(context)

        // Challenge: "Home Meals Are Best"
        val takeawaySpend = db.expenseDao().getTotalSpentForCategory("Takeout", start, end) ?: 0.0
        if (takeawaySpend < 500 && !PrefsManager.hasAchievement(context, "weekly_takeout_challenge")) {
            awardAchievement(context, "weekly_takeout_challenge", 75, "Challenge Complete: Home Meals Are Best!")
        }

        // Challenge: "The Simple Life" (luxuries)
        val luxuryCategories = listOf("Entertainment", "Takeout") // Define what counts as luxury
        val luxurySpend = db.expenseDao().getTotalSpentForCategoryList(luxuryCategories, start, end) ?: 0.0
        if (luxurySpend < 1500 && !PrefsManager.hasAchievement(context, "weekly_luxury_challenge")) {
            awardAchievement(context, "weekly_luxury_challenge", 75, "Challenge Complete: The Simple Life!")
        }
    }

    private suspend fun checkUnderBudgetAchievements(context: Context) {
        if (!PrefsManager.isBankLinked(context)) return

        val db = AppDatabase.getDatabase(context)
        val end = Calendar.getInstance().time
        val startCal = Calendar.getInstance()
        startCal.add(Calendar.MONTH, -1) // Check last month's data
        val start = startCal.time

        val totalBudget = PrefsManager.getMonthlyBudget(context)
        val totalSpent = db.expenseDao().getTotalExpensesInPeriodAsync(start, end)

        if (totalSpent < totalBudget && !PrefsManager.hasAchievement(context, "under_budget_1")) {
            awardAchievement(context, "under_budget_1", 100, "Achievement: Under Budget!")
            // In a real app, you'd track tiers (1, 3, 6 months) in PrefsManager
        }
    }


    // --- HELPER FUNCTIONS ---

    private fun awardAchievement(context: Context, id: String, coins: Int, message: String) {
        PrefsManager.addCoins(context, coins)
        PrefsManager.setAchievementUnlocked(context, id)
        // Run the Toast on the main thread
        GlobalScope.launch(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isConsecutiveDay(cal1: Calendar, cal2: Calendar): Boolean {
        // To correctly handle year boundaries, we check if cal1 is exactly one day before cal2
        val nextDayOfCal1 = cal1.clone() as Calendar
        nextDayOfCal1.add(Calendar.DAY_OF_YEAR, 1)
        return isSameDay(nextDayOfCal1, cal2)
    }
}