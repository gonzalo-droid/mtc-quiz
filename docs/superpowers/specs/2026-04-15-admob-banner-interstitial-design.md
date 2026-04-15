# Sub-proyecto A — AdMob: banner en Home + intersticial en descarga de PDF

**Fecha:** 2026-04-15
**Autor:** Gonzalo Lozg (con asistencia de Claude Code)
**Estado:** Diseño aprobado — pendiente de plan de implementación

---

## 1. Contexto y motivación

La app MTCQuiz monetiza con AdMob. Hoy existe un banner en `HomeScreen` (commit `5ded9bb WIP: added admob`) usando el test ID de Google, sin garantía de centrado horizontal cuando el ancho del banner adaptive anchored es menor al de la pantalla. No hay intersticiales.

Se quiere:

1. Asegurar que el banner esté **centrado horizontalmente en la parte inferior** de Home.
2. Mostrar un **intersticial** al descargar el PDF desde `PdfScreen`, con un **cap de 1 de cada 3 descargas** para respetar las políticas de AdMob y no saturar al usuario.
3. Parametrizar los **IDs de anuncios por build type** (test en debug, reales en release) sin hard-coding.

## 2. Alcance

### Dentro del alcance
- Centrado visual del banner en `HomeScreen`.
- Nueva abstracción `AdsManager` en `core:domain` + implementación en `core:data`.
- Integración del intersticial en el flujo de descarga de `PdfScreen`.
- Contador de descargas persistente en DataStore.
- IDs de anuncios vía `buildConfigField` en `app/build.gradle.kts`, inyectados a features por Hilt.
- Tests unitarios del contador y la regla de cap.

### Fuera del alcance
- UMP / consentimiento GDPR (requiere spec propio).
- Otros ad slots (evaluación, detalle, home-interstitial).
- Rediseño visual (sub-proyecto C).
- Configuración de mediación o bidding.

## 3. Decisiones de diseño

| Decisión | Opción elegida | Razón |
|---|---|---|
| Timing del intersticial | Antes de iniciar la descarga | Transición natural; el usuario espera algo después del tap. |
| Frecuencia | Cada 3 descargas (cap = 3) | Balancea ingresos y UX. |
| IDs por build type | Debug = test IDs de Google, Release = IDs reales | Evita impresiones accidentales en desarrollo. |
| Persistencia del contador | DataStore Preferences | Ya usado en el proyecto; sobrevive entre sesiones. |
| Ubicación de la abstracción | `core:domain` + `core:data` | Reutiliza módulos existentes; evita scaffolding de un módulo nuevo. |
| Fallback si ad no cargó | Invocar `onDismiss` inmediato | El usuario nunca queda bloqueado; descarga prosigue. |

### IDs de AdMob

| Tipo | Debug (test) | Release (real) |
|---|---|---|
| Banner | `ca-app-pub-3940256099942544/9214589741` | `ca-app-pub-1427341798923689/8361981027` |
| Intersticial | `ca-app-pub-3940256099942544/1033173712` | `ca-app-pub-1427341798923689/3029763296` |

### Regla del cap

```
counter = (counter + 1) persisted to DataStore
shouldShowInterstitial = (counter % 3 == 0)
```

El intersticial se muestra en las descargas 3, 6, 9, 12… Las primeras dos descargas son ad-free, lo cual da margen para que nuevos usuarios tengan una buena primera experiencia.

## 4. Arquitectura

### 4.1 Nueva interfaz — `core:domain`

```kotlin
// core/domain/src/main/java/com/gondroid/core/domain/ads/AdsManager.kt
package com.gondroid.core.domain.ads

import android.app.Activity
import android.content.Context

interface AdsManager {
    /** Precarga el intersticial. Idempotente; si ya hay uno cargado, no hace nada. */
    fun preloadPdfInterstitial(context: Context)

    /** Devuelve true si corresponde mostrar el intersticial en esta descarga. */
    suspend fun shouldShowPdfInterstitial(): Boolean

    /**
     * Muestra el intersticial si está cargado. Al cerrarse (dismiss/fail) invoca [onDismiss].
     * Si el ad no está listo, invoca [onDismiss] inmediatamente. Nunca bloquea la UX.
     */
    fun showPdfInterstitial(activity: Activity, onDismiss: () -> Unit)

    /** Incrementa el contador de descargas en DataStore. */
    suspend fun recordPdfDownload()
}
```

### 4.2 Implementación — `core:data`

```
core/data/src/main/java/com/gondroid/core/data/ads/
  ├── AdsManagerImpl.kt        # orquesta InterstitialAd + contador
  ├── AdsPreferences.kt        # wrapper DataStore para pdf_download_count
  └── di/
      └── AdsModule.kt         # @Binds AdsManager, @Provides @Named("admobBannerId"), etc.
```

**AdsManagerImpl** (esquema):

```kotlin
@Singleton
class AdsManagerImpl @Inject constructor(
    private val prefs: AdsPreferences,
    @Named("admobInterstitialId") private val interstitialId: String,
) : AdsManager {

    private var interstitial: InterstitialAd? = null
    private var isLoading: Boolean = false

    override fun preloadPdfInterstitial(context: Context) {
        if (interstitial != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad; isLoading = false
                }
                override fun onAdFailedToLoad(err: LoadAdError) {
                    interstitial = null; isLoading = false
                }
            }
        )
    }

    override suspend fun shouldShowPdfInterstitial(): Boolean {
        val count = prefs.pdfDownloadCount.first()
        return count > 0 && count % 3 == 0
    }

    override fun showPdfInterstitial(activity: Activity, onDismiss: () -> Unit) {
        val ad = interstitial
        if (ad == null) { onDismiss(); return }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                preloadPdfInterstitial(activity.applicationContext) // siguiente
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                interstitial = null; onDismiss()
            }
        }
        ad.show(activity)
    }

    override suspend fun recordPdfDownload() {
        prefs.incrementPdfDownloadCount()
    }
}
```

**AdsPreferences** — DataStore con clave `pdf_download_count: Int`.

### 4.3 Configuración de IDs — `app/build.gradle.kts`

```kotlin
android {
    buildFeatures { buildConfig = true }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_ID",
                "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
                "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        release {
            // ... config existente (minify, signing, etc.)
            buildConfigField("String", "ADMOB_BANNER_ID",
                "\"ca-app-pub-1427341798923689/8361981027\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
                "\"ca-app-pub-1427341798923689/3029763296\"")
        }
    }
}
```

Y exposición a features vía Hilt:

```kotlin
// app/src/main/java/com/gondroid/mtcquiz/di/AdMobIdsModule.kt
@Module
@InstallIn(SingletonComponent::class)
object AdMobIdsModule {
    @Provides @Named("admobBannerId")
    fun bannerId(): String = BuildConfig.ADMOB_BANNER_ID

    @Provides @Named("admobInterstitialId")
    fun interstitialId(): String = BuildConfig.ADMOB_INTERSTITIAL_ID
}
```

### 4.4 Integración en `pdf:presentation`

**PdfScreenViewModel** recibe `AdsManager` por constructor. Nuevo estado y evento:

```kotlin
sealed interface PdfEvent {
    data object StartDownload : PdfEvent
    data object ShowInterstitial : PdfEvent
}

fun onDownloadClicked() = viewModelScope.launch {
    adsManager.recordPdfDownload()
    if (adsManager.shouldShowPdfInterstitial()) {
        eventChannel.send(PdfEvent.ShowInterstitial)
    } else {
        eventChannel.send(PdfEvent.StartDownload)
    }
}

fun onInterstitialClosed() = viewModelScope.launch {
    eventChannel.send(PdfEvent.StartDownload)
}
```

**PdfScreenRoot** (composable):

```kotlin
val activity = LocalContext.current as Activity

LaunchedEffect(Unit) {
    adsManager.preloadPdfInterstitial(activity)
}

ObserveAsEvents(viewModel.events) { event ->
    when (event) {
        PdfEvent.ShowInterstitial -> adsManager.showPdfInterstitial(activity) {
            viewModel.onInterstitialClosed()
        }
        PdfEvent.StartDownload -> {
            // dispara DownloadPdfIfPermitted() como hoy
        }
    }
}
```

Nota: `AdsManager` se inyecta en `PdfScreenViewModel` por constructor. El composable `PdfScreenRoot` recibe la referencia desde el ViewModel vía un método público (`fun getAdsManager(): AdsManager` o propiedad `val adsManager`). Esto evita usar `EntryPoint` en el composable y mantiene la inyección concentrada en el ViewModel.

### 4.5 Banner centrado — `home:presentation`

**Cambio en `HomeScreen.kt` (alrededor de la línea 90):**

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 8.dp),
    contentAlignment = Alignment.Center,
) {
    BannerAd(adUnitId = bannerAdId)
}
```

**Cambio en `BannerAd.kt`:** el `AndroidView` interno usa `Modifier.wrapContentWidth()` en lugar de `fillMaxWidth()`, para que el Box envolvente centre el banner real.

**Cambio en `HomeScreenRoot`:** sustituir el literal del banner ID por el inyectado:

```kotlin
val bannerAdId: String = hiltViewModel<HomeScreenViewModel>().bannerAdId
```

`HomeScreenViewModel` obtiene el ID por `@Inject @Named("admobBannerId") bannerAdId: String`. Evita acoplar el composable a Hilt directamente.

## 5. Flujo de datos — descarga con intersticial

```
Usuario tap "Descargar"
  │
  ▼
PdfScreen dispatch PdfAction.Downloading
  │
  ▼
PdfScreenViewModel.onDownloadClicked()
  ├─ adsManager.recordPdfDownload()        # counter++ persistido
  ├─ if (shouldShowPdfInterstitial())
  │     emit PdfEvent.ShowInterstitial
  │  else
  │     emit PdfEvent.StartDownload
  │
  ▼
PdfScreenRoot (ObserveAsEvents):
  ├─ ShowInterstitial  →  adsManager.showPdfInterstitial(activity) {
  │                           viewModel.onInterstitialClosed()
  │                       }
  │                       └─ emit PdfEvent.StartDownload
  └─ StartDownload     →  DownloadPdfIfPermitted()  # flujo existente
```

Casos degradados:

- **Ad no cargó:** `showPdfInterstitial` llama `onDismiss` inmediatamente → descarga arranca sin demora.
- **Sin red:** `preloadPdfInterstitial` falla silenciosamente; el `AdsManagerImpl` marca `interstitial = null`; la próxima vez se reintentará en el siguiente preload.
- **Usuario sale antes de que termine el ad:** `FullScreenContentCallback.onAdDismissedFullScreenContent` se dispara igual al cerrar la activity; el siguiente preload se prepara.

## 6. Testing

| Test | Tipo | Qué valida |
|---|---|---|
| `AdsPreferencesTest` | Robolectric + DataStore test | El contador persiste e incrementa correctamente. |
| `AdsManagerImplTest` | Unit (con `AdsPreferences` fake) | `shouldShowPdfInterstitial` devuelve true sólo en counter % 3 == 0; contador empieza en 0; `recordPdfDownload` incrementa. |
| `PdfScreenViewModelTest` | Unit (MockK + Turbine) | Secuencia de eventos: al click → `recordPdfDownload` → según regla emite `ShowInterstitial` o `StartDownload`. |

El SDK de AdMob (`InterstitialAd.load/show`, `MobileAds.initialize`) **no se testea unitariamente**: requiere UI y servicios Google Play. Se valida manualmente en dispositivo:

- Descargar 3 PDFs seguidos → en la 3ª se ve el test interstitial.
- Cerrar y reabrir app → contador sigue en 3 → 4ª descarga sin ad, 6ª con ad.
- Activar modo avión antes de la 3ª descarga → no hay bloqueo, descarga procede.

## 7. Riesgos conocidos

- **Políticas de AdMob:** el cap de 3 descargas y el timing "antes de la acción del usuario" están dentro de lo aceptado por [políticas de AdMob sobre intersticiales](https://support.google.com/admob/answer/6201362).
- **Latencia en la primera descarga:** si el usuario va directo a PDF y descarga inmediatamente, puede que el intersticial aún no esté precargado. En ese caso el fallback invoca `onDismiss` inmediato y la descarga arranca; no hay experiencia degradada.
- **Pérdida de datos del contador:** DataStore es local al dispositivo; si el usuario desinstala y reinstala, el contador vuelve a 0. Es aceptable.

## 8. Criterios de aceptación

- [ ] El banner en Home se ve centrado horizontalmente en la parte inferior en dispositivos con distintos anchos de pantalla (phone pequeño, phone grande, tablet).
- [ ] Los IDs de AdMob varían entre debug y release según `BuildConfig`.
- [ ] Al descargar el PDF por primera vez, no se muestra intersticial; en la 3ª, 6ª, 9ª descarga se muestra el intersticial (con internet).
- [ ] Sin internet, la descarga del PDF procede sin mostrar ad ni bloquear la UI.
- [ ] Los tests unitarios nuevos pasan en CI local (`./gradlew test`).
- [ ] No aparecen regresiones en tests existentes.

---

## Anexo — Archivos a modificar / crear

**Crear:**
- `core/domain/src/main/java/com/gondroid/core/domain/ads/AdsManager.kt`
- `core/data/src/main/java/com/gondroid/core/data/ads/AdsManagerImpl.kt`
- `core/data/src/main/java/com/gondroid/core/data/ads/AdsPreferences.kt`
- `core/data/src/main/java/com/gondroid/core/data/ads/di/AdsModule.kt`
- `app/src/main/java/com/gondroid/mtcquiz/di/AdMobIdsModule.kt`
- `core/data/src/test/java/com/gondroid/core/data/ads/AdsManagerImplTest.kt`

**Modificar:**
- `app/build.gradle.kts` — agregar `buildConfigField` por buildType.
- `home/presentation/.../HomeScreen.kt` — envolver banner en `Box(Alignment.Center)` y usar ID inyectado.
- `home/presentation/.../BannerAd.kt` — cambiar modifier interno.
- `home/presentation/.../HomeScreenViewModel.kt` — recibir `bannerAdId` por inyección.
- `pdf/presentation/.../PdfScreen.kt` — agregar `LaunchedEffect` preload + observación de eventos.
- `pdf/presentation/.../PdfScreenViewModel.kt` — nuevas acciones/eventos + `AdsManager` inyectado.
- `pdf/presentation/build.gradle.kts` y `home/presentation/build.gradle.kts` — agregar dependencia `implementation(project(":core:domain"))` si no la tienen ya.
