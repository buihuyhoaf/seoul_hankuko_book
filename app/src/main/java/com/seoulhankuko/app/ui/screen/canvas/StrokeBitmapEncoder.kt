package com.seoulhankuko.app.ui.screen.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import com.seoulhankuko.app.domain.model.Stroke
import java.io.ByteArrayOutputStream
import kotlin.math.max

object StrokeBitmapEncoder {

    private const val BITMAP_DIMENSION = 128  // Display size (like tensorflow)
    private const val FEED_DIMENSION = 64     // Model input size
    private const val PADDING = 6f
    private const val STROKE_WIDTH = 6f

    /**
     * Create bitmap from strokes (128x128 for display, can be resized to 64x64 for model)
     */
    fun toBitmap(strokes: List<Stroke>): Bitmap? {
        if (strokes.isEmpty()) return null

        val points = strokes.flatMap { it.points }
        if (points.isEmpty()) return null

        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxX = points.maxOf { it.x }
        val maxY = points.maxOf { it.y }

        val width = maxX - minX
        val height = maxY - minY

        val drawableSize = (BITMAP_DIMENSION - PADDING * 2).coerceAtLeast(BITMAP_DIMENSION * 0.6f)
        val scale = if (width == 0f && height == 0f) {
            1f
        } else {
            drawableSize / max(width.coerceAtLeast(1f), height.coerceAtLeast(1f))
        }

        val bitmap = Bitmap.createBitmap(BITMAP_DIMENSION, BITMAP_DIMENSION, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = STROKE_WIDTH
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEach { stroke ->
            val path = Path()
            stroke.points.forEachIndexed { index, point ->
                val normalizedX = (point.x - minX) * scale + PADDING
                val normalizedY = (point.y - minY) * scale + PADDING
                if (index == 0) {
                    path.moveTo(normalizedX, normalizedY)
                } else {
                    path.lineTo(normalizedX, normalizedY)
                }
            }

            if (!path.isEmpty) {
                canvas.drawPath(path, paint)
            }
        }

        return bitmap
    }

    fun toBase64(strokes: List<Stroke>): String? {
        if (strokes.isEmpty()) return null

        val points = strokes.flatMap { it.points }
        if (points.isEmpty()) return null

        val minX = points.minOf { it.x }
        val minY = points.minOf { it.y }
        val maxX = points.maxOf { it.x }
        val maxY = points.maxOf { it.y }

        val width = maxX - minX
        val height = maxY - minY

        val drawableSize = (FEED_DIMENSION - PADDING * 2).coerceAtLeast(FEED_DIMENSION * 0.6f)
        val scale = if (width == 0f && height == 0f) {
            1f
        } else {
            drawableSize / max(width.coerceAtLeast(1f), height.coerceAtLeast(1f))
        }

        val bitmap = Bitmap.createBitmap(FEED_DIMENSION, FEED_DIMENSION, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = STROKE_WIDTH
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEach { stroke ->
            val path = Path()
            stroke.points.forEachIndexed { index, point ->
                val normalizedX = (point.x - minX) * scale + PADDING
                val normalizedY = (point.y - minY) * scale + PADDING
                if (index == 0) {
                    path.moveTo(normalizedX, normalizedY)
                } else {
                    path.lineTo(normalizedX, normalizedY)
                }
            }

            if (!path.isEmpty) {
                canvas.drawPath(path, paint)
            }
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        bitmap.recycle()

        val encodedBytes = outputStream.toByteArray()
        if (encodedBytes.isEmpty()) return null

        return Base64.encodeToString(encodedBytes, Base64.NO_WRAP)
    }
}
