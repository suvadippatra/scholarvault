package com.scholarvault.ui.tools.pdf_inverter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class PdfInversionWorker(
    private val appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // We keep these IDs static to avoid creating many notifications
    private val NOTIFICATION_ID = 4567
    private val CHANNEL_ID = "pdf_inversion_channel"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uriStr = inputData.getString("uri") ?: return@withContext Result.failure()
        val uri = Uri.parse(uriStr)
        val newName = inputData.getString("new_name") ?: "Inverted_Document.pdf"
        val pagesToInvertStr = inputData.getString("pages") ?: "all"
        val modeStr = inputData.getString("mode") ?: InvertMode.SMART_INVERT.name
        
        Log.d("PdfInversionWorker", "Starting work for $newName")
        val mode = try { InvertMode.valueOf(modeStr) } catch(e: Exception) { InvertMode.SMART_INVERT }
        val item = PdfItem(
            id = "w_" + System.currentTimeMillis(),
            uri = uri,
            name = newName,
            pageCount = 1, // Doesn't matter, processor will find out
            newName = newName,
            pagesToInvertStr = pagesToInvertStr,
            mode = mode
        )

        createNotificationChannel()

        try {
            PdfProcessingRepository.updateState(
                PdfProcessingState.Processing(fileName = newName, progress = 0, currentPage = 0, totalPages = 1)
            )

            // Start foreground if not started yet to ensure completion
            val foregroundInfo = createForegroundInfo("Starting...", 0)
            try {
                setForeground(foregroundInfo)
            } catch (e: Exception) {
                // If the app is deeply backgrounded, setForeground might fail on Android 12+.
                // We ignore it and let WorkManager handle the execution restriction.
            }

            val processor = PdfInverterProcessor()
            val resultUri = processor.processPdfInversion(appContext, item) { progress ->
                setProgressAsync(workDataOf("progress" to progress))
                
                PdfProcessingRepository.updateState(
                    PdfProcessingState.Processing(
                        fileName = newName,
                        progress = progress,
                        currentPage = progress, // Approximation for now
                        totalPages = 100 // Using percent
                    )
                )

                // Only update notification every 10% to prevent spamming SystemUI
                if (progress % 10 == 0 || progress == 100) {
                    try {
                        notificationManager.notify(NOTIFICATION_ID, createNotification("Inverting $newName", progress))
                    } catch (e: SecurityException) {
                        // Ignore if POST_NOTIFICATIONS is not granted
                    }
                }
            }

            if (resultUri != null) {
                val finalUri = PdfInverterProcessor.saveItemToDownloads(appContext, resultUri, newName)
                if (finalUri != null) {
                    PdfProcessingRepository.updateState(PdfProcessingState.Completed(finalUri, newName))
                    showCompletionNotification(newName)
                    return@withContext Result.success()
                } else {
                    PdfProcessingRepository.updateState(PdfProcessingState.Failed("Failed to save to downloads"))
                    showFailureNotification(newName, "Failed to save")
                    return@withContext Result.failure()
                }
            } else {
                PdfProcessingRepository.updateState(PdfProcessingState.Failed("Processing failed"))
                showFailureNotification(newName, "Processing failed")
                return@withContext Result.failure()
            }
        } catch (e: CancellationException) {
            PdfProcessingRepository.updateState(PdfProcessingState.Failed("Cancelled"))
            notificationManager.cancel(NOTIFICATION_ID)
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            PdfProcessingRepository.updateState(PdfProcessingState.Failed(e.message ?: "Unknown error"))
            showFailureNotification(newName, e.message ?: "Error")
            return@withContext Result.failure()
        }
    }

    private fun createForegroundInfo(message: String, progress: Int): ForegroundInfo {
        val notification = createNotification(message, progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotification(message: String, progress: Int): Notification {
        // Intent that opens app when tapped
        val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("ScholarVault PDF Processing")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download) // Standard system icon
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showCompletionNotification(fileName: String) {
        val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Conversion Complete")
            .setContentText("$fileName has been saved to Downloads.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
        } catch (e: SecurityException) {
            // Ignore if POST_NOTIFICATIONS is not granted
        }
    }

    private fun showFailureNotification(fileName: String, reason: String) {
        val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Conversion Failed")
            .setContentText("Failed to process $fileName: $reason")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID + 2, notification)
        } catch (e: SecurityException) {
            // Ignore if POST_NOTIFICATIONS is not granted
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PDF Processing Tasks",
                NotificationManager.IMPORTANCE_LOW 
            ).apply {
                description = "Shows progress for PDF processing"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
