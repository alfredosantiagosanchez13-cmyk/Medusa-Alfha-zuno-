package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

data class AudioRecorderState(
    val isRecording: Boolean = false,
    val durationMs: Long = 0L,
    val currentAmplitude: Float = 0f,
    val waveformAmplitudes: List<Float> = emptyList(),
    val recordedFile: File? = null,
    val lastRecordedBytes: Long = 0L,
    val errorMessage: String? = null
)

sealed interface RecordingResult {
    data class Success(
        val file: File,
        val durationMs: Long,
        val sampleRate: Int,
        val channels: Int,
        val sizeBytes: Long
    ) : RecordingResult

    data class Error(val message: String) : RecordingResult
}

class RawAudioRecorder(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channels = 2

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var currentFile: File? = null

    private val _state = MutableStateFlow(AudioRecorderState())
    val state: StateFlow<AudioRecorderState> = _state.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (_state.value.isRecording) return true

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            _state.update { it.copy(errorMessage = "Configuración de audio no soportada por el hardware.") }
            return false
        }

        val bufferSize = minBufferSize * 2

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                _state.update { it.copy(errorMessage = "No se pudo inicializar el micrófono del dispositivo.") }
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e("RawAudioRecorder", "Error iniciando AudioRecord: ${e.message}", e)
            _state.update { it.copy(errorMessage = "Error al iniciar grabación: ${e.message}") }
            return false
        }

        val recordsDir = File(context.filesDir, "recorded_vocals").apply { mkdirs() }
        val timestamp = System.currentTimeMillis()
        val wavFile = File(recordsDir, "Zuno_Vocal_Raw_$timestamp.wav")
        currentFile = wavFile

        _state.update {
            it.copy(
                isRecording = true,
                durationMs = 0L,
                currentAmplitude = 0f,
                waveformAmplitudes = emptyList(),
                recordedFile = null,
                errorMessage = null
            )
        }

        recordingJob = scope.launch(Dispatchers.IO) {
            val shortBuffer = ShortArray(1024)
            val byteBuffer = ByteBuffer.allocate(shortBuffer.size * 2).order(ByteOrder.LITTLE_ENDIAN)

            var totalShortsRead = 0L
            val startTime = System.currentTimeMillis()
            val ampHistory = mutableListOf<Float>()

            FileOutputStream(wavFile).use { fos ->
                // Escribir cabecera WAV de 44 bytes temporal que actualizaremos al finalizar
                writeWavHeaderPlaceholder(fos)

                var lastUiUpdate = System.currentTimeMillis()

                while (isActive && _state.value.isRecording) {
                    val readCount = audioRecord?.read(shortBuffer, 0, shortBuffer.size) ?: -1
                    if (readCount > 0) {
                        byteBuffer.clear()
                        var maxAmp = 0.0f
                        for (i in 0 until readCount) {
                            val sample = shortBuffer[i]
                            byteBuffer.putShort(sample)
                            val normalized = abs(sample.toInt()) / 32768.0f
                            if (normalized > maxAmp) maxAmp = normalized
                        }
                        fos.write(byteBuffer.array(), 0, readCount * 2)
                        totalShortsRead += readCount

                        val now = System.currentTimeMillis()
                        if (now - lastUiUpdate > 80) {
                            lastUiUpdate = now
                            ampHistory.add(maxAmp.coerceIn(0.05f, 1.0f))
                            if (ampHistory.size > 40) ampHistory.removeAt(0)

                            val elapsed = now - startTime
                            _state.update {
                                it.copy(
                                    durationMs = elapsed,
                                    currentAmplitude = maxAmp,
                                    waveformAmplitudes = ampHistory.toList()
                                )
                            }
                        }
                    } else if (readCount < 0) {
                        break
                    }
                }
                fos.flush()
            }

            // Actualizar cabecera WAV con tamaño real
            updateWavHeader(wavFile, totalShortsRead * 2L, sampleRate, channels)

            val finalDurationMs = if (sampleRate > 0 && channels > 0) {
                (totalShortsRead * 1000L) / (sampleRate * channels)
            } else 0L

            _state.update {
                it.copy(
                    isRecording = false,
                    durationMs = finalDurationMs,
                    recordedFile = wavFile,
                    lastRecordedBytes = wavFile.length()
                )
            }
        }

        return true
    }

    fun stopRecording(): RecordingResult {
        if (!_state.value.isRecording && currentFile == null) {
            return RecordingResult.Error("No hay grabación activa.")
        }

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        _state.update { it.copy(isRecording = false) }
        recordingJob?.cancel()
        recordingJob = null

        val file = currentFile
        if (file == null || !file.exists() || file.length() <= 44L) {
            return RecordingResult.Error("Grabación vacía o no guardada (0 bytes de audio).")
        }

        val pcmBytes = file.length() - 44L
        val durMs = (pcmBytes * 1000L) / (sampleRate * channels * 2L)

        return RecordingResult.Success(
            file = file,
            durationMs = durMs,
            sampleRate = sampleRate,
            channels = channels,
            sizeBytes = file.length()
        )
    }

    fun cancelRecording() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        recordingJob?.cancel()
        recordingJob = null
        currentFile?.delete()
        currentFile = null
        _state.update {
            it.copy(
                isRecording = false,
                durationMs = 0L,
                currentAmplitude = 0f,
                waveformAmplitudes = emptyList(),
                recordedFile = null
            )
        }
    }

    private fun writeWavHeaderPlaceholder(out: FileOutputStream) {
        val dummyHeader = ByteArray(44)
        out.write(dummyHeader)
    }

    private fun updateWavHeader(file: File, pcmDataLength: Long, sampleRate: Int, channels: Int) {
        if (!file.exists()) return
        val totalDataLen = pcmDataLength + 36
        val byteRate = sampleRate * channels * 2

        val header = ByteArray(44)
        // RIFF/WAVE header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // 'fmt ' chunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1 (PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        // 'data' chunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmDataLength and 0xff).toByte()
        header[41] = ((pcmDataLength shr 8) and 0xff).toByte()
        header[42] = ((pcmDataLength shr 16) and 0xff).toByte()
        header[43] = ((pcmDataLength shr 24) and 0xff).toByte()

        try {
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(0)
                raf.write(header)
            }
        } catch (e: Exception) {
            Log.e("RawAudioRecorder", "Error actualizando cabecera WAV: ${e.message}")
        }
    }
}
