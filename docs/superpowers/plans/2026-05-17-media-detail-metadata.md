# Media Detail Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enrich the native media detail page with best-effort metadata and child-entry structure while keeping playback controls stable.

**Architecture:** Add pure Java metadata parsing first, attach metadata to `MediaDetailState`, then render it in `NativeMediaDetailView` and fetch it from `MainActivity` without blocking playback-source resolution.

**Tech Stack:** Java, Android API 17-compatible views, org.json, existing Gradle unit tests, existing fnOSTV REST/RPC clients.

---

### Task 1: Detail Metadata Parser

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/net/MediaDetailInfo.java`
- Create: `app/src/main/java/com/fnostv/android4/net/MediaDetailInfoParser.java`
- Create: `app/src/test/java/com/fnostv/android4/net/MediaDetailInfoParserTest.java`

- [ ] **Step 1: Write failing parser tests**

Create `MediaDetailInfoParserTest` with tests for:

```java
parsesDataItemMetadata();
parsesChildrenFromEpisodes();
formatsDurationFromSeconds();
```

Expected assertions:

```java
assertEquals("文化大观园", info.title);
assertEquals("2024", info.year);
assertEquals("8.6", info.rating);
assertEquals("纪录片", info.category);
assertEquals("45:30", info.durationLabel);
assertEquals(2, info.children.size());
```

- [ ] **Step 2: Run red test**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: FAIL because `MediaDetailInfo` and `MediaDetailInfoParser` do not exist.

- [ ] **Step 3: Implement `MediaDetailInfo`**

Create immutable fields for title, subtitle, overview, year, rating, category, durationLabel, sourceLabel, and children. Add a static `empty()` factory.

- [ ] **Step 4: Implement `MediaDetailInfoParser`**

Parse `JSONObject` defensively from `data`, `data.item`, `data.detail`, or the root object. Use first-non-empty helpers and parse child arrays named `children`, `episodes`, `seasons`, `versions`, `sources`, or `list`.

- [ ] **Step 5: Run green test**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 2: MediaDetailState Integration

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/ui/MediaDetailState.java`
- Modify: `app/src/test/java/com/fnostv/android4/ui/MediaDetailStateTest.java`

- [ ] **Step 1: Write failing state tests**

Add tests that verify:

```java
state.setLoadingDetail(true);
assertEquals(true, state.loadingDetail);
state.setDetailInfo(info);
assertEquals(false, state.loadingDetail);
assertEquals("文化大观园", state.detailInfo.title);
assertEquals(2, state.detailChildren().size());
```

Also verify detail error does not clear playback sources.

- [ ] **Step 2: Run red test**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: FAIL because `MediaDetailState` has no detail metadata fields.

- [ ] **Step 3: Implement state fields**

Add `MediaDetailInfo detailInfo`, `boolean loadingDetail`, `String detailError`, and `detailChildren()` helper. Keep playback source fields untouched.

- [ ] **Step 4: Run green test**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 3: REST Client Detail Fetch

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/net/FnosRestClient.java`
- Modify: `app/src/test/java/com/fnostv/android4/net/FnosRestClientTest.java`

- [ ] **Step 1: Write failing REST helper tests**

Add tests for `mediaDetailPathOrPayload` behavior if implemented as a static helper, or for parser-only fallback if no stable endpoint exists.

Preferred small helper:

```java
assertEquals("/item/detail?guid=abc", FnosRestClient.mediaDetailPath("abc"));
assertEquals("", FnosRestClient.mediaDetailPath(""));
```

- [ ] **Step 2: Run red test**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: FAIL because the helper does not exist.

- [ ] **Step 3: Add best-effort client method**

Add:

```java
public MediaDetailInfo mediaDetail(String guid) throws FnosRpcException
```

Use `GET /item/detail?guid=<guid>` first. If that endpoint fails, let the caller handle failure. Do not make playback depend on it.

- [ ] **Step 4: Run green test**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 4: Native Detail UI Rendering

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/ui/NativeMediaDetailView.java`

- [ ] **Step 1: Add UI fields**

Add `overviewView`, `detailStatusView`, and `childrenView`.

- [ ] **Step 2: Render metadata**

In `render()`:

- title uses `state.detailInfo.title` when non-empty.
- meta line includes year, rating, category, duration, favorite.
- overview appears only when non-empty.
- children line shows `剧集 N 项` when `detailChildren().size() > 0`.
- detail loading shows `详情加载中`.
- detail error is muted and does not replace playback source status.

- [ ] **Step 3: Run build test**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 5: MainActivity Wiring

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/MainActivity.java`

- [ ] **Step 1: Start detail fetch beside source fetch**

In `openMediaDetail(...)`, after showing the detail page, call `resolveDetailInfo(entry)` when `entry.path.length() > 0`.

- [ ] **Step 2: Implement `resolveDetailInfo(...)`**

Use a background thread:

```java
mediaDetailState.setLoadingDetail(true);
mediaDetailView.update(mediaDetailState);
MediaDetailInfo info = newRestClient().mediaDetail(entry.path);
```

On success, call `state.setDetailInfo(info)`. On failure, call `state.setDetailError("详情信息暂不可用")`.

- [ ] **Step 3: Guard stale callbacks**

Only update the UI if `mediaDetailState != null && mediaDetailState.entry == entry`.

- [ ] **Step 4: Run tests**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 6: Docs and Final Verification

**Files:**
- Modify: `README.md`
- Modify: `RELEASE_NOTES.md`
- Modify: `docs/fnos-interface-map.md`

- [ ] **Step 1: Document behavior**

Mention best-effort detail metadata, child-entry preparation, and non-blocking fallback when detail APIs fail.

- [ ] **Step 2: Run final verification**

Run:

```powershell
rg --line-number --hidden --glob '!/.git/**' --glob '!app/build/**' --glob '!.tooling/**' --glob '!logs/**' '<{7}|={7}|>{7}'
git diff --check
. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest
scripts\build-debug.cmd
```

Expected: no conflict markers, no whitespace errors, unit tests pass, debug build succeeds.

- [ ] **Step 3: Commit and push**

Run:

```powershell
git add app/src/main/java/com/fnostv/android4/MainActivity.java app/src/main/java/com/fnostv/android4/net/FnosRestClient.java app/src/main/java/com/fnostv/android4/net/MediaDetailInfo.java app/src/main/java/com/fnostv/android4/net/MediaDetailInfoParser.java app/src/main/java/com/fnostv/android4/ui/MediaDetailState.java app/src/main/java/com/fnostv/android4/ui/NativeMediaDetailView.java app/src/test/java/com/fnostv/android4/net/FnosRestClientTest.java app/src/test/java/com/fnostv/android4/net/MediaDetailInfoParserTest.java app/src/test/java/com/fnostv/android4/ui/MediaDetailStateTest.java README.md RELEASE_NOTES.md docs/fnos-interface-map.md docs/superpowers/specs/2026-05-17-media-detail-metadata-design.md docs/superpowers/plans/2026-05-17-media-detail-metadata.md
git commit -m "feat: enrich media detail metadata"
git push origin main
```
