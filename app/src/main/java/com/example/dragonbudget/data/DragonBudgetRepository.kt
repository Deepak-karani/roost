package com.example.dragonbudget.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository — Single source of truth for all DragonBudget data.
 * All operations are offline / local via Room.
 */
class DragonBudgetRepository(private val db: DragonBudgetDatabase) {

    private val purchaseDao = db.purchaseDao()
    private val categoryDao = db.budgetCategoryDao()
    private val dragonDao = db.dragonStateDao()
    private val adviceDao = db.aiAdviceDao()
    private val receiptItemDao = db.receiptItemDao()
    private val settingsDao = db.appSettingsDao()

    // ── Purchases ───────────────────────────────

    suspend fun addPurchase(purchase: Purchase): Long = purchaseDao.insert(purchase)

    /**
     * Archive a scanned receipt as a single Purchase plus its line-item children.
     * The Purchase row is what budget math sees (total, category). The
     * ReceiptItem rows are pure audit data so the user can drill in later.
     *
     * Items list is saved exactly as-is — no de-dup or repricing.
     */
    suspend fun addPurchaseWithItems(purchase: Purchase, items: List<ReceiptLineItem>): Long {
        val purchaseId = purchaseDao.insert(purchase)
        if (items.isNotEmpty()) {
            receiptItemDao.insertAll(
                items.map { item ->
                    ReceiptItem(
                        purchaseId = purchaseId,
                        itemName = item.itemName,
                        price = item.price,
                        category = item.suggestedCategory
                    )
                }
            )
        }
        return purchaseId
    }

    fun getReceiptItems(purchaseId: Long): Flow<List<ReceiptItem>> =
        receiptItemDao.getItemsForPurchase(purchaseId)

    fun getAllPurchases(): Flow<List<Purchase>> = purchaseDao.getAllPurchases()

    fun getRecentPurchases(limit: Int = 5): Flow<List<Purchase>> = purchaseDao.getRecentPurchases(limit)

    fun getPurchasesByCategory(category: String): Flow<List<Purchase>> =
        purchaseDao.getPurchasesByCategory(category)

    suspend fun deletePurchase(purchase: Purchase) = purchaseDao.delete(purchase)

    suspend fun updatePurchase(purchase: Purchase) = purchaseDao.update(purchase)

    suspend fun clearAllPurchases() = purchaseDao.deleteAll()

    suspend fun resetDragonState() {
        dragonDao.insertOrUpdate(DragonState())
    }

    suspend fun getSpentInCategory(category: String): Double {
        return purchaseDao.getSpentInCategory(category, getWeekStart())
    }

    suspend fun getTotalSpentThisWeek(): Double {
        return purchaseDao.getTotalSpentSince(getWeekStart())
    }

    // ── Budget Categories ───────────────────────

    fun getAllCategories(): Flow<List<BudgetCategory>> = categoryDao.getAllCategories()

    suspend fun updateCategoryLimit(name: String, limit: Double) {
        categoryDao.updateLimit(name, limit)
    }

    suspend fun deleteCategory(category: BudgetCategory) {
        categoryDao.delete(category)
    }

    suspend fun addCategory(name: String, emoji: String, weeklyLimit: Double = 0.0): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (categoryDao.getCategory(trimmed) != null) return false
        categoryDao.insert(
            BudgetCategory(
                name = trimmed,
                weeklyLimit = weeklyLimit,
                iconEmoji = emoji.ifBlank { Categories.emojiFor(trimmed) }
            )
        )
        return true
    }

    suspend fun getCategoriesWithSpent(): List<BudgetCategoryWithSpent> {
        val weekStart = getWeekStart()
        val rows = categoryDao.getAllCategoriesOnce()
        return rows.map { cat ->
            val spent = purchaseDao.getSpentInCategory(cat.name, weekStart)
            BudgetCategoryWithSpent(
                name = cat.name,
                weeklyLimit = cat.weeklyLimit,
                iconEmoji = cat.iconEmoji,
                spentAmount = spent
            )
        }
    }

    // ── Dragon State ────────────────────────────

    fun getDragonState(): Flow<DragonState?> = dragonDao.getDragonState()

    suspend fun getDragonStateOnce(): DragonState =
        dragonDao.getDragonStateOnce() ?: DragonState()

    suspend fun updateDragonState(state: DragonState) = dragonDao.insertOrUpdate(state)

    // ── AI Advice ───────────────────────────────

    suspend fun saveAdvice(prompt: String, response: String) {
        adviceDao.insert(AIAdvice(prompt = prompt, response = response))
    }

    fun getRecentAdvice(): Flow<List<AIAdvice>> = adviceDao.getRecentAdvice()

    // ── App Settings (overall weekly budget) ────

    fun getSettings(): Flow<AppSettings?> = settingsDao.get()

    suspend fun getSettingsOnce(): AppSettings = settingsDao.getOnce() ?: AppSettings()

    suspend fun setOverallWeeklyBudget(amount: Double) {
        val current = settingsDao.getOnce() ?: AppSettings()
        settingsDao.insertOrUpdate(current.copy(overallWeeklyBudget = amount))
    }

    // ── Seed Data ───────────────────────────────

    suspend fun seedIfNeeded() {
        if (dragonDao.getDragonStateOnce() == null) {
            dragonDao.insertOrUpdate(DragonState())
        }
        // Seed only the default Food category. Users can add the rest
        // (or anything else) via the + button on the Budget screen.
        val anyCategory = categoryDao.getAllCategoriesOnce().firstOrNull()
        if (anyCategory == null) {
            categoryDao.insert(
                BudgetCategory(
                    name = Categories.FOOD,
                    weeklyLimit = 0.0,
                    iconEmoji = Categories.emojiFor(Categories.FOOD)
                )
            )
        }
        // Seed the settings row so the Flow always emits something useful.
        if (settingsDao.getOnce() == null) {
            settingsDao.insertOrUpdate(AppSettings())
        }
    }

    // ── Helpers ─────────────────────────────────

    private fun getWeekStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
