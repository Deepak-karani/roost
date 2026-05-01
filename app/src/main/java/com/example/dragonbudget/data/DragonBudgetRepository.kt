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

    // ── Purchases ───────────────────────────────

    suspend fun addPurchase(purchase: Purchase): Long = purchaseDao.insert(purchase)

    fun getAllPurchases(): Flow<List<Purchase>> = purchaseDao.getAllPurchases()

    fun getRecentPurchases(limit: Int = 5): Flow<List<Purchase>> = purchaseDao.getRecentPurchases(limit)

    fun getPurchasesByCategory(category: String): Flow<List<Purchase>> =
        purchaseDao.getPurchasesByCategory(category)

    suspend fun deletePurchase(purchase: Purchase) = purchaseDao.delete(purchase)

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

    suspend fun getCategoriesWithSpent(): List<BudgetCategoryWithSpent> {
        val weekStart = getWeekStart()
        val categories = mutableListOf<BudgetCategoryWithSpent>()
        for (cat in Categories.ALL) {
            val budgetCat = categoryDao.getCategory(cat)
            val spent = purchaseDao.getSpentInCategory(cat, weekStart)
            categories.add(
                BudgetCategoryWithSpent(
                    name = cat,
                    weeklyLimit = budgetCat?.weeklyLimit ?: 0.0,
                    iconEmoji = budgetCat?.iconEmoji ?: Categories.EMOJIS[cat] ?: "📦",
                    spentAmount = spent
                )
            )
        }
        return categories
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

    // ── Seed Data ───────────────────────────────

    suspend fun seedIfNeeded() {
        // Seed dragon
        if (dragonDao.getDragonStateOnce() == null) {
            dragonDao.insertOrUpdate(DragonState())
        }
        // Seed budget categories
        val existing = categoryDao.getCategory(Categories.FOOD)
        if (existing == null) {
            val defaults = Categories.ALL.map { name ->
                BudgetCategory(
                    name = name,
                    weeklyLimit = 0.0,
                    iconEmoji = Categories.EMOJIS[name] ?: "📦"
                )
            }
            categoryDao.insertAll(defaults)
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
