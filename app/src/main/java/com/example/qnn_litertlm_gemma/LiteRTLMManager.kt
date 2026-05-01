package com.example.qnn_litertlm_gemma

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.LogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Data class for model performance metrics
 */
data class PerformanceMetrics(
    val initializationTimeMs: Long = 0,
    val timeToFirstTokenMs: Long = 0,
    val tokensPerSecond: Double = 0.0,
    val activeBackend: String = "Unknown",
    val memoryUsageMb: Long = 0
)

/**
 * Singleton manager for LiteRT-LM Engine.
 * Handles model initialization, conversation management,
 * and multimodal message sending (text, image, audio).
 *
 * Backend fallback chain: NPU → GPU → CPU
 */
class LiteRTLMManager private constructor(private val context: Context) {
    
    private var engine: Engine? = null
    private var conversation: com.google.ai.edge.litertlm.Conversation? = null
    private var isInitialized = false
    private var currentBackendName: String = "CPU"

    /**
     * Check if the LiteRT-LM engine is initialized and ready for inference.
     */
    fun isEngineReady(): Boolean = isInitialized && engine != null
    
    companion object {
        private const val TAG = "LiteRTLMManager"
        
        init {
            try {
                // Primary native library loading sequence
                Log.i(TAG, "Attempting to load native AI libraries...")
                
                val libs = listOf(
                    "LiteRt", // CORE FIRST to expose symbols like LiteRtGetEnvironmentOptions
                    "QnnSystem", "QnnHtp", "QnnHtpV79Stub", 
                    "GemmaModelConstraintProvider", 
                    "LiteRtDispatch_Qualcomm", "LiteRtGpuAccelerator", 
                    "LiteRtOpenClAccelerator"
                )
                
                for (lib in libs) {
                    try {
                        System.loadLibrary(lib)
                        Log.d(TAG, "Loaded $lib")
                    } catch (e: UnsatisfiedLinkError) {
                        Log.w(TAG, "Optional library $lib not found: ${e.message}")
                    }
                }
                Log.i(TAG, "Native library loading sequence completed")
            } catch (e: Throwable) {
                Log.e(TAG, "Critical failure in native library loading: ${e.message}")
            }
        }

        @Volatile
        private var INSTANCE: LiteRTLMManager? = null
        
        fun getInstance(context: Context): LiteRTLMManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LiteRTLMManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Initialize the LiteRT-LM Engine with the specified model.
     * Uses NPU → GPU → CPU fallback chain.
     */
    suspend fun initialize(
        modelPath: String,
        systemPrompt: String? = null,
        isEmbedding: Boolean = false,
        preferredBackend: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        Log.i(TAG, "Initializing for: $modelPath (preferred: $preferredBackend)")
        if (isInitialized) {
            cleanup()
        }
        
        try {
            if (isEmbedding) {
                Log.w(TAG, "Embedding mode not supported in this version")
                currentBackendName = "CPU"
            } else {
                // Build ordered backend list based on preference
                val backends = buildBackendList(preferredBackend)
                initializeEngineWithFallback(modelPath, backends)
            }
            isInitialized = true
            Log.i(TAG, "Initialization SUCCEEDED on backend: $currentBackendName")
            Result.success(true)
        } catch (e: Throwable) {
            Log.e(TAG, "Initialization FAILED: ${e.message}", e)
            Result.failure(Exception(e))
        }
    }

    /**
     * Build an ordered list of backends to try.
     * NPU → GPU → CPU by default; shifts based on preference.
     */
    private fun buildBackendList(preferred: String?): List<BackendFactory> {
        val allBackends = listOf(
            BackendFactory("NPU") {
                val libDir = context.applicationInfo.nativeLibraryDir
                Log.i(TAG, "Using native library dir for NPU: $libDir")
                Backend.NPU(nativeLibraryDir = libDir)
            },
            BackendFactory("GPU") { Backend.GPU() },
            BackendFactory("CPU") { Backend.CPU() }
        )

        if (preferred == null) return allBackends

        val preferredUpper = preferred.uppercase()
        val preferredIdx = allBackends.indexOfFirst { it.name == preferredUpper }

        return if (preferredIdx > 0) {
            listOf(allBackends[preferredIdx]) + allBackends.filterIndexed { i, _ -> i != preferredIdx }
        } else {
            allBackends
        }
    }

    /**
     * Try each backend in order; stop at the first one that works.
     */
    private fun initializeEngineWithFallback(modelPath: String, backends: List<BackendFactory>) {
        var lastError: Throwable? = null
        
        for (factory in backends) {
            try {
                Log.i(TAG, "Trying backend: ${factory.name}")
                initializeEngine(modelPath, factory)
                Log.i(TAG, "Backend ${factory.name} SUCCEEDED")
                return
            } catch (e: Throwable) {
                Log.w(TAG, "Backend ${factory.name} failed: ${e.message}")
                lastError = e
            }
        }
        
        throw lastError ?: IllegalStateException("All backends failed")
    }

    private fun initializeEngine(modelPath: String, factory: BackendFactory) {
        val file = File(modelPath)
        if (!file.exists()) {
            throw java.io.FileNotFoundException("Model file not found at $modelPath")
        }
        if (!file.canRead()) {
            throw java.io.IOException("Model file not readable (permissions?)")
        }
        Log.i(TAG, "Model file: ${file.length()} bytes, backend: ${factory.name}")

        val backend = factory.create()
        
        Log.i(TAG, "Initializing Engine with backend: ${factory.name}")
        
        // CRITICAL for NPU on some devices: point to where SKEL files are
        try {
            val libDir = factory.nativeLibraryDir ?: context.applicationInfo.nativeLibraryDir
            android.system.Os.setenv("ADSP_LIBRARY_PATH", libDir, true)
            Log.i(TAG, "Set ADSP_LIBRARY_PATH to $libDir")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set ADSP_LIBRARY_PATH: ${e.message}")
        }

        val engineConfig = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            // Cache dir is CRITICAL for JIT compilation speed
            cacheDir = context.cacheDir.path
        )
        
        Log.i(TAG, "Starting Engine.initialize() on ${factory.name}...")
        val startTime = System.currentTimeMillis()
        val candidateEngine = Engine(engineConfig)
        
        try {
            // Using a background thread to prevent UI jank during heavy JIT
            candidateEngine.initialize()
            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "Engine initialization SUCCEEDED in ${duration}ms")
            
            if (duration > 30000 && factory.name == "NPU") {
                Log.w(TAG, "Initialization took >30s. HIGHLY RECOMMENDED to AOT compile the model to reduce this to <5s.")
            }
            
            // Verify conversation works
            val testConv = candidateEngine.createConversation(ConversationConfig())
            testConv.close()
        } catch (e: Throwable) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "Engine initialization FAILED after ${duration}ms: ${e.message}")
            if (factory.name == "NPU" && e.message?.contains("TF_LITE_AUX") == true) {
                Log.e(TAG, "NPU missing AOT payload and JIT compilation failed or was not triggered.")
            }
            candidateEngine.close()
            throw e
        }

        engine = candidateEngine
        currentBackendName = factory.name
    }

    /**
     * Start a new conversation. Closes any existing one first — LiteRT-LM only
     * permits a single live session per engine, and trying to create a second
     * one fails with "FAILED PRECONDITION: a session already exists".
     */
    fun startConversation(systemPrompt: String? = null) {
        if (!isInitialized || engine == null) {
            throw IllegalStateException("Engine not initialized.")
        }

        try {
            conversation?.close()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to close prior conversation: ${e.message}")
        }
        conversation = null

        val conversationConfig = ConversationConfig(
            systemInstruction = if (systemPrompt != null) Contents.of(systemPrompt) else null,
            samplerConfig = SamplerConfig(
                temperature = 0.7,
                topK = 40,
                topP = 0.9
            )
        )
        conversation = engine?.createConversation(conversationConfig)
    }

    /**
     * Send a text-only message and stream the response.
     */
    fun sendMessage(text: String): Flow<String> {
        ensureConversation()
        return conversation!!.sendMessageAsync(text).map { msg ->
            msg.toString()
        }
    }

    /**
     * Send a multimodal message with optional image and/or audio.
     * @param text The text prompt
     * @param imagePath Optional path to an image file
     * @param audioBytes Optional raw audio bytes
     */
    fun sendMultimodalMessage(
        text: String,
        imagePath: String? = null,
        audioBytes: ByteArray? = null
    ): Flow<String> {
        ensureConversation()
        
        val contentParts = mutableListOf<Content>()
        
        // Add image if provided
        if (imagePath != null) {
            contentParts.add(Content.ImageFile(imagePath))
        }
        
        // Add audio if provided
        if (audioBytes != null) {
            contentParts.add(Content.AudioBytes(audioBytes))
        }
        
        // Always add text
        contentParts.add(Content.Text(text))
        
        val contents = Contents.of(*contentParts.toTypedArray())
        
        return conversation!!.sendMessageAsync(contents).map { msg ->
            msg.toString()
        }
    }

    private fun ensureConversation() {
        if (!isInitialized || engine == null) {
            throw IllegalStateException("Engine not initialized.")
        }
        if (conversation == null) {
            startConversation()
        }
    }

    fun getActiveBackendName(): String = currentBackendName

    fun getMemoryUsageMb(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }
    
    fun cleanup() {
        try {
            conversation?.close()
            engine?.close()
            conversation = null
            engine = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}

/**
 * Factory for creating backends lazily.
 * This avoids constructing NPU/GPU backends that might throw
 * before we're ready to handle the exception.
 */
private data class BackendFactory(
    val name: String,
    val nativeLibraryDir: String? = null,
    val create: () -> Backend
)
