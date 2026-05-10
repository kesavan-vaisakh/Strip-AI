package com.stripai.app.scanner

object ModelSignature {

    // File extensions that identify ML model files
    val MODEL_EXTENSIONS = setOf(
        "tflite",    // TensorFlow Lite
        "lite",      // TensorFlow Lite (alternate extension)
        "onnx",      // Open Neural Network Exchange
        "pt",        // PyTorch
        "pth",       // PyTorch
        "torchscript",
        "ptl",       // PyTorch Lite (mobile)
        "pte",       // ExecuTorch / PyTorch Edge
        "gguf",      // GGUF — on-device LLMs (Llama, Gemma, Phi, etc.)
        "ggml",      // GGML (predecessor to GGUF)
        "model",     // MediaPipe / generic
        "binarypb",  // MediaPipe serialised protobuf
        "dlc",       // Qualcomm Neural Processing SDK
        "mnn",       // Alibaba MNN
        "ncnn",      // Tencent NCNN
        "tmfile",    // Tengine
        "ms",        // MindSpore Lite
        "nb",        // PaddlePaddle (NeuroPilot)
        "paddle",    // PaddlePaddle
        "pdmodel",   // PaddlePaddle model
        "hef",       // Hailo
        "mlmodel",   // Apple CoreML (unlikely in APKs but possible)
        "mlpackage",
        "caffemodel",
        "prototxt",
        "param",     // NCNN param file
    )

    // Magic byte signatures — checked against the first 8 bytes of the file
    // Key: file extension, Value: pair of (byte offset, expected bytes)
    val MAGIC_BYTES: Map<String, Pair<Int, ByteArray>> = mapOf(
        // "TFL3" at offset 4
        "tflite" to (4 to byteArrayOf(0x54, 0x46, 0x4C, 0x33)),
        "lite"   to (4 to byteArrayOf(0x54, 0x46, 0x4C, 0x33)),
        // "GGUF" at offset 0
        "gguf"   to (0 to byteArrayOf(0x47, 0x47, 0x55, 0x46)),
        "ggml"   to (0 to byteArrayOf(0x67, 0x67, 0x6D, 0x6C)),
        // PK zip header (PyTorch zip-based format) at offset 0
        "pt"     to (0 to byteArrayOf(0x50, 0x4B)),
        "pth"    to (0 to byteArrayOf(0x50, 0x4B)),
        "ptl"    to (0 to byteArrayOf(0x50, 0x4B)),
    )

    // Patterns for .bin files that are likely ML weights (not generic binary data)
    val MODEL_BIN_PATTERNS = listOf(
        Regex("model[_\\-.].*\\.bin$", RegexOption.IGNORE_CASE),
        Regex(".*[_\\-.]model\\.bin$", RegexOption.IGNORE_CASE),
        Regex("weights[_\\-.].*\\.bin$", RegexOption.IGNORE_CASE),
        Regex(".*[_\\-.]weights\\.bin$", RegexOption.IGNORE_CASE),
        Regex(".*\\.weights\\.bin$", RegexOption.IGNORE_CASE),
        Regex("checkpoint[_\\-.].*\\.bin$", RegexOption.IGNORE_CASE),
        Regex("embedding[_\\-.].*\\.bin$", RegexOption.IGNORE_CASE),
        Regex("vocab[_\\-.].*\\.bin$", RegexOption.IGNORE_CASE),
        Regex("tokenizer[_\\-.].*\\.bin$", RegexOption.IGNORE_CASE),
        Regex(".*encoder.*\\.bin$", RegexOption.IGNORE_CASE),
        Regex(".*decoder.*\\.bin$", RegexOption.IGNORE_CASE),
        Regex(".*transformer.*\\.bin$", RegexOption.IGNORE_CASE),
        Regex(".*_net\\.bin$", RegexOption.IGNORE_CASE),
        Regex("graph\\.bin$", RegexOption.IGNORE_CASE),
        Regex(".*\\.graph\\.bin$", RegexOption.IGNORE_CASE),
        // NCNN-style: anything.bin alongside a .param file (checked contextually in scanner)
        Regex(".*\\.ncnn\\.bin$", RegexOption.IGNORE_CASE),
    )

    // Native .so libraries that signal an ML runtime is bundled
    val ML_RUNTIME_LIBRARIES = setOf(
        // TensorFlow / TFLite
        "libtensorflowlite_jni.so",
        "libtensorflowlite.so",
        "libtensorflowlite_gpu_delegate.so",
        "libtensorflowlite_hexagon_delegate.so",
        "libtensorflow_inference.so",
        "libtensorflow.so",
        "libtflite.so",

        // ONNX Runtime
        "libonnxruntime.so",
        "libonnxruntime_jni.so",
        "libonnxruntime4j_jni.so",

        // PyTorch Mobile
        "libpytorch_jni.so",
        "libpytorch_jni_lite.so",
        "libtorch.so",
        "libtorch_cpu.so",

        // MediaPipe
        "libmediapipe_jni.so",
        "libopencv_java4.so",

        // Google ML Kit / Firebase ML
        "libmlkit.so",
        "libmlkit_jni.so",
        "libfirebase_ml_sdk.so",

        // Qualcomm SNPE / QNN
        "libSNPE.so",
        "libQnnHtp.so",
        "libQnnSystem.so",
        "libQnnCpu.so",
        "libQnnGpu.so",

        // Samsung ONE-RT / NNC
        "libnncrt.so",
        "libone-rt.so",
        "libEden_runtime.so",

        // MediaTek NeuroPilot
        "libneuralnetworks.so",
        "libneuropilot.so",
        "libmtk_neuropilot_client.so",

        // MNN (Alibaba)
        "libMNN.so",
        "libMNN_Express.so",
        "libMNN_CL.so",

        // NCNN (Tencent)
        "libncnn.so",

        // PaddlePaddle Lite
        "libpaddle_light_api_shared.so",
        "libpaddle_full_api_shared.so",

        // OpenCV (common in CV/ML pipelines)
        "libopencv_core.so",
        "libopencv_dnn.so",

        // Arm Compute Library / Arm NN
        "libarmnn.so",
        "libarm_compute.so",
        "libarmnn_support_library.so",

        // MindSpore Lite
        "libmindspore-lite.so",
        "libmindspore-lite-jni.so",

        // Mace (Xiaomi)
        "libmace.so",
        "libmace_jni.so",

        // Android NNAPI wrapper (app-bundled, not the OS one)
        "libneuralnetworks_wrapper.so",

        // Gemini Nano / on-device LLM runners
        "libgemini_nano_jni.so",
        "libliteruntime.so",
        "libliteruntime_jni.so",
    )

    // Path fragments that strongly suggest a model directory
    val MODEL_PATH_HINTS = listOf(
        "models/",
        "model/",
        "ml_models/",
        "mlmodels/",
        "assets/models",
        "assets/ml",
        "assets/tflite",
        "assets/onnx",
    )

    // Known OEM / platform AI packages — name → friendly description
    val KNOWN_AI_PACKAGES: Map<String, String> = mapOf(
        // Google
        "com.google.android.aicore" to "Google AI Core (Gemini Nano host)",
        "com.google.android.gms.tflite" to "Google Play Services — TFLite",
        "com.google.mlkit" to "Google ML Kit",
        "com.google.android.tts" to "Google Text-to-Speech (bundled models)",
        "com.google.android.GoogleCamera" to "Google Camera (Scene ML, Night Sight)",
        "com.google.android.apps.wellbeing" to "Digital Wellbeing (Focus ML)",
        "com.google.android.gms" to "Google Play Services (multiple ML features)",
        "com.google.android.googlequicksearchbox" to "Google Search / Assistant",
        "com.google.android.apps.photos" to "Google Photos (on-device photo ML)",
        "com.google.android.inputmethod.latin" to "Gboard (next-word prediction)",
        "com.google.android.projection.gearhead" to "Android Auto (voice ML)",
        "com.google.android.apps.translate" to "Google Translate (on-device models)",
        "com.google.android.apps.recorder" to "Google Recorder (speech-to-text)",
        "com.google.android.as" to "Android System Intelligence",
        "com.google.android.as.oss" to "Android System Intelligence (OSS)",

        // Samsung
        "com.samsung.android.intelligenceservice" to "Samsung Intelligence Service",
        "com.samsung.android.aicoreservice" to "Samsung AI Core Service",
        "com.samsung.android.visionintelligence" to "Samsung Vision Intelligence",
        "com.samsung.android.bixby.agent" to "Bixby Assistant",
        "com.samsung.android.bixby.service" to "Bixby Service",
        "com.samsung.android.bixbyvision.framework" to "Bixby Vision (object/scene recognition)",
        "com.samsung.android.bixby.wakeup" to "Bixby Wake-up",
        "com.samsung.android.camera.app" to "Samsung Camera (AI scene, portrait)",
        "com.samsung.android.galaxyai" to "Samsung Galaxy AI",
        "com.samsung.android.smartsuggestions" to "Samsung Smart Suggestions",
        "com.samsung.android.app.translator" to "Samsung Translator (on-device)",
        "com.samsung.android.svoiceime" to "Samsung Voice Input",
        "com.samsung.android.livedrawing" to "Samsung Live Drawing (AI)",

        // OnePlus / OPPO
        "com.oneplus.aieffect" to "OnePlus AI Effects",
        "com.oplus.aiunit" to "OPPO AI Unit",
        "com.oplus.speechassist" to "OPPO Speech Assist",
        "com.nearme.intelligence.service" to "OPPO/Realme Intelligence Service",

        // Xiaomi
        "com.miui.voiceassist" to "MIUI Voice Assistant",
        "com.xiaomi.aiasst.service" to "Xiaomi AI Assistant Service",
        "com.xiaomi.mlservice" to "Xiaomi ML Service",
        "com.miui.aod.aieffect" to "MIUI AOD AI Effect",

        // Huawei
        "com.huawei.intelligent" to "Huawei AI Engine",
        "com.huawei.iaware" to "Huawei iAware",
        "com.huawei.hiai" to "Huawei HiAI",

        // Meta
        "com.facebook.orca" to "Messenger (on-device ML features)",
        "com.instagram.android" to "Instagram (on-device effects ML)",

        // Snapchat
        "com.snapchat.android" to "Snapchat (Lenses AR/ML)",

        // Microsoft
        "com.microsoft.launcher" to "Microsoft Launcher (AI features)",
        "com.microsoft.swiftkey" to "SwiftKey (neural prediction)",

        // Qualcomm
        "com.qti.qcc.tfliteruntime" to "Qualcomm TFLite Runtime",
        "com.qti.snapdragon.aisdk" to "Snapdragon AI SDK",
    )

    // Returns true if the header bytes match the expected magic for this extension.
    // Returns true when no magic is defined (extension alone is sufficient evidence).
    fun verifyMagicBytes(ext: String, header: ByteArray): Boolean {
        val (offset, expected) = MAGIC_BYTES[ext] ?: return true
        if (header.size < offset + expected.size) return true // can't verify, give benefit of the doubt
        return expected.indices.all { i -> header[offset + i] == expected[i] }
    }

    fun isModelFile(entryName: String, sizeBytes: Long): Boolean {
        val lower = entryName.lowercase()
        val ext = lower.substringAfterLast('.', "")

        if (ext in MODEL_EXTENSIONS) return true

        if (ext == "bin" && sizeBytes > 500_000) {
            val fileName = lower.substringAfterLast('/')
            if (MODEL_BIN_PATTERNS.any { it.containsMatchIn(fileName) }) return true
        }

        return false
    }

    fun isRuntimeLibrary(entryName: String): Boolean {
        val fileName = entryName.substringAfterLast('/')
        return fileName in ML_RUNTIME_LIBRARIES
    }

    fun modelType(entryName: String): String {
        return when (entryName.lowercase().substringAfterLast('.', "")) {
            "tflite", "lite" -> "TFLite"
            "onnx" -> "ONNX"
            "pt", "pth", "ptl", "torchscript" -> "PyTorch"
            "pte" -> "ExecuTorch"
            "gguf" -> "GGUF (LLM)"
            "ggml" -> "GGML (LLM)"
            "model", "binarypb" -> "MediaPipe"
            "dlc" -> "Qualcomm DLC"
            "mnn" -> "MNN"
            "ncnn" -> "NCNN"
            "param" -> "NCNN param"
            "tmfile" -> "Tengine"
            "ms" -> "MindSpore"
            "nb" -> "PaddlePaddle NB"
            "paddle", "pdmodel" -> "PaddlePaddle"
            "caffemodel", "prototxt" -> "Caffe"
            "mlmodel", "mlpackage" -> "CoreML"
            "bin" -> "Weights (.bin)"
            "hef" -> "Hailo"
            else -> "Model"
        }
    }
}
