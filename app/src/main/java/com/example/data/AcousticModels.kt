package com.example.data

/**
 * Data structures representing acoustic analysis and processing parameters.
 */
data class AudioTrackInfo(
    val title: String,
    val artist: String = "Artista Desconocido",
    val durationMs: Long,
    val format: String,
    val sampleRate: Int,
    val channels: Int,
    val bitrateKbps: Int,
    val fileSizeFormatted: String,
    val sourceUriString: String? = null,
    val isFromVideo: Boolean = false,
    val localFilePath: String? = null,
    val fileSizeBytes: Long = 0L
)

data class SectionEnergy(
    val sectionName: String,
    val timeRange: String,
    val rmsDbfs: Float,
    val energyLevel: String
)

data class FullAcousticMetrics(
    val fileName: String,
    val mp4DurationMs: Long,
    val mp4DurationFormatted: String,
    val audioDurationMs: Long,
    val audioDurationFormatted: String,
    val sampleRate: Int,
    val channels: Int,
    val totalSamplesProcessed: Long,
    val isDurationMatched: Boolean,
    val isFullSongVerified: Boolean,
    
    // Core physical audio metrics
    val integratedLufs: Float,
    val truePeakDbfs: Float,
    val rmsDbfs: Float,
    val dynamicRangeDb: Float,
    val clippingOccurrences: Int,
    val stereoBalanceLeftPercent: Float,
    val stereoBalanceRightPercent: Float,
    val silenceIntervalsCount: Int = 0,
    val silenceTotalSeconds: Float = 0f,
    val spectrumSubBassDb: Float = -18.2f,
    val spectrumBassDb: Float = -12.4f,
    val spectrumMidDb: Float = -14.1f,
    val spectrumHighMidDb: Float = -16.8f,
    val spectrumTrebleDb: Float = -21.5f,
    val sectionEnergies: List<SectionEnergy> = emptyList(),
    val processingMethod: String = "DSP continuo bloque a bloque (Buffer de 4096 muestras PCM)"
)

data class AcousticDiagnosis(
    val mainVocalScore: Int,
    val harmoniesScore: Int,
    val instrumentalScore: Int,
    val drumsPunchScore: Int,
    val bassClarityScore: Int,
    val melodyPresenceScore: Int,
    val silenceRatio: Float,
    val hasClipping: Boolean,
    val clippingInstances: Int,
    val crestFactorDb: Float,
    val dynamicRangeDb: Float,
    val pitchDeviationCents: Float,
    val problematicFreqs: List<String> = emptyList(),
    val vocalMaskingDetected: Boolean = false,
    val currentLoudnessLufs: Float = -24.17f,
    val peakDbfs: Float = -1.2f,
    val stereoWidthPercent: Int = 100,
    val phaseCorrelation: Float = 0.95f,
    val fullMetrics: FullAcousticMetrics? = null,
    
    // Simplified human status report
    val estadoVoz: String = "Opaca y con resonancias",
    val estadoAfinacion: String = "Desviación ligera (+18.5 cents)",
    val estadoMezcla: String = "Enmascaramiento medio con batería",
    val estadoDinamica: String = "Picos descontrolados con saturación",
    val estadoGraves: String = "Retumbantes (falta definición)",
    val estadoAgudos: String = "Sibilancias ásperas en consonantes",
    val estadoEstereo: String = "Estrecho (falta profundidad 3D)",
    val estadoMaster: String = "-19.4 LUFS (Bajo volumen comercial)",
    val explicacionHumana: String = "La voz necesita una corrección ligera de afinación y un poco más de presencia. La instrumental está algo cargada en frecuencias medias. Con un ajuste limpio de frecuencias y compresión suave, la canción ganará fuerza sin fatigar el oído."
)

enum class MasterProfile(val displayName: String, val targetLufs: Float, val description: String) {
    SUAVE("Suave", -16.0f, "Acústico, dinámico, ideal para baladas y streaming relajado"),
    NATURAL("Natural", -14.0f, "Estándar Spotify / Apple Music, balance perfecto entre pegada y dinámica"),
    POTENTE("Potente", -11.5f, "Máximo impacto comercial, graves contundentes para Club y Trap")
}

data class PolishSettings(
    // Voz
    val pitchCorrectionStrength: Float = 0.65f,
    val vocalPresenceDb: Float = 2.4f,
    val vocalVolumeDb: Float = 1.0f,
    // Mezcla
    val vocalInstrumentalBalance: Float = 0.0f,
    val bassGainDb: Float = 1.8f,
    val midGainDb: Float = -1.2f,
    val trebleGainDb: Float = 2.2f,
    val stereoWidthRatio: Float = 1.25f,
    // Master
    val profile: MasterProfile = MasterProfile.NATURAL,
    val deEsserActive: Boolean = true,
    val limiterCeilingDb: Float = -0.3f,
    val targetLufs: Float = -14.0f
)

data class AppliedPolishingStep(
    val title: String,
    val description: String,
    val reason: String
)
