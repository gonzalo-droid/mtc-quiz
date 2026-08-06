# Home Category List Redesign — Design

## Goal

Replace the Home screen's `HorizontalPager` category carousel (one category card visible at a time, swipe to see the rest) with a vertically stacked list of full-width cards showing all 9 categories at once, each with type, code, description, and character image — the same content the current carousel card shows, just laid out as a scrollable list instead of a pager.

## Background

`HomeScreen.kt`'s `CardCategoryItem` currently renders inside a `HorizontalPager` (`rememberPagerState`, `contentPadding = PaddingValues(horizontal = 40.dp)`), with a `graphicsLayer` scale/alpha transition (`lerp` between 0.85f/0.5f and 1f based on `pagerState.currentPageOffsetFraction`) so neighboring cards peek in at reduced size/opacity. Each card is `400.dp` tall with `classType`, `category`, a 4-line `description`, and a bottom-end-aligned character image (`file:///android_asset/anim/${item.examId}_card.png`), colored via `categoryColors(item.category, fallback)` (`CategoryColors.kt`, already defined per-category, e.g. A-I is `#274C93`/white).

To see all 9 categories today, a user must swipe through them one at a time. This redesign trades the pager's per-card "hero" treatment for a scrollable list that surfaces everything at once — explicitly chosen by the user over an initially-proposed 2-column grid, in favor of keeping the description visible per card (a grid without descriptions was rejected).

## Approach

`HomeScreen`'s content `Column` (already `verticalScroll`) keeps the same outer scrolling — no `LazyColumn` needed, there are only 9 categories (confirmed via `CategoryLocalDataSource`), so a plain `Column` iterating `state.categories` is simplest and matches the file's existing pattern of a scrollable `Column` for everything above the pager.

### `CardCategoryItem` (simplified)

Drop the `PagerState`/`index` parameters entirely — the composable only needs the `Category` item and a click handler:

```kotlin
@Composable
fun CardCategoryItem(
    item: Category,
    onItemSelected: () -> Unit = {},
)
```

Card body keeps the same content as today (title/code text block top-start, description below it, character image bottom-end, `categoryColors` background) but:
- Fixed height reduced from `400.dp` to `160.dp` — full-viewport-height cards only made sense as a one-at-a-time pager hero; in a stacked list, 9× 400dp cards would be an excessive scroll. A fixed (not wrap-content) height keeps all 9 cards visually consistent despite descriptions varying in length.
- `description` truncation drops from `maxLines = 4` to `maxLines = 3` to fit the shorter card, still with `TextOverflow.Ellipsis` (unchanged behavior, just a lower cap).
- Text sizing (`titleLarge` for classType, `displayLarge` for category code) shrinks to fit the shorter card — use `titleMedium`/`headlineMedium` respectively as reasonable steps down; exact values are a plan-time judgment call against the real card, not a hard requirement.
- The `graphicsLayer`/`lerp` scale+alpha block is deleted entirely — it only existed to animate the pager's peeking neighbor cards, which no longer exist.
- Background stays the existing flat `categoryColors(item.category, fallback).container`/`.content` pair — no gradient (any gradient shown during brainstorming mockups was mockup-only visual flourish, not a requirement).

### `HomeScreen` (pager removed)

Replace the `HorizontalPager` block:

```kotlin
val pagerState = rememberPagerState(pageCount = { state.categories.size })
HorizontalPager(
    state = pagerState,
    contentPadding = PaddingValues(horizontal = 40.dp)
) { index ->
    CardCategoryItem(
        pagerState = pagerState,
        index = index,
        item = state.categories[index],
        onItemSelected = {
            onAction(HomeAction.OnClickCategory(state.categories[index].id))
        })
}
```

with a plain loop over `state.categories`, each wrapped in `Modifier.fillMaxWidth().padding(horizontal = 16.dp)` (matching the horizontal padding already used by the title/subtitle/streak `Text`s above it in this same `Column`) and spaced with a `Spacer`:

```kotlin
state.categories.forEach { category ->
    CardCategoryItem(
        item = category,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onItemSelected = {
            onAction(HomeAction.OnClickCategory(category.id))
        },
    )
    Spacer(modifier = Modifier.height(12.dp))
}
```

(`CardCategoryItem` gains a `modifier: Modifier = Modifier` parameter alongside `item`/`onItemSelected`, applied to its root `Card`, following the same pattern every other composable in this codebase uses.)

Imports to remove from `HomeScreen.kt`: `androidx.compose.foundation.pager.HorizontalPager`, `androidx.compose.foundation.pager.PagerState`, `androidx.compose.foundation.pager.rememberPagerState`, `androidx.compose.ui.graphics.graphicsLayer`, `androidx.compose.ui.util.lerp`, `kotlin.math.absoluteValue`, `androidx.compose.foundation.layout.PaddingValues` (only used by the removed `contentPadding` arg — confirm nothing else in the file needs it before removing).

## What does not change

- Top bar (title, premium icon, menu icon), streak row, banner ad slot, greeting/subtitle text — all untouched, exactly as confirmed during brainstorming (personalized greeting using `HomeState.userName` was considered and explicitly declined; page-indicator dots were considered and superseded by this list approach, which has no paging to indicate).
- `HomeState`, `HomeScreenViewModel`, navigation (`onAction(HomeAction.OnClickCategory(...))` still navigates to Detail) — untouched. Presentation-layer-only change, confined to `HomeScreen.kt`.
- `CategoryColors.kt`, `Category` domain model — untouched.

## Testing

No existing automated test asserts on `CardCategoryItem`'s pager-specific behavior or exact card height (confirmed: `HomeScreenViewModelTest` only asserts on `HomeState` fields like `categories`/`userName`/`isPremium`, not on rendered Compose output). Update the file's `@Preview` (`PreviewHomeScreenRoot`) if its two-category fixture no longer exercises anything pager-specific — it should keep working unchanged since it just renders `HomeScreen(state = ...)`.
