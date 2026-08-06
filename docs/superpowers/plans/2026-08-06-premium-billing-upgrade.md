# Premium Billing Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a monthly subscription plan alongside the existing annual one with real Play Console pricing, stop the premium flag from flashing ads on cold start, add purchase funnel analytics, and move `isPremium` out of `core:data` into `core:domain` to match the rest of the codebase's repository pattern.

**Architecture:** Split the current `BillingManager` (data-only interface) into a pure-Kotlin `PremiumRepository` domain interface (state: `isPremiumFlow`, `availablePlansFlow`, and non-Activity operations) plus a small Android-only `BillingLauncher` data interface (the one method that needs `android.app.Activity`, which cannot live in `core:domain` since that module is a pure JVM library — see `core/domain/build.gradle.kts:1-4`, only `jetbrains.kotlin.jvm` + `kotlin.serialization`, no Android plugin). Both interfaces are implemented by a single renamed class, `PremiumRepositoryImpl` (was `BillingManagerImpl`), which now queries **two** Play Billing products (`mtcquiz_premium_monthly`, `mtcquiz_premium_annual`) instead of one, maps `ProductDetails` to a domain `SubscriptionPlan` model (so the UI shows the real Play Console price, not a hardcoded string), and persists the last known `isPremium` value to DataStore so the app doesn't show ads for a returning premium user before `refreshPurchaseState()` completes. A new `AnalyticsManager` (same shape as the existing `AdsManager`: interface + impl living directly in `core:data`, no domain counterpart — that's the established pattern for infra managers in this codebase) fires Firebase Analytics events across the purchase funnel.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Google Play Billing Library 7.1.1 (`com.android.billingclient:billing-ktx`), Firebase Analytics (already a `core:data` dependency), AndroidX DataStore Preferences, JUnit4 + MockK + Truth (existing project test stack).

## Global Constraints

- `core:domain` is a pure Kotlin/JVM module (`mtcquiz.jvm.library` — see `core/domain/build.gradle.kts`) — **never** import `android.*` types there.
- Keep the existing annual product ID `mtcquiz_premium_annual` unchanged (avoid a second manual rename in Play Console); add `mtcquiz_premium_monthly` as the new one.
- Follow the existing repository-pattern naming: domain interface in `core/domain/.../repository/`, impl in `core/data/.../repository` or feature-specific data package, bound via a Hilt `@Binds` module — mirror `PreferenceRepository`/`PreferenceRepositoryImpl` and `QuizRepository`.
- Follow the existing DataStore-preference pattern used by `AdsPreferences`/`PreferenceRepositoryImpl`: inject `DataStore<Preferences>` directly, define keys as `private companion object` `*PreferencesKey` constants.
- No new external dependencies needed — `firebase-analytics` and `mockk` are already in `gradle/libs.versions.toml`.
- All new/changed Spanish-facing UI copy stays in Spanish, matching the rest of `PremiumScreen.kt`.
- Every task ends with `./gradlew :<module>:test` (or the most specific module target) passing before moving to the next task.

---

### Task 1: `AnalyticsManager` for the purchase funnel

**Files:**
- Create: `core/data/src/main/java/com/gondroid/core/data/analytics/AnalyticsManager.kt`
- Create: `core/data/src/main/java/com/gondroid/core/data/analytics/AnalyticsManagerImpl.kt`
- Create: `core/data/src/main/java/com/gondroid/core/data/analytics/di/AnalyticsModule.kt`
- Modify: `core/data/src/main/java/com/gondroid/core/data/di/FirebaseModule.kt`
- Modify: `core/data/build.gradle.kts` (add `testImplementation(libs.mockk)` and `testImplementation(libs.robolectric)`)
- Test: `core/data/src/test/java/com/gondroid/core/data/analytics/AnalyticsManagerImplTest.kt`

**Interfaces:**
- Produces: `AnalyticsManager` interface with `logPaywallViewed()`, `logSubscribeClicked(productId: String)`, `logPurchaseCompleted(productId: String)`, `logPurchaseCanceled(productId: String)`, `logPurchaseFailed(productId: String, errorCode: Int)`, `logRestoreClicked()`, `logRestoreCompleted(isPremium: Boolean)` — Task 2 and Task 4 inject and call this.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.gondroid.core.data.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerImplTest {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var manager: AnalyticsManagerImpl

    @Before
    fun setUp() {
        firebaseAnalytics = mockk(relaxed = true)
        manager = AnalyticsManagerImpl(firebaseAnalytics)
    }

    @Test
    fun `logPaywallViewed logs paywall_viewed with no params`() {
        manager.logPaywallViewed()
        verify { firebaseAnalytics.logEvent("paywall_viewed", null) }
    }

    @Test
    fun `logSubscribeClicked logs product id param`() {
        val bundleSlot = slot<android.os.Bundle>()
        manager.logSubscribeClicked("mtcquiz_premium_monthly")
        verify { firebaseAnalytics.logEvent("premium_subscribe_clicked", capture(bundleSlot)) }
        assertThat(bundleSlot.captured.getString("product_id")).isEqualTo("mtcquiz_premium_monthly")
    }

    @Test
    fun `logPurchaseFailed logs product id and error code`() {
        val bundleSlot = slot<android.os.Bundle>()
        manager.logPurchaseFailed("mtcquiz_premium_annual", errorCode = 7)
        verify { firebaseAnalytics.logEvent("premium_purchase_failed", capture(bundleSlot)) }
        assertThat(bundleSlot.captured.getString("product_id")).isEqualTo("mtcquiz_premium_annual")
        assertThat(bundleSlot.captured.getInt("error_code")).isEqualTo(7)
    }

    @Test
    fun `logRestoreCompleted logs is_premium boolean param`() {
        val bundleSlot = slot<android.os.Bundle>()
        manager.logRestoreCompleted(isPremium = true)
        verify { firebaseAnalytics.logEvent("premium_restore_completed", capture(bundleSlot)) }
        assertThat(bundleSlot.captured.getBoolean("is_premium")).isTrue()
    }
}
```

Note: `core:data` is a pure JVM-executed unit-test source set (Robolectric is NOT applied to the module by default — see `AdsManagerCounterRuleTest.kt`, which runs with plain JUnit, no `@RunWith(RobolectricTestRunner::class)`). This new test calls `androidx.core.os.bundleOf(...)` (inside `AnalyticsManagerImpl`), which constructs a real `android.os.Bundle` — unavailable on a plain JVM classpath. Annotate `AnalyticsManagerImplTest` with `@RunWith(RobolectricTestRunner::class)` (add `import org.robolectric.RobolectricTestRunner` and `import org.junit.runner.RunWith`) and add `testImplementation(libs.robolectric)` to `core/data/build.gradle.kts`'s `dependencies { }` block (catalog key confirmed present: `gradle/libs.versions.toml:70,177`, alias `robolectric`, same one `:app` already uses).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:data:test --tests "com.gondroid.core.data.analytics.AnalyticsManagerImplTest"`
Expected: FAIL — `AnalyticsManagerImpl` and `AnalyticsManager` don't exist yet.

- [ ] **Step 3: Write the interface and implementation**

```kotlin
// AnalyticsManager.kt
package com.gondroid.core.data.analytics

interface AnalyticsManager {
    fun logPaywallViewed()
    fun logSubscribeClicked(productId: String)
    fun logPurchaseCompleted(productId: String)
    fun logPurchaseCanceled(productId: String)
    fun logPurchaseFailed(productId: String, errorCode: Int)
    fun logRestoreClicked()
    fun logRestoreCompleted(isPremium: Boolean)
}
```

```kotlin
// AnalyticsManagerImpl.kt
package com.gondroid.core.data.analytics

import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManagerImpl @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
) : AnalyticsManager {

    override fun logPaywallViewed() {
        firebaseAnalytics.logEvent("paywall_viewed", null)
    }

    override fun logSubscribeClicked(productId: String) {
        firebaseAnalytics.logEvent(
            "premium_subscribe_clicked",
            bundleOf("product_id" to productId),
        )
    }

    override fun logPurchaseCompleted(productId: String) {
        firebaseAnalytics.logEvent(
            "premium_purchase_completed",
            bundleOf("product_id" to productId),
        )
    }

    override fun logPurchaseCanceled(productId: String) {
        firebaseAnalytics.logEvent(
            "premium_purchase_canceled",
            bundleOf("product_id" to productId),
        )
    }

    override fun logPurchaseFailed(productId: String, errorCode: Int) {
        firebaseAnalytics.logEvent(
            "premium_purchase_failed",
            bundleOf("product_id" to productId, "error_code" to errorCode),
        )
    }

    override fun logRestoreClicked() {
        firebaseAnalytics.logEvent("premium_restore_clicked", null)
    }

    override fun logRestoreCompleted(isPremium: Boolean) {
        firebaseAnalytics.logEvent(
            "premium_restore_completed",
            bundleOf("is_premium" to isPremium),
        )
    }
}
```

```kotlin
// di/AnalyticsModule.kt
package com.gondroid.core.data.analytics.di

import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.data.analytics.AnalyticsManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsManager(impl: AnalyticsManagerImpl): AnalyticsManager
}
```

Add to `core/data/src/main/java/com/gondroid/core/data/di/FirebaseModule.kt` (inside the existing `object FirebaseModule`, alongside `provideFirebaseAuth`):

```kotlin
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
```

This needs an injected `@ApplicationContext context: Context` parameter on `provideFirebaseAnalytics` — add `@ApplicationContext context: Context` as a parameter to that provider function specifically (don't add it to the whole object), and add the import `com.google.firebase.analytics.FirebaseAnalytics`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core:data:test --tests "com.gondroid.core.data.analytics.AnalyticsManagerImplTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Commit**

```bash
git add core/data/src/main/java/com/gondroid/core/data/analytics core/data/src/main/java/com/gondroid/core/data/di/FirebaseModule.kt core/data/src/test/java/com/gondroid/core/data/analytics core/data/build.gradle.kts
git commit -m "feat: add AnalyticsManager for premium purchase funnel events"
```

---

### Task 2: Domain layer + rewritten billing data layer (multi-plan pricing, DataStore persistence)

**Files:**
- Create: `core/domain/src/main/java/com/gondroid/core/domain/model/SubscriptionPlan.kt`
- Create: `core/domain/src/main/java/com/gondroid/core/domain/repository/PremiumRepository.kt`
- Create: `core/data/src/main/java/com/gondroid/core/data/billing/BillingLauncher.kt`
- Create: `core/data/src/main/java/com/gondroid/core/data/di/CoroutineScopeModule.kt`
- Create: `core/data/src/main/java/com/gondroid/core/data/billing/PremiumRepositoryImpl.kt`
- Delete: `core/data/src/main/java/com/gondroid/core/data/billing/BillingManager.kt`
- Delete: `core/data/src/main/java/com/gondroid/core/data/billing/BillingManagerImpl.kt`
- Modify: `core/data/src/main/java/com/gondroid/core/data/billing/di/BillingModule.kt`
- Test: `core/data/src/test/java/com/gondroid/core/data/billing/PremiumRepositoryImplTest.kt`

**Interfaces:**
- Consumes: `AnalyticsManager` from Task 1 (`com.gondroid.core.data.analytics.AnalyticsManager`).
- Produces: `PremiumRepository` (domain) with `isPremiumFlow: Flow<Boolean>`, `availablePlansFlow: Flow<List<SubscriptionPlan>>`, `suspend fun loadAvailablePlans()`, `suspend fun refreshPurchaseState()`, `suspend fun restorePurchases()`. `BillingLauncher` (data) with `suspend fun launchSubscription(activity: Activity, productId: String): Boolean`. `SubscriptionPlan(productId: String, billingPeriod: BillingPeriod, formattedPrice: String)` + `enum class BillingPeriod { MONTHLY, ANNUAL }`. `@ApplicationScope` qualifier + `CoroutineScope` binding from `CoroutineScopeModule` — a Hilt-provided `@Singleton` background scope, injected into `PremiumRepositoryImpl` instead of constructed inline, specifically so tests can substitute a `TestScope` and make its fire-and-forget DataStore writes visible to `advanceUntilIdle()` (a plain `CoroutineScope(SupervisorJob() + Dispatchers.IO)` built inside the class is invisible to `runTest`'s virtual scheduler — that shape was tried and produced two reliably-failing tests). Task 3 rewires every consumer to these; Task 4 rewires `PremiumViewModel`/`PremiumScreen`. Neither Task 3 nor Task 4 constructs `PremiumRepositoryImpl` directly (they depend on the `PremiumRepository`/`BillingLauncher` interfaces via Hilt or hand-rolled fakes), so this constructor change is isolated to this task.

Real Play Billing connectivity (`BillingClient`, `queryProductDetails`, `launchBillingFlow`) is constructed inline in this class exactly as the original `BillingManagerImpl` did, and — like the original — is **not** unit tested directly (there was no existing test for it either; it was only ever exercised through `FakeBillingManager` at the consumer layer, continued in Task 3). This task adds real unit tests for the two genuinely new, pure pieces: the `ProductDetails → SubscriptionPlan` mapping and the DataStore persistence read/write path. Both are exposed as `internal` members so the test (same Gradle module) can call them directly without a live `BillingClient`.

- [ ] **Step 1: Write the domain model and repository interface (no test needed — pure data/interface, exercised by Step 3's tests)**

```kotlin
// core/domain/src/main/java/com/gondroid/core/domain/model/SubscriptionPlan.kt
package com.gondroid.core.domain.model

data class SubscriptionPlan(
    val productId: String,
    val billingPeriod: BillingPeriod,
    val formattedPrice: String,
)

enum class BillingPeriod { MONTHLY, ANNUAL }
```

```kotlin
// core/domain/src/main/java/com/gondroid/core/domain/repository/PremiumRepository.kt
package com.gondroid.core.domain.repository

import com.gondroid.core.domain.model.SubscriptionPlan
import kotlinx.coroutines.flow.Flow

interface PremiumRepository {

    /** True si el usuario tiene una suscripción premium activa (o la tenía en el último refresh). */
    val isPremiumFlow: Flow<Boolean>

    /** Planes disponibles con su precio real, tal como los devuelve Google Play. Vacío hasta llamar [loadAvailablePlans]. */
    val availablePlansFlow: Flow<List<SubscriptionPlan>>

    /** Consulta a Google Play los productos de suscripción y actualiza [availablePlansFlow]. */
    suspend fun loadAvailablePlans()

    /** Consulta a Google Play si hay compras activas y actualiza [isPremiumFlow]. Llamar en cada inicio de la app. */
    suspend fun refreshPurchaseState()

    /** Restaura compras (reinstalación / cambio de device). Internamente hace lo mismo que [refreshPurchaseState]. */
    suspend fun restorePurchases()
}
```

```kotlin
// core/data/src/main/java/com/gondroid/core/data/billing/BillingLauncher.kt
package com.gondroid.core.data.billing

import android.app.Activity

/**
 * Aparte de [com.gondroid.core.domain.repository.PremiumRepository] porque lanzar el flujo de
 * compra de Play Billing requiere una [Activity], y `core:domain` es un módulo Kotlin puro sin
 * dependencias de Android.
 */
interface BillingLauncher {
    suspend fun launchSubscription(activity: Activity, productId: String): Boolean
}
```

- [ ] **Step 2: Write the failing tests for the new pure/testable behavior**

```kotlin
// core/data/src/test/java/com/gondroid/core/data/billing/PremiumRepositoryImplTest.kt
package com.gondroid.core.data.billing

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.android.billingclient.api.ProductDetails
import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.domain.model.BillingPeriod
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric is required here (not plain JUnit, unlike the rest of core:data's tests) because
 * constructing [PremiumRepositoryImpl] builds a real Play Billing `BillingClient`
 * (`BillingClient.newBuilder(context)...build()`), which touches Android framework classes
 * unavailable on a plain JVM classpath.
 */
@RunWith(RobolectricTestRunner::class)
class PremiumRepositoryImplTest {

    /** Fake in-memory DataStore<Preferences>, same style as the rest of core:data's tests avoid a real DataStore. */
    private class FakeDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(mutablePreferencesOf())
        override val data: Flow<Preferences> = state
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private lateinit var context: Context
    private lateinit var dataStore: FakeDataStore
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var repository: PremiumRepositoryImpl

    /**
     * [PremiumRepositoryImpl] takes its background [kotlinx.coroutines.CoroutineScope] as a
     * constructor param (see the `@ApplicationScope` binding in `CoroutineScopeModule`) instead
     * of building one internally, precisely so a test can hand it this [TestScope] — its
     * fire-and-forget `scope.launch { ... }` calls (DataStore writes, cache-seeding in `init`)
     * become children of this scope, and `testScope.runTest { advanceUntilIdle() }` can see and
     * wait for them. A real `Dispatchers.IO`-backed scope built inside the class would be
     * invisible to `advanceUntilIdle()` and make these two tests flaky/failing.
     */
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        dataStore = FakeDataStore()
        analyticsManager = mockk(relaxed = true)
        repository = PremiumRepositoryImpl(context, dataStore, analyticsManager, testScope)
    }

    @Test
    fun `billingPeriodFor maps known product ids`() {
        assertThat(repository.billingPeriodFor(PremiumRepositoryImpl.PRODUCT_ID_MONTHLY))
            .isEqualTo(BillingPeriod.MONTHLY)
        assertThat(repository.billingPeriodFor(PremiumRepositoryImpl.PRODUCT_ID_ANNUAL))
            .isEqualTo(BillingPeriod.ANNUAL)
        assertThat(repository.billingPeriodFor("unknown_product")).isNull()
    }

    @Test
    fun `mapToSubscriptionPlan builds domain plan from ProductDetails pricing phase`() {
        val pricingPhase = mockk<ProductDetails.PricingPhase> {
            every { formattedPrice } returns "S/ 9.90"
        }
        val pricingPhases = mockk<ProductDetails.PricingPhases> {
            every { pricingPhaseList } returns listOf(pricingPhase)
        }
        val offerDetails = mockk<ProductDetails.SubscriptionOfferDetails> {
            every { this@mockk.pricingPhases } returns pricingPhases
        }
        val details = mockk<ProductDetails> {
            every { productId } returns PremiumRepositoryImpl.PRODUCT_ID_MONTHLY
            every { subscriptionOfferDetails } returns listOf(offerDetails)
        }

        val plan = repository.mapToSubscriptionPlan(details)

        assertThat(plan).isNotNull()
        assertThat(plan!!.productId).isEqualTo(PremiumRepositoryImpl.PRODUCT_ID_MONTHLY)
        assertThat(plan.billingPeriod).isEqualTo(BillingPeriod.MONTHLY)
        assertThat(plan.formattedPrice).isEqualTo("S/ 9.90")
    }

    @Test
    fun `mapToSubscriptionPlan returns null when there is no offer`() {
        val details = mockk<ProductDetails> {
            every { productId } returns PremiumRepositoryImpl.PRODUCT_ID_ANNUAL
            every { subscriptionOfferDetails } returns null
        }

        assertThat(repository.mapToSubscriptionPlan(details)).isNull()
    }

    @Test
    fun `isPremiumFlow seeds from cached DataStore value on init`() = testScope.runTest {
        dataStore.updateData { prefs ->
            prefs.toMutablePreferences().apply { this[PremiumRepositoryImpl.IS_PREMIUM_CACHED_KEY] = true }
        }
        val seededRepository = PremiumRepositoryImpl(context, dataStore, analyticsManager, testScope)
        advanceUntilIdle()

        assertThat((seededRepository.isPremiumFlow as kotlinx.coroutines.flow.StateFlow<Boolean>).value).isTrue()
    }

    @Test
    fun `updateIsPremium writes through to DataStore`() = testScope.runTest {
        repository.updateIsPremium(true)
        advanceUntilIdle()

        val cached = dataStore.data.first()[PremiumRepositoryImpl.IS_PREMIUM_CACHED_KEY] ?: false
        assertThat(cached).isTrue()
        assertThat((repository.isPremiumFlow as kotlinx.coroutines.flow.StateFlow<Boolean>).value).isTrue()
    }
}
```

Note: `testScope.runTest { ... }` (an extension on the already-constructed `TestScope`) reuses `testDispatcher`'s scheduler rather than creating a new isolated one — this is what makes `advanceUntilIdle()` able to observe coroutines that `PremiumRepositoryImpl` launched on the injected `scope` before the test body even started (e.g. `init`'s cache-seeding launch, which fires at `setUp()` time, before either test method's `runTest` block opens).

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew :core:data:test --tests "com.gondroid.core.data.billing.PremiumRepositoryImplTest"`
Expected: FAIL — `PremiumRepositoryImpl` doesn't exist yet.

- [ ] **Step 4: Delete the old `BillingManager` files and write `PremiumRepositoryImpl`**

Delete `core/data/src/main/java/com/gondroid/core/data/billing/BillingManager.kt` and `core/data/src/main/java/com/gondroid/core/data/billing/BillingManagerImpl.kt`.

Create the application-scoped `CoroutineScope` Hilt binding `PremiumRepositoryImpl` needs for its fire-and-forget DataStore writes (used from the non-suspend `PurchasesUpdatedListener` callback and to seed `isPremiumFlow` from the cache in `init`):

```kotlin
// core/data/src/main/java/com/gondroid/core/data/di/CoroutineScopeModule.kt
package com.gondroid.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
```

```kotlin
// core/data/src/main/java/com/gondroid/core/data/billing/PremiumRepositoryImpl.kt
package com.gondroid.core.data.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.data.di.ApplicationScope
import com.gondroid.core.domain.model.BillingPeriod
import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class PremiumRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val analyticsManager: AnalyticsManager,
    @ApplicationScope private val scope: CoroutineScope,
) : PremiumRepository, BillingLauncher {

    companion object {
        const val PRODUCT_ID_MONTHLY = "mtcquiz_premium_monthly"
        const val PRODUCT_ID_ANNUAL = "mtcquiz_premium_annual"
        private val PRODUCT_IDS = listOf(PRODUCT_ID_MONTHLY, PRODUCT_ID_ANNUAL)
        val IS_PREMIUM_CACHED_KEY = booleanPreferencesKey("cached_is_premium")
    }

    private val _isPremium = MutableStateFlow(false)
    override val isPremiumFlow: Flow<Boolean> = _isPremium.asStateFlow()

    private val _availablePlans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    override val availablePlansFlow: Flow<List<SubscriptionPlan>> = _availablePlans.asStateFlow()

    /** ProductDetails reales cacheados desde la última consulta a Play, para lanzar la compra sin re-consultar. */
    private val productDetailsCache = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase -> handlePurchase(purchase) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.d("Billing: user canceled subscription flow")
                analyticsManager.logPurchaseCanceled(purchases?.firstOrNull()?.products?.firstOrNull() ?: "unknown")
            }
            else -> {
                Timber.e("Billing: purchase failed with code ${billingResult.responseCode}")
                analyticsManager.logPurchaseFailed(
                    productId = purchases?.firstOrNull()?.products?.firstOrNull() ?: "unknown",
                    errorCode = billingResult.responseCode,
                )
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases()
        .build()

    init {
        scope.launch {
            _isPremium.value = dataStore.data.map { it[IS_PREMIUM_CACHED_KEY] ?: false }.first()
        }
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        if (cont.isActive) cont.resume(true)
                    } else {
                        Timber.e("Billing: connection failed with code ${result.responseCode}")
                        if (cont.isActive) cont.resume(false)
                    }
                }
                override fun onBillingServiceDisconnected() {
                    Timber.w("Billing: service disconnected")
                }
            })
        }
    }

    override suspend fun loadAvailablePlans() {
        if (!ensureConnected()) return

        val productList = PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        val result: ProductDetailsResult = billingClient.queryProductDetails(params)

        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Timber.e("Billing: failed to query product details: ${result.billingResult.debugMessage}")
            return
        }

        val plans = result.productDetailsList.orEmpty().mapNotNull { details ->
            productDetailsCache[details.productId] = details
            mapToSubscriptionPlan(details)
        }
        _availablePlans.value = plans
    }

    internal fun mapToSubscriptionPlan(details: ProductDetails): SubscriptionPlan? {
        val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return null
        val pricingPhase = offer.pricingPhases.pricingPhaseList.firstOrNull() ?: return null
        val period = billingPeriodFor(details.productId) ?: return null
        return SubscriptionPlan(
            productId = details.productId,
            billingPeriod = period,
            formattedPrice = pricingPhase.formattedPrice,
        )
    }

    internal fun billingPeriodFor(productId: String): BillingPeriod? = when (productId) {
        PRODUCT_ID_MONTHLY -> BillingPeriod.MONTHLY
        PRODUCT_ID_ANNUAL -> BillingPeriod.ANNUAL
        else -> null
    }

    override suspend fun launchSubscription(activity: Activity, productId: String): Boolean {
        if (!ensureConnected()) return false

        var productDetails = productDetailsCache[productId]
        if (productDetails == null) {
            loadAvailablePlans()
            productDetails = productDetailsCache[productId]
        }
        if (productDetails == null) {
            Timber.e("Billing: product '$productId' not found in Play Console")
            return false
        }

        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Timber.e("Billing: no offer found for product '$productId'")
            return false
        }

        analyticsManager.logSubscribeClicked(productId)

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        val flowResult = billingClient.launchBillingFlow(activity, flowParams)
        return flowResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            updateIsPremium(true)
            analyticsManager.logPurchaseCompleted(purchase.products.firstOrNull() ?: "unknown")

            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        Timber.e("Billing: failed to acknowledge purchase: ${result.debugMessage}")
                    }
                }
            }
        }
    }

    internal fun updateIsPremium(value: Boolean) {
        _isPremium.value = value
        scope.launch {
            dataStore.edit { prefs -> prefs[IS_PREMIUM_CACHED_KEY] = value }
        }
    }

    override suspend fun refreshPurchaseState() {
        if (!ensureConnected()) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)

        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            val hasActiveSub = result.purchasesList.any { purchase ->
                purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                    purchase.products.any { it in PRODUCT_IDS }
            }
            updateIsPremium(hasActiveSub)
            Timber.d("Billing: refreshed purchase state, isPremium=$hasActiveSub")
        } else {
            Timber.e("Billing: failed to query purchases: ${result.billingResult.debugMessage}")
        }
    }

    override suspend fun restorePurchases() {
        analyticsManager.logRestoreClicked()
        refreshPurchaseState()
        analyticsManager.logRestoreCompleted(_isPremium.value)
    }
}
```

Update `core/data/src/main/java/com/gondroid/core/data/billing/di/BillingModule.kt`:

```kotlin
package com.gondroid.core.data.billing.di

import com.gondroid.core.data.billing.BillingLauncher
import com.gondroid.core.data.billing.PremiumRepositoryImpl
import com.gondroid.core.domain.repository.PremiumRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindPremiumRepository(impl: PremiumRepositoryImpl): PremiumRepository

    @Binds
    @Singleton
    abstract fun bindBillingLauncher(impl: PremiumRepositoryImpl): BillingLauncher
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :core:data:test --tests "com.gondroid.core.data.billing.PremiumRepositoryImplTest"`
Expected: PASS (5 tests)

- [ ] **Step 6: Commit**

```bash
git add core/domain/src/main/java/com/gondroid/core/domain/model/SubscriptionPlan.kt \
        core/domain/src/main/java/com/gondroid/core/domain/repository/PremiumRepository.kt \
        core/data/src/main/java/com/gondroid/core/data/billing \
        core/data/src/test/java/com/gondroid/core/data/billing
git commit -m "feat: add monthly subscription plan with real Play Console pricing and DataStore-persisted isPremium"
```

---

### Task 3: Rewire every consumer from `BillingManager` to `PremiumRepository`

**Files:**
- Modify: `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreenViewModel.kt`
- Modify: `configuration/presentation/src/main/java/com/gondroid/configuration/presentation/ConfigurationScreenViewModel.kt`
- Modify: `detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailScreenViewModel.kt`
- Modify: `app/src/main/java/com/gondroid/mtcquiz/MainViewModel.kt`
- Modify: `core/data/src/main/java/com/gondroid/core/data/ads/AdsManagerImpl.kt`
- Delete: `core/data/src/test/java/com/gondroid/core/data/ads/FakeBillingManager.kt`
- Create: `core/data/src/test/java/com/gondroid/core/data/ads/FakePremiumRepository.kt`
- Modify: `core/data/src/test/java/com/gondroid/core/data/ads/AdsManagerCounterRuleTest.kt`
- Modify: `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/home/HomeScreenViewModelTest.kt`
- Modify: `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/detail/DetailScreenViewModelTest.kt`

**Interfaces:**
- Consumes: `PremiumRepository` domain interface from Task 2 (`com.gondroid.core.domain.repository.PremiumRepository`, method `isPremiumFlow: Flow<Boolean>` — the only member every one of these consumers uses).

This task is purely mechanical: replace the type `com.gondroid.core.data.billing.BillingManager` with `com.gondroid.core.domain.repository.PremiumRepository` everywhere it's injected for `isPremiumFlow`, and rename the constructor parameter from `billingManager` to `premiumRepository` for clarity. None of these consumers call `launchSubscription` or `restorePurchases`/`refreshPurchaseState` directly (only `PremiumViewModel` and `MainViewModel` do — `MainViewModel` only calls `refreshPurchaseState()`, which stays on `PremiumRepository`, so it needs no `BillingLauncher`).

- [ ] **Step 1: Write/update the failing tests first**

Create `core/data/src/test/java/com/gondroid/core/data/ads/FakePremiumRepository.kt` (replaces `FakeBillingManager.kt`, delete that file):

```kotlin
package com.gondroid.core.data.ads

import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePremiumRepository(isPremium: Boolean = false) : PremiumRepository {
    override val isPremiumFlow: Flow<Boolean> = MutableStateFlow(isPremium)
    override val availablePlansFlow: Flow<List<SubscriptionPlan>> = MutableStateFlow(emptyList())
    override suspend fun loadAvailablePlans() {}
    override suspend fun refreshPurchaseState() {}
    override suspend fun restorePurchases() {}
}
```

Update `core/data/src/test/java/com/gondroid/core/data/ads/AdsManagerCounterRuleTest.kt` — replace the import `com.gondroid.core.data.billing.BillingManager` and the constructor call:

```kotlin
        manager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            premiumRepository = FakePremiumRepository()
        )
```

(remove the now-unused `BillingManager` import; keep the rest of the file — the ten existing `@Test` functions — unchanged, they only exercise `AdsPreferences` counters.)

Update `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/home/HomeScreenViewModelTest.kt` — replace the inline `fakeBillingManager` object (lines 34-39) with:

```kotlin
    private val fakePremiumRepository = object : PremiumRepository {
        override val isPremiumFlow: Flow<Boolean> = flowOf(false)
        override val availablePlansFlow: Flow<List<SubscriptionPlan>> = flowOf(emptyList())
        override suspend fun loadAvailablePlans() = Unit
        override suspend fun refreshPurchaseState() = Unit
        override suspend fun restorePurchases() = Unit
    }
```

and in `setUp()`, change `billingManager = fakeBillingManager` to `premiumRepository = fakePremiumRepository`. Replace the import `com.gondroid.core.data.billing.BillingManager` with `com.gondroid.core.domain.model.SubscriptionPlan` and `com.gondroid.core.domain.repository.PremiumRepository`; drop the now-unused `android.app.Activity` import (no longer needed since `PremiumRepository` has no `launchSubscription`).

Update `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/detail/DetailScreenViewModelTest.kt` — in `createViewModel(...)` (lines 40-45) and the standalone test at lines 74-79, replace both inline `BillingManager` objects with `PremiumRepository` objects following the same shape as `FakePremiumRepository` above (drop `launchSubscription`, add `availablePlansFlow`/`loadAvailablePlans`), rename the `billingManager` constructor arguments passed to `DetailScreenViewModel(...)` to `premiumRepository`, and swap the import from `com.gondroid.core.data.billing.BillingManager` to `com.gondroid.core.domain.repository.PremiumRepository` (plus `com.gondroid.core.domain.model.SubscriptionPlan`). Drop the `android.app.Activity` import if nothing else in the file needs it.

- [ ] **Step 2: Run tests to verify they fail (compile errors — production code not yet updated)**

Run: `./gradlew :core:data:test :app:testDebugUnitTest --tests "com.gondroid.core.data.ads.AdsManagerCounterRuleTest" --tests "com.gondroid.mtcquiz.presentation.screens.home.HomeScreenViewModelTest" --tests "com.gondroid.mtcquiz.presentation.screens.detail.DetailScreenViewModelTest"`
Expected: FAIL to compile — `AdsManagerImpl`, `HomeScreenViewModel`, `DetailScreenViewModel` still take `BillingManager`.

- [ ] **Step 3: Update the production consumers**

`core/data/src/main/java/com/gondroid/core/data/ads/AdsManagerImpl.kt` — change the constructor and the `isPremium` getter:

```kotlin
@Singleton
class AdsManagerImpl @Inject constructor(
    private val prefs: AdsPreferences,
    @Named("admobInterstitialId") private val interstitialId: String,
    private val premiumRepository: com.gondroid.core.domain.repository.PremiumRepository,
) : AdsManager {

    private val isPremium: Boolean
        get() = (premiumRepository.isPremiumFlow as? kotlinx.coroutines.flow.StateFlow)?.value ?: false
```

(Everything else in the file — `preloadPdfInterstitial`, `shouldShowPdfInterstitial`, etc. — is unchanged.)

`home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreenViewModel.kt` — replace the import and constructor param:

```kotlin
import com.gondroid.core.domain.repository.PremiumRepository
// ...
    private val premiumRepository: PremiumRepository,
// ...
        premiumRepository.isPremiumFlow.onEach { isPremium ->
```

`configuration/presentation/src/main/java/com/gondroid/configuration/presentation/ConfigurationScreenViewModel.kt` — same replacement (`billingManager: BillingManager` → `premiumRepository: PremiumRepository`, update the `init` block's `billingManager.isPremiumFlow` → `premiumRepository.isPremiumFlow`).

`detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailScreenViewModel.kt` — same replacement for the `billingManager` constructor param and the `init` block usage.

`app/src/main/java/com/gondroid/mtcquiz/MainViewModel.kt` — replace `import com.gondroid.core.data.billing.BillingManager` with `import com.gondroid.core.domain.repository.PremiumRepository`, rename `private val billingManager: BillingManager` to `private val premiumRepository: PremiumRepository`, and update both usages (`billingManager.isPremiumFlow` → `premiumRepository.isPremiumFlow`, `billingManager.refreshPurchaseState()` → `premiumRepository.refreshPurchaseState()`).

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :core:data:test :app:testDebugUnitTest --tests "com.gondroid.core.data.ads.AdsManagerCounterRuleTest" --tests "com.gondroid.mtcquiz.presentation.screens.home.HomeScreenViewModelTest" --tests "com.gondroid.mtcquiz.presentation.screens.detail.DetailScreenViewModelTest"`
Expected: PASS (all tests in these 3 classes)

- [ ] **Step 5: Full module build to catch any remaining `BillingManager` reference**

Run: `grep -rn "com.gondroid.core.data.billing.BillingManager\b" --include="*.kt" .` (from repo root)
Expected: no matches (the type no longer exists). Fix any stragglers before continuing.

Run: `./gradlew :app:compileDebugKotlin :core:data:compileDebugKotlin :home:presentation:compileDebugKotlin :configuration:presentation:compileDebugKotlin :detail:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor: move isPremium from core:data BillingManager to core:domain PremiumRepository"
```

---

### Task 4: Multi-plan `PremiumScreen` UI + `PremiumViewModel` + analytics wiring

**Files:**
- Modify: `configuration/presentation/src/main/java/com/gondroid/configuration/presentation/premium/PremiumViewModel.kt`
- Modify: `configuration/presentation/src/main/java/com/gondroid/configuration/presentation/premium/PremiumScreen.kt`
- Create: `configuration/presentation/src/test/java/com/gondroid/configuration/presentation/premium/PremiumViewModelTest.kt`
- Modify: `configuration/presentation/build.gradle.kts` (this module has no `testImplementation` entries at all yet — add a `// Test` block with `testImplementation(libs.mockk)`, `testImplementation(libs.kotlinx.coroutines.test)`, and `testImplementation(libs.truth)`, same three catalog aliases `core:data` already uses)

**Interfaces:**
- Consumes: `PremiumRepository` (Task 2/3) for `isPremium`/`availablePlansFlow`/`restorePurchases`, `BillingLauncher` (Task 2) for `launchSubscription(activity, productId)`, `AnalyticsManager` (Task 1) for `logPaywallViewed()`.

- [ ] **Step 1: Write the failing ViewModel test**

```kotlin
package com.gondroid.configuration.presentation.premium

import android.app.Activity
import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.data.billing.BillingLauncher
import com.gondroid.core.domain.model.BillingPeriod
import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PremiumViewModelTest {

    private val monthlyPlan = SubscriptionPlan("mtcquiz_premium_monthly", BillingPeriod.MONTHLY, "S/ 9.90")
    private val annualPlan = SubscriptionPlan("mtcquiz_premium_annual", BillingPeriod.ANNUAL, "S/ 29.90")

    private lateinit var premiumRepository: PremiumRepository
    private lateinit var billingLauncher: BillingLauncher
    private lateinit var analyticsManager: AnalyticsManager

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        premiumRepository = mockk(relaxed = true) {
            every { isPremiumFlow } returns MutableStateFlow(false)
            every { availablePlansFlow } returns MutableStateFlow(listOf(monthlyPlan, annualPlan))
        }
        billingLauncher = mockk(relaxed = true)
        analyticsManager = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init logs paywall viewed and loads available plans`() = runTest {
        PremiumViewModel(premiumRepository, billingLauncher, analyticsManager)

        verify { analyticsManager.logPaywallViewed() }
        coVerify { premiumRepository.loadAvailablePlans() }
    }

    @Test
    fun `available plans default selection is the annual plan`() = runTest {
        val vm = PremiumViewModel(premiumRepository, billingLauncher, analyticsManager)

        assertThat(vm.selectedPlan.value).isEqualTo(annualPlan)
    }

    @Test
    fun `selectPlan updates the selected plan`() = runTest {
        val vm = PremiumViewModel(premiumRepository, billingLauncher, analyticsManager)

        vm.selectPlan(monthlyPlan)

        assertThat(vm.selectedPlan.value).isEqualTo(monthlyPlan)
    }

    @Test
    fun `subscribe launches the selected plan's product id`() = runTest {
        val activity = mockk<Activity>()
        coEvery { billingLauncher.launchSubscription(activity, any()) } returns true
        val vm = PremiumViewModel(premiumRepository, billingLauncher, analyticsManager)
        vm.selectPlan(monthlyPlan)

        vm.subscribe(activity)

        coVerify { billingLauncher.launchSubscription(activity, "mtcquiz_premium_monthly") }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :configuration:presentation:test --tests "com.gondroid.configuration.presentation.premium.PremiumViewModelTest"`
Expected: FAIL — `PremiumViewModel` doesn't take these constructor params yet, `selectedPlan`/`selectPlan` don't exist.

- [ ] **Step 3: Rewrite `PremiumViewModel`**

```kotlin
package com.gondroid.configuration.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.data.billing.BillingLauncher
import com.gondroid.core.domain.model.BillingPeriod
import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumRepository: PremiumRepository,
    private val billingLauncher: BillingLauncher,
    private val analyticsManager: AnalyticsManager,
) : ViewModel() {

    val isPremium = premiumRepository.isPremiumFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val availablePlans = premiumRepository.availablePlansFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlan = MutableStateFlow<SubscriptionPlan?>(null)
    val selectedPlan: StateFlow<SubscriptionPlan?> = _selectedPlan.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage = _restoreMessage.asStateFlow()

    init {
        analyticsManager.logPaywallViewed()
        viewModelScope.launch {
            premiumRepository.loadAvailablePlans()
        }
        viewModelScope.launch {
            premiumRepository.availablePlansFlow.collect { plans ->
                if (_selectedPlan.value == null) {
                    _selectedPlan.value = plans.firstOrNull { it.billingPeriod == BillingPeriod.ANNUAL }
                        ?: plans.firstOrNull()
                }
            }
        }
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    fun subscribe(activity: Activity) = viewModelScope.launch {
        val productId = _selectedPlan.value?.productId ?: return@launch
        _isLoading.value = true
        billingLauncher.launchSubscription(activity, productId)
        _isLoading.value = false
    }

    fun restorePurchases() = viewModelScope.launch {
        _isLoading.value = true
        premiumRepository.restorePurchases()
        _isLoading.value = false
        _restoreMessage.value = if (isPremium.value) {
            "Compra restaurada correctamente"
        } else {
            "No se encontró ninguna suscripción activa"
        }
    }

    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :configuration:presentation:test --tests "com.gondroid.configuration.presentation.premium.PremiumViewModelTest"`
Expected: PASS (4 tests)

- [ ] **Step 5: Rewrite `PremiumScreen` UI for dynamic multi-plan display**

In `configuration/presentation/src/main/java/com/gondroid/configuration/presentation/premium/PremiumScreen.kt`:

Replace `PremiumScreenRoot` (lines 82-98) with a version that reads `availablePlans`/`selectedPlan`/`restoreMessage` and shows a `Toast`/`Snackbar`-style message after restore:

```kotlin
@Composable
fun PremiumScreenRoot(
    viewModel: PremiumViewModel = hiltViewModel(),
    navigateBack: () -> Unit,
) {
    val isPremium by viewModel.isPremium.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val availablePlans by viewModel.availablePlans.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()
    val restoreMessage by viewModel.restoreMessage.collectAsState()
    val activity = LocalContext.current as? Activity
    val context = LocalContext.current

    LaunchedEffect(restoreMessage) {
        restoreMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearRestoreMessage()
        }
    }

    PremiumScreen(
        isPremium = isPremium,
        isLoading = isLoading,
        availablePlans = availablePlans,
        selectedPlan = selectedPlan,
        onSelectPlan = viewModel::selectPlan,
        onSubscribe = { activity?.let { viewModel.subscribe(it) } },
        onRestore = { viewModel.restorePurchases() },
        navigateBack = navigateBack,
    )
}
```

Add the two new imports this needs: `androidx.compose.runtime.LaunchedEffect` (if not already imported) and `com.gondroid.core.domain.model.SubscriptionPlan`, `com.gondroid.core.domain.model.BillingPeriod`.

Update the `PremiumScreen` composable signature (line 102) to accept the new params and replace the single hardcoded `PlanCard` (lines 229-245) with a loop over real plans:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    isPremium: Boolean,
    isLoading: Boolean,
    availablePlans: List<SubscriptionPlan>,
    selectedPlan: SubscriptionPlan?,
    onSelectPlan: (SubscriptionPlan) -> Unit,
    onSubscribe: () -> Unit,
    onRestore: () -> Unit,
    navigateBack: () -> Unit,
) {
```

Replace the "Plan selector" block (previously lines 229-245) with:

```kotlin
                    Text(
                        text = "Elige tu plan",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))

                    availablePlans.forEach { plan ->
                        PlanCard(
                            label = if (plan.billingPeriod == BillingPeriod.MONTHLY) "Mensual" else "Anual",
                            price = plan.formattedPrice,
                            period = if (plan.billingPeriod == BillingPeriod.MONTHLY) "/mes" else "/año",
                            badge = if (plan.billingPeriod == BillingPeriod.ANNUAL) "Mejor valor" else null,
                            selected = plan == selectedPlan,
                            onClick = { onSelectPlan(plan) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
```

Update both `@Preview` functions at the bottom of the file (`PremiumScreenPreview`, `PremiumScreenAlreadyPremiumPreview`) to pass the new required params, e.g.:

```kotlin
@Preview(showBackground = true)
@Composable
fun PremiumScreenPreview() {
    val monthly = SubscriptionPlan("mtcquiz_premium_monthly", BillingPeriod.MONTHLY, "S/ 9.90")
    val annual = SubscriptionPlan("mtcquiz_premium_annual", BillingPeriod.ANNUAL, "S/ 29.90")
    MTCQuizTheme {
        PremiumScreen(
            isPremium = false,
            isLoading = false,
            availablePlans = listOf(monthly, annual),
            selectedPlan = annual,
            onSelectPlan = {},
            onSubscribe = {},
            onRestore = {},
            navigateBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PremiumScreenAlreadyPremiumPreview() {
    MTCQuizTheme {
        PremiumScreen(
            isPremium = true,
            isLoading = false,
            availablePlans = emptyList(),
            selectedPlan = null,
            onSelectPlan = {},
            onSubscribe = {},
            onRestore = {},
            navigateBack = {},
        )
    }
}
```

- [ ] **Step 6: Build the `configuration:presentation` module and confirm no other call sites broke**

Run: `./gradlew :configuration:presentation:compileDebugKotlin :configuration:presentation:test`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 7: Commit**

```bash
git add configuration/presentation/src/main/java/com/gondroid/configuration/presentation/premium configuration/presentation/src/test
git commit -m "feat: show real multi-plan pricing in PremiumScreen and wire paywall/subscribe/restore analytics"
```

---

### Task 5: Full build verification + Play Console setup notes

**Files:**
- None created — verification only, plus a short handoff note.
- Create: `docs/superpowers/plans/2026-08-06-premium-billing-upgrade-PLAY-CONSOLE-SETUP.md`

- [ ] **Step 1: Run the full unit test suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, 0 failures.

- [ ] **Step 2: Run a full assemble to catch any cross-module wiring issue the targeted compiles in earlier tasks missed**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Grep for any leftover reference to the deleted `BillingManager` type or the old hardcoded price string**

Run: `grep -rn "BillingManager\|S/ 19.99\|S/19.99" --include="*.kt" .`
Expected: no matches. If any test double or doc string still references `BillingManager` by name in a comment, that's fine — only fix actual Kotlin type references.

- [ ] **Step 4: Write the Play Console setup handoff note**

```markdown
# Play Console setup needed before this branch can be tested end-to-end

The code now queries **two** subscription products. Create both in Play Console →
Monetize → Products → Subscriptions, on the same app listing used for `mtcquiz_premium_annual`
today:

1. `mtcquiz_premium_monthly` — base plan billed monthly. Suggested price: S/ 9.90/mes.
2. `mtcquiz_premium_annual` — base plan billed yearly (this ID must stay exactly as-is;
   it's already referenced in the code). Suggested price: S/ 29.90/año.

Both must be type "Subscription" (`SUBS`), status "Active", with at least one base plan
and offer published — `PremiumRepositoryImpl.loadAvailablePlans()` reads the first offer's
first pricing phase (`subscriptionOfferDetails.first().pricingPhases.pricingPhaseList.first()`)
to get the price shown in the UI, so each product needs exactly one straightforward
recurring offer (no free trial/intro price needed for this to work, but if you add one,
confirm the first pricing phase is still the recurring one you want displayed — an intro
price phase would show first instead of the regular price).

Nothing else in the code needs to change once these two products are live — `PremiumScreen`
pulls price and period directly from what Play Console returns.
```

- [ ] **Step 5: Final commit**

```bash
git add docs/superpowers/plans/2026-08-06-premium-billing-upgrade-PLAY-CONSOLE-SETUP.md
git commit -m "docs: add Play Console setup checklist for the two premium subscription products"
```
