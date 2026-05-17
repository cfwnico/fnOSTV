# Media Detail Playback Source Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a native media detail screen that lets TV users inspect a media item, choose a playback source, toggle favorite, and start playback with remote-friendly controls.

**Architecture:** Keep `MainActivity` as the coordinator and add focused UI/model helpers under the existing Java Android 4 structure. Pure source-selection and detail-state behavior is covered by JVM tests first; UI wiring follows current `NativeFileBrowserView` and `NativeVideoPlayerView` patterns.

**Tech Stack:** Java, Android SDK 28, no AndroidX, Gradle 6.7.1, existing fnOSTV native UI classes.

---

## File Structure

- Create `app/src/main/java/com/fnostv/android4/player/PlaybackSourceSelector.java`
  - Pure helper for valid source filtering, duplicate removal, label formatting, default source selection, and index clamping.
- Create `app/src/main/java/com/fnostv/android4/ui/MediaDetailState.java`
  - Small state holder for selected entry, favorite state, resolved sources, selected source index, loading state, and error message.
- Create `app/src/main/java/com/fnostv/android4/ui/NativeMediaDetailView.java`
  - Native Android 4 detail screen. Renders poster, metadata, action buttons, source picker state, and handles local remote keys.
- Modify `app/src/main/java/com/fnostv/android4/MainActivity.java`
  - Add detail view creation, selected-entry state, source resolution flow, playback from selected source, favorite toggling, and back behavior.
- Modify `app/src/main/java/com/fnostv/android4/ui/NativeFileBrowserView.java`
  - Keep selection callback unchanged; behavior change lives in `MainActivity`.
- Test `app/src/test/java/com/fnostv/android4/player/PlaybackSourceSelectorTest.java`
- Test `app/src/test/java/com/fnostv/android4/ui/MediaDetailStateTest.java`

## Task 1: Playback Source Selector

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/player/PlaybackSourceSelector.java`
- Test: `app/src/test/java/com/fnostv/android4/player/PlaybackSourceSelectorTest.java`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/fnostv/android4/player/PlaybackSourceSelectorTest.java`:

```java
package com.fnostv.android4.player;

import com.fnostv.android4.net.FnosPlaybackSource;

import java.util.ArrayList;
import java.util.List;

public final class PlaybackSourceSelectorTest {
    public static void main(String[] args) {
        filtersInvalidAndDuplicateSources();
        clampsSourceIndex();
        formatsDisplayLabels();
    }

    private static void filtersInvalidAndDuplicateSources() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("重复", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("空", ""));
        input.add(new FnosPlaybackSource("高清", "http://host/b.mp4"));

        List<FnosPlaybackSource> result = PlaybackSourceSelector.normalize(input);

        assertEquals(2, result.size());
        assertEquals("原画", result.get(0).label);
        assertEquals("http://host/a.mp4", result.get(0).url);
        assertEquals("高清", result.get(1).label);
        assertEquals("http://host/b.mp4", result.get(1).url);
    }

    private static void clampsSourceIndex() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("高清", "http://host/b.mp4"));

        assertEquals(0, PlaybackSourceSelector.clampIndex(input, -1));
        assertEquals(1, PlaybackSourceSelector.clampIndex(input, 9));
        assertEquals(0, PlaybackSourceSelector.clampIndex(new ArrayList<FnosPlaybackSource>(), 9));
    }

    private static void formatsDisplayLabels() {
        List<FnosPlaybackSource> input = new ArrayList<FnosPlaybackSource>();
        input.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        input.add(new FnosPlaybackSource("1080P", "http://host/b.mp4"));

        assertEquals("1/2 原画", PlaybackSourceSelector.displayLabel(input, 0));
        assertEquals("2/2 1080P", PlaybackSourceSelector.displayLabel(input, 1));
        assertEquals("无可用播放源", PlaybackSourceSelector.displayLabel(new ArrayList<FnosPlaybackSource>(), 0));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
. .\scripts\env.ps1
gradle --no-daemon testDebugUnitTest
```

Expected: compilation fails because `PlaybackSourceSelector` does not exist.

- [ ] **Step 3: Implement selector**

Create `app/src/main/java/com/fnostv/android4/player/PlaybackSourceSelector.java`:

```java
package com.fnostv.android4.player;

import com.fnostv.android4.net.FnosPlaybackSource;

import java.util.ArrayList;
import java.util.List;

public final class PlaybackSourceSelector {
    private PlaybackSourceSelector() {
    }

    public static List<FnosPlaybackSource> normalize(List<FnosPlaybackSource> sources) {
        List<FnosPlaybackSource> result = new ArrayList<FnosPlaybackSource>();
        if (sources == null) {
            return result;
        }
        for (int i = 0; i < sources.size(); i++) {
            FnosPlaybackSource source = sources.get(i);
            if (source != null && source.isValid() && !containsUrl(result, source.url)) {
                result.add(source);
            }
        }
        return result;
    }

    public static int clampIndex(List<FnosPlaybackSource> sources, int index) {
        int size = sources == null ? 0 : sources.size();
        if (size <= 0 || index < 0) {
            return 0;
        }
        return index >= size ? size - 1 : index;
    }

    public static FnosPlaybackSource selectedSource(List<FnosPlaybackSource> sources, int index) {
        if (sources == null || sources.size() == 0) {
            return null;
        }
        return sources.get(clampIndex(sources, index));
    }

    public static String displayLabel(List<FnosPlaybackSource> sources, int index) {
        if (sources == null || sources.size() == 0) {
            return "无可用播放源";
        }
        int selected = clampIndex(sources, index);
        FnosPlaybackSource source = sources.get(selected);
        return (selected + 1) + "/" + sources.size() + " " + source.label;
    }

    private static boolean containsUrl(List<FnosPlaybackSource> sources, String url) {
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).url.equals(url)) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 4: Run selector test**

Run:

```powershell
. .\scripts\env.ps1
gradle --no-daemon testDebugUnitTest
```

Expected: `PlaybackSourceSelectorTest` passes.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/fnostv/android4/player/PlaybackSourceSelector.java app/src/test/java/com/fnostv/android4/player/PlaybackSourceSelectorTest.java
git commit -m "feat: add playback source selector"
```

## Task 2: Media Detail State

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/ui/MediaDetailState.java`
- Test: `app/src/test/java/com/fnostv/android4/ui/MediaDetailStateTest.java`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/fnostv/android4/ui/MediaDetailStateTest.java`:

```java
package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosPlaybackSource;

import java.util.ArrayList;
import java.util.List;

public final class MediaDetailStateTest {
    public static void main(String[] args) {
        emptySourcesAreSafe();
        selectsCurrentSource();
        storesLoadingAndErrorState();
    }

    private static void emptySourcesAreSafe() {
        MediaDetailState state = new MediaDetailState(entry(), false);
        assertEquals("无可用播放源", state.sourceLabel());
        assertEquals(null, state.currentSource());
    }

    private static void selectsCurrentSource() {
        MediaDetailState state = new MediaDetailState(entry(), true);
        List<FnosPlaybackSource> sources = new ArrayList<FnosPlaybackSource>();
        sources.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
        sources.add(new FnosPlaybackSource("1080P", "http://host/b.mp4"));

        state.setSources(sources);
        state.selectSource(8);

        assertEquals("2/2 1080P", state.sourceLabel());
        assertEquals("http://host/b.mp4", state.currentSource().url);
        assertEquals(true, state.favorite);
    }

    private static void storesLoadingAndErrorState() {
        MediaDetailState state = new MediaDetailState(entry(), false);
        state.setLoadingSources(true);
        assertEquals(true, state.loadingSources);
        state.setError("播放源准备失败");
        assertEquals(false, state.loadingSources);
        assertEquals("播放源准备失败", state.errorMessage);
    }

    private static FnosFileEntry entry() {
        return new FnosFileEntry("Movie.mkv", "/video/Movie.mkv", false, 1024L, "video/x-matroska", "");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
. .\scripts\env.ps1
gradle --no-daemon testDebugUnitTest
```

Expected: compilation fails because `MediaDetailState` does not exist.

- [ ] **Step 3: Implement state**

Create `app/src/main/java/com/fnostv/android4/ui/MediaDetailState.java`:

```java
package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;
import com.fnostv.android4.net.FnosPlaybackSource;
import com.fnostv.android4.player.PlaybackSourceSelector;

import java.util.ArrayList;
import java.util.List;

public final class MediaDetailState {
    public final FnosFileEntry entry;
    public boolean favorite;
    public boolean loadingSources;
    public String errorMessage = "";
    private final List<FnosPlaybackSource> sources = new ArrayList<FnosPlaybackSource>();
    private int selectedSourceIndex;

    public MediaDetailState(FnosFileEntry entry, boolean favorite) {
        this.entry = entry;
        this.favorite = favorite;
    }

    public void setLoadingSources(boolean loadingSources) {
        this.loadingSources = loadingSources;
        if (loadingSources) {
            errorMessage = "";
        }
    }

    public void setError(String errorMessage) {
        this.loadingSources = false;
        this.errorMessage = errorMessage == null ? "" : errorMessage;
    }

    public void setSources(List<FnosPlaybackSource> resolvedSources) {
        sources.clear();
        sources.addAll(PlaybackSourceSelector.normalize(resolvedSources));
        selectedSourceIndex = PlaybackSourceSelector.clampIndex(sources, selectedSourceIndex);
        loadingSources = false;
        errorMessage = "";
    }

    public List<FnosPlaybackSource> sources() {
        return new ArrayList<FnosPlaybackSource>(sources);
    }

    public void selectSource(int index) {
        selectedSourceIndex = PlaybackSourceSelector.clampIndex(sources, index);
    }

    public int selectedSourceIndex() {
        return selectedSourceIndex;
    }

    public FnosPlaybackSource currentSource() {
        return PlaybackSourceSelector.selectedSource(sources, selectedSourceIndex);
    }

    public String sourceLabel() {
        return PlaybackSourceSelector.displayLabel(sources, selectedSourceIndex);
    }
}
```

- [ ] **Step 4: Run state test**

Run:

```powershell
. .\scripts\env.ps1
gradle --no-daemon testDebugUnitTest
```

Expected: `MediaDetailStateTest` passes.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/fnostv/android4/ui/MediaDetailState.java app/src/test/java/com/fnostv/android4/ui/MediaDetailStateTest.java
git commit -m "feat: add media detail state"
```

## Task 3: Native Media Detail View

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/ui/NativeMediaDetailView.java`

- [ ] **Step 1: Create the listener contract and shell**

Create `NativeMediaDetailView` with this public contract:

```java
public final class NativeMediaDetailView {
    public interface Listener {
        void onDetailPlayRequested(MediaDetailState state);
        void onDetailFavoriteToggled(MediaDetailState state);
        void onDetailBackRequested();
        void onDetailSourceSelected(MediaDetailState state, int sourceIndex);
    }
}
```

The class should mirror existing UI wrappers:

- constructor receives `Context` and `Listener`
- `View create()`
- `void show(MediaDetailState state, String posterBaseUrl)`
- `void update(MediaDetailState state)`
- `void hide()`
- `boolean isVisible()`

- [ ] **Step 2: Implement layout**

Implement a full-screen horizontal layout:

- left: poster frame using `PosterLoader`
- right: title, format/path text, status text, source label, action row
- action row: `播放`, `播放源`, `收藏` or `取消收藏`, `返回`

Use `FnosTheme.COLOR_APP_BG`, `FnosTheme.COLOR_PANEL`, `FnosTheme.COLOR_PRIMARY`, and fixed dp sizes similar to `NativeFileBrowserView`.

- [ ] **Step 3: Implement actions and keys**

Action behavior:

- `播放`: call `listener.onDetailPlayRequested(state)`
- `播放源`: if sources exist, cycle source index and call `listener.onDetailSourceSelected(state, nextIndex)`
- `收藏/取消收藏`: call `listener.onDetailFavoriteToggled(state)`
- `返回`: call `listener.onDetailBackRequested()`

Key behavior:

- `KEYCODE_MENU`: favorite toggle
- `KEYCODE_MEDIA_PLAY_PAUSE` and `KEYCODE_SPACE`: play
- `KEYCODE_BACK`: back

- [ ] **Step 4: Build**

Run:

```powershell
scripts\build-debug.cmd
```

Expected: debug build succeeds.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/fnostv/android4/ui/NativeMediaDetailView.java
git commit -m "feat: add native media detail view"
```

## Task 4: Wire Detail Flow in MainActivity

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/MainActivity.java`

- [ ] **Step 1: Add detail fields and interface**

Update the class implements list to include `NativeMediaDetailView.Listener`.

Add fields:

```java
private NativeMediaDetailView mediaDetailView;
private MediaDetailState mediaDetailState;
private String previousDetailPath = "";
```

- [ ] **Step 2: Create and add detail view**

Where native views are created, instantiate:

```java
mediaDetailView = new NativeMediaDetailView(this, this);
root.addView(mediaDetailView.create(), fullScreenParams());
```

Use the same parent/container style as existing native views.

- [ ] **Step 3: Open detail for video entries**

In `onFileEntrySelected(FnosFileEntry entry)`, keep directory navigation unchanged. For video entries:

```java
openMediaDetail(entry);
```

Implement:

```java
private void openMediaDetail(FnosFileEntry entry) {
    if (entry == null || entry.directory) {
        return;
    }
    previousDetailPath = currentFilePath;
    mediaDetailState = new MediaDetailState(entry, favoriteStore.isFavorite(entry));
    hideNativeListsForDetail();
    mediaDetailView.show(mediaDetailState, profile == null ? "" : profile.baseUrl);
    resolveDetailSources(entry);
}
```

`hideNativeListsForDetail()` should hide home and file browser views but leave the player untouched.

- [ ] **Step 4: Resolve sources into detail state**

Implement:

```java
private void resolveDetailSources(final FnosFileEntry entry) {
    mediaDetailState.setLoadingSources(true);
    mediaDetailView.update(mediaDetailState);
    new Thread(new Runnable() {
        @Override
        public void run() {
            final PlaybackSourcesResult result = resolvePlaybackSources(entry);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mediaDetailState == null || mediaDetailState.entry != entry) {
                        return;
                    }
                    if (result.success) {
                        mediaDetailState.setSources(result.sources);
                    } else {
                        mediaDetailState.setError(result.errorMessage);
                    }
                    mediaDetailView.update(mediaDetailState);
                }
            });
        }
    }, "fnos-detail-sources").start();
}
```

- [ ] **Step 5: Implement listener methods**

Add:

```java
@Override
public void onDetailPlayRequested(MediaDetailState state) {
    if (state == null || state.entry == null) {
        return;
    }
    FnosPlaybackSource source = state.currentSource();
    List<FnosPlaybackSource> sources = state.sources();
    if (source != null) {
        state.selectSource(state.selectedSourceIndex());
    }
    playResolvedSources(state.entry, sources);
}

@Override
public void onDetailFavoriteToggled(MediaDetailState state) {
    if (state == null || state.entry == null) {
        return;
    }
    onFileFavoriteToggled(state.entry);
    state.favorite = favoriteStore.isFavorite(state.entry);
    mediaDetailView.update(state);
}

@Override
public void onDetailSourceSelected(MediaDetailState state, int sourceIndex) {
    if (state == null) {
        return;
    }
    state.selectSource(sourceIndex);
    mediaDetailView.update(state);
}

@Override
public void onDetailBackRequested() {
    closeMediaDetail();
}
```

Adjust `onDetailPlayRequested` if needed so selected source is placed first before calling `nativeVideoPlayerView.play`.

- [ ] **Step 6: Back behavior**

Update `goBack()`:

```java
if (mediaDetailView != null && mediaDetailView.isVisible()) {
    closeMediaDetail();
    return true;
}
```

`closeMediaDetail()` hides detail and restores file browser or home based on the previous visible context.

- [ ] **Step 7: Build**

Run:

```powershell
scripts\build-debug.cmd
```

Expected: debug build succeeds.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/com/fnostv/android4/MainActivity.java
git commit -m "feat: open media detail before playback"
```

## Task 5: Selected Source Playback Ordering

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/ui/MediaDetailState.java`
- Modify: `app/src/test/java/com/fnostv/android4/ui/MediaDetailStateTest.java`
- Modify: `app/src/main/java/com/fnostv/android4/MainActivity.java`

- [ ] **Step 1: Add failing test for selected source ordering**

Add to `MediaDetailStateTest`:

```java
private static void selectedSourceCanBeMovedFirstForPlayback() {
    MediaDetailState state = new MediaDetailState(entry(), false);
    List<FnosPlaybackSource> sources = new ArrayList<FnosPlaybackSource>();
    sources.add(new FnosPlaybackSource("原画", "http://host/a.mp4"));
    sources.add(new FnosPlaybackSource("1080P", "http://host/b.mp4"));
    state.setSources(sources);
    state.selectSource(1);

    List<FnosPlaybackSource> ordered = state.sourcesForPlayback();

    assertEquals("1080P", ordered.get(0).label);
    assertEquals("原画", ordered.get(1).label);
}
```

Call it from `main`.

- [ ] **Step 2: Run test to verify it fails**

Run:

```powershell
. .\scripts\env.ps1
gradle --no-daemon testDebugUnitTest
```

Expected: compilation fails because `sourcesForPlayback` does not exist.

- [ ] **Step 3: Implement playback ordering**

Add to `MediaDetailState`:

```java
public List<FnosPlaybackSource> sourcesForPlayback() {
    List<FnosPlaybackSource> ordered = sources();
    if (ordered.size() <= 1) {
        return ordered;
    }
    int selected = PlaybackSourceSelector.clampIndex(ordered, selectedSourceIndex);
    FnosPlaybackSource first = ordered.remove(selected);
    ordered.add(0, first);
    return ordered;
}
```

Use `state.sourcesForPlayback()` in `MainActivity.onDetailPlayRequested`.

- [ ] **Step 4: Run tests and build**

Run:

```powershell
. .\scripts\env.ps1
gradle --no-daemon testDebugUnitTest
scripts\build-debug.cmd
```

Expected: tests and debug build succeed.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/fnostv/android4/ui/MediaDetailState.java app/src/test/java/com/fnostv/android4/ui/MediaDetailStateTest.java app/src/main/java/com/fnostv/android4/MainActivity.java
git commit -m "feat: play selected media source first"
```

## Task 6: Manual Verification and Docs

**Files:**
- Modify: `README.md`
- Modify: `RELEASE_NOTES.md`

- [ ] **Step 1: Update README**

Add detail and source selection to the feature list and usage flow:

```markdown
- 新增影视详情页，可在播放前查看格式、路径、收藏状态和播放源。
- 支持在详情页切换播放源后再开始播放。
```

- [ ] **Step 2: Update release notes**

Add under current development capabilities:

```markdown
- 影视详情页：从媒体列表进入详情页后再播放。
- 播放源选择：支持播放前查看和切换可用播放源。
- 遥控器增强：详情页支持菜单键收藏、播放键播放、返回键关闭。
```

- [ ] **Step 3: Run final verification**

Run:

```powershell
$conflictPattern = '<{7}|={7}|>{7}'
rg -n $conflictPattern README.md RELEASE_NOTES.md app/src/main/java app/src/test/java
git diff --check
. .\scripts\env.ps1
gradle --no-daemon testDebugUnitTest
scripts\build-debug.cmd
```

Expected:

- no conflict markers
- no diff check errors
- unit tests pass
- debug build succeeds

- [ ] **Step 4: Commit docs**

```powershell
git add README.md RELEASE_NOTES.md
git commit -m "docs: document media detail flow"
```

## Plan Self-Review

- Spec coverage: detail screen, source selection, favorite toggle, remote shortcuts, tests, and debug verification are mapped to tasks.
- Completeness check: every task has concrete files, commands, and expected results.
- Type consistency: `PlaybackSourceSelector`, `MediaDetailState`, and `NativeMediaDetailView` names match across tasks.
