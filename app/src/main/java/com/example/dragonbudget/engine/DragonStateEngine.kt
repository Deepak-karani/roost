package com.example.dragonbudget.engine

import com.example.dragonbudget.data.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * DragonStateEngine — Deterministic dragon health/XP/mood updates.
 *
 * This is the GAME ENGINE. It runs pure logic, no AI inference.
 * Gemma only EXPLAINS the dragon's state — it never controls it.
 */
object DragonStateEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Update dragon state after a new purchase.
     *
     * @param current  Current dragon state
     * @param category The budget category of the purchase
     * @param percentUsed How much of that category's budget is now used (0.0 to 1.5+)
     * @return Updated dragon state
     */
    fun onPurchase(current: DragonState, category: String, percentUsed: Float): DragonState {
        var health = current.health
        var xp = current.xp
        var level = current.level
        var streakDays = current.streakDays
        val today = dateFormat.format(Date())

        // XP rewards for responsible spending
        when {
            percentUsed < 0.50f -> {
                xp += 5  // Under 50% — great spending!
            }
            percentUsed < 0.80f -> {
                // 50-80% — neutral, no XP change
            }
            percentUsed < 1.00f -> {
                health -= 5  // 80-100% — getting close
            }
            else -> {
                health -= 15 // Over budget!
            }
        }

        // Streak tracking: did the user log today?
        if (current.lastLogDate != today) {
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val yesterdayStr = dateFormat.format(yesterday.time)

            streakDays = if (current.lastLogDate == yesterdayStr) {
                current.streakDays + 1
            } else {
                1 // Reset streak
            }

            // 3-day streak bonus
            if (streakDays >= 3 && streakDays % 3 == 0) {
                health += 10
                xp += 20
            }
        }

        // Level up every 100 XP
        while (xp >= 100) {
            xp -= 100
            level += 1
        }

        // Clamp health
        health = health.coerceIn(0, 100)

        // Compute mood based on health
        val mood = computeMood(health)

        return current.copy(
            health = health,
            xp = xp,
            level = level,
            mood = mood,
            lastUpdated = System.currentTimeMillis(),
            streakDays = streakDays,
            lastLogDate = today
        )
    }

    /**
     * Compute mood from health value.
     */
    fun computeMood(health: Int): String = when {
        health >= 85 -> "Energized"
        health >= 60 -> "Stable"
        health >= 35 -> "Worried"
        else -> "Exhausted"
    }

    /**
     * Get the emoji for the current mood.
     */
    fun getMoodEmoji(mood: String): String = when (mood) {
        "Energized" -> "🐉"
        "Stable" -> "🐲"
        "Curious" -> "🐲"
        "Worried" -> "⚠\uFE0F"
        "Exhausted" -> "😴"
        else -> "🐉"
    }

    /**
     * Get health bar color hex for the current health level.
     */
    fun getHealthColor(health: Int): Long = when {
        health >= 70 -> 0xFF00E676  // Green
        health >= 40 -> 0xFFFFAB00  // Amber
        else -> 0xFFFF1744          // Red
    }
}
