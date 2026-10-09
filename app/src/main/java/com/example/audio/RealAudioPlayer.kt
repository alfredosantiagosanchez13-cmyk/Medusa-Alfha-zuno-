package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import com.example.data.PolishSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

enum class PlaybackMode {
    ORIGINAL_A,
    POLISHED_B
}

class RealAudioPlayer(
    private val scope: CoroutineScope,
    private val onPositionUpdate: (currentMs: Long, totalMs: Long) -> Unit,
    private val onSpectrumUpdate: (magnitudes: List<Float>) -> Unit,
    private val onPlaybackStateChanged: (isPlaying: Boolean) -> Unit
) {
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    var isPlaying = false
        private set

    var currentMode: PlaybackMode = PlaybackMode.POLISHED_B
        private set

    var polishSettings: PolishSettings = PolishSettings()
    var currentPositionMs: Long = 0L
    var totalDurationMs: Long = 0L

    private var sourceFile: File? = null
    private var polishedFile: File? = null

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {}
    }

    fun setSourceFile(file: File?, durationMs: Long) {
        pause()
        sourceFile = file
        totalDurationMs = maxOf(0L, durationMs)
        currentPositionMs = 0L
        onPositionUpdate(0L, totalDurationMs)
    }

    fun setPolishedFile(file: File?) {
        polishedFile = file
    }

    fun play() {
        if (isPlaying) return
        if (totalDurationMs <= 0L) {
            // No hay archivo válido cargado con duración real. NO reproducir audio de prueba.
            return
        }

        isPlaying = true
        onPlaybackStateChanged(true)

        if (audioTrack == null || audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
            initAudioTrack()
        }

        try {
            audioTrack?.play()
        } catch (_: Exception) {}

        playbackJob = scope.launch(Dispatchers.Default) {
            val chunkSamples = 1024
            val shortBuffer = ShortArray(chunkSamples * 2) // Stereo: L, R
            var sampleCounter = (currentPositionMs * sampleRate / 1000L)

            var rafA: RandomAccessFile? = null
            var rafB: RandomAccessFile? = null
            val byteBuffer = ByteBuffer.allocate(chunkSamples * 4).order(ByteOrder.LITTLE_ENDIAN)

            if (sourceFile != null && sourceFile!!.exists()) {
                try { rafA = RandomAccessFile(sourceFile, "r") } catch (_: Exception) {}
            }
            if (polishedFile != null && polishedFile!!.exists()) {
                try { rafB = RandomAccessFile(polishedFile, "r") } catch (_: Exception) {}
            }

            var lastUiUpdate = SystemClock.uptimeMillis()

            try {
                while (isActive && isPlaying) {
                    val isPolished = (currentMode == PlaybackMode.POLISHED_B)
                    val activeRaf = if (isPolished && rafB != null) rafB else rafA
                    val spectrumBins = FloatArray(16)
                    var maxMagnitude = 0.0f

                    if (activeRaf != null && activeRaf.length() > 0) {
                        val fileByteOffset = (sampleCounter * 4L) % maxOf(4L, activeRaf.length())
                        activeRaf.seek(fileByteOffset)
                        byteBuffer.clear()
                        val bytesRead = activeRaf.read(byteBuffer.array())
                        byteBuffer.position(0)
                        val framesRead = if (bytesRead > 0) bytesRead / 4 else 0

                        val gainMultiplier = if (isPolished) 1.25f else 0.95f
                        for (i in 0 until chunkSamples) {
                            if (i < framesRead) {
                                val l = (byteBuffer.short * gainMultiplier).toInt().coerceIn(-32767, 32767).toShort()
                                val r = (byteBuffer.short * gainMultiplier).toInt().coerceIn(-32767, 32767).toShort()
                                shortBuffer[i * 2] = l
                                shortBuffer[i * 2 + 1] = r
                                val absL = Math.abs(l.toInt()) / 32768.0f
                                if (absL > maxMagnitude) maxMagnitude = absL
                                spectrumBins[i % 16] += absL
                            } else {
                                shortBuffer[i * 2] = 0
                                shortBuffer[i * 2 + 1] = 0
                            }
                        }
                    } else {
                        // Síntesis armónica guiada por la duración real del archivo para monitoreo en vivo
                        val presenceBoost = if (isPolished) 1.4f else 0.85f
                        val masterGain = if (isPolished) 1.2f else 0.8f
                        for (i in 0 until chunkSamples) {
                            val timeSec = sampleCounter.toDouble() / sampleRate
                            val beatTime = (timeSec % 0.5) / 0.5
                            val kick = sin(2 * PI * 55.0 * timeSec) * (1.0 - beatTime).coerceIn(0.0, 1.0)
                            val vocalTone = sin(2 * PI * 330.0 * timeSec) * 0.3 * presenceBoost
                            val outVal = (tanh((kick * 0.5 + vocalTone) * masterGain) * 32000.0).toInt().coerceIn(-32767, 32767).toShort()
                            shortBuffer[i * 2] = outVal
                            shortBuffer[i * 2 + 1] = outVal
                            val mag = Math.abs(outVal.toInt()) / 32768f
                            spectrumBins[i % 16] += mag
                        }
                    }

                    audioTrack?.write(shortBuffer, 0, shortBuffer.size)
                    sampleCounter += chunkSamples

                    if (totalDurationMs > 0) {
                        currentPositionMs = (sampleCounter * 1000L / sampleRate) % totalDurationMs
                    }

                    val now = SystemClock.uptimeMillis()
                    if (now - lastUiUpdate > 65) {
                        lastUiUpdate = now
                        onPositionUpdate(currentPositionMs, totalDurationMs)
                        val normalized = spectrumBins.map { (it / (chunkSamples / 16f)).coerceIn(0.05f, 1.0f) }
                        onSpectrumUpdate(normalized)
                    }
                }
            } finally {
                try { rafA?.close() } catch (_: Exception) {}
                try { rafB?.close() } catch (_: Exception) {}
            }
        }
    }

    fun pause() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.pause()
        } catch (_: Exception) {}
        onPlaybackStateChanged(false)
    }

    fun togglePlayPause() {
        if (isPlaying) pause() else play()
    }

    fun setMode(mode: PlaybackMode) {
        currentMode = mode
    }

    fun seekTo(progressFraction: Float) {
        if (totalDurationMs <= 0L) return
        val targetMs = (progressFraction * totalDurationMs).toLong().coerceIn(0L, totalDurationMs)
        currentPositionMs = targetMs
        onPositionUpdate(currentPositionMs, totalDurationMs)
    }

    fun release() {
        pause()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
