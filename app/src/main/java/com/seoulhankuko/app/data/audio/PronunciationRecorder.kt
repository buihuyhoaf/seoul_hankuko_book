package com.seoulhankuko.app.data.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PronunciationRecorder @Inject constructor() {

    private var audioRecord: AudioRecord? = null
    private val recordingScope = CoroutineScope(Dispatchers.Default)
    private var recordingJob: Job? = null
    private val outputStream = ByteArrayOutputStream()

    fun startRecording(
        sampleRate: Int = SAMPLE_RATE,
        channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
        audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
    ): Boolean {
        if (isRecording()) return true
        return try {
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                .coerceAtLeast(sampleRate)
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed with state=${record.state}")
                record.release()
                return false
            }
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                Log.e(TAG, "AudioRecord failed to start recording")
                record.release()
                return false
            }
            audioRecord = record
            outputStream.reset()
            recordingJob = recordingScope.launch {
                val buffer = ByteArray(bufferSize)
                while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        outputStream.write(buffer, 0, read)
                    }
                }
            }
            true
        } catch (security: SecurityException) {
            Log.e(TAG, "Security exception while starting recording", security)
            false
        } catch (throwable: Throwable) {
            Log.e(TAG, "Unexpected error while starting recording", throwable)
            false
        }
    }

    suspend fun stopRecording(): ByteArray {
        val record = audioRecord ?: run {
            outputStream.reset()
            return ByteArray(0)
        }
        try {
            recordingJob?.cancelAndJoin()
        } finally {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
            record.release()
            audioRecord = null
            recordingJob = null
        }
        val data = outputStream.toByteArray()
        outputStream.reset()
        return data
    }

    fun reset() {
        recordingJob?.cancel()
        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        audioRecord = null
        recordingJob = null
        outputStream.reset()
    }

    fun isRecording(): Boolean = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNEL_COUNT = 1
        const val BITS_PER_SAMPLE = 16
        private const val TAG = "PronunciationRecorder"
    }
}

