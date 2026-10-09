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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.SongProject
import com.example.ui.theme.MetallicGrayDark
import com.example.ui.theme.MetallicGrayLight
import com.example.ui.theme.MetallicGrayMid
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioGreenLed
import com.example.ui.theme.ZunoBlack
import com.example.ui.theme.ZunoBorderMetallic
import com.example.ui.theme.ZunoGold
import com.example.ui.theme.ZunoGoldBright
import com.example.ui.theme.ZunoSurfaceCard
import com.example.ui.theme.ZunoSurfaceDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ZunoHistoryModal(
    projects: List<SongProject>,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(ZunoSurfaceCard)
                .border(1.dp, ZunoGold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column {
                // Header
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
                                .background(ZunoGold),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = ZunoBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "HISTORIAL ROOM · ANÁLISIS & IA",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${projects.size} sesiones registradas localmente",
                                color = MetallicGrayMid,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MetallicGrayLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (projects.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aún no hay canciones analizadas en la base de datos local.\nAnaliza una pista para registrar automáticamente sus metadatos y los parámetros de Gemini.",
                            color = MetallicGrayMid,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(projects, key = { it.id }) { project ->
                            HistoryItemRow(project)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryItemRow(project: SongProject) {
    val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(project.timestamp))
    var isExpanded by remember { mutableStateOf(false) }

    val hasGeminiAdvice = project.geminiPitchCorrection != null ||
            project.geminiVocalPresenceDb != null ||
            project.geminiBassGainDb != null

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ZunoSurfaceDark)
            .border(
                1.dp,
                if (hasGeminiAdvice) ZunoGold.copy(alpha = 0.5f) else ZunoBorderMetallic,
                RoundedCornerShape(12.dp)
            )
            .clickable { isExpanded = !isExpanded }
            .padding(12.dp)
    ) {
        Column {
            // Fila Principal: Título, Estado y Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasGeminiAdvice) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Con Asesoría Gemini",
                            tint = ZunoGoldBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = project.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (project.isPolished) StudioGreenLed.copy(alpha = 0.15f) else StudioCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (project.isPolished) "PULIDA" else "ANALIZADA",
                            color = if (project.isPolished) StudioGreenLed else StudioCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MetallicGrayLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metadatos Básicos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${project.format} • ${project.durationFormatted} • ${project.sampleRate} Hz • ${if (project.channels == 2) "Estéreo" else "Mono"}",
                    color = MetallicGrayLight,
                    fontSize = 10.sp
                )
                Text(
                    text = dateStr,
                    color = MetallicGrayMid,
                    fontSize = 9.sp
                )
            }

            // Vista expandida con detalles completos y parámetros de masterización de Gemini
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MetallicGrayDark.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "METADATOS & DIAGNÓSTICO",
                        color = ZunoGoldBright,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Voz: ${project.vocalScore}/100 • Sonoridad: ${project.loudnessLufs} LUFS • Perfil: ${project.masterProfile}",
                        color = Color.White,
                        fontSize = 10.sp
                    )

                    if (project.diagnosisSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "“${project.diagnosisSummary}”",
                            color = MetallicGrayLight,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }

                    // Parámetros de Masterización de Gemini
                    if (hasGeminiAdvice) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ZunoGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PARÁMETROS RECIBIDOS DE GEMINI API",
                                color = ZunoGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Pitch Corr: ${String.format("%.2f", project.geminiPitchCorrection ?: 0f)}",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Presencia: +${project.geminiVocalPresenceDb ?: 0f} dB",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Vol Voz: ${project.geminiVocalVolumeDb ?: 0f} dB",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Graves: ${project.geminiBassGainDb ?: 0f} dB",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Medios: ${project.geminiMidGainDb ?: 0f} dB",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Agudos: ${project.geminiTrebleGainDb ?: 0f} dB",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "Estéreo: ${project.geminiStereoWidth ?: 1f}x",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "Target LUFS: ${project.geminiTargetLufs ?: -14.0f}",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "De-Esser: ${if (project.geminiDeEsserActive == true) "ON" else "OFF"}",
                                color = Color.White,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
