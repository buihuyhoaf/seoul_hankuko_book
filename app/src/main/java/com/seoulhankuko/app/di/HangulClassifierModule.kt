package com.seoulhankuko.app.di

import android.content.Context
import com.seoulhankuko.app.ml.HangulTFLiteClassifier
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HangulClassifierModule {
    
    /**
     * Provides HangulTFLiteClassifier instance
     * 
     * Strategy:
     * - Try to load from Supabase (downloads and caches locally)
     * - Falls back to assets if Supabase download fails
     * 
     * To use assets only, change USE_SUPABASE to false
     */
    @Provides
    @Singleton
    fun provideHangulClassifier(
        @ApplicationContext context: Context
    ): HangulTFLiteClassifier {
        val USE_SUPABASE = true // Set to false to use assets only
        
        return if (USE_SUPABASE) {
            try {
                Timber.d("Attempting to load model from Supabase...")
                runBlocking {
                    HangulTFLiteClassifier.createWithSupabase(context)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load from Supabase, using assets fallback")
                HangulTFLiteClassifier.create(context)
            }
        } else {
            Timber.d("Loading model from assets...")
            HangulTFLiteClassifier.create(context)
        }
    }
}

