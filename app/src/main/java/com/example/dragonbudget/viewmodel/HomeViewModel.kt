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

    /**
     * Fraction of the weekly budget still unspent. 1f = none spent yet,
     * 0f = exactly on budget, negative = over budget. If no budget is
     * configured, returns 1f so the dragon doesn't fake-sleep.
     */
    val moneyLeftRatio: StateFlow<Float> = combine(
        _totalSpentThisWeek, _totalBudgetThisWeek
    ) { spent, budget ->
        if (budget <= 0.0) 1f
        else ((budget - spent) / budget).toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1f)

    init {
        viewModelScope.launch {
            repo.seedIfNeeded()
            refreshBudgetTotals()
        }
        // Refresh when purchases or category limits change so the home
        // screen totals stay in sync with edits made on the Budget screen.
        viewModelScope.launch {
            combine(
                repo.getAllPurchases(),
                repo.getAllCategories()
            ) { _, _ -> Unit }.collect {
                refreshBudgetTotals()
            }
        }
    }

    fun renameDragon(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val current = repo.getDragonStateOnce()
            repo.updateDragonState(current.copy(name = trimmed))
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
