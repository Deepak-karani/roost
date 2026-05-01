package com.example.dragonbudget.viewmodel

import androidx.lifecycle.*
import com.example.dragonbudget.AppContainer
import com.example.dragonbudget.data.AIAdvice
import com.example.dragonbudget.engine.PromptBuilder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AskDragonViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository

    private val _response = MutableStateFlow("")
    val response: StateFlow<String> = _response

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    val recentAdvice: StateFlow<List<AIAdvice>> = repo.getRecentAdvice()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dragonName: StateFlow<String> = repo.getDragonState()
        .map { it?.name ?: "your dragon" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "your dragon")

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

    fun askDragon(question: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _response.value = ""

            try {
                val dragon = repo.getDragonStateOnce()
                val categories = repo.getCategoriesWithSpent()
                val overall = repo.getSettingsOnce().overallWeeklyBudget

                // Build the prompt
                val prompt = PromptBuilder.buildAdvicePrompt(dragon, categories, question, overall)

                // Generate response via LiteRT-LM Gemma on NPU
                val answer = container.llmEngine.generateBudgetAdvice(prompt)

                _response.value = answer

                // Save to history
                repo.saveAdvice(question, answer)
            } catch (e: Exception) {
                val name = runCatching { repo.getDragonStateOnce().name }.getOrDefault("Your dragon")
                _response.value = "$name encountered an error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AskDragonViewModel(container) as T
        }
    }
}
