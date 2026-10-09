package com.example.audio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.data.AudioTrackInfo
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

sealed interface IngestionResult {
    data class Success(
        val report: AudioExtractionReport,
        val trackInfo: AudioTrackInfo,
        val localFile: File
    ) : IngestionResult

    data class Failure(
        val errorMessage: String,
        val fileName: String,
        val sourceUri: String
    ) : IngestionResult
}

object FileIngestionManager {
    private const val TAG = "FileIngestionManager"

    /**
     * Ingestión de archivo mediante Android Storage Access Framework (SAF).
     * 1. Toma permiso persistible si el content provider lo soporta.
     * 2. Consulta nombre real (OpenableColumns.DISPLAY_NAME) y tamaño (OpenableColumns.SIZE).
     * 3. Abre InputStream mediante ContentResolver.openInputStream.
     * 4. Copia el archivo completo a almacenamiento interno de la app (context.filesDir/imported_media).
     * 5. Verifica físicamente que el archivo copiado tenga tamaño > 0.
     * 6. Utiliza esa copia local como fuente única para metadatos, extracción y procesamiento.
     */
    fun ingestUri(context: Context, uri: Uri): IngestionResult {
        val sourceUriStr = uri.toString()
        Log.i(TAG, "Iniciando ingestión SAF para URI: $sourceUriStr")

        // 1. Conservar temporalmente / persistir el permiso del URI
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            Log.d(TAG, "takePersistableUriPermission no soportado para este proveedor: ${e.message}")
        }

        // 2. Consultar nombre real y tamaño mediante OpenableColumns
        var displayName: String? = null
        var originalSizeBytes: Long = -1L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1 && !cursor.isNull(nameIdx)) {
                        displayName = cursor.getString(nameIdx)
                    }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) {
                        originalSizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al consultar cursor de OpenableColumns: ${e.message}")
        }

        // Si el nombre no vino en OpenableColumns, extraerlo limpiamente
        if (displayName.isNullOrBlank()) {
            val segment = uri.lastPathSegment
            displayName = if (!segment.isNullOrBlank()) {
                val clean = segment.substringAfterLast("/").substringAfterLast(":")
                if (clean.isNotBlank()) clean else "audio_seleccionado.mp4"
            } else {
                "audio_seleccionado.mp4"
            }
        }

        val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"

        // 3. Abrir InputStream real
        val inputStream: InputStream? = try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            Log.e(TAG, "openInputStream falló: ${e.message}", e)
            null
        }

        if (inputStream == null) {
            return IngestionResult.Failure(
                errorMessage = "NO SE PUDO LEER EL ARCHIVO SELECCIONADO (ContentResolver no pudo abrir el InputStream para este URI).",
                fileName = displayName!!,
                sourceUri = sourceUriStr
            )
        }

        // 4. Copiar a almacenamiento privado interno
        val importDir = File(context.filesDir, "imported_media").apply { mkdirs() }
        val ext = if (displayName!!.contains(".")) "." + displayName!!.substringAfterLast(".") else ".mp4"
        val baseClean = displayName!!.substringBeforeLast(".").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val localDestination = File(importDir, "$baseClean$ext")

        var bytesCopied = 0L
        try {
            inputStream.use { input ->
                FileOutputStream(localDestination).use { output ->
                    val buffer = ByteArray(65536)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                    }
                    output.flush()
                }
            }
        } catch (e: Exception) {
            localDestination.delete()
            return IngestionResult.Failure(
                errorMessage = "NO SE PUDO LEER EL ARCHIVO SELECCIONADO (Error durante la copia física: ${e.message})",
                fileName = displayName!!,
                sourceUri = sourceUriStr
            )
        }

        // 5. Verificar físicamente que el archivo copiado tenga tamaño > 0
        if (!localDestination.exists() || localDestination.length() <= 0L || bytesCopied <= 0L) {
            localDestination.delete()
            return IngestionResult.Failure(
                errorMessage = "NO SE PUDO LEER EL ARCHIVO SELECCIONADO (El archivo físico copiado tiene 0 bytes).",
                fileName = displayName!!,
                sourceUri = sourceUriStr
            )
        }

        val physicalCopiedBytes = localDestination.length()
        val originalSizeFinal = if (originalSizeBytes > 0L) originalSizeBytes else physicalCopiedBytes

        Log.i(TAG, "Archivo copiado exitosamente: ${localDestination.name} ($physicalCopiedBytes bytes físicos)")

        // 6. Extraer audio y verificar características usando exclusivamente la copia local
        val report = AudioExtractor.verifyAndExtractFromLocalFile(
            context = context,
            localFile = localDestination,
            sourceUri = sourceUriStr,
            originalSizeBytes = originalSizeFinal,
            mimeType = mimeType,
            displayName = displayName!!
        )

        if (!report.isSuccess) {
            return IngestionResult.Failure(
                errorMessage = report.errorMessage ?: "Fallo al validar extracción de audio.",
                fileName = displayName!!,
                sourceUri = sourceUriStr
            )
        }

        val trackInfo = AudioTrackInfo(
            title = displayName!!,
            artist = "Importación Real",
            durationMs = report.durationMs,
            format = if (report.mimeType.contains("video", ignoreCase = true) || report.mimeType.contains("mp4", ignoreCase = true)) "MP4 (Audio Extraído)" else "Audio",
            sampleRate = report.sampleRate,
            channels = report.channels,
            bitrateKbps = if (report.durationMs > 0) ((physicalCopiedBytes * 8L) / (report.durationMs)).toInt() else 192,
            fileSizeFormatted = report.fileSizeFormatted,
            sourceUriString = sourceUriStr,
            isFromVideo = report.mimeType.contains("video", ignoreCase = true),
            localFilePath = localDestination.absolutePath,
            fileSizeBytes = physicalCopiedBytes
        )

        return IngestionResult.Success(
            report = report,
            trackInfo = trackInfo,
            localFile = localDestination
        )
    }

    /**
     * Ingestión de pistas de estudio locales integradas (ej. "El Fondo Era el Cimiento.mp4")
     * Copia físicamente el asset a almacenamiento interno para que reciba exactamente el mismo
     * tratamiento e inspección binaria que los archivos subidos por el usuario.
     */
    fun ingestAsset(context: Context, assetName: String): IngestionResult {
        val sourceUriStr = "assets://$assetName"
        Log.i(TAG, "Ingestando pista de estudio desde assets: $assetName")

        val importDir = File(context.filesDir, "imported_media").apply { mkdirs() }
        val sanitized = assetName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val localDestination = File(importDir, sanitized)

        var bytesCopied = 0L
        try {
            context.assets.open(assetName).use { input ->
                FileOutputStream(localDestination).use { output ->
                    val buffer = ByteArray(65536)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                    }
                    output.flush()
                }
            }
        } catch (e: Exception) {
            return IngestionResult.Failure(
                errorMessage = "NO SE PUDO LEER EL ARCHIVO SELECCIONADO (Error al abrir asset: ${e.message})",
                fileName = assetName,
                sourceUri = sourceUriStr
            )
        }

        if (!localDestination.exists() || localDestination.length() <= 0L) {
            return IngestionResult.Failure(
                errorMessage = "NO SE PUDO LEER EL ARCHIVO SELECCIONADO (Copia de asset resultó en 0 bytes).",
                fileName = assetName,
                sourceUri = sourceUriStr
            )
        }

        val physicalBytes = localDestination.length()

        val report = AudioExtractor.verifyAndExtractFromLocalFile(
            context = context,
            localFile = localDestination,
            sourceUri = sourceUriStr,
            originalSizeBytes = physicalBytes,
            mimeType = "video/mp4",
            displayName = assetName
        )

        if (!report.isSuccess) {
            return IngestionResult.Failure(
                errorMessage = report.errorMessage ?: "Fallo al validar extracción.",
                fileName = assetName,
                sourceUri = sourceUriStr
            )
        }

        val trackInfo = AudioTrackInfo(
            title = assetName,
            artist = "Suno Official Master",
            durationMs = report.durationMs,
            format = "MP4 Video (Audio Extraído)",
            sampleRate = report.sampleRate,
            channels = report.channels,
            bitrateKbps = ((physicalBytes * 8L) / (report.durationMs)).toInt(),
            fileSizeFormatted = report.fileSizeFormatted,
            sourceUriString = sourceUriStr,
            isFromVideo = true,
            localFilePath = localDestination.absolutePath,
            fileSizeBytes = physicalBytes
        )

        return IngestionResult.Success(
            report = report,
            trackInfo = trackInfo,
            localFile = localDestination
        )
    }


    /**
     * Ingestión directa de archivos grabados en vivo desde el micrófono.
     * Pasa por la misma validación física e integración acústica.
     */
    fun ingestRecordedFile(context: Context, recordedFile: File): IngestionResult {
        if (!recordedFile.exists() || recordedFile.length() <= 44L) {
            return IngestionResult.Failure(
                errorMessage = "NO SE PUDO LEER EL ARCHIVO GRABADO (Archivo de audio vacío o inexistente).",
                fileName = recordedFile.name,
                sourceUri = "recorded://${recordedFile.name}"
            )
        }

        val physicalBytes = recordedFile.length()
        val report = AudioExtractor.verifyAndExtractFromLocalFile(
            context = context,
            localFile = recordedFile,
            sourceUri = "file://${recordedFile.absolutePath}",
            originalSizeBytes = physicalBytes,
            mimeType = "audio/wav",
            displayName = recordedFile.name
        )

        if (!report.isSuccess) {
            return IngestionResult.Failure(
                errorMessage = report.errorMessage ?: "Fallo al validar audio grabado.",
                fileName = recordedFile.name,
                sourceUri = "file://${recordedFile.absolutePath}"
            )
        }

        val trackInfo = AudioTrackInfo(
            title = recordedFile.name,
            artist = "Grabación Vocal en Vivo",
            durationMs = report.durationMs,
            format = "WAV 16-bit (Voz Cruda)",
            sampleRate = report.sampleRate,
            channels = report.channels,
            bitrateKbps = if (report.durationMs > 0) ((physicalBytes * 8L) / report.durationMs).toInt() else 1411,
            fileSizeFormatted = report.fileSizeFormatted,
            sourceUriString = "file://${recordedFile.absolutePath}",
            isFromVideo = false,
            localFilePath = recordedFile.absolutePath,
            fileSizeBytes = physicalBytes
        )

        return IngestionResult.Success(
            report = report,
            trackInfo = trackInfo,
            localFile = recordedFile
        )
    }

}
