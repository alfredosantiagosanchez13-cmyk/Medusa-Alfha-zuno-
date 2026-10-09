package com.example.audio.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.AcousticDiagnosis
import com.example.data.AudioTrackInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed interface GeminiNetworkResult {
    data class Success(
        val advice: GeminiMasteringAdvice,
        val rawExplanation: String
    ) : GeminiNetworkResult

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : GeminiNetworkResult
}

class GeminiAudioNetworkClient private constructor() {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adviceAdapter = moshi.adapter(GeminiMasteringAdvice::class.java)

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Envía metadatos de audio y diagnóstico acústico a la API de Gemini (gemini-3.5-flash)
     * solicitando análisis integral y parámetros precisos de masterización.
     */
    suspend fun requestMasteringAdvice(
        trackInfo: AudioTrackInfo,
        diagnosis: AcousticDiagnosis,
        customPromptNotes: String? = null
    ): GeminiNetworkResult = withContext(Dispatchers.IO) {
        val apiKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key == "MY_GEMINI_API_KEY" || key.isBlank()) "" else key
        } catch (_: Throwable) {
            ""
        }

        if (apiKey.isBlank()) {
            return@withContext GeminiNetworkResult.Error("GEMINI_API_KEY no configurada en AI Studio Secrets.")
        }

        val systemInstruction = """
            Eres el Productor e Ingeniero de Masterización Jefe de 'ZUNO AI PULIDOR 56300' (Medusa Alfha).
            Principio rector: "ESTO DEVUELVE TIEMPO. TIEMPO = FAMILIA."
            Tu misión es analizar con rigor acústico los metadatos y mediciones de audio que recibas,
            generar una explicación humana cálida, directa y motivadora, y devolver parámetros
            óptimos de procesamiento vocal, mezcla (EQ) y masterización.
            Debes responder en formato JSON estricto con la estructura requerida.
        """.trimIndent()

        val fullMetrics = diagnosis.fullMetrics
        val spectrumInfo = if (fullMetrics != null) {
            """
            - Espectro Sub-Graves (<60Hz): ${fullMetrics.spectrumSubBassDb} dB
            - Graves (60-250Hz): ${fullMetrics.spectrumBassDb} dB
            - Medios (250-2000Hz): ${fullMetrics.spectrumMidDb} dB
            - Medios-Altos (2-6kHz): ${fullMetrics.spectrumHighMidDb} dB
            - Agudos (>6kHz): ${fullMetrics.spectrumTrebleDb} dB
            - Rango Dinámico: ${fullMetrics.dynamicRangeDb} dB
            - Balance Estéreo L/R: ${fullMetrics.stereoBalanceLeftPercent}% / ${fullMetrics.stereoBalanceRightPercent}%
            """.trimIndent()
        } else {
            "- Rango Dinámico: ${diagnosis.dynamicRangeDb} dB\n- Balance Estéreo: ${diagnosis.stereoWidthPercent}%"
        }

        val userPrompt = """
            Analiza los siguientes metadatos y mediciones acústicas de la canción:
            
            CANCIÓN:
            - Título: "${trackInfo.title}"
            - Formato original: ${trackInfo.format}
            - Frecuencia de muestreo: ${trackInfo.sampleRate} Hz
            - Canales: ${trackInfo.channels}
            - Duración: ${trackInfo.durationMs / 1000}s
            - Tamaño archivo: ${trackInfo.fileSizeFormatted}
            
            DIAGNÓSTICO ACÚSTICO:
            - Calidad Vocal Principal: ${diagnosis.mainVocalScore}/100
            - Desviación de Afinación: ${diagnosis.pitchDeviationCents} cents
            - Frecuencias problemáticas detectadas: ${diagnosis.problematicFreqs.joinToString(", ").ifEmpty { "Ninguna crítica" }}
            - Clipping / Saturación: ${if (diagnosis.hasClipping) "${diagnosis.clippingInstances} muestras saturadas" else "0 (Limpio)"}
            - Sonoridad actual: ${diagnosis.currentLoudnessLufs} LUFS
            - Pico True Peak: ${diagnosis.peakDbfs} dBFS
            - Factor de cresta: ${diagnosis.crestFactorDb} dB
            - Enmascaramiento vocal: ${if (diagnosis.vocalMaskingDetected) "Detectado (la instrumental compite con la voz)" else "Despejado"}
            $spectrumInfo
            
            ${if (!customPromptNotes.isNullOrBlank()) "NOTAS ADICIONALES DEL USUARIO: $customPromptNotes" else ""}
            
            RESPONDE ÚNICAMENTE CON UN OBJETO JSON con esta estructura exacta:
            {
              "resumenProductor": "Explicación humana directa y profesional en máximo 3 oraciones.",
              "diagnosticoVocal": "Evaluación del estado de la voz.",
              "diagnosticoMezcla": "Evaluación del equilibrio instrumental y frecuencias.",
              "diagnosticoDinamica": "Evaluación de pegada, saturación y sonoridad.",
              "perfilRecomendado": "Natural", // o 'Suave' o 'Potente'
              "pitchCorrectionStrength": 0.65, // de 0.0 (off) a 1.0 (máxima corrección)
              "vocalPresenceDb": 2.5, // realce vocal en dB (-3.0 a +6.0)
              "vocalVolumeDb": 1.0, // ganancia vocal en dB (-6.0 a +6.0)
              "vocalInstrumentalBalance": 0.0, // balance instrumental (-3.0 a +3.0)
              "bassGainDb": 1.5, // EQ graves en dB (-6.0 a +6.0)
              "midGainDb": -1.2, // EQ medios en dB (-6.0 a +6.0)
              "trebleGainDb": 2.0, // EQ agudos/aire en dB (-6.0 a +6.0)
              "stereoWidthRatio": 1.25, // ancho estéreo (0.8 a 1.6)
              "targetLufs": -14.0, // objetivo comercial (-16.0 suave, -14.0 natural, -11.5 potente)
              "deEsserActive": true,
              "consejosAdicionales": [
                "Consejo 1 para mejorar la toma vocal o mezcla",
                "Consejo 2 de producción musical"
              ]
            }
        """.trimIndent()

        val request = GeminiGenerateContentRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = userPrompt))
                )
            ),
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemInstruction))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.4f,
                topP = 0.95f,
                topK = 40,
                responseMimeType = "application/json"
            )
        )

        try {
            val response = apiService.generateContent(
                model = MODEL_NAME,
                apiKey = apiKey,
                request = request
            )

            val rawText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()

            if (rawText.isNullOrBlank()) {
                return@withContext GeminiNetworkResult.Error("Gemini devolvió una respuesta vacía.")
            }

            // Limpieza si incluye markdown code block
            val cleanJson = if (rawText.startsWith("```")) {
                rawText.substringAfter("\n").substringBeforeLast("```").trim()
            } else {
                rawText
            }

            val advice = try {
                adviceAdapter.fromJson(cleanJson)
            } catch (e: Exception) {
                Log.w(TAG, "Error parseando JSON de Gemini advice: ${e.message}", e)
                null
            }

            if (advice != null) {
                GeminiNetworkResult.Success(
                    advice = advice,
                    rawExplanation = advice.resumenProductor.ifBlank { rawText }
                )
            } else {
                GeminiNetworkResult.Success(
                    advice = GeminiMasteringAdvice(resumenProductor = cleanJson),
                    rawExplanation = cleanJson
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción conectando con Gemini API: ${e.message}", e)
            GeminiNetworkResult.Error(
                message = "Error en llamada a Gemini API: ${e.localizedMessage ?: e.message}",
                throwable = e
            )
        }
    }

    companion object {
        private const val TAG = "GeminiAudioClient"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
        // gemini-3.5-flash es el modelo requerido para tareas de texto/razonamiento general
        private const val MODEL_NAME = "gemini-3.5-flash"

        val instance: GeminiAudioNetworkClient by lazy {
            GeminiAudioNetworkClient()
        }
    }
}
