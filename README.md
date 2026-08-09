# MTCQuiz

[![Android CI/CD](https://github.com/gonzalo-droid/mtc-quiz/actions/workflows/android.yml/badge.svg)](https://github.com/gonzalo-droid/mtc-quiz/actions/workflows/android.yml)

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
| Google Play Billing | 9.1.0 |
| Play In-App Review | 2.0.2 |
| Compose BOM | 2025.02.00 |
| Robolectric | 4.16.1 |
| ktlint | 11.5.0 |
| Min SDK | 26 |
| Target / Compile SDK | 36 (Android 16) |

> ⚠️ Hilt se mantiene en `2.57.2` porque Hilt `2.59+` requiere AGP `9.0+`. Al actualizar AGP considera subir Hilt en el mismo cambio.
>
> ⚠️ Robolectric `4.16+` (requerido para tests contra SDK 36) necesita **JDK 21** para ejecutar tests — no alcanza con JDK 17.

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

- **JDK 21** instalado y configurado (requerido por Robolectric 4.16+ para correr tests contra SDK 36; el bytecode compilado sigue siendo Java 17).
- **Android Studio** Ladybug o superior, con Android SDK 36.
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

## CI/CD

**[.github/workflows/android.yml](.github/workflows/android.yml)** — corre en cada push a `master` y en cada PR: `ktlintCheck` → `./gradlew test` → `./gradlew assembleDebug`. No requiere ningún GitHub Secret: `API_KEY` tiene un valor placeholder por defecto en el convention plugin (`BuildTypes.kt`) cuando `local.properties` no lo define, y usa un `ci/google-services.dummy.json` committeado (datos falsos, mismo `applicationId`) en vez del `google-services.json` real.

**[.github/workflows/deploy-internal.yml](.github/workflows/deploy-internal.yml)** — deploy a Google Play (track Internal Testing), **disparo 100% manual** (`workflow_dispatch`, nunca en push/PR — publicar es una acción deliberada). Corre `fastlane android internal` (`bundleRelease` + `upload_to_play_store`). Requiere 6 secrets reales (keystore, `google-services.json` real, credenciales de Play Console) que no están configurados todavía — ver `docs/superpowers/plans/2026-08-09-play-store-internal-deploy.md` para el checklist completo.

### Estilo de código

`ktlintCheck`/`ktlintFormat` corren sobre todos los módulos (aplicado vía `subprojects {}` en el `build.gradle.kts` raíz):

```bash
./gradlew ktlintCheck    # solo verifica
./gradlew ktlintFormat   # corrige lo auto-corregible
```

---

## Versionado

`projectVersionCode`/`projectVersionName` (`gradle/libs.versions.toml`) se bumpean **manualmente** en cada release — no hay automatización todavía. `versionCode` es un entero secuencial (+1 por release); `versionName` sigue un esquema aproximado a SemVer, sin ser estricto. Historial completo de cambios por versión: **[CHANGELOG.md](CHANGELOG.md)**.

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

- Productos: `mtcquiz_premium_monthly` (mensual) y `mtcquiz_premium_annual` (anual), vía Google Play Billing Library **9.1.0**.
- Verificación client-side con `queryPurchasesAsync()`; estado de compra pendiente declarado explícitamente con `PendingPurchasesParams` (obligatorio desde Billing Library 8+, aunque la app no venda productos de una sola compra).
- Estado `isPremium` persistido en DataStore, tipado como `StateFlow<Boolean>` (no `Flow`) para que consumidores síncronos como `AdsManagerImpl` lean `.value` sin castear.
- **Arquitectura**: `PremiumRepositoryImpl` (`core/data/billing/`) implementa `PremiumRepository` (`core/domain`, expone `isPremiumFlow`/`availablePlansFlow`) y `BillingLauncher` (`core/data/billing/`, expone `launchSubscription` — separado porque requiere una `Activity` y `core:domain` es Kotlin puro). El `BillingClient` real se construye vía `BillingClientFactory` inyectado por Hilt (`BillingModule`), no inline — permite testear el flujo de compra completo con un `BillingClient` mockeado.
- Al ser premium: banner y ambos intersticiales se omiten completamente.
- Acceso desde la pantalla **Hazte Premium** (gradiente oscuro) en la sección Configuración, con selector de plan mensual/anual y precios reales de Play Console.
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
| Suscripción mensual/anual | Google Play Billing Library `billing-ktx:9.1.0`. Productos: `mtcquiz_premium_monthly`, `mtcquiz_premium_annual`. Verificación client-side con `queryPurchasesAsync()`. Estado `isPremium` en DataStore. Arquitectura: `PremiumRepositoryImpl` implementa `PremiumRepository` + `BillingLauncher`, `BillingClient` inyectado vía `BillingClientFactory`/Hilt. | Implementado |
| Eliminar ads para premium | Todos los puntos de ads (`AdsManager`) verifican `isPremium` antes de mostrar. Banner en Home, intersticiales en PDF y evaluación se ocultan si el usuario es premium. | Implementado |
| Popup post-ad | Después de cerrar un intersticial, mostrar dialog: "¿Cansado de los anuncios? Suscríbete por S/XX.XX/año". Botones: [Suscribirme] [No, gracias]. Se implementa en `onDismiss` callback del intersticial. | Implementado |
| Item en configuración | Agregar "Premium" en la sección "Configuración" del menú lateral con badge/icono. Muestra estado actual y opción de compra. | Implementado |

---

Hecho con por [@gonzalo-droid](https://github.com/gonzalo-droid)
