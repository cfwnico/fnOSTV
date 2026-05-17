# Video Player Number Seek Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add number-key seek shortcuts to the native video player while preserving existing remote-control behavior.

**Architecture:** Implement a pure Java `NumberSeekShortcut` helper for key mapping, target calculation, and hint text. Wire it into `NativeVideoPlayerView.onKeyDown(...)` before existing directional controls so supported number keys request a percent seek and unsupported keys fall through unchanged.

**Tech Stack:** Java, Android API 17 key codes, existing Gradle debug unit tests, existing native Android view code.

---

### Task 1: Pure Java Shortcut Helper

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/player/NumberSeekShortcut.java`
- Create: `app/src/test/java/com/fnostv/android4/player/NumberSeekShortcutTest.java`

- [x] **Step 1: Write failing helper tests**

Create `NumberSeekShortcutTest`:

```java
package com.fnostv.android4.player;

import android.view.KeyEvent;

public final class NumberSeekShortcutTest {
    public static void main(String[] args) {
        mapsNumberKeysToPercent();
        ignoresUnsupportedKeys();
        calculatesSeekTargets();
        rejectsUnknownDuration();
        clampsTargetsToDuration();
        formatsHints();
    }

    private static void mapsNumberKeysToPercent() {
        assertEquals(0, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_0));
        assertEquals(10, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_1));
        assertEquals(50, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_5));
        assertEquals(90, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_9));
    }

    private static void ignoresUnsupportedKeys() {
        assertEquals(-1, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals(-1, NumberSeekShortcut.percentForKey(KeyEvent.KEYCODE_MENU));
    }

    private static void calculatesSeekTargets() {
        assertEquals(0, NumberSeekShortcut.targetMs(100000, 0));
        assertEquals(30000, NumberSeekShortcut.targetMs(100000, 30));
        assertEquals(90000, NumberSeekShortcut.targetMs(100000, 90));
    }

    private static void rejectsUnknownDuration() {
        assertEquals(-1, NumberSeekShortcut.targetMs(0, 50));
        assertEquals(-1, NumberSeekShortcut.targetMs(-1000, 50));
    }

    private static void clampsTargetsToDuration() {
        assertEquals(1000, NumberSeekShortcut.targetMs(1000, 150));
        assertEquals(0, NumberSeekShortcut.targetMs(1000, -10));
    }

    private static void formatsHints() {
        assertEquals("回到开头", NumberSeekShortcut.hintForPercent(0));
        assertEquals("跳转 30%", NumberSeekShortcut.hintForPercent(30));
        assertEquals("跳转 90%", NumberSeekShortcut.hintForPercent(90));
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
```

- [x] **Step 2: Run red test**

Run:

```powershell
. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest
```

Expected: FAIL because `NumberSeekShortcut` does not exist.

- [x] **Step 3: Implement helper**

Create `NumberSeekShortcut`:

```java
package com.fnostv.android4.player;

import android.view.KeyEvent;

public final class NumberSeekShortcut {
    private NumberSeekShortcut() {
    }

    public static int percentForKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_0) {
            return 0;
        }
        if (keyCode >= KeyEvent.KEYCODE_1 && keyCode <= KeyEvent.KEYCODE_9) {
            return (keyCode - KeyEvent.KEYCODE_0) * 10;
        }
        return -1;
    }

    public static int targetMs(int durationMs, int percent) {
        if (durationMs <= 0) {
            return -1;
        }
        int clampedPercent = Math.max(0, Math.min(100, percent));
        long target = ((long) durationMs * clampedPercent) / 100L;
        return (int) Math.max(0L, Math.min((long) durationMs, target));
    }

    public static String hintForPercent(int percent) {
        return percent == 0 ? "回到开头" : "跳转 " + percent + "%";
    }
}
```

- [x] **Step 4: Run green test**

Run:

```powershell
. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest
```

Expected: PASS.

### Task 2: Player View Wiring

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/ui/NativeVideoPlayerView.java`

- [x] **Step 1: Import helper**

Add:

```java
import com.fnostv.android4.player.NumberSeekShortcut;
```

- [x] **Step 2: Handle number keys before directional controls**

In `onKeyDown(int keyCode, KeyEvent event)`, after the play/pause block and before the left/right block, add:

```java
int numberSeekPercent = NumberSeekShortcut.percentForKey(keyCode);
if (numberSeekPercent >= 0) {
    seekToPercent(numberSeekPercent);
    return true;
}
```

- [x] **Step 3: Add `seekToPercent` helper**

Near `seekBy(int deltaMs)`, add:

```java
private void seekToPercent(int percent) {
    int target = NumberSeekShortcut.targetMs(duration(), percent);
    if (target < 0) {
        showHint("暂时无法跳转");
        showControlsTemporarily();
        return;
    }
    requestSeekTo(target);
    showHint(NumberSeekShortcut.hintForPercent(percent));
    showControlsTemporarily();
}
```

- [x] **Step 4: Run build test**

Run:

```powershell
. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest
```

Expected: PASS.

### Task 3: Docs and Final Verification

**Files:**
- Modify: `README.md`
- Modify: `RELEASE_NOTES.md`
- Modify: `docs/superpowers/plans/2026-05-17-video-player-number-seek.md`

- [x] **Step 1: Update user-facing docs**

In `README.md`, update the remote keys section with:

```markdown
- 数字键 0-9：播放页按 0 回到开头，按 1-9 跳到 10%-90% 位置。
```

In `RELEASE_NOTES.md`, add:

```markdown
- 播放页数字键跳转：0 回到开头，1-9 快速跳到 10%-90% 位置。
```

- [x] **Step 2: Run final verification**

Run:

```powershell
rg --line-number --hidden --glob '!/.git/**' --glob '!app/build/**' --glob '!.tooling/**' --glob '!logs/**' '<{7}|={7}|>{7}'
git diff --check
. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest
scripts\build-debug.cmd
```

Expected: no conflict markers, no whitespace errors, unit tests pass, debug build succeeds.

- [x] **Step 3: Commit and push**

Run:

```powershell
git add app/src/main/java/com/fnostv/android4/player/NumberSeekShortcut.java app/src/main/java/com/fnostv/android4/ui/NativeVideoPlayerView.java app/src/test/java/com/fnostv/android4/player/NumberSeekShortcutTest.java README.md RELEASE_NOTES.md docs/superpowers/specs/2026-05-17-video-player-number-seek-design.md docs/superpowers/plans/2026-05-17-video-player-number-seek.md
git commit -m "feat: add video number seek shortcuts"
git push origin main
```
