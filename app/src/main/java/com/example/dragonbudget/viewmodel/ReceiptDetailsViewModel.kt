package com.example.dragonbudget.viewmodel

import androidx.lifecycle.*
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.Purchase
import com.example.dragonbudget.data.ReceiptItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Reads one Purchase + its archived ReceiptItem children.
 *
 * The Purchase itself is loaded once (suspend) — Room exposes purchases
 * only as a list flow today, so we filter client-side here. Cheap given
 * the small dataset.
 */
class ReceiptDetailsViewModel(
    private val container: AppContainer,
    private val purchaseId: Long
) : ViewModel() {

    private val repo = container.repository

    private val _purchase = MutableStateFlow<Purchase?>(null)
    val purchase: StateFlow<Purchase?> = _purchase

    val items: StateFlow<List<ReceiptItem>> =
        repo.getReceiptItems(purchaseId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _purchase.value = repo.getAllPurchases()
                .first()
                .firstOrNull { it.id == purchaseId }
        }
    }

    class Factory(
        private val container: AppContainer,
        private val purchaseId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiptDetailsViewModel(container, purchaseId) as T
        }
    }
}
