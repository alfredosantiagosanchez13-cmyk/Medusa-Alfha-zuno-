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
    val isFromVideo: Boolean = false
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
    val silenceIntervalsCount: Int,
    val silenceTotalSeconds: Float,
    
    // Spectrum & Sections
    val spectrumSubBassDb: Float,
    val spectrumBassDb: Float,
    val spectrumMidDb: Float,
    val spectrumHighMidDb: Float,
    val spectrumTrebleDb: Float,
    val sectionEnergies: List<SectionEnergy>,
    
    // Processing method
    val processingMethod: String = "Procesamiento por bloques continuos (Buffer de 4096 muestras)"
)

data class AcousticDiagnosis(
    // Stem awareness
    val mainVocalScore: Int = 85,          // 0-100
    val harmoniesScore: Int = 60,
    val instrumentalScore: Int = 88,
    val drumsPunchScore: Int = 78,
    val bassClarityScore: Int = 72,
    val melodyPresenceScore: Int = 82,
    
    // Acoustic health
    val silenceRatio: Float = 0.04f,       // e.g. 4%
    val hasClipping: Boolean = true,
    val clippingInstances: Int = 7,
    val crestFactorDb: Float = 9.2f,       // Dynamics (Crest factor)
    val dynamicRangeDb: Float = 11.4f,
    val pitchDeviationCents: Float = 18.5f, // Pitch drift detected
    val problematicFreqs: List<String> = listOf("315 Hz (Resonancia/Caja)", "6.8 kHz (Sibilancia)"),
    val vocalMaskingDetected: Boolean = true,
    val currentLoudnessLufs: Float = -19.4f,
    val peakDbfs: Float = 0.2f,            // Over 0dBFS!
    val stereoWidthPercent: Int = 82,      // 0-200%
    val phaseCorrelation: Float = 0.91f,   // 0.0 to 1.0 (phase health)

    // Detailed Metrics Object
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
    val pitchCorrectionStrength: Float = 0.65f, // 0 = none, 1 = maximum
    val vocalPresenceDb: Float = 2.4f,          // boost in dB
    val vocalVolumeDb: Float = 1.0f,
    
    // Mezcla
    val vocalInstrumentalBalance: Float = 0.0f, // -10 (more inst) to +10 (more vocal)
    val bassGainDb: Float = 1.8f,
    val midGainDb: Float = -1.2f,               // clean mud
    val trebleGainDb: Float = 2.2f,             // silky air
    val stereoWidthRatio: Float = 1.25f,        // 1.0 = normal, 1.3 = wide
    
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
