# BuildConfig Usage Examples

## Available BuildConfig Fields

### Default Fields (Auto-generated)
```kotlin
import com.seoulhankuko.app.BuildConfig

// Default fields
BuildConfig.DEBUG          // Boolean - true for debug builds
BuildConfig.APPLICATION_ID // String - "com.seoulhankuko.app"
BuildConfig.VERSION_NAME   // String - "1.0"
BuildConfig.VERSION_CODE   // Int - 1
BuildConfig.BUILD_TYPE     // String - "debug" or "release"
```

### Custom Fields (Added in build.gradle.kts)
```kotlin
// Custom fields we added
BuildConfig.BASE_URL           // String - "http://10.0.2.2:8000/api/"
BuildConfig.APP_NAME           // String - "Seoul Hankuko Book"
BuildConfig.TIMEOUT_SECONDS    // Int - 30
BuildConfig.ENABLE_LOGGING     // Boolean - true
```

## Usage Examples

### 1. In LoggingModule
```kotlin
if (BuildConfig.DEBUG) {
    Timber.plant(Timber.DebugTree())
} else {
    Timber.plant(ReleaseTree())
}
```

### 2. In NetworkModule
```kotlin
return Retrofit.Builder()
    .baseUrl(BuildConfig.BASE_URL)
    .client(okHttpClient)
    .build()
```

### 3. Conditional Logging
```kotlin
val loggingLevel = if (BuildConfig.ENABLE_LOGGING) {
    HttpLoggingInterceptor.Level.BODY
} else {
    HttpLoggingInterceptor.Level.NONE
}
```

### 4. Timeout Configuration
```kotlin
.connectTimeout(BuildConfig.TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
.readTimeout(BuildConfig.TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
.writeTimeout(BuildConfig.TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
```

### 5. App Information
```kotlin
Logger.appStarted() // Logs app name and version
// Could log: "Seoul Hankuko Book v1.0 (Build 1) started"
```

## Adding More Custom Fields

To add more BuildConfig fields, edit `app/build.gradle.kts`:

```kotlin
defaultConfig {
    // ... existing config ...
    
    // Add custom fields
    buildConfigField("String", "API_KEY", "\"your_api_key_here\"")
    buildConfigField("boolean", "FEATURE_X_ENABLED", "true")
    buildConfigField("int", "MAX_RETRY_COUNT", "3")
}
```

Then use them in code:
```kotlin
val apiKey = BuildConfig.API_KEY
val isFeatureEnabled = BuildConfig.FEATURE_X_ENABLED
val maxRetries = BuildConfig.MAX_RETRY_COUNT
```

## Build Variants

You can also set different values for different build types:

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/api/\"")
        buildConfigField("boolean", "ENABLE_LOGGING", "true")
    }
    release {
        buildConfigField("String", "BASE_URL", "\"https://api.seoulhankuko.com/\"")
        buildConfigField("boolean", "ENABLE_LOGGING", "false")
    }
}
```

## Benefits

1. **Type Safety**: Compile-time constants
2. **Environment Specific**: Different values for debug/release
3. **No Runtime Overhead**: Values are inlined at compile time
4. **Centralized Configuration**: All config in one place
5. **IDE Support**: Auto-completion and refactoring
