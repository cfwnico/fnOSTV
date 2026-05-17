# Video Player Number Seek Design

## Goal

Add low-risk remote shortcuts to the native video player so number keys can jump to common playback positions without changing existing directional, menu, speed, or quality behavior.

## Scope

This design covers playback-page number shortcuts only:

- `0` jumps to the start of the video.
- `1` through `9` jump to 10% through 90% of the video.
- Direction keys keep their existing meanings.
- Media rewind and fast-forward keep their existing large seek steps.
- Menu keeps the existing speed-cycle behavior.
- Up and down keep the existing picture-mode and quality-cycle behavior.

The change does not add a new on-screen shortcut guide, modal, or settings toggle.

## Architecture

Add a small pure Java helper under the player package to map Android key codes and durations to seek targets. Keeping this calculation outside `NativeVideoPlayerView` makes it easy to test without Android view instrumentation and keeps the view focused on UI and player calls.

`NativeVideoPlayerView.onKeyDown(...)` checks the helper before the existing directional controls. If the key is a supported number and the player has a positive duration, the view requests a seek to the calculated target, shows a short hint, and returns `true`. If duration is unavailable, it shows the same seek fallback hint already used by failed seek operations.

## Behavior

Number shortcut mapping:

| Key | Target |
| --- | --- |
| `0` | `0 ms` |
| `1` | `duration * 10%` |
| `2` | `duration * 20%` |
| `3` | `duration * 30%` |
| `4` | `duration * 40%` |
| `5` | `duration * 50%` |
| `6` | `duration * 60%` |
| `7` | `duration * 70%` |
| `8` | `duration * 80%` |
| `9` | `duration * 90%` |

Hints:

- `0`: `回到开头`
- `1` through `9`: `跳转 10%` through `跳转 90%`
- unknown duration: `暂时无法跳转`

Targets are clamped to the media duration to avoid overshooting due to integer math.

## Testing

Unit tests cover the helper directly:

- supported number keys map to the expected percent.
- unsupported keys are ignored.
- target calculation returns `-1` when duration is not positive.
- target calculation clamps to the duration.
- hint text matches the user-facing strings.

Existing debug unit tests and debug build remain the final verification gates.
