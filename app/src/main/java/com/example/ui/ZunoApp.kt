package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ZunoAbPlayerControl
import com.example.ui.components.ZunoAudioRecorderCard
import com.example.ui.components.ZunoCoverHero
import com.example.ui.components.ZunoDiagnosisCard
import com.example.ui.components.ZunoExportModal
import com.example.ui.components.ZunoExtractionReportCard
import com.example.ui.components.ZunoHistoryModal
import com.example.ui.components.ZunoManualControls
import com.example.ui.components.ZunoStepCards
import com.example.ui.components.ZunoTopBar
import com.example.ui.components.ZunoWaveformVisualizer
import com.example.ui.theme.StudioRedClipping
import com.example.ui.theme.ZunoBlack

@Composable
fun ZunoApp(
    viewModel: ZunoViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recorderState by viewModel.recorderState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.handleFileSelection(context, uri)
        }
    }

    LaunchedEffect(uiState.userNotification) {
        val message = uiState.userNotification
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ZunoBlack,
        topBar = {
            ZunoTopBar(
                onOpenHistory = { viewModel.toggleHistoryModal(true) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ZunoBlack),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error Alert Box if any module reports a failure
                if (uiState.failedModule != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioRedClipping.copy(alpha = 0.15f))
                                .border(1.dp, StudioRedClipping, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = StudioRedClipping,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "FALLO EN MÓDULO: ${uiState.failedModule}",
                                        color = StudioRedClipping,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = uiState.failureError ?: "Error no especificado.",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. ZUNO Cover Artwork & Metadata Badge
                item {
                    ZunoCoverHero(
                        trackInfo = uiState.currentTrack,
                        isPlaying = uiState.isPlaying,
                        hasClipping = uiState.diagnosis?.hasClipping ?: false
                    )
                }

                // 2. Waveform Visualizer with Live Spectrum
                item {
                    ZunoWaveformVisualizer(
                        currentMs = uiState.currentPositionMs,
                        totalMs = uiState.totalDurationMs,
                        spectrumMagnitudes = uiState.spectrumMagnitudes,
                        playbackMode = uiState.playbackMode,
                        isPlaying = uiState.isPlaying,
                        onSeek = { fraction -> viewModel.seekTo(fraction) }
                    )
                }

                // Captura Vocal en Vivo con Micrófono (AudioRecorder)
                item {
                    ZunoAudioRecorderCard(
                        recorderState = recorderState,
                        onStartRecording = { viewModel.startVocalRecording() },
                        onStopRecording = { viewModel.stopVocalRecording() },
                        onCancelRecording = { viewModel.cancelVocalRecording() },
                        onUseRecordedFile = { file -> viewModel.ingestRecordedVocal(context, file) }
                    )
                }

                // 3. Staged Production Cards (Stage 1 Extracción -> Stage 2 Análisis -> Stage 3 Pulido)
                item {
                    ZunoStepCards(
                        currentStep = uiState.currentStep,
                        trackInfo = uiState.currentTrack,
                        isExtracting = uiState.isExtracting,
                        diagnosis = uiState.diagnosis,
                        geminiStatusNotice = uiState.geminiStatusNotice,
                        appliedSteps = uiState.appliedSteps,
                        onPickFile = { filePickerLauncher.launch(arrayOf("video/*", "audio/*", "*/*")) },
                        onSelectDemo = { demo -> viewModel.selectDemoTrack(context, demo) },
                        onVerifyAndExtract = { viewModel.verifyAndExtractAudio() },
                        onAnalyze = { viewModel.analyzeTrack() },
                        onPolish = { viewModel.polishTrack() }
                    )
                }

                // PRIORIDAD 2: Resultado completo de Extracción & Verificación (6 puntos)
                if (uiState.extractionReport != null) {
                    item {
                        ZunoExtractionReportCard(report = uiState.extractionReport!!)
                    }
                }

                // ORDEN ZUNO 02: Informe de Análisis Acústico Completo (Físico por bloques)
                if (uiState.diagnosis?.fullMetrics != null) {
                    item {
                        com.example.ui.components.ZunoFullAnalysisReportCard(metrics = uiState.diagnosis!!.fullMetrics!!)
                    }
                }

                // 4. Dual A / B Comparison Switch (Habilitado tras pulido o para monitoreo)
                item {
                    ZunoAbPlayerControl(
                        playbackMode = uiState.playbackMode,
                        isPlaying = uiState.isPlaying,
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onSelectMode = { mode -> viewModel.setPlaybackMode(mode) }
                    )
                }

                // 5. AI Diagnosis Report (Estado de la canción)
                if (uiState.diagnosis != null) {
                    item {
                        ZunoDiagnosisCard(diagnosis = uiState.diagnosis!!)
                    }
                }

                // ETAPA PULIR: Informe de Masterización y Pulido completado
                if (uiState.polishResult != null) {
                    item {
                        com.example.ui.components.ZunoPolishReportCard(
                            result = uiState.polishResult!!,
                            steps = uiState.appliedSteps
                        )
                    }
                }

                // 6. Manual Controls (Voz, Mezcla, Master - tras pulido)
                if (uiState.currentStep == WorkflowStep.Polished || uiState.currentStep == WorkflowStep.Analyzed) {
                    item {
                        ZunoManualControls(
                            isExpanded = uiState.manualControlsExpanded,
                            settings = uiState.polishSettings,
                            onToggleExpand = { viewModel.toggleManualControls() },
                            onUpdatePitch = { viewModel.updatePitchStrength(it) },
                            onUpdatePresence = { viewModel.updateVocalPresence(it) },
                            onUpdateVolume = { viewModel.updateVocalVolume(it) },
                            onUpdateBalance = { viewModel.updateBalance(it) },
                            onUpdateBass = { viewModel.updateBass(it) },
                            onUpdateMid = { viewModel.updateMid(it) },
                            onUpdateTreble = { viewModel.updateTreble(it) },
                            onUpdateStereoWidth = { viewModel.updateStereoWidth(it) },
                            onSelectProfile = { viewModel.updateMasterProfile(it) }
                        )
                    }
                }

                // 7. Master Export Section (MP3, WAV, Video)
                if (uiState.currentStep == WorkflowStep.Polished) {
                    item {
                        ZunoExportModal(
                            isExporting = uiState.isExporting,
                            lastResult = uiState.lastExportResult,
                            onExport = { type -> viewModel.exportTrack(context, type) },
                            onShare = { viewModel.shareLastExport(context) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (uiState.showHistoryModal) {
                ZunoHistoryModal(
                    projects = uiState.savedProjects,
                    onClose = { viewModel.toggleHistoryModal(false) }
                )
            }
        }
    }
}
