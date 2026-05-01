package com.example.dragonbudget.data

import androidx.room.*

// ──────────────────────────────────────────────
// Purchase Entity
// ──────────────────────────────────────────────

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amount: Double,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

// ──────────────────────────────────────────────
// Budget Category Entity
// ──────────────────────────────────────────────

@Entity(tableName = "budget_categories")
data class BudgetCategory(
    @PrimaryKey val name: String,
    val weeklyLimit: Double,
    val iconEmoji: String = "📦"
)

// ──────────────────────────────────────────────
// Dragon State Entity
// ──────────────────────────────────────────────

@Entity(tableName = "dragon_state")
data class DragonState(
    @PrimaryKey val id: Int = 1,
    val name: String = "SnapDragon",
    val health: Int = 80,
    val xp: Int = 0,
    val level: Int = 1,
    val mood: String = "Curious",
    val lastUpdated: Long = System.currentTimeMillis(),
    val streakDays: Int = 0,
    val lastLogDate: String = ""
)

// ──────────────────────────────────────────────
// AI Advice Entity (log of dragon responses)
// ──────────────────────────────────────────────

@Entity(tableName = "ai_advice")
data class AIAdvice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis()
)

// ──────────────────────────────────────────────
// Non-entity helper classes
// ──────────────────────────────────────────────

/** Draft from receipt vision scan (not persisted) */
data class PurchaseDraft(
    val merchant: String,
    val amount: Double,
    val suggestedCategory: String,
    val confidence: Float
)

/** Individual line item extracted from a receipt */
data class ReceiptLineItem(
    val itemName: String,
    val price: Double,
    val suggestedCategory: String,
    var selected: Boolean = true
)

/** Full receipt scan result with merchant + individual items */
data class ReceiptScanResult(
    val merchant: String,
    val items: List<ReceiptLineItem>,
    val total: Double,
    val confidence: Float
)

/** Budget category with computed spent amount */
data class BudgetCategoryWithSpent(
    val name: String,
    val weeklyLimit: Double,
    val iconEmoji: String,
    val spentAmount: Double
) {
    val percentUsed: Float get() = if (weeklyLimit > 0) (spentAmount / weeklyLimit).toFloat() else 0f
    val remaining: Double get() = (weeklyLimit - spentAmount).coerceAtLeast(0.0)
}

/** All budget categories */
object Categories {
    const val FOOD = "Food"
    const val GROCERIES = "Groceries"
    const val GAS = "Gas"
    const val SCHOOL = "School"
    const val ENTERTAINMENT = "Entertainment"
    const val SHOPPING = "Shopping"
    const val HOUSEHOLD = "Household"
    const val OTHER = "Other"

    val ALL = listOf(FOOD, GROCERIES, GAS, SCHOOL, ENTERTAINMENT, SHOPPING, HOUSEHOLD, OTHER)

    val EMOJIS = mapOf(
        FOOD to "🍔",
        GROCERIES to "🛒",
        GAS to "⛽",
        SCHOOL to "📚",
        ENTERTAINMENT to "🎮",
        SHOPPING to "🛍️",
        HOUSEHOLD to "🏠",
        OTHER to "📦"
    )

    val DEFAULT_LIMITS = mapOf(
        FOOD to 50.0,
        GROCERIES to 120.0,
        GAS to 60.0,
        SCHOOL to 40.0,
        ENTERTAINMENT to 60.0,
        SHOPPING to 80.0,
        HOUSEHOLD to 50.0,
        OTHER to 40.0
    )

    val LIFESTYLE_PRESETS = mapOf(
        "Student" to mapOf(
            FOOD to 40.0, GROCERIES to 80.0, GAS to 30.0, SCHOOL to 100.0, 
            ENTERTAINMENT to 50.0, SHOPPING to 40.0, HOUSEHOLD to 30.0, OTHER to 30.0
        ),
        "Professional" to mapOf(
            FOOD to 150.0, GROCERIES to 200.0, GAS to 100.0, SCHOOL to 20.0, 
            ENTERTAINMENT to 150.0, SHOPPING to 200.0, HOUSEHOLD to 150.0, OTHER to 100.0
        ),
        "Thrifty" to mapOf(
            FOOD to 30.0, GROCERIES to 60.0, GAS to 40.0, SCHOOL to 10.0, 
            ENTERTAINMENT to 20.0, SHOPPING to 20.0, HOUSEHOLD to 20.0, OTHER to 20.0
        )
    )
}
