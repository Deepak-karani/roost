package com.example.dragonbudget.engine

import com.example.dragonbudget.R
import com.example.dragonbudget.data.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * DragonStateEngine — Deterministic dragon health/XP/mood updates.
 *
 * This is the GAME ENGINE. It runs pure logic, no AI inference.
 * Gemma only EXPLAINS the dragon's state — it never controls it.
 *
 * Tuning notes for the demo:
 *  - Good purchases visibly *heal* the dragon, not just hand out XP. That
 *    way a single tap moves the on-screen art from one frame to the next,
 *    which sells the loop in seconds during the demo.
 *  - Bad purchases hurt more than good purchases heal, so the dragon's
 *    state still feels earned (you can't burn money and stay at full HP).
 *  - The dragon's overall *ratio* of weekly spend to weekly budget is the
 *    primary signal — a single category going over budget shouldn't tank
 *    the whole pet if everywhere else you're fine.
 */
object DragonStateEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Update dragon state after a new purchase.
     *
     * @param current  Current dragon state
     * @param category The budget category of the purchase
     * @param percentUsed How much of THIS category's weekly budget is used (0.0 to 1.5+)
     * @param overallRatio Total weekly spend / total weekly budget (0.0 to 1.5+).
     *                     Pass 0f if no budget is configured.
     */
    fun onPurchase(
        current: DragonState,
        category: String,
        percentUsed: Float,
        overallRatio: Float = 0f
    ): DragonState {
        var health = current.health
        var xp = current.xp
        var level = current.level
        var streakDays = current.streakDays
        val today = dateFormat.format(Date())

        // ── Per-category effect: what the user just did ──
        when {
            percentUsed < 0.50f -> {       // Plenty of room left in this category
                health += 8
                xp += 8
            }
            percentUsed < 0.80f -> {       // Comfortable — small reward
                health += 2
                xp += 4
            }
            percentUsed < 1.00f -> {       // Tight — small bite
                health -= 8
                xp += 2
            }
            percentUsed < 1.25f -> {       // Just over budget
                health -= 16
            }
            else -> {                      // Way over budget
                health -= 24
            }
        }

        // ── Overall-budget bonus/penalty: how is the WEEK going? ──
        // Only applied when the user actually has a budget configured.
        if (overallRatio > 0f) {
            when {
                overallRatio < 0.50f -> health += 4   // Crushing the week
                overallRatio < 0.80f -> health += 1   // On track
                overallRatio < 1.00f -> health -= 2   // Spending fast
                else -> health -= 6                   // Already past total
            }
        }

        // ── Streak tracking ──
        if (current.lastLogDate != today) {
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val yesterdayStr = dateFormat.format(yesterday.time)

            streakDays = if (current.lastLogDate == yesterdayStr) {
                current.streakDays + 1
            } else {
                1
            }
            // Every 3 days of consistent logging: bonus
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

        health = health.coerceIn(0, 100)
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
        "Worried" -> "⚠️"
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

    /**
     * Map current health (0..100) to one of the ten dragon sprites,
     * each covering a 10% bracket. Low health = sleepy/exhausted,
     * high health = excited / celebrating-the-savings ($$ frames).
     */
    fun getDragonDrawableForHealth(health: Int): Int {
        val bracket = (health.coerceIn(0, 100) / 10).coerceAtMost(9)
        return when (bracket) {
            0 -> R.drawable.dragon_sleep_deep      // 0–9   passed out
            1 -> R.drawable.dragon_sleep_light     // 10–19 sleeping lighter
            2 -> R.drawable.dragon_drowsy          // 20–29 still tired, eyes closed
            3 -> R.drawable.dragon_waking          // 30–39 waking up, mouth open
            4 -> R.drawable.dragon_idle_1          // 40–49 sitting, alert
            5 -> R.drawable.dragon_idle_2          // 50–59 wide grin sitting
            6 -> R.drawable.dragon_happy           // 60–69 arms-up excited
            7 -> R.drawable.dragon_money_1         // 70–79 $$ ecstatic
            8 -> R.drawable.dragon_money_2         // 80–89 $$ ecstatic alt
            else -> R.drawable.dragon_content      // 90–100 contented, peaceful smile
        }
    }

    /**
     * Sprite picker that also factors in how broke the user is for the week.
     *
     *   moneyLeftRatio = (totalBudget - totalSpent) / totalBudget
     *
     *   1.0 = nothing spent yet, 0.0 = exactly on budget, < 0 = over budget.
     *
     *  - Over budget (≤0):     deep sleep
     *  - <10% money left:      light sleep
     *  - <25% money left:      drowsy
     *  - otherwise:            normal health-based mapping
     *
     * If no budget is configured (totalBudget == 0), pass moneyLeftRatio = 1f
     * so the dragon falls through to plain health-based art.
     */
    fun getDragonDrawable(health: Int, moneyLeftRatio: Float): Int {
        return when {
            moneyLeftRatio <= 0f   -> R.drawable.dragon_sleep_deep
            moneyLeftRatio < 0.10f -> R.drawable.dragon_sleep_light
            moneyLeftRatio < 0.25f -> R.drawable.dragon_drowsy
            else -> getDragonDrawableForHealth(health)
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  New named-frame system (the canonical path going forward).
    //  Health is derived directly from remaining-budget percentage:
    //    spending 0 % of budget   -> 100 % dragon health
    //    spending 50 % of budget  -> 50 % dragon health
    //    spending 100 %+ of budget -> 0 % dragon health
    //  When no budget is configured, callers should pass health = 100 so the
    //  dragon shows the calm-happy frame instead of being asleep by default.
    // ──────────────────────────────────────────────────────────────────────

    /** Compute dragon health from total weekly spend / total weekly budget. */
    fun budgetHealth(totalSpent: Double, totalBudget: Double): Int {
        if (totalBudget <= 0.0) return 100
        val spentPercent = (totalSpent / totalBudget) * 100.0
        val remainingBudgetPercent = (100.0 - spentPercent).coerceAtLeast(0.0)
        return remainingBudgetPercent.toInt()
    }

    /**
     * Pick the named frame for a given health value (0..100).
     *
     * The frames are anchored so the first frame "starts at 10%": frame 0
     * is the floor that covers 0–19% (deep sleep stays visible even when
     * the user is fully broke), then each subsequent 10% bracket bumps up
     * one frame, ending at frame 9 for a perfect 100% week.
     */
    fun getDragonFrameForHealth(health: Int): DragonFrame {
        return when (health.coerceIn(0, 100)) {
            100        -> DragonFrame.FRAME_9_CALM_HAPPY
            in 90..99  -> DragonFrame.FRAME_8_MONEY_HAPPY
            in 80..89  -> DragonFrame.FRAME_7_MONEY_EXCITED
            in 70..79  -> DragonFrame.FRAME_6_ALERT_HAPPY
            in 60..69  -> DragonFrame.FRAME_5_HAPPY_SITTING
            in 50..59  -> DragonFrame.FRAME_4_EXCITED
            in 40..49  -> DragonFrame.FRAME_3_HAPPY
            in 30..39  -> DragonFrame.FRAME_2_WAKING_UP
            in 20..29  -> DragonFrame.FRAME_1_SLEEPING
            else       -> DragonFrame.FRAME_0_SLEEPING_DEEP
        }
    }

    fun dragonFrameToDrawable(frame: DragonFrame): Int = when (frame) {
        DragonFrame.FRAME_0_SLEEPING_DEEP -> R.drawable.dragon_frame_0_sleeping_deep
        DragonFrame.FRAME_1_SLEEPING      -> R.drawable.dragon_frame_1_sleeping
        DragonFrame.FRAME_2_WAKING_UP     -> R.drawable.dragon_frame_2_waking_up
        DragonFrame.FRAME_3_HAPPY         -> R.drawable.dragon_frame_3_happy
        DragonFrame.FRAME_4_EXCITED       -> R.drawable.dragon_frame_4_excited
        DragonFrame.FRAME_5_HAPPY_SITTING -> R.drawable.dragon_frame_5_happy_sitting
        DragonFrame.FRAME_6_ALERT_HAPPY   -> R.drawable.dragon_frame_6_alert_happy
        DragonFrame.FRAME_7_MONEY_EXCITED -> R.drawable.dragon_frame_7_money_excited
        DragonFrame.FRAME_8_MONEY_HAPPY   -> R.drawable.dragon_frame_8_money_happy
        DragonFrame.FRAME_9_CALM_HAPPY    -> R.drawable.dragon_frame_9_calm_happy
    }

    /** Mood label aligned to the same 10-bracket scheme as the frames. */
    fun getMoodLabel(health: Int): String = when (health.coerceIn(0, 100)) {
        100        -> "Calm and thriving"
        in 90..99  -> "Happy"
        in 80..89  -> "Excited"
        in 70..79  -> "Alert but okay"
        in 60..69  -> "Still good"
        in 50..59  -> "Getting tired"
        in 40..49  -> "Low energy"
        in 30..39  -> "Waking / sleepy"
        in 20..29  -> "Sleepy"
        else       -> "Deep sleep"
    }
}

enum class DragonFrame {
    FRAME_0_SLEEPING_DEEP,
    FRAME_1_SLEEPING,
    FRAME_2_WAKING_UP,
    FRAME_3_HAPPY,
    FRAME_4_EXCITED,
    FRAME_5_HAPPY_SITTING,
    FRAME_6_ALERT_HAPPY,
    FRAME_7_MONEY_EXCITED,
    FRAME_8_MONEY_HAPPY,
    FRAME_9_CALM_HAPPY
}
