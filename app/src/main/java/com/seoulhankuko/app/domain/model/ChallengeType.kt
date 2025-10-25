package com.seoulhankuko.app.domain.model

enum class ChallengeType {
    MULTIPLE_CHOICE,    // ID 1: Select the correct answer from given options with audio support
    FILL_IN_BLANK,      // ID 2: Complete the sentence with the appropriate Korean word or phrase
    TRUE_FALSE,         // ID 3: Determine whether the Korean statement is correct or incorrect
    AUDIO_COMPREHENSION, // ID 4: Listen to Korean audio and answer questions about the content
    WRITING_PRACTICE,   // ID 5: Compose Korean sentences or short texts based on prompts
    READING_COMPREHENSION, // ID 6: Read Korean passages and answer questions about the content
    MATCHING,           // ID 7: Match Korean words with their English translations or definitions
    PRONUNCIATION       // ID 8: Practice and verify correct Korean pronunciation
}

