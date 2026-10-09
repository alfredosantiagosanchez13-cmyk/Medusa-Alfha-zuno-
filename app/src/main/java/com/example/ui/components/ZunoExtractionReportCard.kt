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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioExtractionReport
import com.example.ui.theme.MetallicGrayDark
import com.example.ui.theme.MetallicGrayLight
import com.example.ui.theme.MetallicGrayMid
import com.example.ui.theme.StudioGreenLed
import com.example.ui.theme.StudioRedClipping
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
                color = if (isOk) StudioGreenLed.copy(alpha = 0.5f) else StudioRedClipping.copy(alpha = 0.6f),
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
                        text = if (isOk) "VALIDACIÓN OBLIGATORIA DE INGESTIÓN · EXITOSA" else "ERROR DE INGESTIÓN REAL",
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

        // Si falló, mostrar banner de error explícito (Punto 3: Eliminar fallback silencioso)
        if (!isOk) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(StudioRedClipping.copy(alpha = 0.15f))
                    .border(1.dp, StudioRedClipping.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "NO SE PUDO LEER EL ARCHIVO SELECCIONADO",
                        color = StudioRedClipping,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = report.errorMessage ?: "Fallo al acceder al InputStream o archivo vacío.",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // TABLA COMPLETA DE 11 PUNTOS (ORDEN ZUNO 13 · REPORTE FINAL)
        Text(
            text = "REPORTE FÍSICO DE INGESTIÓN & EXTRACCIÓN:",
            color = ZunoGold,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(ZunoSurfaceDark)
                .border(1.dp, ZunoBorderMetallic, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReportTableRow("ARCHIVO SELECCIONADO", report.fileName)
            ReportTableRow("URI", report.sourceUri.take(45) + if (report.sourceUri.length > 45) "..." else "")
            ReportTableRow("MIME", report.mimeType)
            ReportTableRow(
                "BYTES ORIGINALES",
                if (report.originalSizeBytes > 0) "${report.originalSizeBytes} B (${String.format("%.2f", report.originalSizeBytes / (1024f * 1024f))} MB)"
                else "No disponible"
            )
            ReportTableRow(
                "BYTES COPIADOS",
                "${report.copiedSizeBytes} B (${String.format("%.2f", report.copiedSizeBytes / (1024f * 1024f))} MB) [Físicamente comprobado]"
            )
            ReportTableRow("DURACIÓN", report.durationFormatted)
            ReportTableRow("SAMPLE RATE", if (report.sampleRate > 0) "${report.sampleRate} Hz" else "0 Hz")
            ReportTableRow(
                "CANALES",
                if (report.channels == 2) "2 (Estéreo L/R)" else if (report.channels > 0) "${report.channels} canal(es)" else "0"
            )
            ReportTableRow(
                "LECTURA",
                if (report.readSuccess) "✓ LECTURA ÍNTEGRA (InputStream validado)" else "✗ FALLO DE LECTURA"
            )
            ReportTableRow(
                "EXTRACCIÓN",
                if (report.extractionSuccess) "✓ AUDIO EXTRAÍDO (${report.bytesReadTotal / 1024} KB en disco)" else "✗ SIN EXTRAER"
            )
            ReportTableRow(
                "ESTADO",
                report.statusText,
                isHighlight = true,
                highlightColor = if (isOk) StudioGreenLed else StudioRedClipping
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Nota de Detención de Etapa
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isOk) ZunoGold.copy(alpha = 0.1f) else StudioRedClipping.copy(alpha = 0.1f))
                .border(1.dp, if (isOk) ZunoGold.copy(alpha = 0.25f) else StudioRedClipping.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isOk)
                    "✓ ETAPA 1 COMPLETADA: La copia local ha sido verificada y el audio extraído físicamente. Habilitada progresivamente la ETAPA 2 (ANALIZAR CON IA)."
                else
                    "⏹ ETAPA BLOQUEADA: No se puede avanzar a Análisis ni Pulido mientras el archivo seleccionado no sea leído y verificado físicamente.",
                color = if (isOk) ZunoGoldBright else StudioRedClipping,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun ReportTableRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    highlightColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MetallicGrayMid,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            color = if (isHighlight) highlightColor else Color.White,
            fontSize = 10.sp,
            fontWeight = if (isHighlight) FontWeight.Black else FontWeight.Normal,
            fontFamily = if (isHighlight) FontFamily.Default else FontFamily.Monospace,
            modifier = Modifier.weight(0.58f)
        )
    }
}
