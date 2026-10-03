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
import androidx.compose.material.icons.filled.PlayCircle
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
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioGreenLed
import com.example.ui.theme.StudioOrange
import com.example.ui.theme.StudioRedClipping
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
        // ETAPA 1: CARGAR & COMPROBAR EXTRACCIÓN (PRIORIDAD 2)
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
                                text = "ETAPA 1: CARGA & EXTRACCIÓN",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "MP4, MP3, WAV • Comprobación 6 puntos",
                                color = MetallicGrayMid,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Upload Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ZunoSurfaceDark)
                            .border(1.dp, ZunoBorderMetallic, RoundedCornerShape(8.dp))
                            .clickable { onPickFile() }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("upload_song_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = ZunoGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SUBIR",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Demos Selection with "El Fondo Era el Cimiento.mp4" first
                Text(
                    text = "Seleccionar canción objetivo:",
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

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Extraction Trigger Button
                val extractButtonBrush = if (isExtracted) {
                    Brush.horizontalGradient(listOf(StudioGreenLed.copy(alpha = 0.2f), StudioGreenLed.copy(alpha = 0.2f)))
                } else {
                    Brush.horizontalGradient(listOf(ZunoGold, ZunoGoldBright))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(extractButtonBrush)
                        .border(
                            1.dp,
                            if (isExtracted) StudioGreenLed else ZunoGoldBright,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = !isExtracting) { onVerifyAndExtract() }
                        .testTag("verify_extract_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isExtracting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = ZunoBlack,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "COMPROBANDO ARCHIVO Y EXTRAYENDO AUDIO...",
                                color = ZunoBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isExtracted) Icons.Default.Check else Icons.Default.SettingsVoice,
                                contentDescription = null,
                                tint = if (isExtracted) StudioGreenLed else ZunoBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isExtracted) "✓ AUDIO EXTRAÍDO Y VERIFICADO (DETENIDO)" else "COMPROBAR Y EXTRAER AUDIO (${trackInfo?.title?.take(22) ?: ""})",
                                color = if (isExtracted) StudioGreenLed else ZunoBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        // ETAPA 2: ANALIZAR CON IA (Habilitado progresivamente)
        val isAnalyzing = (currentStep == WorkflowStep.Analyzing)
        val canAnalyze = isExtracted

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (canAnalyze) ZunoSurfaceCard else ZunoSurfaceCard.copy(alpha = 0.5f))
                .border(
                    width = 1.dp,
                    color = if (isAnalyzed) StudioCyan.copy(alpha = 0.5f) else ZunoBorderMetallic,
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
                            stepNumber = "2",
                            isActive = isAnalyzed,
                            activeColor = StudioCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ETAPA 2: ANALIZAR CON IA",
                                color = if (canAnalyze) Color.White else MetallicGrayMid,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (canAnalyze) "Motor local ZUNO (independiente de Gemini)" else "Requiere completar Etapa 1",
                                color = MetallicGrayMid,
                                fontSize = 10.sp
                            )
                        }
                    }

                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = StudioCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canAnalyze) StudioCyan.copy(alpha = 0.15f) else ZunoSurfaceDark)
                                .border(1.dp, if (canAnalyze) StudioCyan else MetallicGrayDark, RoundedCornerShape(8.dp))
                                .clickable(enabled = canAnalyze) { onAnalyze() }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("analyze_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isAnalyzed) "RE-ANALIZAR" else "ANALIZAR",
                                color = if (canAnalyze) StudioCyan else MetallicGrayDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Gemini Status Notice Banner if present
                if (!geminiStatusNotice.isNullOrBlank()) {
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

        // ETAPA 3: ✨ PULIR CANCIÓN CON IA (Habilitado progresivamente)
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

            // Giant Master CTA Button
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

            // Applied Steps Checklist
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
