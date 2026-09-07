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

# Generate a merged, informational-only Kover HTML coverage report across all modules
# Output: app/build/reports/kover/html/index.html
./gradlew :app:koverHtmlReport
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

## Question Banks

The nine question banks live in `app/src/main/assets/`: `json/<examId>_questions.json`, the
source balotarios in `pdf/`, and the sign artwork in `images/` as `.webp`. `QuizRepositoryImpl`
reads the JSON straight from assets — there is no remote question source.

**The PDF is the source of truth.** When a bank disagrees with its balotario the PDF wins:
questions, answers and images, the source's own typos included. The exam is marked against
that document, so a "more correct" answer would make the user fail the real test; nuance
belongs in `fundamento`.

Extraction and auditing scripts are in `.claude/skills/mtc-question-extractor/scripts/`
(see `SKILL.md` for the extractors). The two auditors take nothing from the extractors, so an
extraction bug cannot hide inside its own audit:

```bash
# structure, every title/option present in the PDF, and the answer letter read
# from the PDF's own RESPUESTA column
python3 .claude/skills/mtc-question-extractor/scripts/audit_questions.py [examId ...]

# every .webp compared pixel by pixel with the picture printed in its PDF row
python3 .claude/skills/mtc-question-extractor/scripts/audit_images.py [examId ...]
python3 .claude/skills/mtc-question-extractor/scripts/audit_images.py --render b2a 13 14 15
```

Both need `pdftotext`/`pdftohtml` (poppler) and Pillow. `QuestionAssetsSchemaTest` guards the
structural invariants at build time; the assets are declared as an input of the test task in
`app/build.gradle.kts` so an asset-only change actually re-runs it.

A bank is only ever compared against its own balotario. What another PDF says about the same
question is irrelevant, even when they disagree — each bank mirrors the document its users are
examined on.

Two caveats when acting on a finding. The cell reader is ~90-93% accurate per cell, so never
bulk-overwrite JSON text with it; confirm the change against the PDF itself first — every word
of the new text must appear among that row's own fragments, or read the row with
`audit_images.py --render`. And two things look like PDF content but are artifacts of the
reader: hyphens left by line breaks ("contra- curva") and the underscores of fill-in-the-blank
questions, which `pdftohtml` drops.

Known gaps in the source documents (not defects to fix): `b2c` is missing the row at page 20
Nº28 of its second table, whose fourth option and answer cells are blank; and the RESPUESTA
cell is blank for "La hoja de ruta electrónica se debe elaborar" in a3a/a3b/a3c. Some banks
also repeat a question verbatim, because their PDF does.

## Key Technology Decisions

- **Data source**: local JSON assets (questions), a hardcoded `CategoryLocalDataSource` (categories), Room (evaluations stored locally), DataStore (user preferences)
- **Auth**: Firebase Authentication + Google Sign-In via Credential Manager
- **Async**: Coroutines + Flow throughout; no RxJava
- **Testing**: JUnit4 + MockK + Turbine (Flow testing) + Truth (assertions) + Robolectric (unit tests with Android APIs) + MockWebServer
- **Serialization**: `kotlinx.serialization` (not Gson/Moshi in active use)
- **Min SDK**: 26
