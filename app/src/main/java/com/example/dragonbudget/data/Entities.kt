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
// App Settings — singleton (always id = 1)
// ──────────────────────────────────────────────
//
// Holds top-level user preferences that don't belong to any other entity.
// Today: just the overall weekly budget that category allocations sum into.
// More fields can be added later without another table.
//
// `overallWeeklyBudget = 0.0` means "not yet configured" — used by the
// first-launch dialog to know whether to prompt the user.

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val overallWeeklyBudget: Double = 0.0
)

// ──────────────────────────────────────────────
// Receipt Item Entity — line items archived per Purchase
// ──────────────────────────────────────────────
//
// One Purchase row carries the receipt total used by all budget math.
// ReceiptItem rows are *audit data only* — they store what the OCR
// detected so the user can drill into a saved receipt and see the
// breakdown. Items don't affect category totals, dragon state, or
// any other math. CASCADE delete keeps them tied to their parent
// purchase: if the purchase row is removed, its items go with it.

@Entity(
    tableName = "receipt_items",
    foreignKeys = [
        ForeignKey(
            entity = Purchase::class,
            parentColumns = ["id"],
            childColumns = ["purchaseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("purchaseId")]
)
data class ReceiptItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val itemName: String,
    val price: Double,
    val category: String
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

/** Full receipt scan result with merchant + individual items + extras. */
data class ReceiptScanResult(
    val merchant: String,
    val items: List<ReceiptLineItem>,
    val total: Double,
    val confidence: Float,
    /** Tip / gratuity, if printed on the receipt. 0.0 when none detected. */
    val tip: Double = 0.0,
    /** Service charge / auto-gratuity, if printed separately from tip. */
    val serviceCharge: Double = 0.0
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

/**
 * Categories are user-managed at runtime — the source of truth is the
 * `budget_categories` Room table. This object only carries:
 *
 *   1. The seed name used on first launch (Food).
 *   2. A dictionary of suggested emojis for common category names so a
 *      newly-created "Groceries" lands on 🛒 by default. Unknown names
 *      fall back to FALLBACK_EMOJI ( 📦 ).
 *   3. A small palette the create-category dialog can show as quick picks.
 *
 * No code should iterate this object as if it were the universe of
 * categories. To list every category, collect repo.getAllCategories().
 */
object Categories {
    const val FOOD = "Food"
    const val FALLBACK_EMOJI = "📦"

    val DEFAULT_EMOJIS: Map<String, String> = mapOf(
        FOOD to "🍔",
        "Groceries" to "🛒",
        "Gas" to "⛽",
        "School" to "📚",
        "Entertainment" to "🎮",
        "Shopping" to "🛍️",
        "Household" to "🏠",
        "Transport" to "🚌",
        "Coffee" to "☕",
        "Health" to "💊",
        "Rent" to "🏠",
        "Other" to "📦"
    )

    /** Quick-pick emoji palette shown in the create-category dialog. */
    val EMOJI_PALETTE: List<String> = listOf(
        "🍔", "🛒", "⛽", "📚", "🎮", "🛍️", "🏠", "🚌",
        "☕", "💊", "🍕", "🍣", "🎬", "🎵", "💻", "📦"
    )

    /** Lookup an emoji for a category name. */
    fun emojiFor(name: String): String = DEFAULT_EMOJIS[name] ?: FALLBACK_EMOJI

    // Back-compat shim — code that still references Categories.EMOJIS[name]
    // gets the same DEFAULT_EMOJIS lookup. Prefer emojiFor() in new code.
    val EMOJIS: Map<String, String> get() = DEFAULT_EMOJIS
}
