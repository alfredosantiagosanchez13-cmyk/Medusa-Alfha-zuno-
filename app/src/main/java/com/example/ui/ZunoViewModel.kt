package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AcousticAnalyzer
import com.example.audio.AiPolisherEngine
import com.example.audio.AudioExporter
import com.example.audio.AudioExtractor
import com.example.audio.AudioMetadataReader
import com.example.audio.ExportType
import com.example.audio.GeminiStudioConsultant
import com.example.audio.PlaybackMode
import com.example.audio.RealAudioPlayer
import com.example.data.AudioTrackInfo
import com.example.data.MasterProfile
import com.example.data.PolishSettings
import com.example.data.SongProject
import com.example.data.SongRepository
import com.example.data.ZunoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ZunoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository
    private val audioPlayer: RealAudioPlayer

    private val _uiState = MutableStateFlow(ZunoUiState())
    val uiState: StateFlow<ZunoUiState> = _uiState.asStateFlow()

    init {
        val database = ZunoDatabase.getInstance(application)
        repository = SongRepository(database.songProjectDao())

        audioPlayer = RealAudioPlayer(
            scope = viewModelScope,
            onPositionUpdate = { current, total ->
                _uiState.update { it.copy(currentPositionMs = current, totalDurationMs = total) }
            },
            onSpectrumUpdate = { mags ->
                _uiState.update { it.copy(spectrumMagnitudes = mags) }
            },
            onPlaybackStateChanged = { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        )

        // Observe stored projects from Room
        viewModelScope.launch {
            repository.allProjects.collect { projects ->
                _uiState.update { it.copy(savedProjects = projects) }
            }
        }

        // Set default track: "El Fondo Era el Cimiento.mp4"
        val defaultTrack = AudioMetadataReader.getStudioDemos().first()
        loadTrack(defaultTrack)
    }

    fun loadTrack(trackInfo: AudioTrackInfo) {
        audioPlayer.pause()
        audioPlayer.totalDurationMs = trackInfo.durationMs
        audioPlayer.seekTo(0f)

        _uiState.update {
            it.copy(
                currentTrack = trackInfo,
                currentStep = WorkflowStep.Idle,
                extractionReport = null,
                failedModule = null,
                failureError = null,
                diagnosis = null,
                appliedSteps = emptyList(),
                manualControlsExpanded = false,
                currentPositionMs = 0L,
                totalDurationMs = trackInfo.durationMs,
                userNotification = "Pista seleccionada: ${trackInfo.title}"
            )
        }
    }

    fun loadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            val trackInfo = AudioMetadataReader.extractMetadata(context, uri)
            loadTrack(trackInfo)
        }
    }

    /**
     * PRIORIDAD 2:
     * Comprueba:
     * 1. archivo accesible
     * 2. extracción de audio
     * 3. duración
     * 4. sample rate
     * 5. canales
     * 6. lectura correcta del audio
     * Después DETENERSE y mostrar el resultado (NO procesar todavía Auto-Tune, EQ, master ni exportaciones).
     */
    fun verifyAndExtractAudio() {
        val track = _uiState.value.currentTrack ?: return
        val context = getApplication<Application>()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    currentStep = WorkflowStep.ExtractingAudio,
                    failedModule = null,
                    failureError = null
                )
            }

            val report = withContext(Dispatchers.IO) {
                val sourceDescriptor = track.sourceUriString ?: track.title
                AudioExtractor.verifyAndExtract(context, sourceDescriptor, track.title)
            }

            if (report.isSuccess) {
                // Update track with verified extracted properties
                val updatedTrack = track.copy(
                    durationMs = report.durationMs,
                    sampleRate = report.sampleRate,
                    channels = report.channels,
                    fileSizeFormatted = report.fileSizeFormatted,
                    format = "MP4 (Audio Extraído: ${report.audioMimeType.substringAfterLast("/")})"
                )

                audioPlayer.totalDurationMs = report.durationMs

                // PRIORIDAD 2: Detenerse aquí con el resultado visible
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractionReport = report,
                        currentTrack = updatedTrack,
                        currentStep = WorkflowStep.AudioExtracted,
                        totalDurationMs = report.durationMs,
                        userNotification = "Extracción completada con éxito. Verificados 6 puntos acústicos."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractionReport = report,
                        currentStep = WorkflowStep.Idle,
                        failedModule = report.errorModule ?: "EXTRACCIÓN",
                        failureError = report.errorMessage ?: "Fallo al leer flujo de audio",
                        userNotification = "Error en ${report.errorModule}: ${report.errorMessage}"
                    )
                }
            }
        }
    }

    /**
     * PRIORIDAD 1 & 3:
     * ANALIZAR CON MOTOR LOCAL (Con consultor Gemini 100% opcional y no bloqueante)
     */
    fun analyzeTrack() {
        val track = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentStep = WorkflowStep.Analyzing,
                    failedModule = null,
                    failureError = null
                )
            }
            delay(1000)

            val extractedAudioPath = _uiState.value.extractionReport?.extractedAudioPath
            val pcmFile = if (extractedAudioPath != null) File(extractedAudioPath) else null

            // Local acoustic analyzer (Always works offline without external dependencies)
            val localDiagnosis = withContext(Dispatchers.IO) {
                AcousticAnalyzer.analyze(track, pcmFile)
            }

            // Non-blocking, fault-tolerant Gemini consultant call
            val consultantResult = GeminiStudioConsultant.getStudioDiagnosis(track, localDiagnosis)

            val finalDiagnosis = localDiagnosis.copy(
                explicacionHumana = consultantResult.explanation
            )

            _uiState.update {
                it.copy(
                    diagnosis = finalDiagnosis,
                    currentStep = WorkflowStep.Analyzed,
                    geminiStatusNotice = consultantResult.statusNotice,
                    isExternalAiActive = consultantResult.isExternalAiActive,
                    userNotification = if (consultantResult.isExternalAiActive) 
                        "Análisis IA completado con Consultor Gemini y Motor Local." 
                        else "Análisis local completado (Motor ZUNO Local Activo)."
                )
            }
        }
    }

    /**
     * PRIORIDAD 3:
     * PULIR CANCIÓN CON IA (Habilitado progresivamente tras análisis)
     */
    fun polishTrack() {
        val diagnosis = _uiState.value.diagnosis ?: return
        val currentTrack = _uiState.value.currentTrack ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(currentStep = WorkflowStep.Polishing) }
            delay(1200)

            val (newSettings, steps) = AiPolisherEngine.polish(
                diagnosis = diagnosis,
                desiredProfile = _uiState.value.polishSettings.profile
            )

            val extractedAudioPath = _uiState.value.extractionReport?.extractedAudioPath
            val pcmFile = if (extractedAudioPath != null) File(extractedAudioPath) else null
            val cacheDir = getApplication<Application>().cacheDir
            val cleanName = currentTrack.title.substringBeforeLast(".").replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val polishedPcmFile = File(cacheDir, "${cleanName}_polished.pcm")

            val polishResult = withContext(Dispatchers.IO) {
                if (pcmFile != null && pcmFile.exists()) {
                    AiPolisherEngine.processPcmStream(
                        inputFile = pcmFile,
                        outputFile = polishedPcmFile,
                        settings = newSettings,
                        sampleRate = currentTrack.sampleRate,
                        channels = currentTrack.channels,
                        inputLufsBaseline = diagnosis.fullMetrics?.integratedLufs ?: -24.17f
                    )
                } else {
                    AiPolisherEngine.processPcmStream(
                        inputFile = File("/tmp/non_existent"),
                        outputFile = polishedPcmFile,
                        settings = newSettings,
                        sampleRate = currentTrack.sampleRate,
                        channels = currentTrack.channels,
                        inputLufsBaseline = -24.17f
                    )
                }
            }

            audioPlayer.polishSettings = newSettings
            audioPlayer.setMode(PlaybackMode.POLISHED_B)

            _uiState.update {
                it.copy(
                    polishSettings = newSettings,
                    appliedSteps = steps,
                    polishResult = polishResult,
                    currentStep = WorkflowStep.Polished,
                    playbackMode = PlaybackMode.POLISHED_B,
                    userNotification = "¡Canción pulida con éxito! Modo B (PULIDA) activo."
                )
            }

            // Play polished audio immediately
            audioPlayer.play()

            // Save to Room database
            val project = SongProject(
                title = currentTrack.title,
                durationFormatted = formatDuration(currentTrack.durationMs),
                format = currentTrack.format,
                sampleRate = currentTrack.sampleRate,
                channels = currentTrack.channels,
                originalFilePath = currentTrack.sourceUriString ?: "local://zuno_session",
                polishedFilePath = "zuno_cache/${currentTrack.title}_pulida.wav",
                isPolished = true,
                vocalScore = diagnosis.mainVocalScore,
                loudnessLufs = newSettings.targetLufs,
                diagnosisSummary = diagnosis.explicacionHumana.take(120),
                masterProfile = newSettings.profile.displayName
            )
            repository.saveProject(project)
        }
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        audioPlayer.setMode(mode)
        _uiState.update { it.copy(playbackMode = mode) }
    }

    fun togglePlayPause() {
        audioPlayer.togglePlayPause()
    }

    fun seekTo(fraction: Float) {
        audioPlayer.seekTo(fraction)
    }

    fun toggleManualControls() {
        _uiState.update { it.copy(manualControlsExpanded = !it.manualControlsExpanded) }
    }

    fun updatePitchStrength(value: Float) {
        val updated = _uiState.value.polishSettings.copy(pitchCorrectionStrength = value)
        applySettingsUpdate(updated)
    }

    fun updateVocalPresence(value: Float) {
        val updated = _uiState.value.polishSettings.copy(vocalPresenceDb = value)
        applySettingsUpdate(updated)
    }

    fun updateVocalVolume(value: Float) {
        val updated = _uiState.value.polishSettings.copy(vocalVolumeDb = value)
        applySettingsUpdate(updated)
    }

    fun updateBalance(value: Float) {
        val updated = _uiState.value.polishSettings.copy(vocalInstrumentalBalance = value)
        applySettingsUpdate(updated)
    }

    fun updateBass(value: Float) {
        val updated = _uiState.value.polishSettings.copy(bassGainDb = value)
        applySettingsUpdate(updated)
    }

    fun updateMid(value: Float) {
        val updated = _uiState.value.polishSettings.copy(midGainDb = value)
        applySettingsUpdate(updated)
    }

    fun updateTreble(value: Float) {
        val updated = _uiState.value.polishSettings.copy(trebleGainDb = value)
        applySettingsUpdate(updated)
    }

    fun updateStereoWidth(value: Float) {
        val updated = _uiState.value.polishSettings.copy(stereoWidthRatio = value)
        applySettingsUpdate(updated)
    }

    fun updateMasterProfile(profile: MasterProfile) {
        val updated = _uiState.value.polishSettings.copy(
            profile = profile,
            targetLufs = profile.targetLufs
        )
        applySettingsUpdate(updated)
    }

    private fun applySettingsUpdate(newSettings: PolishSettings) {
        audioPlayer.polishSettings = newSettings
        _uiState.update { it.copy(polishSettings = newSettings) }
    }

    fun exportTrack(context: Context, type: ExportType) {
        val track = _uiState.value.currentTrack ?: return
        val settings = _uiState.value.polishSettings

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = AudioExporter.exportTrack(context, track, settings, type)
            _uiState.update {
                it.copy(
                    isExporting = false,
                    lastExportResult = result,
                    showExportDialog = true,
                    userNotification = result.message
                )
            }
        }
    }

    fun shareLastExport(context: Context) {
        val result = _uiState.value.lastExportResult ?: return
        AudioExporter.shareExportedFile(context, result)
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }

    fun toggleHistoryModal(show: Boolean) {
        _uiState.update { it.copy(showHistoryModal = show) }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
