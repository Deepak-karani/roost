# ModelGarden-QNN-LiteRT — Gemma 4 Models On-Device Chat

A premium **multimodal** on-device LLM chat application for Android, powered by **Google LiteRT-LM**. Features **Gemma 4 models** with support for **text, image, and audio** inputs, running entirely on-device with **NPU/GPU/CPU** acceleration.

## [Gemma 4 Models](https://ai.google.dev/gemma/docs/core)

[Gemma 4](https://ai.google.dev/gemma/docs/core) is Google's latest family of open models, built from the same research as Gemini.

*   **Multimodal**: Understands **text + images + audio** natively
*   **Architecture**: Per-Layer Embeddings (PLE), Shared KV Cache, variable aspect ratio vision encoder
*   **Context**: Up to 32K tokens
*   **License**: Apache 2.0 (fully open)
*   **Collection**: [HuggingFace Gemma 4 Collection](https://huggingface.co/collections/google/gemma-4)

## Features

*   **Gemma 4 models** as the default on-device models
*   **Multimodal Input**: Attach images from gallery and record audio directly in-app
*   **NPU → GPU → CPU** backend fallback for optimal performance on Snapdragon 8 Elite
*   **ADB Push Support**: Push the model from PC — no in-app download needed for large files
*   **Multi-Model Support**: Switch between Gemma 4, Gemma 3n, Qwen 3, Gemma 3 1B
*   **Real-time Benchmarks**: TTFT, tokens/sec, token count
*   **Modern Premium UI**: Deep Blue & Soft Gray aesthetic with streaming responses

## Benchmarks (Samsung S25 Ultra - Snapdragon 8 Elite)

| Metric | Gemma 4 | Gemma 3n | Qwen 3 0.6B |
| :--- | :--- | :--- | :--- |
| **Model Size** | Variable | ~1.5 GB | ~0.5 GB |
| **Modalities** | Text + Image + Audio | Text | Text |
| **Context Length** | 32K | 8K | 4K |
| **Backend** | NPU/GPU/CPU | GPU/CPU | GPU/CPU |

> **Note**: Performance benchmarks from the [Gemma 4 family](https://ai.google.dev/gemma/docs/core) show excellent throughput on Android with GPU acceleration via XNNPack and ML Drift.

## Setup & Installation

[![Open In Colab](https://colab.research.google.com/assets/colab-badge.svg)](https://colab.research.google.com/github/carrycooldude/ModelGarden-QNN-LiteRT/blob/main/google_colab/LiteRT_Gemma4_NPU_AOT_Compilation.ipynb)

### Prerequisites
*   Android Studio Ladybug (or newer)
*   Samsung S25 Ultra (or any Android 10+ device with ARM64)
*   ~3GB free storage for the model

### 1. Clone the Repository
```bash
git clone https://github.com/carrycooldude/ModelGarden-QNN-LiteRT.git
cd ModelGarden-QNN-LiteRT
```

### 2. Build & Install the APK
```bash
./gradlew installDebug
```

### 3. Push the Model via ADB (Recommended)

Download the **LiteRT Community Gemma 4** model on your PC from HuggingFace:
```bash
# Download the LiteRT Community Gemma 4 model
curl -L -o gemma-4.litertlm "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm"

# Push to phone
adb push gemma-4.litertlm /sdcard/Download/
```

Or use the Hugging Face CLI:
```bash
pip install huggingface_hub
# Download the LiteRT Community Gemma 4 model
huggingface-cli download litert-community/gemma-4-E2B-it-litert-lm gemma-4-E2B-it.litertlm --local-dir .
mv gemma-4-E2B-it.litertlm gemma-4.litertlm
adb push gemma-4.litertlm /sdcard/Download/
```

The app will automatically detect the model in `/sdcard/Download/` on launch.

### 4. Usage
1.  **Launch the App**: The app detects the Gemma 4 model and initializes (NPU → GPU → CPU)
2.  **Chat**: Type messages for text-only conversations
3.  **Image Input**: Tap the gallery button to attach an image, then ask about it
4.  **Audio Input**: Tap the mic button to record audio, tap again to stop
5.  **Switch Models**: Settings → Select Model to try other models
6.  **Benchmarks**: Watch real-time TTFT and tokens/sec in the header

## Notes on Hardware Acceleration

*   The app tries **NPU** first (Qualcomm Hexagon on Snapdragon 8 Elite), then falls back to **GPU** (OpenCL/ML Drift), then **CPU** (XNNPack)
*   NPU requires device-specific libraries — falls back gracefully if unavailable
*   `cacheDir` is used for faster model reloading on subsequent launches

## References

*   [Gemma 4 Overview](https://ai.google.dev/gemma/docs/core)
*   [Gemma 4 Model Card](https://ai.google.dev/gemma/docs/core/model_card_4)
*   [HuggingFace Gemma 4 Collection](https://huggingface.co/collections/google/gemma-4)
*   [HuggingFace Gemma 4 Blog](https://huggingface.co/blog/gemma4)
*   [LiteRT-LM Android Guide](https://ai.google.dev/edge/litert-lm/android)
*   [LiteRT-LM Models](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)

## Demo

https://github.com/user-attachments/assets/4c3c494e-a119-45d5-9726-4e43b2351ed9

## License
Apache 2.0
