package com.example.dragonbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.dragonbudget.ui.DragonBudgetNavHost
import com.example.dragonbudget.ui.theme.DragonBudgetTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        android.util.Log.i("MainActivity", "=== APP STARTING ===")
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()

            android.util.Log.i("MainActivity", "Creating AppContainer...")
            // Initialize AppContainer (this loads the DB and native library instances)
            val appContainer = AppContainer(applicationContext)

            android.util.Log.i("MainActivity", "Initializing AI background task...")
            // Start AI initialization once in the background. Wrapped so any
            // failure (missing model file, NPU init crash) doesn't take down
            // the UI — the rest of the app still works with regex fallbacks.
            initializeAI(appContainer)

            android.util.Log.i("MainActivity", "Setting content...")
            setContent {
                DragonBudgetTheme {
                    DragonBudgetNavHost(appContainer = appContainer)
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "CRITICAL STARTUP ERROR", e)
            setContent {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Roost encountered a critical error on launch.\nError: ${e.localizedMessage ?: "Unknown"}",
                        color = Color.Red,
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    private fun initializeAI(appContainer: AppContainer) {
        this.lifecycleScope.launch {
            try {
                val downloader = com.example.qnn_litertlm_gemma.ModelDownloader(applicationContext)

                // Pick the first model in AVAILABLE_MODELS that's actually present on the device.
                val available = com.example.qnn_litertlm_gemma.ModelDownloader.AVAILABLE_MODELS
                val present = available.firstOrNull { downloader.isModelDownloaded(it) }

                if (present == null) {
                    android.util.Log.e(
                        "MainActivity",
                        "AI model NOT FOUND. Push one of these via ADB to /sdcard/Android/data/com.example.dragonbudget/files/ : " +
                            available.joinToString(", ") { it.filename }
                    )
                    return@launch
                }

                val modelPath = downloader.getModelPath(present)
                // libLiteRtDispatch_Qualcomm.so was patched (patchelf --add-needed
                // libLiteRt.so) so NPU dispatch can resolve LiteRtGetEnvironmentOptions.
                // If NPU still fails, the manager falls back to GPU then CPU.
                val backend = present.preferredBackend ?: "NPU"
                android.util.Log.i("MainActivity", "Initializing AI with model: ${present.filename} at $modelPath (backend=$backend)")

                val result = appContainer.liteRTLMManager.initialize(
                    modelPath = modelPath,
                    systemPrompt = present.systemPrompt,
                    preferredBackend = backend
                )
                if (result.isSuccess) {
                    android.util.Log.i(
                        "MainActivity",
                        "AI ready on backend: ${appContainer.liteRTLMManager.getActiveBackendName()}"
                    )
                } else {
                    android.util.Log.e("MainActivity", "AI init failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Throwable) {
                android.util.Log.e("MainActivity", "AI init threw", e)
            }
        }
    }
}
