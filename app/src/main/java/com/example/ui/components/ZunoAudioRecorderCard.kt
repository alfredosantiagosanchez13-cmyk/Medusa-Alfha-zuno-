package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioRecorderState
import com.example.ui.theme.MetallicGrayDark
import com.example.ui.theme.MetallicGrayLight
import com.example.ui.theme.MetallicGrayMid
import com.example.ui.theme.StudioGreenLed
import com.example.ui.theme.StudioRedClipping
import com.example.ui.theme.ZunoBlack
import com.example.ui.theme.ZunoBorderMetallic
import com.example.ui.theme.ZunoGold
import com.example.ui.theme.ZunoGoldBright
import com.example.ui.theme.ZunoGoldDark
import com.example.ui.theme.ZunoSurfaceCard
import com.example.ui.theme.ZunoSurfaceDark
import java.io.File

@Composable
fun ZunoAudioRecorderCard(
    recorderState: AudioRecorderState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onCancelRecording: () -> Unit,
    onUseRecordedFile: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionDeniedNotice by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionDeniedNotice = false
            onStartRecording()
        } else {
            permissionDeniedNotice = true
        }
    }

    val isRecording = recorderState.isRecording
    val recordedFile = recorderState.recordedFile

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZunoSurfaceCard)
            .border(
                width = 1.dp,
                color = if (isRecording) StudioRedClipping else ZunoBorderMetallic,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) StudioRedClipping.copy(alpha = 0.2f) else ZunoGold.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isRecording) StudioRedClipping else ZunoGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CAPTURA VOCAL EN VIVO · MICRÓFONO",
                        color = if (isRecording) StudioRedClipping else ZunoGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Grabación directa 16-bit PCM (44.1 kHz Estéreo) sin compresión",
                        color = MetallicGrayMid,
                        fontSize = 10.sp
                    )
                }
            }

            if (isRecording) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(StudioRedClipping)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "REC",
                        color = StudioRedClipping,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Alerta de permiso denegado
        if (permissionDeniedNotice) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioRedClipping.copy(alpha = 0.15f))
                    .border(1.dp, StudioRedClipping.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Permiso de micrófono requerido para grabar voces. Concede el permiso en la configuración del dispositivo.",
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Panel de Estado de Grabación en Tiempo Real
        if (isRecording) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ZunoSurfaceDark)
                    .border(1.dp, StudioRedClipping.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Timer
                val totalSec = recorderState.durationMs / 1000L
                val tenths = (recorderState.durationMs % 1000L) / 100L
                val min = totalSec / 60
                val sec = totalSec % 60
                val timerFormatted = String.format("%02d:%02d.%d", min, sec, tenths)

                Text(
                    text = timerFormatted,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // VU Meter en tiempo real
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VU:",
                        color = MetallicGrayMid,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { recorderState.currentAmplitude.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (recorderState.currentAmplitude > 0.85f) StudioRedClipping else StudioGreenLed,
                        trackColor = MetallicGrayDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mini Waveform
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val amplitudes = recorderState.waveformAmplitudes
                    for (amp in amplitudes) {
                        val barHeight = (amp * 36f).coerceIn(4f, 36f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (amp > 0.8f) StudioRedClipping else ZunoGold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botón Detener
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.horizontalGradient(listOf(StudioRedClipping, StudioRedClipping)))
                        .clickable { onStopRecording() }
                        .testTag("stop_recording_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DETENER Y GUARDAR TOMA VOCAL",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        } else if (recordedFile != null && recordedFile.exists()) {
            // Panel de Toma Grabada Lista
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ZunoSurfaceDark)
                    .border(1.dp, StudioGreenLed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(StudioGreenLed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = StudioGreenLed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TOMA VOCAL GRABADA CON ÉXITO",
                            color = StudioGreenLed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${recordedFile.length() / 1024} KB",
                        color = MetallicGrayLight,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = recordedFile.name,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón Descartar
                    Box(
                        modifier = Modifier
                            .weight(0.35f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MetallicGrayDark)
                            .border(1.dp, ZunoBorderMetallic, RoundedCornerShape(10.dp))
                            .clickable { onCancelRecording() }
                            .testTag("discard_recording_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MetallicGrayLight,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DESCARTAR",
                                color = MetallicGrayLight,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Botón Usar para pulido
                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(listOf(ZunoGold, ZunoGoldBright)))
                            .clickable { onUseRecordedFile(recordedFile) }
                            .testTag("use_recording_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = ZunoBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "USAR PARA PULIR CON IA",
                                color = ZunoBlack,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        } else {
            // Estado Inicial: Botón para Iniciar Grabación
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.horizontalGradient(listOf(ZunoSurfaceDark, MetallicGrayDark)))
                    .border(1.dp, ZunoGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasPermission) {
                            onStartRecording()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    .testTag("start_recording_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = ZunoGoldBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "● INICIAR GRABACIÓN VOCAL CRUDA",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
