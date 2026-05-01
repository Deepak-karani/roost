package com.example.dragonbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.dragonbudget.ui.DragonBudgetNavHost
import com.example.dragonbudget.ui.theme.DragonBudgetTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = AppContainer(applicationContext)
        appContainer.initAction = { initializeAI(appContainer) }

        // Initialize the NPU model in the background
        initializeAI(appContainer)

        setContent {
            DragonBudgetTheme {
                DragonBudgetNavHost(appContainer = appContainer)
            }
        }
    }

    private fun initializeAI(appContainer: AppContainer) {
        this.lifecycleScope.launch {
            try {
                // Try multiple common paths for the model file
                val modelName = "gemma-4-E2B-it.litertlm" // Standard name from guide
                val modelNameAlt = "gemma-4-E2B-it_qualcomm_sm8750.litertlm" // Variant name
                
                val possibleDirs = listOf(
                    this@MainActivity.getExternalFilesDir(null)?.absolutePath,
                    "/sdcard/Download",
                    "/sdcard/Downloads",
                    "/storage/emulated/0/Download",
                    "/sdcard/Android/data/com.example.qnn_litertlm_gemma/files",
                    "/sdcard/Android/data/com.example.dragonbudget/files"
                ).filterNotNull()
                
                var modelFile: java.io.File? = null
                val searchedPaths = mutableListOf<String>()
                
                for (dir in possibleDirs) {
                    val f1 = java.io.File(dir, modelName)
                    searchedPaths.add(f1.absolutePath)
                    if (f1.exists()) {
                        modelFile = f1
                        break
                    }
                    val f2 = java.io.File(dir, modelNameAlt)
                    searchedPaths.add(f2.absolutePath)
                    if (f2.exists()) {
                        modelFile = f2
                        break
                    }
                }

                if (modelFile != null) {
                    appContainer.liteRTLMManager.initialize(
                        modelPath = modelFile.absolutePath,
                        preferredBackend = "NPU"
                    )
                } else {
                    android.util.Log.e("MainActivity", "AI Model NOT FOUND. Paths searched: ${searchedPaths.joinToString(", ")}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
