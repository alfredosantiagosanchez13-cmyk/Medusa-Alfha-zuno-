package com.example.ui

import com.example.audio.AudioExtractionReport
import com.example.audio.ExportResult
import com.example.audio.PlaybackMode
import com.example.audio.PolishProcessingResult
import com.example.data.AcousticDiagnosis
import com.example.data.AppliedPolishingStep
import com.example.data.AudioTrackInfo
import com.example.data.PolishSettings
import com.example.data.SongProject

sealed interface WorkflowStep {
    object Idle : WorkflowStep
    object ExtractingAudio : WorkflowStep
    object AudioExtracted : WorkflowStep   // Detenido aquí en Prioridad 2 con informe completo
    object Analyzing : WorkflowStep
    object Analyzed : WorkflowStep
    object Polishing : WorkflowStep
    object Polished : WorkflowStep
}

data class ZunoUiState(
    val currentStep: WorkflowStep = WorkflowStep.Idle,
    val currentTrack: AudioTrackInfo? = null,
    
    // Extracción y Verificación de Audio Real
    val isExtracting: Boolean = false,
    val extractionReport: AudioExtractionReport? = null,
    val failedModule: String? = null,
    val failureError: String? = null,

    // Diagnóstico Acústico (100% Local con Consultor Gemini opcional)
    val diagnosis: AcousticDiagnosis? = null,
    val geminiStatusNotice: String? = "Consultor IA externo no disponible. Continuando con motor local.",
    val isExternalAiActive: Boolean = false,
    val geminiAdvice: com.example.audio.network.GeminiMasteringAdvice? = null,

    // Pulido con IA
    val appliedSteps: List<AppliedPolishingStep> = emptyList(),
    val polishSettings: PolishSettings = PolishSettings(),
    val polishResult: PolishProcessingResult? = null,
    val manualControlsExpanded: Boolean = false,
    
    // Playback state
    val isPlaying: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.POLISHED_B,
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val spectrumMagnitudes: List<Float> = List(16) { 0.15f },

    // Exporting state
    val isExporting: Boolean = false,
    val lastExportResult: ExportResult? = null,
    val showExportDialog: Boolean = false,

    // Project history
    val savedProjects: List<SongProject> = emptyList(),
    val showHistoryModal: Boolean = false,
    
    // User feedback / snackbar
    val userNotification: String? = null
)
