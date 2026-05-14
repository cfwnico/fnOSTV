# Native Media Library UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a stable Android 4 native fnOSTV experience that imitates Feiniu Video's login, home, and media library settings while keeping media library management local and reliable.

**Architecture:** Add a local media library domain layer first, then wire it into settings and home screens. The app stores library definitions locally, scans fnOS file directories through the existing RPC client, and renders media rows from the local index instead of depending on unstable web-only mediaCenter administration APIs.

**Tech Stack:** Java, Android SDK 17 compatible views, SharedPreferences, org.json, existing OkHttp 3.12 RPC layer, existing native player.

---

## File Structure

- Create `app/src/main/java/com/fnostv/android4/media/MediaLibrary.java`: immutable library definition.
- Create `app/src/main/java/com/fnostv/android4/media/MediaLibraryCategory.java`: category constants and labels.
- Create `app/src/main/java/com/fnostv/android4/media/MediaLibraryClassifier.java`: path/name classification helpers.
- Create `app/src/main/java/com/fnostv/android4/media/MediaLibraryStore.java`: SharedPreferences JSON persistence for local libraries.
- Create `app/src/main/java/com/fnostv/android4/media/MediaIndexStore.java`: SharedPreferences JSON persistence for scanned media entries.
- Create `app/src/main/java/com/fnostv/android4/media/MediaLibraryScanner.java`: breadth-first scanner using `FnosRpcClient.listDir`.
- Create `app/src/main/java/com/fnostv/android4/ui/NativeSettingsView.java`: native settings shell with media library management.
- Modify `app/src/main/java/com/fnostv/android4/ui/SettingsForm.java`: polish login UI to match the reference.
- Modify `app/src/main/java/com/fnostv/android4/ui/NativeHomeView.java`: render Feiniu-like cards, icons, and real local media counts.
- Modify `app/src/main/java/com/fnostv/android4/MainActivity.java`: connect stores, scanner, settings actions, home counts, and category lists.
- Create `app/src/test/java/com/fnostv/android4/media/MediaLibraryClassifierTest.java`: plain Java regression tests for classification and path normalization.

## Task 1: Media Library Domain

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/media/MediaLibrary.java`
- Create: `app/src/main/java/com/fnostv/android4/media/MediaLibraryCategory.java`
- Create: `app/src/main/java/com/fnostv/android4/media/MediaLibraryClassifier.java`
- Test: `app/src/test/java/com/fnostv/android4/media/MediaLibraryClassifierTest.java`

- [ ] **Step 1: Write the failing classifier test**

```java
public final class MediaLibraryClassifierTest {
    public static void main(String[] args) {
        assertEquals("/video/Movies", MediaLibraryClassifier.normalizePath(" /video/Movies/ "));
        assertEquals("", MediaLibraryClassifier.normalizePath(" / "));
        assertEquals(MediaLibraryCategory.MOVIE, MediaLibraryClassifier.inferCategory("Movies", "/volume1/Movies"));
        assertEquals(MediaLibraryCategory.TV, MediaLibraryClassifier.inferCategory("Show.S01E02.mkv", "/volume1/TV/Show"));
        assertEquals(MediaLibraryCategory.OTHER, MediaLibraryClassifier.inferCategory("family.mov", "/volume1/HomeVideo"));
        assertTrue(MediaLibraryClassifier.isSupportedVideo("clip.rmvb"));
        assertFalse(MediaLibraryClassifier.isSupportedVideo("poster.jpg"));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("Expected false");
        }
    }
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `javac -encoding UTF-8 app/src/test/java/com/fnostv/android4/media/MediaLibraryClassifierTest.java`

Expected: FAIL because `MediaLibraryClassifier` does not exist.

- [ ] **Step 3: Implement the minimal domain classes**

Create the three media classes with immutable fields, category labels, path normalization, video extension detection, and category inference.

- [ ] **Step 4: Run the test and verify it passes**

Run: `javac -encoding UTF-8 -d .tooling/test-classes app/src/main/java/com/fnostv/android4/media/MediaLibraryCategory.java app/src/main/java/com/fnostv/android4/media/MediaLibraryClassifier.java app/src/test/java/com/fnostv/android4/media/MediaLibraryClassifierTest.java`

Run: `java -cp .tooling/test-classes com.fnostv.android4.media.MediaLibraryClassifierTest`

Expected: exit code 0.

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/plans/2026-05-12-native-media-library-ui.md app/src/main/java/com/fnostv/android4/media app/src/test/java/com/fnostv/android4/media
git commit -m "feat: add native media library domain"
```

## Task 2: Local Persistence and Scanner

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/media/MediaLibraryStore.java`
- Create: `app/src/main/java/com/fnostv/android4/media/MediaIndexStore.java`
- Create: `app/src/main/java/com/fnostv/android4/media/MediaLibraryScanner.java`
- Modify: `app/src/main/java/com/fnostv/android4/MainActivity.java`

- [ ] **Step 1: Add persistence stores**

Implement JSON array storage in the existing app preferences. Limit libraries to 200 and index entries to 1000. Deduplicate by path.

- [ ] **Step 2: Add scanner**

Implement bounded breadth-first scanning. For every configured root path, call `FnosRpcClient.listDir(session, path)`, enqueue directories up to depth 4, and keep supported video files.

- [ ] **Step 3: Wire MainActivity**

Initialize stores, seed one default “影视大全” library when none exist, and update `knownMediaEntries()` to merge index, recent playback, and favorites.

- [ ] **Step 4: Verify build**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: exit code 0.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fnostv/android4/media app/src/main/java/com/fnostv/android4/MainActivity.java
git commit -m "feat: persist and scan native media libraries"
```

## Task 3: Native Media Library Settings

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/ui/NativeSettingsView.java`
- Modify: `app/src/main/java/com/fnostv/android4/SettingsActivity.java`

- [ ] **Step 1: Build settings shell**

Create a full-screen dark settings view with left tabs: 账号连接, 媒体库, 外观, 服务. The media library tab contains add, scan, sort, and one row per library.

- [ ] **Step 2: Add library editor**

Use Android 4 compatible `AlertDialog` forms for library name, category, and comma-separated folder paths. Save through `MediaLibraryStore`.

- [ ] **Step 3: Add scan action**

Trigger `MediaLibraryScanner` in a worker thread, show status text, update `MediaIndexStore`, and refresh rows/counts when complete.

- [ ] **Step 4: Verify build**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: exit code 0.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fnostv/android4/ui/NativeSettingsView.java app/src/main/java/com/fnostv/android4/SettingsActivity.java
git commit -m "feat: add native media library settings"
```

## Task 4: Feiniu-Style Login and Home

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/ui/SettingsForm.java`
- Modify: `app/src/main/java/com/fnostv/android4/ui/NativeHomeView.java`
- Modify: `app/src/main/java/com/fnostv/android4/ui/FnosActionIconButton.java`
- Modify: `app/src/main/java/com/fnostv/android4/ui/FnosSidebarIconView.java`

- [ ] **Step 1: Polish login**

Match the reference structure: poster wall background, centered translucent panel, brand logo/title, server URL, username, password, keep login, SSL trust, primary login, secondary NAS login placeholder.

- [ ] **Step 2: Polish home**

Render left navigation with proper drawn icons, top circular icon buttons, media library cards, continue watching cards, favorite quick access, and category counts.

- [ ] **Step 3: Wire home data**

Show real library count, indexed media count, recent count, favorite count, and category counts.

- [ ] **Step 4: Verify build**

Run: `.\gradlew.bat :app:assembleDebug`

Expected: exit code 0.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fnostv/android4/ui app/src/main/java/com/fnostv/android4/MainActivity.java
git commit -m "feat: restyle native fnostv screens"
```

## Task 5: End-to-End Debug Notes and Push

**Files:**
- Modify: `README.md`
- Modify: `RELEASE_NOTES.md`

- [ ] **Step 1: Document the native media library flow**

Add instructions for login, opening settings, adding media library paths, scanning, browsing categories, and playing videos.

- [ ] **Step 2: Run verification**

Run: `.\gradlew.bat :app:assembleDebug`

Run emulator/debug scripts if the local emulator is available.

- [ ] **Step 3: Commit and push**

```bash
git add README.md RELEASE_NOTES.md
git commit -m "docs: describe native media library flow"
git push
```
