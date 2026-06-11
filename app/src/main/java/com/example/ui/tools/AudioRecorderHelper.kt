package com.scholarvault.ui.tools

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.*

class AudioRecorderHelper(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    var isPaused = false
        private set
    private var recordingJob: Job? = null
    private var pcmFile: File? = null
    
    var currentOutputFile: File? = null
        private set
        
    var currentFormat: String = "m4a" // m4a, ogg, wav
    private var wavAmplitude = 0

    @SuppressLint("MissingPermission")
    fun startRecording(outputFile: File, format: String, lowBitrate: Boolean = false) {
        currentOutputFile = outputFile
        currentFormat = format
        isRecording = true
        isPaused = false
        wavAmplitude = 0

        if (format == "wav") {
            startWavRecording(outputFile, lowBitrate)
        } else {
            startMediaRecorder(outputFile, format, lowBitrate)
        }
    }

    private fun startMediaRecorder(outputFile: File, format: String, lowBitrate: Boolean) {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            
            if (format == "ogg") {
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                     setOutputFormat(MediaRecorder.OutputFormat.OGG)
                     setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                 } else {
                     setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                     setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                 }
            } else {
                 setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                 setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                 if (lowBitrate) {
                     setAudioEncodingBitRate(16000)
                     setAudioSamplingRate(16000)
                 } else {
                     setAudioEncodingBitRate(128000)
                     setAudioSamplingRate(44100)
                 }
            }
            
            setOutputFile(outputFile.absolutePath)
            
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWavRecording(outputFile: File, lowBitrate: Boolean) {
        val sampleRate = if (lowBitrate) 16000 else 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
        
        pcmFile = File(context.cacheDir, "temp_record.pcm")
        
        audioRecord?.startRecording()
        
        recordingJob = CoroutineScope(Dispatchers.IO).launch {
            val outputStream = FileOutputStream(pcmFile)
            val data = ByteArray(bufferSize)
            
            while (isRecording) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: 0
                if (!isPaused && read > 0) {
                    outputStream.write(data, 0, read)
                    
                    // Dynamic volume (RMS) calculation for 16-bit PCM little endian
                    var sum = 0.0
                    val numSamples = read / 2
                    for (i in 0 until numSamples) {
                        if (2 * i + 1 < read) {
                            val sample = ((data[2 * i + 1].toInt() shl 8) or (data[2 * i].toInt() and 0xFF)).toShort()
                            sum += sample * sample
                        }
                    }
                    val rmsVal = if (numSamples > 0) Math.sqrt(sum / numSamples) else 0.0
                    wavAmplitude = rmsVal.toInt()
                } else {
                    wavAmplitude = 0
                }
            }
            outputStream.close()
            
            // Convert to WAV
            pcmToWav(pcmFile!!, outputFile, sampleRate, 1, 16)
            pcmFile?.delete()
        }
    }

    fun pauseRecording() {
        if (!isRecording || isPaused) return
        isPaused = true
        if (currentFormat != "wav") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    mediaRecorder?.pause()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun resumeRecording() {
        if (!isRecording || !isPaused) return
        isPaused = false
        if (currentFormat != "wav") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    mediaRecorder?.resume()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        isPaused = false
        
        if (currentFormat == "wav") {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            // Job finishes and converts
        } else {
            try {
                mediaRecorder?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }
    
    fun getMaxAmplitude(): Int {
        if (isPaused) return 0
        return if (currentFormat == "wav") {
            wavAmplitude
        } else {
            mediaRecorder?.maxAmplitude ?: 0
        }
    }

    private fun pcmToWav(pcmFile: File, wavFile: File, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val pcmLength = pcmFile.length()
        val wavLength = pcmLength + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val inStream = FileInputStream(pcmFile)
        val outStream = FileOutputStream(wavFile)

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (wavLength and 0xff).toByte()
        header[5] = ((wavLength shr 8) and 0xff).toByte()
        header[6] = ((wavLength shr 16) and 0xff).toByte()
        header[7] = ((wavLength shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmLength and 0xff).toByte()
        header[41] = ((pcmLength shr 8) and 0xff).toByte()
        header[42] = ((pcmLength shr 16) and 0xff).toByte()
        header[43] = ((pcmLength shr 24) and 0xff).toByte()

        outStream.write(header, 0, 44)

        val buffer = ByteArray(8192)
        var bytesRead = inStream.read(buffer)
        while (bytesRead != -1) {
            outStream.write(buffer, 0, bytesRead)
            bytesRead = inStream.read(buffer)
        }

        inStream.close()
        outStream.close()
    }
}
