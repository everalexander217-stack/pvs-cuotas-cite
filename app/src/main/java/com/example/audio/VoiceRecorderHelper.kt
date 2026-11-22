package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

private const val TAG = "VoiceRecorderHelper"

class VoiceRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var activeRecording: Boolean = false

    fun startRecording(): Boolean {
        stopAndReleaseOnly()
        return try {
            val file = File(context.cacheDir, "pvs_voice_${System.currentTimeMillis()}.m4a")
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = file
            activeRecording = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            stopAndReleaseOnly()
            false
        }
    }

    fun stopAndReadBytes(): ByteArray? {
        if (!activeRecording) return null
        val capturedFile = outputFile
        return try {
            recorder?.stop()
            val bytes = capturedFile?.takeIf { it.exists() && it.length() > 0 }?.readBytes()
            bytes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop audio recording cleanly", e)
            null
        } finally {
            stopAndReleaseOnly()
            capturedFile?.delete()
        }
    }

    fun cancelRecording() {
        val capturedFile = outputFile
        stopAndReleaseOnly()
        capturedFile?.delete()
    }

    private fun stopAndReleaseOnly() {
        activeRecording = false
        try {
            recorder?.reset()
        } catch (_: Exception) {
        }
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
        outputFile = null
    }
}
