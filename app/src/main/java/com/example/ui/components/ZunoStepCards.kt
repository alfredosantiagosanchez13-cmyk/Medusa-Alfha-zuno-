package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioMetadataReader
import com.example.data.AcousticDiagnosis
import com.example.data.AppliedPolishingStep
import com.example.data.AudioTrackInfo
import com.example.ui.WorkflowStep
import com.example.ui.theme.MetallicGrayDark
import com.example.ui.theme.MetallicGrayLight
import com.example.ui.theme.MetallicGrayMid
import com.example.ui.theme.StudioGreenLed
import com.example.ui.theme.ZunoBlack
import com.example.ui.theme.ZunoBorderMetallic
import com.example.ui.theme.ZunoGold
import com.example.ui.theme.ZunoGoldBright
import com.example.ui.theme.ZunoGoldDark
import com.example.ui.theme.ZunoSurfaceCard
import com.example.ui.theme.ZunoSurfaceDark

@Composable
fun ZunoStepCards(
    currentStep: WorkflowStep,
    trackInfo: AudioTrackInfo?,
    isExtracting: Boolean,
    diagnosis: AcousticDiagnosis?,
    geminiStatusNotice: String?,
    appliedSteps: List<AppliedPolishingStep>,
    onPickFile: () -> Unit,
    onSelectDemo: (AudioTrackInfo) -> Unit,
    onVerifyAndExtract: () -> Unit,
    onAnalyze: () -> Unit,
    onPolish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isExtracted = (currentStep == WorkflowStep.AudioExtracted || currentStep == WorkflowStep.Analyzing || currentStep == WorkflowStep.Analyzed || currentStep == WorkflowStep.Polishing || currentStep == WorkflowStep.Polished)
    val isAnalyzed = (currentStep == WorkflowStep.Analyzed || currentStep == WorkflowStep.Polishing || currentStep == WorkflowStep.Polished)
    val isPolished = (currentStep == WorkflowStep.Polished)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ETAPA 1: CARGA & INGESTIÓN REAL (SAF)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ZunoSurfaceCard)
                .border(
                    width = 1.dp,
                    color = if (isExtracted) StudioGreenLed.copy(alpha = 0.5f) else ZunoBorderMetallic,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StepNumberBadge(
                            stepNumber = "1",
                            isActive = isExtracted,
                            activeColor = StudioGreenLed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ETAPA 1: INGESTIÓN & EXTRACCIÓN REAL",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Storage Access Framework • Sin datos simulados",
                                color = MetallicGrayMid,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Botón SUBIR con SAF
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZunoSurfaceDark)
                            .border(1.dp, ZunoGold, RoundedCornerShape(8.dp))
                            .clickable { onPickFile() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("upload_song_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = ZunoGoldBright,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "SUBIR ARCHIVO",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Selector de demos de estudio ("El Fondo Era el Cimiento.mp4" prioritario)
                Text(
                    text = "O cargar pista de estudio integrada:",
                    color = MetallicGrayMid,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val demos = AudioMetadataReader.getStudioDemos()
                    demos.forEach { demo ->
                        val isSelected = (trackInfo?.title == demo.title)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ZunoGold.copy(alpha = 0.2f) else ZunoSurfaceDark)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) ZunoGold else ZunoBorderMetallic,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelectDemo(demo) }
                                .padding(horizontal = 4.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (demo.title.contains("Cimiento")) "El Cimiento.mp4" else demo.title.substringBefore(" -").take(14),
                                color = if (isSelected) ZunoGoldBright else MetallicGrayLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (isExtracting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZunoSurfaceDark)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ZunoGold,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Copiando archivo a memoria interna y extrayendo audio...",
                            color = ZunoGoldBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ETAPA 2: ANALIZAR CON IA (Habilitado progresivamente solo tras ingestión correcta)
        val isAnalyzing = (currentStep == WorkflowStep.Analyzing)
        val canAnalyze = isExtracted

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ZunoSurfaceCard)
                .border(
                    width = 1.dp,
                    color = if (isAnalyzed) StudioGreenLed.copy(alpha = 0.5f) else if (canAnalyze) ZunoGold.copy(alpha = 0.4f) else ZunoBorderMetallic,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepNumberBadge(
                        stepNumber = "2",
                        isActive = isAnalyzed,
                        activeColor = if (isAnalyzed) StudioGreenLed else ZunoGold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ETAPA 2: DIAGNÓSTICO ACÚSTICO COMPLETO",
                            color = if (canAnalyze) Color.White else MetallicGrayMid,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (canAnalyze) "Cálculo físico de LUFS, True Peak, RMS y Rango Dinámico" else "Bloqueado: Requiere completar Ingestión en Etapa 1",
                            color = MetallicGrayLight,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val analyzeButtonBrush = if (canAnalyze) {
                    Brush.horizontalGradient(listOf(ZunoGold, ZunoGoldBright))
                } else {
                    Brush.horizontalGradient(listOf(MetallicGrayDark, MetallicGrayDark))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(analyzeButtonBrush)
                        .border(
                            1.dp,
                            if (canAnalyze) ZunoGoldBright else ZunoBorderMetallic,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = canAnalyze && !isAnalyzing) { onAnalyze() }
                        .testTag("analyze_song_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isAnalyzing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ZunoBlack,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ANALIZANDO BLOQUES DE AUDIO...",
                                color = ZunoBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (canAnalyze) Icons.Default.AutoAwesome else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (canAnalyze) ZunoBlack else MetallicGrayLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isAnalyzed) "✓ RE-ANALIZAR PISTA FÍSICA" else if (canAnalyze) "✨ ANALIZAR CON IA" else "ANALIZAR (COMPLETAR INGESTIÓN PRIMERO)",
                                color = if (canAnalyze) ZunoBlack else MetallicGrayLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                if (!geminiStatusNotice.isNullOrBlank() && canAnalyze) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(ZunoSurfaceDark)
                            .border(1.dp, ZunoBorderMetallic, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ZunoGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = geminiStatusNotice,
                                color = MetallicGrayLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // ETAPA 3: PRODUCCIÓN Y MASTERIZACIÓN (Habilitado progresivamente solo tras análisis)
        val isPolishing = (currentStep == WorkflowStep.Polishing)
        val canPolish = isAnalyzed

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (isPolished) ZunoGold.copy(alpha = 0.15f) else ZunoSurfaceCard,
                            ZunoSurfaceCard
                        )
                    )
                )
                .border(
                    width = if (isPolished) 2.dp else 1.dp,
                    color = if (isPolished) ZunoGoldBright else if (canPolish) ZunoGold.copy(alpha = 0.4f) else ZunoBorderMetallic,
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepNumberBadge(
                    stepNumber = "3",
                    isActive = isPolished,
                    activeColor = ZunoGold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ETAPA 3: PRODUCCIÓN Y MASTERIZACIÓN",
                        color = if (canPolish) ZunoGoldBright else MetallicGrayMid,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (canPolish) "Auto-Tune, EQ, de-esser y master comercial" else "Inactivo hasta completar Análisis",
                        color = MetallicGrayLight,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (canPolish) Brush.horizontalGradient(listOf(ZunoGoldBright, ZunoGold, ZunoGoldDark))
                        else Brush.horizontalGradient(listOf(MetallicGrayDark, MetallicGrayDark))
                    )
                    .clickable(enabled = canPolish && !isPolishing) { onPolish() }
                    .testTag("polish_master_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isPolishing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = ZunoBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PULIENDO CON MOTOR LOCAL...",
                            color = ZunoBlack,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (canPolish) Icons.Default.AutoAwesome else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (canPolish) ZunoBlack else MetallicGrayLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPolished) "✨ VOLVER A PULIR CON IA" else if (canPolish) "✨ PULIR CANCIÓN CON IA" else "PULIR (COMPLETAR ANÁLISIS PRIMERO)",
                            color = if (canPolish) ZunoBlack else MetallicGrayLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Checklist de pasos aplicados
            AnimatedVisibility(visible = appliedSteps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "MÓDULOS APLICADOS:",
                        color = ZunoGold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    appliedSteps.forEach { step ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(StudioGreenLed.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = StudioGreenLed,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${step.title}: ${step.description}",
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepNumberBadge(
    stepNumber: String,
    isActive: Boolean = false,
    activeColor: Color = ZunoGold
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor else MetallicGrayDark),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stepNumber,
            color = if (isActive) ZunoBlack else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
    }
}
