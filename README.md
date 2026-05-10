# Strip AI

> Find the AI models hiding on your Android device.

Strip AI scans every installed app and surfaces bundled on-device AI/ML models — TFLite, ONNX, PyTorch, GGUF, MediaPipe, and more — that were silently shipped or downloaded to your phone without your explicit consent. No root required. Fully offline.

---

## Why this exists

Every major app now ships ML models directly inside the APK or downloads them silently at first launch. Your keyboard predicts your words. Your camera classifies your scenes. Your launcher ranks your notifications. None of it was disclosed. Strip AI makes it visible.

---

## Screenshots

| Idle | Scanning | Results |
|------|----------|---------|
| *(tap Scan Device)* | *(progress + current app)* | *(grouped by company)* |

---

## Features

- **Deep APK inspection** — opens every installed APK as a ZIP and walks its entries, including split APKs from App Bundles
- **Broad model detection** — recognises TFLite, ONNX, PyTorch, ExecuTorch, GGUF/GGML (on-device LLMs), MediaPipe, Qualcomm DLC, MNN, NCNN, PaddlePaddle, MindSpore, Caffe, CoreML, and generic weight files
- **Magic byte verification** — validates file headers (TFL3 for TFLite, GGUF magic, PK zip header for PyTorch) to eliminate false positives from extension-only matching
- **Runtime library detection** — catches apps that ship ML runtimes (TFLite JNI, ONNX Runtime, MediaPipe, SNPE, Samsung NNC, etc.) even without bundled model files
- **Storage monitoring** — scans external/download storage via MediaStore for model files apps pull down after install (`.gguf` LLM weights, `.tflite`, `.onnx`, etc.)
- **Known AI services** — flags pre-installed OEM AI packages: Google AI Core, Bixby, MIUI AI, Huawei HiAI, and more
- **Grouped results** — apps sorted by company (Google, Samsung, Other OEM, Third-Party) and ranked by AI footprint within each group
- **ADB command generator** — tap "Copy ADB disable" on any app to copy `adb shell pm disable-user --user 0 <pkg>` to clipboard; no root needed, fully reversible
- **Share report** — exports a 1080×1350 summary card as PNG via the system share sheet, ready for Reddit/Twitter
- **Risk levels** — 🔴 High (>100 MB), 🟠 Medium (>10 MB), 🟡 Low (<10 MB), 🔵 Runtime only
- **No root** — reads APK paths from `PackageManager`, which are world-readable by default
- **Fully offline** — zero network calls, zero analytics, zero telemetry

---

## Tech stack

| Layer | Choice |
|-------|--------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | Single-activity MVVM with `StateFlow` |
| Async | Coroutines on `Dispatchers.IO` |
| APK parsing | `java.util.zip.ZipFile` |
| Storage scan | `MediaStore` (Downloads + Files) |
| App icons | Accompanist `DrawablePainter` |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 |
| Backend | None |

---

## Project structure

```
app/src/main/java/com/stripai/app/
├── MainActivity.kt             # Entry point, storage permission request
├── scanner/
│   ├── ModelSignature.kt       # Extensions, magic bytes, .bin patterns, runtime .so names, OEM packages
│   ├── ApkScanner.kt           # Opens APKs as ZIPs, emits progress, returns ScanResult
│   ├── StorageScanner.kt       # MediaStore scan for downloaded model files
│   └── ScanResult.kt           # Data classes + RiskLevel enum
└── ui/
    ├── MainViewModel.kt        # ScanUiState sealed class, launches scanner in viewModelScope
    ├── MainScreen.kt           # Idle / Scanning / Results screens in Compose
    ├── ShareUtils.kt           # Canvas-drawn share card + system share intent
    └── theme/
        ├── Color.kt            # Dark palette, red accent #FF3B30
        ├── Theme.kt            # Material3 dark-only theme
        └── Type.kt
```

---

## Build

### Prerequisites

- Android Studio (bundles the JDK)
- Android SDK 35 (installed via Android Studio setup wizard)
- A physical Android device with USB debugging enabled — emulators have no interesting apps to scan

### Steps

```bash
# 1. Clone
git clone https://github.com/kesavan-vaisakh/Strip-AI.git
cd Strip-AI

# 2. Point Gradle at your SDK
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# 3. Build
./gradlew assembleDebug

# 4. Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and hit **Run**.

---

## How the scanner works

```
MainActivity → request READ_EXTERNAL_STORAGE (API ≤ 32 only)

PackageManager.getInstalledApplications()
    └── for each app:
            ApplicationInfo.sourceDir          → open as ZipFile
            ApplicationInfo.splitSourceDirs[]  → open each split APK too
                └── for each ZIP entry:
                        extension in MODEL_EXTENSIONS?
                            → read first 8 bytes
                            → verify magic bytes (TFL3 / GGUF / PK …)
                            → DetectedModel
                        .bin matching MODEL_BIN_PATTERNS + size > 500 KB?
                            → DetectedModel
                        filename in ML_RUNTIME_LIBRARIES?
                            → DetectedRuntime

PackageManager.getPackageInfo() × KNOWN_AI_PACKAGES → KnownAiPackage list

MediaStore query (Downloads + Files) × MODEL_EXTENSIONS → DownloadedModel list

Sort apps by total model size descending
Group into: Google · Samsung · Other OEM · Third-Party
```

Corrupted or DRM-protected APKs throw on `ZipFile` open — these are caught and skipped silently.

---

## Detected model formats

| Framework | Extensions | Magic bytes verified |
|-----------|-----------|---------------------|
| TensorFlow Lite | `.tflite`, `.lite` | `TFL3` at offset 4 ✓ |
| ONNX | `.onnx` | — |
| PyTorch Mobile | `.pt`, `.pth`, `.ptl`, `.torchscript` | `PK` zip header ✓ |
| ExecuTorch | `.pte` | — |
| GGUF (LLMs) | `.gguf` | `GGUF` at offset 0 ✓ |
| GGML | `.ggml` | `ggml` at offset 0 ✓ |
| MediaPipe | `.model`, `.binarypb` | — |
| Qualcomm SNPE/QNN | `.dlc` | — |
| MNN (Alibaba) | `.mnn` | — |
| NCNN (Tencent) | `.ncnn`, `.param` | — |
| PaddlePaddle | `.paddle`, `.pdmodel`, `.nb` | — |
| MindSpore Lite | `.ms` | — |
| Caffe | `.caffemodel`, `.prototxt` | — |
| CoreML | `.mlmodel`, `.mlpackage` | — |
| Hailo | `.hef` | — |
| Generic weights | `.bin` (pattern + size matched) | — |

---

## Permissions

| Permission | Why |
|-----------|-----|
| `QUERY_ALL_PACKAGES` | Enumerate all installed apps |
| `READ_EXTERNAL_STORAGE` | Scan downloaded model files (API 26–32 only; not needed on 33+) |

No internet permission, no root.

> **Note:** `QUERY_ALL_PACKAGES` triggers Play Store review for apps distributed there. Strip AI targets sideload and F-Droid distribution, so this is not a concern.

---

## Risk level thresholds

| Badge | Condition |
|-------|-----------|
| 🔴 High | > 100 MB of model files |
| 🟠 Medium | > 10 MB of model files |
| 🟡 Low | Any model files < 10 MB |
| 🔵 Runtime only | ML runtime `.so` present, no model files found |
| 🟢 Clean | Nothing detected (not shown in results) |

---

## Known limitations

- **False positives on `.bin` files** — not every `.bin` is an ML model. Strip AI uses size thresholds and filename patterns to reduce noise, but some generic binary assets may appear. A disclaimer is shown in-app.
- **Encrypted APKs** — some apps (notably banking apps) use APK-level encryption. These are skipped.
- **Private downloaded models** — models fetched at runtime into app-private storage (`/data/data/...`) are not visible without root. Strip AI catches models in external/download storage via MediaStore, but not private directories.
- **Dynamic delivery** — Play Feature Delivery modules installed on-demand may not appear if not yet downloaded.

---

## Roadmap

- [x] Export scan report as shareable image
- [x] ADB command generator to disable known AI packages
- [x] Storage monitoring — detect models downloaded after install
- [ ] Community-sourced model signature database
- [ ] F-Droid release

---

## License

MIT
