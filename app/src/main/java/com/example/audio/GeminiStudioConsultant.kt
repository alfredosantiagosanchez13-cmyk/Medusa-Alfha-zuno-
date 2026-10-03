package com.example.audio

import android.util.Log
import com.example.BuildConfig
import com.example.data.AcousticDiagnosis
import com.example.data.AudioTrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GeminiConsultantResult(
    val explanation: String,
    val isExternalAiActive: Boolean,
    val statusNotice: String?
)

object GeminiStudioConsultant {
    private const val TAG = "GeminiStudio"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    // Short timeout to guarantee Gemini will NEVER hang the local production pipeline
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getStudioDiagnosis(
        trackInfo: AudioTrackInfo,
        diagnosis: AcousticDiagnosis
    ): GeminiConsultantResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
        } catch (_: Throwable) {
            ""
        }

        if (apiKey.isBlank()) {
            return@withContext GeminiConsultantResult(
                explanation = diagnosis.explicacionHumana,
                isExternalAiActive = false,
                statusNotice = "Consultor IA externo no disponible. Continuando con motor local."
            )
        }

        val prompt = """
            Actúa como productor e ingeniero de masterización de ZUNO AI PULIDOR 56300 (Medusa Alfha).
            Principio: "ESTO DEVUELVE TIEMPO. TIEMPO = FAMILIA."
            
            Analiza estos datos acústicos de "${trackInfo.title}":
            - Voz principal: ${diagnosis.mainVocalScore}/100
            - Afinación: ${diagnosis.pitchDeviationCents} cents
            - Frecuencias molestas: ${diagnosis.problematicFreqs.joinToString(", ")}
            - Clipping: ${if (diagnosis.hasClipping) "${diagnosis.clippingInstances} overs" else "Limpio"}
            - Loudness: ${diagnosis.currentLoudnessLufs} LUFS
            - Estéreo: ${diagnosis.stereoWidthPercent}%
            
            Escribe una explicación en lenguaje humano directo, cálido y profesional (máximo 3 oraciones).
            NO uses tecnicismos complicados.
        """.trimIndent()

        val resultFromNetwork = withTimeoutOrNull(9000L) {
            try {
                val jsonBody = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            }
                            put("parts", parts)
                        }
                        put(contentObj)
                    }
                    put("contents", contents)
                }

                val request = Request.Builder()
                    .url("$BASE_URL?key=$apiKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (response.isSuccessful && responseString.isNotBlank()) {
                    val rootJson = JSONObject(responseString)
                    val candidates = rootJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")

                    if (!text.isNullOrBlank()) {
                        GeminiConsultantResult(
                            explanation = text.trim(),
                            isExternalAiActive = true,
                            statusNotice = null
                        )
                    } else null
                } else {
                    Log.w(TAG, "Gemini call non-200: ${response.code}")
                    null
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Gemini call exception: ${e.message}")
                null
            }
        }

        if (resultFromNetwork != null) {
            return@withContext resultFromNetwork
        }

        // Graceful fallback: NEVER interrupt the pipeline
        return@withContext GeminiConsultantResult(
            explanation = diagnosis.explicacionHumana,
            isExternalAiActive = false,
            statusNotice = "Consultor IA externo no disponible. Continuando con motor local."
        )
    }
}
