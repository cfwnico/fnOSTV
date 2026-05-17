# MediaCenter Gateway Design

## Goal

Make the media center loading path stable and easier to reason about by extracting its fallback order from `MainActivity` into a small gateway that records where data came from and why earlier sources failed.

## Scope

This phase covers the "影视大全" loading path only:

- REST media libraries and `/item/list` remain the first source.
- Local media index remains the second source.
- RPC `mediaCenter` candidates remain the third source.
- File browsing fallback remains the last source.
- The UI subtitle should describe the selected source and include concise fallback context when useful.

This phase does not add service-side media library creation, editing, deleting, sorting, or a new filter panel.

## Design

Add `MediaCenterGateway` under `com.fnostv.android4.media`. It owns only source selection and fallback decisions. Network and storage details stay behind small provider interfaces so the fallback behavior is unit-testable without real fnOS connectivity.

Provider order:

1. `RestProvider`
   - `libraries()`
   - `items(path, category, pageSize)`
2. `LocalIndexProvider`
   - `entries()`
3. `RpcProvider`
   - `entries()`
4. `FileProvider`
   - `files(path)`

The gateway returns `MediaCenterLoad`:

- `success`
- `title`
- `subtitle`
- `list`
- `sortEntries`
- `message`
- `source`

Root loading should use REST libraries first. If libraries exist, load the first library's items. If libraries are empty, try all REST items. If REST fails or returns empty, try local index, then RPC mediaCenter, then file fallback.

Child paths that look like REST media GUIDs should try REST item loading first, then file fallback.

## Error Handling

Each provider failure is captured as a short trace entry such as `REST: HTTP 500` or `RPC: errno=10000002`. The UI should not show a long stack of internal errors, but logs and subtitles can expose enough context to understand which path won.

## Testing

Unit tests cover:

- REST library items win over other sources.
- Empty REST falls back to local index.
- REST and local empty fall back to RPC mediaCenter.
- REST child GUID falls back to file list when REST fails.
- Failure trace is preserved in the final message when no source works.

## Acceptance Criteria

- `MainActivity.loadMediaCenter` delegates to the gateway.
- Existing visible behavior is preserved for happy paths.
- Subtitles identify `fnOS 影视媒体库`, `本地媒体库索引`, `fnOS mediaCenter 回退`, or file fallback.
- Unit tests pass.
- Debug build passes.
