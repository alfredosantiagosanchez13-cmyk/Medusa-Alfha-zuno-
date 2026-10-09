package com.example.audio

import com.example.audio.network.GeminiAudioNetworkClient
import com.example.audio.network.GeminiMasteringAdvice
import com.example.audio.network.GeminiNetworkResult
import com.example.data.AcousticDiagnosis
import com.example.data.AudioTrackInfo
import com.example.data.MasterProfile
import com.example.data.PolishSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GeminiConsultantResult(
    val explanation: String,
    val isExternalAiActive: Boolean,
    val statusNotice: String?,
    val masteringAdvice: GeminiMasteringAdvice? = null
)

/**
 * Consultor de estudio ZUNO respaldado por Retrofit y Gemini API (gemini-3.5-flash).
 * Integra metadatos de audio físicos para obtener recomendaciones de masterización
 * y explicaciones profesionales. Si la clave no está configurada o hay fallo de red,
 * conmuta con seguridad y transparencia al motor local sin interrumpir el flujo.
 */
object GeminiStudioConsultant {

    private val client by lazy { GeminiAudioNetworkClient.instance }

    suspend fun getStudioDiagnosis(
        trackInfo: AudioTrackInfo,
        diagnosis: AcousticDiagnosis,
        notes: String? = null
    ): GeminiConsultantResult = withContext(Dispatchers.IO) {
        val networkResult = client.requestMasteringAdvice(trackInfo, diagnosis, notes)

        when (networkResult) {
            is GeminiNetworkResult.Success -> {
                GeminiConsultantResult(
                    explanation = networkResult.rawExplanation,
                    isExternalAiActive = true,
                    statusNotice = null,
                    masteringAdvice = networkResult.advice
                )
            }
            is GeminiNetworkResult.Error -> {
                GeminiConsultantResult(
                    explanation = diagnosis.explicacionHumana,
                    isExternalAiActive = false,
                    statusNotice = "Consultor IA externo no disponible (${networkResult.message}). Continuando con motor local.",
                    masteringAdvice = null
                )
            }
        }
    }

    /**
     * Aplica los parámetros recomendados por Gemini (si existen) sobre las configuraciones
     * de pulido y masterización.
     */
    fun applyAdviceToSettings(
        currentSettings: PolishSettings,
        advice: GeminiMasteringAdvice?
    ): PolishSettings {
        if (advice == null) return currentSettings

        val profile = when (advice.perfilRecomendado.lowercase()) {
            "suave" -> MasterProfile.SUAVE
            "potente" -> MasterProfile.POTENTE
            else -> MasterProfile.NATURAL
        }

        return currentSettings.copy(
            profile = profile,
            pitchCorrectionStrength = advice.pitchCorrectionStrength ?: currentSettings.pitchCorrectionStrength,
            vocalPresenceDb = advice.vocalPresenceDb ?: currentSettings.vocalPresenceDb,
            vocalVolumeDb = advice.vocalVolumeDb ?: currentSettings.vocalVolumeDb,
            vocalInstrumentalBalance = advice.vocalInstrumentalBalance ?: currentSettings.vocalInstrumentalBalance,
            bassGainDb = advice.bassGainDb ?: currentSettings.bassGainDb,
            midGainDb = advice.midGainDb ?: currentSettings.midGainDb,
            trebleGainDb = advice.trebleGainDb ?: currentSettings.trebleGainDb,
            stereoWidthRatio = advice.stereoWidthRatio ?: currentSettings.stereoWidthRatio,
            targetLufs = advice.targetLufs ?: profile.targetLufs,
            deEsserActive = advice.deEsserActive ?: currentSettings.deEsserActive
        )
    }
}
