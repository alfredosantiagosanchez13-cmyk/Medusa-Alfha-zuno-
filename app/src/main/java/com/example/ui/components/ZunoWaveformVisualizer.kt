package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackMode
import com.example.ui.theme.MetallicGrayDark
import com.example.ui.theme.MetallicGrayLight
import com.example.ui.theme.MetallicGrayMid
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.ZunoGold
import com.example.ui.theme.ZunoGoldBright
import com.example.ui.theme.ZunoSurfaceDark

@Composable
fun ZunoWaveformVisualizer(
    currentMs: Long,
    totalMs: Long,
    spectrumMagnitudes: List<Float>,
    playbackMode: PlaybackMode,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalMs > 0) (currentMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ZunoSurfaceDark)
            .padding(14.dp)
    ) {
        // Frequency Spectrum Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            val isPolished = (playbackMode == PlaybackMode.POLISHED_B)
            val barBrush = if (isPolished) {
                Brush.verticalGradient(listOf(ZunoGoldBright, ZunoGold, StudioCyan))
            } else {
                Brush.verticalGradient(listOf(MetallicGrayLight, MetallicGrayDark))
            }

            spectrumMagnitudes.take(16).forEachIndexed { index, rawMag ->
                val targetHeight = if (isPlaying) {
                    (rawMag * if (isPolished) 1.25f else 0.75f).coerceIn(0.1f, 1.0f)
                } else 0.12f

                val animatedHeight by animateFloatAsState(
                    targetValue = targetHeight,
                    animationSpec = tween(durationMillis = 80),
                    label = "bar_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .fillMaxHeight(animatedHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(barBrush)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrubber Slider
        Slider(
            value = progress,
            onValueChange = { onSeek(it) },
            colors = SliderDefaults.colors(
                thumbColor = if (playbackMode == PlaybackMode.POLISHED_B) ZunoGoldBright else MetallicGrayLight,
                activeTrackColor = if (playbackMode == PlaybackMode.POLISHED_B) ZunoGold else MetallicGrayMid,
                inactiveTrackColor = MetallicGrayDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .testTag("audio_scrubber")
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Timestamps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentMs),
                color = if (playbackMode == PlaybackMode.POLISHED_B) ZunoGold else MetallicGrayLight,
                fontSize = 11.sp
            )
            Text(
                text = if (playbackMode == PlaybackMode.POLISHED_B) "B: PULIDA 56300" else "A: ORIGINAL",
                color = MetallicGrayMid,
                fontSize = 10.sp
            )
            Text(
                text = formatDuration(totalMs),
                color = MetallicGrayLight,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
