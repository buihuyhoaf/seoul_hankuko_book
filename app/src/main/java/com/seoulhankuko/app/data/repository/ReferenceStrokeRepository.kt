package com.seoulhankuko.app.data.repository

import android.content.Context
import androidx.compose.ui.geometry.Offset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.seoulhankuko.app.domain.model.ReferencePattern
import com.seoulhankuko.app.domain.model.ReferenceStroke
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON data structure for stroke files
 */
private data class StrokePointJson(
    val x: Float,
    val y: Float
)

private data class StrokeJson(
    val strokeIndex: Int,
    val points: List<StrokePointJson>
)

private data class CharacterStrokesJson(
    val character: String,
    val strokes: List<StrokeJson>
)

/**
 * Repository for loading reference stroke patterns from JSON files
 * TODO: Implement Room persistence for offline access
 */
@Singleton
class ReferenceStrokeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    
    private val cache = mutableMapOf<String, ReferencePattern>()
    
    /**
     * Load reference pattern for a character from JSON file
     * 
     * @param character Hangul character (e.g., "ㅏ", "가")
     * @return ReferencePattern or null if not found
     */
    suspend fun getReferencePattern(character: String): ReferencePattern? {
        // Check cache first
        cache[character]?.let { return it }
        
        return withContext(Dispatchers.IO) {
            try {
                // Map character to filename (e.g., "ㅏ" -> "a_stroke.json")
                val filename = getFilenameForCharacter(character)
                
                // Load JSON from assets
                val inputStream: InputStream = context.assets.open("strokes/$filename")
                    ?: return@withContext null
                
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                
                // Parse JSON
                val typeToken = object : TypeToken<CharacterStrokesJson>() {}
                val data: CharacterStrokesJson = gson.fromJson(jsonString, typeToken.type)
                
                // Convert to domain model
                val referenceStrokes = data.strokes.map { strokeJson ->
                    ReferenceStroke(
                        character = data.character,
                        strokeIndex = strokeJson.strokeIndex,
                        points = strokeJson.points.map { pointJson ->
                            Offset(pointJson.x, pointJson.y)
                        }
                    )
                }
                
                val pattern = ReferencePattern(
                    character = data.character,
                    strokes = referenceStrokes.sortedBy { it.strokeIndex }
                )
                
                // Cache the pattern
                cache[character] = pattern
                
                pattern
            } catch (e: Exception) {
                // Return mock pattern if file not found
                createMockPattern(character)
            }
        }
    }
    
    /**
     * Map character to JSON filename
     */
    private fun getFilenameForCharacter(character: String): String {
        // Simple mapping - can be extended
        return when (character) {
            "ㅏ" -> "a_stroke.json"
            "가" -> "ga_stroke.json"
            "ㄱ" -> "giyeok_stroke.json"
            else -> "${character}_stroke.json"
        }
    }
    
    /**
     * Create mock pattern if JSON file not found
     * This allows development without all stroke files
     */
    private fun createMockPattern(character: String): ReferencePattern {
        // Create a simple mock pattern
        val mockStroke = ReferenceStroke(
            character = character,
            strokeIndex = 0,
            points = listOf(
                Offset(0.3f, 0.2f),
                Offset(0.3f, 0.8f),
                Offset(0.7f, 0.8f)
            )
        )
        
        val pattern = ReferencePattern(
            character = character,
            strokes = listOf(mockStroke)
        )
        
        cache[character] = pattern
        return pattern
    }
    
    /**
     * Clear cache (useful for testing)
     */
    fun clearCache() {
        cache.clear()
    }
}

