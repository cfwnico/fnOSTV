# Old Device Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tune playback startup and poster decoding so fnOSTV behaves better on older Android TV boxes.

**Architecture:** Keep the existing native Java architecture. Update `PlaybackStrategy` for a narrower fast-start LAN profile, add a tiny pure helper for poster bitmap sample size, and wire it into `PosterLoader` while keeping UI behavior unchanged.

**Tech Stack:** Java, Android API 17-compatible code, Gradle unit tests, Android debug build.

---

### Task 1: Playback Fast-Start Strategy

**Files:**
- Modify: `app/src/test/java/com/fnostv/android4/player/PlaybackStrategyTest.java`
- Modify: `app/src/main/java/com/fnostv/android4/player/PlaybackStrategy.java`

- [x] **Step 1: Write the failing tests**

Add assertions that LAN MP4 uses a tighter low-latency profile, localhost is treated as LAN, internet MP4 remains stable, and high-risk LAN media remains fluent.

- [x] **Step 2: Run test to verify it fails**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: FAIL because the current LAN low-latency profile still uses the older 3500ms/1500ms cache and 512KB/800ms probe settings.

- [x] **Step 3: Implement minimal playback strategy changes**

Set low-risk LAN/localhost playback to `networkCachingMs=2500`, `fileCachingMs=1000`, `probeSizeKb=384`, and `analyzeDurationMs=600`. Keep remote internet MP4 and high-risk media unchanged.

- [x] **Step 4: Run test to verify it passes**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 2: Poster Decode Sampling

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/ui/BitmapSampleSize.java`
- Create: `app/src/test/java/com/fnostv/android4/ui/BitmapSampleSizeTest.java`
- Modify: `app/src/main/java/com/fnostv/android4/ui/PosterLoader.java`

- [x] **Step 1: Write the failing helper tests**

Cover these cases:

```java
assertEquals(4, BitmapSampleSize.forBounds(4000, 3000, 720, 720));
assertEquals(2, BitmapSampleSize.forBounds(1600, 900, 720, 720));
assertEquals(1, BitmapSampleSize.forBounds(500, 300, 720, 720));
assertEquals(1, BitmapSampleSize.forBounds(0, 300, 720, 720));
```

- [x] **Step 2: Run test to verify it fails**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: FAIL because `BitmapSampleSize` does not exist yet.

- [x] **Step 3: Implement the helper and poster decode path**

Add `BitmapSampleSize.forBounds(...)` with power-of-two sampling, then update `PosterLoader` to download image bytes, decode bounds, set `inSampleSize`, and decode from byte array.

- [x] **Step 4: Run test to verify it passes**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 3: Docs and Verification

**Files:**
- Modify: `README.md`
- Modify: `RELEASE_NOTES.md`

- [x] **Step 1: Document the performance behavior**

Mention the faster LAN low-risk playback profile and bounded poster decode behavior.

- [x] **Step 2: Run final verification**

Run:

```powershell
$conflictPattern = '<{7}|={7}|>{7}'; Get-ChildItem -Recurse -File | Where-Object { $_.FullName -notmatch '\\.git\\|\\app\\build\\|\\.tooling\\|\\logs\\' } | Select-String -Pattern $conflictPattern
git diff --check
. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest
scripts\build-debug.cmd
```

Expected: no conflict markers, no whitespace errors, unit tests pass, debug build succeeds.

- [x] **Step 3: Commit**

Run:

```powershell
git add app/src/main/java/com/fnostv/android4/player/PlaybackStrategy.java app/src/test/java/com/fnostv/android4/player/PlaybackStrategyTest.java app/src/main/java/com/fnostv/android4/ui/BitmapSampleSize.java app/src/test/java/com/fnostv/android4/ui/BitmapSampleSizeTest.java app/src/main/java/com/fnostv/android4/ui/PosterLoader.java README.md RELEASE_NOTES.md docs/superpowers/specs/2026-05-17-old-device-performance-design.md docs/superpowers/plans/2026-05-17-old-device-performance.md
git commit -m "feat: tune old device performance"
```
