# Home Poster Sections Design

## Context

The current native home screen has three poster-aware cards: media library, recent playback, and favorites. `MainActivity.updateHomeCounts()` gathers local and REST data, then calls `HomePosterSlots.from(...)` to choose one representative entry per area. This keeps the home screen light, but it makes the poster wall feel sparse and does not give old devices a clear paging boundary when more media becomes available.

## Goals

- Expand the home screen from fixed single-slot cards to multiple poster sections.
- Keep the first implementation friendly to Android 4 devices by limiting section size and total rendered cards.
- Reuse existing data sources: REST media entries, local media index, recent playback, and favorites.
- Keep the section-building logic pure Java and unit-tested before touching Android views.
- Preserve the current navigation actions for `影视大全`, `最近播放`, `收藏`, and category entry points.

## Non-Goals

- No RecyclerView migration in this slice.
- No infinite scroll or background paging API in this slice.
- No new mediaCenter server contract.
- No redesign of the whole home page shell, sidebar, or top action bar.
- No image cache rewrite beyond the existing `PosterLoader` behavior.

## Recommended Approach

Use a staged "bounded sections" approach.

1. Add a pure data model:
   - `HomePosterSection`: title, action, entries, maximum visible card count.
   - `HomePosterSections`: factory methods that build sections from media, recent, and favorite lists.
2. Keep current hero and sidebar intact.
3. Replace the two-card row under the hero with a vertical set of sections:
   - `继续观看`: recent playback, max 6 cards.
   - `我的收藏`: favorites, max 6 cards.
   - `影视大全`: mixed known media, max 8 cards.
   - Optional category sections when data exists: `电影`, `电视节目`, `其他`, max 6 cards each.
4. Render only bounded entries per section. If a section has more items than the limit, show a compact "更多" card that triggers the section action.

This gives the homepage a richer poster wall while still keeping view creation bounded on old TV boxes.

## Components

### HomePosterSection

A small immutable model under `com.fnostv.android4.ui`.

Fields:

- `String title`
- `String action`
- `List<FnosFileEntry> entries`
- `int maxVisible`
- `boolean hasMore`

Behavior:

- Removes null entries.
- Keeps only video entries or directory entries with poster art.
- De-duplicates by path.
- Exposes `visibleEntries()` limited to `maxVisible`.

### HomePosterSections

A pure builder that turns known media, recent, and favorites into ordered sections.

Rules:

- Recent section appears first when recent has entries.
- Favorites section appears second when favorites has entries.
- Media section appears even when empty, so the home screen still has a stable entry into `影视大全`.
- Category sections appear only when matching entries exist.
- Duplicates are removed inside each section, not globally, so a favorite item can still appear in "我的收藏" and "影视大全".

### NativeHomeView

`NativeHomeView` will accept a `List<HomePosterSection>` and render it into the existing content column. The first pass should use normal `LinearLayout` rows with bounded cards, because Android 4 support is more important than fancy scrolling mechanics.

Expected UI behavior:

- Focus order follows section order from left to right, top to bottom.
- Poster cards still use `PosterLoader`.
- Empty media state shows a single action card that opens `影视大全`.
- "更多" card opens the section action rather than rendering all hidden items.

### MainActivity

`updateHomeCounts()` will continue to calculate counts, then call:

- local fallback: `HomePosterSections.from(known, recent, favorites)`
- REST refresh: `HomePosterSections.from(mediaEntries, recentList.entries, favoriteList.entries)`

The current `HomePosterSlots` can remain during migration or be replaced once the new section renderer is stable.

## Error Handling

- If REST home data fails, keep rendering local sections from local media index, recent playback, and favorites.
- If poster loading fails, keep the existing fallback text inside each card.
- If a section has no entries, omit it except for the media library section.
- If all lists are empty, show only the stable media library entry and sidebar counts.

## Testing

Unit tests should cover:

- Section ordering.
- Per-section entry limits.
- Duplicate removal by path.
- Recent/favorite sections omitted when empty.
- Media section remains available when all lists are empty.
- Category sections split movie, TV, and other entries using the same naming heuristics as current home counts.

## Success Criteria

- Home section-building logic is unit-tested without Android runtime dependencies.
- Home screen can render multiple sections with bounded card counts.
- Existing counts, sidebar navigation, and poster loading still work.
- Debug build succeeds on the current Android toolchain.
