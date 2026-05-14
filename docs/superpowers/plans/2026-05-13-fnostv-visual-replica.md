# fnOSTV Visual Replica Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the first Phase B slice: Feiniu-style native list shell plus poster metadata plumbing for Android 4.

**Architecture:** Keep the existing Java Android 4 project and extend the current native UI classes. Add testable poster URL/model helpers first, then adapt `NativeFileBrowserView` into a shell with sidebar navigation and stable list cards. Keep playback code unchanged in this pass.

**Tech Stack:** Java, Android SDK 28 APIs with `minSdkVersion 17`, OkHttp 3.12.x, plain Java main-method tests.

---

## File Structure

- Modify `docs/superpowers/specs/2026-05-13-fnostv-visual-playback-design.md`: optimized design language and implementation boundaries.
- Modify `app/src/main/java/com/fnostv/android4/net/FnosFileEntry.java`: add poster path metadata while preserving the existing constructor.
- Modify `app/src/main/java/com/fnostv/android4/net/FnosRestClient.java`: parse `poster` and `poster_list` fields, add testable poster image URL construction.
- Modify `app/src/test/java/com/fnostv/android4/net/FnosRestClientTest.java`: add red/green parser and poster URL tests.
- Modify `app/src/main/java/com/fnostv/android4/ui/NativeFileBrowserView.java`: add Feiniu-style sidebar shell, stable toolbar chips, card focus style, and poster-aware fallback text.
- Modify `app/src/main/java/com/fnostv/android4/MainActivity.java`: route the list sidebar actions through the same actions as the home sidebar.

## Task 1: Poster Metadata Contract

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/net/FnosFileEntry.java`
- Modify: `app/src/main/java/com/fnostv/android4/net/FnosRestClient.java`
- Test: `app/src/test/java/com/fnostv/android4/net/FnosRestClientTest.java`

- [ ] **Step 1: Write failing tests**

Add tests that assert REST entries keep poster metadata and `/sys/img` URLs are built from server poster paths.

- [ ] **Step 2: Run tests and confirm RED**

Run:

```powershell
.\.tooling\jdk\jdk-11.0.31+11\bin\javac.exe -cp app\src\main\java -d .tooling\test-classes app\src\test\java\com\fnostv\android4\net\FnosRestClientTest.java
```

Expected: compile failure because `posterPath` and `posterImageUrl` do not exist yet.

- [ ] **Step 3: Implement model/parser helpers**

Add `posterPath` and overloaded constructors to `FnosFileEntry`. Add `posterPath(JSONObject)` and `posterImageUrl(String, String, int)` to `FnosRestClient`, and pass parsed poster paths from REST entries.

- [ ] **Step 4: Run tests and confirm GREEN**

Run the same compile command, then:

```powershell
.\.tooling\jdk\jdk-11.0.31+11\bin\java.exe -cp .tooling\test-classes;app\src\main\java com.fnostv.android4.net.FnosRestClientTest
```

Expected: process exits `0`.

## Task 2: Native List Shell Replica

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/ui/NativeFileBrowserView.java`
- Modify: `app/src/main/java/com/fnostv/android4/MainActivity.java`

- [ ] **Step 1: Add browser sidebar callback**

Extend `NativeFileBrowserView.Listener` with `void onBrowserAction(String action);` and implement it in `MainActivity` by forwarding to `onHomeAction(action)`.

- [ ] **Step 2: Wrap list content in a two-column shell**

Change `NativeFileBrowserView.create()` so the root is horizontal: a Feiniu-style sidebar on the left and the existing header/tools/grid content on the right.

- [ ] **Step 3: Add sidebar entries**

Add sidebar buttons for home, favorites, media library, all, movie, TV, and other. Reuse `FnosSidebarIconView` types and `NativeHomeView.ACTION_*` constants.

- [ ] **Step 4: Refine toolbar and card focus**

Apply button focus styling to filter/sort/layout chips, use card focused background on media cards, and make poster fallback text include poster presence/resolution cues without layout shift.

## Task 3: Verification

**Files:**
- Build verification only.

- [ ] **Step 1: Run focused Java tests**

Run:

```powershell
.\.tooling\jdk\jdk-11.0.31+11\bin\javac.exe -cp app\src\main\java -d .tooling\test-classes app\src\test\java\com\fnostv\android4\net\FnosRestClientTest.java
.\.tooling\jdk\jdk-11.0.31+11\bin\java.exe -cp .tooling\test-classes;app\src\main\java com.fnostv.android4.net.FnosRestClientTest
```

Expected: exit `0`.

- [ ] **Step 2: Build debug APK**

Run:

```powershell
scripts\build-debug.cmd
```

Expected: Gradle exits `0` and creates the debug APK.

- [ ] **Step 3: Report any remaining E2E gap**

If emulator or browser automation is unavailable, state that APK build and unit tests passed but device screenshot E2E was not run in this session.
