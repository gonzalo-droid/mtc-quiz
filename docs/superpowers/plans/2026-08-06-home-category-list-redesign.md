# Home Category List Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Home's `HorizontalPager` category carousel with a vertically stacked list of full-width category cards, all visible via scroll, each keeping the existing type/code/description/image content.

**Architecture:** Single-file change confined to `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt` — `CardCategoryItem` drops its `PagerState`/`index` params and pager-transition animation; `HomeScreen`'s content `Column` (already `verticalScroll`) iterates `state.categories` directly instead of hosting a `HorizontalPager`.

**Tech Stack:** Jetpack Compose, Material3.

## Global Constraints

- Presentation-layer only — no change to `HomeState`, `HomeScreenViewModel`, `CategoryColors.kt`, the `Category` domain model, or navigation.
- No change to the top bar, streak row, banner ad slot, or greeting/subtitle text.
- Design spec (full rationale, not required reading for implementation — this plan carries every value needed): `docs/superpowers/specs/2026-08-06-home-category-list-redesign-design.md`.

---

### Task 1: Replace the category pager with a stacked list

**Files:**
- Modify: `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt`

**Interfaces:** None — this is the only file in the change; no other file consumes anything from it beyond the existing `HomeScreenRoot`/`HomeScreen` public composables, which keep the same signatures.

- [ ] **Step 1: Read the current file first**

Read `home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt` in full before editing — this plan was written against the version at commit `f14dfed` on `master`. If it has diverged, adapt the edits below to the real current content rather than blindly applying a patch that no longer matches.

- [ ] **Step 2: Update imports**

Remove these now-unused imports:
```kotlin
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
```

(`PaddingValues` is only used today by the pager's `contentPadding = PaddingValues(horizontal = 40.dp)` — confirm nothing else in the file references `PaddingValues` before removing; if something else does, keep the import.)

- [ ] **Step 3: Replace the pager block in `HomeScreen`**

Replace:
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

with:
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

- [ ] **Step 4: Simplify `CardCategoryItem`**

Replace the whole `CardCategoryItem` function:
```kotlin
@Composable
fun CardCategoryItem(
    pagerState: PagerState,
    index: Int,
    item: Category,
    onItemSelected: () -> Unit = {}
) {

    val pageOffSet = (pagerState.currentPage - index) + pagerState.currentPageOffsetFraction

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
            .graphicsLayer {
                lerp(
                    start = 0.85f,
                    stop = 1f,
                    fraction = 1f - pageOffSet.absoluteValue.coerceIn(0f, 1f)
                ).also { scale ->
                    scaleX = scale
                    scaleY = scale
                }
                alpha = lerp(
                    start = 0.5f,
                    stop = 1f,
                    fraction = 1f - pageOffSet.absoluteValue.coerceIn(0f, 1f)
                )
            },
        onClick = onItemSelected
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            Column(
                modifier = Modifier
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = item.classType,
                    fontSize = 15.sp,
                    color = colors.content,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.category,
                    color = colors.content,
                    modifier = Modifier
                        .padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    maxLines = 4,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.content,
                    overflow = TextOverflow.Ellipsis
                )
            }

            AsyncImage(
                model = "file:///android_asset/anim/${item.examId}_card.png",
                contentDescription = "image_category",
                modifier = Modifier.align(Alignment.BottomEnd),
                contentScale = ContentScale.Fit
            )
        }
    }

}
```

with:
```kotlin
@Composable
fun CardCategoryItem(
    item: Category,
    modifier: Modifier = Modifier,
    onItemSelected: () -> Unit = {}
) {

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
        modifier = modifier.height(160.dp),
        onClick = onItemSelected
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = item.classType,
                    color = colors.content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = item.category,
                    color = colors.content,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.description,
                    maxLines = 3,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.content,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.68f),
                )
            }

            AsyncImage(
                model = "file:///android_asset/anim/${item.examId}_card.png",
                contentDescription = "image_category",
                modifier = Modifier.align(Alignment.BottomEnd),
                contentScale = ContentScale.Fit
            )
        }
    }

}
```

Notes on this rewrite: the inner `Column`'s duplicate `.padding(horizontal = 16.dp)` on every `Text` (redundant with the outer `Column`'s own `start = 16.dp, end = 16.dp` padding in the original) is dropped — the outer padding alone is sufficient, this is a straightforward simplification, not a functional change. `Modifier.fillMaxWidth(0.68f)` on the description keeps text from running underneath the character image on the right, mirroring what the original's implicit `max 75%`-ish width (via the `Column`'s own bounds relative to the image) achieved differently since the layout changed from a `contentAlignment = Alignment.Center` `Box` to a plain `Box`; if this specific fraction doesn't look right against the real character images at 160dp height, adjust it — it's a visual tuning value, not a hard requirement.

`fillMaxWidth(0.68f)` requires `androidx.compose.foundation.layout.fillMaxWidth` — already imported in this file (used elsewhere), no new import needed. `Box(modifier = Modifier.fillMaxSize())` no longer needs `contentAlignment = Alignment.Center` since children are explicitly aligned (`TopStart`, `BottomEnd`) — dropping it is intentional, not an oversight.

- [ ] **Step 5: Compile-verify**

Run: `./gradlew :home:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL. If `fontSize`/`sp` import (`androidx.compose.ui.unit.sp`) becomes unused after this change (the original `CardCategoryItem` used `fontSize = 15.sp` directly, which this rewrite removes in favor of `MaterialTheme.typography.titleMedium`'s own size), the compiler will flag it — remove the `import androidx.compose.ui.unit.sp` line only if actually unused (check the rest of the file first; it may still be used elsewhere, e.g. the streak text/dp values don't use `sp` but double check before removing).

- [ ] **Step 6: Run the existing test suite for this module**

Run: `./gradlew :app:test --tests "com.gondroid.mtcquiz.presentation.screens.home.HomeScreenViewModelTest"`
Expected: BUILD SUCCESSFUL, all tests passing. This ViewModel test doesn't exercise Composable rendering, so it should be unaffected — a failure here would mean something in this change broke unrelated code, not that new tests are expected for this UI-only change.

- [ ] **Step 7: Manual verification (if an emulator/device is available)**

Install: `./gradlew installDebug`. Open Home and confirm: all 9 categories are visible via vertical scroll (no swipe gesture needed), each card shows type/code/description/character image without text overlapping the image, tapping any card navigates to its Detail screen (same as before). This step is a nice-to-have if a device is available; do not block completion on it if none is running — say so explicitly in the report instead.

- [ ] **Step 8: Commit**

```bash
git add home/presentation/src/main/java/com/gondroid/home/presentation/HomeScreen.kt
git commit -m "refactor: replace Home category pager with a scrollable stacked list"
```

## Self-Review Notes

- **Spec coverage:** the spec's single requirement (pager → stacked list, `CardCategoryItem` simplified, pager-transition animation removed, fixed 160dp height, 3-line description, flat category colors preserved, everything else untouched) is fully covered by this one task.
- **Placeholder scan:** none — every step has real, complete code.
- **Type consistency:** `CardCategoryItem(item: Category, modifier: Modifier = Modifier, onItemSelected: () -> Unit = {})` is the only call site's signature and is used consistently in Step 3's `HomeScreen` edit and Step 4's `CardCategoryItem` definition.
