package com.seoulhankuko.app.domain.model

/**
 * Domain model representing a group of Hangul characters
 */
data class HangulGroup(
    val title: String,
    val items: List<HangulChar>
)

