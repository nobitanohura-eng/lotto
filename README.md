# Laxmi Lotto Application

Official Laxmi Lotto Android Application codebase with embedded offline web interface and background relay capabilities.

## Build Instructions

### Prerequisites
- JDK 21 (Temurin)
- Android Studio / Gradle 8.10+

### Building Locally
```bash
./gradlew assembleDebug
```

### Building via GitHub Actions
Push code to `main` branch to trigger automatic APK build via `.github/workflows/build.yml`.
