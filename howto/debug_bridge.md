# Debug Command Bridge (`scripts/debug_cmd.sh`)

Controls the **debug** build from `adb` without tapping the UI — state reads,
repro sequences, and screenshot-free verification. No effect in release builds
(receiver returns early when `BuildConfig.DEBUG` is false).

## One-liner

```bash
bash scripts/debug_cmd.sh <cmd> [args]          # SERIAL env or first device
bash scripts/debug_cmd.sh dump_state            # state + hardware readback
adb shell logcat -d | grep -a "V DebugCommand"  # same output, manual form
```

## Commands (single action `com.milkys.soundbooster.DEBUG`)

| Command | Args | Effect (mirrors dashboard paths) |
|---|---|---|
| `booster_on` / `booster_off` / `booster_toggle` | — | `setBoostEnabled` + foreground service start/stop |
| `level` | `30` (0–100) | `setBoostProgress` |
| `preset` | `"Rock"` | `applyPreset` (bands hit hardware synchronously) |
| `set_eq` | `true` / `false` | `setEqEnabled` (rejected with toast path when booster OFF) |
| `set_band` | `<band 0-4> <dB -15..15>` | `setBandLevel` (marks preset Custom) |
| `overlay_show` / `overlay_hide` / `overlay_toggle` | — | `setFloatingEnabled` + service lifecycle |
| `overlay_power_on` / `overlay_power_off` / `overlay_power_toggle` | — | Overlay ⏻ path incl. service wiring |
| `dump_state` | — | Logs `isBoosted/progress/isEqEnabled/bands/preset/favorites/isFloating` **plus hardware readback** (`hwEqOn/hwEnhOn/hwBandsMb/trackNull/playing`) |

## Raw form (no script)

```bash
adb shell am broadcast -n com.milkys.soundbooster/.ui.DebugCommandReceiver \
  -a com.milkys.soundbooster.DEBUG --es cmd dump_state
```

The explicit `-n` is mandatory: Android 8+ silently drops implicit
(action-only) broadcasts to manifest receivers, so `-a ...` alone never arrives.

## Reading hardware state

`dump_state` prints one `V DebugCommand` line, e.g.:

```
isBoosted=true progress=20 isEqEnabled=true bands=4, 2, -1, 2, 5 preset=Rock \
favorites=[...] isFloating=false hwEqOn=true hwEnhOn=true \
hwBandsMb=[400, 200, -100, 200, 500] trackNull=false playing=true
```

Rule of thumb: **state ≠ hardware is the bug class** — if `isEqEnabled=true`
but `hwEqOn=false` (or `hwBandsMb` shows stale millibels), the write path
diverged. Since the Q1-A sync-write fix, all EQ writes apply on the caller
thread, so consecutive dumps must agree with zero lag window.

## Related

- Receiver: `app/src/main/java/com/milkys/soundbooster/ui/DebugCommandReceiver.kt`
- Hardware probe: `AudioEffectManager.hardwareSnapshot()`
- Verbose flow logs: `app/src/main/java/com/milkys/soundbooster/ui/DebugLogBridge.kt` + `scripts/log_tail.sh`
- Distinguishing same-sha rebuilds on device: DEBUG header shows
  `GIT_SHA.BUILD_MOMENT` (commit time, deterministic per HEAD)
