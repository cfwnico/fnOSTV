# Home Poster Sections Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Expand the native home screen into bounded poster sections while preserving Android 4 performance and current navigation.

**Architecture:** Add pure Java section models first, verify them with JVM tests, then wire `MainActivity` and `NativeHomeView` to render bounded poster sections. Keep the existing shell, sidebar, counts, and `PosterLoader` behavior.

**Tech Stack:** Java, Android API 17-compatible views, existing Gradle unit test flow, existing native UI helpers.

---

### Task 1: Pure Home Poster Section Model

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/ui/HomePosterSection.java`
- Create: `app/src/main/java/com/fnostv/android4/ui/HomePosterSections.java`
- Create: `app/src/test/java/com/fnostv/android4/ui/HomePosterSectionsTest.java`

- [ ] **Step 1: Write the failing tests**

Add `HomePosterSectionsTest` with these test cases:

```java
package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.List;

public final class HomePosterSectionsTest {
    public static void main(String[] args) {
        buildsRecentFavoritesAndMediaSectionsInOrder();
        limitsVisibleEntriesAndMarksHasMore();
        removesDuplicatePathsInsideEachSection();
        keepsMediaSectionWhenAllListsAreEmpty();
    }

    private static void buildsRecentFavoritesAndMediaSectionsInOrder() {
        List<HomePosterSection> sections = HomePosterSections.from(
                list(entry("Movie One", "/movie/one.mp4", "/poster/one.webp")),
                list(entry("Recent One", "/recent/one.mp4", "/poster/recent.webp")),
                list(entry("Favorite One", "/favorite/one.mp4", "/poster/favorite.webp")));

        assertEquals("继续观看", sections.get(0).title);
        assertEquals(NativeHomeView.ACTION_RECENT, sections.get(0).action);
        assertEquals("我的收藏", sections.get(1).title);
        assertEquals(NativeHomeView.ACTION_FAVORITES, sections.get(1).action);
        assertEquals("影视大全", sections.get(2).title);
        assertEquals(NativeHomeView.ACTION_MEDIA, sections.get(2).action);
    }

    private static void limitsVisibleEntriesAndMarksHasMore() {
        List<FnosFileEntry> recent = new ArrayList<FnosFileEntry>();
        for (int i = 0; i < 8; i++) {
            recent.add(entry("Recent " + i, "/recent/" + i + ".mp4", "/poster/" + i + ".webp"));
        }

        HomePosterSection section = HomePosterSections.from(null, recent, null).get(0);

        assertEquals(6, section.visibleEntries().size());
        assertTrue(section.hasMore);
    }

    private static void removesDuplicatePathsInsideEachSection() {
        List<FnosFileEntry> media = list(
                entry("First", "/same/path.mp4", "/poster/first.webp"),
                entry("Duplicate", "/same/path.mp4", "/poster/duplicate.webp"));

        HomePosterSection section = HomePosterSections.from(media, null, null).get(0);

        assertEquals(1, section.entries.size());
        assertEquals("First", section.entries.get(0).name);
    }

    private static void keepsMediaSectionWhenAllListsAreEmpty() {
        List<HomePosterSection> sections = HomePosterSections.from(null, null, null);

        assertEquals(1, sections.size());
        assertEquals("影视大全", sections.get(0).title);
        assertEquals(0, sections.get(0).entries.size());
    }

    private static List<FnosFileEntry> list(FnosFileEntry... entries) {
        List<FnosFileEntry> values = new ArrayList<FnosFileEntry>();
        for (int i = 0; i < entries.length; i++) {
            values.add(entries[i]);
        }
        return values;
    }

    private static FnosFileEntry entry(String name, String path, String posterPath) {
        return new FnosFileEntry(name, path, false, 0L, "Video", "", posterPath);
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
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: FAIL because `HomePosterSection` and `HomePosterSections` do not exist.

- [ ] **Step 3: Implement `HomePosterSection`**

Create `HomePosterSection`:

```java
package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class HomePosterSection {
    public final String title;
    public final String action;
    public final List<FnosFileEntry> entries;
    public final int maxVisible;
    public final boolean hasMore;

    HomePosterSection(String title, String action, List<FnosFileEntry> entries, int maxVisible) {
        this.title = title == null ? "" : title;
        this.action = action == null ? "" : action;
        this.entries = Collections.unmodifiableList(entries == null
                ? new ArrayList<FnosFileEntry>()
                : new ArrayList<FnosFileEntry>(entries));
        this.maxVisible = Math.max(1, maxVisible);
        this.hasMore = this.entries.size() > this.maxVisible;
    }

    public List<FnosFileEntry> visibleEntries() {
        int end = Math.min(entries.size(), maxVisible);
        return Collections.unmodifiableList(new ArrayList<FnosFileEntry>(entries.subList(0, end)));
    }
}
```

- [ ] **Step 4: Implement `HomePosterSections`**

Create `HomePosterSections` with bounded section defaults:

```java
package com.fnostv.android4.ui;

import com.fnostv.android4.net.FnosFileEntry;

import java.util.ArrayList;
import java.util.List;

public final class HomePosterSections {
    private static final int RECENT_LIMIT = 6;
    private static final int FAVORITE_LIMIT = 6;
    private static final int MEDIA_LIMIT = 8;

    private HomePosterSections() {
    }

    public static List<HomePosterSection> from(List<FnosFileEntry> media, List<FnosFileEntry> recent, List<FnosFileEntry> favorite) {
        List<HomePosterSection> sections = new ArrayList<HomePosterSection>();
        List<FnosFileEntry> recentEntries = clean(recent);
        if (recentEntries.size() > 0) {
            sections.add(new HomePosterSection("继续观看", NativeHomeView.ACTION_RECENT, recentEntries, RECENT_LIMIT));
        }
        List<FnosFileEntry> favoriteEntries = clean(favorite);
        if (favoriteEntries.size() > 0) {
            sections.add(new HomePosterSection("我的收藏", NativeHomeView.ACTION_FAVORITES, favoriteEntries, FAVORITE_LIMIT));
        }
        sections.add(new HomePosterSection("影视大全", NativeHomeView.ACTION_MEDIA, clean(media), MEDIA_LIMIT));
        return sections;
    }

    private static List<FnosFileEntry> clean(List<FnosFileEntry> entries) {
        List<FnosFileEntry> values = new ArrayList<FnosFileEntry>();
        if (entries == null) {
            return values;
        }
        for (int i = 0; i < entries.size(); i++) {
            FnosFileEntry entry = entries.get(i);
            if (entry == null || containsPath(values, entry.path)) {
                continue;
            }
            if (entry.isVideo() || (entry.directory && entry.posterPath.length() > 0)) {
                values.add(entry);
            }
        }
        return values;
    }

    private static boolean containsPath(List<FnosFileEntry> entries, String path) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).path.equals(path)) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 5: Run tests to verify green**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 2: Wire Home Sections Into MainActivity

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/MainActivity.java`
- Modify: `app/src/main/java/com/fnostv/android4/ui/NativeHomeView.java`

- [ ] **Step 1: Add failing compile-time usage**

Update `MainActivity.updateHomeCounts()` to call a new `nativeHomeView.updatePosterSections(...)` method after the existing `updatePosterCards(...)` call:

```java
nativeHomeView.updatePosterSections(HomePosterSections.from(known, recent, favorites));
```

Also update the REST success branch:

```java
nativeHomeView.updatePosterSections(HomePosterSections.from(mediaEntries, recentList.entries, favoriteList.entries));
```

Import `com.fnostv.android4.ui.HomePosterSections`.

- [ ] **Step 2: Run test to verify it fails**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: FAIL because `NativeHomeView.updatePosterSections(...)` does not exist.

- [ ] **Step 3: Add a no-op `NativeHomeView.updatePosterSections(...)`**

Add the method first with storage only:

```java
private List<HomePosterSection> posterSections = new ArrayList<HomePosterSection>();

public void updatePosterSections(List<HomePosterSection> sections) {
    posterSections = sections == null ? new ArrayList<HomePosterSection>() : new ArrayList<HomePosterSection>(sections);
}
```

Add imports for `java.util.ArrayList` and `java.util.List` if not already present.

- [ ] **Step 4: Run tests to verify compile is green**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 3: Render Bounded Poster Sections

**Files:**
- Modify: `app/src/main/java/com/fnostv/android4/ui/NativeHomeView.java`

- [ ] **Step 1: Introduce a section container**

Add a field:

```java
private LinearLayout posterSectionContainer;
```

In `content()`, after the hero card, replace the fixed two-card row area with:

```java
posterSectionContainer = new LinearLayout(context);
posterSectionContainer.setOrientation(LinearLayout.VERTICAL);
content.addView(posterSectionContainer, rowParams(0, 0));
renderPosterSections();
```

- [ ] **Step 2: Implement `renderPosterSections()`**

Add:

```java
private void renderPosterSections() {
    if (posterSectionContainer == null) {
        return;
    }
    posterSectionContainer.removeAllViews();
    if (posterSections.size() == 0) {
        posterSectionContainer.addView(sectionLink("影视大全  ›", ACTION_MEDIA), rowParams(0, 12));
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(mediaCard("影视大全\n浏览媒体库", ACTION_MEDIA, dp(160), dp(235), false), new LinearLayout.LayoutParams(dp(160), dp(235)));
        posterSectionContainer.addView(row, rowParams(0, 0));
        return;
    }
    for (int i = 0; i < posterSections.size(); i++) {
        HomePosterSection section = posterSections.get(i);
        posterSectionContainer.addView(sectionLink(section.title + "  ›", section.action), rowParams(i == 0 ? 0 : 18, 12));
        posterSectionContainer.addView(sectionRow(section), rowParams(0, 0));
    }
}
```

- [ ] **Step 3: Implement `sectionRow(...)`**

Add:

```java
private View sectionRow(HomePosterSection section) {
    LinearLayout row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    List<FnosFileEntry> entries = section.visibleEntries();
    if (entries.size() == 0) {
        row.addView(mediaCard(section.title + "\n暂无内容", section.action, dp(160), dp(235), false), new LinearLayout.LayoutParams(dp(160), dp(235)));
        return row;
    }
    for (int i = 0; i < entries.size(); i++) {
        HomeMediaCard card = mediaCard(entries.get(i).name, section.action, dp(160), dp(235), false);
        card.setEntry(entries.get(i));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(160), dp(235));
        if (i > 0) {
            params.leftMargin = dp(16);
        }
        row.addView(card, params);
    }
    if (section.hasMore) {
        LinearLayout.LayoutParams moreParams = new LinearLayout.LayoutParams(dp(120), dp(235));
        moreParams.leftMargin = dp(16);
        row.addView(mediaCard("更多\n" + section.title, section.action, dp(120), dp(235), false), moreParams);
    }
    return row;
}
```

- [ ] **Step 4: Re-render on updates**

Change `updatePosterSections(...)` to call:

```java
renderPosterSections();
```

Keep `updatePosterCards(...)` during this slice so the hero, recent, and favorite fields continue working while the new section renderer matures.

- [ ] **Step 5: Run build verification**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 4: Documentation and Final Verification

**Files:**
- Modify: `README.md`
- Modify: `RELEASE_NOTES.md`

- [ ] **Step 1: Update documentation**

Add README capability bullets:

```markdown
- 首页海报墙支持继续观看、收藏和影视大全等分区展示，并限制每区渲染数量以照顾旧设备性能。
```

Add release note:

```markdown
- 首页海报墙分区：继续观看、收藏和影视大全按有限数量展示，避免旧设备一次渲染过多卡片。
```

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
git add app/src/main/java/com/fnostv/android4/MainActivity.java app/src/main/java/com/fnostv/android4/ui/NativeHomeView.java app/src/main/java/com/fnostv/android4/ui/HomePosterSection.java app/src/main/java/com/fnostv/android4/ui/HomePosterSections.java app/src/test/java/com/fnostv/android4/ui/HomePosterSectionsTest.java README.md RELEASE_NOTES.md docs/superpowers/specs/2026-05-17-home-poster-sections-design.md docs/superpowers/plans/2026-05-17-home-poster-sections.md
git commit -m "feat: add home poster sections"
git push origin main
```
