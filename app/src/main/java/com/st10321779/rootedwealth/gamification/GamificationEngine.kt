package com.st10321779.rootedwealth.gamification

import android.content.Context
import android.widget.Toast
import com.st10321779.rootedwealth.repository.FirebaseRepository
import com.st10321779.rootedwealth.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object GamificationEngine {

    suspend fun processNewEntry(context: Context) {
        checkAndAwardFirstExpenseAchievement(context)
        updateStreak(context)
    }

    private suspend fun checkAndAwardFirstExpenseAchievement(context: Context) {
        val firebaseRepository = FirebaseRepository()
        val count = firebaseRepository.getExpenseCount()
        val achievementId = "first_expense"
        if (count == 1 && !PrefsManager.hasAchievement(context, achievementId)) {
            awardAchievement(context, achievementId, 50, "Achievement: Getting Started!")
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
            1 // Streak broken
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
                Toast.makeText(context, " $streak Day Streak! +$bonus Coins!", Toast.LENGTH_LONG).show()
            }
            if (streak == 7 || streak == 30) {
                val achievementId = "diligent_logger_$streak"
                if (!PrefsManager.hasAchievement(context, achievementId)) {
                    awardAchievement(context, achievementId, 50, "Achievement: Diligent Logger!")
                }
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

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isConsecutiveDay(cal1: Calendar, cal2: Calendar): Boolean {
        val nextDayOfCal1 = cal1.clone() as Calendar
        nextDayOfCal1.add(Calendar.DAY_OF_YEAR, 1)
        return isSameDay(nextDayOfCal1, cal2)
    }
}