package com.stripai.app.scanner

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageScanner(private val context: Context) {

    suspend fun scanDownloadedModels(): List<DownloadedModel> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DownloadedModel>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
        )

        // Build a selection that matches any known model extension
        val extList = ModelSignature.MODEL_EXTENSIONS.toList()
        val selection = extList.joinToString(" OR ") {
            "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        }
        val selectionArgs = extList.map { "%.${it}" }.toTypedArray()

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Files.FileColumns.SIZE} DESC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    val path = cursor.getString(dataCol) ?: continue
                    val size = cursor.getLong(sizeCol)
                    if (size < 10_000) continue // skip tiny files — unlikely to be real models

                    results.add(
                        DownloadedModel(
                            fileName = name,
                            absolutePath = path,
                            sizeBytes = size,
                            type = ModelSignature.modelType(name),
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Permission denied or MediaStore unavailable — return empty
        }

        results
    }
}
