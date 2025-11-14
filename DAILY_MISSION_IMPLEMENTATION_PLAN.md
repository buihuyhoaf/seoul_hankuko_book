# Daily Mission + x2 EXP Implementation Plan

## 📋 Tổng quan

Flow: Daily Mission + x2 EXP stacked (phiên bản tối giản)

### Mục tiêu
- Server sinh 3 mission ngẫu nhiên mỗi ngày cho user
- User hoàn thành mission → nhận x2 EXP trong 15 phút
- Bonus có thể stack (cộng dồn thời gian)
- App quản lý bonus local, server chỉ ghi nhận EXP

---

## 🗄️ Phase 1: Database Schema (Backend)

### 1.1 Tạo Migration

**File:** `migrations/versions/YYYYMMDDHHMMSS_create_daily_missions.py`

```python
"""create daily_missions table

Revision ID: create_daily_missions
Revises: [previous_revision]
Create Date: YYYY-MM-DD HH:MM:SS
"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

def upgrade():
    op.create_table(
        'daily_missions',
        sa.Column('id', postgresql.UUID(as_uuid=True), primary_key=True),
        sa.Column('user_id', postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column('mission_id', sa.String(50), nullable=False),  # "m1", "m2", "m3"
        sa.Column('type', sa.String(20), nullable=False),  # "lesson", "speaking", "listening"
        sa.Column('target', sa.Integer(), nullable=False),
        sa.Column('progress', sa.Integer(), default=0),
        sa.Column('is_completed', sa.Boolean(), default=False),
        sa.Column('date', sa.Date(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('completed_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.UniqueConstraint('user_id', 'mission_id', 'date', name='unique_user_mission_date')
    )
    op.create_index('idx_daily_missions_user_date', 'daily_missions', ['user_id', 'date'])
    op.create_index('idx_daily_missions_user_type', 'daily_missions', ['user_id', 'type'])

def downgrade():
    op.drop_table('daily_missions')
```

### 1.2 Tạo Model

**File:** `src/app/models/gamification.py` (thêm vào)

```python
class DailyMission(Base):
    __tablename__ = "daily_missions"

    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4, init=False)
    user_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    mission_id: Mapped[str] = mapped_column(String(50))  # "m1", "m2", "m3"
    type: Mapped[str] = mapped_column(String(20))  # "lesson", "speaking", "listening"
    target: Mapped[int] = mapped_column(Integer)
    progress: Mapped[int] = mapped_column(Integer, default=0)
    is_completed: Mapped[bool] = mapped_column(Boolean, default=False)
    date: Mapped[date] = mapped_column(Date)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default_factory=lambda: datetime.now(UTC))
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True)

    __table_args__ = (
        UniqueConstraint('user_id', 'mission_id', 'date', name='unique_user_mission_date'),
    )
```

### 1.3 Update User Model Relationship

**File:** `src/app/models/user.py` (thêm vào relationships)

```python
daily_missions = relationship("DailyMission", back_populates="user")
```

**File:** `src/app/models/gamification.py` (thêm vào DailyMission)

```python
user = relationship("User", back_populates="daily_missions")
```

---

## 🔌 Phase 2: Backend API Endpoints

### 2.1 Tạo Schemas

**File:** `src/app/schemas/mission.py` (mới)

```python
from pydantic import BaseModel
from typing import Optional
from datetime import date

class MissionResponse(BaseModel):
    mission_id: str
    type: str  # "lesson", "speaking", "listening"
    target: int
    progress: int
    is_completed: bool

class TodayMissionsResponse(BaseModel):
    missions: list[MissionResponse]

class ActivityRequest(BaseModel):
    type: str  # "lesson", "speaking", "listening"

class ActivityResponse(BaseModel):
    mission_completed: bool
    mission_id: Optional[str] = None
    expires_at: Optional[int] = None  # timestamp in milliseconds

class ExpRequest(BaseModel):
    exp: int  # EXP đã được nhân đôi ở client
```

### 2.2 Tạo API Router

**File:** `src/app/api/v1/missions.py` (mới)

#### 2.2.1 GET `/api/v1/missions/today`

**Chức năng:**
- Lấy 3 mission của ngày hôm nay
- Nếu chưa có → tự động sinh
- Trả về progress hiện tại

**Logic:**
```python
@router.get("/missions/today", response_model=TodayMissionsResponse)
async def get_today_missions(
    db: Annotated[AsyncSession, Depends(async_get_db)],
    current_user: Annotated[dict, Depends(get_current_user)]
) -> TodayMissionsResponse:
    user_id = UUID(str(current_user["id"]))
    today = date.today()
    
    # Check existing missions
    missions_query = select(DailyMission).filter(
        DailyMission.user_id == user_id,
        DailyMission.date == today
    )
    missions_result = await db.execute(missions_query)
    existing_missions = missions_result.scalars().all()
    
    if not existing_missions:
        # Generate new missions
        existing_missions = await _generate_daily_missions(db, user_id, today)
    
    return TodayMissionsResponse(
        missions=[
            MissionResponse(
                mission_id=m.mission_id,
                type=m.type,
                target=m.target,
                progress=m.progress,
                is_completed=m.is_completed
            )
            for m in existing_missions
        ]
    )
```

#### 2.2.2 POST `/api/v1/activity`

**Chức năng:**
- Nhận activity event từ app
- Cập nhật progress của mission tương ứng
- Nếu mission hoàn thành → trả về `mission_completed: true` + `expires_at`

**Logic:**
```python
@router.post("/activity", response_model=ActivityResponse)
async def track_activity(
    payload: ActivityRequest,
    db: Annotated[AsyncSession, Depends(async_get_db)],
    current_user: Annotated[dict, Depends(get_current_user)]
) -> ActivityResponse:
    user_id = UUID(str(current_user["id"]))
    today = date.today()
    
    # Get active missions for this type
    missions_query = select(DailyMission).filter(
        DailyMission.user_id == user_id,
        DailyMission.date == today,
        DailyMission.type == payload.type,
        DailyMission.is_completed == False
    )
    missions_result = await db.execute(missions_query)
    missions = missions_result.scalars().all()
    
    completed_mission = None
    for mission in missions:
        mission.progress += 1
        if mission.progress >= mission.target:
            mission.is_completed = True
            mission.completed_at = datetime.now(UTC)
            completed_mission = mission
    
    await db.commit()
    
    if completed_mission:
        # Calculate expires_at (15 minutes from now)
        expires_at_ms = int((datetime.now(UTC) + timedelta(minutes=15)).timestamp() * 1000)
        return ActivityResponse(
            mission_completed=True,
            mission_id=completed_mission.mission_id,
            expires_at=expires_at_ms
        )
    
    return ActivityResponse(mission_completed=False)
```

#### 2.2.3 POST `/api/v1/exp`

**Chức năng:**
- Nhận EXP đã được nhân đôi ở client
- Cộng vào tổng EXP của user
- Log vào UserExpLog

**Logic:**
```python
@router.post("/exp", response_model=dict)
async def add_exp(
    payload: ExpRequest,
    db: Annotated[AsyncSession, Depends(async_get_db)],
    current_user: Annotated[dict, Depends(get_current_user)]
) -> dict:
    user_id = UUID(str(current_user["id"]))
    
    user_query = select(User).filter(User.id == user_id)
    user_result = await db.execute(user_query)
    user = user_result.scalar_one_or_none()
    
    if not user:
        raise NotFoundException("User not found")
    
    user.exp += payload.exp
    
    exp_log = UserExpLog(
        user_id=user_id,
        source="lesson_activity",  # hoặc "mission_bonus"
        amount=payload.exp
    )
    db.add(exp_log)
    await db.commit()
    
    return {"message": "EXP added", "total_exp": user.exp}
```

### 2.3 Helper Functions

**File:** `src/app/api/v1/missions.py` (thêm vào)

```python
import random
from datetime import timedelta

async def _generate_daily_missions(
    db: AsyncSession,
    user_id: UUID,
    target_date: date
) -> list[DailyMission]:
    """Generate 3 random missions for a day"""
    mission_types = ["lesson", "speaking", "listening"]
    # Target values: có thể random hoặc dựa trên level user
    target_options = [1, 2, 3]
    
    missions = []
    for i, mission_type in enumerate(mission_types):
        mission = DailyMission(
            user_id=user_id,
            mission_id=f"m{i+1}",
            type=mission_type,
            target=random.choice(target_options),
            progress=0,
            date=target_date,
            created_at=datetime.now(UTC)
        )
        missions.append(mission)
        db.add(mission)
    
    await db.commit()
    return missions
```

### 2.4 Register Router

**File:** `src/app/api/v1/__init__.py`

```python
from .missions import router as missions_router

router.include_router(missions_router)
```

---

## 📱 Phase 3: Android App - Data Layer

### 3.1 API Models

**File:** `app/src/main/java/com/seoulhankuko/app/data/api/model/MissionModels.kt` (mới)

```kotlin
package com.seoulhankuko.app.data.api.model

import com.google.gson.annotations.SerializedName

// Mission Response
data class MissionResponse(
    @SerializedName("mission_id")
    val missionId: String,
    val type: String, // "lesson", "speaking", "listening"
    val target: Int,
    val progress: Int,
    @SerializedName("is_completed")
    val isCompleted: Boolean
)

// Today Missions Response
data class TodayMissionsResponse(
    val missions: List<MissionResponse>
)

// Activity Request
data class ActivityRequest(
    val type: String // "lesson", "speaking", "listening"
)

// Activity Response
data class ActivityResponse(
    @SerializedName("mission_completed")
    val missionCompleted: Boolean,
    @SerializedName("mission_id")
    val missionId: String? = null,
    @SerializedName("expires_at")
    val expiresAt: Long? = null // timestamp in milliseconds
)

// EXP Request
data class ExpRequest(
    val exp: Int // EXP đã được nhân đôi ở client
)

// EXP Response
data class ExpResponse(
    val message: String,
    @SerializedName("total_exp")
    val totalExp: Int
)
```

### 3.2 API Service

**File:** `app/src/main/java/com/seoulhankuko/app/data/api/service/ApiService.kt` (thêm vào)

```kotlin
// Missions endpoints
@GET("v1/missions/today")
suspend fun getTodayMissions(
    @Header("Authorization") token: String? = null
): Response<TodayMissionsResponse>

@POST("v1/activity")
suspend fun trackActivity(
    @Header("Authorization") token: String? = null,
    @Body body: ActivityRequest
): Response<ActivityResponse>

@POST("v1/exp")
suspend fun addExp(
    @Header("Authorization") token: String? = null,
    @Body body: ExpRequest
): Response<ExpResponse>
```

### 3.3 Repository

**File:** `app/src/main/java/com/seoulhankuko/app/data/repository/MissionRepository.kt` (mới)

```kotlin
package com.seoulhankuko.app.data.repository

import com.seoulhankuko.app.data.api.model.*
import com.seoulhankuko.app.data.api.service.ApiService
import javax.inject.Inject

class MissionRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getTodayMissions(token: String?): Result<TodayMissionsResponse> {
        return try {
            val response = apiService.getTodayMissions(token)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get missions"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun trackActivity(type: String, token: String?): Result<ActivityResponse> {
        return try {
            val response = apiService.trackActivity(token, ActivityRequest(type = type))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to track activity"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addExp(exp: Int, token: String?): Result<ExpResponse> {
        return try {
            val response = apiService.addExp(token, ExpRequest(exp = exp))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to add EXP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 🎯 Phase 4: Android App - Exp Bonus Manager

### 4.1 ExpBonusManager

**File:** `app/src/main/java/com/seoulhankuko/app/domain/manager/ExpBonusManager.kt` (mới)

```kotlin
package com.seoulhankuko.app.domain.manager

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpBonusManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("exp_bonus_prefs", Context.MODE_PRIVATE)
    
    private val KEY_EXPIRES_AT = "expires_at"
    
    /**
     * Activate x2 EXP bonus for specified duration
     * If bonus is already active, stack the time
     */
    fun activateBonus(durationMinutes: Int = 15) {
        val now = System.currentTimeMillis()
        val durationMs = durationMinutes * 60 * 1000L
        
        val currentExpiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        
        val newExpiresAt = if (isActive(currentExpiresAt)) {
            // Stack: add to existing time
            currentExpiresAt + durationMs
        } else {
            // New: start from now
            now + durationMs
        }
        
        prefs.edit().putLong(KEY_EXPIRES_AT, newExpiresAt).apply()
    }
    
    /**
     * Check if bonus is currently active
     */
    fun isActive(): Boolean {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return isActive(expiresAt)
    }
    
    private fun isActive(expiresAt: Long): Boolean {
        return expiresAt > 0 && System.currentTimeMillis() < expiresAt
    }
    
    /**
     * Get remaining time in milliseconds
     */
    fun getRemainingTime(): Long {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return if (isActive(expiresAt)) {
            maxOf(0, expiresAt - System.currentTimeMillis())
        } else {
            0L
        }
    }
    
    /**
     * Get remaining time in seconds (for countdown display)
     */
    fun getRemainingTimeSeconds(): Long {
        return getRemainingTime() / 1000
    }
    
    /**
     * Calculate EXP with bonus multiplier
     */
    fun calculateExpWithBonus(baseExp: Int): Int {
        return if (isActive()) {
            baseExp * 2
        } else {
            baseExp
        }
    }
    
    /**
     * Get expires_at timestamp (for sync with server)
     */
    fun getExpiresAt(): Long? {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return if (expiresAt > 0) expiresAt else null
    }
    
    /**
     * Set expires_at from server (for sync)
     */
    fun setExpiresAt(timestamp: Long) {
        prefs.edit().putLong(KEY_EXPIRES_AT, timestamp).apply()
    }
    
    /**
     * Clear bonus (for testing or manual reset)
     */
    fun clearBonus() {
        prefs.edit().remove(KEY_EXPIRES_AT).apply()
    }
}
```

### 4.2 Exp Bonus Log

**File:** `app/src/main/java/com/seoulhankuko/app/domain/model/ExpBonusLog.kt` (mới)

```kotlin
package com.seoulhankuko.app.domain.model

data class ExpBonusLog(
    val missionId: String,
    val startTime: Long, // timestamp
    val duration: Int, // minutes
    val expiresAt: Long // timestamp
)
```

---

## 🔄 Phase 5: Android App - Integration

### 5.1 Update LessonViewModel

**File:** `app/src/main/java/com/seoulhankuko/app/presentation/viewmodel/LessonViewModel.kt`

**Thêm vào:**
```kotlin
@Inject lateinit var expBonusManager: ExpBonusManager
@Inject lateinit var missionRepository: MissionRepository

// Khi hoàn thành lesson/question
private suspend fun handleActivityCompletion(type: String) {
    // Track activity
    missionRepository.trackActivity(type, authToken).onSuccess { response ->
        if (response.missionCompleted && response.expiresAt != null) {
            // Activate bonus
            expBonusManager.setExpiresAt(response.expiresAt)
            // Log bonus activation
            logExpBonus(response.missionId, response.expiresAt)
        }
    }
}

// Khi tính EXP
private fun calculateFinalExp(baseExp: Int): Int {
    return expBonusManager.calculateExpWithBonus(baseExp)
}

// Khi gửi EXP lên server
private suspend fun sendExpToServer(exp: Int) {
    val finalExp = calculateFinalExp(exp)
    missionRepository.addExp(finalExp, authToken)
}
```

### 5.2 Activity Type Mapping

**File:** `app/src/main/java/com/seoulhankuko/app/domain/model/ActivityType.kt` (mới)

```kotlin
package com.seoulhankuko.app.domain.model

enum class ActivityType(val apiValue: String) {
    LESSON("lesson"),
    SPEAKING("speaking"),
    LISTENING("listening");
    
    companion object {
        fun fromApiValue(value: String): ActivityType? {
            return values().find { it.apiValue == value }
        }
    }
}
```

### 5.3 Mission ViewModel

**File:** `app/src/main/java/com/seoulhankuko/app/presentation/viewmodel/MissionViewModel.kt` (mới)

```kotlin
package com.seoulhankuko.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seoulhankuko.app.data.api.model.MissionResponse
import com.seoulhankuko.app.data.repository.MissionRepository
import com.seoulhankuko.app.domain.manager.ExpBonusManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MissionViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
    private val expBonusManager: ExpBonusManager
) : ViewModel() {
    
    private val _missions = MutableStateFlow<List<MissionResponse>>(emptyList())
    val missions: StateFlow<List<MissionResponse>> = _missions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    val bonusRemainingTime: StateFlow<Long> = 
        MutableStateFlow(expBonusManager.getRemainingTimeSeconds())
    
    val isBonusActive: StateFlow<Boolean> = 
        MutableStateFlow(expBonusManager.isActive())
    
    fun loadTodayMissions(token: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            missionRepository.getTodayMissions(token)
                .onSuccess { response ->
                    _missions.value = response.missions
                }
                .onFailure { exception ->
                    _error.value = exception.message
                }
            
            _isLoading.value = false
        }
    }
    
    fun updateBonusStatus() {
        bonusRemainingTime.value = expBonusManager.getRemainingTimeSeconds()
        isBonusActive.value = expBonusManager.isActive()
    }
}
```

---

## 🎨 Phase 6: Android App - UI

### 6.1 Mission Screen UI

**File:** `app/src/main/java/com/seoulhankuko/app/presentation/screens/MissionScreen.kt` (update)

```kotlin
@Composable
fun MissionScreen(
    onNavigateToHome: () -> Unit,
    // ... other nav params
    missionViewModel: MissionViewModel = hiltViewModel()
) {
    val missions by missionViewModel.missions.collectAsStateWithLifecycle()
    val isLoading by missionViewModel.isLoading.collectAsStateWithLifecycle()
    val bonusRemaining by missionViewModel.bonusRemainingTime.collectAsStateWithLifecycle()
    val isBonusActive by missionViewModel.isBonusActive.collectAsStateWithLifecycle()
    
    // Load missions on init
    LaunchedEffect(Unit) {
        missionViewModel.loadTodayMissions(authToken)
    }
    
    // Update bonus countdown
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Update every second
            missionViewModel.updateBonusStatus()
        }
    }
    
    MainScaffold(/* ... */) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Bonus indicator
            if (isBonusActive) {
                BonusIndicatorCard(
                    remainingSeconds = bonusRemaining,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Missions list
            LazyColumn {
                items(missions) { mission ->
                    MissionCard(
                        mission = mission,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
```

### 6.2 Mission Card Component

**File:** `app/src/main/java/com/seoulhankuko/app/presentation/components/MissionCard.kt` (mới)

```kotlin
@Composable
fun MissionCard(
    mission: MissionResponse,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (mission.isCompleted) 
                Color(0xFF4CAF50) else Color.White
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = getMissionTypeLabel(mission.type),
                style = MaterialTheme.typography.titleMedium
            )
            LinearProgressIndicator(
                progress = { mission.progress.toFloat() / mission.target.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${mission.progress}/${mission.target}",
                style = MaterialTheme.typography.bodySmall
            )
            if (mission.isCompleted) {
                Text(
                    text = "✓ Hoàn thành",
                    color = Color.White
                )
            }
        }
    }
}

private fun getMissionTypeLabel(type: String): String {
    return when (type) {
        "lesson" -> "Học bài"
        "speaking" -> "Luyện nói"
        "listening" -> "Luyện nghe"
        else -> type
    }
}
```

### 6.3 Bonus Indicator Component

**File:** `app/src/main/java/com/seoulhankuko/app/presentation/components/BonusIndicatorCard.kt` (mới)

```kotlin
@Composable
fun BonusIndicatorCard(
    remainingSeconds: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFD700) // Gold
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ x2 EXP",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatTime(remainingSeconds),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
```

---

## 🧪 Phase 7: Testing

### 7.1 Backend Tests

**File:** `tests/test_missions_api.py` (mới)

```python
import pytest
from datetime import date, timedelta
from uuid import uuid4

@pytest.mark.asyncio
async def test_get_today_missions(client, test_user, auth_token):
    """Test getting today's missions"""
    response = await client.get(
        "/api/v1/missions/today",
        headers={"Authorization": f"Bearer {auth_token}"}
    )
    assert response.status_code == 200
    data = response.json()
    assert len(data["missions"]) == 3
    assert all("mission_id" in m for m in data["missions"])

@pytest.mark.asyncio
async def test_track_activity_complete_mission(client, test_user, auth_token):
    """Test tracking activity and completing mission"""
    # First get missions
    missions_response = await client.get(
        "/api/v1/missions/today",
        headers={"Authorization": f"Bearer {auth_token}"}
    )
    missions = missions_response.json()["missions"]
    
    # Find a mission with target = 1
    mission = next((m for m in missions if m["target"] == 1), None)
    if mission:
        # Track activity
        activity_response = await client.post(
            "/api/v1/activity",
            headers={"Authorization": f"Bearer {auth_token}"},
            json={"type": mission["type"]}
        )
        assert activity_response.status_code == 200
        data = activity_response.json()
        assert data["mission_completed"] == True
        assert "expires_at" in data
        assert data["expires_at"] > 0

@pytest.mark.asyncio
async def test_add_exp(client, test_user, auth_token):
    """Test adding EXP"""
    response = await client.post(
        "/api/v1/exp",
        headers={"Authorization": f"Bearer {auth_token}"},
        json={"exp": 100}
    )
    assert response.status_code == 200
    data = response.json()
    assert "total_exp" in data
```

### 7.2 Android Tests

**File:** `app/src/test/java/com/seoulhankuko/app/domain/manager/ExpBonusManagerTest.kt` (mới)

```kotlin
@Test
fun `test activate bonus`() {
    val manager = ExpBonusManager(context)
    manager.activateBonus(15)
    assertTrue(manager.isActive())
}

@Test
fun `test stack bonus`() {
    val manager = ExpBonusManager(context)
    manager.activateBonus(15)
    val firstExpiresAt = manager.getExpiresAt()
    
    Thread.sleep(1000) // Wait 1 second
    manager.activateBonus(15)
    val secondExpiresAt = manager.getExpiresAt()
    
    assertTrue(secondExpiresAt!! > firstExpiresAt!!)
}

@Test
fun `test calculate exp with bonus`() {
    val manager = ExpBonusManager(context)
    manager.activateBonus(15)
    val result = manager.calculateExpWithBonus(50)
    assertEquals(100, result)
}
```

---

## 📝 Phase 8: Documentation & Cleanup

### 8.1 API Documentation

- Update API docs với endpoints mới
- Thêm examples cho request/response

### 8.2 Code Comments

- Thêm JSDoc/KDoc cho các functions quan trọng
- Document edge cases

### 8.3 Migration Guide

- Document cách migrate từ DailyGoal sang DailyMission (nếu cần)

---

## ✅ Checklist Implementation

### Backend
- [ ] Tạo migration cho `daily_missions` table
- [ ] Tạo `DailyMission` model
- [ ] Update User model relationship
- [ ] Tạo schemas (`MissionResponse`, `ActivityRequest`, etc.)
- [ ] Implement `GET /api/v1/missions/today`
- [ ] Implement `POST /api/v1/activity`
- [ ] Implement `POST /api/v1/exp`
- [ ] Implement `_generate_daily_missions()` helper
- [ ] Register router trong `__init__.py`
- [ ] Run migration
- [ ] Test endpoints với Postman/curl

### Android App - Data Layer
- [ ] Tạo `MissionModels.kt` với các data classes
- [ ] Thêm endpoints vào `ApiService.kt`
- [ ] Tạo `MissionRepository.kt`
- [ ] Test API calls

### Android App - Domain Layer
- [ ] Tạo `ExpBonusManager.kt`
- [ ] Tạo `ActivityType.kt` enum
- [ ] Tạo `ExpBonusLog.kt` model
- [ ] Test ExpBonusManager logic

### Android App - Presentation Layer
- [ ] Tạo `MissionViewModel.kt`
- [ ] Update `MissionScreen.kt` UI
- [ ] Tạo `MissionCard.kt` component
- [ ] Tạo `BonusIndicatorCard.kt` component
- [ ] Integrate với `LessonViewModel.kt`
- [ ] Add countdown timer logic

### Integration
- [ ] Map activity types (lesson → "lesson", speaking → "speaking", etc.)
- [ ] Integrate activity tracking vào lesson completion flow
- [ ] Integrate EXP calculation với bonus
- [ ] Test end-to-end flow

### Testing
- [ ] Backend unit tests
- [ ] Backend integration tests
- [ ] Android unit tests
- [ ] Android integration tests
- [ ] Manual testing: complete mission → check bonus
- [ ] Manual testing: stack bonus
- [ ] Manual testing: app restart với active bonus

### Polish
- [ ] Error handling
- [ ] Loading states
- [ ] Empty states
- [ ] UI/UX improvements
- [ ] Performance optimization

---

## 🚀 Timeline Estimate

- **Phase 1 (Database)**: 2-3 hours
- **Phase 2 (Backend API)**: 4-6 hours
- **Phase 3 (Android Data)**: 2-3 hours
- **Phase 4 (Exp Bonus Manager)**: 2-3 hours
- **Phase 5 (Integration)**: 3-4 hours
- **Phase 6 (UI)**: 4-6 hours
- **Phase 7 (Testing)**: 3-4 hours
- **Phase 8 (Documentation)**: 1-2 hours

**Total Estimate**: 21-31 hours (3-4 ngày làm việc)

---

## 🔍 Edge Cases & Considerations

### 1. Timezone Issues
- Server dùng UTC, app cần convert đúng
- Mission date reset theo timezone của user

### 2. Race Conditions
- Nhiều activity cùng lúc → dùng database transaction
- Bonus stacking → thread-safe trong ExpBonusManager

### 3. App Restart
- Persist `expiresAt` trong SharedPreferences
- Restore khi app mở lại

### 4. Network Failures
- Retry logic cho API calls
- Queue activity events nếu offline

### 5. Mission Generation
- Đảm bảo mỗi user chỉ có 3 missions/ngày
- Unique constraint trong database

---

## 📚 Notes

- Flow này là phiên bản tối giản, có thể mở rộng sau:
  - Server-side validation cho bonus
  - Multi-device sync
  - Mission history
  - Mission rewards khác ngoài x2 EXP
  - Mission difficulty levels

- Ưu tiên: Đơn giản, dễ maintain, dễ test

