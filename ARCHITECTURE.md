# Arquitectura

Este documento describe cómo está construido MTCQuiz a nivel de código: capas, módulos, flujo de datos y patrones. Para qué hace la app, cómo levantarla y su roadmap, ver [README.md](README.md).

---

## Arquitectura general

El proyecto sigue **Clean Architecture** con separación estricta en tres capas dentro de cada feature:

```
presentation/  ←  Compose Screens + ViewModels (UI state, acciones, eventos)
domain/        ←  Interfaces de repositorios + modelos puros (puro Kotlin, sin Android)
data/          ←  Implementaciones de repositorios, DAOs, fuentes externas
```

Estas capas se implementan como **módulos Gradle independientes**, lo que impide que capas superiores accedan directamente a capas inferiores y permite compilación incremental.

---

## Estructura de módulos

```
MTCQuiz/
├── app/                        # Punto de entrada: Application, MainActivity, NavigationRoot
│
├── core/
│   ├── domain/                 # Modelos compartidos (Category, Question, Evaluation…)
│   │                           # Interfaces: QuizRepository, AuthRepository, PreferenceRepository,
│   │                           # PremiumRepository
│   ├── data/                   # repository/  — QuizRepositoryImpl, AuthRepositoryImpl
│   │                           # billing/     — PremiumRepositoryImpl, BillingLauncher, BillingClientFactory
│   │                           # ads/         — AdsManagerImpl
│   │                           # analytics/   — AnalyticsManager (funnel de compra premium)
│   │                           # adapter/     — FacebookAuthAdapter (sin terminar, ver Estado del proyecto)
│   │                           # local/       — DataStore (preferencias)
│   ├── database/               # Room: MTCDatabase, DAOs, entidades, mappers
│   └── presentation/
│       ├── designsystem/       # Theme, colores, tipografía, componentes reutilizables
│       └── ui/                 # Rutas de navegación, UiText, ObserveAsEvents
│
├── auth/
│   ├── domain/
│   ├── data/
│   └── presentation/           # LoginScreen (código presente; no forma parte del flujo real hoy —
│                                # el gate de isLoggedIn en NavigationRoot está comentado)
│
├── home/
│   ├── domain/
│   ├── data/
│   └── presentation/           # HomeScreen (lista de categorías)
│
├── detail/
│   ├── domain/
│   ├── data/
│   └── presentation/           # DetailScreen (opciones: evaluar, revisar, ver PDF)
│
├── evaluation/
│   ├── domain/
│   ├── data/
│   └── presentation/           # EvaluationScreen (quiz cronometrado) + SummaryScreen
│                                # stats/    — StatsScreen (estadísticas de progreso)
│                                # history/  — HistoryScreen (historial de evaluaciones)
│                                # review/   — ReviewErrorsScreen (repaso de preguntas falladas)
│
├── questionreview/
│   ├── domain/
│   ├── data/
│   └── presentation/           # QuestionsScreen (repaso sin tiempo)
│
├── pdf/
│   ├── domain/
│   ├── data/
│   └── presentation/           # PdfScreen (visor de temario: zoom, descarga vía MediaStore + Snackbar)
│
├── configuration/
│   ├── domain/
│   ├── data/
│   └── presentation/           # ConfigurationScreen — Estadísticas/Historial (mueven a evaluation/*
│                                # arriba), Personalización, Premium, Calificar/Compartir app
│                                # customize/  — CustomizeScreen
│                                # premium/    — PremiumScreen
│                                # term/       — TermScreen (WebView, Términos y condiciones)
│                                # privacy/    — PrivacyScreen (WebView, Política de privacidad)
│                                # tarifas/    — TarifasScreen (Trámites asociados)
│
└── build-logic/
    └── convention/             # Plugins de convención Gradle
```

> `evaluation/presentation` agrupa Stats/History/Review porque comparten `EvaluationRepository`; `configuration/presentation` solo las referencia por navegación (ver Navegación más abajo). Si reorganizás cualquiera de las dos, actualizá este árbol.

---

## Flujo de datos

### Fuentes de datos por tipo

| Dato | Fuente | Módulo responsable |
|---|---|---|
| Categorías de licencia | Lista hardcodeada en Kotlin (`categoriesLocalDataSource`) | `core:data` |
| Preguntas | Archivos JSON en `assets/json/` | `core:data` |
| Evaluaciones (historial) | Room — tabla `evaluations` | `core:database` |
| Preferencias del usuario | DataStore Preferences | `core:data` |
| Estado premium | Google Play Billing (`PremiumRepositoryImpl`), cacheado en DataStore como `StateFlow<Boolean>` | `core:data` |
| Autenticación | Firebase Auth + Google Sign-In (código presente, no gatea el flujo hoy) | `core:data` |
| PDFs del temario | Assets locales, copiados a caché para visualizar y a `MediaStore`/Descargas al exportar | `pdf` |

### Ciclo de una evaluación

```
EvaluationScreenViewModel
  │
  ├─ init: lee preferencias (tiempo, nº preguntas) via PreferenceRepository
  ├─ getCategoryById()          →  categoriesLocalDataSource
  ├─ getQuestionsByCategory()   →  assets/json/<pathJson>.json  →  toma N preguntas si isTake=true
  │
  │  [el usuario responde cada pregunta]
  │
  ├─ saveAnswer()    →  acumula QuestionResult en lista local
  ├─ saveExam()      →  calcula aprobado/reprobado según preferencePercentage
  │                  →  EvaluationDao.upsertEvaluation()  →  Room
  └─ emite EvaluationEvent.EvaluationCreated  →  navega a SummaryScreen
```

---

## Patrón de ViewModel

Todos los ViewModels siguen la misma estructura:

```kotlin
// Estado de UI — data class inmutable
data class EvaluationState(
    val questions: List<Question> = emptyList(),
    val question: Question = Question(),
    val indexQuestion: Int = 0,
    // ...
)

// Acciones del usuario — sealed interface o clase sellada
sealed interface EvaluationAction {  }

// Eventos de un solo disparo (navegación, toasts) — Channel
private var eventChannel = Channel<EvaluationEvent>()
val event = eventChannel.receiveAsFlow()

// Estado expuesto como StateFlow
val state = _state.asStateFlow()
```

En la UI, `ObserveAsEvents` (en `core:presentation:ui`) consume el `Flow` de eventos sin pérdidas asociadas al ciclo de vida.

---

## Navegación

La navegación es completamente **type-safe** usando `@Serializable`, con un único `NavHost` para toda la app:

```kotlin
// core:presentation:ui — Routes.kt
@Serializable data class EvaluationScreenRoute(val categoryId: String)

// app — NavigationRoot.kt  (único NavHost de la app)
composable<EvaluationScreenRoute> {
    val viewModel = hiltViewModel<EvaluationScreenViewModel>()
    EvaluationScreenRoot(viewModel = viewModel)
}
```

Los argumentos de ruta se recuperan en el ViewModel con `savedStateHandle.toRoute<EvaluationScreenRoute>()`.

Screens que viven en un módulo (p. ej. `TermScreenRoute`/`PrivacyScreenRoute` en `configuration/presentation`) igual se registran centralmente en `NavigationRoot.kt` — no hay sub-grafos de navegación por módulo.

---

## Plugins de convención (build-logic)

Para evitar duplicar configuración Gradle, `build-logic/convention` define plugins reutilizables:

| Plugin | Cuándo usarlo |
|---|---|
| `mtcquiz.android.application.compose` | Módulo `:app` |
| `mtcquiz.android.feature.ui` | Módulos `presentation/` de cada feature |
| `mtcquiz.android.library` | Módulos `data/` de features |
| `mtcquiz.jvm.library` | Módulos `domain/` (puro Kotlin) |
| `mtcquiz.android.room` | Módulos que usan Room |
| `mtcquiz.android.hilt` | Cualquier módulo que necesite Hilt |

Un módulo `presentation` típico solo necesita:

```kotlin
plugins {
    alias(libs.plugins.mtcquiz.android.feature.ui)
    alias(libs.plugins.mtcquiz.android.hilt)
}
```

---

## Inyección de dependencias

Hilt gestiona el grafo completo. Cada módulo declara sus propios `@Module`:

- `core:data` → `RepositoryModule`, `AuthRepositoryModule`, `DataStoreModule`, `FirebaseModule`, `BillingModule` (provee `BillingClient` vía `BillingClientFactory`)
- `core:database` → `DataModule` (provee `MTCDatabase` y los DAOs)
- `app` → `AppModule` (dependencias de nivel aplicación)

Los repositorios de `core:domain` son las únicas interfaces que los ViewModels de features conocen; nunca dependen directamente de Room, Firebase o DataStore.

---

## Estado del proyecto (deuda técnica conocida)

- **Autenticación sin gatear el flujo**: `AuthRepositoryImpl` y `FacebookAuthAdapter` existen (este último con métodos `TODO`), pero el chequeo `isLoggedIn` en `NavigationRoot.kt` está comentado — la app siempre entra a `HomeScreenRoute`, sin login obligatorio. Decisión explícita de scope, no un bug.
- **`EvaluationScreenViewModelTest`** tiene ~22 tests vacíos (`// TODO implement test`).
- **Navegación a "About"** es un no-op en `NavigationRoot.kt` (el botón que la disparaba está oculto en Configuración).
