# Skill: qa-automation

> Mirrors `.gemini/rules/model_delegation.md` + `.gemini/rules/rules.md` for Muse Spark (opencode). Keep `.gemini/` for backward compat; this skill is the source for `opencode.json` delegates.

## Shortcuts (zero-delay)

| Shortcut | Agent | Model | Use |
|---|---|---|---|
| `/quick <task>` | `@quick-task` | `flash_lite` | Git ops, reads, greps, minor edits, status checks |
| `/standard <task>` | `@standard-dev` | `flash` | Compose UI, unit tests, bug fixes, build analysis |
| `/hard-fix <task>` | `@complex-architect` | `pro` | Audio DSP, architecture, memory leak, security |

## Workflow (7-step qc/reports — EQ always tunable)

0. **Device prep (before any dump/screencap/swipe/click after pm clear/install -r):** `bash scripts/device_prep.sh hm5xr8gueiz5x4c6 .build-outputs/app-playstore-debug.apk` + `bash scripts/device_prep.sh A1013A5320TH000257 .build-outputs/app-playstore-debug.apk` — Q17 `pm grant POST_NOTIFICATIONS` silent + Q18 `run-as has_seen_onboarding=true` (fallback tap `GET STARTED`) — prevents `Allow`/`GET STARTED` blocking `uiautomator dump`/`screencap` (see `AGENTS.md:§3.2 D`).
1. Read `qc/QC_SUMMARY.md:1-30` top — `Next Actions` (queue, ONLY OPEN) + `Bug Status` (skip FIXED) + `Last updated` vs `git log --oneline qc/QC_SUMMARY.md` (loop prevention, `qc/QC_SUMMARY.md:1` singleton, no datestamp).
2. Check `qc_plan.md` + `qc/checklists/*` for gates & device matrix (Redmi `hm5xr8gueiz5x4c6`, ADVAN `A1013A5320TH000257`).
3. Execute task (code, tests, docs).
4. Run `./gradlew testDebugUnitTest` (reports `qc/reports/tests/`) — must include `EqualizerComponent +/-` `performClick` → `Text +1dB` + `_eqBands` Flow + `equalizer?.setBandLevel` (booster OFF/ON, Flat→Custom, both devices) — EQ is always tunable (`MainActivity.kt:3088` + `AudioEffectManager.kt:464`).
5. Run `./gradlew lintDebug` (0 errors, `qc/reports/lint/`) + `./gradlew verifyRoborazziDebug` (outputs `qc/reports/roborazzi/`, reference `app/src/test/screenshots/`).
6. Run `scripts/check_requirements.sh` (also checks `scrcpy` soft WARN) and `adb` smoke — verify EQ `+/-` on both devices (`screencap qc/artifacts/screenshots/manual/eq-*.png`, Flat→Custom and Custom, booster OFF/ON) — EQ always tunable, Q1.
7. Update `qc/QC_SUMMARY.md` (add `Run YYYY-MM-DD` log, move `Bug Status` to FIXED, clear `Next Actions` queue) + `qc/changelogs/` if release.

## Rules

- Before ANY QC task, read `qc/QC_SUMMARY.md:1-30` queue vs log split (never act on FIXED/history rows).
- Task logging: `.ai-agent-task/` is gitignored ephemeral; do NOT use `.plan/` (Jules only) — use `qc/` canon (see `AGENTS.md:§3.3`).
- Reports gitignored: `qc/reports/`, `qc/artifacts/`, `qc/traces/`; tracked: `qc/changelogs/*.md`, `qc/fixtures/*`, `qc/checklists/*.md`, `qc/QC_SUMMARY.md`.
