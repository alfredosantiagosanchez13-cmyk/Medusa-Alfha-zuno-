package com.example.audio

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.AudioTrackInfo
import java.io.File

object AudioMetadataReader {

    fun extractMetadata(context: Context, uri: Uri): AudioTrackInfo {
        val retriever = MediaMetadataRetriever()
        var title = "Audio Desconocido"
        var artist = "Zuno Production"
        var durationMs = 15000L
        var format = "MP4"
        var sampleRate = 44100
        var channels = 2
        var bitrate = 192
        var isFromVideo = false
        var fileSizeFormatted = "164 KB"

        try {
            retriever.setDataSource(context, uri)
            
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (!metaTitle.isNullOrBlank()) title = metaTitle
            
            val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            if (!metaArtist.isNullOrBlank()) artist = metaArtist

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            if (!durationStr.isNullOrBlank()) {
                durationMs = durationStr.toLongOrNull() ?: 15000L
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
                format = "AUDIO"
            }

            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            if (!bitrateStr.isNullOrBlank()) {
                bitrate = (bitrateStr.toIntOrNull() ?: 192000) / 1000
            }

            // Estimate file size
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                val sizeBytes = pfd.statSize
                if (sizeBytes > 0) {
                    val sizeMb = sizeBytes / (1024f * 1024f)
                    fileSizeFormatted = if (sizeMb < 1.0f) "${sizeBytes / 1024} KB" else String.format("%.1f MB", sizeMb)
                }
            }

            // Extract clean title from URI
            val uriPath = uri.lastPathSegment
            if (title == "Audio Desconocido" && !uriPath.isNullOrBlank()) {
                val clean = uriPath.substringAfterLast("/").substringAfterLast(":")
                if (clean.isNotBlank()) title = clean
            }

        } catch (e: Exception) {
            title = uri.lastPathSegment ?: "El Fondo Era el Cimiento.mp4"
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
            isFromVideo = isFromVideo
        )
    }

    /**
     * Pre-configured studio demo tracks for immediate one-click testing.
     * "El Fondo Era el Cimiento.mp4" is the primary target track.
     */
    fun getStudioDemos(): List<AudioTrackInfo> {
        return listOf(
            AudioTrackInfo(
                title = "El Fondo Era el Cimiento.mp4",
                artist = "@alfhaseguridad070",
                durationMs = 298000L,
                format = "MP4 Video (Suno Official Master)",
                sampleRate = 44100,
                channels = 2,
                bitrateKbps = 192,
                fileSizeFormatted = "2.84 MB",
                sourceUriString = "assets://El Fondo Era el Cimiento.mp4",
                isFromVideo = true
            ),
            AudioTrackInfo(
                title = "ZUNO 56300 - Trap Urbano (Raw Mix)",
                artist = "Medusa Alfha Records",
                durationMs = 158000L,
                format = "WAV 24-bit",
                sampleRate = 48000,
                channels = 2,
                bitrateKbps = 1536,
                fileSizeFormatted = "28.8 MB",
                sourceUriString = "demo://zuno_trap_56300",
                isFromVideo = false
            ),
            AudioTrackInfo(
                title = "Medusa Alfha - Balada Acústica & Voz",
                artist = "Alfha Studio",
                durationMs = 192000L,
                format = "FLAC Lossless",
                sampleRate = 44100,
                channels = 2,
                bitrateKbps = 1411,
                fileSizeFormatted = "34.1 MB",
                sourceUriString = "demo://medusa_vocal_ballad",
                isFromVideo = false
            )
        )
    }
}
