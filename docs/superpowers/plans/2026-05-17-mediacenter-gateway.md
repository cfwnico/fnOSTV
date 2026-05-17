# MediaCenter Gateway Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract media center loading fallback decisions into a tested gateway and wire `MainActivity` to use it.

**Architecture:** Add a pure `MediaCenterGateway` in the `media` package with provider interfaces for REST, local index, RPC, and file fallback. `MainActivity` supplies real providers, while unit tests supply fake providers.

**Tech Stack:** Java, Android SDK 28, Gradle 6.7.1, existing fnOSTV `FnosFileList` and `FnosFileEntry` models.

---

## Tasks

- [ ] Add `MediaCenterGatewayTest` that exercises fallback order and failure traces.
- [ ] Implement `MediaCenterGateway` and `MediaCenterLoad`.
- [ ] Wire `MainActivity.loadMediaCenter` through the gateway.
- [ ] Update README / RELEASE_NOTES to mention stable media center fallback.
- [ ] Run final verification: conflict marker scan, `git diff --check`, `gradle --no-daemon testDebugUnitTest`, and `scripts\build-debug.cmd`.

## Key Files

- Create `app/src/main/java/com/fnostv/android4/media/MediaCenterGateway.java`
- Create `app/src/main/java/com/fnostv/android4/media/MediaCenterLoad.java`
- Create `app/src/test/java/com/fnostv/android4/media/MediaCenterGatewayTest.java`
- Modify `app/src/main/java/com/fnostv/android4/MainActivity.java`
- Modify `README.md`
- Modify `RELEASE_NOTES.md`

## Self-Review

- Spec coverage: root fallback, child fallback, subtitles, failure traces, tests, and build verification are covered.
- Completeness check: each file and verification command is listed.
- Type consistency: the plan uses `MediaCenterGateway` and `MediaCenterLoad` consistently.
