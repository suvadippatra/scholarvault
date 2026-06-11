package com.scholarvault.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.scholarvault.MainActivity
import com.scholarvault.ui.tools.AudioRecorderHelper
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class AudioRecordingService : Service() {

    private lateinit var recorderHelper: AudioRecorderHelper
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var durationJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "audio_recorder_channel_sv"

        const val ACTION_START = "com.scholarvault.action.START"
        const val ACTION_PAUSE = "com.scholarvault.action.PAUSE"
        const val ACTION_RESUME = "com.scholarvault.action.RESUME"
        const val ACTION_STOP = "com.scholarvault.action.STOP"
        const val ACTION_CANCEL = "com.scholarvault.action.CANCEL"

        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_FORMAT = "extra_format"
        const val EXTRA_LOW_SIZE = "extra_low_size"

        val isRecordingState = MutableStateFlow(false)
        val isPausedState = MutableStateFlow(false)
        val durationMsState = MutableStateFlow(0L)
        val amplitudeState = MutableStateFlow(0)
        val currentFileState = MutableStateFlow<File?>(null)
        val selectedFormatState = MutableStateFlow("m4a")
        
        // Polled latest amplitude list
        val dynamicAmplitudes = mutableListOf<Float>()
        
        // Signals that a finished recording is ready to recover or save
        val completedRecordingFile = MutableStateFlow<File?>(null)
    }

    override fun onCreate() {
        super.onCreate()
        recorderHelper = AudioRecorderHelper(applicationContext)
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START -> {
                val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
                val format = intent.getStringExtra(EXTRA_FORMAT) ?: "m4a"
                val lowBitrate = intent.getBooleanExtra(EXTRA_LOW_SIZE, false)
                if (filePath != null) {
                    startRecording(File(filePath), format, lowBitrate)
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
            ACTION_CANCEL -> cancelRecording()
        }

        return START_STICKY
    }

    private fun startRecording(file: File, format: String, lowBitrate: Boolean) {
        if (isRecordingState.value) return
        
        currentFileState.value = file
        selectedFormatState.value = format
        
        recorderHelper.startRecording(file, format, lowBitrate)
        
        isRecordingState.value = true
        isPausedState.value = false
        durationMsState.value = 0L
        synchronized(dynamicAmplitudes) {
            dynamicAmplitudes.clear()
        }

        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startUpdateLoops()
    }

    private fun startUpdateLoops() {
        durationJob?.cancel()
        durationJob = serviceScope.launch {
            while (isRecordingState.value) {
                delay(100)
                if (!isPausedState.value) {
                    durationMsState.value += 100
                    
                    val amp = recorderHelper.getMaxAmplitude()
                    amplitudeState.value = amp
                    
                    synchronized(dynamicAmplitudes) {
                        dynamicAmplitudes.add(amp.toFloat())
                        if (dynamicAmplitudes.size > 80) {
                            dynamicAmplitudes.removeAt(0)
                        }
                    }
                } else {
                    amplitudeState.value = 0
                }
                
                updateNotification()
            }
        }
    }

    private fun pauseRecording() {
        if (!isRecordingState.value || isPausedState.value) return
        recorderHelper.pauseRecording()
        isPausedState.value = true
        updateNotification()
    }

    private fun resumeRecording() {
        if (!isRecordingState.value || !isPausedState.value) return
        recorderHelper.resumeRecording()
        isPausedState.value = false
        updateNotification()
    }

    private fun stopRecording() {
        if (!isRecordingState.value) return
        
        recorderHelper.stopRecording()
        
        val finishedFile = currentFileState.value
        isRecordingState.value = false
        isPausedState.value = false
        durationMsState.value = 0L
        amplitudeState.value = 0
        
        durationJob?.cancel()
        durationJob = null
        
        completedRecordingFile.value = finishedFile
        
        stopForeground(true)
        stopSelf()
    }

    private fun cancelRecording() {
        if (isRecordingState.value) {
            recorderHelper.stopRecording()
        }
        val file = currentFileState.value
        if (file?.exists() == true) {
            file.delete()
        }
        isRecordingState.value = false
        isPausedState.value = false
        durationMsState.value = 0L
        amplitudeState.value = 0
        currentFileState.value = null
        durationJob?.cancel()
        durationJob = null
        
        stopForeground(true)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ScholarVault Sound Recording",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows active recording controls and status"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "sound_recorder")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 201, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (isPausedState.value) {
            val resumeIntent = Intent(this, AudioRecordingService::class.java).apply { action = ACTION_RESUME }
            val pResumeIntent = PendingIntent.getService(this, 202, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_play, "Resume", pResumeIntent).build()
        } else {
            val pauseIntent = Intent(this, AudioRecordingService::class.java).apply { action = ACTION_PAUSE }
            val pPauseIntent = PendingIntent.getService(this, 203, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action.Builder(android.R.drawable.ic_media_pause, "Pause", pPauseIntent).build()
        }

        val saveIntent = Intent(this, AudioRecordingService::class.java).apply { action = ACTION_STOP }
        val pSaveIntent = PendingIntent.getService(this, 204, saveIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val saveAction = NotificationCompat.Action.Builder(android.R.drawable.ic_menu_save, "Save", pSaveIntent).build()

        val seconds = durationMsState.value / 1000
        val mm = seconds / 60
        val ss = seconds % 60
        val durationStr = String.format("%02d:%02d", mm, ss)

        val title = if (isPausedState.value) "Recording Paused" else "Recording Audio..."
        val text = "Duration: $durationStr"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(pauseResumeAction)
            .addAction(saveAction)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
