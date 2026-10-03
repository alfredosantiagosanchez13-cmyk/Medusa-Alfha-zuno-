package com.example.ui.components

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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.audio.PlaybackMode
import com.example.ui.theme.MetallicGrayDark
import com.example.ui.theme.MetallicGrayLight
import com.example.ui.theme.MetallicGrayMid
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioGreenLed
import com.example.ui.theme.ZunoBlack
import com.example.ui.theme.ZunoBorderMetallic
import com.example.ui.theme.ZunoGold
import com.example.ui.theme.ZunoGoldBright
import com.example.ui.theme.ZunoGoldDark
import com.example.ui.theme.ZunoSurfaceCard
import com.example.ui.theme.ZunoSurfaceDark

@Composable
fun ZunoAbPlayerControl(
    playbackMode: PlaybackMode,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onSelectMode: (PlaybackMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ZunoSurfaceCard)
            .border(1.dp, ZunoBorderMetallic, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Label Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMPARACIÓN A / B EN TIEMPO REAL",
                color = MetallicGrayMid,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) StudioGreenLed else MetallicGrayDark)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPlaying) "MONITOR ACTIVO" else "PAUSADO",
                    color = if (isPlaying) StudioGreenLed else MetallicGrayMid,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dual A / B instant buttons + Play/Pause button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Button A: ORIGINAL
            val isASelected = (playbackMode == PlaybackMode.ORIGINAL_A)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isASelected) MetallicGrayDark else ZunoSurfaceDark)
                    .border(
                        width = if (isASelected) 2.dp else 1.dp,
                        color = if (isASelected) MetallicGrayLight else ZunoBorderMetallic,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectMode(PlaybackMode.ORIGINAL_A) }
                    .padding(horizontal = 8.dp)
                    .testTag("mode_a_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "A = ORIGINAL ▶",
                        color = if (isASelected) Color.White else MetallicGrayLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isASelected) "● ACTIVA (Sin procesar)" else "Cruda",
                        color = if (isASelected) MetallicGrayLight else MetallicGrayMid,
                        fontSize = 10.sp
                    )
                }
            }

            // Center Play / Pause Round Button
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(ZunoGoldBright, ZunoGold, ZunoGoldDark)
                        )
                    )
                    .clickable { onTogglePlayPause() }
                    .testTag("play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = ZunoBlack,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Button B: PULIDA
            val isBSelected = (playbackMode == PlaybackMode.POLISHED_B)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isBSelected) ZunoGold.copy(alpha = 0.18f) else ZunoSurfaceDark)
                    .border(
                        width = if (isBSelected) 2.dp else 1.dp,
                        color = if (isBSelected) ZunoGoldBright else ZunoBorderMetallic,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectMode(PlaybackMode.POLISHED_B) }
                    .padding(horizontal = 8.dp)
                    .testTag("mode_b_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "B = PULIDA ▶",
                        color = if (isBSelected) ZunoGoldBright else MetallicGrayLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isBSelected) "★ MASTERIZADA" else "IA 56300",
                        color = if (isBSelected) ZunoGold else MetallicGrayMid,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
