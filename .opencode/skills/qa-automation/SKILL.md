# Skill: qa-automation

> Mirrors `.gemini/rules/model_delegation.md` + `.gemini/rules/rules.md` for Muse Spark (opencode). Keep `.gemini/` for backward compat; this skill is the source for `opencode.json` delegates.

## Shortcuts (zero-delay)

| Shortcut | Agent | Model | Use |
|---|---|---|---|
| `/quick <task>` | `@quick-task` | `flash_lite` | Git ops, reads, greps, minor edits, status checks |
| `/standard <task>` | `@standard-dev` | `flash` | Compose UI, unit tests, bug fixes, build analysis |
| `/hard-fix <task>` | `@complex-architect` | `pro` | Audio DSP, architecture, memory leak, security |

## Workflow (6-step qc/reports)

1. Read `qc/QC_SUMMARY.md:1-30` top — `Next Actions` (queue, ONLY OPEN) + `Bug Status` (skip FIXED) + `Last updated` vs `git log --oneline qc/QC_SUMMARY.md` (loop prevention, `qc/QC_SUMMARY.md:1` singleton, no datestamp).
2. Check `qc_plan.md` + `qc/checklists/*` for gates & device matrix (Redmi `hm5xr8gueiz5x4c6`, ADVAN `A1013A5320TH000257`).
3. Execute task (code, tests, docs).
4. Run `./gradlew testDebugUnitTest` (reports `qc/reports/tests/`) + `./gradlew lintDebug` (0 errors, `qc/reports/lint/`) + `./gradlew verifyRoborazziDebug` (outputs `qc/reports/roborazzi/`, reference `app/src/test/screenshots/`).
5. Run `scripts/check_requirements.sh` (also checks `scrcpy` soft WARN) and `adb` smoke if needed (`qc/checklists/smoke.md`).
6. Update `qc/QC_SUMMARY.md` (add `Run YYYY-MM-DD` log, move `Bug Status` to FIXED, clear `Next Actions` queue) + `qc/changelogs/` if release.

## Rules

- Before ANY QC task, read `qc/QC_SUMMARY.md:1-30` queue vs log split (never act on FIXED/history rows).
- Task logging: `.ai-agent-task/` is gitignored ephemeral; do NOT use `.plan/` (Jules only) — use `qc/` canon (see `AGENTS.md:§3.3`).
- Reports gitignored: `qc/reports/`, `qc/artifacts/`, `qc/traces/`; tracked: `qc/changelogs/*.md`, `qc/fixtures/*`, `qc/checklists/*.md`, `qc/QC_SUMMARY.md`.
