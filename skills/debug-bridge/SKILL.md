# Skill: debug-bridge

> Tap-free `adb` control + hardware-state reads for Milkys Sound Booster debug
> builds. Full reference: `howto/debug_bridge.md`. DEBUG builds only — no effect
> in release (receiver returns early when `BuildConfig.DEBUG` is false).

## One-liner

```bash
bash scripts/debug_cmd.sh <cmd> [args]          # SERIAL env or first device
bash scripts/debug_cmd.sh dump_state            # state + hardware readback
```

## Command table (single action `com.milkys.soundbooster.DEBUG`)

| Command | Args | Effect |
|---|---|---|
| `booster_on` / `booster_off` / `booster_toggle` | — | Booster + foreground service |
| `level` | `30` (0–100) | `setBoostProgress` |
| `preset` | `"Rock"` | `applyPreset` (sync hardware write) |
| `set_eq` | `true` / `false` | `setEqEnabled` (booster-gated) |
| `set_band` | `<0-4> <dB>` | `setBandLevel` (marks Custom) |
| `overlay_show` / `overlay_hide` / `overlay_toggle` | — | Floating widget + service |
| `overlay_power_on` / `off` / `toggle` | — | Overlay ⏻ path |
| `dump_state` | — | State + hardware readback |

Pass args **split** (`scripts/debug_cmd.sh set_eq true`), never quoted as one
(a single `"set_eq true"` arg matches no command and silently no-ops).

## Hard rules

- **Explicit `-n` is mandatory.** Android 8+ silently drops implicit
  (action-only) broadcasts to manifest receivers. Always address
  `-n com.milkys.soundbooster/.ui.DebugCommandReceiver` (the script does this).
- **State ≠ hardware is the bug class.** `dump_state` prints flow state plus
  `hwEqOn/hwEnhOn/hwBandsMb/trackNull/playing`. If `isEqEnabled=true` but
  `hwEqOn=false` (or `hwBandsMb` shows stale millibels), the write path
  diverged. Post sync-write fix, consecutive dumps must agree with zero lag.
- EQ writes are synchronous on the caller thread; OFF was always sync, ON now
  is too — rapid on/off/on must show state==hardware on every immediate dump.

## Pointers

- Receiver: `app/src/main/java/com/milkys/soundbooster/ui/DebugCommandReceiver.kt`
- Hardware probe: `AudioEffectManager.hardwareSnapshot()`
- Flow logs: `ui/DebugLogBridge.kt` + `scripts/log_tail.sh`
- Distinguishing same-sha rebuilds: DEBUG header `GIT_SHA.BUILD_MOMENT`
  (commit time, deterministic per HEAD — wall clock broke Roborazzi verify)
