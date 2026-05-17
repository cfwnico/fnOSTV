# Media Detail and Playback Source Design

## Goal

Build the first future-plan slice for fnOSTV Android: a usable media detail screen, clearer playback source selection, and richer remote shortcuts that work on Android 4 television devices.

## Scope

This slice focuses on the path from a media card to playback:

- Open a video item into a detail screen instead of immediately playing.
- Show title, format, path, poster, favorite state, and available playback source summary.
- Let the user choose play, favorite, source selection, or back using a remote.
- Keep direct playback available from detail without adding a heavy metadata dependency.
- Improve remote shortcuts only where they support this flow.

Not included in this slice:

- Full server-side mediaCenter administration.
- A complete movie metadata scraper.
- A new onboarding wizard.
- Large UI performance rewrites.
- A redesigned home page.

## User Experience

When the user selects a video from the file browser, media center list, category list, or poster row, the app opens a native detail screen. The detail screen presents the poster on the left, text metadata on the right, and a small horizontal action row:

- Play
- Source
- Favorite or Unfavorite
- Back

The default focus lands on Play. Pressing OK plays the selected source. Pressing Source opens a compact source picker when multiple valid sources are available. If only one source exists, the screen shows "原画" and OK plays directly.

Remote behavior:

- OK: activate focused action.
- Back: close source picker, close detail, or return to the previous page.
- Menu: toggle favorite on detail; keep existing player speed behavior during playback.
- Play/Pause: start playback from detail, toggle playback in player.
- Left/Right: move between detail actions and source choices.

## Data Model

The existing `FnosFileEntry` stays the primary media item. A small UI model should be added for detail rendering:

- `MediaDetailState`
  - `entry`
  - `favorite`
  - `sources`
  - `selectedSourceIndex`
  - `loadingSources`
  - `errorMessage`

`FnosPlaybackSource` should remain simple, but add helper behavior through a small selector utility instead of bloating the model:

- filter invalid sources
- remove duplicate URLs
- choose a default source
- generate a display label with index and quality label

## UI Components

Create `NativeMediaDetailView` in the existing `ui` package. It should follow current native view patterns:

- pure Java Android view construction
- no AndroidX
- use `FnosTheme`
- stable focusable buttons
- fixed dimensions suitable for old TV devices
- reuse `PosterLoader`

`NativeFileBrowserView` should continue to render grids. It should not own detail state.

`MainActivity` should coordinate:

- selected media entry
- source loading thread
- detail view visibility
- favorite toggles
- playback launch
- back stack behavior

## Playback Source Selection

Source resolution already exists in `MainActivity.resolvePlaybackSources` through `FnosRpcClient.playbackSources`. The new flow should reuse that logic but expose the result before playback:

1. User opens detail.
2. Detail shows "播放源准备中".
3. Background thread resolves sources.
4. Detail updates source label or error message.
5. Play uses selected source first.

If source resolution fails, detail should still allow external fallback through the existing playback failure path, and it should show a concise error message.

## Remote Navigation

Global `RemoteKeyHandler` should stay conservative. Detail-specific key handling belongs in `NativeMediaDetailView` where focus and picker state are visible.

Add only global shortcuts that are already safe across screens:

- media play/pause starts playback when detail is visible
- menu toggles favorite when detail is visible

Screen-specific focus and left/right behavior should be handled by the focused view hierarchy.

## Testing

Use small Java unit tests for pure behavior:

- `PlaybackSourceSelectorTest`
  - filters invalid sources
  - removes duplicate URLs
  - preserves source order
  - formats labels

- `MediaDetailStateTest`
  - clamps selected source index
  - reports current source
  - handles empty source list

UI behavior should be verified through debug build and emulator/manual flow:

- open media list
- select video
- detail appears
- toggle favorite with menu
- select source
- play
- return to list

## Acceptance Criteria

- Selecting a video opens detail instead of immediate playback.
- Detail can start playback using resolved sources.
- Multiple playback sources can be selected before playback.
- Favorite can be toggled from detail.
- Back behavior returns to the previous list without losing list visibility.
- Existing player controls still work.
- Existing debug build passes.
