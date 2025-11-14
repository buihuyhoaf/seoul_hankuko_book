# Daily Mission Implementation Checklist

## 🗄️ Backend - Database & Models

- [ ] **Migration**: Tạo `create_daily_missions.py` migration
- [ ] **Model**: Thêm `DailyMission` vào `models/gamification.py`
- [ ] **Relationship**: Update User model với `daily_missions` relationship
- [ ] **Run Migration**: Test migration up/down

## 🔌 Backend - API Endpoints

- [ ] **Schemas**: Tạo `schemas/mission.py` với:
  - [ ] `MissionResponse`
  - [ ] `TodayMissionsResponse`
  - [ ] `ActivityRequest`
  - [ ] `ActivityResponse`
  - [ ] `ExpRequest`

- [ ] **Router**: Tạo `api/v1/missions.py` với:
  - [ ] `GET /api/v1/missions/today` - Lấy missions hôm nay
  - [ ] `POST /api/v1/activity` - Track activity, update progress
  - [ ] `POST /api/v1/exp` - Thêm EXP (đã nhân đôi)

- [ ] **Helper**: Implement `_generate_daily_missions()`
- [ ] **Register**: Thêm router vào `api/v1/__init__.py`

## 📱 Android - Data Layer

- [ ] **Models**: Tạo `MissionModels.kt` với:
  - [ ] `MissionResponse`
  - [ ] `TodayMissionsResponse`
  - [ ] `ActivityRequest`
  - [ ] `ActivityResponse`
  - [ ] `ExpRequest`
  - [ ] `ExpResponse`

- [ ] **API Service**: Thêm vào `ApiService.kt`:
  - [ ] `getTodayMissions()`
  - [ ] `trackActivity()`
  - [ ] `addExp()`

- [ ] **Repository**: Tạo `MissionRepository.kt` với:
  - [ ] `getTodayMissions()`
  - [ ] `trackActivity()`
  - [ ] `addExp()`

## 🎯 Android - Domain Layer

- [ ] **ExpBonusManager**: Tạo `ExpBonusManager.kt` với:
  - [ ] `activateBonus()` - Activate/stack bonus
  - [ ] `isActive()` - Check if bonus active
  - [ ] `getRemainingTime()` - Get remaining time
  - [ ] `calculateExpWithBonus()` - Calculate EXP with multiplier
  - [ ] `setExpiresAt()` - Sync với server
  - [ ] Persist trong SharedPreferences

- [ ] **Models**: Tạo:
  - [ ] `ActivityType.kt` enum
  - [ ] `ExpBonusLog.kt` data class

## 🔄 Android - Integration

- [ ] **LessonViewModel**: Update để:
  - [ ] Track activity khi hoàn thành lesson/question
  - [ ] Activate bonus khi nhận `mission_completed`
  - [ ] Tính EXP với bonus trước khi gửi server
  - [ ] Gửi EXP đã nhân đôi lên server

- [ ] **Activity Mapping**: Map activity types:
  - [ ] Lesson completion → "lesson"
  - [ ] Speaking exercise → "speaking"
  - [ ] Listening exercise → "listening"

## 🎨 Android - UI

- [ ] **MissionViewModel**: Tạo với:
  - [ ] Load missions
  - [ ] Update bonus status
  - [ ] Countdown timer

- [ ] **MissionScreen**: Update UI với:
  - [ ] Hiển thị 3 missions
  - [ ] Progress bars
  - [ ] Completion status

- [ ] **Components**: Tạo:
  - [ ] `MissionCard.kt` - Card hiển thị mission
  - [ ] `BonusIndicatorCard.kt` - Hiển thị x2 EXP countdown

- [ ] **Countdown**: Implement real-time countdown timer

## 🧪 Testing

- [ ] **Backend Tests**:
  - [ ] Test get today missions
  - [ ] Test track activity
  - [ ] Test complete mission
  - [ ] Test add EXP

- [ ] **Android Tests**:
  - [ ] Test ExpBonusManager
  - [ ] Test MissionRepository
  - [ ] Test activity tracking integration

- [ ] **Manual Testing**:
  - [ ] Complete mission → check bonus activation
  - [ ] Stack bonus (complete 2 missions)
  - [ ] App restart với active bonus
  - [ ] Countdown timer accuracy
  - [ ] EXP calculation với/không bonus

## 📝 Documentation

- [ ] Update API documentation
- [ ] Add code comments
- [ ] Update README nếu cần

## 🚀 Deployment

- [ ] Run migration trên staging
- [ ] Test trên staging environment
- [ ] Deploy backend
- [ ] Deploy Android app (beta test)
- [ ] Monitor logs và errors

---

## Quick Reference

### API Endpoints
- `GET /api/v1/missions/today` - Get today's missions
- `POST /api/v1/activity` - Track activity
- `POST /api/v1/exp` - Add EXP

### Key Files
- Backend: `models/gamification.py`, `api/v1/missions.py`
- Android: `ExpBonusManager.kt`, `MissionRepository.kt`, `MissionViewModel.kt`

### Testing Commands
```bash
# Backend
pytest tests/test_missions_api.py

# Android
./gradlew test
```

