# Rules: Delegation & Logging

## ⚡ Shortcuts & Delegation
- `/quick <task>` → `@quick-task` (`flash_lite`): Git ops, reads, greps, minor edits, status checks.
- `/standard <task>` → `@standard-dev` (`flash`): Features, Compose/UI tweaks, unit tests, build log checks.
- `/hard-fix-sonnet <task>` or `/hard-fix <task>` → `@complex-architect` (`pro` / Sonnet 4.6): Multi-file refactor, audio DSP, services.
  - Workflow: Read `qc/QC_SUMMARY.md:1-30` (queue vs log) → Check `qc_plan.md` + `qc/checklists/*` → Execute → Run `./gradlew testDebugUnitTest` (`qc/reports/tests/`), `./gradlew lintDebug` (`qc/reports/lint/`), `./gradlew verifyRoborazziDebug` (`qc/reports/roborazzi/`), then update `qc/QC_SUMMARY.md` + `qc/changelogs/` if release. Mirrored in `.opencode/skills/qa-automation/SKILL.md:1` (keep `.gemini/` for compat).

## 🎯 Auto Classifier (Default)
- **T1 (`flash_lite`)**: Git, file reads/greps, minor edits, script runs.
- **T2 (`flash`)**: Unit tests, UI tweaks, small refactors, bug log fixes.
- **T3 (`pro`)**: Architecture redesign, audio DSP/Service bugs, security/memory leak audits.

## 🤖 Subagent Specs
- `@quick-task`: `Model: "flash_lite"`, `Role: "Quick Task Runner"`
- `@standard-dev`: `Model: "flash"`, `Role: "Standard Developer"`
- `@complex-architect`: `Model: "pro"`, `Role: "Complex Architect"`

## 📄 Task Logging (`.ai-agent-task/`) + QC Summary
- Format: `YYMMDD-HHMMSS-<type>.md` (`plan` | `report` | `error`). Do NOT touch `.plan/` (Jules only).
- Lifecycle: Create `plan.md` (>2 files or DSP logic) → Rename to `report.md` on completion → Create `error.md` on failure/cancellation.
- **Before ANY QC task:** Read `qc/QC_SUMMARY.md:1-30` top — `Next Actions` (queue, ONLY OPEN) + `Bug Status` (skip FIXED) + `Last updated` vs `git log --oneline qc/QC_SUMMARY.md`. Treat `Run` sections as log, not queue. Loop prevention: never act on FIXED/history rows.
- **Before ANY device dump/screencap/swipe/click after pm clear/install -r (hm5xr8gueiz5x4c6 + A1013A5320TH000257):** `bash scripts/device_prep.sh hm5xr8gueiz5x4c6 .build-outputs/app-playstore-debug.apk` — Q17 `pm grant POST_NOTIFICATIONS` silent + Q18 `run-as has_seen_onboarding=true` (fallback tap `GET STARTED`) — prevents `Allow`/`GET STARTED` blocking `uiautomator dump`/`screencap` (see `AGENTS.md:§3.2 D`).
