package com.example.audio.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Modelo estructurado devuelto por Gemini con el análisis detallado
 * y parámetros recomendados de ecualización, corrección vocal y masterización.
 */
@JsonClass(generateAdapter = true)
data class GeminiMasteringAdvice(
    @Json(name = "resumenProductor") val resumenProductor: String = "",
    @Json(name = "diagnosticoVocal") val diagnosticoVocal: String = "",
    @Json(name = "diagnosticoMezcla") val diagnosticoMezcla: String = "",
    @Json(name = "diagnosticoDinamica") val diagnosticoDinamica: String = "",
    @Json(name = "perfilRecomendado") val perfilRecomendado: String = "Natural", // Suave, Natural, Potente
    @Json(name = "pitchCorrectionStrength") val pitchCorrectionStrength: Float? = null,
    @Json(name = "vocalPresenceDb") val vocalPresenceDb: Float? = null,
    @Json(name = "vocalVolumeDb") val vocalVolumeDb: Float? = null,
    @Json(name = "vocalInstrumentalBalance") val vocalInstrumentalBalance: Float? = null,
    @Json(name = "bassGainDb") val bassGainDb: Float? = null,
    @Json(name = "midGainDb") val midGainDb: Float? = null,
    @Json(name = "trebleGainDb") val trebleGainDb: Float? = null,
    @Json(name = "stereoWidthRatio") val stereoWidthRatio: Float? = null,
    @Json(name = "targetLufs") val targetLufs: Float? = null,
    @Json(name = "deEsserActive") val deEsserActive: Boolean? = null,
    @Json(name = "consejosAdicionales") val consejosAdicionales: List<String> = emptyList()
)
