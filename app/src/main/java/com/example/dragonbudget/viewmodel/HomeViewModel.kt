package com.example.dragonbudget.viewmodel

import androidx.lifecycle.*
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository

    val dragonState: StateFlow<DragonState> = repo.getDragonState()
        .map { it ?: DragonState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DragonState())

    val recentPurchases: StateFlow<List<Purchase>> = repo.getRecentPurchases(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _totalSpentThisWeek = MutableStateFlow(0.0)
    val totalSpentThisWeek: StateFlow<Double> = _totalSpentThisWeek

    private val _totalBudgetThisWeek = MutableStateFlow(0.0)
    val totalBudgetThisWeek: StateFlow<Double> = _totalBudgetThisWeek

    private val _topCategory = MutableStateFlow<BudgetCategoryWithSpent?>(null)
    val topCategory: StateFlow<BudgetCategoryWithSpent?> = _topCategory

    init {
        viewModelScope.launch {
            repo.seedIfNeeded()
            refreshBudgetTotals()
        }
        // Refresh when purchases change
        viewModelScope.launch {
            repo.getAllPurchases().collect {
                refreshBudgetTotals()
            }
        }
    }

    private suspend fun refreshBudgetTotals() {
        val cats = repo.getCategoriesWithSpent()
        _totalSpentThisWeek.value = cats.sumOf { it.spentAmount }
        _totalBudgetThisWeek.value = cats.sumOf { it.weeklyLimit }
        _topCategory.value = cats.filter { it.spentAmount > 0 }.maxByOrNull { it.spentAmount }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(container) as T
        }
    }
}
