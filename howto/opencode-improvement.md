# OpenCode Reply Readability — What Changed (Q1-A Style-Only)

Web article: why opencode replies were messy, what changed, before/after examples.

## 1. Problem

Replies pasted full gradle/logcat walls inline, no TL;DR, inconsistent `path:line` refs.
Only `Q#` labeling was enforced (`AGENTS.md §6`, `opencode.json question_labeling`,
`qa-automation SKILL.md`, `.gemini/rules`). No verbosity cap, template, or fence rule.

### Before example — prompt vs reply

Prompt:

```text
is this app ready for public release?
```

Reply (before — messy wall, ~600 words, no TL;DR, log dump inline):

```text
Plan — Public Release Gaps R1 Signing + R2 keep 0.1.25 26090505 + R3 ... (450-word single paragraph, then full task list, then 5 tables, then 200-line gradle log pasted inline, then uiautomator dump inline, no headings, inconsistent qc/reports/ pointers)...
> Task :app:mergeDebugGlobalSynthetics UP-TO-DATE ... BUILD SUCCESSFUL in 28s 43 actionable tasks ...
<node index=0 text="MILKYS APP" bounds=... /> ...
```

Issues: no summary first, log in chat, no `file:line`, mixed `Q17/Q18` with `Q1`.

## 2. Fix (Q1-A style-only)

Added `opencode.json instructions` + one-line reference from `qa-automation SKILL.md Rules`.
No Q# rename, no Grav trim, no table alignment in this pass.

### File: `opencode.json` — original

```json
  "question_labeling": {
    "per_message_reset": "Q1-A",
    "multi_choice_only": "Q2-B",
    "hybrid": "Q1-C",
    "rule": "Every assistant turn MUST label Q1, Q2, Q3 per message reset. Only multi-choice get Q1-A, Q1-B. Binary may be plain Confirm ...? Yes without Q#. Never walls — prefer Q#-A/B/C blocks. Allows Q1-B, Q2-A or Yes.",
    "never_walls": true
  }
```

### File: `opencode.json` — new (added `instructions`)

```json
  "question_labeling": {
    "per_message_reset": "Q1-A",
    "multi_choice_only": "Q2-B",
    "hybrid": "Q1-C",
    "rule": "Every assistant turn MUST label Q1, Q2, Q3 per message reset. Only multi-choice get Q1-A, Q1-B. Binary may be plain Confirm ...? Yes without Q#. Never walls — prefer Q#-A/B/C blocks. Allows Q1-B, Q2-A or Yes.",
    "never_walls": true
  },
  "instructions": "Reply style: TL;DR first (max 3 lines). Keep status replies under ~300 words; details go in tables, files, or qc/ reports, not walls. Structure: ## Summary / ## Changes (file:line) / ## Verify / ## Next. Use markdown tables over prose, fenced code with language (bash/kotlin), absolute paths with :line. Never paste full gradle/logcat diffs in chat — point to qc/reports/ or qc/artifacts/. One idea per bullet. Follow Q# labeling for questions."
```

### File: `.opencode/skills/qa-automation/SKILL.md` — original (`## Rules`)

```md
## Rules

- Before ANY QC task, read `qc/QC_SUMMARY.md:1-30` queue vs log split (never act on FIXED/history rows).
- Task logging: `.ai-agent-task/` is gitignored ephemeral; do NOT use `.plan/` (Jules only) — use `qc/` canon (see `AGENTS.md:§3.3`).
- Reports gitignored: `qc/reports/`, `qc/artifacts/`, `qc/traces/`; tracked: `qc/changelogs/*.md`, `qc/fixtures/*`, `qc/checklists/*.md`, `qc/QC_SUMMARY.md`.
```

### File: `.opencode/skills/qa-automation/SKILL.md` — new (added 1 bullet)

```md
## Rules

- Before ANY QC task, read `qc/QC_SUMMARY.md:1-30` queue vs log split (never act on FIXED/history rows).
- Task logging: `.ai-agent-task/` is gitignored ephemeral; do NOT use `.plan/` (Jules only) — use `qc/` canon (see `AGENTS.md:§3.3`).
- Reports gitignored: `qc/reports/`, `qc/artifacts/`, `qc/traces/`; tracked: `qc/changelogs/*.md`, `qc/fixtures/*`, `qc/checklists/*.md`, `qc/QC_SUMMARY.md`.
- Reply style: see `opencode.json instructions` — TL;DR first, ~300-word cap, `## Summary / Changes / Verify / Next` template, tables over walls, fenced code, `path:line` refs, logs to `qc/` not chat.
```

### After example — same prompt, new style

Prompt:

```text
is this app ready for public release?
```

Reply (after — TL;DR + table, ~120 words):

```md
## Summary
Not yet — 4 P0 blockers (signing .aab, 1.0.0, prod AdMob, store assets). QA green.

## Changes
| # | Item | File:line |
|---|---|---|
| R1 | Signing via secrets | `app/build.gradle.kts:106` |

## Verify
`lint 0e / test 20/20 / verify 6/6` → `qc/reports/`

## Next
Q1: Which P0 first?
  Q1-A: Signing aab
  Q1-B: Store assets
```

## 3. Files changed

| File | Change |
|---|---|
| `opencode.json` | + `instructions` string (style caps, template, fences, `path:line`) |
| `.opencode/skills/qa-automation/SKILL.md` | + 1 bullet referencing `opencode.json instructions` |
| `howto/opencode-improvement.md` | New (this article) |

Out of scope (Q1-A): Q# rename, Grav wall trim, shortcut table alignment.
