# MTCQuiz

Aplicación Android para practicar el examen de reglas de tránsito del Ministerio de Transportes y Comunicaciones del Perú (MTC). Presenta preguntas de selección múltiple por categoría de licencia, registra evaluaciones y permite revisar el temario en PDF.

---

## Tecnologías

| Capa | Herramientas |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Arquitectura | Clean Architecture + MVVM |
| DI | Hilt |
| Base de datos local | Room (evaluaciones) + DataStore (preferencias) |
| Fuente de preguntas | Firebase Realtime Database + JSON en assets |
| Auth | Firebase Authentication + Google Sign-In (Credential Manager) |
| Backend | Firebase Realtime Database, Analytics, Crashlytics |
| Async | Coroutines + Flow |
| Serialización | kotlinx.serialization |
| Monetización | Google AdMob (banner + intersticiales) + Google Play Billing (suscripción premium) |
| Testing | JUnit4, MockK, Turbine, Truth, Robolectric, MockWebServer |

### Stack de versiones principales

| Componente | Versión |
|---|---|
| Kotlin | 2.2.21 |
| KSP | 2.2.21-2.0.5 |
| Android Gradle Plugin | 8.10.0 |
| Hilt | 2.57.2 |
| Room | 2.8.4 |
| Google Play Billing | 7.1.1 |
| Play In-App Review | 2.0.2 |
| Compose BOM | 2025.02.00 |
| Min SDK | 26 |
| Target / Compile SDK | 35 |

> ⚠️ Hilt se mantiene en `2.57.2` porque Hilt `2.59+` requiere AGP `9.0+`. Al actualizar AGP considera subir Hilt en el mismo cambio.

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
│   │                           # Interfaces: QuizRepository, AuthRepository, PreferenceRepository
│   ├── data/                   # Implementaciones: QuizRepositoryImpl, AuthRepositoryImpl
│   │                           # Firebase Auth, Google Sign-In, DataStore
│   ├── database/               # Room: MTCDatabase, DAOs, entidades, mappers
│   └── presentation/
│       ├── designsystem/       # Theme, colores, tipografía, componentes reutilizables
│       └── ui/                 # Rutas de navegación, UiText, ObserveAsEvents
│
├── auth/
│   ├── domain/
│   ├── data/
│   └── presentation/           # LoginScreen
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
│
├── questionreview/
│   ├── domain/
│   ├── data/
│   └── presentation/           # QuestionsScreen (repaso sin tiempo)
│
├── pdf/
│   ├── domain/
│   ├── data/
│   └── presentation/           # PdfScreen (visor de temario)
│
├── configuration/
│   ├── domain/
│   ├── data/
│   └── presentation/           # ConfigurationScreen, CustomizeScreen, TermScreen, PremiumScreen, StatisticsScreen, EvaluationHistoryScreen, ErrorReviewScreen
│
└── build-logic/
    └── convention/             # Plugins de convención Gradle
```

---

## Flujo de datos

### Fuentes de datos por tipo

| Dato | Fuente | Módulo responsable |
|---|---|---|
| Categorías de licencia | Lista hardcodeada en Kotlin (`categoriesLocalDataSource`) | `core:data` |
| Preguntas | Archivos JSON en `assets/json/` | `core:data` |
| Evaluaciones (historial) | Room — tabla `evaluations` | `core:database` |
| Preferencias del usuario | DataStore Preferences | `core:data` |
| Autenticación | Firebase Auth + Google Sign-In | `core:data` |

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

La navegación es completamente **type-safe** usando `@Serializable`:

```kotlin
// core:presentation:ui — Routes.kt
@Serializable data class EvaluationScreenRoute(val categoryId: String)

// app — NavigationRoot.kt  (único NavHost de la app)
composable<EvaluationScreenRoute> {
    val viewModel = hiltViewModel<EvaluationScreenViewModel>()
    EvaluationScreenRoot(viewModel = viewModel)
}
```

Los argumentos de ruta se recuperan en el ViewModel con `savedStateHand, e.toRoute<EvaluationScreenRoute>()`.

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

- `core:data` → `RepositoryModule`, `AuthRepositoryModule`, `DataStoreModule`, `FirebaseModule`
- `core:database` → `DataModule` (provee `MTCDatabase` y los DAOs)
- `app` → `AppModule` (dependencias de nivel aplicación)

Los repositorios de `core:domain` son las únicas interfaces que los ViewModels de features conocen; nunca dependen directamente de Room, Firebase o DataStore.

---

## Levantar el proyecto

### Prerrequisitos

- **JDK 17** instalado y configurado (el proyecto compila a bytecode Java 11/17 según el módulo).
- **Android Studio** Ladybug o superior, con Android SDK 35.
- Un **dispositivo o emulador** con API 26+.
- Un proyecto **Firebase** con:
  - Authentication (Google Sign-In habilitado)
  - Realtime Database (para preguntas y categorías)
  - Analytics y Crashlytics (opcionales pero recomendados)

### Pasos

1. **Clona el repositorio:**
   ```bash
   git clone https://github.com/gonzalo-droid/MTCQuiz.git
   cd MTCQuiz
   ```

2. **Coloca `google-services.json`** (descargado de tu proyecto Firebase) en la carpeta `app/`.

3. **Sincroniza Gradle** desde Android Studio o en consola:
   ```bash
   ./gradlew --refresh-dependencies
   ```

4. **Ejecuta la app** en un dispositivo/emulador con API 26+.

### Comandos útiles

```bash
# Build completo
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Ejecutar tests unitarios (todos los módulos)
./gradlew test

# Tests de un módulo específico
./gradlew :evaluation:domain:test

# Tests instrumentados (requiere dispositivo/emulador conectado)
./gradlew connectedAndroidTest

# Detener todos los daemons de Gradle (útil tras cambios de versión)
./gradlew --stop
```

### Build de release (firmado)

Define estas variables de entorno (o en `~/.gradle/gradle.properties`):

```
MTC_KEYSTORE_PATH=/ruta/absoluta/al/keystore.jks
MTC_KEYSTORE_PASSWORD=...
MTC_KEY_ALIAS=...
MTC_KEY_PASSWORD=...
```

Luego:

```bash
./gradlew assembleRelease
```

---

## Monetización

### AdMob

| Formato | Ubicación | Comportamiento |
|---|---|---|
| Banner | Home (centrado) | Oculto para usuarios premium |
| Intersticial | Antes de descarga PDF | Se muestra cada 3 descargas |
| Intersticial | Antes de iniciar evaluación | Se muestra cada 3 evaluaciones |

Los IDs de anuncios están parametrizados por build type: IDs de prueba en `debug`, IDs reales en `release`. Se configuran como `resValue` en `app/build.gradle.kts`.

### Google Play Billing — Suscripción Premium

- Producto: `mtcquiz_premium_annual` (suscripción anual).
- Verificación client-side con `queryPurchasesAsync()`.
- Estado `isPremium` persistido en DataStore.
- Arquitectura: `BillingManager` en `core/data/billing/`, `SubscriptionRepository` en `core/domain`.
- Al ser premium: banner y ambos intersticiales se omiten completamente.
- Acceso desde la pantalla **Hazte Premium** (gradiente oscuro) en la sección Configuración.
- Tras cerrar un intersticial se muestra un dialog de upsell ("¿Cansado de los anuncios? Suscríbete").

---

## Funcionalidades implementadas

### Estudio y evaluación

| Feature | Descripción |
|---|---|
| Historial de evaluaciones | Pantalla con cards que muestran cada evaluación pasada con badge aprobado/reprobado, fecha, puntaje y categoría. Accesible desde la sección "Mi progreso" en Configuración. |
| Preguntas falladas persistidas | Las preguntas incorrectas se almacenan en Room como columna JSON (`failed_questions`). Migración de esquema v1 → v2 ya aplicada. |
| Repaso de errores | Pantalla dedicada de quiz que muestra únicamente las preguntas que el usuario falló. Accesible desde el detalle de cada evaluación en el historial. |
| Estadísticas de progreso | Tasa de aprobación global, desglose de rendimiento por categoría y total de preguntas respondidas. Accesible desde la sección "Mi progreso". |
| Racha diaria (streak) | Contador de días consecutivos de estudio con icono de fuego visible en HomeScreen. |

### Configuración (secciones)

**Mi progreso**
- Estadísticas de progreso
- Historial de evaluaciones

**Configuración**
- Toggle dark/light mode
- Personalizar valores (número de preguntas, tiempo, porcentaje de aprobación)
- Hazte Premium

**Información**
- Términos y condiciones (WebView con manejo offline + retry)
- Trámites asociados (WebView con manejo offline + retry)
- Calificar la app (In-App Review API + fallback a Play Store)
- Nosotros

### UX y diseño

- **Paleta**: indigo profundo como color primario + amber/gold como acento.
- **Tipografía**: fuente Inter (reemplaza Poppins).
- **Transiciones de navegación**: `slideIn` / `fadeIn` entre pantallas.
- **WebViews**: manejo de estado offline con pantalla de error y botón de reintento.
- **Pantalla Premium**: fondo con gradiente oscuro.

---

## Funcionalidades futuras

Propuestas de diferenciación basadas en análisis de apps competidoras ([DMV Genie](https://driving-tests.org/dmv-genie/), [Zutobi](https://zutobi.com/us), [Drivio](https://apps.apple.com/us/app/drivio-dmv-practice-test-2026/id6748651210)) y del mercado peruano.

### Tier 1 — Alto impacto

| Feature | Descripción | Estado |
|---|---|---|
| Explicación de respuesta correcta | Pre-generar explicaciones con IA (Claude API) para cada pregunta del banco, citando el artículo del Reglamento Nacional de Tránsito. Se almacenan en el JSON de assets como campo `explanation`. Funciona offline, costo único ~$1-2 USD. | Propuesta |
| Modo repaso de errores | Quiz que muestra SOLO las preguntas que el usuario falló en evaluaciones previas. Usa los datos ya persistidos en Room (tabla `evaluations`, columna `question_results`). | Implementado |
| Racha diaria (streak) | Contador de días consecutivos de estudio. Notificación push + badge visual. Modelo Duolingo para retención. | Implementado |
| Estadísticas de progreso | Gráfica de evolución (% aprobación por semana), categorías más débiles, total de preguntas respondidas. | Implementado |

### Tier 2 — Medio-alto impacto

| Feature | Descripción | Estado |
|---|---|---|
| Gamificación con niveles | XP por quiz completado, niveles (Principiante → Experto), badges por logros ("10 evaluaciones aprobadas", "racha de 7 días"). | Propuesta |
| Leaderboard anónimo | Ranking semanal de usuarios por % de aprobación. Requiere Firebase Realtime DB (ya integrado). | Propuesta |
| Señales de tránsito interactivas | Quiz visual: muestra la señal → el usuario responde qué significa. Formato diferente al texto. | Propuesta |

### Tier 3 — Moonshot

| Feature | Descripción | Estado |
|---|---|---|
| AI Tutor | Chatbot "Preguntale a MTCQuiz" que explica reglas de tránsito. Requiere integración con Claude/GPT API en runtime. | Propuesta |
| Modo offline completo | Descargar todo el banco de preguntas + funcionar sin internet. Importante en zonas rurales de Perú. | Propuesta |

### Monetización — Suscripción Premium

| Feature | Descripción | Estado |
|---|---|---|
| Suscripción anual | Integrar Google Play Billing Library (`billing-ktx:7.x`). Producto: `mtcquiz_premium_annual`. Verificación client-side con `queryPurchasesAsync()`. Estado `isPremium` en DataStore. Arquitectura: `BillingManager` interface+impl en `core/data/billing/`, `SubscriptionRepository` en `core/domain`. | Implementado |
| Eliminar ads para premium | Todos los puntos de ads (`AdsManager`) verifican `isPremium` antes de mostrar. Banner en Home, intersticiales en PDF y evaluación se ocultan si el usuario es premium. | Implementado |
| Popup post-ad | Después de cerrar un intersticial, mostrar dialog: "¿Cansado de los anuncios? Suscríbete por S/XX.XX/año". Botones: [Suscribirme] [No, gracias]. Se implementa en `onDismiss` callback del intersticial. | Implementado |
| Item en configuración | Agregar "Premium" en la sección "Configuración" del menú lateral con badge/icono. Muestra estado actual y opción de compra. | Implementado |

---

Hecho con por [@gonzalo-droid](https://github.com/gonzalo-droid)
