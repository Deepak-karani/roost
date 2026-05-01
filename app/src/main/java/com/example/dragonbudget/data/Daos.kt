package com.example.dragonbudget.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ──────────────────────────────────────────────
// Purchase DAO
// ──────────────────────────────────────────────

@Dao
interface PurchaseDao {
    @Insert
    suspend fun insert(purchase: Purchase): Long

    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPurchases(limit: Int = 5): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE category = :category ORDER BY timestamp DESC")
    fun getPurchasesByCategory(category: String): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getPurchasesSince(since: Long): Flow<List<Purchase>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM purchases WHERE category = :category AND timestamp >= :since")
    suspend fun getSpentInCategory(category: String, since: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM purchases WHERE timestamp >= :since")
    suspend fun getTotalSpentSince(since: Long): Double

    @Delete
    suspend fun delete(purchase: Purchase)

    @Query("DELETE FROM purchases")
    suspend fun deleteAll()
}

// ──────────────────────────────────────────────
// BudgetCategory DAO
// ──────────────────────────────────────────────

@Dao
interface BudgetCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<BudgetCategory>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: BudgetCategory)

    @Query("SELECT * FROM budget_categories ORDER BY name")
    fun getAllCategories(): Flow<List<BudgetCategory>>

    @Query("SELECT * FROM budget_categories WHERE name = :name")
    suspend fun getCategory(name: String): BudgetCategory?

    @Query("UPDATE budget_categories SET weeklyLimit = :limit WHERE name = :name")
    suspend fun updateLimit(name: String, limit: Double)
}

// ──────────────────────────────────────────────
// DragonState DAO
// ──────────────────────────────────────────────

@Dao
interface DragonStateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: DragonState)

    @Query("SELECT * FROM dragon_state WHERE id = 1")
    fun getDragonState(): Flow<DragonState?>

    @Query("SELECT * FROM dragon_state WHERE id = 1")
    suspend fun getDragonStateOnce(): DragonState?

    @Query("UPDATE dragon_state SET health = :health, xp = :xp, level = :level, mood = :mood, lastUpdated = :lastUpdated, streakDays = :streakDays, lastLogDate = :lastLogDate WHERE id = 1")
    suspend fun updateState(health: Int, xp: Int, level: Int, mood: String, lastUpdated: Long, streakDays: Int, lastLogDate: String)
}

// ──────────────────────────────────────────────
// AI Advice DAO
// ──────────────────────────────────────────────

@Dao
interface AIAdviceDao {
    @Insert
    suspend fun insert(advice: AIAdvice): Long

    @Query("SELECT * FROM ai_advice ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAdvice(limit: Int = 10): Flow<List<AIAdvice>>
}
