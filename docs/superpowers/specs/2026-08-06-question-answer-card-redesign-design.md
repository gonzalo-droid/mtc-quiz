# Question/Answer Card Redesign — Design

## Goal

Redesign the shared question-display components used by both the Evaluación
(Simulacro) screen and the Estudio (Study) screen: fold the question header
and its answer options into one visually unified card, fix the hardcoded
`Color.White`/`Color.Black`/`Color.Gray`/raw-hex colors that break dark
theme, and add non-color feedback (letter badges, ✓/✕ icons) for
accessibility. Scope is deliberately "correction + polish," not an
interaction redesign — no screen's top bar, timer, progress bar, or search
changes, and no screen's interaction model (Evaluación's verify-then-advance
flow, Estudio's always-revealed browsing) changes.

## Background

`EvaluationScreen.kt` and `QuestionsScreen.kt` (Estudio) both currently
render a question as: `CardQuestion` (a `Card` containing the title and an
optional `LazyRow` of sign images) followed by each answer option as its own
separate `Card` (`CardAnswer`, wrapped per-screen as `AnswerCard` in
Evaluación and `ItemAnswerCard` in Estudio). This reads as loose, floating
pieces rather than one cohesive question block, and `CardAnswer`'s colors
are hardcoded (`Color.White`, `Color.Black`, `Color.Gray`, raw hex greens
and reds) instead of `MaterialTheme.colorScheme` tokens — in dark mode this
renders as a bright white card with black text sitting on a dark screen.
Correct/incorrect feedback is color-only, with no icon, which is an
accessibility gap.

`CardQuestion`'s Coil `AsyncImage` (already patched once this session to add
a placeholder/error state — see `CardQuestion.kt`) also has two small
pre-existing rough edges worth fixing while this file is open again: the
placeholder/error icon isn't tinted (renders as an unstyled black glyph),
and the grey background plate shows behind successfully loaded images too,
not just behind the loading/error state.

## Approach

Two new shared composables in `core:presentation:designsystem/components`
replace the current per-option card pattern with one unified card per
question. `EvaluationScreen` and `QuestionsScreen` both migrate to them,
each supplying its own state mapping — the components themselves carry no
screen-specific logic, only rendering.

### `AnswerOptionState` (new enum, in `AnswerOptionRow.kt`)

```kotlin
enum class AnswerOptionState {
    Unselected,        // neutral, not yet chosen (or, in Estudio, simply not the answer)
    Selected,           // chosen, not yet verified (Evaluación only, pre-verify)
    RevealedCorrect,    // verified/revealed and this option is correct
    RevealedIncorrect,  // verified and this option was chosen and is wrong
    CorrectAnswerHint,  // verified, user chose a different (wrong) option — this is what they should have picked
}
```

### `AnswerOptionRow` (new composable, replaces `CardAnswer`)

```kotlin
@Composable
fun AnswerOptionRow(
    letter: String,          // "A", "B", "C", "D"
    text: String,
    state: AnswerOptionState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,   // null = non-interactive (Estudio, or post-verify in Evaluación)
)
```

A `Row` (not a bordered `Card`) with a circular letter badge, the option
text, and a trailing ✓/✕ icon on `RevealedCorrect`/`RevealedIncorrect`.
Background and content color come from `state`:

| State | Container | Content | Badge |
|---|---|---|---|
| `Unselected` | transparent | `onSurface` | `surfaceContainerHighest` bg, `onSurfaceVariant` text |
| `Selected` | `secondaryContainer` | `onSecondaryContainer` | same tint as container |
| `RevealedCorrect` / `CorrectAnswerHint` | **new** `successContainer` | **new** `onSuccessContainer` | same tint |
| `RevealedIncorrect` | `errorContainer` | `onErrorContainer` | same tint |

`errorContainer`/`onErrorContainer` and `secondaryContainer`/
`onSecondaryContainer` already exist in `Color.kt` for both light and dark
and are reused as-is. `successContainer`/`onSuccessContainer` do not exist
— Material3's `tertiary` role in this theme is amber/gold (see
`tertiaryLight`/`tertiaryDark` in `Color.kt`), not green, so it is not a
substitute. Add four new constants to `Color.kt` (light + dark), sized
tonally consistent with the existing container/on-container pairs (compare
against `errorContainerLight`/`onErrorContainerLight` for tonal weight):

```kotlin
val successContainerLight = Color(0xFFC7F7CB)
val onSuccessContainerLight = Color(0xFF0D3311)
val successContainerDark = Color(0xFF1E4620)
val onSuccessContainerDark = Color(0xFFA6F5AC)
```

`Theme.kt` currently builds a stock Material3 `lightColorScheme()`/
`darkColorScheme()` with no custom fields and no existing extension
mechanism — `ColorScheme` itself has no `successContainer` slot to fill.
Add one: a small `ExtendedColors` data class plus a `CompositionLocal`,
provided by `MTCQuizTheme` alongside `MaterialTheme` and read through a
`MaterialTheme.extendedColors`-style accessor (the standard pattern for
extra semantic colors on top of Material3):

```kotlin
// ExtendedColors.kt (new file, core:presentation:designsystem)
data class ExtendedColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(successContainerLight, onSuccessContainerLight)
}

val MaterialTheme.extendedColors: ExtendedColors
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
```

`MTCQuizTheme` wraps its existing `MaterialTheme { content() }` call in
`CompositionLocalProvider(LocalExtendedColors provides if (darkTheme)
ExtendedColors(successContainerDark, onSuccessContainerDark) else
ExtendedColors(successContainerLight, onSuccessContainerLight)) { ... }`.
`AnswerOptionRow` then reads `MaterialTheme.extendedColors.successContainer`
the same way it reads `MaterialTheme.colorScheme.errorContainer`.

### `QuestionAnswerCard` (new composable, replaces `CardQuestion`)

```kotlin
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
)
```

One `Card` (`containerColor = MaterialTheme.colorScheme.surfaceContainerLow`,
`shape = RoundedCornerShape(16.dp)` — matching the house style already
established in `DetailScreen.kt` and `CustomizeScreen.kt`) containing, top
to bottom: title text, the existing image `LazyRow` (unchanged, still one
`AsyncImage` per image in `questionImages`, still `220.dp` each) if
`questionImages` is non-empty, a `HorizontalDivider` (`onSurface` at low
alpha, matching the divider style already used in `unified-card` mockups),
then one `AnswerOptionRow` per entry in `options`, each calling
`onOptionClick(index)` on click if provided.

**Image placeholder fix (folded into this same file since it's being
touched):** tint the placeholder/error `rememberVectorPainter` with
`ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)`, and scope
the `surfaceVariant` background plate to only the placeholder/error visual
state, not the loaded-image state — e.g. via Coil's `SubcomposeAsyncImage`
switching background only in the `loading`/`error` branches, rather than an
unconditional `Modifier.background(...)` on every state.

### Migration in `EvaluationScreen.kt`

Replace the `CardQuestion` + `itemsIndexed(state.question.options) { AnswerCard(...) }`
pair with one `QuestionAnswerCard` call. State mapping per option index,
using the screen's existing `selectedOption`, `state.answerWasSelected`,
`isCorrectAnswerSelected` locals (unchanged):

```kotlin
val optionState = when {
    !state.answerWasSelected -> if (option == selectedOption) Selected else Unselected
    option == selectedOption && isCorrectAnswerSelected -> RevealedCorrect
    option == selectedOption && !isCorrectAnswerSelected -> RevealedIncorrect
    isCorrectAnswerSelected == false && state.question.isCorrectAnswer(index) -> CorrectAnswerHint
    else -> Unselected
}
```

`onOptionClick` is passed only pre-verify (`isCorrectAnswerSelected == null`
equivalent to `!state.answerWasSelected`); once verified, pass `null` so the
row becomes non-interactive, matching current behavior where `AnswerCard`'s
`clickable(enabled = isCorrectAnswerSelected == null)` already gates this.
`AnswerCard` (the screen-local wrapper) is deleted — its logic moves into
this mapping plus the shared `AnswerOptionRow`.

### Migration in `QuestionsScreen.kt` (Estudio)

Every option is always revealed, there is no selection: `state = if
(question.isCorrectAnswer(index)) RevealedCorrect else Unselected`,
`onOptionClick = null`. `ItemAnswerCard` is deleted.

### Cleanup

`CardAnswer.kt` and `CardQuestion.kt` are removed once both call sites
migrate (their previews move to the two new files). No other file in the
codebase references `CardAnswer`/`AnswerCard`/`ItemAnswerCard`/`CardQuestion`
outside these two screens and their own preview functions (confirmed via
repo-wide grep) — a safe, fully-contained rename/replace with no other
call-sites to update.

## What does not change

- `EvaluationScreen`'s `TopAppBar`, timer chip, `LinearProgressComponent`
  usage, `ButtonsAction`, and both dialogs (`FinishedTimeDialog`,
  `CancelEvaluation`) — untouched.
- `QuestionsScreen`'s search bar, `LinearProgressComponent` usage, and
  scroll-based progress calculation — untouched.
- Both screens' ViewModels, state classes, and navigation — untouched. This
  is a presentation-layer-only change confined to
  `core:presentation:designsystem:components` plus the two screens'
  question-rendering call sites.
- No DB, network, or DataStore schema involved.

## Testing

No existing automated test asserts on `CardAnswer`/`AnswerCard` internals or
colors (confirmed via grep — `EvaluationScreenRootTest.kt`'s one relevant
assertion checks question title text via `onNodeWithText`, which
`QuestionAnswerCard` still renders identically). Add Compose previews for
`AnswerOptionRow` (all five states, light + dark) and `QuestionAnswerCard`
(with and without images) in the new files, following the existing
`@Preview(showBackground = true)` convention used throughout this design
system module.

