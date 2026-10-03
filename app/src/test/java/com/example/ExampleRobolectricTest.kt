package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AcousticAnalyzer
import com.example.audio.AudioExtractor
import com.example.audio.AudioMetadataReader
import com.example.audio.GeminiStudioConsultant
import com.example.data.AudioTrackInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ZUNO AI PULIDOR 56300", appName)
    }

    @Test
    fun `PRIORIDAD 1 - motor sobrevive caida de gemini sin cancelar flujo`() = runBlocking {
        val testTrack = AudioTrackInfo(
            title = "El Fondo Era el Cimiento.mp4",
            durationMs = 15000L,
            format = "MP4",
            sampleRate = 44100,
            channels = 2,
            bitrateKbps = 192,
            fileSizeFormatted = "164 KB"
        )
        val localDiagnosis = AcousticAnalyzer.analyze(testTrack)

        // Force call to Gemini without valid credentials
        val result = GeminiStudioConsultant.getStudioDiagnosis(testTrack, localDiagnosis)

        // Must NOT throw, must NOT be null, and must provide graceful local fallback notice
        assertNotNull(result)
        assertNotNull(result.explanation)
        assertFalse("Gemini no debe estar activo con clave inválida o ausente", result.isExternalAiActive)
        assertEquals(
            "Consultor IA externo no disponible. Continuando con motor local.",
            result.statusNotice
        )
    }

    @Test
    fun `PRIORIDAD 2 - verificacion 6 puntos de El Fondo Era el Cimiento mp4`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Localizar el archivo real en assets
        val assetFile = File("src/main/assets/El Fondo Era el Cimiento.mp4")
        val targetFile = if (assetFile.exists()) assetFile else File("/app/applet/app/src/main/assets/El Fondo Era el Cimiento.mp4")

        assertTrue("El archivo debe existir físicamente", targetFile.exists())

        // Ejecutar extracción y verificación de los 6 puntos
        val report = AudioExtractor.verifyAndExtract(context, targetFile, targetFile.name)

        // 1. Archivo accesible
        assertTrue("1. Archivo accesible", report.isAccessible)
        assertTrue("Tamaño mayor a cero", report.fileSizeBytes > 0)

        // 2. Extracción de audio
        assertTrue("2. Extracción de audio", report.audioTrackFound)
        assertTrue("Mime de audio válido", report.audioMimeType.startsWith("audio/"))

        // 3. Duración (~4:58 = ~298s)
        assertTrue("3. Duración válida (~4:58)", report.durationMs in 290000..305000)

        // 4. Sample rate
        assertEquals("4. Sample rate", 44100, report.sampleRate)

        // 5. Canales
        assertEquals("5. Canales (estéreo)", 2, report.channels)

        // 6. Lectura correcta del audio
        assertTrue("6. Lectura correcta de paquetes de audio", report.audioReadSuccess)
        assertTrue("Paquetes leídos > 0", report.samplesReadCount > 0)
        assertTrue("Bytes leídos > 0", report.bytesReadTotal > 0)

        // Detenerse aquí: comprobar que el reporte es exitoso y no procesa etapas siguientes
        assertTrue("Extracción exitosa", report.isSuccess)
    }
}
