package com.seoulhankuko.app.ml

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*
import timber.log.Timber

/**
 * Custom ResponseBody wrapper that tracks download progress
 * Emits progress updates via callback as bytes are read
 */
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressCallback: (Long, Long) -> Unit
) : ResponseBody() {
    
    private val bufferedSource: BufferedSource by lazy {
        source(responseBody.source()).buffer()
    }
    
    override fun contentType(): MediaType? = responseBody.contentType()
    
    override fun contentLength(): Long = responseBody.contentLength()
    
    override fun source(): BufferedSource = bufferedSource
    
    private fun source(source: Source): Source {
        return object : ForwardingSource(source) {
            var totalBytesRead = 0L
            
            override fun read(sink: Buffer, byteCount: Long): Long {
                val bytesRead = super.read(sink, byteCount)
                totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                progressCallback(totalBytesRead, responseBody.contentLength())
                return bytesRead
            }
        }
    }
}

