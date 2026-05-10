package com.stripai.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.stripai.app.scanner.AppScanResult
import com.stripai.app.scanner.DownloadedModel
import com.stripai.app.scanner.RiskLevel
import com.stripai.app.scanner.ScanResult
import com.stripai.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    uiState: ScanUiState,
    onScanClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when (uiState) {
            is ScanUiState.Idle -> IdleScreen(onScanClick)
            is ScanUiState.Scanning -> ScanningScreen(uiState)
            is ScanUiState.Complete -> ResultsScreen(uiState.result, uiState.completedAtMs, onResetClick)
            is ScanUiState.Error -> ErrorScreen(uiState.message, onResetClick)
        }
    }
}

@Composable
private fun IdleScreen(onScanClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Surface)
                .border(2.dp, RedAccent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "⚡",
                fontSize = 40.sp,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Strip AI",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Find hidden AI on your device",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onScanClick,
            colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
        ) {
            Text(
                text = "Scan Device",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "No root required · Fully offline",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
    }
}

@Composable
private fun ScanningScreen(state: ScanUiState.Scanning) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            color = RedAccent,
            strokeWidth = 3.dp,
            modifier = Modifier.size(56.dp),
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Scanning…",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )

        Spacer(Modifier.height(12.dp))

        if (state.total > 0) {
            Text(
                text = "${state.progress} / ${state.total} apps",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.progress.toFloat() / state.total },
                color = RedAccent,
                trackColor = Surface,
                modifier = Modifier
                    .width(240.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = state.currentApp,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 280.dp),
        )
    }
}

private enum class AppCategory(val label: String) {
    GOOGLE("Google Services"),
    SAMSUNG("Samsung"),
    OTHER_OEM("Other OEM"),
    THIRD_PARTY("Third-Party Apps"),
    RUNTIME_ONLY("ML Runtimes Only"),
}

private fun AppScanResult.category(runtimeOnly: Boolean): AppCategory {
    val pkg = packageName
    return when {
        pkg.startsWith("com.google") || pkg.startsWith("com.android.google") -> AppCategory.GOOGLE
        pkg.startsWith("com.samsung") -> AppCategory.SAMSUNG
        pkg.startsWith("com.oneplus") || pkg.startsWith("com.oplus") ||
        pkg.startsWith("com.miui") || pkg.startsWith("com.xiaomi") ||
        pkg.startsWith("com.huawei") || pkg.startsWith("com.nearme") -> AppCategory.OTHER_OEM
        runtimeOnly -> AppCategory.RUNTIME_ONLY
        else -> AppCategory.THIRD_PARTY
    }
}

@Composable
private fun ResultsScreen(result: ScanResult, completedAtMs: Long, onRescanClick: () -> Unit) {
    val context = LocalContext.current
    val appsWithModels = result.appsWithModels
    val appsRuntimeOnly = result.appsWithRuntimeOnly

    val grouped: Map<AppCategory, List<AppScanResult>> = buildMap {
        appsWithModels.groupBy { it.category(false) }.forEach { (cat, list) ->
            put(cat, (getOrDefault(cat, emptyList()) + list).sortedByDescending { it.totalModelSizeBytes })
        }
        appsRuntimeOnly.groupBy { it.category(true) }.forEach { (cat, list) ->
            put(cat, (getOrDefault(cat, emptyList()) + list))
        }
    }

    val lastScanned = remember(completedAtMs) {
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        "Last scanned: ${fmt.format(Date(completedAtMs))}"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SummaryCard(result) }

        item { Spacer(Modifier.height(4.dp)) }

        if (result.installedKnownAiPackages.isNotEmpty()) {
            item { SectionHeader("Known AI Services", result.installedKnownAiPackages.size) }
            items(result.installedKnownAiPackages) { pkg ->
                KnownAiPackageCard(pkg.friendlyName, pkg.packageName)
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        if (result.downloadedModels.isNotEmpty()) {
            item { SectionHeader("Downloaded to Device", result.downloadedModels.size) }
            items(result.downloadedModels) { model ->
                DownloadedModelCard(model)
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        if (grouped.isEmpty() && result.installedKnownAiPackages.isEmpty() && result.downloadedModels.isEmpty()) {
            item { CleanDeviceCard() }
        }

        AppCategory.values().forEach { category ->
            val apps = grouped[category] ?: return@forEach
            item {
                CollapsibleSection(title = category.label, count = apps.size, apps = apps)
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            if (result.totalModelCount > 0) {
                Button(
                    onClick = { shareReport(context, result) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Report", color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onRescanClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Scan Again")
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = lastScanned,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }

        item {
            Text(
                text = "Scanned ${result.totalAppsScanned} apps in ${result.scanDurationMs / 1000.0}s · Some .bin files may be false positives",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun CollapsibleSection(title: String, count: Int, apps: List<AppScanResult>) {
    var expanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextMuted,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                apps.forEach { app -> AppResultCard(app) }
            }
        }
    }
}

@Composable
private fun SummaryCard(result: ScanResult) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = "AI Footprint",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                SummaryMetric(
                    value = result.totalModelCount.toString(),
                    label = "Models",
                    color = RedAccent,
                )
                SummaryMetric(
                    value = result.appsWithModels.size.toString(),
                    label = "Apps",
                    color = RedAccent,
                )
                SummaryMetric(
                    value = formatSize(result.totalModelSizeBytes),
                    label = "Total Size",
                    color = RedAccent,
                )
            }
            if (result.totalModelCount > 0) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "These AI models were installed on your device without your explicit consent.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            fontFamily = FontFamily.Default,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceVariant)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun KnownAiPackageCard(friendlyName: String, packageName: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(RiskRuntimeOnly),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = friendlyName,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
            )
        }
        AdbCopyButton(packageName = packageName, context = context)
    }
}

@Composable
private fun AppResultCard(app: AppScanResult) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    val appIcon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    val riskColor = when (app.riskLevel) {
        RiskLevel.HIGH -> RiskHigh
        RiskLevel.MEDIUM -> RiskMedium
        RiskLevel.LOW -> RiskLow
        RiskLevel.RUNTIME_ONLY -> RiskRuntimeOnly
        RiskLevel.NONE -> RiskNone
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .animateContentSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App icon
            if (appIcon != null) {
                androidx.compose.foundation.Image(
                    painter = rememberDrawablePainter(appIcon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariant),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = buildString {
                        if (app.models.isNotEmpty()) append("${app.models.size} model${if (app.models.size > 1) "s" else ""}")
                        if (app.models.isNotEmpty() && app.runtimes.isNotEmpty()) append(" · ")
                        if (app.runtimes.isNotEmpty()) append("${app.runtimes.size} runtime lib${if (app.runtimes.size > 1) "s" else ""}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(riskColor),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = app.riskLevel.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = riskColor,
                    )
                }
                if (app.models.isNotEmpty()) {
                    Text(
                        text = formatSize(app.totalModelSizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
            }

            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TextMuted,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 64.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HorizontalDivider(color = CardBorder, modifier = Modifier.padding(bottom = 4.dp))

                app.models.forEach { model ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = model.path,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = model.type,
                                style = MaterialTheme.typography.labelMedium,
                                color = RedAccent,
                            )
                            Text(
                                text = formatSize(model.sizeBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                            )
                        }
                    }
                }

                if (app.runtimes.isNotEmpty()) {
                    if (app.models.isNotEmpty()) {
                        Text(
                            text = "Runtime Libraries",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    app.runtimes.forEach { rt ->
                        Text(
                            text = rt.libraryName,
                            style = MaterialTheme.typography.bodySmall,
                            color = RiskRuntimeOnly,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                AdbCopyButton(
                    packageName = app.packageName,
                    context = LocalContext.current,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
private fun CleanDeviceCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "✅", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Your device looks clean",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No bundled AI models detected",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(text = "Scan failed", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
        ) {
            Text("Try Again")
        }
    }
}

@Composable
private fun AdbCopyButton(
    packageName: String,
    context: Context,
    modifier: Modifier = Modifier,
) {
    val command = "adb shell pm disable-user --user 0 $packageName"
    TextButton(
        onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("adb command", command))
            Toast.makeText(context, "ADB command copied", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = "Copy ADB disable",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )
    }
}

@Composable
private fun DownloadedModelCard(model: DownloadedModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface)
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(RiskHigh),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.fileName,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = model.absolutePath,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = model.type,
                style = MaterialTheme.typography.labelMedium,
                color = RedAccent,
            )
            Text(
                text = formatSize(model.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
