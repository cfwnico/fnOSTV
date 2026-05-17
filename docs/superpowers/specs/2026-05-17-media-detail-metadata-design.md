# Media Detail Metadata Design

## Context

The current native media detail page is intentionally small. It shows the selected entry name, format, file size, path, favorite state, and playback source status. This is enough to confirm playback, but it does not yet feel like a complete film or TV detail page. The interface notes also call out that `/item/{guid}` and `/video/{guid}` detail, season, episode, and version APIs still need more capture.

## Goals

- Enrich the native detail page with metadata when it is available.
- Keep playback usable even when metadata APIs fail or return partial data.
- Add a pure Java metadata model and parser so the risky part can be tested without Android runtime dependencies.
- Prepare the detail state for child entries such as episodes, seasons, versions, or alternate media records.
- Keep the first implementation bounded and Android 4 friendly.

## Non-Goals

- No full server API reverse engineering in this slice.
- No complete episode playback grid yet.
- No new playback engine behavior.
- No global navigation redesign.
- No dependency on a single exact fnOS detail response shape.

## Recommended Approach

Use a defensive metadata layer.

1. Add `MediaDetailInfo` as a small immutable detail snapshot.
2. Add `MediaDetailInfoParser` to parse several likely REST response shapes:
   - `{data:{...}}`
   - `{data:{item:{...}}}`
   - `{data:{detail:{...}}}`
   - `{data:{list:[...]}}` for child rows
3. Extend `MediaDetailState` with:
   - `detailInfo`
   - `loadingDetail`
   - `detailError`
   - `children`
4. Update `NativeMediaDetailView` to show metadata if present:
   - title from detail info when available
   - year, rating, category, duration, source type line
   - overview/description text
   - child count line such as `剧集 12 项`
5. Add a best-effort REST fetch path in `MainActivity.openMediaDetail(...)`. If metadata fetch fails, keep the existing source resolution and playback controls working.

## Data Model

`MediaDetailInfo` should contain:

- `title`
- `subtitle`
- `overview`
- `year`
- `rating`
- `category`
- `durationLabel`
- `sourceLabel`
- `children`

`children` uses existing `FnosFileEntry` so episodes or versions can be opened through the same detail/playback path later.

## Parsing Rules

`MediaDetailInfoParser` should be permissive:

- Title: first non-empty of `title`, `name`, `tv_title`, `file_name`.
- Subtitle: first non-empty of `original_title`, `subtitle`, `season_title`, `album_title`.
- Overview: first non-empty of `overview`, `description`, `plot`, `summary`, `intro`.
- Year: `year`, `release_year`, or first four digits of `release_date`.
- Rating: `rating`, `score`, `douban_score`, `tmdb_score`, formatted without noisy trailing `.0`.
- Category: first non-empty of `category`, `type`, `media_type`, `tags.type`.
- Duration: format seconds or milliseconds into `H:mm:ss` or `mm:ss`; preserve strings when already present.
- Children: parse arrays named `children`, `episodes`, `seasons`, `versions`, `sources`, or `list`.

## MainActivity Flow

When opening detail:

1. Build `MediaDetailState` as today.
2. Show the page immediately.
3. Start playback source resolution as today.
4. Start metadata fetch in parallel only when the entry path looks like a media guid or REST item key.
5. On success, update `MediaDetailState` and re-render.
6. On failure, store a concise detail error but do not block playback.

## Error Handling

- Missing metadata fields render as empty, not as `"null"`.
- Metadata fetch failure does not overwrite playback-source errors.
- Children with no path/guid are ignored.
- If no detail API is available, the UI remains exactly as useful as the current detail page.

## Testing

Unit tests should cover:

- Parsing title, overview, year, rating, and category from a `data.item` response.
- Parsing child rows from `episodes` or `list`.
- Formatting duration from seconds.
- `MediaDetailState` stores metadata and loading/error state independently from playback sources.

## Success Criteria

- JVM tests cover metadata parsing and state transitions.
- Detail page can display metadata without breaking existing playback/source/favorite controls.
- Existing playback-source behavior still passes tests.
- Debug build succeeds.
