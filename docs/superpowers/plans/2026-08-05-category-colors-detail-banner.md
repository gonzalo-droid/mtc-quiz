# Category Colors, Detail Banner Ad, Small-Screen Scroll Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Give each Home category card a distinct, class-grouped color; show the AdMob banner ad on the Detail screen (matching Home); fix a pre-existing category data bug found along the way; and make Home/Detail scroll instead of clipping content on short screens.

**Architecture:** Four independent-but-related changes to the `home/presentation` and `detail/presentation` feature modules, plus one data fix in `core:data` and one shared-UI extraction into `core:presentation:ui` (a module both feature modules already depend on) so the banner-ad lifecycle logic isn't duplicated.

**Tech Stack:** Jetpack Compose, Kotlin, Hilt DI, JUnit4 + Truth for JVM unit tests (no Robolectric needed — `androidx.compose.ui.graphics.Color` and plain data classes are pure Kotlin).

## Global Constraints

- Category color palette (grouped by license class, approved via visual companion):

  | `category` code | Container color | Content color |
  |---|---|---|
  | `A-I` | `#274C93` | white |
  | `A-IIa` | `#3461B3` | white |
  | `A-IIb` | `#3F76D6` | white |
  | `A-IIIa` | `#5C8CE0` | white |
  | `A-IIIb` | `#7BA3E8` | `#12233F` |
  | `A-IIIc` | `#9EBCEF` | `#12233F` |
  | `B-IIa` | `#B5651D` | white |
  | `B-IIb` | `#D07A2B` | white |
  | `B-IIc` | `#E89A4D` | `#12233F` |

- Color scope is Home only — Detail screen keeps its current styling (per approved design).
- `CategoryLocalDataSource`'s `id="8"/"9"/"10"` are NOT renumbered (may already be persisted in `Evaluation.categoryId` in Room).
- The banner ad is gated by `!isPremium`, matching Home's existing convention exactly.
- `@Named("admobBannerId")` is already bound app-wide at `SingletonComponent` scope (`app/src/main/java/com/gondroid/mtcquiz/di/AdMobIdsModule.kt`) — no new Hilt module needed.

---

### Task 1: Category color palette

**Files:**
- Create: `home/presentation/src/main/java/com/gondroid/home/presentation/CategoryColors.kt`
- Create: `home/presentation/src/test/java/com/gondroid/home/presentation/CategoryColorsTest.kt`
- Modify: `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt:282-356` (the `CardCategoryItem` composable)

**Interfaces:**
- Produces: `data class CategoryColorScheme(val container: Color, val content: Color)` and `fun categoryColors(category: String, fallback: CategoryColorScheme): CategoryColorScheme` — both in package `com.gondroid.home.presentation`. Consumed by `CardCategoryItem` in this same task.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gondroid.home.presentation

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryColorsTest {

    private val fallback = CategoryColorScheme(container = Color.Gray, content = Color.Black)

    @Test
    fun `known category codes map to their documented colors`() {
        val expected = mapOf(
            "A-I" to CategoryColorScheme(Color(0xFF274C93), Color.White),
            "A-IIa" to CategoryColorScheme(Color(0xFF3461B3), Color.White),
            "A-IIb" to CategoryColorScheme(Color(0xFF3F76D6), Color.White),
            "A-IIIa" to CategoryColorScheme(Color(0xFF5C8CE0), Color.White),
            "A-IIIb" to CategoryColorScheme(Color(0xFF7BA3E8), Color(0xFF12233F)),
            "A-IIIc" to CategoryColorScheme(Color(0xFF9EBCEF), Color(0xFF12233F)),
            "B-IIa" to CategoryColorScheme(Color(0xFFB5651D), Color.White),
            "B-IIb" to CategoryColorScheme(Color(0xFFD07A2B), Color.White),
            "B-IIc" to CategoryColorScheme(Color(0xFFE89A4D), Color(0xFF12233F)),
        )
        expected.forEach { (code, colorScheme) ->
            assertThat(categoryColors(code, fallback)).isEqualTo(colorScheme)
        }
    }

    @Test
    fun `unknown category code falls back to the provided default`() {
        assertThat(categoryColors("Z-999", fallback)).isEqualTo(fallback)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :home:presentation:testDebugUnitTest --tests "com.gondroid.home.presentation.CategoryColorsTest"`
Expected: FAIL — `categoryColors`/`CategoryColorScheme` not defined yet.

- [ ] **Step 3: Write `CategoryColors.kt`**

```kotlin
package com.gondroid.home.presentation

import androidx.compose.ui.graphics.Color

data class CategoryColorScheme(
    val container: Color,
    val content: Color,
)

private val categoryColorMap = mapOf(
    "A-I" to CategoryColorScheme(Color(0xFF274C93), Color.White),
    "A-IIa" to CategoryColorScheme(Color(0xFF3461B3), Color.White),
    "A-IIb" to CategoryColorScheme(Color(0xFF3F76D6), Color.White),
    "A-IIIa" to CategoryColorScheme(Color(0xFF5C8CE0), Color.White),
    "A-IIIb" to CategoryColorScheme(Color(0xFF7BA3E8), Color(0xFF12233F)),
    "A-IIIc" to CategoryColorScheme(Color(0xFF9EBCEF), Color(0xFF12233F)),
    "B-IIa" to CategoryColorScheme(Color(0xFFB5651D), Color.White),
    "B-IIb" to CategoryColorScheme(Color(0xFFD07A2B), Color.White),
    "B-IIc" to CategoryColorScheme(Color(0xFFE89A4D), Color(0xFF12233F)),
)

fun categoryColors(category: String, fallback: CategoryColorScheme): CategoryColorScheme =
    categoryColorMap[category] ?: fallback
```

`categoryColors` is a plain function (not `@Composable`) so it stays trivially unit-testable — the caller reads `MaterialTheme.colorScheme` and passes it as `fallback`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :home:presentation:testDebugUnitTest --tests "com.gondroid.home.presentation.CategoryColorsTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Wire `categoryColors` into `CardCategoryItem`**

In `HomeScreen.kt`, `CardCategoryItem` currently (lines 292-345) uses a fixed
`CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)`
and `MaterialTheme.colorScheme.onPrimary` for all three `Text` calls inside.
Replace:

```kotlin
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .height(400.dp)
```

with:

```kotlin
    val colors = categoryColors(
        category = item.category,
        fallback = CategoryColorScheme(
            container = MaterialTheme.colorScheme.primary,
            content = MaterialTheme.colorScheme.onPrimary,
        ),
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = colors.container,
            contentColor = colors.content,
        ),
        modifier = Modifier
            .height(400.dp)
```

Then replace all three occurrences of `color = MaterialTheme.colorScheme.onPrimary` inside this
same `CardCategoryItem` function (the `classType` Text at line ~323, the `category` Text at
line ~330, and the `description` Text at line ~342) with `color = colors.content`.

- [ ] **Step 6: Build and manually verify**

Run: `./gradlew :home:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

Then install the debug build (`./gradlew installDebug`) on a running emulator/device, open the
Home screen, and swipe through the category pager to confirm each card shows its documented
color from the Global Constraints table (blue family for Class A, darkening per sub-category;
warm/orange family for Class B) with readable text.

- [ ] **Step 7: Commit**

```bash
git add home/presentation/src/main/java/com/gondroid/home/presentation/CategoryColors.kt home/presentation/src/test/java/com/gondroid/home/presentation/CategoryColorsTest.kt home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt
git commit -m "feat: color-code Home category cards by license class"
```

---

### Task 2: Fix category data bug (wrong pathJson, phantom B-I category)

**Files:**
- Modify: `core/data/src/main/java/com/gondroid/core/data/local/CategoryLocalDataSource.kt`
- Create: `core/data/src/test/java/com/gondroid/core/data/local/CategoryLocalDataSourceTest.kt`

**Interfaces:**
- Consumes: nothing from Task 1 (independent).
- Produces: `categoriesLocalDataSource` (unchanged name/type — `List<Category>`) now has 9 entries instead of 10, with corrected `pathJson` values on the 3 Class B sub-categories. Consumed by `QuizRepositoryImpl` (already existing code, no changes needed there).

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gondroid.core.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryLocalDataSourceTest {

    @Test
    fun `list has exactly 9 categories, no unimplemented B-I placeholder`() {
        assertThat(categoriesLocalDataSource).hasSize(9)
        assertThat(categoriesLocalDataSource.map { it.category }).doesNotContain("B-I")
    }

    @Test
    fun `class B categories point at their own balotario JSON, not a1`() {
        val byCategory = categoriesLocalDataSource.associateBy { it.category }
        assertThat(byCategory.getValue("B-IIa").pathJson).isEqualTo("b2a_questions.json")
        assertThat(byCategory.getValue("B-IIb").pathJson).isEqualTo("b2b_questions.json")
        assertThat(byCategory.getValue("B-IIc").pathJson).isEqualTo("b2c_questions.json")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:data:testDebugUnitTest --tests "com.gondroid.core.data.local.CategoryLocalDataSourceTest"`
Expected: FAIL — list currently has 10 entries and `B-IIa`/`B-IIb`/`B-IIc` all have `pathJson = "a1_questions.json"`.

- [ ] **Step 3: Fix `CategoryLocalDataSource.kt`**

Remove the `id="7"` (`B-I`) entry entirely — it has no real balotario PDF/JSON behind it
(its `pdf`/`pathJson` currently point at `CLASE_A_I.pdf`/`a2a_questions.json`, which is wrong
data, not a real B-I balotario). Add a one-line comment above the Class B block explaining the
gap so a future reader isn't confused that Class A has 6 sub-categories but Class B only has 3:

```kotlin
    /**
     * Class licence B
     * (B-I / triciclos has no real balotario PDF yet, intentionally omitted)
     */
```

Then fix the three `pathJson` values (leave everything else on `id="8"`, `id="9"`, `id="10"`
unchanged — their `pdf` fields are already correct):

```kotlin
    Category(
        id = "8",
        title = "CLASE B - CATEGORIA II-A",
        category = "B-IIa",
        classType = CLASS_B,
        description = "Bicimotos para transportar pasajeros o mercancías.",
        image = CardTypeEnum.A1.drawable,
        pdf = "CLASE_B_IIA.pdf",
        pathJson = "b2a_questions.json"
    ),
    Category(
        id = "9",
        title = "CLASE B - CATEGORIA II-B",
        category = "B-IIb",
        classType = CLASS_B,
        description = "Los mismos que B-IIa y también Motocicletas (2 ruedas) o Motocicletas con Sidecar (3 ruedas) para transportar pasajeros o mercancías.",
        image = CardTypeEnum.A1.drawable,
        pdf = "CLASE_B_IIB.pdf",
        pathJson = "b2b_questions.json"
    ),
    Category(
        id = "10",
        title = "CLASE B - CATEGORIA II-C",
        category = "B-IIc",
        classType = CLASS_B,
        description = "Los mismos que B-IIa y B-IIb y también Mototaxis y Trimotos (3 ruedas) destinadas al transporte de pasajeros",
        image = CardTypeEnum.A1.drawable,
        pdf = "CLASE_B_IIC.pdf",
        pathJson = "b2c_questions.json"
    ),
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:data:testDebugUnitTest --tests "com.gondroid.core.data.local.CategoryLocalDataSourceTest"`
Expected: PASS (2 tests).

- [ ] **Step 5: Run the broader `core:data` and `app` test suites to catch any other reference to the removed `B-I`/`id="7"` category**

Run: `./gradlew :core:data:test :app:test`
Expected: BUILD SUCCESSFUL. If anything references `id="7"` or category `"B-I"` (e.g. a stale
persisted `Evaluation.categoryId = "7"` fixture in a test), fix that call site now — but do not
re-add the `B-I` category to satisfy a test; the removal is intentional.

- [ ] **Step 6: Commit**

```bash
git add core/data/src/main/java/com/gondroid/core/data/local/CategoryLocalDataSource.kt core/data/src/test/java/com/gondroid/core/data/local/CategoryLocalDataSourceTest.kt
git commit -m "fix: correct B-license category pathJson, drop unimplemented B-I"
```

---

### Task 3: Extract shared `BannerAdSlot`, wire into Home, add Home scroll fix

**Files:**
- Create: `core/presentation/ui/src/main/java/com/gondroid/core/presentation/ui/BannerAdView.kt`
- Create: `core/presentation/ui/src/main/java/com/gondroid/core/presentation/ui/BannerAdSlot.kt`
- Modify: `core/presentation/ui/build.gradle.kts`
- Delete: `home/presentation/src/main/java/com/gondroid/home/presentation/BannerAd.kt`
- Modify: `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt` (`HomeScreenRoot` lines 71-150, and the content `Column` at lines 205-278)
- Modify: `home/presentation/build.gradle.kts` (drop now-unused admob dependency)

**Interfaces:**
- Consumes: nothing from Tasks 1/2 (independent; can run in parallel with them).
- Produces: `@Composable fun BannerAdSlot(bannerAdId: String, isPremium: Boolean, modifier: Modifier = Modifier)` in package `com.gondroid.core.presentation.ui`. Consumed by `HomeScreenRoot` in this task, and by `DetailScreenRoot`/`DetailScreen` in Task 4.

- [ ] **Step 1: Add the AdMob dependency to `core:presentation:ui`**

In `core/presentation/ui/build.gradle.kts`, inside the `dependencies { }` block, add:

```kotlin
    implementation(libs.play.services.admob)
```

- [ ] **Step 2: Move the `BannerAd` composable to `core:presentation:ui` as `BannerAdView`**

Create `core/presentation/ui/src/main/java/com/gondroid/core/presentation/ui/BannerAdView.kt`
with the same logic as today's `home/presentation/.../BannerAd.kt`, just renamed and re-packaged
(the rename avoids confusion with the new higher-level `BannerAdSlot`):

```kotlin
package com.gondroid.core.presentation.ui

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

/**
 * Renders a loaded [AdView]. Callers own the AdView's lifecycle (creation,
 * loadAd, destroy) — see [BannerAdSlot] for the usual entry point.
 */
@Composable
fun BannerAdView(adView: AdView, modifier: Modifier = Modifier) {
    // Ad load does not work in preview mode because it requires a network connection.
    if (LocalInspectionMode.current) {
        Box { Text(text = "Google Mobile Ads preview banner.", modifier.align(Alignment.Center)) }
        return
    }

    AndroidView(modifier = modifier.wrapContentWidth(), factory = { adView })

    // Pause and resume the AdView when the lifecycle is paused and resumed.
    LifecycleResumeEffect(adView) {
        adView.resume()
        onPauseOrDispose { adView.pause() }
    }
}
```

- [ ] **Step 3: Write `BannerAdSlot.kt`**

This absorbs the AdView creation/load/dispose block that currently lives inline in
`HomeScreenRoot` (`HomeScreen.kt:81-117,145-148`), plus the premium gate and the centered `Box`
wrapper (`HomeScreen.kt:128-141`):

```kotlin
package com.gondroid.core.presentation.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Creates, loads, and tears down a banner [AdView] for [bannerAdId], and
 * renders it centered at the bottom of whatever content this is placed in.
 * Renders nothing when [isPremium] is true.
 */
@Composable
fun BannerAdSlot(bannerAdId: String, isPremium: Boolean, modifier: Modifier = Modifier) {
    if (isPremium) return

    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            adUnitId = bannerAdId
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 360)
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("adMobTest", "Banner ad was loaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("adMobTest", "Banner ad failed to load: ${error.message}")
                }

                override fun onAdImpression() {
                    Log.d("adMobTest", "Banner ad recorded an impression.")
                }

                override fun onAdClicked() {
                    Log.d("adMobTest", "Banner ad was clicked.")
                }
            }
        }
    }

    val isInspectionMode = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (!isInspectionMode) {
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(Unit) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BannerAdView(adView)
    }
}
```

- [ ] **Step 4: Delete the old `home/presentation` `BannerAd.kt`**

```bash
git rm home/presentation/src/main/java/com/gondroid/home/presentation/BannerAd.kt
```

- [ ] **Step 5: Simplify `HomeScreenRoot` to use `BannerAdSlot`, and remove the now-unused admob imports**

In `HomeScreen.kt`, `HomeScreenRoot` (currently lines 71-150) has three admob-related pieces:
the `adView = remember { ... }` block (lines 83-106), the `LaunchedEffect` that loads it (lines
110-117), and the `DisposableEffect` that destroys it (lines 145-148) — plus the `content = { ... }`
block passed into `HomeScreen` (lines 128-141) that wraps `BannerAd(adView, Modifier)`.

Replace the whole function body with:

```kotlin
@RequiresPermission(Manifest.permission.INTERNET)
@Composable
fun HomeScreenRoot(
    viewModel: HomeScreenViewModel,
    navigateToDetail: (String) -> Unit,
    navigateToConfiguration: () -> Unit,
    navigateToPremium: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    HomeScreen(
        state = state,
        onAction = { action ->
            when (action) {
                is HomeAction.OnClickCategory -> navigateToDetail(action.categoryId)
                is HomeAction.GoToConfiguration -> navigateToConfiguration()
                is HomeAction.GoToPremium -> navigateToPremium()
            }
        },
        content = {
            BannerAdSlot(bannerAdId = viewModel.bannerAdId, isPremium = state.isPremium)
        }
    )
}
```

Add the import `import com.gondroid.core.presentation.ui.BannerAdSlot` at the top of the file.
Remove now-unused imports: `android.util.Log`, `androidx.compose.runtime.DisposableEffect`,
`androidx.compose.runtime.LaunchedEffect`, `androidx.compose.runtime.remember`,
`androidx.compose.ui.platform.LocalContext`, `androidx.compose.ui.platform.LocalInspectionMode`
(check first whether `LocalInspectionMode` is still used elsewhere in this file — it is not,
per the current file contents), and the four `com.google.android.gms.ads.*` imports — the Kotlin
compiler will flag any of these that are still actually needed elsewhere in the file, so let the
compiler in Step 8 be the final check rather than removing blind.

- [ ] **Step 6: Add scroll to Home's content `Column`, remove the now-meaningless weighted spacer**

In `HomeScreen.kt`, the content `Column` (currently lines 205-278) is:

```kotlin
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(vertical = 16.dp),
        ) {
```//... ends with:
```kotlin
            Spacer(modifier = Modifier.weight(1f))

            content()

        }
```

Change the modifier to add scrolling, and remove the weighted `Spacer` immediately before
`content()` (a `weight(1f)` spacer has nothing to expand against once the column becomes
scrollable — height is no longer bounded by the `Scaffold`):

```kotlin
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
```
```kotlin
            content()

        }
```

Add imports: `androidx.compose.foundation.rememberScrollState`, `androidx.compose.foundation.verticalScroll`.

- [ ] **Step 7: Remove the now-unused AdMob dependency from `home/presentation`**

In `home/presentation/build.gradle.kts`, remove this line (nothing in `home/presentation` still
references `com.google.android.gms.ads.*` types directly after Steps 4-5):

```kotlin
    // adMob
    implementation(libs.play.services.admob)
```

- [ ] **Step 8: Build and fix any compiler errors**

Run: `./gradlew :core:presentation:ui:compileDebugKotlin :home:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. Fix any unused-import warnings-as-suggestions or missed references
the steps above called out as "let the compiler check."

- [ ] **Step 9: Run the Home unit test suite**

Run: `./gradlew :home:presentation:test`
Expected: BUILD SUCCESSFUL — `HomeScreenViewModelTest` and any other existing Home tests still
pass unchanged (this task doesn't touch `HomeScreenViewModel.kt`'s logic, only `HomeScreen.kt`'s
Compose UI).

- [ ] **Step 10: Manual verification on device/emulator**

Install (`./gradlew installDebug`), open the Home screen, and confirm: (a) the banner ad still
renders at the bottom exactly as before (or the "Google Mobile Ads preview banner." placeholder
text in `@Preview`), (b) resize the emulator window/switch to a small AVD profile (e.g. a short
landscape size) and confirm the whole Home screen content — header text, streak row, category
pager, banner — is reachable by scrolling instead of being clipped.

- [ ] **Step 11: Commit**

```bash
git add core/presentation/ui/src/main/java/com/gondroid/core/presentation/ui/BannerAdView.kt core/presentation/ui/src/main/java/com/gondroid/core/presentation/ui/BannerAdSlot.kt core/presentation/ui/build.gradle.kts home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt home/presentation/build.gradle.kts
git commit -m "refactor: extract shared BannerAdSlot, make Home scrollable"
```

---

### Task 4: Show banner ad on Detail screen, add Detail scroll fix

**Files:**
- Modify: `detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailScreenViewModel.kt`
- Modify: `detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailState.kt`
- Modify: `detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailScreen.kt`

**Interfaces:**
- Consumes: `BannerAdSlot(bannerAdId: String, isPremium: Boolean, modifier: Modifier = Modifier)` from Task 3 — this task cannot start until Task 3's `BannerAdSlot` exists (`detail/presentation` already depends on `core:presentation:ui` today, so no new module dependency is needed here).

- [ ] **Step 1: Inject `BillingManager` and the banner ad id into `DetailScreenViewModel`**

Read the current file first: `detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailScreenViewModel.kt`.
Add two constructor params and expose premium status the same way `HomeScreenViewModel` does
(`home/presentation/.../HomeScreenViewModel.kt:48-52`):

```kotlin
package com.gondroid.detail.presentation


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.core.data.ads.AdsManager
import com.gondroid.core.data.billing.BillingManager
import com.gondroid.core.domain.repository.QuizRepository
import com.gondroid.core.presentation.ui.DetailScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

sealed interface DetailEvent {
    data class NavigateToEvaluation(val categoryId: String) : DetailEvent
    data object ShowEvaluationInterstitial : DetailEvent
}

@HiltViewModel
class DetailScreenViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
    private val billingManager: BillingManager,
    val adsManager: AdsManager,
    @Named("admobBannerId") val bannerAdId: String,
) : ViewModel() {

    private var _state = MutableStateFlow(DetailState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<DetailEvent>()
    val events: Flow<DetailEvent> = eventChannel.receiveAsFlow()

    private val data = savedStateHandle.toRoute<DetailScreenRoute>()

    init {
        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    _state.update {
                        it.copy(category = category)
                    }
                }
            }
        }

        billingManager.isPremiumFlow.onEach { isPremium ->
            _state.update {
                it.copy(isPremium = isPremium)
            }
        }.launchIn(viewModelScope)
    }
```

Leave the rest of the file (`onStartEvaluation`, `onInterstitialClosed`, etc.) unchanged.

- [ ] **Step 2: Add `isPremium` to `DetailState`**

```kotlin
package com.gondroid.detail.presentation

import com.gondroid.core.domain.model.Category

data class DetailState(
    val category: Category = Category(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isPremium: Boolean = false,
)
```

- [ ] **Step 3: Thread `bannerAdId` through `DetailScreenRoot` into `DetailScreen`, and render `BannerAdSlot`**

In `DetailScreen.kt`, add the import:

```kotlin
import com.gondroid.core.presentation.ui.BannerAdSlot
```

In `DetailScreenRoot`, the call to `DetailScreen(...)` (currently lines 100-111) passes `state`
and `onAction`. Add `bannerAdId`:

```kotlin
    DetailScreen(
        state = state,
        bannerAdId = viewModel.bannerAdId,
        onAction = { action ->
            when (action) {
                is DetailAction.Back -> navigateBack()
                is DetailAction.GoToConfiguration -> navigateToConfiguration()
                is DetailAction.GoToEvaluation -> viewModel.onStartEvaluation(action.categoryId)
                is DetailAction.GoToQuestions -> navigateToQuestions(action.categoryId)
                is DetailAction.ShowPDF -> navigateToShowPDF(action.categoryId)
            }
        }
    )
```

Change the `DetailScreen` composable's signature (currently lines 116-119) to accept it:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailState,
    bannerAdId: String,
    onAction: (DetailAction) -> Unit,
) {
```

Inside `DetailScreen`, the content `Column` (currently lines 172-226) ends with:

```kotlin
            Spacer(modifier = Modifier.weight(1f))

            ButtonsAction(
                onGoToEvaluation = { onAction(DetailAction.GoToEvaluation(state.category.id)) },
                onGoToQuestions = { onAction(DetailAction.GoToQuestions(state.category.id)) },
                onShowPdf = { onAction(DetailAction.ShowPDF(state.category.id)) }
            )
        }

    }
}
```

Replace it with (drop the weighted spacer — see Step 4 for why — and add the banner ad after
the buttons):

```kotlin
            ButtonsAction(
                onGoToEvaluation = { onAction(DetailAction.GoToEvaluation(state.category.id)) },
                onGoToQuestions = { onAction(DetailAction.GoToQuestions(state.category.id)) },
                onShowPdf = { onAction(DetailAction.ShowPDF(state.category.id)) }
            )

            BannerAdSlot(bannerAdId = bannerAdId, isPremium = state.isPremium)
        }

    }
}
```

- [ ] **Step 4: Add scroll to Detail's content `Column`**

The same content `Column`'s opening modifier (currently lines 172-177) is:

```kotlin
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(16.dp),
        ) {
```

Change it to:

```kotlin
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
```

Add imports: `androidx.compose.foundation.rememberScrollState`, `androidx.compose.foundation.verticalScroll`.

- [ ] **Step 5: Fix the `@Preview` composable to match the new `DetailScreen` signature**

`PreviewDetailScreenRoot` (currently lines 289-310) calls `DetailScreen(state = ..., onAction = {})`
without `bannerAdId` — add it:

```kotlin
@Preview(
    showBackground = true,
)
@Composable
fun PreviewDetailScreenRoot() {
    MTCQuizTheme {
        DetailScreen(
            state = DetailState(
                category =   Category(
                    id = "1",
                    title = "CLASE A - CATEGORIA I",
                    category = "A-I",
                    classType = "CLASE A",
                    description = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A.",
                    image = CardTypeEnum.A1.drawable,
                    pdf = "CLASE_A_I.pdf"
                ),
            ),
            bannerAdId = "test-banner-id",
            onAction = {}
        )
    }
}
```

- [ ] **Step 6: Build and fix any compiler errors**

Run: `./gradlew :detail:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Run the Detail unit test suite**

Run: `./gradlew :detail:presentation:test`
Expected: BUILD SUCCESSFUL. If any existing test constructs `DetailScreenViewModel` directly
(check for a `DetailScreenViewModelTest.kt`), it will need a `billingManager`/`bannerAdId` value
supplied — use a fake `BillingManager` matching whatever pattern `HomeScreenViewModelTest` uses
(check `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/home/HomeScreenViewModelTest.kt`
for the existing fake, and reuse it if it's already shared/accessible from `detail/presentation`'s
test source set — if it's private to the `app` module's test sources, write an equivalent small
fake scoped to `detail/presentation`'s own tests instead of trying to share across modules).

- [ ] **Step 8: Manual verification on device/emulator**

Install (`./gradlew installDebug`), tap a Home category card to reach Detail, and confirm: (a)
the banner ad renders at the bottom, matching Home's; (b) resize to a small AVD profile and
confirm the description text, disclaimer, buttons, and banner are all reachable by scrolling
instead of being clipped or overlapping.

- [ ] **Step 9: Commit**

```bash
git add detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailScreenViewModel.kt detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailState.kt detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailScreen.kt
git commit -m "feat: show banner ad on Detail screen, make it scrollable"
```

## Self-Review Notes

- **Spec coverage:** Section A (palette) → Task 1. Section B (data bug fix) → Task 2. Section C
  (Detail banner) → Tasks 3 (shared `BannerAdSlot`) + 4 (Detail wiring). Section D (scroll safety)
  → folded into Task 3 (Home) and Task 4 (Detail), since both touch the same `Column` the banner
  work already modifies.
- **Ordering:** Tasks 1 and 2 are fully independent of everything else and each other. Task 3 must
  land before Task 4 (Task 4 consumes `BannerAdSlot`). Tasks 1/2/3 could run in parallel; Task 4
  is the only one with a hard dependency.
- **Type consistency:** `CategoryColorScheme`/`categoryColors` (Task 1), `BannerAdSlot`'s exact
  signature (Task 3, consumed verbatim in Task 4), and `DetailScreen`'s new `bannerAdId: String`
  parameter (Task 4, used in both the `DetailScreenRoot` call site and the `@Preview`) are
  consistent across every task that references them.
