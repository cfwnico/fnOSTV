# First Configuration Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let first-time users save server/account settings and continue directly into app login without an extra settings-screen exit.

**Architecture:** Add a pure `SettingsCompletionFlow` decision helper, wire it into `SettingsActivity`, and document the setup behavior.

**Tech Stack:** Java, Android API 17-compatible code, Gradle unit tests, Android debug build.

---

### Task 1: First Configuration Completion Decision

**Files:**
- Create: `app/src/main/java/com/fnostv/android4/ui/SettingsCompletionFlow.java`
- Create: `app/src/test/java/com/fnostv/android4/ui/SettingsCompletionFlowTest.java`
- Modify: `app/src/main/java/com/fnostv/android4/SettingsActivity.java`

- [x] **Step 1: Write the failing tests**

Add tests for first configuration finishing after a valid profile, existing profile edits staying in settings, and invalid profile saves keeping the account editor open.

- [x] **Step 2: Run test to verify it fails**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: FAIL because `SettingsCompletionFlow` does not exist.

- [x] **Step 3: Implement the helper and activity wiring**

Add `SettingsCompletionFlow.afterAccountSave(...)` and call it after `ProfileStore.save(profile)` in `SettingsActivity.onSaveRequested(...)`.

- [x] **Step 4: Run test to verify it passes**

Run: `. .\scripts\env.ps1; gradle --no-daemon testDebugUnitTest`

Expected: PASS.

### Task 2: Documentation and Verification

**Files:**
- Modify: `README.md`
- Modify: `RELEASE_NOTES.md`

- [x] **Step 1: Document first configuration behavior**

Mention that first valid configuration now returns to the app and starts the native login flow.

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
git add app/src/main/java/com/fnostv/android4/SettingsActivity.java app/src/main/java/com/fnostv/android4/ui/SettingsCompletionFlow.java app/src/test/java/com/fnostv/android4/ui/SettingsCompletionFlowTest.java README.md RELEASE_NOTES.md docs/superpowers/specs/2026-05-17-first-configuration-flow-design.md docs/superpowers/plans/2026-05-17-first-configuration-flow.md
git commit -m "feat: streamline first configuration"
git push origin main
```
