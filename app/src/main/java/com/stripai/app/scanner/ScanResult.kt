package com.stripai.app.scanner

data class DetectedModel(
    val path: String,
    val fileName: String,
    val type: String,
    val sizeBytes: Long,
)

data class DetectedRuntime(
    val libraryName: String,
    val path: String,
)

enum class RiskLevel(val label: String, val emoji: String) {
    HIGH("High", "🔴"),
    MEDIUM("Medium", "🟠"),
    LOW("Low", "🟡"),
    RUNTIME_ONLY("Runtime only", "🔵"),
    NONE("Clean", "🟢"),
}

data class AppScanResult(
    val packageName: String,
    val appName: String,
    val models: List<DetectedModel>,
    val runtimes: List<DetectedRuntime>,
    val scanError: String? = null,
) {
    val totalModelSizeBytes: Long get() = models.sumOf { it.sizeBytes }
    val totalModelSizeMB: Float get() = totalModelSizeBytes / (1024f * 1024f)

    val riskLevel: RiskLevel get() = when {
        totalModelSizeBytes > 100 * 1024 * 1024 -> RiskLevel.HIGH
        totalModelSizeBytes > 10 * 1024 * 1024 -> RiskLevel.MEDIUM
        models.isNotEmpty() -> RiskLevel.LOW
        runtimes.isNotEmpty() -> RiskLevel.RUNTIME_ONLY
        else -> RiskLevel.NONE
    }

    val hasAi: Boolean get() = models.isNotEmpty() || runtimes.isNotEmpty()
}

data class DownloadedModel(
    val fileName: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val type: String,
)

data class KnownAiPackage(
    val packageName: String,
    val friendlyName: String,
    val isInstalled: Boolean,
)

data class ScanResult(
    val apps: List<AppScanResult>,
    val knownAiPackages: List<KnownAiPackage>,
    val downloadedModels: List<DownloadedModel> = emptyList(),
    val totalAppsScanned: Int,
    val scanDurationMs: Long,
) {
    val appsWithModels: List<AppScanResult>
        get() = apps.filter { it.models.isNotEmpty() }
            .sortedByDescending { it.totalModelSizeBytes }

    val appsWithRuntimeOnly: List<AppScanResult>
        get() = apps.filter { it.models.isEmpty() && it.runtimes.isNotEmpty() }

    val totalModelCount: Int get() = apps.sumOf { it.models.size }

    val totalModelSizeBytes: Long get() = apps.sumOf { it.totalModelSizeBytes }

    val totalModelSizeMB: Float get() = totalModelSizeBytes / (1024f * 1024f)

    val installedKnownAiPackages: List<KnownAiPackage>
        get() = knownAiPackages.filter { it.isInstalled }
}
