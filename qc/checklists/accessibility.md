# Accessibility Checklist — `AGENTS.md` §3.2 B

- [ ] All `Icon` have `contentDescription = stringResource(R.string.*)` or `null` decorative.
- [ ] All `IconButton`/`Surface` have `Modifier.defaultMinSize(48.dp)` (audit `HearingWarningCard.kt:43` close button).
- [ ] No `Modifier.height(...)` on text containers; use `heightIn`/`wrapContent`.
- [ ] `lintDebug` with `accessibility` filter: 0 errors (`./gradlew lintDebug` → `qc/reports/lint/`).
- [ ] Font scale 0.85x / 1.0x / 1.3x / 2.0x — no clipping, EQ sliders visible, wrapping correct (spot-check on Redmi via Settings or `bash scripts/qc_redmi_matrix.sh`, see `qc_plan.md:§5.7` — 6 combos).

## Redmi Display / Font Matrix (see `qc_plan.md:§5.7`, `qc/checklists/smoke.md`)

- [ ] 6-combo Settings sweep (Small/Default/Largest × 0.85x/1.0x/1.3x/2.0x) on Redmi `hm5xr8gueiz5x4c6` — verify scrollability, 5-band sliders, HearingWarningCard, no truncation, 48dp targets
- [ ] Evidence PNGs in `qc/artifacts/screenshots/manual/redmi-*.png` (ephemeral, gitignored) + optional video `scrcpy --record /tmp/qc-redmi-matrix-*.mp4`
