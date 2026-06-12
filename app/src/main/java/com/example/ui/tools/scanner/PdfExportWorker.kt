package com.scholarvault.ui.tools.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.Data
import com.scholarvault.data.model.ScannedDocumentEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PdfExportWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val urisJsonStr = inputData.getString("uris") ?: return@withContext Result.failure()
        val uris = JSONArray(urisJsonStr).let { array ->
            (0 until array.length()).map { Uri.parse(array.getString(it)) }
        }
        val enableOcr = inputData.getBoolean("enableOcr", false)
        val compressionLevel = inputData.getFloat("compressionLevel", 1f)
        val paperSize = inputData.getString("paperSize") ?: "A4"
        val fitMode = inputData.getString("fitMode") ?: "Full Fit"
        val filterMode = inputData.getString("filterMode") ?: "Original"
        val dpiMode = inputData.getString("dpiMode") ?: "300 DPI"
        val scanId = inputData.getString("scanId")

        Log.d("PdfExportWorker", "Starting PDF export. Total pages: ${uris.size}")

        try {
            setForeground(createForegroundInfo(0, uris.size))
        } catch (e: Exception) {
            Log.e("PdfExportWorker", "Failed to setForeground initiating: ${e.message}")
        }
        setProgress(Data.Builder().putInt("progress", 0).putInt("max", uris.size).build())

        var pdfWidth = 595
        var pdfHeight = 842
        
        when (paperSize) {
            "A3" -> { pdfWidth = 842; pdfHeight = 1190 }
            "A4" -> { pdfWidth = 595; pdfHeight = 842 }
            "Legal" -> { pdfWidth = 612; pdfHeight = 1008 }
            "Letter" -> { pdfWidth = 612; pdfHeight = 792 }
            "Custom (1000x1000)" -> { pdfWidth = 1000; pdfHeight = 1000 }
        }

        val pdfDocument = PdfDocument()
        val paint = Paint()
        
        val dpiScaleFactor = when (dpiMode) {
            "150 DPI" -> 0.5f
            "72 DPI" -> 0.24f
            else -> 1f // 300 DPI
        }
        
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val extractedTexts = mutableListOf<String>()

        // Process in batches of 3 to avoid OOM while parallelizing
        val batchSize = 3
        for (batchIndex in uris.indices step batchSize) {
            val batchUris = uris.subList(batchIndex, minOf(batchIndex + batchSize, uris.size))
            
            val processedBitmaps = batchUris.mapIndexed { localIndex, uri ->
                val absoluteIndex = batchIndex + localIndex
                async(Dispatchers.IO) {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    var inputStream = applicationContext.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream.close()
                    }

                    val compressionScale = when (compressionLevel.toInt()) {
                        0 -> 1.0f
                        1 -> 0.6f
                        else -> 0.3f
                    }
                    val scale = compressionScale * dpiScaleFactor
                    options.inSampleSize = if (scale < 1f) (1f / scale).toInt() else 1
                    options.inJustDecodeBounds = false
                    options.inPreferredConfig = Bitmap.Config.RGB_565
                    
                    inputStream = applicationContext.contentResolver.openInputStream(uri)
                    var bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream?.close()
                    
                    if (bitmap != null && filterMode != "Original") {
                        val filteredBitmap = FilterEngine.applyFilter(bitmap, filterMode)
                        if (filteredBitmap != bitmap) {
                            bitmap.recycle()
                            bitmap = filteredBitmap
                        }
                    }
                    
                    var text: String? = null
                    if (enableOcr && bitmap != null) {
                        try {
                            val latch = CountDownLatch(1)
                            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                                .addOnSuccessListener { result ->
                                    text = "Page ${absoluteIndex + 1}:\n${result.text}\n"
                                    latch.countDown()
                                }
                                .addOnFailureListener { latch.countDown() }
                            latch.await(30, TimeUnit.SECONDS)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    Triple(absoluteIndex, bitmap, text)
                }
            }.awaitAll()

            processedBitmaps.forEach { (index, bitmap, text) ->
                if (text != null) extractedTexts.add(text)
                
                if (bitmap != null) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pdfWidth, pdfHeight, index + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    val destRect = android.graphics.RectF(0f, 0f, pdfWidth.toFloat(), pdfHeight.toFloat())
                    when (fitMode) {
                        "Full Fit" -> {
                            val s = minOf(pdfWidth.toFloat() / bitmap.width, pdfHeight.toFloat() / bitmap.height)
                            val nw = bitmap.width * s
                            val nh = bitmap.height * s
                            val dx = (pdfWidth - nw) / 2f
                            val dy = (pdfHeight - nh) / 2f
                            destRect.set(dx, dy, dx + nw, dy + nh)
                        }
                        "Full Width" -> {
                            val s = pdfWidth.toFloat() / bitmap.width
                            val nh = bitmap.height * s
                            destRect.set(0f, 0f, pdfWidth.toFloat(), nh)
                        }
                        "Full Height" -> {
                            val s = pdfHeight.toFloat() / bitmap.height
                            val nw = bitmap.width * s
                            destRect.set(0f, 0f, nw, pdfHeight.toFloat())
                        }
                        "Zoomed Out" -> {
                            val s = minOf(pdfWidth.toFloat() / bitmap.width, pdfHeight.toFloat() / bitmap.height) * 0.8f
                            val nw = bitmap.width * s
                            val nh = bitmap.height * s
                            val dx = (pdfWidth - nw) / 2f
                            val dy = (pdfHeight - nh) / 2f
                            destRect.set(dx, dy, dx + nw, dy + nh)
                        }
                        else -> {
                            destRect.set(0f, 0f, pdfWidth.toFloat(), pdfHeight.toFloat())
                        }
                    }

                    canvas.drawBitmap(bitmap, null, destRect, paint)
                    pdfDocument.finishPage(page)
                    
                    bitmap.recycle()
                }
                System.gc()

                try {
                    setForeground(createForegroundInfo(index + 1, uris.size))
                } catch (e: Exception) {
                    Log.e("PdfExportWorker", "Failed to setForeground update: ${e.message}")
                }
                setProgress(Data.Builder().putInt("progress", index + 1).putInt("max", uris.size).build())
            }
        }

        val customFileName = inputData.getString("customFileName")
        val customOutputUri = inputData.getString("customOutputUri")
        val fileName = if (customFileName != null) {
            if (customFileName.endsWith(".pdf", ignoreCase = true)) customFileName else "$customFileName.pdf"
        } else {
            "Scanned_Document_${System.currentTimeMillis()}.pdf"
        }
        val file = File(applicationContext.filesDir, fileName)
        val fos = FileOutputStream(file)
        pdfDocument.writeTo(fos)
        pdfDocument.close()
        fos.close()

        if (customOutputUri != null) {
            try {
                val uri = android.net.Uri.parse(customOutputUri)
                applicationContext.contentResolver.openOutputStream(uri)?.use { outStream ->
                    file.inputStream().use { it.copyTo(outStream) }
                }
            } catch (e: Exception) {
                Log.e("PdfExportWorker", "Failed to write to custom uri", e)
            }
        }

        if (enableOcr && extractedTexts.isNotEmpty()) {
            val txtFile = File(applicationContext.filesDir, fileName.replace(".pdf", "_ocr.txt"))
            txtFile.writeText(extractedTexts.joinToString("\n---\n"))
        }

        val pagePaths = JSONArray()
        pagePaths.put(file.absolutePath)
        for (u in uris) {
            pagePaths.put(u)
        }
        
        val db = (applicationContext as com.scholarvault.MainApplication).database
        val finalEntity = ScannedDocumentEntity(
            id = scanId ?: java.util.UUID.randomUUID().toString(),
            name = fileName,
            pagePaths = pagePaths.toString()
        )
        db.scannedDocumentDao().insertScan(finalEntity)

        Log.d("PdfExportWorker", "PDF export complete: ${file.absolutePath}")

        val outputData = androidx.work.Data.Builder()
            .putString("scanId", finalEntity.id)
            .putString("fileName", fileName)
            .putString("filePath", file.absolutePath)
            .build()
            
        Result.success(outputData)
    }

    private fun createForegroundInfo(progress: Int, max: Int): ForegroundInfo {
        val CHANNEL_ID = "pdf_export_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "PDF Export Tasks",
                android.app.NotificationManager.IMPORTANCE_LOW 
            ).apply {
                description = "Shows progress for PDF scanning exports"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Generating PDF"
        val cancel = "Cancel"
        
        val intent = androidx.work.WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText("Processed $progress of $max pages...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .setProgress(max, progress, false)
            .build()
            
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return ForegroundInfo(1002, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        }
        return ForegroundInfo(1002, notification)
    }
}
