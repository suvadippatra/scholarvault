package com.scholarvault.ui.tools.pdf_nup

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NUpProcessingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var activeProcessingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START_PROCESSING) {
            startProcessing()
        } else if (action == ACTION_CANCEL_PROCESSING) {
            cancelProcessing()
        }
        
        // Handle legacy updateProgress intent if any code still uses it directly
        val progress = intent?.getIntExtra("PROGRESS", -1) ?: -1
        if (progress != -1) {
            val max = intent?.getIntExtra("MAX", 100) ?: 100
            val message = intent?.getStringExtra("MESSAGE") ?: "Processing PDF..."
            val isDone = intent?.getBooleanExtra("DONE", false) ?: false
            
            _processProgress.value = if (max > 0) progress.toFloat() / max else 0f
            _processStatusMessage.value = message
            
            val notification = createNotification(message, progress, max)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (isDone) {
                stopForegroundAndSelf()
            }
        }
        
        return START_NOT_STICKY
    }

    private fun startProcessing() {
        activeProcessingJob?.cancel()
        activeProcessingJob = serviceScope.launch {
            _isProcessing.value = true
            _processProgress.value = 0f
            _processStatusMessage.value = "Starting background job..."
            _outputPdfUri.value = null
            
            val initialNotification = createNotification("Initializing background processing...", 0, 100)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, initialNotification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val config = activeConfig
                val itemsToProcess = activeItems
                
                if (itemsToProcess.isEmpty()) {
                    _isProcessing.value = false
                    stopForegroundAndSelf()
                    return@launch
                }

                if (config.processingMode == ProcessingMode.MERGE_ALL || config.processingMode == ProcessingMode.GRID_REPEAT) {
                    // Update states to PROCESSING
                    val updates = itemsToProcess.associate { it.id to ItemStateUpdate(NUpProcessingState.PROCESSING) }
                    _itemStates.value = _itemStates.value + updates

                    val generator = VirtualPageSequenceGenerator(itemsToProcess, config)
                    val sequence = generator.generateSequence()
                    val totalPages = generator.calculateTotalOutputPages(config.rows, config.columns)

                    val onProgress: (Int) -> Unit = { sheetRun ->
                        _processProgress.value = if (totalPages > 0) sheetRun.toFloat() / totalPages else 0f
                        val msg = "Processing Sheet $sheetRun of $totalPages"
                        _processStatusMessage.value = msg
                        updateLiveNotification(msg, sheetRun, totalPages)
                    }

                    val tempFile = NUpProcessor.processCombined(this@NUpProcessingService, sequence, config, totalPages, onProgress)

                    if (tempFile != null) {
                        val defaultName = if (config.outputFileName.isNotBlank()) config.outputFileName else if (config.processingMode == ProcessingMode.GRID_REPEAT) "PhotoGrid_NUp" else "Merged_NUp"
                        val savedUri = saveToMediaStore(this@NUpProcessingService, tempFile, defaultName)
                        if (savedUri != null) {
                            val successUpdates = itemsToProcess.associate { it.id to ItemStateUpdate(NUpProcessingState.SUCCESS, savedUri) }
                            _itemStates.value = _itemStates.value + successUpdates
                            _outputPdfUri.value = savedUri
                            
                            showCompletionNotification(defaultName)
                        } else {
                            val errorUpdates = itemsToProcess.associate { it.id to ItemStateUpdate(NUpProcessingState.ERROR, errorMessage = "Failed to save to MediaStore") }
                            _itemStates.value = _itemStates.value + errorUpdates
                        }
                        tempFile.delete()
                    } else {
                        val errorUpdates = itemsToProcess.associate { it.id to ItemStateUpdate(NUpProcessingState.ERROR, errorMessage = "Processing failed") }
                        _itemStates.value = _itemStates.value + errorUpdates
                    }
                } else {
                    // PARALLEL_BATCH mode
                    for (item in itemsToProcess) {
                        _itemStates.value = _itemStates.value + (item.id to ItemStateUpdate(NUpProcessingState.PROCESSING))
                        _processStatusMessage.value = "Processing ${item.name}..."
                        
                        val generator = VirtualPageSequenceGenerator(listOf(item), config)
                        val sequence = generator.generateSequenceFor(item)
                        val itemCells = sequence.count()
                        val itemPages = Math.ceil(itemCells.toDouble() / (config.columns * config.rows)).toInt()

                        val tempFile = NUpProcessor.processCombined(this@NUpProcessingService, sequence, config, itemPages) { sheetRun ->
                            _processProgress.value = if (itemPages > 0) sheetRun.toFloat() / itemPages else 0f
                            val msg = "Processing ${item.name} ($sheetRun / $itemPages)"
                            _processStatusMessage.value = msg
                            updateLiveNotification(msg, sheetRun, itemPages)
                        }

                        if (tempFile != null) {
                            val savedUri = saveToMediaStore(this@NUpProcessingService, tempFile, item.name)
                            if (savedUri != null) {
                                _itemStates.value = _itemStates.value + (item.id to ItemStateUpdate(NUpProcessingState.SUCCESS, savedUri))
                            } else {
                                _itemStates.value = _itemStates.value + (item.id to ItemStateUpdate(NUpProcessingState.ERROR, errorMessage = "Failed to save to MediaStore"))
                            }
                            tempFile.delete()
                        } else {
                            _itemStates.value = _itemStates.value + (item.id to ItemStateUpdate(NUpProcessingState.ERROR, errorMessage = "Processing failed"))
                        }
                    }
                    showCompletionNotification("Batch complete")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _processStatusMessage.value = "Error: ${e.localizedMessage}"
            } finally {
                _isProcessing.value = false
                stopForegroundAndSelf()
            }
        }
    }

    private fun cancelProcessing() {
        activeProcessingJob?.cancel()
        activeProcessingJob = null
        _isProcessing.value = false
        _processStatusMessage.value = "Cancelled by user"
        
        // Update any items currently processing to ERROR
        val cancelledUpdates = _itemStates.value.mapValues { (_, value) ->
            if (value.state == NUpProcessingState.PROCESSING) {
                ItemStateUpdate(NUpProcessingState.ERROR, errorMessage = "Cancelled")
            } else value
        }
        _itemStates.value = cancelledUpdates
        
        stopForegroundAndSelf()
    }

    private fun updateLiveNotification(message: String, progress: Int, max: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        try {
            manager.notify(NOTIFICATION_ID, createNotification(message, progress, max))
        } catch (e: SecurityException) {
            // Ignore if notification permission is not granted
        }
    }

    private fun showCompletionNotification(title: String) {
        val manager = getSystemService(NotificationManager::class.java)
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, "NUP_CHANNEL")
            .setContentTitle("ScholarVault N-Up Success")
            .setContentText("Finished generating $title.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        try {
            manager.notify(NOTIFICATION_ID + 1, notification)
        } catch (e: SecurityException) {
            // Ignore if notification permission is not granted
        }
    }

    private fun stopForegroundAndSelf() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "NUP_CHANNEL",
                "PDF Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress for N-Up PDF export"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(message: String, progress: Int, max: Int): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, "NUP_CHANNEL")
            .setContentTitle("N-Up PDF Output")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(max, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 15234
        
        const val ACTION_START_PROCESSING = "com.scholarvault.action.START_PROCESSING"
        const val ACTION_CANCEL_PROCESSING = "com.scholarvault.action.CANCEL_PROCESSING"

        // Universal shared StateFlow backings for UI/ViewModel observation
        private val _isProcessing = MutableStateFlow(false)
        val isProcessing = _isProcessing.asStateFlow()

        private val _processProgress = MutableStateFlow(0f)
        val processProgress = _processProgress.asStateFlow()

        private val _processStatusMessage = MutableStateFlow("")
        val processStatusMessage = _processStatusMessage.asStateFlow()

        private val _outputPdfUri = MutableStateFlow<Uri?>(null)
        val outputPdfUri = _outputPdfUri.asStateFlow()

        // Track states of actual items
        private val _itemStates = MutableStateFlow<Map<String, ItemStateUpdate>>(emptyMap())
        val itemStates = _itemStates.value

        // Read thread-safe copy of states
        val itemStatesFlow = _itemStates.asStateFlow()

        // Static parameters Cache
        @Volatile
        var activeItems: List<NUpItem> = emptyList()
        @Volatile
        var activeConfig: NUpConfig = NUpConfig()

        fun startAndProcess(context: Context, items: List<NUpItem>, config: NUpConfig) {
            activeItems = items
            activeConfig = config
            
            // Clear prior results/states
            _outputPdfUri.value = null
            val newStates = items.associate { it.id to ItemStateUpdate(NUpProcessingState.IDLE) }
            _itemStates.value = newStates

            val intent = Intent(context, NUpProcessingService::class.java).apply {
                action = ACTION_START_PROCESSING
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun clearItemStates() {
            _outputPdfUri.value = null
            _itemStates.value = emptyMap()
        }

        fun markSuccessStatesAsPending() {
            _outputPdfUri.value = null
            val current = _itemStates.value
            val updated = current.mapValues { (_, update) ->
                if (update.state == NUpProcessingState.SUCCESS) {
                    ItemStateUpdate(NUpProcessingState.PENDING, null, null)
                } else {
                    update
                }
            }
            _itemStates.value = updated
        }

        fun cancel(context: Context) {
            val intent = Intent(context, NUpProcessingService::class.java).apply {
                action = ACTION_CANCEL_PROCESSING
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Legacy helper kept for backward compatibility if any other class uses it
        fun updateProgress(context: Context, message: String, progress: Int, max: Int, isDone: Boolean = false) {
            val intent = Intent(context, NUpProcessingService::class.java).apply {
                putExtra("MESSAGE", message)
                putExtra("PROGRESS", progress)
                putExtra("MAX", max)
                putExtra("DONE", isDone)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isDone) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun saveToMediaStore(context: Context, tempFile: File, originalName: String): Uri? {
            val resolver = context.contentResolver
            val cleanName = if (originalName.endsWith(".pdf", ignoreCase = true)) originalName else "$originalName.pdf"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ScholarVault/NUp")
                }
            }

            return try {
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
                uri
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

data class ItemStateUpdate(
    val state: NUpProcessingState,
    val resultUri: Uri? = null,
    val errorMessage: String? = null
)

