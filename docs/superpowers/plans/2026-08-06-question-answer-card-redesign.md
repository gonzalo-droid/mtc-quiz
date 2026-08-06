# Question/Answer Card Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current "question card + separate floating answer cards" pattern used by the Evaluación (Simulacro) and Estudio screens with one unified card per question, fix hardcoded colors that break dark theme, and add non-color (letter badge + icon) feedback for accessibility.

**Architecture:** Two new shared Compose components in `core:presentation:designsystem:components` (`AnswerOptionRow`, `QuestionAnswerCard`) replace the current `CardAnswer`/`CardQuestion` pair. `EvaluationScreen` and `QuestionsScreen` (Estudio) each migrate to the new components, supplying their own per-option state mapping — the shared components hold no screen-specific logic. A new `ExtendedColors` composition local supplies a success (green) container color pair that Material3's stock `ColorScheme` has no slot for.

**Tech Stack:** Jetpack Compose, Material3, Coil3 (`coil3.compose.AsyncImage`), existing `MTCQuizTheme`.

## Global Constraints

- Scope is "correction + polish," not an interaction redesign: no change to either screen's top bar, timer, progress bar, search, ViewModel, state class, or navigation.
- No DB/network/DataStore schema involved — presentation-layer only.
- Follow the existing house `Card` style already used in `DetailScreen.kt`/`CustomizeScreen.kt`: `containerColor = MaterialTheme.colorScheme.surfaceContainerLow`, `shape = RoundedCornerShape(16.dp)`.
- Design spec (read for full rationale, not required for implementation — this plan already carries every value needed): `docs/superpowers/specs/2026-08-06-question-answer-card-redesign-design.md`.

---

### Task 1: Success color tokens + `ExtendedColors` theme wiring

**Files:**
- Modify: `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/Color.kt`
- Create: `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/ExtendedColors.kt`
- Modify: `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/Theme.kt`

**Interfaces:**
- Produces: `val MaterialTheme.extendedColors: ExtendedColors` (readable from any `@Composable` inside `MTCQuizTheme { }`), with `ExtendedColors.successContainer: Color` and `ExtendedColors.onSuccessContainer: Color` — Task 2 consumes this.

- [ ] **Step 1: Add the four new color constants**

Open `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/Color.kt`. Find the light-scheme constant block (it has `val tertiaryLight = ...`, `val errorLight = ...` etc.) and add these four lines near the other `*Light`/`*Dark` container pairs (exact placement doesn't matter, keep light and dark grouped like the rest of the file):

```kotlin
val successContainerLight = Color(0xFFC7F7CB)
val onSuccessContainerLight = Color(0xFF0D3311)
val successContainerDark = Color(0xFF1E4620)
val onSuccessContainerDark = Color(0xFFA6F5AC)
```

- [ ] **Step 2: Create `ExtendedColors.kt`**

```kotlin
package com.gondroid.core.presentation.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class ExtendedColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        successContainer = successContainerLight,
        onSuccessContainer = onSuccessContainerLight,
    )
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
```

- [ ] **Step 3: Provide it from `MTCQuizTheme`**

Read `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/Theme.kt` first to confirm it still matches this plan's expectation (it should be exactly the `lightColorScheme(...)`/`darkColorScheme(...)`/`MTCQuizTheme` structure with no custom `ColorScheme` fields — if it has changed since this plan was written, adapt Step 3 to the real structure rather than guessing).

Replace the `MTCQuizTheme` function body:

```kotlin
@Composable
fun MTCQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkScheme else lightScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
```

with:

```kotlin
@Composable
fun MTCQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) darkScheme else lightScheme
    val extendedColors = if (darkTheme) {
        ExtendedColors(
            successContainer = successContainerDark,
            onSuccessContainer = onSuccessContainerDark,
        )
    } else {
        ExtendedColors(
            successContainer = successContainerLight,
            onSuccessContainer = onSuccessContainerLight,
        )
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
```

Add the import `androidx.compose.runtime.CompositionLocalProvider` to `Theme.kt`.

- [ ] **Step 4: Compile-verify**

Run: `./gradlew :core:presentation:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/Color.kt core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/ExtendedColors.kt core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/Theme.kt
git commit -m "feat: add success container color tokens and ExtendedColors theme local"
```

---

### Task 2: `AnswerOptionRow` composable

**Files:**
- Create: `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/AnswerOptionRow.kt`

**Interfaces:**
- Consumes: `MaterialTheme.extendedColors` (from Task 1).
- Produces: `enum class AnswerOptionState { Unselected, Selected, RevealedCorrect, RevealedIncorrect, CorrectAnswerHint }` and `@Composable fun AnswerOptionRow(letter: String, text: String, state: AnswerOptionState, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null)` — Task 3 and Tasks 4/5 consume both.

- [ ] **Step 1: Create the file**

```kotlin
package com.gondroid.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gondroid.core.presentation.designsystem.MTCQuizTheme
import com.gondroid.core.presentation.designsystem.extendedColors

enum class AnswerOptionState {
    Unselected,
    Selected,
    RevealedCorrect,
    RevealedIncorrect,
    CorrectAnswerHint,
}

private data class AnswerOptionColors(
    val container: Color,
    val content: Color,
    val badgeContainer: Color,
    val badgeContent: Color,
)

@Composable
private fun colorsFor(state: AnswerOptionState): AnswerOptionColors {
    val scheme = MaterialTheme.colorScheme
    val extended = MaterialTheme.extendedColors
    return when (state) {
        AnswerOptionState.Unselected -> AnswerOptionColors(
            container = Color.Transparent,
            content = scheme.onSurface,
            badgeContainer = scheme.surfaceContainerHighest,
            badgeContent = scheme.onSurfaceVariant,
        )

        AnswerOptionState.Selected -> AnswerOptionColors(
            container = scheme.secondaryContainer,
            content = scheme.onSecondaryContainer,
            badgeContainer = scheme.secondaryContainer,
            badgeContent = scheme.onSecondaryContainer,
        )

        AnswerOptionState.RevealedCorrect, AnswerOptionState.CorrectAnswerHint -> AnswerOptionColors(
            container = extended.successContainer,
            content = extended.onSuccessContainer,
            badgeContainer = extended.successContainer,
            badgeContent = extended.onSuccessContainer,
        )

        AnswerOptionState.RevealedIncorrect -> AnswerOptionColors(
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
            badgeContainer = scheme.errorContainer,
            badgeContent = scheme.onErrorContainer,
        )
    }
}

@Composable
fun AnswerOptionRow(
    letter: String,
    text: String,
    state: AnswerOptionState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = colorsFor(state)

    Row(
        modifier = modifier
            .background(colors.container, RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(colors.badgeContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.labelSmall,
                color = colors.badgeContent,
                textAlign = TextAlign.Center,
            )
        }

        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = colors.content,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )

        when (state) {
            AnswerOptionState.RevealedCorrect, AnswerOptionState.CorrectAnswerHint -> Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "correct",
                tint = colors.content,
                modifier = Modifier.size(18.dp),
            )

            AnswerOptionState.RevealedIncorrect -> Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "incorrect",
                tint = colors.content,
                modifier = Modifier.size(18.dp),
            )

            else -> Unit
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAnswerOptionRowStates() {
    MTCQuizTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            AnswerOptionRow(letter = "A", text = "Opción sin seleccionar", state = AnswerOptionState.Unselected, onClick = {})
            AnswerOptionRow(letter = "B", text = "Opción seleccionada (sin verificar)", state = AnswerOptionState.Selected, onClick = {})
            AnswerOptionRow(letter = "C", text = "Respuesta correcta revelada", state = AnswerOptionState.RevealedCorrect)
            AnswerOptionRow(letter = "D", text = "Respuesta incorrecta elegida", state = AnswerOptionState.RevealedIncorrect)
            AnswerOptionRow(letter = "A", text = "Esta era la correcta (el usuario eligió otra)", state = AnswerOptionState.CorrectAnswerHint)
        }
    }
}
```

- [ ] **Step 2: Compile-verify**

Run: `./gradlew :core:presentation:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/AnswerOptionRow.kt
git commit -m "feat: add AnswerOptionRow replacing CardAnswer with theme-aware states"
```

---

### Task 3: `QuestionAnswerCard` composable

**Files:**
- Create: `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/QuestionAnswerCard.kt`

**Interfaces:**
- Consumes: `AnswerOptionRow`, `AnswerOptionState` (from Task 2).
- Produces: `data class AnswerOption(val letter: String, val text: String, val state: AnswerOptionState)` and `@Composable fun QuestionAnswerCard(title: String, options: List<AnswerOption>, modifier: Modifier = Modifier, questionImages: List<String> = emptyList(), onOptionClick: ((index: Int) -> Unit)? = null)` — Tasks 4 and 5 consume both.

- [ ] **Step 1: Create the file**

```kotlin
package com.gondroid.core.presentation.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.gondroid.core.presentation.designsystem.MTCQuizTheme

data class AnswerOption(
    val letter: String,
    val text: String,
    val state: AnswerOptionState,
)

@Composable
fun QuestionAnswerCard(
    title: String,
    options: List<AnswerOption>,
    modifier: Modifier = Modifier,
    questionImages: List<String> = emptyList(),
    onOptionClick: ((index: Int) -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (questionImages.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    ) {
                        items(questionImages) { name ->
                            QuestionImage(name = name)
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                options.forEachIndexed { index, option ->
                    AnswerOptionRow(
                        modifier = Modifier.fillMaxWidth(),
                        letter = option.letter,
                        text = option.text,
                        state = option.state,
                        onClick = onOptionClick?.let { callback -> { callback(index) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestionImage(name: String) {
    var isLoaded by remember(name) { mutableStateOf(false) }
    AsyncImage(
        model = "file:///android_asset/images/$name.webp",
        contentDescription = name,
        modifier = Modifier
            .size(220.dp)
            .then(
                if (!isLoaded) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            ),
        contentScale = ContentScale.Fit,
        placeholder = rememberVectorPainter(Icons.Outlined.Image),
        error = rememberVectorPainter(Icons.Outlined.BrokenImage),
        colorFilter = if (!isLoaded) ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant) else null,
        onState = { state -> isLoaded = state is AsyncImagePainter.State.Success },
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewQuestionAnswerCardNoImage() {
    MTCQuizTheme {
        QuestionAnswerCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            title = "3.- ¿Cuál señal indica velocidad máxima?",
            options = listOf(
                AnswerOption("A", "Círculo rojo con número", AnswerOptionState.RevealedCorrect),
                AnswerOption("B", "Triángulo amarillo", AnswerOptionState.Unselected),
                AnswerOption("C", "Rombo azul", AnswerOptionState.Unselected),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewQuestionAnswerCardWithImages() {
    MTCQuizTheme {
        QuestionAnswerCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            title = "1.- ¿Cuál de las siguientes señales es preventiva?",
            questionImages = listOf("q14_a_b2a", "q14_b_b2a", "q14_c_b2a"),
            options = listOf(
                AnswerOption("A", "Camino sinuoso", AnswerOptionState.Selected),
                AnswerOption("B", "No camiones", AnswerOptionState.Unselected),
                AnswerOption("C", "Ciclistas en pendiente", AnswerOptionState.Unselected),
            ),
            onOptionClick = {},
        )
    }
}
```

- [ ] **Step 2: Compile-verify**

Run: `./gradlew :core:presentation:designsystem:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If `AsyncImagePainter.State` or the `onState` parameter on `coil3.compose.AsyncImage` doesn't resolve, check the Coil3 version in `gradle/libs.versions.toml` and confirm the import path is `coil3.compose.AsyncImagePainter` (not `coil3.compose.AsyncImagePainter.State` as a separate import) — `State` is a nested sealed interface, referenced as `AsyncImagePainter.State.Success`.

- [ ] **Step 3: Commit**

```bash
git add core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/QuestionAnswerCard.kt
git commit -m "feat: add QuestionAnswerCard replacing CardQuestion with a unified question+answers surface"
```

---

### Task 4: Migrate `EvaluationScreen.kt`

**Files:**
- Modify: `evaluation/presentation/src/main/java/com/gondroid/evaluation/presentation/EvaluationScreen.kt`

**Interfaces:**
- Consumes: `QuestionAnswerCard`, `AnswerOption`, `AnswerOptionState` (from Tasks 2/3).

- [ ] **Step 1: Read the current file first**

Read `evaluation/presentation/src/main/java/com/gondroid/evaluation/presentation/EvaluationScreen.kt` in full before editing — this plan was written against the version merged at commit `4590333` on `master`. If it has diverged, adapt the edits below to the real current content rather than blindly applying a patch that no longer matches.

- [ ] **Step 2: Update imports**

Replace:
```kotlin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
```
with:
```kotlin
import androidx.compose.foundation.lazy.LazyColumn
```

Replace:
```kotlin
import com.gondroid.core.presentation.designsystem.components.CardAnswer
import com.gondroid.core.presentation.designsystem.components.CardQuestion
import com.gondroid.core.presentation.designsystem.components.LinearProgressComponent
```
with:
```kotlin
import com.gondroid.core.presentation.designsystem.components.AnswerOption
import com.gondroid.core.presentation.designsystem.components.AnswerOptionState
import com.gondroid.core.presentation.designsystem.components.LinearProgressComponent
import com.gondroid.core.presentation.designsystem.components.QuestionAnswerCard
```

- [ ] **Step 3: Replace the LazyColumn body**

Replace this whole block (currently rendering `CardQuestion` then a mid-list weighted `Spacer` then `itemsIndexed` over `AnswerCard`):

```kotlin
            LazyColumn(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Top
            ) {
                item {
                    CardQuestion(
                        modifier = Modifier.fillMaxWidth(),
                        title = "${state.question.id}.- ${state.question.title}",
                        questionImages = state.question.imagens,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item {
                    Spacer(modifier = Modifier.weight(1f))
                }

                itemsIndexed(state.question.options) { index, option ->
                    AnswerCard(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = option,
                        isCorrect = state.question.isCorrectAnswer(index),
                        isSelected = selectedOption == option,
                        isCorrectAnswerSelected = if (state.answerWasSelected) isCorrectAnswerSelected else null,
                        onClick = {
                            selectedOption = option
                        })
                }
            }
```

with:

```kotlin
            LazyColumn(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Top
            ) {
                item {
                    QuestionAnswerCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "${state.question.id}.- ${state.question.title}",
                        questionImages = state.question.imagens,
                        options = state.question.options.mapIndexed { index, option ->
                            val letter = ('a' + index).uppercaseChar().toString()
                            val optionState = when {
                                state.answerWasSelected -> when {
                                    option == selectedOption && isCorrectAnswerSelected -> AnswerOptionState.RevealedCorrect
                                    option == selectedOption && !isCorrectAnswerSelected -> AnswerOptionState.RevealedIncorrect
                                    !isCorrectAnswerSelected && state.question.isCorrectAnswer(index) -> AnswerOptionState.CorrectAnswerHint
                                    else -> AnswerOptionState.Unselected
                                }
                                option == selectedOption -> AnswerOptionState.Selected
                                else -> AnswerOptionState.Unselected
                            }
                            AnswerOption(letter = letter, text = option, state = optionState)
                        },
                        onOptionClick = if (!state.answerWasSelected) {
                            { index -> selectedOption = state.question.options[index] }
                        } else {
                            null
                        },
                    )
                }
            }
```

This is a direct behavior-preserving translation of the removed `AnswerCard`'s nested `when` (same outer `state.answerWasSelected` gate, same four inner branches) — read both side by side if anything looks off, don't guess at a simplification.

The mid-list `Spacer(modifier = Modifier.weight(1f))` item is intentionally dropped: it existed to space apart the old multi-item list (question card + N separate answer-card items); with a single `QuestionAnswerCard` item there is nothing left to space apart.

- [ ] **Step 4: Delete the now-unused `AnswerCard` function**

Find and delete this entire function (it's no longer called anywhere in this file):

```kotlin
@Composable
fun AnswerCard(
    state: EvaluationState,
    text: String,
    isCorrect: Boolean,
    isSelected: Boolean,
    isCorrectAnswerSelected: Boolean?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val (backgroundColor, borderColor) = when {
        state.answerWasSelected -> when {
            isSelected && isCorrect -> Color(0xFFC8E6C9) to Color(0xFF388E3C)
            isSelected && !isCorrect -> Color(0xFFFFCDD2) to Color(0xFFD32F2F)
            isCorrectAnswerSelected == false && isCorrect -> Color(0xFFC8E6C9) to Color(0xFF388E3C)
            else -> Color.White to Color.Gray
        }

        isSelected -> MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.secondary
        else -> Color.White to Color.Gray
    }

    val textColor = if (isSelected && !state.answerWasSelected) Color.White else Color.Black

    CardAnswer(
        modifier = modifier.clickable(enabled = isCorrectAnswerSelected == null) {
            onClick()
        },
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        textColor = textColor,
        text = text
    )
}
```

- [ ] **Step 5: Compile-verify**

Run: `./gradlew :evaluation:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If `Color` becomes an unused import after this deletion, remove `import androidx.compose.ui.graphics.Color` too — but check first: the top bar's timer chip still uses `color = Color.White` for its text, so this import is very likely still needed. Only remove it if the compiler actually flags it unused.

- [ ] **Step 6: Run the existing test suite for this module**

Run: `./gradlew :app:testDebugUnitTest --tests "com.gondroid.mtcquiz.presentation.screens.evaluation.*"`
Expected: BUILD SUCCESSFUL (or "no tests found" if this package doesn't exist — check `app/src/test/java/com/gondroid/mtcquiz/presentation/screens/` for the real test package name for Evaluation before concluding there's nothing to run; if there's an existing `EvaluationScreenViewModelTest` under a different package, run that instead). This screen's ViewModel is untouched by this task, so no test behavior should change — a failure here means something in this task broke unrelated code, not that new tests are expected.

- [ ] **Step 7: Commit**

```bash
git add evaluation/presentation/src/main/java/com/gondroid/evaluation/presentation/EvaluationScreen.kt
git commit -m "refactor: migrate EvaluationScreen to QuestionAnswerCard"
```

---

### Task 5: Migrate `QuestionsScreen.kt` (Estudio)

**Files:**
- Modify: `questionreview/presentation/src/main/java/com/gondroid/questionreview/presentation/QuestionsScreen.kt`

**Interfaces:**
- Consumes: `QuestionAnswerCard`, `AnswerOption`, `AnswerOptionState` (from Tasks 2/3).

- [ ] **Step 1: Read the current file first**

Read `questionreview/presentation/src/main/java/com/gondroid/questionreview/presentation/QuestionsScreen.kt` in full before editing, for the same reason as Task 4 Step 1.

- [ ] **Step 2: Update imports**

Replace:
```kotlin
import com.gondroid.core.presentation.designsystem.components.CardAnswer
import com.gondroid.core.presentation.designsystem.components.CardQuestion
import com.gondroid.core.presentation.designsystem.components.LinearProgressComponent
```
with:
```kotlin
import com.gondroid.core.presentation.designsystem.components.AnswerOption
import com.gondroid.core.presentation.designsystem.components.AnswerOptionState
import com.gondroid.core.presentation.designsystem.components.LinearProgressComponent
import com.gondroid.core.presentation.designsystem.components.QuestionAnswerCard
```

- [ ] **Step 3: Replace the per-question rendering block**

Replace:
```kotlin
                    items(
                        items = filteredItems,
                        key = { questions -> questions.id }
                    ) { question ->
                        CardQuestion(
                            modifier = Modifier.fillMaxWidth(),
                            title = "${question.id}.- ${question.title}",
                            questionImages = question.imagens,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        question.options.forEachIndexed { index, option ->
                            ItemAnswerCard(
                                text = option,
                                isCorrectAnswer = question.isCorrectAnswer(index),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
```

with:

```kotlin
                    items(
                        items = filteredItems,
                        key = { questions -> questions.id }
                    ) { question ->
                        QuestionAnswerCard(
                            modifier = Modifier.fillMaxWidth(),
                            title = "${question.id}.- ${question.title}",
                            questionImages = question.imagens,
                            options = question.options.mapIndexed { index, option ->
                                val letter = ('a' + index).uppercaseChar().toString()
                                val optionState = if (question.isCorrectAnswer(index)) {
                                    AnswerOptionState.RevealedCorrect
                                } else {
                                    AnswerOptionState.Unselected
                                }
                                AnswerOption(letter = letter, text = option, state = optionState)
                            },
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
```

- [ ] **Step 4: Delete the now-unused `ItemAnswerCard` function**

Find and delete this entire function:

```kotlin
@Composable
fun ItemAnswerCard(
    text: String,
    isCorrectAnswer: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isCorrectAnswer) Color(0xFFC8E6C9) else Color.White

    val borderColor = if (isCorrectAnswer) Color(0xFF388E3C) else Color.Gray

    CardAnswer(
        modifier = modifier,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        text = text
    )
}
```

- [ ] **Step 5: Compile-verify**

Run: `./gradlew :questionreview:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. `androidx.compose.ui.graphics.Color` is very likely still needed (used by the search `TextField`'s `unfocusedContainerColor = Color.Transparent`) — only remove that import if the compiler flags it unused.

- [ ] **Step 6: Commit**

```bash
git add questionreview/presentation/src/main/java/com/gondroid/questionreview/presentation/QuestionsScreen.kt
git commit -m "refactor: migrate QuestionsScreen (Estudio) to QuestionAnswerCard"
```

---

### Task 6: Delete the old components and run full verification

**Files:**
- Delete: `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardAnswer.kt`
- Delete: `core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardQuestion.kt`

**Interfaces:** None — this is cleanup after both consumers (Tasks 4, 5) have migrated.

- [ ] **Step 1: Confirm nothing else references the old components**

Run: `grep -rn "CardAnswer\|CardQuestion" --include="*.kt" . | grep -v /build/`

Expected: no results (or only results inside the two files being deleted in this task, if the grep matches their own declarations). If anything else shows up, stop and investigate — do not delete a component something else still depends on.

- [ ] **Step 2: Delete both files**

```bash
git rm core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardAnswer.kt
git rm core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardQuestion.kt
```

- [ ] **Step 3: Full build verification**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Full unit test suite**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL, all tests passing.

- [ ] **Step 5: Manual verification (if an emulator/device is available)**

Install: `./gradlew installDebug`. Open a category's Detail screen, tap "Estudiar" (Study) and confirm questions render as one unified card with letter badges and the correct answer highlighted green with a check icon. Go back, tap "Comenzar evaluación" (Start evaluation), select an answer, verify it, and confirm: pre-verify the selected option highlights with the secondary-container tint (no badge color change until verify), post-verify the chosen option turns green (correct) or red (incorrect) with a ✓/✕ icon, and if wrong, the actually-correct option is also highlighted green. Toggle the device's dark theme (Settings → Display) and repeat both checks — confirm no white/black hardcoded cards appear. This step is a nice-to-have if a device is available; do not block completion on it if none is running — say so explicitly in the report instead.

- [ ] **Step 6: Commit**

```bash
git add core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardAnswer.kt core/presentation/designsystem/src/main/java/com/gondroid/core/presentation/designsystem/components/CardQuestion.kt
git commit -m "chore: remove CardAnswer and CardQuestion, superseded by QuestionAnswerCard/AnswerOptionRow"
```

## Self-Review Notes

- **Spec coverage:** all sections of the design spec are covered — `ExtendedColors`/success tokens (Task 1), `AnswerOptionRow` with all 5 states and the color table (Task 2), `QuestionAnswerCard` including the folded-in image placeholder tint/conditional-background fix (Task 3), both screen migrations preserving exact existing interaction semantics (Tasks 4, 5), cleanup (Task 6).
- **Type consistency:** `AnswerOptionState` (Task 2) and `AnswerOption` (Task 3) are used identically by name and field order in Tasks 4 and 5 — `letter`, `text`, `state` in that order everywhere.
- **Behavior preservation check:** Task 4's replacement `when` block was written as a structural mirror of the exact `AnswerCard` function being deleted in the same task (same four branches, same outer gate) specifically so a reviewer can diff the old and new logic side by side rather than trusting a rewritten summary.
