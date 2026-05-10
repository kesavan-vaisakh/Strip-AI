package com.stripai.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.stripai.app.scanner.ScanResult
import java.io.File

fun shareReport(context: Context, result: ScanResult) {
    val bitmap = buildShareBitmap(result)
    val file = File(context.cacheDir, "strip_ai_report.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bitmap.recycle()

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, "Found ${result.totalModelCount} hidden AI models across ${result.appsWithModels.size} apps (${formatShareSize(result.totalModelSizeBytes)}) — scanned with Strip AI")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share AI Report"))
}

private fun buildShareBitmap(result: ScanResult): Bitmap {
    val W = 1080
    val H = 1350
    val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val pad = 72f

    // Background
    canvas.drawColor(Color.parseColor("#0D0D0D"))

    val p = Paint(Paint.ANTI_ALIAS_FLAG)

    // — Header bar —
    p.color = Color.parseColor("#1A1A1A")
    canvas.drawRect(0f, 0f, W.toFloat(), 140f, p)

    p.color = Color.parseColor("#FF3B30")
    p.textSize = 52f
    p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    p.textAlign = Paint.Align.LEFT
    canvas.drawText("Strip AI", pad, 88f, p)

    p.color = Color.parseColor("#555555")
    p.textSize = 30f
    p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    p.textAlign = Paint.Align.RIGHT
    canvas.drawText("AI Footprint Report", W - pad, 88f, p)

    // — Metrics section —
    val metricsTop = 200f
    val colW = (W - pad * 2) / 3f

    data class Metric(val value: String, val label: String)
    val metrics = listOf(
        Metric(result.totalModelCount.toString(), "MODELS"),
        Metric(result.appsWithModels.size.toString(), "APPS"),
        Metric(formatShareSize(result.totalModelSizeBytes), "TOTAL SIZE"),
    )

    metrics.forEachIndexed { i, m ->
        val cx = pad + i * colW + colW / 2f

        p.color = Color.parseColor("#FF3B30")
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textAlign = Paint.Align.CENTER
        p.textSize = if (m.value.length > 5) 80f else 100f
        canvas.drawText(m.value, cx, metricsTop + 90f, p)

        p.color = Color.parseColor("#888888")
        p.textSize = 28f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(m.label, cx, metricsTop + 130f, p)
    }

    // — Tagline —
    p.color = Color.parseColor("#CCCCCC")
    p.textSize = 34f
    p.textAlign = Paint.Align.CENTER
    p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText(
        "Installed without your explicit consent.",
        W / 2f, metricsTop + 200f, p
    )

    // — Divider —
    val divY = metricsTop + 240f
    p.color = Color.parseColor("#2E2E2E")
    p.strokeWidth = 1.5f
    canvas.drawLine(pad, divY, W - pad, divY, p)

    // — Top offenders —
    p.color = Color.parseColor("#555555")
    p.textSize = 26f
    p.textAlign = Paint.Align.LEFT
    p.letterSpacing = 0.12f
    canvas.drawText("TOP OFFENDERS", pad, divY + 52f, p)
    p.letterSpacing = 0f

    val topApps = result.appsWithModels.take(5)
    topApps.forEachIndexed { i, app ->
        val rowY = divY + 110f + i * 130f

        // Row background
        p.color = Color.parseColor("#1A1A1A")
        val rect = RectF(pad, rowY - 70f, W - pad, rowY + 48f)
        canvas.drawRoundRect(rect, 16f, 16f, p)

        // Risk dot
        p.color = when {
            app.totalModelSizeBytes > 100 * 1024 * 1024 -> Color.parseColor("#FF3B30")
            app.totalModelSizeBytes > 10 * 1024 * 1024 -> Color.parseColor("#FF9500")
            else -> Color.parseColor("#FFD60A")
        }
        canvas.drawCircle(pad + 32f, rowY - 12f, 10f, p)

        // App name
        p.color = Color.parseColor("#EEEEEE")
        p.textSize = 38f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        p.textAlign = Paint.Align.LEFT
        val name = if (app.appName.length > 22) app.appName.take(20) + "…" else app.appName
        canvas.drawText(name, pad + 60f, rowY - 2f, p)

        // Model count
        p.color = Color.parseColor("#888888")
        p.textSize = 28f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("${app.models.size} model${if (app.models.size > 1) "s" else ""}", pad + 60f, rowY + 38f, p)

        // Size
        p.color = Color.parseColor("#FF3B30")
        p.textAlign = Paint.Align.RIGHT
        p.textSize = 34f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatShareSize(app.totalModelSizeBytes), W - pad - 24f, rowY - 2f, p)
    }

    // — Footer —
    val footerY = H - 80f
    p.color = Color.parseColor("#2E2E2E")
    p.strokeWidth = 1f
    canvas.drawLine(pad, footerY - 40f, W - pad, footerY - 40f, p)

    p.color = Color.parseColor("#444444")
    p.textSize = 28f
    p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    p.textAlign = Paint.Align.LEFT
    canvas.drawText("Scanned ${result.totalAppsScanned} apps · No root required · Fully offline", pad, footerY, p)

    return bitmap
}

private fun formatShareSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
