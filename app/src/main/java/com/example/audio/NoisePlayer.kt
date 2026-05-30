package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.concurrent.thread

/**
 * Highly optimized digital filters for real-time psychoacoustic shaping.
 */
class LowPassFilter(private val alpha: Float) {
    private var prevOutput = 0f
    fun process(input: Float): Float {
        val output = prevOutput + alpha * (input - prevOutput)
        prevOutput = output
        return output
    }
}

class HighPassFilter(private val alpha: Float) {
    private var prevInput = 0f
    private var prevOutput = 0f
    fun process(input: Float): Float {
        val output = alpha * (prevOutput + input - prevInput)
        prevInput = input
        prevOutput = output
        return output
    }
}

/**
 * Real-time DSP synthesis engine that feeds a background AudioTrack stream.
 */
class NoisePlayer {
    
    enum class NoiseType {
        WHITE, GRAY, PINK
    }

    private var audioTrack: AudioTrack? = null
    
    @Volatile 
    private var isPlaying = false
    
    @Volatile 
    private var currentType = NoiseType.WHITE
    
    @Volatile 
    private var whiteVolume = 0.5f
    
    @Volatile 
    private var grayVolume = 0.5f
    
    @Volatile 
    private var pinkVolume = 0.5f

    private var playThread: Thread? = null

    fun start() {
        if (isPlaying) return
        isPlaying = true
        
        playThread = thread(start = true, name = "NoiseSynthThread", priority = Thread.MAX_PRIORITY) {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = maxOf(minBufferSize, 2048)
                
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                // Noise Generators State
                val random = java.util.Random()
                
                // Voss-McCartney Pink Noise State: 12-pole approximation for rich pink frequency slope
                val pinkRows = FloatArray(12)
                var pinkRunningSum = 0f
                var pinkIndex = 0
                for (i in pinkRows.indices) {
                    pinkRows[i] = random.nextFloat() * 2f - 1f
                    pinkRunningSum += pinkRows[i]
                }

                // Gray Noise Filters (designed for 44100 Hz Fs)
                // 1st order LP: cutoff around 150 Hz to capture deep low rumbles
                val lpFilter = LowPassFilter(0.021f)
                // 1st order HP: cutoff around 6000 Hz to capture crystal high hisses
                val hpFilter = HighPassFilter(0.55f)

                // Cache an audio buffer frame
                val shortBuffer = ShortArray(bufferSize / 2)

                while (isPlaying) {
                    val activeType = currentType
                    val activeVolume = when (activeType) {
                        NoiseType.WHITE -> whiteVolume
                        NoiseType.GRAY -> grayVolume
                        NoiseType.PINK -> pinkVolume
                    }

                    for (i in shortBuffer.indices) {
                        var sample = 0f
                        when (activeType) {
                            NoiseType.WHITE -> {
                                sample = random.nextFloat() * 2f - 1f
                            }
                            NoiseType.PINK -> {
                                // Real-time Voss-McCartney algorithm
                                pinkIndex = (pinkIndex + 1) and 4095 // modulo 4096
                                val diff = if (pinkIndex == 0) 11 else Integer.numberOfTrailingZeros(pinkIndex)
                                pinkRunningSum -= pinkRows[diff]
                                pinkRows[diff] = random.nextFloat() * 2f - 1f
                                pinkRunningSum += pinkRows[diff]
                                val white = random.nextFloat() * 2f - 1f
                                // Voss pink: normalize sum of 12 rows + white
                                sample = (pinkRunningSum + white) / 13f
                            }
                            NoiseType.GRAY -> {
                                val white = random.nextFloat() * 2f - 1f
                                // Parallel low-pass & high-pass combination creates a U-shaped equal loudness curve dip
                                val lp = lpFilter.process(white)
                                val hp = hpFilter.process(white)
                                val mid = white * 0.08f
                                sample = lp * 1.5f + hp * 1.8f + mid
                                // Safe clip check
                                if (sample > 1f) sample = 1f
                                else if (sample < -1f) sample = -1f
                            }
                        }

                        val scaled = sample * activeVolume
                        shortBuffer[i] = (scaled * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    val written = audioTrack?.write(shortBuffer, 0, shortBuffer.size) ?: 0
                    if (written < 0) {
                        Log.e("NoisePlayer", "Error writing to AudioTrack: $written")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("NoisePlayer", "Audio loop exception", e)
            } finally {
                releaseAudio()
            }
        }
    }

    fun stop() {
        isPlaying = false
        try {
            playThread?.join(500)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        playThread = null
    }

    fun updateParams(type: NoiseType, whiteVol: Float, grayVol: Float, pinkVol: Float) {
        currentType = type
        whiteVolume = whiteVol
        grayVolume = grayVol
        pinkVolume = pinkVol
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying

    private fun releaseAudio() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("NoisePlayer", "Error releasing AudioTrack", e)
        }
        audioTrack = null
    }
}
