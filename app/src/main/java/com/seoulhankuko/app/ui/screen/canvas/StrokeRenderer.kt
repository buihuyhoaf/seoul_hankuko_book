package com.seoulhankuko.app.ui.screen.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.seoulhankuko.app.domain.model.Stroke as StrokeModel
import com.seoulhankuko.app.domain.model.StrokePoint

/**
 * Renderer for drawing strokes on canvas
 */
@Composable
fun StrokeRenderer(
    completedStrokes: List<StrokeModel>,
    currentStroke: List<StrokePoint>,
    templateStrokes: List<StrokeModel>?,
    showTemplate: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Draw template strokes (if enabled)
        // Template strokes use normalized coordinates (0-1), need to scale to canvas
        if (showTemplate && templateStrokes != null) {
            templateStrokes.forEach { stroke ->
                drawTemplateStroke(
                    stroke = stroke,
                    canvasWidth = canvasWidth,
                    canvasHeight = canvasHeight,
                    color = Color.LightGray.copy(alpha = 0.3f),
                    strokeWidth = 4f
                )
            }
        }
        
        // Draw completed user strokes
        completedStrokes.forEach { stroke ->
            drawStroke(
                stroke = stroke,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                color = Color.Blue,
                strokeWidth = 6f
            )
        }
        
        // Draw current stroke being drawn
        if (currentStroke.isNotEmpty()) {
            drawStroke(
                points = currentStroke,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                color = Color.Blue,
                strokeWidth = 6f
            )
        }
    }
}

/**
 * Draw a stroke on canvas
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    stroke: StrokeModel,
    canvasWidth: Float,
    canvasHeight: Float,
    color: Color,
    strokeWidth: Float
) {
    drawStroke(
        points = stroke.points,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        color = color,
        strokeWidth = strokeWidth
    )
}

/**
 * Draw points as a path on canvas
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStroke(
    points: List<StrokePoint>,
    canvasWidth: Float,
    canvasHeight: Float,
    color: Color,
    strokeWidth: Float
) {
    if (points.isEmpty()) return
    
    val path = Path()
    val firstPoint = points[0]
    // Points are already in canvas coordinates, just clamp to bounds
    val x0 = firstPoint.x.coerceIn(0f, canvasWidth)
    val y0 = firstPoint.y.coerceIn(0f, canvasHeight)
    
    path.moveTo(x0, y0)
    
    // Draw lines between consecutive points
    for (i in 1 until points.size) {
        val point = points[i]
        val x = point.x.coerceIn(0f, canvasWidth)
        val y = point.y.coerceIn(0f, canvasHeight)
        path.lineTo(x, y)
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
}

/**
 * Draw template stroke with normalized coordinates (0-1)
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTemplateStroke(
    stroke: StrokeModel,
    canvasWidth: Float,
    canvasHeight: Float,
    color: Color,
    strokeWidth: Float
) {
    if (stroke.points.isEmpty()) return
    
    val path = Path()
    val firstPoint = stroke.points[0]
    // Template uses normalized coordinates (0-1), scale to canvas
    val x0 = (firstPoint.x * canvasWidth).coerceIn(0f, canvasWidth)
    val y0 = (firstPoint.y * canvasHeight).coerceIn(0f, canvasHeight)
    
    path.moveTo(x0, y0)
    
    for (i in 1 until stroke.points.size) {
        val point = stroke.points[i]
        val x = (point.x * canvasWidth).coerceIn(0f, canvasWidth)
        val y = (point.y * canvasHeight).coerceIn(0f, canvasHeight)
        path.lineTo(x, y)
    }
    
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    )
}

