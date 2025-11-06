package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.domain.model.HangulChar
import com.seoulhankuko.app.domain.model.HangulGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Hangul alphabet data
 * Provides mock data for Hangul characters grouped by type
 */
@Singleton
class HangulRepository @Inject constructor() {

    /**
     * Get all Hangul groups as Flow
     */
    fun getHangulGroups(): Flow<List<HangulGroup>> {
        return flowOf(provideMockHangulGroups())
    }

    /**
     * Provides mock data for Hangul characters
     * Groups: Basic Vowels, Compound Vowels, Basic Consonants, Aspirated Consonants, Tense Consonants
     */
    private fun provideMockHangulGroups(): List<HangulGroup> {
        return listOf(
            // Basic Vowels
            HangulGroup(
                title = "Nguyên âm cơ bản",
                items = listOf(
                    HangulChar("ㅏ", "a"),
                    HangulChar("ㅑ", "ya"),
                    HangulChar("ㅓ", "eo"),
                    HangulChar("ㅕ", "yeo"),
                    HangulChar("ㅗ", "o"),
                    HangulChar("ㅛ", "yo"),
                    HangulChar("ㅜ", "u"),
                    HangulChar("ㅠ", "yu"),
                    HangulChar("ㅡ", "eu"),
                    HangulChar("ㅣ", "i")
                )
            ),
            // Compound Vowels
            HangulGroup(
                title = "Nguyên âm đôi",
                items = listOf(
                    HangulChar("ㅐ", "ae"),
                    HangulChar("ㅒ", "yae"),
                    HangulChar("ㅔ", "e"),
                    HangulChar("ㅖ", "ye"),
                    HangulChar("ㅘ", "wa"),
                    HangulChar("ㅙ", "wae"),
                    HangulChar("ㅚ", "oe"),
                    HangulChar("ㅝ", "wo"),
                    HangulChar("ㅞ", "we"),
                    HangulChar("ㅟ", "wi"),
                    HangulChar("ㅢ", "ui")
                )
            ),
            // Basic Consonants
            HangulGroup(
                title = "Phụ âm cơ bản",
                items = listOf(
                    HangulChar("ㄱ", "g/k"),
                    HangulChar("ㄴ", "n"),
                    HangulChar("ㄷ", "d/t"),
                    HangulChar("ㄹ", "r/l"),
                    HangulChar("ㅁ", "m"),
                    HangulChar("ㅂ", "b/p"),
                    HangulChar("ㅅ", "s"),
                    HangulChar("ㅇ", "ng"),
                    HangulChar("ㅈ", "j"),
                    HangulChar("ㅎ", "h")
                )
            ),
            // Aspirated Consonants
            HangulGroup(
                title = "Phụ âm bật hơi",
                items = listOf(
                    HangulChar("ㅊ", "ch"),
                    HangulChar("ㅋ", "k"),
                    HangulChar("ㅌ", "t"),
                    HangulChar("ㅍ", "p")
                )
            ),
            // Tense Consonants
            HangulGroup(
                title = "Phụ âm căng",
                items = listOf(
                    HangulChar("ㄲ", "kk"),
                    HangulChar("ㄸ", "tt"),
                    HangulChar("ㅃ", "pp"),
                    HangulChar("ㅆ", "ss"),
                    HangulChar("ㅉ", "jj")
                )
            )
        )
    }
}

