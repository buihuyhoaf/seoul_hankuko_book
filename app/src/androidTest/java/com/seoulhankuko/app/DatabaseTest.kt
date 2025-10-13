package com.seoulhankuko.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.seoulhankuko.app.data.database.AppDatabase
import com.seoulhankuko.app.data.database.daos.*
import com.seoulhankuko.app.data.database.entities.*
import com.seoulhankuko.app.domain.model.ChallengeType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var database: AppDatabase
    private lateinit var courseDao: CourseDao
    private lateinit var unitDao: UnitDao
    private lateinit var lessonDao: LessonDao
    private lateinit var challengeDao: ChallengeDao
    private lateinit var challengeOptionDao: ChallengeOptionDao
    private lateinit var userProgressDao: UserProgressDao
    private lateinit var challengeProgressDao: ChallengeProgressDao

    @Before
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        courseDao = database.courseDao()
        unitDao = database.unitDao()
        lessonDao = database.lessonDao()
        challengeDao = database.challengeDao()
        challengeOptionDao = database.challengeOptionDao()
        userProgressDao = database.userProgressDao()
        challengeProgressDao = database.challengeProgressDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testDatabaseCreation() = runBlocking {
        // Test that database is created successfully
        assertNotNull(database)
        assertNotNull(courseDao)
        assertNotNull(unitDao)
        assertNotNull(lessonDao)
        assertNotNull(challengeDao)
        assertNotNull(challengeOptionDao)
        assertNotNull(userProgressDao)
        assertNotNull(challengeProgressDao)
    }

    @Test
    fun testCourseOperations() = runBlocking {
        // Test Course CRUD operations
        val course = Course(
            id = 1,
            title = "Korean Basics",
            imageSrc = "korean_basics.png"
        )

        // Insert
        courseDao.insertCourse(course)
        
        // Read
        val retrievedCourse = courseDao.getCourseById(1)
        assertNotNull(retrievedCourse)
        assertEquals("Korean Basics", retrievedCourse?.title)
        assertEquals("korean_basics.png", retrievedCourse?.imageSrc)

        // Test Flow
        val coursesFlow = courseDao.getAllCourses()
        val courses = coursesFlow.first()
        assertEquals(1, courses.size)
        assertEquals("Korean Basics", courses[0].title)
    }

    @Test
    fun testUnitOperations() = runBlocking {
        // First create a course
        val course = Course(
            id = 1,
            title = "Korean Basics",
            imageSrc = "korean_basics.png"
        )
        courseDao.insertCourse(course)

        // Test Unit CRUD operations
        val unit = UnitEntity(
            id = 1,
            title = "Unit 1: Greetings",
            description = "Learn basic greetings in Korean",
            courseId = 1,
            order = 1
        )

        // Insert
        unitDao.insertUnit(unit)

        // Read
        val retrievedUnit = unitDao.getUnitById(1)
        assertNotNull(retrievedUnit)
        assertEquals("Unit 1: Greetings", retrievedUnit?.title)
        assertEquals(1, retrievedUnit?.courseId)

        // Test Flow with course relationship
        val unitsFlow = unitDao.getUnitsByCourseId(1)
        val units = unitsFlow.first()
        assertEquals(1, units.size)
        assertEquals("Unit 1: Greetings", units[0].title)
    }

    @Test
    fun testUserProgressOperations() = runBlocking {
        // Test UserProgress CRUD operations
        val userProgress = UserProgress(
            userId = "test_user",
            userName = "Test User",
            userImageSrc = "/test_avatar.png",
            activeCourseId = 1,
            hearts = 5,
            points = 100
        )

        // Insert
        userProgressDao.insertUserProgress(userProgress)

        // Read
        val retrievedProgress = userProgressDao.getUserProgress("test_user").first()
        assertNotNull(retrievedProgress)
        assertEquals("Test User", retrievedProgress?.userName)
        assertEquals(5, retrievedProgress?.hearts)
        assertEquals(100, retrievedProgress?.points)

        // Update hearts
        userProgressDao.updateHearts("test_user", 3)
        val updatedProgress = userProgressDao.getUserProgress("test_user").first()
        assertEquals(3, updatedProgress?.hearts)

        // Update points
        userProgressDao.updatePoints("test_user", 150)
        val updatedPoints = userProgressDao.getUserProgress("test_user").first()
        assertEquals(150, updatedPoints?.points)
    }

    @Test
    fun testForeignKeysAndCascadeDelete() = runBlocking {
        // Test that foreign keys work and cascade delete functions properly
        
        // Create course
        val course = Course(id = 1, title = "Korean Basics", imageSrc = "korean_basics.png")
        courseDao.insertCourse(course)

        // Create unit linked to course
        val unit = UnitEntity(id = 1, title = "Unit 1", description = "Unit 1", courseId = 1, order = 1)
        unitDao.insertUnit(unit)

        // Create lesson linked to unit
        val lesson = Lesson(id = 1, title = "Lesson 1", unitId = 1, order = 1)
        lessonDao.insertLesson(lesson)

        // Create challenge linked to lesson
        val challenge = Challenge(id = 1, lessonId = 1, type = ChallengeType.SELECT, question = "Test?", order = 1)
        challengeDao.insertChallenge(challenge)

        // Verify all exist
        assertNotNull(courseDao.getCourseById(1))
        assertNotNull(unitDao.getUnitById(1))
        assertNotNull(lessonDao.getLessonById(1))
        assertNotNull(challengeDao.getChallengeById(1))

        // Delete course - should cascade delete unit, lesson, and challenge
        courseDao.deleteCourse(course)

        // Verify cascade delete worked
        assertNull(courseDao.getCourseById(1))
        assertNull(unitDao.getUnitById(1))
        assertNull(lessonDao.getLessonById(1))
        assertNull(challengeDao.getChallengeById(1))
    }

    @Test
    fun testTopTenUsers() = runBlocking {
        // Test ranking functionality
        val user1 = UserProgress(userId = "user1", userName = "User 1", points = 100)
        val user2 = UserProgress(userId = "user2", userName = "User 2", points = 200)
        val user3 = UserProgress(userId = "user3", userName = "User 3", points = 150)

        userProgressDao.insertUserProgress(user1)
        userProgressDao.insertUserProgress(user2)
        userProgressDao.insertUserProgress(user3)

        val topUsers = userProgressDao.getTopTenUsers().first()
        assertEquals(3, topUsers.size)
        
        // Should be ordered by points descending
        assertEquals("user2", topUsers[0].userId) // 200 points
        assertEquals("user3", topUsers[1].userId) // 150 points
        assertEquals("user1", topUsers[2].userId) // 100 points
    }
}