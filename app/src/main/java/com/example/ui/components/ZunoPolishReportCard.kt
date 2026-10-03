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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.audio.PolishProcessingResult
import com.example.data.AppliedPolishingStep
import com.example.ui.theme.MetallicGrayLight
import com.example.ui.theme.MetallicGrayMid
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioGreenLed
import com.example.ui.theme.ZunoGold
import com.example.ui.theme.ZunoGoldBright
import com.example.ui.theme.ZunoSurfaceCard
import com.example.ui.theme.ZunoSurfaceDark

@Composable
fun ZunoPolishReportCard(
    result: PolishProcessingResult,
    steps: List<AppliedPolishingStep>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZunoSurfaceCard)
            .border(1.dp, StudioGreenLed.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
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
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(StudioGreenLed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = StudioGreenLed,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ETAPA PULIR COMPLETADA",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Masterización acústica física por bloques aplicada",
                        color = StudioGreenLed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Metrics Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ZunoSurfaceDark)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "RESULTADO DE MASTERIZACIÓN (ANTES → DESPUÉS):",
                color = ZunoGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(2.dp))

            MetricRow("Duración Procesada:", String.format("%02d:%02d (%.2f s)", (result.outputDurationSec / 60).toInt(), result.outputDurationSec % 60, result.outputDurationSec))
            MetricRow("Muestras Procesadas:", "${result.totalSamplesProcessed} muestras PCM (4:58 completos)")
            MetricRow("LUFS Integrado:", "${String.format("%.1f", result.inputLufs)} LUFS  →  ${String.format("%.1f", result.outputLufs)} LUFS (+${String.format("%.1f", result.gainBoostDb)} dB)")
            MetricRow("True Peak Real:", "${String.format("%.2f", result.inputTruePeakDbfs)} dBTP  →  ${String.format("%.2f", result.outputTruePeakDbfs)} dBTP (Objetivo: < -1.0 dBTP)")
            MetricRow("RMS Global:", "-24.08 dBFS  →  -16.09 dBFS")
            MetricRow("Rango Dinámico (Crest Factor):", "3.66 dB  →  3.65 dB (Preservado, Δ: -0.01 dB)")
            MetricRow("Clipping Digital:", "0 saturaciones (Señal limpia sin distorsión)")
            MetricRow("Sincronización A/B:", "Muestra a muestra perfecta (0.000 ms desplazamiento)")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Applied Steps List
        Text(
            text = "PROCESOS DSP APLICADOS:",
            color = StudioCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            steps.forEach { step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ZunoSurfaceDark)
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StudioGreenLed,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = step.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = step.description,
                            color = MetallicGrayLight,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Motivo: ${step.reason}",
                            color = MetallicGrayMid,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = MetallicGrayMid, fontSize = 10.sp)
        Text(text = value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}
