package com.scholarvault.ui.tools.focustimer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*

enum class TimerTheme {
    Minimalist,
    FlipClock,
    Wonderspace,
    SevenSegment
}

enum class TimerBackground {
    Black,
    Photo,
    Video
}

enum class TimerSound {
    None,
    SeaBeach,
    Raining,
    Autumn,
    Muse,
    Storm
}

enum class LofiTrack {
    None,
    LofiChill,
    LofiBeats,
    LofiStudy,
    LofiJazz
}

data class TimerPreset(val title: String, val minutes: Int)

object FocusTimerManager {
    var title by mutableStateOf("Study Session")
    var totalSeconds by mutableIntStateOf(25 * 60)
    var remainingSeconds by mutableIntStateOf(25 * 60)
    private var _isRunning = mutableStateOf(false)
    var hasStarted by mutableStateOf(false)
    
    var isRunning: Boolean
        get() = _isRunning.value
        set(value) {
            _isRunning.value = value
            if (value) {
                hasStarted = true
                startTicker()
            } else {
                stopTicker()
            }
        }
        
    var historyPresets by mutableStateOf(
        listOf(
            TimerPreset("Pomodoro", 25),
            TimerPreset("Short Break", 5),
            TimerPreset("Long Break", 15),
            TimerPreset("Deep Work", 60)
        )
    )
    var isFullScreen by mutableStateOf(false)
    
    var currentTheme by mutableStateOf(TimerTheme.Wonderspace)
    var currentBackground by mutableStateOf(TimerBackground.Black)
    var currentSound by mutableStateOf(TimerSound.None)
    var currentLofiTrack by mutableStateOf(LofiTrack.None)
    var isSoundEnabled by mutableStateOf(false)
    var customAudioUrl by mutableStateOf("")
    var isUsingCustomAudio by mutableStateOf(false)

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun startTicker() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isRunning && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--
                if (remainingSeconds <= 0) {
                    isRunning = false
                }
            }
        }
    }

    private fun stopTicker() {
        job?.cancel()
        job = null
    }

    fun reset() {
        isRunning = false
        remainingSeconds = totalSeconds
    }

    fun toggle() {
        isRunning = !isRunning
    }

    fun setTimer(minutes: Int) {
        totalSeconds = minutes * 60
        remainingSeconds = totalSeconds
        isRunning = false
    }
    
    fun setTimerCustom(minutes: Int, seconds: Int = 0) {
        totalSeconds = minutes * 60 + seconds
        remainingSeconds = totalSeconds
        isRunning = false
    }
    
    fun saveAsPreset(newTitle: String, minutes: Int) {
        title = newTitle
        setTimer(minutes)
        val newPreset = TimerPreset(newTitle, minutes)
        if (!historyPresets.contains(newPreset)) {
            historyPresets = (listOf(newPreset) + historyPresets).take(8)
        }
    }
    
    fun movePreset(fromIndex: Int, toIndex: Int) {
        val current = historyPresets.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            historyPresets = current
        }
    }
}
