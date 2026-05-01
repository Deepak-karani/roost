package com.example.dragonbudget.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.*
import com.example.dragonbudget.engine.DragonStateEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AddPurchaseViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    val dragonHealth: StateFlow<Int> = repo.getDragonState()
        .map { it?.health ?: 80 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 80)

    val moneyLeftRatio: StateFlow<Float> = combine(
        repo.getAllPurchases(),
        repo.getAllCategories()
    ) { _, _ ->
        val cats = repo.getCategoriesWithSpent()
        val limit = cats.sumOf { it.weeklyLimit }
        if (limit <= 0.0) 1f
        else ((limit - cats.sumOf { it.spentAmount }) / limit).toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1f)

    /** Live list of user-managed categories — the source of truth for dropdowns. */
    val categories: StateFlow<List<BudgetCategory>> = repo.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Single-item scan (legacy)
    private val _scanResult = MutableStateFlow<PurchaseDraft?>(null)
    val scanResult: StateFlow<PurchaseDraft?> = _scanResult

    // Multi-item scan result
    private val _receiptScan = MutableStateFlow<ReceiptScanResult?>(null)
    val receiptScan: StateFlow<ReceiptScanResult?> = _receiptScan

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError

    fun savePurchase(merchant: String, amount: Double, category: String, note: String) {
        viewModelScope.launch {
            val purchase = Purchase(
                merchant = merchant,
                amount = amount,
                category = category,
                note = note
            )
            repo.addPurchase(purchase)

            val cats = repo.getCategoriesWithSpent()
            val catInfo = cats.find { it.name == category }
            val percentUsed = catInfo?.percentUsed ?: 0f
            val overallRatio = overallRatio(cats)

            val currentDragon = repo.getDragonStateOnce()
            val updatedDragon = DragonStateEngine.onPurchase(currentDragon, category, percentUsed, overallRatio)
            repo.updateDragonState(updatedDragon)

            _saved.value = true
        }
    }

    /**
     * Save the scanned receipt as ONE Purchase row using the receipt total
     * for the amount, and archive every line item as a ReceiptItem child
     * for drill-in later. The Purchase's category is the most-common
     * category across the items (falls back to "Other"). Budget math uses
     * only the Purchase row's total — items are audit data only.
     */
    fun saveAllScannedItems(merchant: String, items: List<ReceiptLineItem>) {
        viewModelScope.launch {
            val scan = _receiptScan.value ?: return@launch
            // Only save the items the user kept checked. Tip and service charge
            // (if printed on the receipt) are added on top — they're real money
            // that left the wallet, so budget math has to count them.
            val kept = items.filter { it.selected }
            val keptSum = kept.sumOf { it.price }
            val extras = scan.tip + scan.serviceCharge
            val totalToSave = when {
                kept.isNotEmpty() -> keptSum + extras
                scan.total > 0.0 -> scan.total
                else -> return@launch
            }
            if (totalToSave <= 0.0) return@launch

            // Pick the Purchase's overall category by majority among the kept items.
            val mainCategory = kept.groupBy { it.suggestedCategory }
                .maxByOrNull { it.value.size }?.key ?: "Other"

            val noteParts = mutableListOf<String>()
            if (kept.isNotEmpty()) noteParts += "${kept.size} items"
            if (scan.tip > 0) noteParts += "tip $${String.format("%.2f", scan.tip)}"
            if (scan.serviceCharge > 0) noteParts += "service $${String.format("%.2f", scan.serviceCharge)}"

            val purchase = Purchase(
                merchant = merchant.ifBlank { "Receipt" },
                amount = totalToSave,
                category = mainCategory,
                note = noteParts.joinToString(" · ")
            )
            repo.addPurchaseWithItems(purchase, kept)

            // Dragon updates against the chosen category + overall weekly ratio.
            val cats = repo.getCategoriesWithSpent()
            val catInfo = cats.find { it.name == mainCategory }
            val percentUsed = catInfo?.percentUsed ?: 0f
            val overallRatioVal = overallRatio(cats)

            val currentDragon = repo.getDragonStateOnce()
            val updatedDragon = DragonStateEngine.onPurchase(currentDragon, mainCategory, percentUsed, overallRatioVal)
            repo.updateDragonState(updatedDragon)

            _saved.value = true
        }
    }

    private fun overallRatio(cats: List<BudgetCategoryWithSpent>): Float {
        val totalLimit = cats.sumOf { it.weeklyLimit }
        if (totalLimit <= 0.0) return 0f
        val totalSpent = cats.sumOf { it.spentAmount }
        return (totalSpent / totalLimit).toFloat()
    }

    /**
     * Toggle selection of a scanned line item.
     */
    fun toggleItemSelection(index: Int) {
        val current = _receiptScan.value ?: return
        val updatedItems = current.items.toMutableList()
        if (index in updatedItems.indices) {
            val item = updatedItems[index]
            updatedItems[index] = item.copy(selected = !item.selected)
            _receiptScan.value = current.copy(items = updatedItems)
        }
    }

    fun clearScan() {
        _receiptScan.value = null
        _scanResult.value = null
        _scanError.value = null
    }

    fun scanReceipt(uri: Uri) {
        viewModelScope.launch {
            _isScanning.value = true
            _scanError.value = null
            _scanResult.value = null
            _receiptScan.value = null
            try {
                Log.d("AddPurchaseVM", "Starting multi-item receipt scan for URI: $uri")
                val raw = container.visionEngine.extractReceiptItems(uri)
                Log.d("AddPurchaseVM", "Scan result: merchant=${raw.merchant}, ${raw.items.size} items, total=${raw.total}")

                // Re-bucket every item's suggested category into one of the
                // user's actual categories. The OCR engine guesses with a
                // hardcoded keyword list (Groceries / Food / Gas / etc.); if
                // the user only has "Food", we don't want to leave dangling
                // "Groceries" categories that don't exist on their Budget screen.
                val userCats = repo.getCategoriesWithSpent().map { it.name }
                val fallback = userCats.firstOrNull() ?: Categories.FOOD
                val result = raw.copy(
                    items = raw.items.map { item ->
                        val mapped = if (item.suggestedCategory in userCats) {
                            item.suggestedCategory
                        } else fallback
                        item.copy(suggestedCategory = mapped)
                    }
                )

                _receiptScan.value = result

                if (result.items.isEmpty() && result.total > 0) {
                    _scanResult.value = PurchaseDraft(
                        merchant = result.merchant,
                        amount = result.total,
                        suggestedCategory = fallback,
                        confidence = result.confidence
                    )
                    _scanError.value = "Could not detect individual items. Total detected: \$${String.format("%.2f", result.total)}"
                } else if (result.items.isEmpty()) {
                    _scanError.value = "Could not detect any items or prices on the receipt."
                }
            } catch (e: Exception) {
                Log.e("AddPurchaseVM", "Scan failed", e)
                _scanError.value = "Scan failed: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddPurchaseViewModel(container) as T
        }
    }
}
