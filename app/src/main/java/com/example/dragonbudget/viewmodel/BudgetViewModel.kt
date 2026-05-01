package com.example.dragonbudget.viewmodel

import androidx.lifecycle.*
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BudgetViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository

    private val _categoriesWithSpent = MutableStateFlow<List<BudgetCategoryWithSpent>>(emptyList())
    val categoriesWithSpent: StateFlow<List<BudgetCategoryWithSpent>> = _categoriesWithSpent

    init {
        refreshCategories()
        // Auto-refresh when purchases change
        viewModelScope.launch {
            repo.getAllPurchases().collect {
                refreshCategories()
            }
        }
    }

    private fun refreshCategories() {
        viewModelScope.launch {
            _categoriesWithSpent.value = repo.getCategoriesWithSpent()
        }
    }

    fun updateLimit(name: String, newLimit: Double) {
        viewModelScope.launch {
            repo.updateCategoryLimit(name, newLimit)
            refreshCategories()
        }
    }

    fun resetBudget() {
        viewModelScope.launch {
            repo.clearAllPurchases()
            repo.resetDragonState()
            refreshCategories()
        }
    }

    fun applyLifestylePreset(presetName: String) {
        viewModelScope.launch {
            val limits = Categories.LIFESTYLE_PRESETS[presetName] ?: return@launch
            limits.forEach { (name, limit) ->
                repo.updateCategoryLimit(name, limit)
            }
            refreshCategories()
        }
    }

    fun adjustCategorySpent(categoryName: String, targetSpent: Double) {
        viewModelScope.launch {
            val cats = repo.getCategoriesWithSpent()
            val current = cats.find { it.name == categoryName }?.spentAmount ?: 0.0
            val diff = targetSpent - current
            
            if (Math.abs(diff) > 0.01) {
                repo.addPurchase(
                    Purchase(
                        merchant = "Manual Adjustment",
                        amount = diff,
                        category = categoryName,
                        timestamp = System.currentTimeMillis(),
                        note = "Adjusted total to $${String.format("%.2f", targetSpent)}"
                    )
                )
                refreshCategories()
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BudgetViewModel(container) as T
        }
    }
}
