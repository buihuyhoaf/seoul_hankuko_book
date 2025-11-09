package com.seoulhankuko.app.data.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavUtils {

    fun pcmToWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channelCount: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channelCount * bitsPerSample / 8
        val blockAlign = (channelCount * bitsPerSample / 8).toShort()
        val dataSize = pcmData.size
        val totalDataLen = dataSize + 36

        val header = ByteArrayOutputStream().apply {
            write("RIFF".toByteArray(Charsets.US_ASCII))
            writeLittleEndianInt(totalDataLen)
            write("WAVE".toByteArray(Charsets.US_ASCII))
            write("fmt ".toByteArray(Charsets.US_ASCII))
            writeLittleEndianInt(16) // Subchunk1Size for PCM
            writeLittleEndianShort(1) // AudioFormat PCM
            writeLittleEndianShort(channelCount.toShort())
            writeLittleEndianInt(sampleRate)
            writeLittleEndianInt(byteRate)
            writeLittleEndianShort(blockAlign)
            writeLittleEndianShort(bitsPerSample.toShort())
            write("data".toByteArray(Charsets.US_ASCII))
            writeLittleEndianInt(dataSize)
        }.toByteArray()

        return ByteArrayOutputStream(header.size + pcmData.size).apply {
            write(header)
            write(pcmData)
        }.toByteArray()
    }

    private fun ByteArrayOutputStream.writeLittleEndianInt(value: Int) {
        write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array())
    }

    private fun ByteArrayOutputStream.writeLittleEndianShort(value: Short) {
        write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array())
    }
}

