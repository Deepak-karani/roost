package com.example.dragonbudget.viewmodel

import androidx.lifecycle.*
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.Purchase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val allPurchases = repo.getAllPurchases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<Purchase>> = combine(allPurchases, _selectedCategory) { all, cat ->
        if (cat == null) all else all.filter { it.category == cat }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpent: StateFlow<Double> = purchases.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun clearAll() {
        viewModelScope.launch {
            repo.clearAllPurchases()
            repo.resetDragonState()
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(container) as T
        }
    }
}
