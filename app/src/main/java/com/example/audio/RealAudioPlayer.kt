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

    @Volatile
    var isPlaying: Boolean = false
        private set

    @Volatile
    var currentMode: PlaybackMode = PlaybackMode.POLISHED_B

    @Volatile
    var polishSettings: PolishSettings = PolishSettings()

    var totalDurationMs: Long = 180000L
    private var currentPositionMs: Long = 0L

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(8192)

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

    fun play() {
        if (isPlaying) return
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

            val pcmA = java.io.File("/tmp/cimiento_extracted_full.pcm")
            val pcmB = java.io.File("/tmp/cimiento_polished_full.pcm")
            val hasPcmFiles = pcmA.exists() && pcmB.exists()

            var rafA: java.io.RandomAccessFile? = null
            var rafB: java.io.RandomAccessFile? = null
            val byteBuffer = java.nio.ByteBuffer.allocate(chunkSamples * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)

            if (hasPcmFiles) {
                try {
                    rafA = java.io.RandomAccessFile(pcmA, "r")
                    rafB = java.io.RandomAccessFile(pcmB, "r")
                } catch (_: Exception) {}
            }

            // Musical parameters: tempo 120 bpm (2 beats per sec)
            val beatDurationSec = 0.5
            val chordFreqs = doubleArrayOf(220.0, 261.63, 329.63, 392.0) // A minor 7

            var lastUiUpdate = SystemClock.uptimeMillis()

            try {
                while (isActive && isPlaying) {
                    val isPolished = (currentMode == PlaybackMode.POLISHED_B)
                    val activeRaf = if (isPolished) rafB else rafA

                    val spectrumBins = FloatArray(16)
                    var maxMagnitude = 0.0f

                    if (activeRaf != null) {
                        val fileByteOffset = (sampleCounter * 4L) % maxOf(4L, activeRaf.length())
                        activeRaf.seek(fileByteOffset)
                        byteBuffer.clear()
                        val bytesRead = activeRaf.read(byteBuffer.array())
                        byteBuffer.position(0)
                        val framesRead = if (bytesRead > 0) bytesRead / 4 else 0

                        for (i in 0 until chunkSamples) {
                            if (i < framesRead) {
                                val l = byteBuffer.short
                                val r = byteBuffer.short
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
                        val presenceBoost = if (isPolished) (1.0f + (polishSettings.vocalPresenceDb / 6.0f)) else 0.85f
                        val bassPunch = if (isPolished) (1.0f + (polishSettings.bassGainDb / 8.0f)) else 1.0f
                        val trebleAir = if (isPolished) (1.0f + (polishSettings.trebleGainDb / 7.0f)) else 0.8f
                        val stereoWidth = if (isPolished) polishSettings.stereoWidthRatio else 0.75f
                        val masterGain = if (isPolished) 1.25f else 0.75f

                        for (i in 0 until chunkSamples) {
                            val timeSec = sampleCounter.toDouble() / sampleRate
                            val beatIndex = ((timeSec / beatDurationSec) % 4).toInt()

                            val beatProgress = (timeSec % beatDurationSec) / beatDurationSec
                            val kickEnv = (1.0 - beatProgress).coerceIn(0.0, 1.0)
                            val kickWave = sin(2 * PI * (55.0 * (1.0 + 2.0 * kickEnv)) * timeSec) * kickEnv * kickEnv
                            val subBass = sin(2 * PI * 55.0 * timeSec) * 0.4 * bassPunch

                            val snareEnv = if (beatIndex == 1 || beatIndex == 3) {
                                (1.0 - beatProgress * 2.0).coerceIn(0.0, 1.0)
                            } else 0.0
                            val snareNoise = (sin(2 * PI * 420.0 * timeSec) + (sin(2 * PI * 1800.0 * timeSec) * 0.5)) * snareEnv * 0.35

                            val hatEnv = (1.0 - ((timeSec % 0.25) / 0.25) * 4.0).coerceIn(0.0, 1.0)
                            val hatWave = (sin(2 * PI * 8500.0 * timeSec) * 0.15 + sin(2 * PI * 12000.0 * timeSec) * 0.1) * hatEnv * trebleAir

                            val melodyNote = chordFreqs[((timeSec * 1.5).toInt()) % chordFreqs.size]
                            val vibrato = sin(2 * PI * 5.0 * timeSec) * if (isPolished) 1.5 else 4.0
                            val vocalWave = (
                                sin(2 * PI * (melodyNote + vibrato) * timeSec) * 0.45 +
                                sin(2 * PI * (melodyNote * 2 + vibrato) * timeSec) * 0.15 * presenceBoost +
                                sin(2 * PI * (melodyNote * 3) * timeSec) * 0.08
                            ) * presenceBoost

                            val rawMono = (kickWave * 0.5 + subBass * 0.4 + snareNoise + hatWave + vocalWave * 0.6)
                            val leftPan = rawMono * (1.0 - (stereoWidth - 1.0) * 0.3 * cos(2 * PI * 0.5 * timeSec))
                            val rightPan = rawMono * (1.0 + (stereoWidth - 1.0) * 0.3 * cos(2 * PI * 0.5 * timeSec))

                            val leftOut = (tanh(leftPan * masterGain) * 32000.0).toInt().coerceIn(-32767, 32767).toShort()
                            val rightOut = (tanh(rightPan * masterGain) * 32000.0).toInt().coerceIn(-32767, 32767).toShort()

                            shortBuffer[i * 2] = leftOut
                            shortBuffer[i * 2 + 1] = rightOut

                            val absSample = Math.abs(leftOut.toInt()) / 32768.0f
                            if (absSample > maxMagnitude) maxMagnitude = absSample
                            spectrumBins[i % 16] += absSample
                        }
                    }

                    audioTrack?.write(shortBuffer, 0, shortBuffer.size)
                    sampleCounter += chunkSamples
                    currentPositionMs = (sampleCounter * 1000L / sampleRate) % totalDurationMs

                    val now = SystemClock.uptimeMillis()
                    if (now - lastUiUpdate > 65) {
                        lastUiUpdate = now
                        onPositionUpdate(currentPositionMs, totalDurationMs)
                        val normalizedSpectrum = spectrumBins.map { (it / (chunkSamples / 16f)).coerceIn(0.05f, 1.0f) }
                        onSpectrumUpdate(normalizedSpectrum)
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
