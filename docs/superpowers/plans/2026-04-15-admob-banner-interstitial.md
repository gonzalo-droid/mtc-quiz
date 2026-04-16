# AdMob: banner en Home + intersticial en descarga de PDF — Plan de implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Centrar el banner de AdMob en Home y agregar un intersticial antes de descargar un PDF, disparado cada 3 intentos, con IDs de AdMob parametrizados por build type.

**Architecture:** Nueva abstracción `AdsManager` (interfaz + impl) ubicada en `core:data/ads/`. Contador persistente en DataStore Preferences. IDs inyectados por Hilt desde el módulo `app` usando `BuildConfig` fields. Integración por eventos en `PdfScreenViewModel`.

**Tech Stack:** Kotlin 2.2.21, Jetpack Compose, Hilt 2.57.2, DataStore Preferences, Google Mobile Ads SDK (`play-services-ads`), Robolectric + MockK + Turbine para tests.

---

## Ajuste respecto al spec

Al revisar patrones del repo, `core:domain` es un módulo **pure JVM** (plugin `mtcquiz.jvm.library`) y no puede referenciar tipos Android (`Context`, `Activity`). Por eso la interfaz `AdsManager` vive en **`core:data/ads/AdsManager.kt`** en vez de `core:domain`. El resto del diseño se mantiene.

---

## Estructura de archivos

**Crear:**
- `core/data/src/main/java/com/gondroid/core/data/ads/AdsManager.kt` — interfaz
- `core/data/src/main/java/com/gondroid/core/data/ads/AdsManagerImpl.kt` — implementación (lógica de cap + SDK AdMob)
- `core/data/src/main/java/com/gondroid/core/data/ads/AdsPreferences.kt` — wrapper DataStore
- `core/data/src/main/java/com/gondroid/core/data/ads/di/AdsModule.kt` — `@Binds` + `@Provides`
- `core/data/src/test/java/com/gondroid/core/data/ads/AdsManagerCounterRuleTest.kt` — tests unitarios
- `core/data/src/test/java/com/gondroid/core/data/ads/FakeAdsPreferences.kt` — fake para tests
- `app/src/main/java/com/gondroid/mtcquiz/di/AdMobIdsModule.kt` — `@Provides @Named` de los IDs
- `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/pdf/PdfScreenViewModelTest.kt` — tests del VM (nuevo — confirmar si existe antes)

**Modificar:**
- `app/build.gradle.kts` — agregar `buildConfigField` en `debug` y `release`
- `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreenViewModel.kt` — agregar `bannerAdId` inyectado
- `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt` — usar ID del VM, envolver banner en `Box(contentAlignment = Center)`
- `home/presentation/src/main/java/com/gondroid/home/presentation/BannerAd.kt` — cambiar `fillMaxWidth()` a `wrapContentWidth()`
- `pdf/presentation/src/main/java/com/gondroid/pdf/presentation/PdfScreenViewModel.kt` — inyectar `AdsManager`, agregar eventos
- `pdf/presentation/src/main/java/com/gondroid/pdf/presentation/PdfScreen.kt` — `LaunchedEffect` preload + `ObserveAsEvents`
- `pdf/presentation/build.gradle.kts` — dependencia a `:core:data` si no la tuviera
- `app/src/main/java/com/gondroid/mtcquiz/presentation/screens/pdf/` u otros tests que deban actualizarse por el nuevo parámetro del VM

---

## Task 1: Configurar `buildConfigField` con los IDs de AdMob por build type

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Leer estado actual del archivo**

Abrir `app/build.gradle.kts`. Identificar el bloque `android { ... }` y dentro los `buildTypes { debug { ... } release { ... } }`. Confirmar si `buildFeatures.buildConfig = true` ya está habilitado. Si no, agregarlo.

- [ ] **Step 2: Agregar `buildConfigField` en debug y release**

Dentro de `android { buildTypes { ... } }`, garantizar lo siguiente:

```kotlin
android {
    buildFeatures {
        buildConfig = true
        // ... resto existente (compose, etc.)
    }

    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_ID",
                "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
                "\"ca-app-pub-3940256099942544/1033173712\"")
        }
        release {
            // ... resto existente (minify, proguard, signing)
            buildConfigField("String", "ADMOB_BANNER_ID",
                "\"ca-app-pub-1427341798923689/8361981027\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID",
                "\"ca-app-pub-1427341798923689/3029763296\"")
        }
    }
}
```

- [ ] **Step 3: Compilar para verificar sintaxis**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Verificar que `BuildConfig.ADMOB_BANNER_ID` y `BuildConfig.ADMOB_INTERSTITIAL_ID` existen**

Abrir `app/build/generated/source/buildConfig/debug/.../BuildConfig.java` y confirmar que aparecen las dos constantes.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts
git commit -m "feat(app): add AdMob ID buildConfigFields per build type"
```

---

## Task 2: Crear `AdsPreferences` (contador persistente en DataStore)

**Files:**
- Create: `core/data/src/main/java/com/gondroid/core/data/ads/AdsPreferences.kt`

- [ ] **Step 1: Revisar patrón existente de DataStore**

Leer `core/data/src/main/java/com/gondroid/core/data/repository/PreferenceRepositoryImpl.kt` para seguir el mismo patrón (constructor recibe `DataStore<Preferences>`, keys en `companion object`, reads vía `.data.map`, writes vía `.edit`).

- [ ] **Step 2: Crear `AdsPreferences.kt`**

Crear el archivo con contenido exacto:

```kotlin
package com.gondroid.core.data.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class AdsPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    open val pdfDownloadCount: Flow<Int> = dataStore.data
        .map { prefs -> prefs[PDF_DOWNLOAD_COUNT] ?: 0 }

    open suspend fun incrementPdfDownloadCount(): Int {
        var newValue = 0
        dataStore.edit { prefs ->
            val current = prefs[PDF_DOWNLOAD_COUNT] ?: 0
            newValue = current + 1
            prefs[PDF_DOWNLOAD_COUNT] = newValue
        }
        return newValue
    }

    open suspend fun currentPdfDownloadCount(): Int = pdfDownloadCount.first()

    private companion object {
        val PDF_DOWNLOAD_COUNT = intPreferencesKey("pdf_download_count")
    }
}
```

La clase se marca `open` y los métodos `open` para permitir que tests unitarios extiendan con un fake (ver Task 4).

- [ ] **Step 3: Compilar**

Run: `./gradlew :core:data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add core/data/src/main/java/com/gondroid/core/data/ads/AdsPreferences.kt
git commit -m "feat(core-data): add AdsPreferences DataStore counter"
```

---

## Task 3: Definir la interfaz `AdsManager`

**Files:**
- Create: `core/data/src/main/java/com/gondroid/core/data/ads/AdsManager.kt`

- [ ] **Step 1: Crear el archivo con la interfaz**

```kotlin
package com.gondroid.core.data.ads

import android.app.Activity
import android.content.Context

interface AdsManager {

    /**
     * Precarga el intersticial para tenerlo listo. Idempotente: si ya hay uno cargado o
     * una carga en curso, no hace nada.
     */
    fun preloadPdfInterstitial(context: Context)

    /**
     * Devuelve true si según la regla de frecuencia (cada 3 descargas) corresponde mostrar
     * el intersticial en esta descarga.
     */
    suspend fun shouldShowPdfInterstitial(): Boolean

    /**
     * Muestra el intersticial precargado. Si está cargado, al cerrarse invoca [onDismiss].
     * Si no está cargado o falla al mostrar, invoca [onDismiss] inmediatamente. Nunca bloquea.
     */
    fun showPdfInterstitial(activity: Activity, onDismiss: () -> Unit)

    /**
     * Incrementa el contador persistente de descargas. Se llama ANTES de decidir si mostrar
     * el intersticial.
     */
    suspend fun recordPdfDownload()
}
```

- [ ] **Step 2: Compilar**

Run: `./gradlew :core:data:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add core/data/src/main/java/com/gondroid/core/data/ads/AdsManager.kt
git commit -m "feat(core-data): add AdsManager interface"
```

---

## Task 4: Implementar lógica de cap (TDD) en `AdsManagerImpl`

**Archivos:**
- Create: `core/data/src/main/java/com/gondroid/core/data/ads/AdsManagerImpl.kt`
- Create: `core/data/src/test/java/com/gondroid/core/data/ads/AdsManagerCounterRuleTest.kt`
- Create: `core/data/src/test/java/com/gondroid/core/data/ads/FakeAdsPreferences.kt`

- [ ] **Step 1: Crear el fake de AdsPreferences**

Crear `core/data/src/test/java/com/gondroid/core/data/ads/FakeAdsPreferences.kt`:

```kotlin
package com.gondroid.core.data.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAdsPreferences : AdsPreferences(dataStore = ThrowingDataStore) {
    private val _count = MutableStateFlow(0)
    override val pdfDownloadCount: Flow<Int> get() = _count

    override suspend fun incrementPdfDownloadCount(): Int {
        _count.value += 1
        return _count.value
    }

    override suspend fun currentPdfDownloadCount(): Int = _count.value
}

private object ThrowingDataStore : DataStore<Preferences> {
    override val data get() = error("FakeAdsPreferences no debe tocar DataStore real")
    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        error("FakeAdsPreferences no debe tocar DataStore real")
}
```

Como `AdsPreferences` ya es `open` desde Task 2, el fake puede extenderla sin tocar la clase real.

- [ ] **Step 2: Escribir el test que debe fallar**

```kotlin
// core/data/src/test/java/com/gondroid/core/data/ads/AdsManagerCounterRuleTest.kt
package com.gondroid.core.data.ads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AdsManagerCounterRuleTest {

    private lateinit var prefs: FakeAdsPreferences
    private lateinit var manager: AdsManagerImpl

    @Before fun setUp() {
        prefs = FakeAdsPreferences()
        manager = AdsManagerImpl(prefs = prefs, interstitialId = "test-id")
    }

    @Test fun `first download does not show interstitial`() = runTest {
        manager.recordPdfDownload()
        assertThat(manager.shouldShowPdfInterstitial()).isFalse()
    }

    @Test fun `second download does not show interstitial`() = runTest {
        repeat(2) { manager.recordPdfDownload() }
        assertThat(manager.shouldShowPdfInterstitial()).isFalse()
    }

    @Test fun `third download shows interstitial`() = runTest {
        repeat(3) { manager.recordPdfDownload() }
        assertThat(manager.shouldShowPdfInterstitial()).isTrue()
    }

    @Test fun `sixth download shows interstitial`() = runTest {
        repeat(6) { manager.recordPdfDownload() }
        assertThat(manager.shouldShowPdfInterstitial()).isTrue()
    }

    @Test fun `counter 0 never shows interstitial`() = runTest {
        assertThat(manager.shouldShowPdfInterstitial()).isFalse()
    }
}
```

- [ ] **Step 3: Correr tests para verificar que fallan**

Run: `./gradlew :core:data:testDebugUnitTest --tests "com.gondroid.core.data.ads.AdsManagerCounterRuleTest"`
Expected: compile error (AdsManagerImpl no existe aún) o 5 tests FAIL.

- [ ] **Step 4: Implementar `AdsManagerImpl` mínima**

```kotlin
// core/data/src/main/java/com/gondroid/core/data/ads/AdsManagerImpl.kt
package com.gondroid.core.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

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
                    interstitial = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(err: LoadAdError) {
                    interstitial = null
                    isLoading = false
                }
            }
        )
    }

    override suspend fun shouldShowPdfInterstitial(): Boolean {
        val count = prefs.currentPdfDownloadCount()
        return count > 0 && count % 3 == 0
    }

    override fun showPdfInterstitial(activity: Activity, onDismiss: () -> Unit) {
        val ad = interstitial
        if (ad == null) {
            onDismiss()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                preloadPdfInterstitial(activity.applicationContext)
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                interstitial = null
                onDismiss()
            }
        }
        ad.show(activity)
    }

    override suspend fun recordPdfDownload() {
        prefs.incrementPdfDownloadCount()
    }
}
```

- [ ] **Step 5: Correr tests y verificar que pasen**

Run: `./gradlew :core:data:testDebugUnitTest --tests "com.gondroid.core.data.ads.AdsManagerCounterRuleTest"`
Expected: 5 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add core/data/src/main/java/com/gondroid/core/data/ads/AdsManagerImpl.kt
git add core/data/src/main/java/com/gondroid/core/data/ads/AdsPreferences.kt
git add core/data/src/test/java/com/gondroid/core/data/ads/
git commit -m "feat(core-data): implement AdsManager with 1-in-3 interstitial cap"
```

---

## Task 5: Wire Hilt — `AdsModule` y `AdMobIdsModule`

**Files:**
- Create: `core/data/src/main/java/com/gondroid/core/data/ads/di/AdsModule.kt`
- Create: `app/src/main/java/com/gondroid/mtcquiz/di/AdMobIdsModule.kt`

- [ ] **Step 1: Crear `AdsModule.kt` en core:data**

```kotlin
package com.gondroid.core.data.ads.di

import com.gondroid.core.data.ads.AdsManager
import com.gondroid.core.data.ads.AdsManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindAdsManager(impl: AdsManagerImpl): AdsManager
}
```

- [ ] **Step 2: Crear `AdMobIdsModule.kt` en app**

```kotlin
package com.gondroid.mtcquiz.di

import com.gondroid.mtcquiz.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdMobIdsModule {

    @Provides
    @Singleton
    @Named("admobBannerId")
    fun provideBannerId(): String = BuildConfig.ADMOB_BANNER_ID

    @Provides
    @Singleton
    @Named("admobInterstitialId")
    fun provideInterstitialId(): String = BuildConfig.ADMOB_INTERSTITIAL_ID
}
```

- [ ] **Step 3: Compilar**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL. Si falla por `Hilt binding missing`, revisar que `AdsManagerImpl` tenga `@Inject constructor` (debe tenerlo desde Task 4).

- [ ] **Step 4: Commit**

```bash
git add core/data/src/main/java/com/gondroid/core/data/ads/di/AdsModule.kt
git add app/src/main/java/com/gondroid/mtcquiz/di/AdMobIdsModule.kt
git commit -m "feat(di): wire AdsManager and AdMob IDs into Hilt graph"
```

---

## Task 6: Centrar banner en Home usando ID inyectado

**Files:**
- Modify: `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreenViewModel.kt`
- Modify: `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt`
- Modify: `home/presentation/src/main/java/com/gondroid/home/presentation/BannerAd.kt`

- [ ] **Step 1: Inyectar `bannerAdId` en `HomeScreenViewModel`**

Editar `HomeScreenViewModel.kt` para agregar el parámetro:

```kotlin
@HiltViewModel
class HomeScreenViewModel
@Inject
constructor(
    private val repository: QuizRepository,
    private val preferenceRepository: PreferenceRepository,
    @Named("admobBannerId") val bannerAdId: String,
) : ViewModel() {
    // ... resto sin cambios
}
```

Agregar el import: `import javax.inject.Named`.

- [ ] **Step 2: Actualizar fakes de tests si aplica**

Abrir `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/home/HomeScreenViewModelTest.kt`. El constructor ahora requiere `bannerAdId`. En `setUp()`, pasar un valor dummy:

```kotlin
viewModel = HomeScreenViewModel(
    repository = repository,
    preferenceRepository = preferenceRepository,
    bannerAdId = "test-banner-id",
)
```

- [ ] **Step 3: Correr tests existentes de HomeScreenViewModel**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gondroid.mtcquiz.presentation.screens.home.HomeScreenViewModelTest"`
Expected: todos los tests existentes siguen pasando.

- [ ] **Step 4: Cambiar `BannerAd.kt` a `wrapContentWidth`**

```kotlin
package com.gondroid.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.ads.AdView

@Composable
fun BannerAd(adView: AdView, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box { Text(text = "Google Mobile Ads preview banner.", modifier.align(Alignment.Center)) }
        return
    }

    AndroidView(modifier = modifier.wrapContentWidth(), factory = { adView })

    LifecycleResumeEffect(adView) {
        adView.resume()
        onPauseOrDispose { adView.pause() }
    }
}
```

- [ ] **Step 5: Actualizar `HomeScreen.kt` — usar ID del VM y envolver en `Box(Alignment.Center)`**

En `HomeScreenRoot` reemplazar la línea:

```kotlin
adView.adUnitId = "ca-app-pub-3940256099942544/9214589741"
```

por:

```kotlin
adView.adUnitId = viewModel.bannerAdId
```

Y dentro del bloque `content = { ... }`, reemplazar el `Column { Box { BannerAd(...) } }` por:

```kotlin
content = {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            BannerAd(adView, Modifier)
        }
    }
}
```

Agregar los imports necesarios: `import androidx.compose.foundation.layout.padding`, `import androidx.compose.ui.Alignment`, `import androidx.compose.ui.unit.dp`.

- [ ] **Step 6: Compilar**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add home/presentation/src/main/java/com/gondroid/home/presentation/
git add app/src/test/java/com/gondroid/mtcquiz/presentation/screens/home/HomeScreenViewModelTest.kt
git commit -m "feat(home): inject AdMob banner ID and center banner horizontally"
```

---

## Task 7: Modificar `PdfScreenViewModel` para integrar `AdsManager` (TDD)

**Files:**
- Modify: `pdf/presentation/src/main/java/com/gondroid/pdf/presentation/PdfScreenViewModel.kt`
- Modify: `pdf/presentation/build.gradle.kts` (verificar dep a `:core:data`)
- Create (si no existe): `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/pdf/PdfScreenViewModelTest.kt`

- [ ] **Step 1: Verificar que `:pdf:presentation` depende de `:core:data`**

Abrir `pdf/presentation/build.gradle.kts`. Confirmar que existe `implementation(project(":core:data"))`. Si no, agregarlo.

- [ ] **Step 2: Agregar `PdfEvent` sealed interface al archivo del VM**

Al inicio de `PdfScreenViewModel.kt` (fuera de la clase, bajo los imports):

```kotlin
sealed interface PdfEvent {
    data object StartDownload : PdfEvent
    data object ShowInterstitial : PdfEvent
}
```

- [ ] **Step 3: Inyectar `AdsManager` en el VM y agregar canal de eventos**

Modificar el constructor y los miembros:

```kotlin
@HiltViewModel
class PdfScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
    val adsManager: AdsManager,
) : ViewModel() {

    private var _state = MutableStateFlow(PdfState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<PdfEvent>()
    val events: Flow<PdfEvent> = eventChannel.receiveAsFlow()

    // ... resto del código existente
}
```

Nota: `adsManager` se declara `val` (público) en vez de `private val` para que `PdfScreenRoot` pueda invocar `viewModel.adsManager.showPdfInterstitial()` sin EntryPoints adicionales. Ver Task 8.

Imports nuevos: `import com.gondroid.core.data.ads.AdsManager`, `import kotlinx.coroutines.channels.Channel`, `import kotlinx.coroutines.flow.Flow`, `import kotlinx.coroutines.flow.receiveAsFlow`.

- [ ] **Step 4: Agregar métodos nuevos al VM**

Reemplazar/ampliar el método `downloading(value: Boolean)` con:

```kotlin
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

fun onDownloadFinished() {
    _state.update { it.copy(shouldDownload = false, isLoading = false) }
}

fun onDownloadStarting() {
    _state.update { it.copy(shouldDownload = true, isLoading = true) }
}
```

Mantener `downloading(value: Boolean)` por compatibilidad con otros callers si existen (buscar con Grep). Marcarla como `@Deprecated` y delegar a los nuevos:

```kotlin
@Deprecated("Use onDownloadClicked / onDownloadFinished instead")
fun downloading(value: Boolean) {
    if (value) onDownloadStarting() else onDownloadFinished()
}
```

Imports necesarios: `import kotlinx.coroutines.launch`.

- [ ] **Step 5: Escribir tests del VM**

Crear `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/pdf/PdfScreenViewModelTest.kt`:

```kotlin
package com.gondroid.mtcquiz.presentation.screens.pdf

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.gondroid.core.data.ads.AdsManager
import com.gondroid.pdf.presentation.PdfEvent
import com.gondroid.pdf.presentation.PdfScreenViewModel
import com.gondroid.mtcquiz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PdfScreenViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val adsManager: AdsManager = mockk(relaxed = true)
    private val repository = FakeQuizRepositoryForPdf()   // crear fake mínimo o usar uno existente
    private val savedStateHandle = SavedStateHandle(mapOf("categoryId" to "A1"))

    private fun createViewModel() = PdfScreenViewModel(
        savedStateHandle = savedStateHandle,
        repository = repository,
        adsManager = adsManager,
    )

    @Test fun `onDownloadClicked records download and emits StartDownload when cap not met`() = runTest {
        coEvery { adsManager.shouldShowPdfInterstitial() } returns false
        val vm = createViewModel()

        vm.events.test {
            vm.onDownloadClicked()
            assertThat(awaitItem()).isEqualTo(PdfEvent.StartDownload)
        }
        coVerify { adsManager.recordPdfDownload() }
    }

    @Test fun `onDownloadClicked emits ShowInterstitial when cap is met`() = runTest {
        coEvery { adsManager.shouldShowPdfInterstitial() } returns true
        val vm = createViewModel()

        vm.events.test {
            vm.onDownloadClicked()
            assertThat(awaitItem()).isEqualTo(PdfEvent.ShowInterstitial)
        }
    }

    @Test fun `onInterstitialClosed emits StartDownload`() = runTest {
        val vm = createViewModel()
        vm.events.test {
            vm.onInterstitialClosed()
            assertThat(awaitItem()).isEqualTo(PdfEvent.StartDownload)
        }
    }
}
```

Nota: el fake `FakeQuizRepositoryForPdf` debe implementar sólo los métodos que use el VM de PDF (probablemente `getCategoryById`). Si ya hay un `QuizRepositoryFake` en el repo, reutilizarlo. Buscar con: `grep -r "QuizRepositoryFake" app/src/test` y usar esa clase.

- [ ] **Step 6: Correr tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gondroid.mtcquiz.presentation.screens.pdf.PdfScreenViewModelTest"`
Expected: 3 tests PASS.

- [ ] **Step 7: Compilar módulo completo**

Run: `./gradlew :pdf:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit**

```bash
git add pdf/presentation/src/main/java/com/gondroid/pdf/presentation/PdfScreenViewModel.kt
git add pdf/presentation/build.gradle.kts
git add app/src/test/java/com/gondroid/mtcquiz/presentation/screens/pdf/PdfScreenViewModelTest.kt
git commit -m "feat(pdf): integrate AdsManager event flow into PdfScreenViewModel"
```

---

## Task 8: Conectar `PdfScreen` al flujo de eventos

**Files:**
- Modify: `pdf/presentation/src/main/java/com/gondroid/pdf/presentation/PdfScreen.kt`

- [ ] **Step 1: Agregar `LaunchedEffect` para precargar al entrar a la pantalla**

El `AdsManager` ya se expone como `val adsManager` en el VM (Task 7). No hace falta tocar firmas de composables ni `NavigationRoot.kt`.

Dentro de `PdfScreenRoot`, debajo de los `val state by ... `:

```kotlin
val context = LocalContext.current
LaunchedEffect(Unit) {
    viewModel.adsManager.preloadPdfInterstitial(context)
}
```

- [ ] **Step 2: Observar eventos del VM y manejar los casos**

Justo después del LaunchedEffect, agregar:

```kotlin
val activity = LocalContext.current as? Activity

ObserveAsEvents(viewModel.events) { event ->
    when (event) {
        PdfEvent.ShowInterstitial -> {
            val act = activity
            if (act != null) {
                viewModel.adsManager.showPdfInterstitial(act) {
                    viewModel.onInterstitialClosed()
                }
            } else {
                viewModel.onInterstitialClosed()
            }
        }
        PdfEvent.StartDownload -> viewModel.onDownloadStarting()
    }
}
```

Imports: `import android.app.Activity`, `import androidx.compose.runtime.LaunchedEffect`, `import com.gondroid.core.presentation.ui.ObserveAsEvents`, `import com.gondroid.pdf.presentation.PdfEvent`.

Verificar que `:pdf:presentation/build.gradle.kts` ya declara `implementation(project(":core:presentation:ui"))`. Si no, agregarlo.

- [ ] **Step 3: Cambiar el handler del botón descargar**

En `PdfScreen` (el composable interno no Root), dentro de `actions = { Row(modifier = Modifier.clickable { onAction(PdfAction.Downloading) }) ... }` el action sigue igual. En el `onAction` del Root (alrededor de la línea 157-164), modificar:

```kotlin
onAction = { action ->
    when (action) {
        is PdfAction.Back -> navigateBack()
        PdfAction.Downloading -> viewModel.onDownloadClicked()  // era: viewModel.downloading(true)
    }
},
```

- [ ] **Step 4: Actualizar el consumidor de `state.shouldDownload`**

Las líneas 111-122 actuales (`DownloadPdfIfPermitted`) siguen igual, pero el callback de éxito debe llamar `viewModel.onDownloadFinished()` en lugar de `viewModel.downloading(false)`:

```kotlin
if (state.shouldDownload) {
    DownloadPdfIfPermitted(
        context = context,
        nameFile = state.category.pdf
    ) { success ->
        showMessage = if (success) context.getString(R.string.success_download_pdf)
                      else context.getString(R.string.failure_download_pdf)
        viewModel.onDownloadFinished()
    }
}
```

- [ ] **Step 5: Compilar**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Correr toda la suite de tests**

Run: `./gradlew test`
Expected: todos los tests PASS.

- [ ] **Step 7: Commit**

```bash
git add pdf/presentation/src/main/java/com/gondroid/pdf/presentation/PdfScreen.kt
git add pdf/presentation/src/main/java/com/gondroid/pdf/presentation/PdfScreenViewModel.kt
git commit -m "feat(pdf): show interstitial before download via event bus"
```

---

## Task 9: Verificación manual en dispositivo

**Files:** ninguno — checklist de QA.

- [ ] **Step 1: Instalar APK debug en un dispositivo físico con internet**

Run: `./gradlew :app:installDebug`

- [ ] **Step 2: Validar banner en Home**

Abrir la app → pantalla Home. Comprobar:
- [ ] El banner aparece **centrado horizontalmente** en la parte inferior (visible en teléfonos chicos y grandes).
- [ ] No se corta ni se pega al borde.
- [ ] En LogCat aparecen los eventos `onAdLoaded` del banner (test ID).

- [ ] **Step 3: Validar intersticial de PDF**

Ir a una categoría → abrir PDF. Descargar el PDF **3 veces seguidas** (la app permite re-entrar). En la 3ª descarga debe aparecer el intersticial de prueba. Cerrarlo → comprobar que el Toast "descarga exitosa" aparece y el archivo queda en Downloads.

Luego:
- [ ] 4ª y 5ª descarga: sin intersticial.
- [ ] 6ª descarga: intersticial.

- [ ] **Step 4: Validar caso sin internet**

Activar modo avión. Descargar PDF una vez (cuando correspondería intersticial):
- [ ] La descarga procede sin bloquearse.
- [ ] No aparece intersticial (al no haber red no cargó).
- [ ] No se imprimen crashes en LogCat.

- [ ] **Step 5: Validar persistencia del contador**

Con el contador en N (p.ej. 4 después de una sesión previa): cerrar app (swipe away), reabrir, descargar 2 PDFs más. En la 2ª descarga (contador = 6) debe aparecer el intersticial.

- [ ] **Step 6: Registro manual del resultado**

Si algo falla, abrir issue/commit con detalles. Si todo OK, continuar.

---

## Task 10: Actualizar memoria del proyecto

**Files:**
- Create: `~/.claude/projects/-Users-gonzalo-AndroidStudioProjects-MTCQuiz/memory/project_ads_architecture.md`
- Modify: `~/.claude/projects/-Users-gonzalo-AndroidStudioProjects-MTCQuiz/memory/MEMORY.md`

- [ ] **Step 1: Escribir memoria de arquitectura de ads**

Crear archivo con frontmatter y contenido:

```markdown
---
name: Ads architecture lives in core:data with AdsManager
description: AdMob integration uses an AdsManager interface in core:data; interstitial has a 1-in-3 cap stored in DataStore; IDs are injected per build type
type: project
---

AdMob integration structure:

- `AdsManager` interface + `AdsManagerImpl` live in `core/data/src/main/java/com/gondroid/core/data/ads/`.
- Counter persistence in DataStore via `AdsPreferences` (key `pdf_download_count`).
- Rule: show interstitial when `count > 0 && count % 3 == 0` (downloads 3, 6, 9…).
- Ad unit IDs injected via Hilt `@Named("admobBannerId")` and `@Named("admobInterstitialId")`; values come from `BuildConfig` fields set in `app/build.gradle.kts` (debug = test IDs, release = real IDs).

**Why:** Designed in spec `docs/superpowers/specs/2026-04-15-admob-banner-interstitial-design.md`. Single abstraction makes it easy to add more ad slots (home interstitial, evaluation banner) later.

**How to apply:** When adding a new ad spot, extend `AdsManager` with new methods rather than creating a parallel abstraction. When the user asks to change ad frequency or add new ad IDs, modify `AdsManagerImpl.shouldShowPdfInterstitial` and `AdMobIdsModule` respectively.
```

- [ ] **Step 2: Agregar línea al índice**

Editar `MEMORY.md` y añadir:

```
- [Ads architecture (AdsManager + DataStore cap)](project_ads_architecture.md) — AdMob lives in core:data; 1-in-3 interstitial cap; IDs by build type
```

---

## Criterios globales de aceptación

- [ ] `./gradlew build` pasa localmente.
- [ ] `./gradlew test` pasa localmente (tests nuevos incluidos).
- [ ] Banner en Home se ve centrado en dispositivos chicos y grandes.
- [ ] Intersticial aparece exactamente en las descargas 3, 6, 9 con internet.
- [ ] Sin internet, la descarga nunca se bloquea.
- [ ] Memoria actualizada.
- [ ] 6-7 commits independientes con mensajes convencionales.
