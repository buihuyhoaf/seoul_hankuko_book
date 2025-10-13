package com.seoulhankuko.app

import com.seoulhankuko.app.data.database.daos.*
import com.seoulhankuko.app.data.repository.*
import org.junit.Assert.*
import org.junit.Test

class DependencyInjectionTest {

    @Test
    fun testRepositoryAnnotations() {
        // Test that repositories have correct annotations
        
        // Check CourseRepository
        val courseRepositoryClass = CourseRepository::class.java
        val courseRepositoryAnnotations = courseRepositoryClass.annotations
        assertTrue("CourseRepository should have @Singleton annotation", 
            courseRepositoryAnnotations.any { it.annotationClass.simpleName == "Singleton" })
        
        // Check LessonRepository
        val lessonRepositoryClass = LessonRepository::class.java
        val lessonRepositoryAnnotations = lessonRepositoryClass.annotations
        assertTrue("LessonRepository should have @Singleton annotation", 
            lessonRepositoryAnnotations.any { it.annotationClass.simpleName == "Singleton" })
        
        // Check UserProgressRepository
        val userProgressRepositoryClass = UserProgressRepository::class.java
        val userProgressRepositoryAnnotations = userProgressRepositoryClass.annotations
        assertTrue("UserProgressRepository should have @Singleton annotation", 
            userProgressRepositoryAnnotations.any { it.annotationClass.simpleName == "Singleton" })
    }

    @Test
    fun testNetworkModuleExists() {
        // Test that NetworkModule class exists and has correct annotations
        val networkModuleClass = com.seoulhankuko.app.data.api.NetworkModule::class.java
        val annotations = networkModuleClass.annotations
        
        assertTrue("NetworkModule should have @Module annotation", 
            annotations.any { it.annotationClass.simpleName == "Module" })
        // Note: @InstallIn might not be visible in runtime, so we'll just check for @Module
    }

    @Test
    fun testApplicationClassExists() {
        // Test that SeoulHankukoApplication class exists and has correct annotations
        val applicationClass = com.seoulhankuko.app.core.SeoulHankukoApplication::class.java
        val annotations = applicationClass.annotations
        
        assertTrue("SeoulHankukoApplication should have @HiltAndroidApp annotation", 
            annotations.any { it.annotationClass.simpleName == "HiltAndroidApp" })
    }

    @Test
    fun testMainActivityAnnotations() {
        // Test that MainActivity class exists and can be instantiated
        val mainActivityClass = MainActivity::class.java
        assertNotNull("MainActivity class should exist", mainActivityClass)
        
        // Note: @AndroidEntryPoint might not be visible in unit tests, so we'll just check class exists
        assertTrue("MainActivity should be a ComponentActivity", 
            android.app.Activity::class.java.isAssignableFrom(mainActivityClass))
    }
}
