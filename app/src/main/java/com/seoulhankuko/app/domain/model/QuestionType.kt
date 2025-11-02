package com.seoulhankuko.app.domain.model

enum class QuestionType {
    MULTIPLE_CHOICE,        // ID 1: Select the correct answer from given options with audio support
    BLANK,                  // ID 2: Fill in the blank (FILL_IN_BLANK renamed to BLANK)
    MATCHING,               // ID 3: Match Korean words with their English translations or definitions
    AUDIO_COMPREHENSION,    // ID 4: Listen to Korean audio and answer questions about the content
    PRONUNCIATION,          // ID 5: Practice and verify correct Korean pronunciation
    SENTENCE_ORDER,         // ID 6: Reorder words/sentences to form correct sentence
    IMAGE_SELECTION         // ID 7: Select the correct image based on description or word given
}

