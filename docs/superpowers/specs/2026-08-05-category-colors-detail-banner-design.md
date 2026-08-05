# Category colors, Detail banner ad, small-screen scroll fixes

## Context

Three related UI/UX asks on the Home and Detail screens:

1. Each Home category card should get a distinct color, grouped visually by
   license class, so users can identify flows by color.
2. The Detail screen (reached by tapping a Home card) should also show the
   AdMob banner ad, matching Home's existing pattern.
3. Home and Detail should not overlap/clip content on small screens —
   components should scroll instead.

While investigating the category card data, a pre-existing, unrelated data
bug was found and is included in this scope per the user's explicit request
(see "Category data bug fix" below).

## A. Category color palette

Grouped by license class: a blue family for Class A (darkest for A-I,
lightening through the sub-categories), a warm family for Class B. Approved
via the visual companion (option B).

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

New file `home/presentation/src/main/java/com/gondroid/home/presentation/CategoryColors.kt`:

```kotlin
data class CategoryColorScheme(val container: Color, val content: Color)

fun categoryColors(category: String): CategoryColorScheme
```

Unknown category codes fall back to
`CategoryColorScheme(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)`
— the caller reads `MaterialTheme` at the call site and passes it in, since
this is a plain (non-`@Composable`) function.

`CardCategoryItem` (`home/presentation/.../HomeScreen.kt:282-356`) uses
`categoryColors(item.category)` for its `CardDefaults.cardColors`
`containerColor`/`contentColor` instead of the current fixed
`MaterialTheme.colorScheme.primary`/`onPrimary`.

Scope: Home only, per the approved design — Detail screen keeps its current
styling, unaffected by category color.

## B. Category data bug fix

`core/data/src/main/java/com/gondroid/core/data/local/CategoryLocalDataSource.kt`
currently has 10 entries; 3 have the wrong `pathJson` and 1 has no real
balotario behind it:

- `id="8"` (`B-IIa`): `pathJson` changes from `"a1_questions.json"` to
  `"b2a_questions.json"`.
- `id="9"` (`B-IIb`): `pathJson` changes from `"a1_questions.json"` to
  `"b2b_questions.json"`.
- `id="10"` (`B-IIc`): `pathJson` changes from `"a1_questions.json"` to
  `"b2c_questions.json"`.
- `id="7"` (`B-I`, triciclos): removed from the list entirely — no real
  balotario PDF/JSON exists for it. A short comment explains why Class B
  has only 3 sub-categories instead of 4, so a future reader isn't
  confused by the gap.

Ids are not renumbered — `id="8"`/`"9"`/`"10"` stay as-is, since
`Evaluation.categoryId` values already persisted in Room could reference
them.

`QuizRepositoryImpl` reads this list directly (`categoriesLocalDataSource`,
no Firebase involved for categories), so this fix takes effect immediately
for the Home category list and the questions loaded per category.

## C. Detail screen banner ad

The AdView creation/load/pause/resume/destroy logic (`HomeScreen.kt:83-117`,
`145-148`, plus the `BannerAd` composable in `home/presentation/.../BannerAd.kt`)
today lives only in `home/presentation`. Extracting it once, now that a
second screen needs it, avoids duplicating ~35 lines of lifecycle-sensitive
code.

**New shared composable** in `core/presentation/ui` (a module both
`home/presentation` and `detail/presentation` already depend on):

```kotlin
// core/presentation/ui/.../BannerAdSlot.kt
@Composable
fun BannerAdSlot(bannerAdId: String, isPremium: Boolean, modifier: Modifier = Modifier)
```

Encapsulates: AdView creation + `AdListener` logging + `loadAd` +
`LifecycleResumeEffect` pause/resume + `DisposableEffect` destroy + the
`if (!isPremium) { ... }` gate + the centered `Box` wrapper — everything
`HomeScreenRoot` currently does inline, minus the `HomeScreen`-specific
`content: @Composable () -> Unit` plumbing.

`core/presentation/ui/build.gradle.kts` gains `implementation(libs.play.services.admob)`.

**`HomeScreenRoot`** (`HomeScreen.kt`): the inline AdView block and the
`content = { ... }` slot passed into `HomeScreen` are replaced by a call to
`BannerAdSlot(bannerAdId = viewModel.bannerAdId, isPremium = state.isPremium)`
placed the same way (bottom of the screen content, inside the scrollable
Column per section D).

**`DetailScreenViewModel`**: gains `private val billingManager: BillingManager`
(exposes `isPremium` into `DetailState`, mirroring
`HomeScreenViewModel.kt:48-52`) and `@Named("admobBannerId") val bannerAdId: String`.

**`DetailState`**: gains `val isPremium: Boolean = false`.

**`DetailScreen`** (`DetailScreen.kt`): `BannerAdSlot(...)` added as the
last item in the scrollable content Column, after `ButtonsAction` — visible
only to non-premium users, matching Home.

## D. Small-screen scroll safety

Both `HomeScreen`'s content `Column` (`HomeScreen.kt:205-278`) and
`DetailScreen`'s content `Column` (`DetailScreen.kt:172-226`) are currently
fixed, non-scrolling `Column`s relying on a weighted `Spacer` to push
content to the bottom — which clips/overlaps on short screens instead of
reflowing.

Both wrap their content in `Modifier.verticalScroll(rememberScrollState())`.
The weighted `Spacer(Modifier.weight(1f))` pattern is incompatible with
`verticalScroll` (a scrollable column has unbounded height, so `weight`
has nothing to fill against) and is removed from both screens; the banner
ad (Detail) and existing bottom content (Home) become the last items in
the scrollable column instead of being pinned via spacer-push. On tall
screens this looks the same as today (content still ends up near the
bottom because there's little to scroll); on short screens everything
becomes reachable by scrolling instead of being cut off.

## Testing

- `CategoryColors.kt`: a small JVM/unit test asserting each of the 9 known
  category codes maps to its documented color pair, and an unknown code
  falls back to the provided default.
- `CategoryLocalDataSource.kt`: existing tests (if any reference this list)
  re-verified; a new assertion that the list has 9 entries (not 10) and
  that `id="8"/"9"/"10"` have the corrected `pathJson` values.
- Manual verification on the emulator (small and normal screen sizes) for
  the color palette, the Detail banner, and scroll behavior — screenshots
  compared against the approved mockup direction.
