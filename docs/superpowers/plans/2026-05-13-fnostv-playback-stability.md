# fnOSTV Playback Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve Phase C playback startup and fallback behavior without changing the player engine stack.

**Architecture:** Start with `PlaybackStrategy` because it is pure Java and already covered by main-method tests. Tune LAN/direct media separately from remote or high-risk media, then verify the debug APK still builds before any device E2E.

**Tech Stack:** Java, Android SDK 28 APIs with `minSdkVersion 17`, VLC/IJK through existing `PlayerEngine` abstractions.

---

## Task 1: LAN Playback Strategy

**Files:**
- Modify: `app/src/test/java/com/fnostv/android4/player/PlaybackStrategyTest.java`
- Modify: `app/src/main/java/com/fnostv/android4/player/PlaybackStrategy.java`

- [ ] **Step 1: Write failing tests**

Add tests for LAN HTTP MP4 fast-start cache and high-risk LAN MKV retaining fluent software playback.

- [ ] **Step 2: Run tests and confirm RED**

Run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ". .\scripts\env.ps1; gradle.bat :app:testDebugUnitTest --no-daemon"
```

Expected: the new LAN MP4 assertion fails before the strategy is tuned.

- [ ] **Step 3: Implement strategy**

Add private-network host detection for `10.*`, `172.16.*` through `172.31.*`, `192.168.*`, `127.*`, and `localhost`. For low-risk LAN media, keep hardware decode and stable profile, but reduce network cache to improve startup.

- [ ] **Step 4: Run tests and confirm GREEN**

Run the Gradle unit test command again. Expected: build success.

## Task 2: Build Verification

**Files:**
- Build verification only.

- [ ] **Step 1: Build debug APK**

Run:

```powershell
scripts\build-debug.cmd
```

Expected: Gradle exits `0` and writes `app\build\outputs\apk\debug\app-debug.apk`.

- [ ] **Step 2: Check device availability**

Run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command ". .\scripts\env.ps1; adb devices"
```

Expected: if no devices are attached, report that device E2E remains pending.
