package com.seoulhankuko.app.ml

import android.content.Context
import android.graphics.Bitmap
import com.seoulhankuko.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max

/**
 * TFLite classifier for Hangul character recognition
 * Similar to HangulClassifier.java but using TFLite instead of TensorFlow Inference Interface
 */
data class Prediction(
    val char: String,
    val confidence: Float,
    val index: Int
)

class HangulTFLiteClassifier private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>
) {
    companion object {
        private const val MODEL_FILE = "hangul_stroke_model.tflite"
        private const val LABEL_FILE = "2350-common-hangul.txt"
        private const val FEED_DIMENSION = 64
        private const val BITMAP_DIMENSION = 128
        private const val NUM_PREDICTIONS = 5
        
        // Supabase Storage URL - Update this with your actual Supabase project URL
        // Format: https://[project-ref].supabase.co/storage/v1/object/public/[bucket]/[file]
        private const val DEFAULT_SUPABASE_MODEL_URL = "https://uclzdocdphxgyarchdjt.supabase.co/storage/v1/object/public/questions-images/hangul_stroke_model%20(1).tflite"
        
        /**
         * Create classifier instance by loading model and labels from assets
         */
        fun create(context: Context): HangulTFLiteClassifier {
            try {
                // Load model
                val modelBuffer = loadModelFile(context, MODEL_FILE)
                val interpreter = Interpreter(modelBuffer)
                
                // Load labels
                val labels = loadLabels(context, LABEL_FILE)
                
                Timber.d("HangulTFLiteClassifier created successfully with ${labels.size} labels")
                
                return HangulTFLiteClassifier(interpreter, labels)
            } catch (e: Exception) {
                Timber.e(e, "Failed to create HangulTFLiteClassifier")
                throw RuntimeException("Error loading pre-trained model", e)
            }
        }
        
        /**
         * Create classifier instance by downloading model from Supabase if not cached locally
         * Falls back to assets if download fails
         */
        suspend fun createWithSupabase(
            context: Context,
            supabaseUrl: String? = null
        ): HangulTFLiteClassifier = withContext(Dispatchers.IO) {
            val modelUrl = supabaseUrl ?: try {
                // Try to get from BuildConfig if available
                val field = BuildConfig::class.java.getDeclaredField("SUPABASE_MODEL_URL")
                field.isAccessible = true
                field.get(null) as? String
            } catch (e: Exception) {
                Timber.w(e, "Could not read SUPABASE_MODEL_URL from BuildConfig, using default")
                null
            } ?: DEFAULT_SUPABASE_MODEL_URL
            
            Timber.d("Using Supabase model URL: $modelUrl")
            
            val modelFile = File(context.filesDir, MODEL_FILE)
            
            try {
                // Download model if not exists or if we want to force update
                if (!modelFile.exists() || modelFile.length() == 0L) {
                    Timber.d("Model not found locally, downloading from Supabase...")
                    downloadModelFromSupabase(context, modelUrl, modelFile)
                } else {
                    Timber.d("Model found locally (${modelFile.length()} bytes), using cached version")
                }
                
                // Load from local file
                val modelBuffer = loadModelFileFromLocal(modelFile)
                val interpreter = Interpreter(modelBuffer)
                val labels = loadLabels(context, LABEL_FILE)
                
                Timber.d("HangulTFLiteClassifier created successfully with ${labels.size} labels from Supabase")
                return@withContext HangulTFLiteClassifier(interpreter, labels)
            } catch (e: Exception) {
                Timber.w(e, "Failed to load from Supabase, falling back to assets")
                // Fallback to assets
                return@withContext create(context)
            }
        }
        
        /**
         * Download model from Supabase Storage
         */
        private suspend fun downloadModelFromSupabase(
            context: Context,
            url: String,
            outputFile: File
        ) = withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .build()
                
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Failed to download model: ${response.code} ${response.message}")
                    }
                    
                    response.body?.let { body ->
                        val contentLength = body.contentLength()
                        Timber.d("Downloading model: $contentLength bytes")
                        
                        // Ensure parent directory exists
                        outputFile.parentFile?.mkdirs()
                        
                        body.byteStream().use { input ->
                            outputFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        Timber.d("Model downloaded successfully: ${outputFile.length()} bytes")
                    } ?: throw IOException("Response body is null")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to download model from Supabase")
                // Delete partial file if exists
                if (outputFile.exists()) {
                    outputFile.delete()
                }
                throw e
            }
        }
        
        /**
         * Load model from local file
         */
        private fun loadModelFileFromLocal(file: File): MappedByteBuffer {
            val inputStream = FileInputStream(file)
            val fileChannel = inputStream.channel
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size())
        }
        
        /**
         * Load TFLite model from assets
         */
        private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
            val fileDescriptor = context.assets.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
        
        /**
         * Load labels from assets
         */
        private fun loadLabels(context: Context, labelPath: String): List<String> {
            val labels = mutableListOf<String>()
            try {
                context.assets.open(labelPath).bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty()) {
                            labels.add(trimmed)
                        }
                    }
                }
                Timber.d("Loaded ${labels.size} labels from $labelPath")
            } catch (e: IOException) {
                Timber.e(e, "Failed to load labels from $labelPath")
                throw e
            }
            return labels
        }
    }
    
    /**
     * Classify bitmap and return top predictions
     * @param bitmap Input bitmap (will be resized to 64x64)
     * @return List of top predictions sorted by confidence (descending)
     */
    fun classify(bitmap: Bitmap): List<Prediction> {
        try {
            // Log bitmap info
            Timber.d("Bitmap info: width=${bitmap.width}, height=${bitmap.height}, config=${bitmap.config}")
            
            // Check if bitmap has any non-black pixels
            val samplePixels = IntArray(100)
            bitmap.getPixels(samplePixels, 0, 10, 0, 0, 10, 10)
            val hasNonBlackPixels = samplePixels.any { (it and 0xFFFFFF) != 0 }
            Timber.d("Bitmap has non-black pixels: $hasNonBlackPixels")
            
            // Get input and output tensor details
            val numInputs = interpreter.inputTensorCount
            val numOutputs = interpreter.outputTensorCount
            Timber.d("Model has $numInputs input(s) and $numOutputs output(s)")
            
            // Log all input tensors
            for (i in 0 until numInputs) {
                val tensor = interpreter.getInputTensor(i)
                val shape = tensor.shape()
                val dataType = tensor.dataType()
                Timber.d("Input tensor $i: shape=${shape.contentToString()}, dataType=$dataType (${dataType.name})")
            }
            
            // Log all output tensors
            for (i in 0 until numOutputs) {
                val tensor = interpreter.getOutputTensor(i)
                val shape = tensor.shape()
                val dataType = tensor.dataType()
                Timber.d("Output tensor $i: shape=${shape.contentToString()}, dataType=$dataType (${dataType.name})")
            }
            
            val inputTensor = interpreter.getInputTensor(0)
            val outputTensor = interpreter.getOutputTensor(0)
            
            // Log input shape and data type for debugging
            val inputShape = inputTensor.shape()
            val inputDataType = inputTensor.dataType()
            Timber.d("Using input tensor 0: shape=${inputShape.contentToString()}, dataType=$inputDataType (${inputDataType.name})")
            
            // Check if input data type is supported
            if (inputDataType != DataType.FLOAT32) {
                Timber.e("Unsupported input data type: $inputDataType. Expected FLOAT32.")
                return emptyList()
            }
            
            // Prepare keepProb buffer if model needs it (dropout placeholder)
            var keepProbBuffer: ByteBuffer? = null
            if (numInputs > 1) {
                Timber.d("Model has $numInputs inputs - preparing keepProb input like original TensorFlow model")
                try {
                    val keepProbTensor = interpreter.getInputTensor(1)
                    val keepProbShape = keepProbTensor.shape()
                    Timber.d("Second input (keepProb) shape: ${keepProbShape.contentToString()}")
                    // Create keepProb buffer - value 1.0 means no dropout (inference mode)
                    if (keepProbShape.contentEquals(intArrayOf(1)) || keepProbShape.isEmpty()) {
                        keepProbBuffer = ByteBuffer.allocateDirect(4)
                        keepProbBuffer.order(ByteOrder.nativeOrder())
                        keepProbBuffer.putFloat(1.0f)
                        keepProbBuffer.rewind()
                        Timber.d("Prepared keepProb buffer with value 1.0")
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Could not prepare keepProb input")
                }
            }
            
            // Get output shape and data type
            val outputShape = outputTensor.shape()
            val outputDataType = outputTensor.dataType()
            Timber.d("Output tensor shape: ${outputShape.contentToString()}, dataType: $outputDataType (${outputDataType.name})")
            
            // Check if output data type is supported
            if (outputDataType != DataType.FLOAT32) {
                Timber.e("Unsupported output data type: $outputDataType. Expected FLOAT32.")
                return emptyList()
            }
            
            // Output shape could be [1, numClasses] or [numClasses]
            val numClasses = if (outputShape.size == 2) {
                outputShape[1]
            } else {
                outputShape[0]
            }
            
            Timber.d("Number of classes: $numClasses")
            
            // Prepare input ByteBuffer with shape [1, 64, 64, 1]
            // Get FloatArray from preprocessing
            val (floatArray, totalSize) = preprocessBitmapToFloatArray(bitmap, inputTensor)
            
            // CRITICAL FIX: Create a fresh ByteBuffer from FloatArray right before inference
            // This ensures data is not corrupted by previous operations
            val inputBuffer = ByteBuffer.allocateDirect(totalSize * 4)
            inputBuffer.order(ByteOrder.nativeOrder())
            
            // Write FloatArray to ByteBuffer
            for (value in floatArray) {
                inputBuffer.putFloat(value)
            }
            
            // Verify input buffer size matches tensor size
            val expectedInputSize = inputShape.fold(1) { acc, dim -> acc * dim } * 4 // 4 bytes per float
            val actualInputSize = inputBuffer.capacity()
            Timber.d("Input buffer size: $actualInputSize bytes, expected: $expectedInputSize bytes")
            if (actualInputSize != expectedInputSize) {
                Timber.e("Input buffer size mismatch! Expected $expectedInputSize, got $actualInputSize")
            }
            
            // Verify buffer was written correctly
            inputBuffer.rewind()
            val totalFloats = totalSize
            val checkIndices = listOf(0, 149, totalFloats / 2, totalFloats - 10).filter { it >= 0 && it < totalFloats }
            val checkValues = mutableListOf<Float>()
            for (idx in checkIndices) {
                inputBuffer.position(idx * 4)
                checkValues.add(inputBuffer.float)
            }
            inputBuffer.rewind()
            
            Timber.d("Fresh input buffer values at indices $checkIndices: $checkValues")
            Timber.d("Input buffer position: ${inputBuffer.position()}, limit: ${inputBuffer.limit()}, capacity: ${inputBuffer.capacity()}")
            
            // Check if all checked values are 0
            val allZeros = checkValues.all { it == 0.0f }
            if (allZeros) {
                Timber.e("ERROR: Fresh input buffer has all zeros! FloatArray max: ${floatArray.maxOrNull()}, non-zero count: ${floatArray.count { it > 0.01f }}")
            } else {
                val maxValue = checkValues.maxOrNull() ?: 0f
                Timber.d("Fresh input buffer max value: $maxValue")
            }
            
            // Prepare output ByteBuffer
            val outputSize = if (outputShape.size == 2) {
                outputShape[0] * outputShape[1]
            } else {
                outputShape[0]
            }
            val outputBuffer = ByteBuffer.allocateDirect(outputSize * 4)
            outputBuffer.order(ByteOrder.nativeOrder())
            
            // Verify output buffer size
            val expectedOutputSize = outputSize * 4
            val actualOutputSize = outputBuffer.capacity()
            Timber.d("Output buffer size: $actualOutputSize bytes, expected: $expectedOutputSize bytes")
            
            // Ensure input buffer is properly positioned and has correct limit
            inputBuffer.rewind()
            inputBuffer.limit(inputBuffer.capacity()) // Reset limit to full capacity
            outputBuffer.rewind()
            keepProbBuffer?.rewind()
            
            // Run inference - use runForMultipleInputsOutputs if model has multiple inputs
            Timber.d("Running inference...")
            try {
                if (numInputs > 1 && keepProbBuffer != null) {
                    // Model has multiple inputs, use runForMultipleInputsOutputs
                    // API expects Array<Any> for inputs and Map<Int, Any> for outputs
                    val inputs = arrayOf<Any>(inputBuffer, keepProbBuffer)
                    val outputs = mutableMapOf<Int, Any>()
                    outputs[0] = outputBuffer
                    interpreter.runForMultipleInputsOutputs(inputs, outputs)
                    Timber.d("Inference completed successfully with multiple inputs")
                } else {
                    // Single input, use regular run
                    interpreter.run(inputBuffer, outputBuffer)
                    Timber.d("Inference completed successfully with single input")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during inference")
                throw e
            }
            
            // Log raw output buffer bytes (first 40 bytes = 10 floats) BEFORE reading
            outputBuffer.rewind()
            val rawBytes = ByteArray(minOf(40, outputBuffer.remaining()))
            val outputCheckPosition = outputBuffer.position()
            outputBuffer.get(rawBytes)
            outputBuffer.position(outputCheckPosition)
            Timber.d("Raw output bytes (first 40, hex): ${rawBytes.joinToString(separator = " ") { "%02x".format(it.toInt() and 0xFF) }}")
            
            // Extract probabilities from output buffer
            outputBuffer.rewind()
            val probabilities = FloatArray(numClasses)
            for (i in 0 until numClasses) {
                probabilities[i] = outputBuffer.float
            }
            
            // Log output buffer position after reading
            Timber.d("Output buffer position after reading: ${outputBuffer.position()}, remaining: ${outputBuffer.remaining()}")
            
            // Log raw output for debugging
            Timber.d("Raw probabilities (first 10): ${probabilities.take(10).joinToString()}")
            Timber.d("Raw probabilities - min: ${probabilities.minOrNull()}, max: ${probabilities.maxOrNull()}, sum: ${probabilities.sum()}")
            
            // Handle NaN and invalid values
            for (i in probabilities.indices) {
                val value = probabilities[i]
                probabilities[i] = when {
                    value.isNaN() -> {
                        Timber.w("NaN detected at index $i")
                        0.0f
                    }
                    value.isInfinite() -> {
                        Timber.w("Infinite value detected at index $i")
                        0.0f
                    }
                    value < 0.0f -> 0.0f
                    value > 1.0f -> 1.0f
                    else -> value
                }
            }
            
            // Apply softmax if output is logits (values outside [0, 1] or sum != 1.0)
            val sum = probabilities.sum()
            if (sum > 0.001f && (probabilities.any { it < 0 || it > 1 } || kotlin.math.abs(sum - 1.0f) > 0.1f)) {
                // Likely logits, apply softmax
                val maxProb = probabilities.maxOrNull() ?: 0f
                val expValues = probabilities.map { kotlin.math.exp((it - maxProb).toDouble()).toFloat() }
                val expSum = expValues.sum()
                if (expSum > 0) {
                    for (i in probabilities.indices) {
                        probabilities[i] = expValues[i] / expSum
                    }
                }
                Timber.d("Applied softmax to convert logits to probabilities")
            }
            
            // Verify probabilities are valid
            val finalSum = probabilities.sum()
            Timber.d("Final probabilities sum: $finalSum, min: ${probabilities.minOrNull()}, max: ${probabilities.maxOrNull()}")
            
            // Get top-k predictions
            val predictions = getTopKPredictions(probabilities, NUM_PREDICTIONS)
            
            // Log top 5 predictions for debugging
            Timber.d("Classification completed:")
            predictions.take(5).forEachIndexed { index, pred ->
                Timber.d("  Top ${index + 1}: ${pred.char} (confidence: ${pred.confidence}, index: ${pred.index})")
            }
            
            // Check if specific characters are in predictions (for debugging)
            val targetChars = listOf("실", "날", "닐", "노")
            targetChars.forEach { char ->
                val index = labels.indexOf(char)
                if (index >= 0 && index < probabilities.size) {
                    val confidence = probabilities[index]
                    val rank = predictions.indexOfFirst { it.char == char } + 1
                    if (rank > 0) {
                        Timber.d("  '$char' found at rank $rank with confidence: $confidence (index: $index)")
                    } else {
                        Timber.w("  '$char' NOT in top ${NUM_PREDICTIONS} predictions! Confidence: $confidence (index: $index)")
                    }
                }
            }
            
            return predictions
        } catch (e: Exception) {
            Timber.e(e, "Error during classification")
            return emptyList()
        }
    }
    
    /**
     * Preprocess bitmap: resize to 64x64 and normalize to [0.0, 1.0]
     * Returns FloatArray and total size for creating ByteBuffer
     */
    private fun preprocessBitmapToFloatArray(bitmap: Bitmap, inputTensor: org.tensorflow.lite.Tensor): Pair<FloatArray, Int> {
        // Resize to 64x64 (exactly like tensorflow project PaintView.getPixelData())
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, FEED_DIMENSION, FEED_DIMENSION, false)
        
        val width = FEED_DIMENSION
        val height = FEED_DIMENSION
        
        // Log original bitmap info before resize
        val originalSample = IntArray(100)
        bitmap.getPixels(originalSample, 0, 10, 0, 0, 10, 10)
        val originalHasData = originalSample.any { (it and 0xFFFFFF) != 0 }
        Timber.d("Original bitmap (128x128) has non-black pixels in top-left: $originalHasData")
        
        // Get pixels from resized bitmap (exactly like tensorflow project)
        val pixels = IntArray(width * height)
        resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Log resized bitmap info
        val resizedHasData = pixels.any { (it and 0xFFFFFF) != 0 }
        val maxPixelValue = pixels.maxOfOrNull { (it and 0xFF) } ?: 0
        Timber.d("Resized bitmap (64x64) has non-black pixels: $resizedHasData, max pixel value: $maxPixelValue")
        
        // Get input tensor shape
        val inputShape = inputTensor.shape()
        val batchSize = inputShape[0]
        val tensorHeight = inputShape[1]
        val tensorWidth = inputShape[2]
        val channels = inputShape[3]
        
        // Create FloatArray first, then convert to ByteBuffer
        // This ensures data is properly written
        val totalSize = batchSize * tensorHeight * tensorWidth * channels
        val floatArray = FloatArray(totalSize)
        
        // Convert to float array and normalize (EXACTLY like tensorflow project)
        // Here we want to convert each pixel to a floating point number between 0.0 and 1.0
        // with 1.0 being white and 0.0 being black.
        // Write in row-major order: [batch][height][width][channel]
        var maxNormalized = 0f
        var nonZeroCount = 0
        var arrayIndex = 0
        for (row in 0 until tensorHeight) {
            for (col in 0 until tensorWidth) {
                val index = row * width + col
                val pix = pixels[index]
                // Extract blue channel (lowest 8 bits) - same as original: int b = pix & 0xff;
                val b = pix and 0xff
                // Normalize to [0.0, 1.0] where 1.0 = white, 0.0 = black
                val normalized = b / 255.0f
                floatArray[arrayIndex++] = normalized
                
                if (normalized > maxNormalized) {
                    maxNormalized = normalized
                }
                if (normalized > 0.01f) { // Threshold to count as non-zero
                    nonZeroCount++
                }
            }
        }
        
        Timber.d("Preprocessing complete: max normalized value = $maxNormalized, non-zero pixels = $nonZeroCount / ${width * height}")
        
        // Find where non-zero values are in the array
        val firstNonZeroIndex = floatArray.indexOfFirst { it > 0.01f }
        val sampleIndices = listOf(0, firstNonZeroIndex, totalSize / 2, totalSize - 10).filter { it >= 0 && it < totalSize }
        val sampleValues = sampleIndices.map { floatArray[it] }
        Timber.d("FloatArray samples at indices $sampleIndices: $sampleValues, max: ${floatArray.maxOrNull()}")
        
        resizedBitmap.recycle()
        
        return Pair(floatArray, totalSize)
    }
    
    /**
     * Preprocess bitmap: resize to 64x64 and normalize to [0.0, 1.0]
     * EXACTLY like PaintView.getPixelData() in tensorflow project
     * 
     * TensorFlow project code:
     * ```java
     * float[] returnPixels = new float[pixels.length];
     * for (int i = 0; i < pixels.length; ++i) {
     *     int pix = pixels[i];
     *     int b = pix & 0xff;
     *     returnPixels[i] = (float) (b/255.0);
     * }
     * ```
     * 
     * Then feed with: tfInterface.feed(inputName, pixels, 1, 64, 64, 1)
     * Which automatically reshapes float[] to [1, 64, 64, 1]
     * 
     * For TFLite, we need ByteBuffer with same data arranged in row-major order
     */
    private fun preprocessBitmapToByteBuffer(bitmap: Bitmap, inputTensor: org.tensorflow.lite.Tensor): ByteBuffer {
        // Resize to 64x64 (exactly like tensorflow project PaintView.getPixelData())
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, FEED_DIMENSION, FEED_DIMENSION, false)
        
        val width = FEED_DIMENSION
        val height = FEED_DIMENSION
        
        // Log original bitmap info before resize
        val originalSample = IntArray(100)
        bitmap.getPixels(originalSample, 0, 10, 0, 0, 10, 10)
        val originalHasData = originalSample.any { (it and 0xFFFFFF) != 0 }
        Timber.d("Original bitmap (128x128) has non-black pixels in top-left: $originalHasData")
        
        // Get pixels from resized bitmap (exactly like tensorflow project)
        val pixels = IntArray(width * height)
        resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Log resized bitmap info
        val resizedHasData = pixels.any { (it and 0xFFFFFF) != 0 }
        val maxPixelValue = pixels.maxOfOrNull { (it and 0xFF) } ?: 0
        Timber.d("Resized bitmap (64x64) has non-black pixels: $resizedHasData, max pixel value: $maxPixelValue")
        
        // Get input tensor shape
        val inputShape = inputTensor.shape()
        val batchSize = inputShape[0]
        val tensorHeight = inputShape[1]
        val tensorWidth = inputShape[2]
        val channels = inputShape[3]
        
        // Create FloatArray first, then convert to ByteBuffer
        // This ensures data is properly written
        val totalSize = batchSize * tensorHeight * tensorWidth * channels
        val floatArray = FloatArray(totalSize)
        
        // Convert to float array and normalize (EXACTLY like tensorflow project)
        // Here we want to convert each pixel to a floating point number between 0.0 and 1.0
        // with 1.0 being white and 0.0 being black.
        // Write in row-major order: [batch][height][width][channel]
        var maxNormalized = 0f
        var nonZeroCount = 0
        var arrayIndex = 0
        for (row in 0 until tensorHeight) {
            for (col in 0 until tensorWidth) {
                val index = row * width + col
                val pix = pixels[index]
                // Extract blue channel (lowest 8 bits) - same as original: int b = pix & 0xff;
                val b = pix and 0xff
                // Normalize to [0.0, 1.0] where 1.0 = white, 0.0 = black
                val normalized = b / 255.0f
                floatArray[arrayIndex++] = normalized
                
                if (normalized > maxNormalized) {
                    maxNormalized = normalized
                }
                if (normalized > 0.01f) { // Threshold to count as non-zero
                    nonZeroCount++
                }
            }
        }
        
        Timber.d("Preprocessing complete: max normalized value = $maxNormalized, non-zero pixels = $nonZeroCount / ${width * height}")
        
        // Find where non-zero values are in the array
        val firstNonZeroIndex = floatArray.indexOfFirst { it > 0.01f }
        val sampleIndices = listOf(0, firstNonZeroIndex, totalSize / 2, totalSize - 10).filter { it >= 0 && it < totalSize }
        val sampleValues = sampleIndices.map { floatArray[it] }
        Timber.d("FloatArray samples at indices $sampleIndices: $sampleValues, max: ${floatArray.maxOrNull()}")
        
        // Convert FloatArray to ByteBuffer - write directly to avoid FloatBuffer view issues
        val inputBuffer = ByteBuffer.allocateDirect(totalSize * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // Write directly to ByteBuffer to ensure data is properly written
        for (value in floatArray) {
            inputBuffer.putFloat(value)
        }
        
        // Rewind ByteBuffer to position 0
        inputBuffer.rewind()
        
        resizedBitmap.recycle()
        
        // Verify buffer was written correctly by reading back values at same indices
        val verifyValues = mutableListOf<Float>()
        val originalPosition = inputBuffer.position()
        for (idx in sampleIndices) {
            inputBuffer.position(idx * 4)
            verifyValues.add(inputBuffer.float)
        }
        // CRITICAL: Always rewind to position 0 after verification, don't restore original position
        inputBuffer.rewind()
        Timber.d("Verification: ByteBuffer values at indices $sampleIndices: $verifyValues")
        
        // Ensure buffer is at position 0 and limit is set to full capacity
        // Don't set limit to totalSize * 4, keep it at capacity to allow full read
        inputBuffer.rewind()
        inputBuffer.limit(inputBuffer.capacity()) // Set limit to full capacity
        Timber.d("Buffer position: ${inputBuffer.position()}, capacity: ${inputBuffer.capacity()}, limit: ${inputBuffer.limit()}")
        
        return inputBuffer
    }
    
    /**
     * Get top-k predictions sorted by confidence
     */
    private fun getTopKPredictions(probabilities: FloatArray, k: Int): List<Prediction> {
        // Create list of (index, probability) pairs
        val indexed = probabilities.mapIndexed { index, prob -> index to prob }
        
        // Sort by probability (descending) and take top k
        val topK = indexed.sortedByDescending { it.second }.take(k)
        
        // Convert to Prediction objects
        return topK.map { (index, confidence) ->
            val char = if (index < labels.size) {
                labels[index]
            } else {
                "?"
            }
            Prediction(
                char = char,
                confidence = confidence.coerceIn(0f, 1f),
                index = index
            )
        }
    }
    
    /**
     * Clean up resources
     */
    fun close() {
        interpreter.close()
    }
}

