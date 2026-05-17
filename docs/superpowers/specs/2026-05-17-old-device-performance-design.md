# Old Device Performance Design

## Context

fnOSTV targets Android 4.2+ TV boxes where CPU, memory, and video decode support vary widely. Recent work added a native media detail page, playback source selection, and a more stable mediaCenter fallback path. The next safe improvement is to make the already-native flow feel lighter on older devices without changing the main navigation model.

## Goals

- Start common LAN MP4 playback faster on home networks.
- Keep high-risk formats conservative so old devices can still fall back to software-friendly playback.
- Reduce poster-wall memory pressure by decoding downloaded poster images with bounded sampling.
- Keep the change small, testable, and compatible with the current Java Android stack.

## Non-Goals

- No RecyclerView or full poster-wall virtualization migration in this slice.
- No new global settings page for performance modes.
- No change to the player engine fallback order.
- No remote API contract change.

## Design

### Playback Fast-Start Strategy

`PlaybackStrategy` will distinguish three common cases:

- LAN/localhost low-risk MP4: use a faster low-latency profile with lower startup cache and smaller probe/analyze values.
- Internet remote low-risk MP4: keep the stable 6000ms/3000ms cache profile.
- High-risk formats such as MKV, HEVC, 4K, or very large files: keep the fluent software-friendly profile even when the URL is on LAN.

This gives old boxes a quicker path for the common local NAS MP4 case while preserving the safety path for formats that commonly stress hardware decode.

### Poster Memory Protection

`PosterLoader` will stop decoding remote poster streams directly at their original dimensions. It will download the response bytes, read image bounds first, calculate a power-of-two sample size for a maximum poster decode target, and then decode the sampled bitmap.

The sample-size calculation will live in a small pure Java helper so the behavior can be covered by JVM unit tests without depending on Android bitmap internals.

## Success Criteria

- Unit tests prove LAN MP4 uses the fast-start cache/probe profile.
- Unit tests prove normal internet MP4 stays on the stable remote profile.
- Unit tests prove high-risk LAN media still uses the fluent software-friendly profile.
- Unit tests prove poster sample-size calculation keeps large poster images bounded and leaves small images untouched.
- Debug build still succeeds.

