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

    /** Overall weekly budget set by the user (settings.overallWeeklyBudget). */
    val overallBudget: StateFlow<Double> = repo.getSettings()
        .map { it?.overallWeeklyBudget ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Sum of all per-category weeklyLimit values. */
    val allocatedTotal: StateFlow<Double> = _categoriesWithSpent
        .map { list -> list.sumOf { it.weeklyLimit } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    /** Money in the overall budget that hasn't been allocated to any category. */
    val unallocated: StateFlow<Double> = combine(overallBudget, allocatedTotal) { overall, alloc ->
        (overall - alloc).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        refreshCategories()
        // Auto-refresh when purchases or category limits change.
        viewModelScope.launch {
            combine(
                repo.getAllPurchases(),
                repo.getAllCategories()
            ) { _, _ -> Unit }.collect {
                refreshCategories()
            }
        }
    }

    private fun refreshCategories() {
        viewModelScope.launch {
            _categoriesWithSpent.value = repo.getCategoriesWithSpent()
        }
    }

    /**
     * Set a category's weekly allocation. Returns the unallocated remainder
     * after the change via [onResult] when the change is rejected because it
     * would push total allocations past the overall budget.
     */
    fun updateLimit(name: String, newLimit: Double, onResult: (UpdateLimitResult) -> Unit = {}) {
        viewModelScope.launch {
            val overall = repo.getSettingsOnce().overallWeeklyBudget
            val cats = repo.getCategoriesWithSpent()
            val current = cats.find { it.name == name }?.weeklyLimit ?: 0.0
            val otherTotal = cats.sumOf { it.weeklyLimit } - current
            val maxAllowed = (overall - otherTotal).coerceAtLeast(0.0)
            if (newLimit > maxAllowed + 0.001) {
                onResult(UpdateLimitResult.Rejected(maxAllowed))
                return@launch
            }
            repo.updateCategoryLimit(name, newLimit)
            refreshCategories()
            onResult(UpdateLimitResult.Accepted)
        }
    }

    /**
     * Add a new user category. Rejects the requested allocation if it
     * would push total allocations past the overall budget. A new
     * category with weeklyLimit = 0 is always allowed (just an empty
     * bucket the user can fill in later).
     */
    fun addCategory(
        name: String,
        emoji: String,
        weeklyLimit: Double = 0.0,
        onResult: (AddCategoryResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (weeklyLimit > 0) {
                val overall = repo.getSettingsOnce().overallWeeklyBudget
                val cats = repo.getCategoriesWithSpent()
                val otherTotal = cats.sumOf { it.weeklyLimit }
                val maxAllowed = (overall - otherTotal).coerceAtLeast(0.0)
                if (weeklyLimit > maxAllowed + 0.001) {
                    onResult(AddCategoryResult.OverAllocated(maxAllowed))
                    return@launch
                }
            }
            val ok = repo.addCategory(name, emoji, weeklyLimit)
            onResult(if (ok) AddCategoryResult.Accepted else AddCategoryResult.NameTaken)
        }
    }

    /**
     * Set the overall weekly budget. Refuses values lower than the sum
     * of existing category allocations — clean way to enforce the
     * sub ≤ overall invariant.
     */
    fun setOverallBudget(amount: Double, onResult: (SetOverallResult) -> Unit = {}) {
        viewModelScope.launch {
            if (amount <= 0.0) {
                onResult(SetOverallResult.Invalid)
                return@launch
            }
            val cats = repo.getCategoriesWithSpent()
            val allocated = cats.sumOf { it.weeklyLimit }
            if (amount + 0.001 < allocated) {
                onResult(SetOverallResult.BelowAllocations(allocated))
                return@launch
            }
            repo.setOverallWeeklyBudget(amount)
            refreshCategories()
            onResult(SetOverallResult.Accepted)
        }
    }

    fun deleteCategory(name: String) {
        viewModelScope.launch {
            val cat = repo.getAllCategories().first().firstOrNull { it.name == name }
            if (cat != null) repo.deleteCategory(cat)
        }
    }

    fun resetBudget() {
        viewModelScope.launch {
            repo.clearAllPurchases()
            repo.resetDragonState()
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

sealed interface UpdateLimitResult {
    data object Accepted : UpdateLimitResult
    /** New limit was rejected; the caller can offer up to [maxAllowed] more. */
    data class Rejected(val maxAllowed: Double) : UpdateLimitResult
}

sealed interface AddCategoryResult {
    data object Accepted : AddCategoryResult
    data object NameTaken : AddCategoryResult
    data class OverAllocated(val maxAllowed: Double) : AddCategoryResult
}

sealed interface SetOverallResult {
    data object Accepted : SetOverallResult
    data object Invalid : SetOverallResult
    /** Overall must be ≥ existing allocations. */
    data class BelowAllocations(val allocated: Double) : SetOverallResult
}
