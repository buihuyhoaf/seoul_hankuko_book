# LanguageTool Local Server Integration Plan (Option B)

## 📋 Overview

**Mục tiêu**: Tích hợp LanguageTool server vào cùng FastAPI service để:
- ✅ Tránh rate limiting (20 requests/ngày từ public API)
- ✅ Không giới hạn ký tự (từ 10K → unlimited)
- ✅ Bảo mật tốt hơn (dữ liệu không gửi ra ngoài)
- ✅ Chỉ dùng 1 service (tiết kiệm free plan slot)
- ✅ Đơn giản hơn (1 deployment thay vì 2)

**Approach**: Chạy LanguageTool server như subprocess trong cùng container với FastAPI

**Deployment**: Render.com Free Plan

---

## 🏗️ Architecture

```
┌─────────────────────────────────────┐
│  Docker Container (512MB RAM)      │
│  ┌───────────────────────────────┐  │
│  │  FastAPI (Python)             │  │
│  │  - Main API                   │  │
│  │  - LanguageTool Manager       │  │
│  └───────────┬───────────────────┘  │
│              │ subprocess            │
│  ┌───────────▼───────────────────┐  │
│  │  LanguageTool Server (Java)  │  │
│  │  - Port 8010 (internal)     │  │
│  └──────────────────────────────┘  │
└─────────────────────────────────────┘
```

**Flow**:
1. Container starts → FastAPI lifespan starts
2. FastAPI spawns LanguageTool server as subprocess
3. LanguageTool server starts on port 8010 (internal)
4. User submit bài viết → FastAPI
5. FastAPI calls `http://localhost:8010` (LanguageTool)
6. LanguageTool processes và trả về
7. FastAPI trả về cho user

---

## 📝 Implementation Steps

### Phase 1: Update Dockerfile

#### Step 1.1: Add Java and LanguageTool

**File**: `Dockerfile`

Thay đổi phần Final Stage:

```dockerfile
# --------- Final Stage ---------
FROM python:3.11-slim

# Update package lists and install dependencies + Java (cho LanguageTool)
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    openjdk-17-jre-headless \
    && rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
RUN groupadd --gid 1000 app \
    && useradd --uid 1000 --gid app --shell /bin/bash --create-home app

# Download và setup LanguageTool (as root, then chown)
RUN mkdir -p /opt/languagetool && \
    cd /opt/languagetool && \
    curl -L https://languagetool.org/download/LanguageTool-stable.zip -o languagetool.zip && \
    unzip languagetool.zip && \
    rm languagetool.zip && \
    chown -R app:app /opt/languagetool

# Copy the virtual environment from the builder stage
COPY --from=builder --chown=app:app /app/.venv /app/.venv

# Copy source code from builder stage
COPY --from=builder --chown=app:app /app/src /code/src
COPY --from=builder --chown=app:app /app/src/migrations /code/migrations
COPY --from=builder --chown=app:app /app/src/alembic.ini /code/alembic.ini
COPY --from=builder --chown=app:app /app/deploy.sh /code/deploy.sh

# Ensure the virtual environment is in the PATH
ENV PATH="/app/.venv/bin:$PATH"

# Set default port (can be overridden by Render/Railway)
ENV PORT=8000
ENV LANGUAGETOOL_PORT=8010

# Make deployment script executable
RUN chmod +x /code/deploy.sh

# Switch to the non-root user
USER app

# Set the working directory
WORKDIR /code

# Use deployment script to run migrations then start server
CMD ["/code/deploy.sh"]
```

**Lưu ý**:
- Java 17 JRE headless (~100MB)
- LanguageTool zip (~50MB)
- Total thêm ~150MB vào image size
- Memory usage: FastAPI ~100MB + LanguageTool ~300MB = ~400MB < 512MB limit ✅

---

### Phase 2: Create LanguageTool Server Manager

#### Step 2.1: Create Server Manager Module

**File**: `src/app/services/languagetool_server.py`

```python
"""
Manage LanguageTool server as subprocess within FastAPI container.

This module handles starting/stopping LanguageTool server as a background
process when using local server mode.
"""
import subprocess
import logging
import time
import os
from pathlib import Path
from typing import Optional

logger = logging.getLogger(__name__)

_languagetool_process: Optional[subprocess.Popen] = None
_languagetool_port: int = 8010


def find_languagetool_jar() -> Optional[Path]:
    """Find LanguageTool server JAR file."""
    lt_base = Path("/opt/languagetool")
    
    if not lt_base.exists():
        logger.warning("LanguageTool directory not found at /opt/languagetool")
        return None
    
    # Look for LanguageTool-*/languagetool-server.jar
    jar_files = list(lt_base.glob("LanguageTool-*/languagetool-server.jar"))
    
    if not jar_files:
        logger.warning("LanguageTool server JAR not found")
        return None
    
    return jar_files[0]


def is_server_ready(port: int, timeout: float = 2.0) -> bool:
    """Check if LanguageTool server is ready."""
    try:
        import requests
        response = requests.get(
            f"http://localhost:{port}/v2/languages",
            timeout=timeout
        )
        return response.status_code == 200
    except Exception:
        return False


def start_languagetool_server(port: int = 8010) -> bool:
    """
    Start LanguageTool server as background subprocess.
    
    Args:
        port: Port to run LanguageTool server on (default: 8010)
        
    Returns:
        True if server started successfully, False otherwise
    """
    global _languagetool_process, _languagetool_port
    
    if _languagetool_process is not None:
        # Check if process is still alive
        if _languagetool_process.poll() is None:
            logger.info("LanguageTool server already running")
            return True
        else:
            logger.warning("LanguageTool server process died, restarting...")
            _languagetool_process = None
    
    jar_path = find_languagetool_jar()
    if jar_path is None:
        logger.error("Cannot start LanguageTool server: JAR not found")
        return False
    
    jar_dir = jar_path.parent
    
    try:
        logger.info(f"Starting LanguageTool server on port {port}...")
        
        # Start LanguageTool server as subprocess
        process = subprocess.Popen(
            [
                "java",
                "-Xmx300m",  # Max heap: 300MB (leave room for FastAPI)
                "-Xms100m",  # Initial heap: 100MB
                "-XX:+UseG1GC",  # Use G1 garbage collector (better for small heaps)
                "-cp", str(jar_path),
                "org.languagetool.server.HTTPServer",
                "--port", str(port),
                "--public",
                "--allow-origin", "*"
            ],
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            cwd=str(jar_dir),
            env=os.environ.copy()
        )
        
        # Wait for server to be ready (max 30 seconds)
        logger.info("Waiting for LanguageTool server to be ready...")
        for i in range(30):
            if process.poll() is not None:
                # Process died
                stdout, stderr = process.communicate()
                logger.error(f"LanguageTool server failed to start:")
                logger.error(f"STDOUT: {stdout.decode() if stdout else 'None'}")
                logger.error(f"STDERR: {stderr.decode() if stderr else 'None'}")
                return False
            
            if is_server_ready(port):
                logger.info(f"✅ LanguageTool server started successfully on port {port}")
                _languagetool_process = process
                _languagetool_port = port
                return True
            
            time.sleep(1)
        
        # Timeout
        logger.error("LanguageTool server failed to start within 30 seconds")
        process.terminate()
        process.wait(timeout=5)
        return False
        
    except Exception as e:
        logger.exception(f"Failed to start LanguageTool server: {e}")
        return False


def stop_languagetool_server() -> None:
    """Stop LanguageTool server subprocess."""
    global _languagetool_process
    
    if _languagetool_process is None:
        return
    
    try:
        logger.info("Stopping LanguageTool server...")
        _languagetool_process.terminate()
        
        # Wait up to 5 seconds for graceful shutdown
        try:
            _languagetool_process.wait(timeout=5)
        except subprocess.TimeoutExpired:
            # Force kill if not responding
            logger.warning("LanguageTool server did not stop gracefully, forcing kill...")
            _languagetool_process.kill()
            _languagetool_process.wait()
        
        logger.info("LanguageTool server stopped")
        _languagetool_process = None
        
    except Exception as e:
        logger.exception(f"Error stopping LanguageTool server: {e}")
        _languagetool_process = None


def get_languagetool_status() -> dict:
    """Get current status of LanguageTool server."""
    global _languagetool_process, _languagetool_port
    
    if _languagetool_process is None:
        return {
            "running": False,
            "port": _languagetool_port,
            "ready": False
        }
    
    is_alive = _languagetool_process.poll() is None
    is_ready = is_alive and is_server_ready(_languagetool_port)
    
    return {
        "running": is_alive,
        "port": _languagetool_port,
        "ready": is_ready
    }
```

---

### Phase 3: Update Configuration

#### Step 3.1: Add LanguageTool Settings

**File**: `src/app/core/config.py`

Thêm class mới sau `MLModelSettings`:

```python
class LanguageToolSettings(BaseSettings):
    """Settings for LanguageTool integration."""
    # Sử dụng local server hay public API
    LANGUAGETOOL_USE_LOCAL: bool = config("LANGUAGETOOL_USE_LOCAL", default=False, cast=bool)
    # Port của local server (internal, không cần expose)
    LANGUAGETOOL_PORT: int = config("LANGUAGETOOL_PORT", default=8010, cast=int)
    # Language code (mặc định là "ko" cho tiếng Hàn)
    LANGUAGETOOL_LANG: str = config("LANGUAGETOOL_LANG", default="ko")
```

Update `Settings` class:

```python
class Settings(
    AppSettings,
    SQLiteSettings,
    PostgresSettings,
    CryptSettings,
    GoogleAuthSettings,
    FirstUserSettings,
    TestSettings,
    RedisCacheSettings,
    ClientSideCacheSettings,
    RedisQueueSettings,
    RedisRateLimiterSettings,
    DefaultRateLimitSettings,
    CRUDAdminSettings,
    EnvironmentSettings,
    SupabaseSettings,
    MLModelSettings,
    LanguageToolSettings,  # Thêm dòng này
):
    pass
```

---

### Phase 4: Integrate into FastAPI Lifespan

#### Step 4.1: Update setup.py

**File**: `src/app/core/setup.py`

Update `lifespan` function trong `lifespan_factory`:

```python
@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator:
    from asyncio import Event

    initialization_complete = Event()
    app.state.initialization_complete = initialization_complete

    await set_threadpool_tokens()

    try:
        # Redis initialization - gracefully handle if Redis is not available
        if isinstance(settings, RedisCacheSettings):
            try:
                await create_redis_cache_pool()
            except Exception as e:
                logger.warning(f"Redis cache pool initialization failed (Redis may not be available): {e}")

        if isinstance(settings, RedisQueueSettings):
            try:
                await create_redis_queue_pool()
            except Exception as e:
                logger.warning(f"Redis queue pool initialization failed (Redis may not be available): {e}")

        if isinstance(settings, RedisRateLimiterSettings):
            try:
                await create_redis_rate_limit_pool()
            except Exception as e:
                logger.warning(f"Redis rate limiter pool initialization failed (Redis may not be available): {e}")

        if create_tables_on_start:
            await create_tables()

        # Start LanguageTool server if using local mode
        if settings.LANGUAGETOOL_USE_LOCAL:
            from ..services.languagetool_server import start_languagetool_server
            # Start in background thread (non-blocking)
            import threading
            def start_lt():
                success = start_languagetool_server(port=settings.LANGUAGETOOL_PORT)
                if not success:
                    logger.warning("LanguageTool local server failed to start, will fallback to public API")
            
            thread = threading.Thread(target=start_lt, daemon=True)
            thread.start()
            logger.info("LanguageTool server startup initiated in background")

        # ML model loading: DISABLED on startup to save memory (512MB limit on Render free tier)
        # Model will be loaded lazily on first request via get_model() in stroke_api.py
        # This prevents OOM errors during deployment
        # await load_ml_model(background=True, allow_mock=True)

        initialization_complete.set()

        yield

    finally:
        # Cleanup
        if isinstance(settings, RedisCacheSettings):
            await close_redis_cache_pool()

        if isinstance(settings, RedisQueueSettings):
            await close_redis_queue_pool()

        if isinstance(settings, RedisRateLimiterSettings):
            await close_redis_rate_limit_pool()
        
        # Stop LanguageTool server
        if settings.LANGUAGETOOL_USE_LOCAL:
            from ..services.languagetool_server import stop_languagetool_server
            stop_languagetool_server()
```

---

### Phase 5: Update LanguageTool Client Code

#### Step 5.1: Update writing_ai.py

**File**: `src/app/services/writing_ai.py`

Update hàm `_get_language_tool()`:

```python
from ..core.config import settings

@lru_cache(maxsize=1)
def _get_language_tool() -> language_tool_python.LanguageTool | language_tool_python.LanguageToolPublicAPI:
    """
    Reuse a single LanguageTool client for Korean to avoid cold starts.
    
    Returns:
        LanguageTool instance (local server) or LanguageToolPublicAPI (public API)
    """
    if settings.LANGUAGETOOL_USE_LOCAL:
        # Sử dụng local server (chạy trong cùng container)
        local_url = f"http://localhost:{settings.LANGUAGETOOL_PORT}"
        logger.info(f"Using LanguageTool local server: {local_url}")
        return language_tool_python.LanguageTool(
            language=settings.LANGUAGETOOL_LANG,
            remote_server=local_url
        )
    else:
        # Sử dụng public API (fallback)
        logger.info("Using LanguageTool public API")
        return language_tool_python.LanguageToolPublicAPI(settings.LANGUAGETOOL_LANG)
```

Update `evaluate_writing_with_ai()` với better error handling:

```python
async def evaluate_writing_with_ai(text: str) -> WritingAiEvaluationResult:
    """
    Evaluate a learner's writing using AI.

    Notes
    -----
    - Uses LanguageTool local server (if configured) or public API as fallback.
    - Wraps LanguageTool failures and returns safe fallback feedback.
    """
    text = text.strip()
    if not text:
        return WritingAiEvaluationResult(
            score=10.0,
            feedback="Không có nội dung để chấm.",
            spelling_score=10.0,
            grammar_score=10.0,
            corrected_text=None,
        )

    try:
        tool = _get_language_tool()
        matches = tool.check(text)
        corrected_text = tool.correct(text)

        spelling_errors, grammar_errors = _categorize_matches(matches)
        total_tokens = max(len(text.split()), 1)
        spelling_score = _score_from_counts(spelling_errors, total_tokens)
        grammar_score = _score_from_counts(grammar_errors, total_tokens)
        overall_score = round((spelling_score + grammar_score) / 2.0, 2)
        feedback = _build_feedback(spelling_errors, grammar_errors, corrected_text)

        return WritingAiEvaluationResult(
            score=overall_score,
            feedback=feedback,
            spelling_score=spelling_score,
            grammar_score=grammar_score,
            corrected_text=corrected_text if corrected_text != text else None,
        )
    except (LanguageToolError, ConnectionError, TimeoutError) as exc:
        logger.warning("LanguageTool evaluation failed: %s", exc)
        
        # Fallback: thử public API nếu local server fail
        if settings.LANGUAGETOOL_USE_LOCAL:
            logger.info("Falling back to public API due to local server error")
            try:
                public_tool = language_tool_python.LanguageToolPublicAPI(settings.LANGUAGETOOL_LANG)
                matches = public_tool.check(text)
                corrected_text = public_tool.correct(text)
                
                spelling_errors, grammar_errors = _categorize_matches(matches)
                total_tokens = max(len(text.split()), 1)
                spelling_score = _score_from_counts(spelling_errors, total_tokens)
                grammar_score = _score_from_counts(grammar_errors, total_tokens)
                overall_score = round((spelling_score + grammar_score) / 2.0, 2)
                feedback = _build_feedback(spelling_errors, grammar_errors, corrected_text)
                
                return WritingAiEvaluationResult(
                    score=overall_score,
                    feedback=feedback + " (Sử dụng public API do local server không khả dụng)",
                    spelling_score=spelling_score,
                    grammar_score=grammar_score,
                    corrected_text=corrected_text if corrected_text != text else None,
                )
            except Exception as fallback_exc:
                logger.error("Public API fallback also failed: %s", fallback_exc)
        
        return WritingAiEvaluationResult(
            score=None,
            feedback="Không thể kết nối tới dịch vụ kiểm tra ngôn ngữ. Vui lòng thử lại sau.",
        )
    except Exception as exc:
        logger.exception("Unexpected error during AI evaluation: %s", exc)
        return WritingAiEvaluationResult(
            score=None,
            feedback="Đã xảy ra lỗi khi chấm bài viết. Vui lòng thử lại sau.",
        )
```

#### Step 5.2: Update grade.py

**File**: `src/app/api/v1/grade.py`

Tương tự, update hàm `get_tool()`:

```python
from ..core.config import settings

@lru_cache(maxsize=1)
def get_tool() -> language_tool_python.LanguageTool | language_tool_python.LanguageToolPublicAPI:
    """Reuse a single LanguageTool client for Korean."""
    
    if settings.LANGUAGETOOL_USE_LOCAL:
        local_url = f"http://localhost:{settings.LANGUAGETOOL_PORT}"
        logger.info(f"Using LanguageTool local server: {local_url}")
        return language_tool_python.LanguageTool(
            language=settings.LANGUAGETOOL_LANG,
            remote_server=local_url
        )
    else:
        logger.info("Using LanguageTool public API")
        return language_tool_python.LanguageToolPublicAPI(settings.LANGUAGETOOL_LANG)
```

---

### Phase 6: Add Health Check Endpoint

#### Step 6.1: Create Health Check

**File**: `src/app/api/v1/health.py` (hoặc update existing)

Thêm endpoint để check LanguageTool status:

```python
from fastapi import APIRouter
from ...core.config import settings
from ...services.languagetool_server import get_languagetool_status

router = APIRouter(prefix="/health", tags=["health"])

@router.get("/languagetool")
async def check_languagetool_health():
    """Check LanguageTool server health status."""
    if not settings.LANGUAGETOOL_USE_LOCAL:
        return {
            "mode": "public_api",
            "status": "ok"
        }
    
    status = get_languagetool_status()
    return {
        "mode": "local_server",
        "status": "ready" if status["ready"] else "not_ready",
        "running": status["running"],
        "port": status["port"]
    }
```

---

### Phase 7: Update Dependencies

#### Step 7.1: Add requests dependency

**File**: `pyproject.toml`

Đảm bảo có `requests` trong dependencies (đã có sẵn trong project):

```toml
dependencies = [
    # ... existing dependencies ...
    "requests>=2.31.0",  # Đã có sẵn
    "language-tool-python>=2.7.0",  # Đã có sẵn
]
```

---

## 🧪 Testing

### Phase 8: Local Testing

#### Step 8.1: Test Docker Build

```bash
# Build image
docker build -t korean-learning:test .

# Run container
docker run -d \
  -p 8000:8000 \
  -e LANGUAGETOOL_USE_LOCAL=true \
  -e LANGUAGETOOL_PORT=8010 \
  -e LANGUAGETOOL_LANG=ko \
  korean-learning:test

# Check logs
docker logs <container_id>

# Test health check
curl http://localhost:8000/api/v1/health/languagetool

# Test grading
curl -X POST http://localhost:8000/api/v1/grade/korean \
  -H "Content-Type: application/json" \
  -d '{"text": "안녕하세요"}'
```

#### Step 8.2: Test Fallback

```bash
# Test với local server disabled
docker run -d \
  -p 8000:8000 \
  -e LANGUAGETOOL_USE_LOCAL=false \
  korean-learning:test

# Should use public API
curl -X POST http://localhost:8000/api/v1/grade/korean \
  -H "Content-Type: application/json" \
  -d '{"text": "안녕하세요"}'
```

---

## 🚀 Deployment

### Phase 9: Deploy to Render

#### Step 9.1: Update render.yaml

**File**: `render.yaml`

Không cần thay đổi gì! Chỉ cần update environment variables:

```yaml
services:
  - type: web
    name: korean-learning-api
    env: docker
    dockerfilePath: ./Dockerfile
    dockerContext: .
    plan: starter  # hoặc free
    envVars:
      - key: DATABASE_URL
        fromDatabase:
          name: korean-learning-db
          property: connectionString
      - key: SECRET_KEY
        generateValue: true
      - key: ENVIRONMENT
        value: production
      - key: POSTGRES_URL
        fromDatabase:
          name: korean-learning-db
          property: connectionString
      # Thêm LanguageTool config
      - key: LANGUAGETOOL_USE_LOCAL
        value: "true"
      - key: LANGUAGETOOL_PORT
        value: "8010"
      - key: LANGUAGETOOL_LANG
        value: "ko"
    healthCheckPath: /health
```

#### Step 9.2: Deploy

1. Push code lên Git
2. Render sẽ tự động detect và deploy
3. Monitor logs để đảm bảo LanguageTool server start thành công
4. Test health check endpoint

---

## 📊 Monitoring & Troubleshooting

### Health Checks

1. **Main API Health**: `/health`
2. **LanguageTool Health**: `/api/v1/health/languagetool`

### Common Issues

#### Issue 1: LanguageTool Server Not Starting

**Symptom**: Logs show "LanguageTool server failed to start"

**Possible Causes**:
- Java not installed
- LanguageTool JAR not found
- Port 8010 already in use
- Out of memory

**Solution**:
- Check Dockerfile có install Java
- Check `/opt/languagetool` có tồn tại
- Check logs để xem error message
- Reduce JVM memory: `-Xmx200m` thay vì `300m`

#### Issue 2: Out of Memory

**Symptom**: Container crashes hoặc OOM errors

**Solution**:
- Reduce LanguageTool memory: `-Xmx200m`
- Disable ML model loading (đã disable)
- Monitor memory usage

#### Issue 3: Slow Startup

**Symptom**: First request sau khi deploy rất chậm

**Cause**: LanguageTool server đang start (có thể mất 20-30s)

**Solution**:
- Normal behavior, không cần fix
- Có thể add startup probe để wait

#### Issue 4: Connection Refused

**Symptom**: `Connection refused` khi gọi LanguageTool

**Solution**:
- Check LanguageTool server đã start chưa
- Check port number đúng chưa
- Check logs của LanguageTool process

### Logging

Monitor logs để track:
- LanguageTool server startup time
- Request/response times
- Error rates
- Memory usage

---

## ✅ Checklist

### Pre-Deployment
- [ ] Update `Dockerfile` (add Java + LanguageTool)
- [ ] Create `languagetool_server.py`
- [ ] Add `LanguageToolSettings` to config
- [ ] Update `setup.py` lifespan
- [ ] Update `writing_ai.py`
- [ ] Update `grade.py`
- [ ] Add health check endpoint
- [ ] Test locally với Docker

### Deployment
- [ ] Update `render.yaml` env vars
- [ ] Push code to Git
- [ ] Monitor deployment logs
- [ ] Verify LanguageTool server starts
- [ ] Test health check endpoint
- [ ] Test grading endpoint

### Post-Deployment
- [ ] Monitor logs for 24h
- [ ] Check memory usage
- [ ] Verify no rate limiting issues
- [ ] Test với bài viết dài (>10K chars)

---

## 🔄 Rollback Plan

Nếu có vấn đề, có thể rollback nhanh:

1. Set `LANGUAGETOOL_USE_LOCAL=false` trong Render dashboard
2. Restart service
3. System sẽ tự động fallback về public API

Hoặc revert Dockerfile về version cũ (remove Java + LanguageTool).

---

## 📈 Future Improvements

1. **Caching**: Cache results cho cùng text
2. **Health Monitoring**: Add metrics (Prometheus)
3. **Auto-restart**: Auto-restart LanguageTool nếu crash
4. **Resource Limits**: Fine-tune JVM memory based on usage

---

## 📚 References

- LanguageTool Download: https://languagetool.org/download
- LanguageTool Server Docs: https://dev.languagetool.org/http-server
- Java Memory Tuning: https://www.oracle.com/java/technologies/javase/vmoptions-jsp.html

