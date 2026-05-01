package com.example.dragonbudget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Purchase::class, BudgetCategory::class, DragonState::class, AIAdvice::class, ReceiptItem::class, AppSettings::class],
    version = 3,
    exportSchema = false
)
abstract class DragonBudgetDatabase : RoomDatabase() {
    abstract fun purchaseDao(): PurchaseDao
    abstract fun budgetCategoryDao(): BudgetCategoryDao
    abstract fun dragonStateDao(): DragonStateDao
    abstract fun aiAdviceDao(): AIAdviceDao
    abstract fun receiptItemDao(): ReceiptItemDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: DragonBudgetDatabase? = null

        fun getDatabase(context: Context): DragonBudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DragonBudgetDatabase::class.java,
                    "dragonbudget.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
