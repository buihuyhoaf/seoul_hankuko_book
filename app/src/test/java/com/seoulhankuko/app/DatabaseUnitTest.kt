package com.seoulhankuko.app

import com.seoulhankuko.app.data.database.entities.*
import com.seoulhankuko.app.domain.model.ChallengeType
import org.junit.Assert.*
import org.junit.Test

class DatabaseUnitTest {

    @Test
    fun testCourseEntity() {
        val course = Course(
            id = 1,
            title = "Korean Basics",
            imageSrc = "korean_basics.png"
        )
        
        assertEquals(1, course.id)
        assertEquals("Korean Basics", course.title)
        assertEquals("korean_basics.png", course.imageSrc)
    }

    @Test
    fun testUnitEntity() {
        val unit = UnitEntity(
            id = 1,
            title = "Unit 1: Greetings",
            description = "Learn basic greetings in Korean",
            courseId = 1,
            order = 1
        )
        
        assertEquals(1, unit.id)
        assertEquals("Unit 1: Greetings", unit.title)
        assertEquals("Learn basic greetings in Korean", unit.description)
        assertEquals(1, unit.courseId)
        assertEquals(1, unit.order)
    }

    @Test
    fun testLessonEntity() {
        val lesson = Lesson(
            id = 1,
            title = "Lesson 1: Hello",
            unitId = 1,
            order = 1
        )
        
        assertEquals(1, lesson.id)
        assertEquals("Lesson 1: Hello", lesson.title)
        assertEquals(1, lesson.unitId)
        assertEquals(1, lesson.order)
    }

    @Test
    fun testChallengeEntity() {
        val challenge = Challenge(
            id = 1,
            lessonId = 1,
            type = ChallengeType.SELECT,
            question = "What does 안녕하세요 mean?",
            order = 1
        )
        
        assertEquals(1, challenge.id)
        assertEquals(1, challenge.lessonId)
        assertEquals(ChallengeType.SELECT, challenge.type)
        assertEquals("What does 안녕하세요 mean?", challenge.question)
        assertEquals(1, challenge.order)
    }

    @Test
    fun testChallengeOptionEntity() {
        val option = ChallengeOption(
            id = 1,
            challengeId = 1,
            text = "Hello",
            correct = true,
            imageSrc = "hello.png",
            audioSrc = "hello.mp3"
        )
        
        assertEquals(1, option.id)
        assertEquals(1, option.challengeId)
        assertEquals("Hello", option.text)
        assertTrue(option.correct)
        assertEquals("hello.png", option.imageSrc)
        assertEquals("hello.mp3", option.audioSrc)
    }

    @Test
    fun testUserProgressEntity() {
        val userProgress = UserProgress(
            userId = "test_user",
            userName = "Test User",
            userImageSrc = "/test_avatar.png",
            activeCourseId = 1,
            hearts = 5,
            points = 100
        )
        
        assertEquals("test_user", userProgress.userId)
        assertEquals("Test User", userProgress.userName)
        assertEquals("/test_avatar.png", userProgress.userImageSrc)
        assertEquals(1, userProgress.activeCourseId)
        assertEquals(5, userProgress.hearts)
        assertEquals(100, userProgress.points)
    }

    @Test
    fun testChallengeProgressEntity() {
        val challengeProgress = ChallengeProgress(
            id = 1,
            userId = "test_user",
            challengeId = 1,
            completed = true
        )
        
        assertEquals(1, challengeProgress.id)
        assertEquals("test_user", challengeProgress.userId)
        assertEquals(1, challengeProgress.challengeId)
        assertTrue(challengeProgress.completed)
    }

    @Test
    fun testUserSubscriptionEntity() {
        val subscription = UserSubscription(
            id = 1,
            userId = "test_user",
            stripeCustomerId = "cus_123",
            stripeSubscriptionId = "sub_123",
            stripePriceId = "price_123",
            stripeCurrentPeriodEnd = 1234567890L
        )
        
        assertEquals(1, subscription.id)
        assertEquals("test_user", subscription.userId)
        assertEquals("cus_123", subscription.stripeCustomerId)
        assertEquals("sub_123", subscription.stripeSubscriptionId)
        assertEquals("price_123", subscription.stripePriceId)
        assertEquals(1234567890L, subscription.stripeCurrentPeriodEnd)
    }

    @Test
    fun testChallengeTypeEnum() {
        assertEquals(ChallengeType.SELECT, ChallengeType.valueOf("SELECT"))
        assertEquals(ChallengeType.ASSIST, ChallengeType.valueOf("ASSIST"))
        
        assertEquals("SELECT", ChallengeType.SELECT.name)
        assertEquals("ASSIST", ChallengeType.ASSIST.name)
    }
}


