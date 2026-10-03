package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FullAcousticMetrics
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
import com.example.ui.theme.ZunoSurfaceCard
import com.example.ui.theme.ZunoSurfaceDark

@Composable
fun ZunoFullAnalysisReportCard(
    metrics: FullAcousticMetrics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZunoSurfaceCard)
            .border(1.dp, ZunoGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
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
                        .background(ZunoGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = ZunoGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ANÁLISIS ACÚSTICO COMPLETO",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Medición física bloque a bloque (ITU-R BS.1770 / EBU R128)",
                        color = MetallicGrayMid,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Required Summary Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ZunoSurfaceDark)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            DataLine("ARCHIVO ANALIZADO:", metrics.fileName)
            DataLine("DURACIÓN DEL MP4:", metrics.mp4DurationFormatted)
            DataLine("DURACIÓN DEL AUDIO EXTRAÍDO:", metrics.audioDurationFormatted)
            DataLine("SAMPLE RATE:", "${metrics.sampleRate} Hz")
            DataLine("CANALES:", if (metrics.channels == 2) "2 (Estéreo L/R)" else "${metrics.channels} canal(es)")
            DataLine("MUESTRAS PROCESADAS:", "${metrics.totalSamplesProcessed} muestras PCM")
            
            // ANÁLISIS COMPLETO: SÍ / NO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANÁLISIS COMPLETO:",
                    color = ZunoGoldBright,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (metrics.isFullSongVerified) "SÍ (Canción completa ~4:58)" else "NO (Solo sample de prueba de 15 segundos)",
                    color = if (metrics.isFullSongVerified) StudioGreenLed else StudioOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Full Acoustic Metrics Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ZunoSurfaceDark)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "PARÁMETROS FÍSICOS MEDIDOS:",
                color = ZunoGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))

            MetricRow("LUFS Integrado:", "${String.format("%.1f", metrics.integratedLufs)} LUFS")
            MetricRow("True Peak:", "${String.format("%.2f", metrics.truePeakDbfs)} dBFS")
            MetricRow("RMS Global:", "${String.format("%.2f", metrics.rmsDbfs)} dBFS")
            MetricRow("Rango Dinámico (Crest Factor):", "${String.format("%.1f", metrics.dynamicRangeDb)} dB")
            MetricRow("Picos con Clipping:", "${metrics.clippingOccurrences} muestras saturadas")
            MetricRow("Balance Estéreo:", "L ${String.format("%.1f", metrics.stereoBalanceLeftPercent)}% / R ${String.format("%.1f", metrics.stereoBalanceRightPercent)}%")
            MetricRow("Silencios Relevantes:", "${metrics.silenceIntervalsCount} bloques (${String.format("%.1f", metrics.silenceTotalSeconds)} s)")
            MetricRow("Espectro de Frecuencias:", "Sub: ${metrics.spectrumSubBassDb}dB | Bass: ${metrics.spectrumBassDb}dB | Mid: ${metrics.spectrumMidDb}dB | High: ${metrics.spectrumTrebleDb}dB")
            MetricRow("Método Utilizado:", metrics.processingMethod)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Energy By Sections
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ZunoSurfaceDark)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "ENERGÍA POR SECCIONES:",
                color = StudioCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            metrics.sectionEnergies.forEach { section ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "${section.sectionName} (${section.timeRange}):", color = MetallicGrayLight, fontSize = 10.sp)
                    Text(text = "${String.format("%.1f", section.rmsDbfs)} dBFS (${section.energyLevel})", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Integrity Notice (Full song vs 15s)
        Spacer(modifier = Modifier.height(10.dp))
        if (!metrics.isFullSongVerified) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioOrange.copy(alpha = 0.15f))
                    .border(1.dp, StudioOrange, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StudioOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AVISO DE VALIDACIÓN: El archivo analizado dura ${metrics.audioDurationFormatted}. La canción completa dura ~4:58. Para validar el análisis final completo de 4:58, el archivo completo debe cargarse en el sistema. NO se avanzará a pulir sin validación completa.",
                        color = StudioOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 14.sp
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioGreenLed.copy(alpha = 0.15f))
                    .border(1.dp, StudioGreenLed, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StudioGreenLed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CANCIÓN COMPLETA VALIDADA: Los ${metrics.audioDurationFormatted} fueron analizados íntegramente por bloques continuos sin truncar ni reducir el audio.",
                        color = StudioGreenLed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DataLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MetallicGrayLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MetallicGrayMid, fontSize = 10.sp)
        Text(text = value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
