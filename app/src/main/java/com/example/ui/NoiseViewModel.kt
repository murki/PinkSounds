package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.NoisePlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NoiseViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("NoiseScapePrefs", Context.MODE_PRIVATE)

    private val player = NoisePlayer()

    // Active Profile State
    private val _activeType = MutableStateFlow(
        NoisePlayer.NoiseType.valueOf(
            sharedPrefs.getString("active_type", NoisePlayer.NoiseType.WHITE.name) ?: NoisePlayer.NoiseType.WHITE.name
        )
    )
    val activeType: StateFlow<NoisePlayer.NoiseType> = _activeType.asStateFlow()

    // Playing State
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Volume Sliders State (ranges: 0.0f to 1.0f)
    private val _whiteVolume = MutableStateFlow(sharedPrefs.getFloat("white_vol", 0.5f))
    val whiteVolume: StateFlow<Float> = _whiteVolume.asStateFlow()

    private val _grayVolume = MutableStateFlow(sharedPrefs.getFloat("gray_vol", 0.5f))
    val grayVolume: StateFlow<Float> = _grayVolume.asStateFlow()

    private val _pinkVolume = MutableStateFlow(sharedPrefs.getFloat("pink_vol", 0.5f))
    val pinkVolume: StateFlow<Float> = _pinkVolume.asStateFlow()

    // Sleep Timer States
    // Remaining seconds left in the countdown, if null - no active timer
    private val _timerRemainingSeconds = MutableStateFlow<Int?>(null)
    val timerRemainingSeconds: StateFlow<Int?> = _timerRemainingSeconds.asStateFlow()

    private var timerJob: Job? = null

    init {
        updatePlayerParams()
    }

    /**
     * Toggles sound play vs pause stop state.
     */
    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    private fun startPlayback() {
        updatePlayerParams()
        player.start()
        _isPlaying.value = true
    }

    private fun pausePlayback() {
        player.stop()
        _isPlaying.value = false
    }

    /**
     * Rotate profile forward (e.g. Swiped up or select next option).
     */
    fun rotateNext() {
        val nextType = when (_activeType.value) {
            NoisePlayer.NoiseType.WHITE -> NoisePlayer.NoiseType.GRAY
            NoisePlayer.NoiseType.GRAY -> NoisePlayer.NoiseType.PINK
            NoisePlayer.NoiseType.PINK -> NoisePlayer.NoiseType.WHITE
        }
        changeType(nextType)
    }

    /**
     * Rotate profile backward (e.g. Swiped down or select previous option).
     */
    fun rotatePrevious() {
        val prevType = when (_activeType.value) {
            NoisePlayer.NoiseType.WHITE -> NoisePlayer.NoiseType.PINK
            NoisePlayer.NoiseType.PINK -> NoisePlayer.NoiseType.GRAY
            NoisePlayer.NoiseType.GRAY -> NoisePlayer.NoiseType.WHITE
        }
        changeType(prevType)
    }

    /**
     * Switch specific noise profile directly.
     */
    fun changeType(type: NoisePlayer.NoiseType) {
        _activeType.value = type
        sharedPrefs.edit().putString("active_type", type.name).apply()
        updatePlayerParams()
    }

    /**
     * Adjust volume for white noise.
     */
    fun setWhiteVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _whiteVolume.value = clamped
        sharedPrefs.edit().putFloat("white_vol", clamped).apply()
        updatePlayerParams()
    }

    /**
     * Adjust volume for gray noise.
     */
    fun setGrayVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _grayVolume.value = clamped
        sharedPrefs.edit().putFloat("gray_vol", clamped).apply()
        updatePlayerParams()
    }

    /**
     * Adjust volume for pink noise.
     */
    fun setPinkVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _pinkVolume.value = clamped
        sharedPrefs.edit().putFloat("pink_vol", clamped).apply()
        updatePlayerParams()
    }

    /**
     * Start a real countdown sleep timer in minutes.
     */
    fun startSleepTimer(minutes: Int) {
        timerJob?.cancel()
        _timerRemainingSeconds.value = minutes * 60

        // Auto start if not already playing
        if (!_isPlaying.value) {
            startPlayback()
        }

        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _timerRemainingSeconds.value ?: break
                if (current <= 1) {
                    _timerRemainingSeconds.value = null
                    pausePlayback()
                    break
                } else {
                    _timerRemainingSeconds.value = current - 1
                }
            }
        }
    }

    /**
     * Turn off the sleep timer.
     */
    fun cancelSleepTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerRemainingSeconds.value = null
    }

    /**
     * Apply a predefined acoustic mixture preset.
     */
    fun applyPreset(presetName: String) {
        when (presetName) {
            "Sleep" -> {
                setWhiteVolume(0.1f)
                setGrayVolume(0.3f)
                setPinkVolume(0.9f)
                changeType(NoisePlayer.NoiseType.PINK)
            }
            "Focus" -> {
                setWhiteVolume(0.8f)
                setGrayVolume(0.4f)
                setPinkVolume(0.2f)
                changeType(NoisePlayer.NoiseType.WHITE)
            }
            "Calm" -> {
                setWhiteVolume(0.2f)
                setGrayVolume(0.8f)
                setPinkVolume(0.5f)
                changeType(NoisePlayer.NoiseType.GRAY)
            }
            "Mute" -> {
                setWhiteVolume(0.0f)
                setGrayVolume(0.0f)
                setPinkVolume(0.0f)
            }
        }
    }

    private fun updatePlayerParams() {
        player.updateParams(
            type = _activeType.value,
            whiteVol = _whiteVolume.value,
            grayVol = _grayVolume.value,
            pinkVol = _pinkVolume.value
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        player.stop()
    }
}
