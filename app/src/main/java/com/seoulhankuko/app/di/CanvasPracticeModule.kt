package com.seoulhankuko.app.di

import com.seoulhankuko.app.domain.usecase.StrokeAutoCorrector
import com.seoulhankuko.app.domain.usecase.StrokeComparator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Canvas Practice dependencies
 * Note: Gson is provided by NetworkModule
 */
@Module
@InstallIn(SingletonComponent::class)
object CanvasPracticeModule {
    
    /**
     * Provide StrokeComparator instance
     */
    @Provides
    @Singleton
    fun provideStrokeComparator(): StrokeComparator {
        return StrokeComparator()
    }
    
    /**
     * Provide StrokeAutoCorrector instance
     */
    @Provides
    @Singleton
    fun provideStrokeAutoCorrector(): StrokeAutoCorrector {
        return StrokeAutoCorrector()
    }
}

