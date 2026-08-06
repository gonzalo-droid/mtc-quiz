# Pending Fixes Punch List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clear six previously-identified, independent minor defects: a missing image-load error state, two data-quality issues in extracted exam content, a missing ViewModel test, a broken androidTest DI wiring that blocks all instrumented tests, and a confusing input-validation UX bug.

**Architecture:** Six fully independent tasks touching six different files/areas. No task depends on another — any order works, and they can be reviewed/merged incrementally.

**Tech Stack:** Jetpack Compose, Kotlin, Hilt, JUnit4 + MockK + Truth + Robolectric (existing test stack), Coil3 for image loading, `pdftoppm`/`pdftohtml` (poppler-utils, already used by `.claude/skills/mtc-question-extractor/`) for PDF inspection.

## Global Constraints

- Do not touch anything outside each task's stated scope — these are narrow, independent fixes, not a refactor pass.
- For the two data-content tasks (2 and 3): this is real driving-exam content. Never invent or guess factual content. Where the correct text can't be determined with high confidence from the source PDF, leave it as-is and document the uncertainty in the task's commit message / report rather than guessing.
- `app/google-services.json` must be present in this worktree for `:app` module tests to run for real (already copied in during worktree setup) — if a fresh dispatch doesn't find it, that's worth flagging, not silently working around.

---

### Task 1: Coil error/placeholder state for question images

**Files:**
- Modify: `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardQuestion.kt`

**Interfaces:** None — self-contained UI change, no new public API.

**Problem:** `AsyncImage` in `CardQuestion.kt` (the composable that renders a question's associated sign/illustration images) has no `error` or `placeholder` parameter. If an image asset is missing, corrupted, or slow to load, Coil currently renders nothing at all — a silent blank gap in the card with no indication anything was supposed to be there.

- [ ] **Step 1: Add a Material icon as the error/placeholder visual**

Read the current file first (`core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardQuestion.kt`) to confirm the exact current `AsyncImage` call before editing — this plan was written against this content:

```kotlin
                        AsyncImage(
                            model = "file:///android_asset/images/$name.webp",
                            contentDescription = name,
                            modifier = Modifier.size(220.dp),
                            contentScale = ContentScale.Fit,
                        )
```

Replace it with a version that shows a `BrokenImage` icon (via `rememberVectorPainter`) while loading and on error, so a slow or failed load never renders as an empty gap:

```kotlin
                        AsyncImage(
                            model = "file:///android_asset/images/$name.webp",
                            contentDescription = name,
                            modifier = Modifier
                                .size(220.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp),
                                ),
                            contentScale = ContentScale.Fit,
                            placeholder = rememberVectorPainter(Icons.Outlined.Image),
                            error = rememberVectorPainter(Icons.Outlined.BrokenImage),
                        )
```

Add these imports:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.ui.graphics.vector.rememberVectorPainter
```

`core/presentation/designsystem`'s `build.gradle.kts` already depends on `libs.androidx.compose.material.icons.extended` (confirmed present — it's what every other screen in this codebase uses for icons like `Icons.Default.Menu`), so no new dependency is needed.

- [ ] **Step 2: Build and verify**

Run: `./gradlew :core:presentation:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Manual verification**

If an emulator/device is available, install the app (`./gradlew installDebug`), open a question with an image (e.g. category A-I, question 4 — "señal vertical reglamentaria R-3"), and confirm it still renders correctly (the placeholder/error path shouldn't visually interfere with a successful load). This is a nice-to-have check, not a hard requirement — compiling and the existing `CardQuestion` preview not crashing is the minimum bar.

- [ ] **Step 4: Commit**

```bash
git add core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardQuestion.kt
git commit -m "fix: show a placeholder/error icon when a question image fails to load"
```

---

### Task 2: Fix fundamento cross-leak on b2b/b2c questions 126/127

**Files:**
- Modify: `app/src/main/assets/json/b2b_questions.json`
- Modify: `app/src/main/assets/json/b2c_questions.json`

**Interfaces:** None — pure data fix, both files already validated by `app/src/test/java/com/gondroid/mtcquiz/data/local/QuestionAssetsSchemaTest.kt`, which must still pass after this change.

**Problem:** In both `b2b_questions.json` and `b2c_questions.json`, question 126's `fundamento` field bleeds into question 127's real content, and question 127's `fundamento` is missing its own opening. Confirmed current (wrong) state in `b2b_questions.json`:

```
id 126 fundamento: "El VIN esta constituido por 17 caracteres, según el artículo 12 del RNV Artículo 14 del RNV señala que 7. Parachoques delantero sin filos angulares cortantes, ni que"
id 127 fundamento: "excedan el ancho del vehículo. 8. Parachoques posterior y/o dispositivo antiempotramiento sin filos angulares cortantes, ni que excedan el ancho del vehículo. Tratándose de dispositivo antiempotramiento se debe cumplir con los requisitos técnicos aprobados."
```

This field is **not currently displayed anywhere in the app UI** (`Question.argument`, mapped from JSON key `fundamento`, has zero read sites in any Composable — confirmed by repo-wide grep in an earlier session). This is a real but currently invisible data-quality issue. Low urgency, but worth correcting since the source PDF is available to verify against.

- [ ] **Step 1: Render the relevant PDF page to see the real fundamento text and boundary**

```bash
mkdir -p /tmp/fundamento_check
pdftotext -layout app/src/main/assets/pdf/CLASE_B_IIB.pdf - | grep -n -A 3 -B 3 "Parachoques delantero"
```

This will show the real surrounding text from the PDF's own layout — read enough context to determine exactly where question 126's own fundamento ends and question 127's begins. If the text layout is ambiguous, render the actual page as an image and read it directly:

```bash
pdftotext -layout -f 1 -l 40 app/src/main/assets/pdf/CLASE_B_IIB.pdf - | grep -n "^12[4-8]" 
```
(adjust the page range `-f`/`-l` based on where question 126 falls — question density is roughly 5-6 questions per page in these PDFs, so question 126 is roughly around page 20-22; binary-search the page range if the first guess misses). Once you've located the right page number, render it as an image and read it directly for the clearest ground truth:
```bash
pdftoppm -png -r 150 -f <page> -l <page> app/src/main/assets/pdf/CLASE_B_IIB.pdf /tmp/fundamento_check/page
```
Then read the resulting PNG with the Read tool.

- [ ] **Step 2: Correct both files' `fundamento` fields for ids 126 and 127**

Based on what the rendered page actually shows, edit `app/src/main/assets/json/b2b_questions.json` and `app/src/main/assets/json/b2c_questions.json` (the leak is present identically in both, per the report this plan is based on) so that:
- Question 126's `fundamento` contains only its own real text (likely just the VIN/article-12 sentence, without the "Artículo 14... Parachoques delantero..." fragment that belongs to 127).
- Question 127's `fundamento` contains its own complete text, starting from "Artículo 14 del RNV señala que 7. Parachoques delantero..." through to the end (both the delantero and posterior points, if the PDF presents them as one fundamento for a single question — verify this against the rendered page rather than assuming).

Use the Edit tool directly on the JSON files — do not run any extraction script for this, it's a two-field hand correction once you know the correct text from the rendered page.

- [ ] **Step 3: Verify the JSON files are still valid and pass the existing schema test**

Run: `python3 -c "import json; json.load(open('app/src/main/assets/json/b2b_questions.json')); json.load(open('app/src/main/assets/json/b2c_questions.json')); print('valid json')"`
Expected: `valid json`

Run: `./gradlew :app:testDebugUnitTest --tests "com.gondroid.mtcquiz.data.local.QuestionAssetsSchemaTest"`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/json/b2b_questions.json app/src/main/assets/json/b2c_questions.json
git commit -m "fix: correct fundamento cross-leak between b2b/b2c questions 126 and 127"
```

---

### Task 3: Review b2a/b2b/b2c questions 13-15 option text against their real images

**Files:**
- Modify (only if you find and confirm a real discrepancy): `app/src/main/assets/json/b2a_questions.json`, `app/src/main/assets/json/b2b_questions.json`, `app/src/main/assets/json/b2c_questions.json`

**Interfaces:** None — pure data review/fix, same schema test as Task 2 applies if you touch these files.

**Problem (from an earlier session's review):** For `b2a` ids 13-15, some option text describes something different from what the associated image actually shows (e.g. id 13 option c's text says "animales en la vía" but the actual sign image is a cyclist-warning icon; id 13 option b claims "figura de motocicleta" but the real sign is "NO VOLTEAR A LA IZQUIERDA"; id 15's option a claims "giro prohibido" but the real sign is a crossed-out motorcycle, and option c claims "ceda el paso... con flecha" but the real sign is "MANTENGA SU DERECHA"). For `b2b`/`b2c`, the same ids (12,13,14,15,16,17,18,21,23,24) had some options that were literally just bare `"a)"`/`"b)"`/`"c)"` labels with no descriptive text at all (the PDF renders those as picture-only options) — a prior task already recovered most of these from `b2a`'s hand-curated text, but ids 14 and 15 were deliberately left alone at the time because it looked like their bare-letter options were the PDF's genuine intentional layout (each letter maps to its own distinct image, confirmed by separate `.webp` files existing per option).

This task is a targeted content review, not a rewrite: verify each of these specific rows against the actual images already extracted to `app/src/main/assets/images/`, and only change text you can confirm is wrong by looking at the real sign.

- [ ] **Step 1: Read the actual images for the affected rows**

For `b2a`, `b2b`, and `b2c`, read (with the Read tool — it supports images) the webp files for ids 13, 14, and 15. The naming convention is `q{id}_{letter}_{examId}.webp` (see `.claude/skills/mtc-question-extractor/SKILL.md` for the full naming rules, including the a3b/a3c document-position exception, which does not apply to b2a/b2b/b2c since those don't have restarting Nº columns). For example, for b2a id 13's options: `app/src/main/assets/images/q13_a_b2a.webp`, `q13_b_b2a.webp`, `q13_c_b2a.webp`, etc. — check which images actually exist for each id (some options may not have an image at all, meaning that option's real content genuinely is text-only in the PDF).

Cross-reference what you see against `app/src/main/assets/json/b2a_questions.json`'s (and b2b's, and c2c's) current `options` array for the same id.

- [ ] **Step 2: For each confirmed real mismatch, correct the option text**

Only edit an option's text if you can clearly see the image shows something different from what the text claims (e.g. the text says "cyclist" but the image is unambiguously a different sign). Write a corrected description that accurately matches the real sign — keep the existing `"a) "`/`"b) "`/etc. letter-prefix format used throughout this dataset. If you're not confident what the correct description should be (the image is ambiguous, or you can't tell which real-world MTC sign it corresponds to), do not guess — leave that specific option as-is and note it in your report as still needing human review.

For `b2b`/`b2c` ids 14 and 15 specifically: first confirm whether each lettered option genuinely has its own distinct image (if so, the bare `"a)"`/`"b)"` labels may be intentionally minimal and pairing them with a real description per option is the fix — write one only if the image content is clear enough to describe confidently) or whether some options are actually blank/duplicate placeholders (in which case leave them, matching the earlier session's judgment call not to touch them without more certainty).

- [ ] **Step 3: Verify JSON validity and schema test**

Run: `python3 -c "import json; [json.load(open(f'app/src/main/assets/json/{f}')) for f in ('b2a_questions.json','b2b_questions.json','b2c_questions.json')]; print('valid json')"`
Expected: `valid json`

Run: `./gradlew :app:testDebugUnitTest --tests "com.gondroid.mtcquiz.data.local.QuestionAssetsSchemaTest"`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit, documenting exactly what changed and what's still uncertain**

```bash
git add app/src/main/assets/json/b2a_questions.json app/src/main/assets/json/b2b_questions.json app/src/main/assets/json/b2c_questions.json
git commit -m "fix: correct option text that didn't match its sign image (b2a/b2b/b2c ids 13-15)"
```

If you made zero changes because nothing could be confirmed with high confidence, commit nothing for this task and say so plainly in your final report — do not force a commit just to have one.

---

### Task 4: Add `DetailScreenViewModelTest`

**Files:**
- Create: `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/detail/DetailScreenViewModelTest.kt`

**Interfaces:**
- Consumes: `DetailScreenViewModel` (constructor: `savedStateHandle: SavedStateHandle, repository: QuizRepository, billingManager: BillingManager, adsManager: AdsManager, bannerAdId: String` — file: `detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailScreenViewModel.kt`), `QuizRepositoryFake` (already exists at `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/QuizRepositoryFake.kt`, has categories with ids `"1"` and `"2"`).

**Problem:** `DetailScreenViewModel` has no test coverage at all — it was extended with `BillingManager`/`isPremium` wiring in an earlier session with no accompanying test. `HomeScreenViewModelTest` and `PdfScreenViewModelTest` (both in `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/`) show the two patterns to combine here: an inline fake `BillingManager` object (from `HomeScreenViewModelTest`) and a `SavedStateHandle`-driven Robolectric test with a mocked `AdsManager` (from `PdfScreenViewModelTest`).

- [ ] **Step 1: Read both precedent test files first**

Read `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/home/HomeScreenViewModelTest.kt` and `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/pdf/PdfScreenViewModelTest.kt` in full before writing this test, so the style matches exactly (Truth assertions, MockK, `@RunWith(RobolectricTestRunner::class)`, `MainDispatcherRule`).

- [ ] **Step 2: Write the test file**

```kotlin
package com.gondroid.mtcquiz.presentation.screens.detail

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.gondroid.core.data.ads.AdsManager
import com.gondroid.core.data.billing.BillingManager
import com.gondroid.detail.presentation.DetailEvent
import com.gondroid.detail.presentation.DetailScreenViewModel
import com.gondroid.mtcquiz.presentation.screens.QuizRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DetailScreenViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val adsManager: AdsManager = mockk(relaxed = true)

    private fun createViewModel(
        isPremium: Boolean = false,
        categoryId: String = "1",
    ): DetailScreenViewModel {
        val billingManager = object : BillingManager {
            override val isPremiumFlow: Flow<Boolean> = flowOf(isPremium)
            override suspend fun launchSubscription(activity: Activity): Boolean = false
            override suspend fun refreshPurchaseState() = Unit
            override suspend fun restorePurchases() = Unit
        }
        val savedStateHandle = SavedStateHandle(mapOf("categoryId" to categoryId))
        return DetailScreenViewModel(
            savedStateHandle = savedStateHandle,
            repository = QuizRepositoryFake(),
            billingManager = billingManager,
            adsManager = adsManager,
            bannerAdId = "test-banner-id",
        )
    }

    @Test
    fun `state loads the category matching the route's categoryId`() = runTest {
        val vm = createViewModel(categoryId = "1")
        advanceUntilIdle()
        assertThat(vm.state.value.category.id).isEqualTo("1")
        assertThat(vm.state.value.category.title).isEqualTo("CLASE A - CATEGORIA I")
    }

    @Test
    fun `state reflects isPremium from BillingManager`() = runTest {
        val vm = createViewModel(isPremium = true)
        advanceUntilIdle()
        assertThat(vm.state.value.isPremium).isTrue()
    }

    @Test
    fun `state defaults isPremium to false when BillingManager reports not premium`() = runTest {
        val vm = createViewModel(isPremium = false)
        advanceUntilIdle()
        assertThat(vm.state.value.isPremium).isFalse()
    }

    @Test
    fun `onStartEvaluation navigates directly when interstitial should not show`() = runTest {
        coEvery { adsManager.shouldShowEvaluationInterstitial() } returns false
        val vm = createViewModel()
        vm.events.test {
            vm.onStartEvaluation("1")
            assertThat(awaitItem()).isEqualTo(DetailEvent.NavigateToEvaluation("1"))
        }
        coVerify { adsManager.recordEvaluationStart() }
    }

    @Test
    fun `onStartEvaluation shows interstitial event when interstitial should show`() = runTest {
        coEvery { adsManager.shouldShowEvaluationInterstitial() } returns true
        val vm = createViewModel()
        vm.events.test {
            vm.onStartEvaluation("1")
            assertThat(awaitItem()).isEqualTo(DetailEvent.ShowEvaluationInterstitial)
        }
        coVerify { adsManager.recordEvaluationStart() }
    }

    @Test
    fun `onInterstitialClosed navigates to evaluation`() = runTest {
        val vm = createViewModel()
        vm.events.test {
            vm.onInterstitialClosed("1")
            assertThat(awaitItem()).isEqualTo(DetailEvent.NavigateToEvaluation("1"))
        }
    }
}
```

- [ ] **Step 3: Run the test to verify it passes**

Run: `./gradlew :app:test --tests "com.gondroid.mtcquiz.presentation.screens.detail.DetailScreenViewModelTest"`
Expected: BUILD SUCCESSFUL, 6 tests passed.

If `DetailEvent` isn't publicly accessible from the `app` module's test source set (check the actual visibility modifier in `detail/presentation/src/main/java/com/gondroid/detail/presentation/DetailScreenViewModel.kt` — it should be a public `sealed interface`, so this should just work, matching how `PdfEvent` is used in `PdfScreenViewModelTest.kt`), fix the import rather than working around it.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/com/gondroid/mtcquiz/presentation/screens/detail/DetailScreenViewModelTest.kt
git commit -m "test: add DetailScreenViewModelTest covering category load, isPremium, and interstitial gating"
```

---

### Task 5: Fix broken `TestDataModule` import blocking all instrumented tests

**Files:**
- Modify: `app/src/androidTest/kotlin/TestDataModule.kt`

**Interfaces:** None — this is a one-line import fix.

**Problem:** `./gradlew :app:kspDebugAndroidTestKotlin` currently fails, which means **no instrumented (androidTest) test in this project can currently compile or run at all**. Root cause: `TestDataModule.kt` imports `com.gondroid.mtcquiz.data.di.DataModule` — a class that does not exist at that package path. The real `DataModule` Hilt module lives at `com.gondroid.core.database.di.DataModule` (file: `core/database/src/main/java/com/gondroid/core/database/di/DataModule.kt`), which `app`'s `build.gradle.kts` already depends on via `implementation(projects.core.database)` — so this is purely a wrong import path, not a missing dependency.

- [ ] **Step 1: Read the current file**

Read `app/src/androidTest/kotlin/TestDataModule.kt` in full to confirm the current wrong import before editing.

- [ ] **Step 2: Fix the import**

Change:
```kotlin
import com.gondroid.mtcquiz.data.di.DataModule
```
to:
```kotlin
import com.gondroid.core.database.di.DataModule
```

- [ ] **Step 3: Also add the missing `DismissedQuestionDao` provider for parity with the real module**

The real `DataModule` (`core/database/src/main/java/com/gondroid/core/database/di/DataModule.kt`) provides both `EvaluationDao` and `DismissedQuestionDao`. `TestDataModule` currently only provides `EvaluationDao` — add the missing one so `TestDataModule` is a complete `@TestInstallIn` replacement, not a partial one that would fail confusingly later if any androidTest ever needs `DismissedQuestionDao`:

```kotlin
    @Provides
    fun provideDismissedQuestionDao(database: MTCDatabase): DismissedQuestionDao = database.dismissedQuestionDao()
```

Add the import `import com.gondroid.core.database.dao.DismissedQuestionDao` (matching the real module's import).

- [ ] **Step 4: Verify the androidTest source set now compiles**

Run: `./gradlew :app:kspDebugAndroidTestKotlin :app:compileDebugAndroidTestKotlin`
Expected: BUILD SUCCESSFUL — this was the exact failing task before this fix.

Do not attempt to run `connectedAndroidTest` (that requires a connected emulator/device and is out of scope for this fix — the goal here is just to unblock compilation, which was completely broken before).

- [ ] **Step 5: Commit**

```bash
git add app/src/androidTest/kotlin/TestDataModule.kt
git commit -m "fix: correct TestDataModule's DataModule import path, unblocking androidTest compilation"
```

---

### Task 6: Fix confusing input validation in the Customize/Personalization screen

**Files:**
- Modify: `configuration/presentation/src/main/java/com/gondroid/configuration/presentation/customize/CustomizeScreen.kt`

**Interfaces:** None — internal UI behavior change, `ItemField`'s signature gains one new optional parameter but all existing call sites in this same file are updated in this same task.

**Problem:** Currently, `ItemField`'s `onValueChange` callback (wired up in `CustomizeScreen`) only updates the field's displayed value when the typed number is already within its valid range — e.g. for the "1 - 1000" field, typing "2000" makes the last "0" keystroke appear to do nothing (the field silently reverts to "200", the last valid value), with no error message explaining why. This reads as a broken/unresponsive text field rather than validation feedback.

- [ ] **Step 1: Read the current file**

Read `configuration/presentation/src/main/java/com/gondroid/configuration/presentation/customize/CustomizeScreen.kt` in full to confirm current content before editing (this plan was written against the version already fixed in a prior session's "remove dead empty space" commit, which wrapped the three `ItemField`s in a `Card` — that structure should remain unchanged, only the validation logic inside changes).

- [ ] **Step 2: Change `ItemField` to accept an explicit `isError` + error message, instead of inferring only from blank**

Replace the `ItemField` composable's current body:

```kotlin
@Composable
fun ItemField(
    label: String,
    subLabel: String = "",
    modifier: Modifier,
    value: String,
    onValueChange: (String) -> Unit
) {

    Text(
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        text = label
    )
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
        },
        maxLines = 1,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
    )
    if (value.isBlank()) {
        Text(
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            text = "Debe ingresar un valor"
        )
    } else {
        Text(
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            text = subLabel
        )
    }


}
```

with:

```kotlin
@Composable
fun ItemField(
    label: String,
    subLabel: String = "",
    modifier: Modifier,
    value: String,
    isError: Boolean = false,
    errorMessage: String = "",
    onValueChange: (String) -> Unit
) {

    Text(
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        text = label
    )
    OutlinedTextField(
        modifier = modifier,
        value = value,
        onValueChange = { newValue ->
            onValueChange(newValue)
        },
        isError = isError || value.isBlank(),
        maxLines = 1,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
    )
    when {
        value.isBlank() -> Text(
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            text = "Debe ingresar un valor"
        )
        isError -> Text(
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            text = errorMessage
        )
        else -> Text(
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            text = subLabel
        )
    }


}
```

- [ ] **Step 3: Update `CustomizeScreen`'s three `ItemField` call sites so the field always reflects what was typed, with a range check surfaced via `isError` instead of silently rejecting keystrokes**

Replace the three `onValueChange` lambdas (currently filtering the input before deciding whether to update state at all) with versions that always accept digit-only input and let `isError` carry the range violation:

```kotlin
            ItemField(
                value = timeToFinishEvaluation,
                label = stringResource(R.string.time_to_evaluation),
                subLabel = "1 - 1000",
                isError = timeToFinishEvaluation.isNotBlank() &&
                    (timeToFinishEvaluation.toIntOrNull() == null || timeToFinishEvaluation.toIntOrNull() !in 1..1000),
                errorMessage = "Debe ser un número entre 1 y 1000",
                modifier = Modifier.fillMaxWidth()
            ) { newValue ->
                if (newValue.all { it.isDigit() }) {
                    timeToFinishEvaluation = newValue
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ItemField(
                value = numberQuestions,
                label = stringResource(R.string.number_of_question_to_evaluation),
                subLabel = "1 - 1000",
                isError = numberQuestions.isNotBlank() &&
                    (numberQuestions.toIntOrNull() == null || numberQuestions.toIntOrNull() !in 1..1000),
                errorMessage = "Debe ser un número entre 1 y 1000",
                modifier = Modifier.fillMaxWidth(),
            ) { newValue ->
                if (newValue.all { it.isDigit() }) {
                    numberQuestions = newValue
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ItemField(
                value = percentageToApprovedEvaluation,
                label = stringResource(R.string.percentage_approbe_to_evaluation),
                subLabel = "1 - 100 (%)",
                isError = percentageToApprovedEvaluation.isNotBlank() &&
                    (percentageToApprovedEvaluation.toIntOrNull() == null || percentageToApprovedEvaluation.toIntOrNull() !in 1..100),
                errorMessage = "Debe ser un número entre 1 y 100",
                modifier = Modifier.fillMaxWidth(),
            ) { newValue ->
                if (newValue.all { it.isDigit() }) {
                    percentageToApprovedEvaluation = newValue
                }
            }
```

- [ ] **Step 4: Update the "Actualizar valores" button's `enabled` condition to also require all three values to be in range, not just non-blank**

Find the `ButtonsAction(...)` call at the bottom of `CustomizeScreen` and replace its `enabled` expression:

```kotlin
                enabled = numberQuestions.isNotBlank() && timeToFinishEvaluation.isNotBlank() && percentageToApprovedEvaluation.isNotBlank(),
```

with:

```kotlin
                enabled = timeToFinishEvaluation.toIntOrNull()?.let { it in 1..1000 } == true &&
                    numberQuestions.toIntOrNull()?.let { it in 1..1000 } == true &&
                    percentageToApprovedEvaluation.toIntOrNull()?.let { it in 1..100 } == true,
```

- [ ] **Step 5: Build and verify**

Run: `./gradlew :configuration:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Manual verification**

If an emulator/device is available, install (`./gradlew installDebug`), navigate to Configuración → Personalización, type a value outside the valid range in one field (e.g. "2000" in the "1-1000" field), and confirm: (a) the field visibly shows what you typed (doesn't silently revert), (b) an error message appears explaining the valid range, (c) the "Actualizar valores" button becomes disabled while any field is invalid, (d) correcting the value back into range re-enables the button and shows the normal subLabel text again.

- [ ] **Step 7: Commit**

```bash
git add configuration/presentation/src/main/java/com/gondroid/configuration/presentation/customize/CustomizeScreen.kt
git commit -m "fix: show inline range-validation errors in Customize screen instead of silently rejecting input"
```

## Self-Review Notes

- **Spec coverage:** all 6 previously-identified items have a task: Coil placeholder/error (Task 1), fundamento cross-leak (Task 2), b2a/b2b/b2c option-text mismatches (Task 3), missing DetailScreenViewModelTest (Task 4), broken androidTest compilation (Task 5), Customize screen validation UX (Task 6).
- **Independence:** all 6 tasks touch disjoint files and have no cross-task interfaces — they can be done in any order, and a failure/block on one task (e.g. Task 3's content review taking longer or finding nothing confidently fixable) doesn't block any other task.
- **Placeholder scan:** Tasks 2 and 3 intentionally don't hardcode the exact corrected text, since that must come from reading the real PDF/images — this is not a "TBD" placeholder, it's a documented investigation step with an exact, runnable procedure (render page/read image, then hand-edit) and an explicit instruction not to guess if uncertain, which satisfies the plan's own Global Constraints section on this exact point.
