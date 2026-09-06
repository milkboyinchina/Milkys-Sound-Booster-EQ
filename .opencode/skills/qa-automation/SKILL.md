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

## ❓ Global Question Labeling (Q# — Mandatory per A16 Hybrid Q1-C)

* **Per-message reset (Q1-A):** Every assistant turn that asks questions MUST label them `Q1`, `Q2`, `Q3` in order starting at `1` for that message. Do not carry numbers across turns.
* **Multi-choice only (Q2-B):** Only questions with multiple choice answers get sub-labels `Q1-A`, `Q1-B`, `Q1-C` under that `Q#`. Single yes/no confirms may be asked directly in plain text `Confirm ...? Yes` without `Q#` prefix (binary exception, Proposal) — otherwise stay as `Q1`/`Q2` without sub-labels.
* **Never walls (Proposal):** Never write open-ended walls of text when a structured `Q1-A/B/C` choice block is possible — always prefer `Q#-A/B/C` for tradeoffs. Allows deterministic reply `Q1-B, Q2-A` or `Yes`.

## Grav MCP — https://learn.getgrav.org/20/advanced/mcp-server

* **Purpose:** Grav MCP Server (`grav-mcp` npm, Node.js 18+) exposes Grav 2.0 via Model Context Protocol to AI clients (Claude, Cursor). Not a Grav plugin — standalone `npx grav-mcp` that talks to site over HTTPS ` /api/v1` via API Plugin. Admin2 for humans, MCP for AI, same API.
* **Requirements:** Grav 2.0 site with API Plugin enabled + `GRAV_API_URL=https://mysite.com/api` + `GRAV_API_KEY=grav_...` (generate: `bin/plugin api keys:generate --user=admin --name="Claude MCP"`). Test: `GRAV_API_URL=https://milkboy.my.id/api GRAV_API_KEY=grav_... npx grav-mcp`.
* **This Project (Android, not Grav):** Local test `grav_discover_plugins` → `0` `grav_get_system_info` → `Resource not found` confirms no local Grav (expected — this repo is `Milkys-Sound-Booster-EQ` Android, not a Grav site). For `https://milkboy.my.id/term/general-app-privacy` use remote Grav API via MCP with `GRAV_API_URL=https://milkboy.my.id/api` + dedicated least-privilege user `api.pages.write` (Q1-B annex). Annex prepared `qc/fixtures/privacy/general-app-privacy-annex-milkys.md` for manual `Grav Admin → term → general-app-privacy Section 8` if MCP 404 (remote `grav_list_languages 404` without key).
* **Tools (70 across 11 domains):** `list_pages/get_page/create_page/update_page/delete_page`, `list_languages/get_page_translations/create_translation`, `list_page_media/upload_page_media`, `get_config/update_config` (ETag `If-Match`), `manage_api_keys`, `check_updates/install_package`, `get_system_info/clear_cache`, `list_webhooks`, etc. Resources `grav://system/info, grav://languages` + Prompts `create_blog_post, translate_page, site_health_check`.
