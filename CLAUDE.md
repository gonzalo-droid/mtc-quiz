# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MTCQuiz is an Android app for practicing Peru's MTC (Ministerio de Transportes y Comunicaciones) traffic rules exam. It is a multi-module Clean Architecture project using Jetpack Compose and Hilt.

## Build & Development Commands

```bash
# Build the entire project
./gradlew build

# Run all unit tests
./gradlew test

# Run unit tests for a specific module
./gradlew :evaluation:domain:test

# Run a single test class
./gradlew :app:test --tests "com.gondroid.mtcquiz.presentation.screens.home.HomeScreenViewModelTest"

# Run instrumented (Android) tests
./gradlew connectedAndroidTest

# Run instrumented tests for a specific module
./gradlew :app:connectedAndroidTest

# Assemble debug APK
./gradlew assembleDebug

# Assemble release APK (requires signing env vars — see below)
./gradlew assembleRelease
```

### Release Signing

Release builds require these env vars (or `gradle.properties` entries):
- `MTC_KEYSTORE_PATH`, `MTC_KEYSTORE_PASSWORD`, `MTC_KEY_ALIAS`, `MTC_KEY_PASSWORD`

### Firebase Setup

Place `google-services.json` in `app/` before building. Required for Firebase Auth, Realtime Database, Analytics, and Crashlytics.

## Module Architecture

The project uses a strict multi-module structure. Every feature module follows the same three-layer layout:

```
<feature>/
  domain/      ← Pure Kotlin (use cases, models, repository interfaces) — no Android deps
  data/        ← Repository implementations, Room DAOs, Firebase, DataStore
  presentation/ ← Compose screens + ViewModels; uses mtcquiz.android.feature.ui plugin
```

Current features: `auth`, `home`, `detail`, `evaluation`, `questionreview`, `pdf`, `configuration`

Core modules:
- `core:domain` — shared domain models (`Category`, `Question`, `Answer`, `Evaluation`, `QuizRepository`, `AuthRepository`)
- `core:data` — shared data implementations (Firebase Realtime DB, DataStore preferences, Google Sign-In)
- `core:database` — Room database (`MTCDatabase`) with DAOs and entity mappers
- `core:presentation:designsystem` — single `MaterialTheme` entry point, reusable Compose components
- `core:presentation:ui` — type-safe navigation routes, `UiText`, `ObserveAsEvents`, shared utilities

## Convention Plugins (build-logic)

All modules use Gradle convention plugins instead of duplicating build config. Available plugins:

| Plugin ID | Use for |
|---|---|
| `mtcquiz.android.application` | App module |
| `mtcquiz.android.application.compose` | App module with Compose |
| `mtcquiz.android.library` | Library modules |
| `mtcquiz.android.library.compose` | Library modules with Compose |
| `mtcquiz.android.feature.ui` | Feature presentation modules (wraps library.compose + UI deps) |
| `mtcquiz.android.hilt` | Any module needing Hilt DI |
| `mtcquiz.android.room` | Any module using Room |
| `mtcquiz.jvm.library` | Pure Kotlin/JVM modules (e.g. domain layers) |

## Navigation

Navigation uses type-safe routes via `@Serializable` objects/data classes defined in `core:presentation:ui` (`Routes.kt`). All routes are registered in `app/NavigationRoot.kt` — the single `NavHost` for the whole app. When adding a new screen, define its route in `Routes.kt` and add the `composable<>` entry in `NavigationRoot.kt`.

## Dependency Injection

Hilt is used throughout. Each module provides its own `@Module` classes. The `app` module aggregates all feature DI graphs. Use `@InstallIn(SingletonComponent::class)` for app-scoped dependencies and `@InstallIn(ViewModelComponent::class)` for ViewModel-scoped ones.

## Key Technology Decisions

- **Data source**: Firebase Realtime Database (questions/categories fetched remotely), Room (evaluations stored locally), DataStore (user preferences)
- **Auth**: Firebase Authentication + Google Sign-In via Credential Manager
- **Async**: Coroutines + Flow throughout; no RxJava
- **Testing**: JUnit4 + MockK + Turbine (Flow testing) + Truth (assertions) + Robolectric (unit tests with Android APIs) + MockWebServer
- **Serialization**: `kotlinx.serialization` (not Gson/Moshi in active use)
- **Min SDK**: 26
