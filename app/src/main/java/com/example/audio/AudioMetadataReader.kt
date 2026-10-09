package com.example.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.AudioTrackInfo

object AudioMetadataReader {

    fun extractMetadata(context: Context, uri: Uri): AudioTrackInfo {
        val retriever = MediaMetadataRetriever()
        var title = "Audio Desconocido"
        var artist = "Zuno Production"
        var durationMs = 0L
        var format = "Desconocido"
        var sampleRate = 0
        var channels = 0
        var bitrate = 0
        var isFromVideo = false
        var fileSizeFormatted = "0 B"
        var originalSizeBytes = 0L

        // Consultar nombre y tamaño reales mediante OpenableColumns
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx != -1) cursor.getString(nameIdx)?.let { title = it }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) {
                        originalSizeBytes = cursor.getLong(sizeIdx)
                        val mb = originalSizeBytes / (1024f * 1024f)
                        fileSizeFormatted = if (mb >= 1.0f) String.format("%.2f MB", mb) else "${originalSizeBytes / 1024} KB"
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            retriever.setDataSource(context, uri)
            
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (!metaTitle.isNullOrBlank()) title = metaTitle
            
            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            if (!metaArtist.isNullOrBlank()) artist = metaArtist

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (!durationStr.isNullOrBlank()) {
                durationMs = durationStr.toLongOrNull() ?: 0L
            }

            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
            if (mime.contains("video", ignoreCase = true) || mime.contains("mp4", ignoreCase = true)) {
                isFromVideo = true
                format = "MP4 (Audio Extraído)"
            } else if (mime.contains("mpeg") || mime.contains("mp3")) {
                format = "MP3"
            } else if (mime.contains("wav")) {
                format = "WAV"
            } else if (mime.contains("flac")) {
                format = "FLAC"
            } else if (mime.contains("m4a") || mime.contains("mp4a")) {
                format = "M4A"
            } else {
                format = if (mime.isNotBlank()) mime else "AUDIO"
            }

            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (!bitrateStr.isNullOrBlank()) {
                bitrate = (bitrateStr.toIntOrNull() ?: 0) / 1000
            }
        } catch (_: Exception) {
            if (title == "Audio Desconocido") {
                val clean = uri.lastPathSegment?.substringAfterLast("/")?.substringAfterLast(":")
                if (!clean.isNullOrBlank()) title = clean
            }
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        return AudioTrackInfo(
            title = title,
            artist = artist,
            durationMs = durationMs,
            format = format,
            sampleRate = sampleRate,
            channels = channels,
            bitrateKbps = bitrate,
            fileSizeFormatted = fileSizeFormatted,
            sourceUriString = uri.toString(),
            isFromVideo = isFromVideo,
            fileSizeBytes = originalSizeBytes
        )
    }

    /**
     * Pistas de estudio para comprobación inmediata.
     * "El Fondo Era el Cimiento.mp4" es la pista principal requerida.
     */
    fun getStudioDemos(): List<AudioTrackInfo> {
        return listOf(
            AudioTrackInfo(
                title = "El Fondo Era el Cimiento.mp4",
                artist = "@alfhaseguridad070",
                durationMs = 298000L,
                format = "MP4 Video (Audio Extraído)",
                sampleRate = 44100,
                channels = 2,
                bitrateKbps = 192,
                fileSizeFormatted = "2.57 MB",
                sourceUriString = "assets://El Fondo Era el Cimiento.mp4",
                isFromVideo = true,
                fileSizeBytes = 2700246L
            )
        )
    }
}
