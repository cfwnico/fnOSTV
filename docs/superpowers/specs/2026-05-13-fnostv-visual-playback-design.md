# fnOSTV Visual Replica And Playback Stabilization Design

Date: 2026-05-13

## Goal

Build fnOSTV as a native Android 4-compatible client that first visually matches the Feiniu Video web app, then stabilizes playback performance and end-to-end TV remote workflows.

The work order is:

1. Phase B: one-to-one visual replica of the core Feiniu Video experience.
2. Phase C: playback performance, fallback behavior, and end-to-end validation.

## Current Context

The repository is already a Java Android application targeting legacy TV boxes and TV devices. It avoids Kotlin, Compose, AndroidX, and modern WebView assumptions. The current app includes native login, native home/list/player screens, RPC login and file APIs, a REST client for Feiniu Video media APIs, VLC/IJK playback engines, local favorites, recent playback, and media library scanning.

The Feiniu Video web app uses modern module-based frontend assets. Android 4 WebView cannot reliably render it, so the visual replica must be native Android UI, not a WebView wrapper.

The available 2026-05-13 captures cover login, home, favorites, media library, all, movie, TV, and other pages. The current live server at `192.168.0.198:5666` is reachable on TCP port `5666`, but the in-app browser automation timed out while loading it. Implementation should use the saved captures as the visual baseline and use direct network/API scripts or APK screenshots for verification.

## Phase B Scope: Visual Replica

Phase B focuses on matching the web app's primary TV browsing surface while staying native and responsive on Android 4 devices.

Included screens:

- Home: left sidebar, favorite/media/category counts, top action icons, media library hero card, recent/favorite entry cards.
- Media library list: title, filter/sort/layout affordances, item count, poster grid, empty/loading/error states.
- Category lists: all, movie, TV, and other, using the same list shell and card grid rules.
- Favorites: list and empty states.
- Search entry: native search dialog that opens from the top search icon and returns results in the same list shell.
- Settings entry: preserve existing settings access and keep its visual language aligned with the new shell.

Not included in the first visual pass:

- Pixel-perfect reproduction of every web animation.
- Complete web filter drawer parity.
- Full media detail page parity unless required to support the list-to-play path.
- Server-side media library management parity. Local media library management remains the stable Android fallback until those APIs are fully confirmed.

## Phase B Design

### Layout

Use one native shell for home and list-like pages:

- Fixed left sidebar suitable for 720p/1080p TV screens.
- Main content pane with compact top bar.
- Poster/card grid sized by dp constants rather than viewport-derived font scaling.
- Stable card dimensions so focus rings, labels, counts, and loading states do not shift layout.
- No nested cards. Page surfaces remain flat; only individual repeated media items are card-like.

### Visual Style

Match the Feiniu Video dark TV UI:

- Deep neutral background.
- Slightly lighter sidebar and content cards.
- Muted secondary text.
- Strong focus outline/background for remote navigation.
- Icon-first top actions and sidebar icons.
- Poster art where available, text fallback where unavailable.

The current app already has `FnosTheme`, `FnosSidebarIconView`, `FnosActionIconButton`, `NativeHomeView`, and `NativeFileBrowserView`. Phase B should extend these existing native patterns rather than introduce a new UI framework.

### Data Flow

The visual replica should consume the current REST/RPC abstraction:

- `FnosRestClient.mediaCounts()` for sidebar and home counts.
- `FnosRestClient.mediaLibraries()` for media library entry cards.
- `FnosRestClient.mediaItems(...)` for all/library/category/search lists.
- `FnosRestClient.favoriteItems()` for favorites.
- `FnosRestClient.recentItems()` plus `RecentPlaybackStore` for recent playback fallback.
- RPC file browsing and download APIs as fallback when REST media APIs are unavailable.

The UI should display server REST data first when available, then local media index or RPC file data as fallback. Fallbacks must be visible in logs and non-disruptive to the user.

### Poster Images

Add an Android 4-safe poster loader:

- Convert server poster paths such as `/ad/18/...webp` into `/v/api/v1/sys/img/...`.
- Include the REST authorization token where required.
- Cache decoded bitmaps in memory with a small LRU budget.
- Downsample images to card size before rendering.
- Fall back to text poster cards when the image is missing, unsupported, or too large.

WebP support varies on old Android versions. If a decoded WebP fails, show the text fallback rather than blocking list rendering.

### Remote Navigation

Every visible command and card must be reachable by D-pad:

- Sidebar items.
- Top search/user/settings buttons.
- Home cards.
- Grid/list items.
- Dialog buttons and search input.

Focus must be visually obvious on both dark and poster backgrounds. Back behavior should return from player to list, list to home, and dialogs to their parent screen.

## Phase C Scope: Playback Performance And E2E

Phase C starts after the visual shell is stable.

Included:

- Keep the current playback engine order: VLC first, IJK fallback, software decode retry, external player fallback.
- Tune playback options by source type and device capability.
- Preserve quick start for direct file URLs and support multi-source quality switching.
- Verify TV remote controls: play/pause, back, left/right seek, up fill mode, down quality, menu speed.
- Record diagnostic logs for engine choice, source quality, cache profile, first frame, buffering, fallback, seek, and error reasons.

Not included:

- Replacing VLC/IJK with a modern AndroidX media stack.
- Raising min SDK.
- Depending on codecs unavailable on Android 4 devices.

## Phase C Design

### Playback Strategy

`PlaybackStrategy` remains the central decision point. It should classify playback into:

- Remote HTTP stream: larger network cache, conservative decode defaults.
- Local/direct file stream: lower cache and faster start.
- Unsupported or risky format: try internal only when `FnosFileEntry.canTryNativePlayback()` allows it, otherwise open external player.
- Failed hardware decode: retry software decode once before external fallback.

### Player View

`NativeVideoPlayerView` should keep a small, TV-readable overlay:

- Title.
- Engine and source label.
- Progress and time.
- Buffering/fallback messages.
- Current speed, fill mode, and quality.

The overlay should never block basic playback or prevent back navigation.

### Verification

Verification should include:

- Unit tests for REST payloads/parsers, poster URL conversion, media entry mapping, and playback strategy.
- Debug APK build.
- Emulator or real-device install.
- Manual or scripted E2E flow: login, home, all, TV, favorites, search, open playable item, seek, change speed, switch quality, back to list, back to home.
- Screenshots for home, list, favorites/search, and playback overlay.
- Log review for authentication, REST fetches, poster load failures, playback engine/fallback, and first-frame events.

## Risks

- Live server availability can interrupt real-time API exploration. Saved captures should be used as a baseline, and live verification should be retried once the server and browser tooling are stable.
- Android 4 image decoding, especially WebP, may fail on some devices. The UI must degrade to text cards.
- Current LibVLC dependency effectively targets Android 4.2+. Supporting Android 4.0/4.1 requires an IJK-only build variant or external-player-only playback mode.
- Full web parity is larger than one pass. Phase B intentionally prioritizes the visible TV browsing surface over advanced web-only panels.

## Acceptance Criteria

Phase B is complete when:

- Home, media library, all, TV, movie, other, favorites, and search results share a native visual shell close to Feiniu Video's web captures.
- Counts and list data load from REST where available.
- Posters render or degrade cleanly.
- Every visible action is D-pad reachable and has a clear focus state.
- The app builds and the main screens can be screenshot-tested on the Android debug target.

Phase C is complete when:

- A playable media item opens in the native player.
- VLC/IJK/software/external fallback behavior is logged and works as designed.
- Remote playback controls work from the player.
- End-to-end flow is verified on emulator or real device, with screenshots and logs saved under `.tooling` or `logs`.
