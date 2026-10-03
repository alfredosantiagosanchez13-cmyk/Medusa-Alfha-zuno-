package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MasterProfile
import com.example.data.PolishSettings
import com.example.ui.theme.MetallicGrayDark
import com.example.ui.theme.MetallicGrayLight
import com.example.ui.theme.MetallicGrayMid
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.ZunoBlack
import com.example.ui.theme.ZunoBorderMetallic
import com.example.ui.theme.ZunoGold
import com.example.ui.theme.ZunoGoldBright
import com.example.ui.theme.ZunoSurfaceCard
import com.example.ui.theme.ZunoSurfaceDark

@Composable
fun ZunoManualControls(
    isExpanded: Boolean,
    settings: PolishSettings,
    onToggleExpand: () -> Unit,
    onUpdatePitch: (Float) -> Unit,
    onUpdatePresence: (Float) -> Unit,
    onUpdateVolume: (Float) -> Unit,
    onUpdateBalance: (Float) -> Unit,
    onUpdateBass: (Float) -> Unit,
    onUpdateMid: (Float) -> Unit,
    onUpdateTreble: (Float) -> Unit,
    onUpdateStereoWidth: (Float) -> Unit,
    onSelectProfile: (MasterProfile) -> Unit,
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
        // Collapsible Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ZunoSurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = ZunoGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CONTROL MANUAL OPCIONAL",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (isExpanded) "Ajusta parámetros finos de voz, mezcla y master" else "Modo Automático activo (predeterminado)",
                        color = MetallicGrayMid,
                        fontSize = 10.sp
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = ZunoGold,
                modifier = Modifier.size(24.dp)
            )
        }

        // Expanded Body
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: VOZ
                ControlSection(title = "VOZ") {
                    ManualSlider(
                        label = "Afinación (Auto-Tune)",
                        value = settings.pitchCorrectionStrength,
                        valueRange = 0f..1f,
                        valueDisplay = "${(settings.pitchCorrectionStrength * 100).toInt()}%",
                        onValueChange = onUpdatePitch
                    )
                    ManualSlider(
                        label = "Presencia Vocal",
                        value = settings.vocalPresenceDb,
                        valueRange = 0f..6f,
                        valueDisplay = String.format("+%.1f dB", settings.vocalPresenceDb),
                        onValueChange = onUpdatePresence
                    )
                    ManualSlider(
                        label = "Volumen de Voz",
                        value = settings.vocalVolumeDb,
                        valueRange = -3f..5f,
                        valueDisplay = String.format("%+.1f dB", settings.vocalVolumeDb),
                        onValueChange = onUpdateVolume
                    )
                }

                // Section 2: MEZCLA
                ControlSection(title = "MEZCLA") {
                    ManualSlider(
                        label = "Balance Voz / Instrumental",
                        value = settings.vocalInstrumentalBalance,
                        valueRange = -5f..5f,
                        valueDisplay = if (settings.vocalInstrumentalBalance >= 0) "+${settings.vocalInstrumentalBalance.toInt()} Voz" else "${settings.vocalInstrumentalBalance.toInt()} Inst",
                        onValueChange = onUpdateBalance
                    )
                    ManualSlider(
                        label = "Graves (Low)",
                        value = settings.bassGainDb,
                        valueRange = -4f..6f,
                        valueDisplay = String.format("%+.1f dB", settings.bassGainDb),
                        onValueChange = onUpdateBass
                    )
                    ManualSlider(
                        label = "Medios (Limpieza Mud)",
                        value = settings.midGainDb,
                        valueRange = -4f..3f,
                        valueDisplay = String.format("%+.1f dB", settings.midGainDb),
                        onValueChange = onUpdateMid
                    )
                    ManualSlider(
                        label = "Agudos (Aire & Sibilancia)",
                        value = settings.trebleGainDb,
                        valueRange = -3f..5f,
                        valueDisplay = String.format("%+.1f dB", settings.trebleGainDb),
                        onValueChange = onUpdateTreble
                    )
                    ManualSlider(
                        label = "Estéreo (Ancho 3D)",
                        value = settings.stereoWidthRatio,
                        valueRange = 0.8f..1.6f,
                        valueDisplay = "${(settings.stereoWidthRatio * 100).toInt()}%",
                        onValueChange = onUpdateStereoWidth
                    )
                }

                // Section 3: MASTER
                ControlSection(title = "MASTER") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MasterProfile.values().forEach { profile ->
                            val isSelected = (settings.profile == profile)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) ZunoGold else ZunoSurfaceDark)
                                    .border(1.dp, if (isSelected) ZunoGoldBright else ZunoBorderMetallic, RoundedCornerShape(10.dp))
                                    .clickable { onSelectProfile(profile) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = profile.displayName.uppercase(),
                                        color = if (isSelected) ZunoBlack else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "${profile.targetLufs} LUFS",
                                        color = if (isSelected) ZunoBlack.copy(alpha = 0.8f) else MetallicGrayLight,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ZunoSurfaceDark)
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = ZunoGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ManualSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueDisplay: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = MetallicGrayLight, fontSize = 11.sp)
            Text(text = valueDisplay, color = ZunoGoldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ZunoGoldBright,
                activeTrackColor = ZunoGold,
                inactiveTrackColor = MetallicGrayDark
            ),
            modifier = Modifier.height(26.dp)
        )
    }
}
