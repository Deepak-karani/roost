

# Bringing Multimodal Gemma 4 E2B to the Edge: A Deep Dive into LiteRT-LM and Qualcomm QNN

Running Large Language Models (LLMs) on mobile devices used to be a futuristic dream. Today, with Google's release of the **Gemma 4** family and the powerful **LiteRT-LM** framework, deploying highly capable, multimodal models directly on a smartphone is not just possible—it's highly efficient. 

In this post, we'll explore the architectural leaps in Gemma 4, how LiteRT-LM orchestrates on-device inference, the role of Qualcomm's QNN for NPU acceleration, and the practical engineering changes required to build a production-ready Android application for a 2.58GB AI model.

---

## The Gemma 4 Architecture

Google's Gemma 4 represents a significant leap for open-weight edge models. The **Gemma 4 E2B** (Effective ~2 Billion parameter) model is specifically tailored for mobile and edge environments, bringing several major innovations:

1. **Native Multimodality**: Unlike previous iterations that were text-only, Gemma 4 natively understands Text, Images, and Audio. It utilizes a variable aspect ratio vision encoder, allowing it to process images at various resolutions without degrading context.
2. **Per-Layer Embeddings (PLE)**: This technique reduces the memory footprint of embeddings, crucial for devices with shared RAM architectures like Android phones.
3. **Shared KV Cache**: Enhances the efficiency of context management, allowing the model to comfortably handle up to a **32K token context window** while minimizing memory spikes.

---

## The Engine Architecture: LiteRT & LiteRT-LM

To run a model of this complexity on Android, we use **LiteRT** (formerly TensorFlow Lite) alongside **LiteRT-LM** (LiteRT for Large Models). Understanding the distinction between the two is key to mastering on-device AI.

### LiteRT: The Execution Layer
At the lowest level, **LiteRT** is the runtime engine responsible for executing the static computation graph of the neural network. It takes the mathematical operations (MatMul, Add, Softmax) defined in the `.litertlm` (or `.tflite`) file and maps them to physical hardware via **Delegates**.
- **XNNPack Delegate (CPU)**: Highly optimized vector math for ARM cores.
- **OpenCL / ML Drift (GPU)**: Leverages parallel compute capabilities on the Adreno GPU.
- **QNN Delegate (NPU)**: Offloads quantized integer operations to the Hexagon NPU for maximum efficiency.

### LiteRT-LM: The Orchestration Layer
Large Language Models require much more than just graph execution. They require an active, stateful loop. **LiteRT-LM** acts as a C++ orchestration layer (wrapped elegantly in a Kotlin/JNI API) that sits *above* LiteRT. It abstracts away the massive complexity of LLM inference:

1. **Native Tokenization & Detokenization**: Instead of relying on Python or slow Java implementations, LiteRT-LM binds directly to a highly optimized C++ tokenizer (typically SentencePiece or BPE) to encode text, audio, and vision tokens into the integer arrays Gemma expects.
2. **KV Cache Management**: In LLMs, predicting the next word requires remembering the previous context. LiteRT-LM automatically allocates, manages, and updates the Key-Value (KV) cache tensors for every token generated. For Gemma 4, it expertly handles the new **Shared KV Cache** architecture to minimize memory footprint.
3. **Sampler Configuration**: It manages the auto-regressive generation loop, applying algorithms like Top-K and Top-P sampling natively in C++ before feeding the selected token back into the LiteRT graph.
4. **Multimodal Content Routing**: It packages arrays of `Content.ImageFile` and `Content.AudioBytes`, routes them through the specific Vision/Audio encoder sub-graphs, and projects them into the unified embedding space alongside text tokens.

### Enter Qualcomm QNN

The Samsung S25 Ultra (powered by the Snapdragon 8 Elite) features a highly capable Hexagon NPU. By utilizing the **Qualcomm Neural Network (QNN) Delegate**, LiteRT can offload the massive matrix multiplications of the transformer blocks from the CPU/GPU directly to the NPU. This drastically reduces power consumption and thermal throttling, maximizing Tokens-per-Second (TPS).

However, utilizing the NPU requires the model to contain specific, pre-compiled QNN payload binaries (`TF_LITE_AUX`). If these are missing, a robust app must gracefully fall back to the GPU (using OpenCL/ML Drift) or the CPU (using XNNPack).

---

## System Architecture & Engineering Changes

When upgrading our Android application from Gemma 3 to Gemma 4, we encountered several engineering hurdles—from API shifts to handling a massive 2.58GB payload under modern Android scoped storage rules.

Here is a visual representation of how the new on-device inference pipeline operates:

```mermaid
graph TD
    %% User Inputs
    subgraph UI["User Interface"]
        T[Text Input]
        I[Image Gallery]
        A[Audio Recorder]
    end

    %% Framework Layer
    subgraph LRLM["LiteRT-LM Framework"]
        Router[Backend Router / Manager]
        Config[EngineConfig + CacheDir]
        Tokenizer[Native Tokenizer]
    end

    %% Hardware Backends
    subgraph HW["Snapdragon 8 Elite Hardware"]
        NPU["Hexagon NPU<br>(via QNN Delegate)"]
        GPU["Adreno GPU<br>(via OpenCL)"]
        CPU["Oryon CPU<br>(via XNNPack)"]
    end

    %% Connections
    T & I & A -->|Contents| Router
    Router -->|Initialize Engine| Config
    Config --> Tokenizer
    
    %% Fallback Logic
    Tokenizer -->|Primary| NPU
    NPU -.->|Fallback (Missing TF_LITE_AUX)| GPU
    GPU -.->|Fallback| CPU
    
    NPU & GPU & CPU -->|Generated Tokens| UI
```

### Key Implementation Upgrades

#### 1. Upgrading the LiteRT-LM Core
Gemma 4's architecture (specifically PLE and the new shared KV cache) requires the latest runtime. We updated our build configuration to pull `latest.release` of the LiteRT-LM Android libraries, ensuring compatibility with the new model structure.

#### 2. Multimodal API Integration
To unlock Gemma 4's vision and audio capabilities, we overhauled our `LiteRTLMManager`. We moved away from text-only `sendMessage()` calls to the new multimodal `Contents` builder:
```kotlin
val contentParts = mutableListOf<Content>()
contentParts.add(Content.ImageFile(imagePath))
contentParts.add(Content.AudioBytes(audioBytes))
contentParts.add(Content.Text(textPrompt))
val contents = Contents.of(*contentParts.toTypedArray())
```
Additionally, the `EngineConfig` was updated to allocate backend delegates specifically for vision and audio sub-models (e.g., routing Vision to the GPU while keeping Audio on the CPU).

#### 3. Overcoming the 2.58GB Payload Barrier (ADB Push Workflow)
Downloading a 2.58GB model over a standard HTTP connection directly within an Android app is inherently unstable. Mobile networks drop, `HttpURLConnection` struggles with redirects, and Android 15's **Scoped Storage** heavily restricts app access to public folders like `/sdcard/Download/`.

**The Solution:** We implemented a direct ADB-push detection system. 
Instead of relying on in-app downloads, developers and users can download the model securely on their PC and push it directly into the app's unrestricted external data directory:
```bash
adb push gemma-4-E2B-it.litertlm /sdcard/Android/data/com.example.qnn_litertlm_gemma/files/
```
The app's `ModelDownloader` class intercepts this on startup, validates the payload size, and instantly initializes the engine without copying or downloading, reducing load times drastically and eliminating SocketExceptions.

#### 4. The NPU → GPU → CPU Fallback Chain
Because open-source model weights (like those on Hugging Face) often lack the device-specific QNN pre-compiled binaries, forcing an NPU connection can cause the app to crash. 

We implemented a lazy-loading `BackendFactory` that attempts to spin up the NPU. If it throws a `LiteRtLmJniException` (specifically reporting `TF_LITE_AUX not found`), the app instantly catches the exception, abandons the NPU, and seamlessly initializes the OpenCL GPU backend instead.

---

## Conclusion

Deploying Gemma 4 E2B on an Android device is a testament to how fast edge AI is moving. By combining Google's highly optimized models, the abstraction of LiteRT-LM, and Qualcomm's hardware acceleration, we can achieve true, private, multimodal intelligence right in our pockets. 

With the engineering bottlenecks of storage permissions, fallback routing, and dependency management solved, the foundation is set to build the next generation of privacy-first, context-aware mobile applications.
