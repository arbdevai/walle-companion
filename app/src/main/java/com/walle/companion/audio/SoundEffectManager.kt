package com.walle.companion.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class SoundEffectManager(
    private val isEnabledProvider: () -> Boolean
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val sampleRate = 22050

    fun playBlink() {
        if (!isEnabledProvider()) return
        scope.launch {
            generateSweep(startFreq = 1600.0, endFreq = 2200.0, durationMs = 35, volume = 0.35f)
        }
    }

    fun playHappyChirp() {
        if (!isEnabledProvider()) return
        scope.launch {
            generateSweep(startFreq = 800.0, endFreq = 1400.0, durationMs = 80, volume = 0.5f)
            generateSweep(startFreq = 1400.0, endFreq = 2200.0, durationMs = 110, volume = 0.55f)
        }
    }

    fun playGiggle() {
        if (!isEnabledProvider()) return
        scope.launch {
            generateSweep(startFreq = 1500.0, endFreq = 2200.0, durationMs = 50, volume = 0.5f)
            generateSweep(startFreq = 1700.0, endFreq = 2400.0, durationMs = 50, volume = 0.55f)
            generateSweep(startFreq = 1900.0, endFreq = 2600.0, durationMs = 60, volume = 0.55f)
        }
    }

    fun playCuriousChirp() {
        if (!isEnabledProvider()) return
        scope.launch {
            generateSweep(startFreq = 650.0, endFreq = 1350.0, durationMs = 160, volume = 0.5f)
        }
    }

    fun playSurprise() {
        if (!isEnabledProvider()) return
        scope.launch {
            generateSweep(startFreq = 2200.0, endFreq = 1500.0, durationMs = 60, volume = 0.6f)
            generateSweep(startFreq = 1800.0, endFreq = 2600.0, durationMs = 90, volume = 0.6f)
        }
    }

    fun playDizzy() {
        if (!isEnabledProvider()) return
        scope.launch {
            generateSweep(startFreq = 1200.0, endFreq = 800.0, durationMs = 120, volume = 0.5f)
            generateSweep(startFreq = 800.0, endFreq = 1400.0, durationMs = 120, volume = 0.5f)
        }
    }

    fun playTap() {
        if (!isEnabledProvider()) return
        scope.launch {
            generateSweep(startFreq = 1200.0, endFreq = 1600.0, durationMs = 45, volume = 0.4f)
        }
    }

    private fun generateSweep(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Float
    ) {
        try {
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            if (numSamples <= 0) return

            val buffer = ShortArray(numSamples)
            var phase = 0.0

            for (i in 0 until numSamples) {
                val progress = i.toDouble() / numSamples
                val currentFreq = startFreq + (endFreq - startFreq) * progress
                phase += 2.0 * Math.PI * currentFreq / sampleRate

                val attack = (i.toDouble() / (numSamples * 0.15)).coerceAtMost(1.0)
                val decay = ((numSamples - i).toDouble() / (numSamples * 0.25)).coerceAtMost(1.0)
                val envelope = attack * decay

                val sample = (sin(phase) * envelope * Short.MAX_VALUE * volume).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val bufferSizeInBytes = (buffer.size * 2).coerceAtLeast(minBufferSize)

            // Safe legacy AudioTrack instantiation compatible with Android 5.0/5.1 (API 21/22)
            val track: AudioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSizeInBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSizeInBytes,
                    AudioTrack.MODE_STATIC
                )
            }

            try {
                track.write(buffer, 0, buffer.size)
                track.play()
                Thread.sleep(durationMs.toLong() + 20)
            } finally {
                try {
                    track.stop()
                    track.release()
                } catch (_: Throwable) {
                }
            }
        } catch (_: Throwable) {
            // Guarantee that audio errors will never crash older Android runtimes
        }
    }
}
