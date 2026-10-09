package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room para persistir el historial completo de canciones analizadas y procesadas,
 * incluyendo metadatos de archivo (nombre, tamaño, duración, formato, sample rate, canales),
 * marca de tiempo (timestamp) y los parámetros de masterización recomendados por la API de Gemini.
 */
@Entity(tableName = "song_projects")
data class SongProject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val durationFormatted: String,
    val format: String,
    val sampleRate: Int,
    val channels: Int,
    val fileSizeBytes: Long = 0L,
    val originalFilePath: String,
    val polishedFilePath: String? = null,
    val masterFilePath: String? = null,
    val isPolished: Boolean = false,
    val vocalScore: Int = 80,
    val loudnessLufs: Float = -14.0f,
    val diagnosisSummary: String = "",
    val masterProfile: String = "Natural",
    val timestamp: Long = System.currentTimeMillis(),

    // --- Parámetros de Masterización recomendados por la API de Gemini ---
    val geminiPitchCorrection: Float? = null,
    val geminiVocalPresenceDb: Float? = null,
    val geminiVocalVolumeDb: Float? = null,
    val geminiBalanceDb: Float? = null,
    val geminiBassGainDb: Float? = null,
    val geminiMidGainDb: Float? = null,
    val geminiTrebleGainDb: Float? = null,
    val geminiStereoWidth: Float? = null,
    val geminiTargetLufs: Float? = null,
    val geminiDeEsserActive: Boolean? = null,
    val geminiProductorResumen: String? = null
)
