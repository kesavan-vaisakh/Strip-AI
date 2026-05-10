package com.stripai.app.scanner

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipFile

class ApkScanner(private val context: Context) {

    data class Progress(
        val current: Int,
        val total: Int,
        val currentAppName: String,
    )

    suspend fun scan(onProgress: (Progress) -> Unit): ScanResult = withContext(Dispatchers.IO) {
        val startMs = System.currentTimeMillis()
        val pm = context.packageManager

        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val total = installedApps.size
        val results = mutableListOf<AppScanResult>()

        installedApps.forEachIndexed { index, appInfo ->
            val appName = pm.getApplicationLabel(appInfo).toString()
            onProgress(Progress(index + 1, total, appName))

            val result = scanApp(appInfo, appName)
            if (result.hasAi || result.scanError != null) {
                results.add(result)
            }
        }

        val knownAiPackages = ModelSignature.KNOWN_AI_PACKAGES.map { (pkg, friendlyName) ->
            KnownAiPackage(
                packageName = pkg,
                friendlyName = friendlyName,
                isInstalled = try {
                    pm.getPackageInfo(pkg, 0)
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }
            )
        }

        ScanResult(
            apps = results,
            knownAiPackages = knownAiPackages,
            totalAppsScanned = total,
            scanDurationMs = System.currentTimeMillis() - startMs,
        )
    }

    private fun scanApp(appInfo: ApplicationInfo, appName: String): AppScanResult {
        val models = mutableListOf<DetectedModel>()
        val runtimes = mutableListOf<DetectedRuntime>()
        var scanError: String? = null

        val apkPaths = mutableListOf<String>()
        apkPaths.add(appInfo.sourceDir)
        appInfo.splitSourceDirs?.let { apkPaths.addAll(it) }

        for (apkPath in apkPaths) {
            try {
                ZipFile(apkPath).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.isDirectory) continue

                        val name = entry.name
                        val size = entry.size.coerceAtLeast(0)

                        when {
                            ModelSignature.isModelFile(name, size) -> {
                                val ext = name.lowercase().substringAfterLast('.', "")
                                val header = try {
                                    zip.getInputStream(entry).use { stream ->
                                        val buf = ByteArray(8)
                                        val read = stream.read(buf)
                                        if (read > 0) buf.copyOf(read) else ByteArray(0)
                                    }
                                } catch (_: Exception) { ByteArray(0) }
                                if (!ModelSignature.verifyMagicBytes(ext, header)) continue
                                val fileName = name.substringAfterLast('/')
                                models.add(
                                    DetectedModel(
                                        path = name,
                                        fileName = fileName,
                                        type = ModelSignature.modelType(name),
                                        sizeBytes = size,
                                    )
                                )
                            }
                            ModelSignature.isRuntimeLibrary(name) -> {
                                val fileName = name.substringAfterLast('/')
                                // avoid duplicates across splits
                                if (runtimes.none { it.libraryName == fileName }) {
                                    runtimes.add(DetectedRuntime(libraryName = fileName, path = name))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // corrupted / protected APKs are common — skip silently unless it's the primary APK
                if (apkPath == appInfo.sourceDir && models.isEmpty() && runtimes.isEmpty()) {
                    scanError = e.javaClass.simpleName
                }
            }
        }

        return AppScanResult(
            packageName = appInfo.packageName,
            appName = appName,
            models = models,
            runtimes = runtimes,
            scanError = scanError,
        )
    }
}
