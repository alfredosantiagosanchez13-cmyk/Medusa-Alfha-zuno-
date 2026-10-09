package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AcousticAnalyzer
import com.example.audio.AiPolisherEngine
import com.example.audio.AudioExporter
import com.example.audio.AudioExtractor
import com.example.audio.AudioMetadataReader
import com.example.audio.AudioRecorderState
import com.example.audio.FileIngestionManager
import com.example.audio.GeminiStudioConsultant
import com.example.audio.IngestionResult
import com.example.audio.PlaybackMode
import com.example.audio.RawAudioRecorder
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
import java.io.File

class ZunoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SongRepository
    private val audioPlayer: RealAudioPlayer
    val audioRecorder: RawAudioRecorder = RawAudioRecorder(application, viewModelScope)

    val recorderState: StateFlow<AudioRecorderState> = audioRecorder.state

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
            onSpectrumUpdate = { magnitudes ->
                _uiState.update { it.copy(spectrumMagnitudes = magnitudes) }
            },
            onPlaybackStateChanged = { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        )

        // Observar proyectos guardados en Room
        viewModelScope.launch {
            repository.allProjects.collect { projects ->
                _uiState.update { it.copy(savedProjects = projects) }
            }
        }
    }

    /**
     * Grabación de voz cruda con el micrófono
     */
    fun startVocalRecording(): Boolean {
        audioPlayer.pause()
        return audioRecorder.startRecording()
    }

    fun stopVocalRecording() {
        val result = audioRecorder.stopRecording()
        if (result is com.example.audio.RecordingResult.Success) {
            _uiState.update {
                it.copy(userNotification = "Toma vocal guardada (${result.sizeBytes / 1024} KB). Lista para procesar.")
            }
        } else if (result is com.example.audio.RecordingResult.Error) {
            _uiState.update { it.copy(userNotification = result.message) }
        }
    }

    fun cancelVocalRecording() {
        audioRecorder.cancelRecording()
    }

    fun ingestRecordedVocal(context: Context, recordedFile: File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    currentStep = WorkflowStep.ExtractingAudio,
                    failedModule = null,
                    failureError = null,
                    userNotification = "Ingestando toma vocal grabada..."
                )
            }

            val result = withContext(Dispatchers.IO) {
                FileIngestionManager.ingestRecordedFile(context, recordedFile)
            }

            when (result) {
                is IngestionResult.Success -> {
                    audioPlayer.setSourceFile(result.localFile, result.trackInfo.durationMs)
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            currentTrack = result.trackInfo,
                            extractionReport = result.report,
                            currentStep = WorkflowStep.AudioExtracted,
                            totalDurationMs = result.trackInfo.durationMs,
                            currentPositionMs = 0L,
                            failedModule = null,
                            failureError = null,
                            diagnosis = null,
                            appliedSteps = emptyList(),
                            polishResult = null,
                            userNotification = "Voz cruda ingesta y verificada: ${result.trackInfo.title}"
                        )
                    }
                }
                is IngestionResult.Failure -> {
                    audioPlayer.pause()
                    audioPlayer.setSourceFile(null, 0L)
                    val failureReport = AudioExtractor.createFailureReport(
                        fileName = result.fileName,
                        sourceUri = result.sourceUri,
                        error = result.errorMessage
                    )
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            currentTrack = null,
                            extractionReport = failureReport,
                            currentStep = WorkflowStep.Idle,
                            totalDurationMs = 0L,
                            currentPositionMs = 0L,
                            failedModule = "INGESTIÓN DE GRABACIÓN",
                            failureError = result.errorMessage,
                            userNotification = result.errorMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * INGESTIÓN REAL CON ANDROID STORAGE ACCESS FRAMEWORK (ORDEN ZUNO 13)
     */
    fun handleFileSelection(context: Context, uri: Uri) {
        viewModelScope.launch {
            audioPlayer.pause()
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    currentStep = WorkflowStep.ExtractingAudio,
                    failedModule = null,
                    failureError = null,
                    userNotification = "Accediendo al archivo mediante Storage Access Framework..."
                )
            }

            val result = withContext(Dispatchers.IO) {
                FileIngestionManager.ingestUri(context, uri)
            }

            when (result) {
                is IngestionResult.Success -> {
                    audioPlayer.setSourceFile(result.localFile, result.trackInfo.durationMs)
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            currentTrack = result.trackInfo,
                            extractionReport = result.report,
                            currentStep = WorkflowStep.AudioExtracted,
                            totalDurationMs = result.trackInfo.durationMs,
                            currentPositionMs = 0L,
                            failedModule = null,
                            failureError = null,
                            diagnosis = null,
                            appliedSteps = emptyList(),
                            polishResult = null,
                            userNotification = "Archivo ingestado y verificado: ${result.trackInfo.title}"
                        )
                    }
                }
                is IngestionResult.Failure -> {
                    audioPlayer.pause()
                    audioPlayer.setSourceFile(null, 0L)
                    val failureReport = AudioExtractor.createFailureReport(
                        fileName = result.fileName,
                        sourceUri = result.sourceUri,
                        error = result.errorMessage
                    )
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            currentTrack = null,
                            extractionReport = failureReport,
                            currentStep = WorkflowStep.Idle,
                            totalDurationMs = 0L,
                            currentPositionMs = 0L,
                            failedModule = "INGESTIÓN REAL",
                            failureError = result.errorMessage,
                            userNotification = result.errorMessage
                        )
                    }
                }
            }
        }
    }

    fun loadFromUri(context: Context, uri: Uri) = handleFileSelection(context, uri)

    /**
     * Selección de pista demo de estudio (ej. "El Fondo Era el Cimiento.mp4")
     */
    fun selectDemoTrack(context: Context, demo: AudioTrackInfo) {
        viewModelScope.launch {
            audioPlayer.pause()
            _uiState.update {
                it.copy(
                    isExtracting = true,
                    currentStep = WorkflowStep.ExtractingAudio,
                    failedModule = null,
                    failureError = null,
                    userNotification = "Cargando e ingestando pista de estudio física..."
                )
            }

            val result = withContext(Dispatchers.IO) {
                if (demo.title.contains("Cimiento", ignoreCase = true) || demo.title.endsWith(".mp4")) {
                    FileIngestionManager.ingestAsset(context, "El Fondo Era el Cimiento.mp4")
                } else {
                    FileIngestionManager.ingestAsset(context, demo.title)
                }
            }

            when (result) {
                is IngestionResult.Success -> {
                    audioPlayer.setSourceFile(result.localFile, result.trackInfo.durationMs)
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            currentTrack = result.trackInfo,
                            extractionReport = result.report,
                            currentStep = WorkflowStep.AudioExtracted,
                            totalDurationMs = result.trackInfo.durationMs,
                            currentPositionMs = 0L,
                            failedModule = null,
                            failureError = null,
                            diagnosis = null,
                            appliedSteps = emptyList(),
                            polishResult = null,
                            userNotification = "Pista de estudio ingesta: ${result.trackInfo.title} (${result.trackInfo.durationMs / 1000}s)"
                        )
                    }
                }
                is IngestionResult.Failure -> {
                    audioPlayer.setSourceFile(null, 0L)
                    val failureReport = AudioExtractor.createFailureReport(
                        fileName = result.fileName,
                        sourceUri = result.sourceUri,
                        error = result.errorMessage
                    )
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            currentTrack = null,
                            extractionReport = failureReport,
                            currentStep = WorkflowStep.Idle,
                            totalDurationMs = 0L,
                            currentPositionMs = 0L,
                            failedModule = "INGESTIÓN DE DEMO",
                            failureError = result.errorMessage,
                            userNotification = result.errorMessage
                        )
                    }
                }
            }
        }
    }

    fun loadTrack(trackInfo: AudioTrackInfo) {
        val context = getApplication<Application>()
        selectDemoTrack(context, trackInfo)
    }

    fun verifyAndExtractAudio() {
        val track = _uiState.value.currentTrack ?: return
        val localPath = track.localFilePath
        if (localPath == null || !File(localPath).exists()) {
            _uiState.update {
                it.copy(
                    failedModule = "EXTRACCIÓN",
                    failureError = "El archivo físico local no está disponible.",
                    userNotification = "NO SE PUDO LEER EL ARCHIVO SELECCIONADO: Copia local no encontrada."
                )
            }
            return
        }

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
                AudioExtractor.verifyAndExtractFromLocalFile(
                    context = context,
                    localFile = File(localPath),
                    sourceUri = track.sourceUriString ?: localPath,
                    originalSizeBytes = track.fileSizeBytes,
                    mimeType = track.format,
                    displayName = track.title
                )
            }
            if (report.isSuccess) {
                audioPlayer.totalDurationMs = report.durationMs
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractionReport = report,
                        currentStep = WorkflowStep.AudioExtracted,
                        totalDurationMs = report.durationMs,
                        userNotification = "Extracción física verificada con éxito."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExtracting = false,
                        extractionReport = report,
                        currentStep = WorkflowStep.Idle,
                        failedModule = "EXTRACCIÓN",
                        failureError = report.errorMessage ?: "Fallo al extraer audio",
                        userNotification = "Error: ${report.errorMessage}"
                    )
                }
            }
        }
    }

    /**
     * ETAPA 2: ANALIZAR CON MOTOR LOCAL (Con consultor Gemini opcional)
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
            delay(800)
            val extractedAudioPath = _uiState.value.extractionReport?.extractedAudioPath
                ?: track.localFilePath
            val pcmFile = if (extractedAudioPath != null) File(extractedAudioPath) else null

            val localDiagnosis = withContext(Dispatchers.IO) {
                AcousticAnalyzer.analyze(track, pcmFile)
            }

            val consultantResult = GeminiStudioConsultant.getStudioDiagnosis(track, localDiagnosis)
            val finalDiagnosis = localDiagnosis.copy(
                explicacionHumana = consultantResult.explanation
            )

            val updatedSettings = if (consultantResult.masteringAdvice != null) {
                GeminiStudioConsultant.applyAdviceToSettings(_uiState.value.polishSettings, consultantResult.masteringAdvice)
            } else {
                _uiState.value.polishSettings
            }

            _uiState.update {
                it.copy(
                    diagnosis = finalDiagnosis,
                    currentStep = WorkflowStep.Analyzed,
                    geminiStatusNotice = consultantResult.statusNotice,
                    isExternalAiActive = consultantResult.isExternalAiActive,
                    geminiAdvice = consultantResult.masteringAdvice,
                    polishSettings = updatedSettings,
                    userNotification = if (consultantResult.isExternalAiActive)
                        "Análisis IA completado con Consultor Gemini y Motor Local."
                    else "Análisis local completado (Motor ZUNO Local Activo)."
                )
            }

            // Persistir inmediatamente en Room el historial del análisis con metadatos y parámetros de Gemini
            val advice = consultantResult.masteringAdvice
            val projectRecord = SongProject(
                title = track.title,
                durationFormatted = formatDuration(track.durationMs),
                format = track.format,
                sampleRate = track.sampleRate,
                channels = track.channels,
                fileSizeBytes = track.fileSizeBytes,
                originalFilePath = track.localFilePath ?: (track.sourceUriString ?: "local://zuno"),
                polishedFilePath = null,
                isPolished = false,
                vocalScore = finalDiagnosis.mainVocalScore,
                loudnessLufs = finalDiagnosis.currentLoudnessLufs,
                diagnosisSummary = finalDiagnosis.explicacionHumana.take(150),
                masterProfile = updatedSettings.profile.displayName,
                timestamp = System.currentTimeMillis(),
                geminiPitchCorrection = advice?.pitchCorrectionStrength,
                geminiVocalPresenceDb = advice?.vocalPresenceDb,
                geminiVocalVolumeDb = advice?.vocalVolumeDb,
                geminiBalanceDb = advice?.vocalInstrumentalBalance,
                geminiBassGainDb = advice?.bassGainDb,
                geminiMidGainDb = advice?.midGainDb,
                geminiTrebleGainDb = advice?.trebleGainDb,
                geminiStereoWidth = advice?.stereoWidthRatio,
                geminiTargetLufs = advice?.targetLufs,
                geminiDeEsserActive = advice?.deEsserActive,
                geminiProductorResumen = advice?.resumenProductor ?: finalDiagnosis.explicacionHumana
            )
            repository.saveProject(projectRecord)
        }
    }

    /**
     * ETAPA 3: PULIR CANCIÓN CON IA
     */
    fun polishTrack() {
        val diagnosis = _uiState.value.diagnosis ?: return
        val currentTrack = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(currentStep = WorkflowStep.Polishing) }
            delay(1000)
            val (newSettings, steps) = AiPolisherEngine.polish(
                diagnosis = diagnosis,
                desiredProfile = _uiState.value.polishSettings.profile
            )

            val extractedAudioPath = _uiState.value.extractionReport?.extractedAudioPath
                ?: currentTrack.localFilePath
            val pcmFile = if (extractedAudioPath != null) File(extractedAudioPath) else null

            val cacheDir = getApplication<Application>().filesDir
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
                        inputFile = File(cacheDir, "non_existent"),
                        outputFile = polishedPcmFile,
                        settings = newSettings,
                        sampleRate = currentTrack.sampleRate,
                        channels = currentTrack.channels,
                        inputLufsBaseline = -24.17f
                    )
                }
            }

            audioPlayer.polishSettings = newSettings
            audioPlayer.setPolishedFile(polishedPcmFile)
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

            audioPlayer.play()

            // Guardar proyecto en Room
            val advice = _uiState.value.geminiAdvice
            val project = SongProject(
                title = currentTrack.title,
                durationFormatted = formatDuration(currentTrack.durationMs),
                format = currentTrack.format,
                sampleRate = currentTrack.sampleRate,
                channels = currentTrack.channels,
                fileSizeBytes = currentTrack.fileSizeBytes,
                originalFilePath = currentTrack.localFilePath ?: (currentTrack.sourceUriString ?: "local://zuno"),
                polishedFilePath = polishedPcmFile.absolutePath,
                isPolished = true,
                vocalScore = diagnosis.mainVocalScore,
                loudnessLufs = newSettings.targetLufs,
                diagnosisSummary = diagnosis.explicacionHumana.take(150),
                masterProfile = newSettings.profile.displayName,
                timestamp = System.currentTimeMillis(),
                geminiPitchCorrection = advice?.pitchCorrectionStrength ?: newSettings.pitchCorrectionStrength,
                geminiVocalPresenceDb = advice?.vocalPresenceDb ?: newSettings.vocalPresenceDb,
                geminiVocalVolumeDb = advice?.vocalVolumeDb ?: newSettings.vocalVolumeDb,
                geminiBalanceDb = advice?.vocalInstrumentalBalance ?: newSettings.vocalInstrumentalBalance,
                geminiBassGainDb = advice?.bassGainDb ?: newSettings.bassGainDb,
                geminiMidGainDb = advice?.midGainDb ?: newSettings.midGainDb,
                geminiTrebleGainDb = advice?.trebleGainDb ?: newSettings.trebleGainDb,
                geminiStereoWidth = advice?.stereoWidthRatio ?: newSettings.stereoWidthRatio,
                geminiTargetLufs = advice?.targetLufs ?: newSettings.targetLufs,
                geminiDeEsserActive = advice?.deEsserActive ?: newSettings.deEsserActive,
                geminiProductorResumen = advice?.resumenProductor ?: diagnosis.explicacionHumana
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

    fun seekTo(progressFraction: Float) {
        audioPlayer.seekTo(progressFraction)
    }

    fun setMasterProfile(profile: MasterProfile) {
        _uiState.update {
            it.copy(polishSettings = it.polishSettings.copy(profile = profile))
        }
    }

    fun updatePolishSettings(settings: PolishSettings) {
        audioPlayer.polishSettings = settings
        _uiState.update { it.copy(polishSettings = settings) }
    }

    fun toggleManualControls() {
        _uiState.update { it.copy(manualControlsExpanded = !it.manualControlsExpanded) }
    }

    fun toggleHistoryModal(show: Boolean) {
        _uiState.update { it.copy(showHistoryModal = show) }
    }

    fun toggleExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun updatePitchStrength(value: Float) = updatePolishSettings(_uiState.value.polishSettings.copy(pitchCorrectionStrength = value))
    fun updateVocalPresence(value: Float) = updatePolishSettings(_uiState.value.polishSettings.copy(vocalPresenceDb = value))
    fun updateVocalVolume(value: Float) = updatePolishSettings(_uiState.value.polishSettings.copy(vocalVolumeDb = value))
    fun updateBalance(value: Float) = updatePolishSettings(_uiState.value.polishSettings.copy(vocalInstrumentalBalance = value))
    fun updateBass(value: Float) = updatePolishSettings(_uiState.value.polishSettings.copy(bassGainDb = value))
    fun updateMid(value: Float) = updatePolishSettings(_uiState.value.polishSettings.copy(midGainDb = value))
    fun updateTreble(value: Float) = updatePolishSettings(_uiState.value.polishSettings.copy(trebleGainDb = value))
    fun updateStereoWidth(value: Float) = updatePolishSettings(_uiState.value.polishSettings.copy(stereoWidthRatio = value))
    fun updateMasterProfile(profile: MasterProfile) = setMasterProfile(profile)

    fun exportTrack(context: Context, type: com.example.audio.ExportType) {
        val currentTrack = _uiState.value.currentTrack ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }
            val result = AudioExporter.exportTrack(context, currentTrack, _uiState.value.polishSettings, type)
            _uiState.update {
                it.copy(
                    isExporting = false,
                    lastExportResult = result,
                    showExportDialog = true,
                    userNotification = if (result.isSuccess) "Exportado: ${result.fileName}" else "Error al exportar"
                )
            }
        }
    }

    fun shareLastExport(context: Context) {
        val res = _uiState.value.lastExportResult ?: return
        AudioExporter.shareExportedFile(context, res)
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        audioRecorder.cancelRecording()
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000L
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }
}
