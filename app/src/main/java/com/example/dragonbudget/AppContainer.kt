package com.example.dragonbudget

import android.content.Context
import com.example.dragonbudget.data.DragonBudgetDatabase
import com.example.dragonbudget.data.DragonBudgetRepository
import com.example.dragonbudget.engine.*

/**
 * Simple dependency container. No Hilt/Dagger for hackathon simplicity.
 *
 * AI Stack:
 * - LLM: LiteRT-LM Gemma 4 on Qualcomm NPU
 * - Vision: ML Kit OCR → Smart Parser → (optional) Gemma refinement
 */
class AppContainer(context: Context) {
    val database = DragonBudgetDatabase.getDatabase(context)
    val repository = DragonBudgetRepository(database)

    // LiteRT-LM Manager (Qualcomm NPU inference)
    val liteRTLMManager by lazy { 
        com.example.qnn_litertlm_gemma.LiteRTLMManager.getInstance(context) 
    }

    // LLM Engine: Gemma 4 via LiteRT-LM on Snapdragon NPU
    val llmEngine: LocalLLMEngine by lazy { 
        LiteRtGemmaEngine(liteRTLMManager) 
    }
    
    // Vision Engine: ML Kit OCR → Smart Parser → Gemma refinement pipeline
    val visionEngine: ReceiptVisionEngine by lazy {
        MLKitReceiptVisionEngine(context, liteRTLMManager, repository)
    }

    // Callback to re-trigger initialization from UI
    var initAction: (() -> Unit)? = null
}
