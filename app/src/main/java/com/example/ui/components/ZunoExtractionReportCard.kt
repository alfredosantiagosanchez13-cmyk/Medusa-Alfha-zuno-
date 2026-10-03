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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.VideoFile
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
import com.example.audio.AudioExtractionReport
import com.example.ui.theme.MetallicGrayLight
import com.example.ui.theme.MetallicGrayMid
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioGreenLed
import com.example.ui.theme.StudioRedClipping
import com.example.ui.theme.ZunoBlack
import com.example.ui.theme.ZunoBorderMetallic
import com.example.ui.theme.ZunoGold
import com.example.ui.theme.ZunoGoldBright
import com.example.ui.theme.ZunoSurfaceCard
import com.example.ui.theme.ZunoSurfaceDark

@Composable
fun ZunoExtractionReportCard(
    report: AudioExtractionReport,
    modifier: Modifier = Modifier
) {
    val isOk = report.isSuccess

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZunoSurfaceCard)
            .border(
                width = 1.dp,
                color = if (isOk) StudioGreenLed.copy(alpha = 0.5f) else StudioRedClipping.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
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
                        .background(if (isOk) StudioGreenLed.copy(alpha = 0.2f) else StudioRedClipping.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isOk) StudioGreenLed else StudioRedClipping,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isOk) "RESULTADO: EXTRACCIÓN DE AUDIO EXITOSA" else "RESULTADO: FALLO EN EXTRACCIÓN",
                        color = if (isOk) StudioGreenLed else StudioRedClipping,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = report.fileName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 6-Point Verification Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ZunoSurfaceDark)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CheckRow(
                number = "1",
                label = "Archivo accesible",
                status = if (report.isAccessible) "ACCESIBLE (${report.fileSizeFormatted})" else "NO ACCESIBLE",
                isOk = report.isAccessible
            )
            CheckRow(
                number = "2",
                label = "Extracción de audio",
                status = if (report.audioTrackFound) "CORRECTA (Stream ${report.audioMimeType})" else "SIN PISTA DE AUDIO",
                isOk = report.audioTrackFound
            )
            CheckRow(
                number = "3",
                label = "Duración",
                status = report.durationFormatted,
                isOk = report.durationMs > 0
            )
            CheckRow(
                number = "4",
                label = "Sample rate",
                status = "${report.sampleRate} Hz",
                isOk = report.sampleRate > 0
            )
            CheckRow(
                number = "5",
                label = "Canales",
                status = if (report.channels == 2) "2 (Estéreo L/R)" else "${report.channels} canal(es)",
                isOk = report.channels > 0
            )
            CheckRow(
                number = "6",
                label = "Lectura correcta audio",
                status = if (report.audioReadSuccess) "LECTURA ÍNTEGRA (${report.samplesReadCount} paquetes, ${report.bytesReadTotal / 1024} KB)" else "ERROR DE LECTURA",
                isOk = report.audioReadSuccess
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Halt Notice: "Detenido tras extracción"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(ZunoGold.copy(alpha = 0.1f))
                .border(1.dp, ZunoGold.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = "⏹ ETAPA DETENIDA: La pista ha sido extraída y verificada. Auto-Tune, EQ, mezcla, master y exportación permanecen inactivos hasta habilitar progresivamente la siguiente etapa.",
                color = ZunoGoldBright,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun CheckRow(
    number: String,
    label: String,
    status: String,
    isOk: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.45f)) {
            Text(
                text = "$number. $label:",
                color = MetallicGrayLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.55f)) {
            Text(
                text = (if (isOk) "✓ " else "✗ ") + status,
                color = if (isOk) Color.White else StudioRedClipping,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
