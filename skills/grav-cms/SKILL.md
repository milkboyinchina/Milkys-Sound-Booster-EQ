# Skill: grav-cms

> Grav CMS edits over MCP (`grav-milkboy_my_id`, configured in
> `~/.config/opencode/opencode.jsonc`). Full annex workflow background:
> `qc/fixtures/privacy/general-app-privacy-annex-milkys.md`.

## Connection (hard rules)

- `GRAV_API_URL` **must end in `/api`** (e.g. `https://milkboy.my.id/api`).
  Without the suffix every call — even `get_system_info` — fails with
  `Resource not found`. This was the actual outage cause once; check this first.
- Key needs `api.pages.read` for reads, `api.pages.write` for `update_page`.
  Verify with `get_system_info` (expect `grav_version`, API plugin enabled).

## Page workflow

- Routes: privacy page is `/term/general-app-privacy` (`en` + `id`
  translations — reviewers check every locale you ship, update both).
- Read-modify-write: `get_page({route})` → append → `update_page({route,
  content, lang})`. `update_page` replaces `content` wholesale; header fields
  deep-merge. Always re-read first, never compose from memory.
- **Idempotency guard:** skip when the marker (e.g. `## 8.`) is already
  present — required before every write.
- Verify after every edit with `webfetch` on the public URL (rendered page,
  not just the API echo).

## Content rules (public page — Play reviewers read it)

- Publish only user-facing disclosure: package, on-device processing,
  per-permission justification, AdMob/UMP behavior, consent controls, contact.
- **Never publish internal dev details:** test AdMob IDs, test device hashes,
  testTags, file/line refs, build SHAs, QA device serials. The local fixture
  mixes both — treat it as source material, not paste-ready copy.
- Consent/off-by-default claims must match the shipped default
  (`KEY_ADS_ENABLED`). If the default flips, the page flips in the same batch
  **before** that AAB reaches review.

## Pointers

- Fixture (stale-prone, verify against code): `qc/fixtures/privacy/general-app-privacy-annex-milkys.md`
- Canonical URL: `https://milkboy.my.id/term/general-app-privacy`
- Related: `skills/debug-bridge/SKILL.md`, `howto/debug_bridge.md`
