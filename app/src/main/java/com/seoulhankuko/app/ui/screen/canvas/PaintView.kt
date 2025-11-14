package com.seoulhankuko.app.ui.screen.canvas

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.Matrix as AndroidMatrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.Matrix as ComposeMatrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import timber.log.Timber

/**
 * PaintView composable - Similar to PaintView.java from tensorflow project
 * 
 * Features:
 * - 128x128 bitmap for display (black background, white strokes)
 * - Transform matrix to scale and center bitmap in view
 * - Touch handling to draw paths
 * - Method to get pixel data for model inference
 */
@Composable
fun PaintView(
    modifier: Modifier = Modifier,
    onBitmapChanged: (Bitmap?) -> Unit = {}
) {
    val bitmapDimension = 128
    val feedDimension = 64
    
    // State for bitmap and paths
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var paths by remember { mutableStateOf<List<Path>>(emptyList()) }
    var transformMatrix by remember { mutableStateOf<AndroidMatrix?>(null) }
    var inverseMatrix by remember { mutableStateOf<AndroidMatrix?>(null) }
    var scaleFactor by remember { mutableStateOf(1f) }
    var translateX by remember { mutableStateOf(0f) }
    var translateY by remember { mutableStateOf(0f) }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    
    // Initialize bitmap
    LaunchedEffect(Unit) {
        bitmap = Bitmap.createBitmap(bitmapDimension, bitmapDimension, Bitmap.Config.ARGB_8888)
        bitmap?.let {
            val canvas = AndroidCanvas(it)
            canvas.drawColor(Color.BLACK)
            onBitmapChanged(it)
        }
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            bitmap?.recycle()
        }
    }
    
    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(ComposeColor.Black)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            // Setup matrices
                            setupMatrices(
                                size = size,
                                bitmapDimension = bitmapDimension
                            ) { transform, inverse, scale, tx, ty ->
                                transformMatrix = transform
                                inverseMatrix = inverse
                                scaleFactor = scale
                                translateX = tx
                                translateY = ty
                            }
                            
                            // Convert screen coordinates to bitmap coordinates
                            val bitmapCoords = inverseMatrix?.let { inv ->
                                transformPoint(offset, inv)
                            } ?: offset
                            
                            // Create new path
                            val newPath = Path().apply {
                                moveTo(bitmapCoords.x, bitmapCoords.y)
                                lineTo(bitmapCoords.x, bitmapCoords.y)
                            }
                            currentPath = newPath
                            paths = paths + newPath
                            
                            // Draw on bitmap
                            bitmap?.let { bmp ->
                                val canvas = AndroidCanvas(bmp)
                                val paint = Paint().apply {
                                    color = Color.WHITE
                                    style = Paint.Style.STROKE
                                    strokeWidth = 6f
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                                canvas.drawPath(newPath, paint)
                                onBitmapChanged(bmp)
                            }
                        },
                        onDrag = { change, _ ->
                            val bitmapCoords = inverseMatrix?.let { inv ->
                                transformPoint(change.position, inv)
                            } ?: change.position
                            
                            currentPath?.lineTo(bitmapCoords.x, bitmapCoords.y)
                            
                            // Draw on bitmap
                            bitmap?.let { bmp ->
                                val canvas = AndroidCanvas(bmp)
                                val paint = Paint().apply {
                                    color = Color.WHITE
                                    style = Paint.Style.STROKE
                                    strokeWidth = 6f
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                                currentPath?.let { path ->
                                    canvas.drawPath(path, paint)
                                    onBitmapChanged(bmp)
                                }
                            }
                        },
                        onDragEnd = {
                            currentPath = null
                        }
                    )
                }
        ) {
            // Draw bitmap with transform
            bitmap?.let { bmp ->
                withTransform({
                    scale(scaleFactor, scaleFactor)
                    translate(translateX, translateY)
                }) {
                    drawImage(
                        image = bmp.asImageBitmap(),
                        topLeft = Offset.Zero
                    )
                }
            }
        }
    }
}

/**
 * Setup transform matrices to scale and center bitmap in view
 */
private fun setupMatrices(
    size: IntSize,
    bitmapDimension: Int,
    onMatricesReady: (AndroidMatrix, AndroidMatrix, Float, Float, Float) -> Unit
) {
    val width = size.width
    val height = size.height
    val scaleW = width / bitmapDimension.toFloat()
    val scaleH = height / bitmapDimension.toFloat()
    
    val scale = minOf(scaleW, scaleH)
    
    // Translation to center bitmap in view after scaling
    val centerX = bitmapDimension * scale / 2f
    val centerY = bitmapDimension * scale / 2f
    val dx = width / 2f - centerX
    val dy = height / 2f - centerY
    
    val transform = AndroidMatrix().apply {
        setScale(scale, scale)
        postTranslate(dx, dy)
    }
    
    val inverse = AndroidMatrix().apply {
        set(transform)
        invert(this)
    }
    
    onMatricesReady(transform, inverse, scale, dx, dy)
}

/**
 * Transform point using inverse matrix
 */
private fun transformPoint(point: Offset, matrix: AndroidMatrix): Offset {
    val points = floatArrayOf(point.x, point.y)
    matrix.mapPoints(points)
    return Offset(points[0], points[1])
}

/**
 * Get pixel data from bitmap for model inference
 * Returns float array normalized to [0.0, 1.0] where 1.0 = white, 0.0 = black
 */
fun getPixelData(bitmap: Bitmap?): FloatArray? {
    if (bitmap == null) return null
    
    // Resize to 64x64
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, false)
    
    val width = 64
    val height = 64
    val pixels = IntArray(width * height)
    resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
    
    val returnPixels = FloatArray(pixels.size)
    
    // Convert each pixel to float [0.0, 1.0] where 1.0 = white, 0.0 = black
    for (i in pixels.indices) {
        val pix = pixels[i]
        val b = pix and 0xff
        returnPixels[i] = b / 255.0f
    }
    
    resizedBitmap.recycle()
    
    return returnPixels
}

/**
 * Reset paint view - clear all drawings
 */
fun resetPaintView(bitmap: Bitmap?): Bitmap? {
    bitmap?.let {
        val canvas = AndroidCanvas(it)
        canvas.drawColor(Color.BLACK)
    }
    return bitmap
}
