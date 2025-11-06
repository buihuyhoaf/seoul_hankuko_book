package com.seoulhankuko.app.di

import com.seoulhankuko.app.data.repository.MockStrokePatternRepository
import com.seoulhankuko.app.domain.usecase.StrokePatternRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Canvas-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CanvasModule {
    
    /**
     * Binds MockStrokePatternRepository to StrokePatternRepository interface
     * TODO: Replace with Room-based implementation when persistence is added
     */
    @Binds
    @Singleton
    abstract fun bindStrokePatternRepository(
        mockRepository: MockStrokePatternRepository
    ): StrokePatternRepository
}

