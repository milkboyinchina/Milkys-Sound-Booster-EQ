# Antigravity Workspace Rule: Automatic Model Delegation & Zero-Delay Shortcuts

This rule governs model tier selection and subagent orchestration across all tasks in this workspace.

---

## ⚡ 1. Zero-Delay Execution Shortcuts (Immediate Routing)

When the user starts a prompt with a slash shortcut, **bypass evaluation overhead completely** and instantly invoke the corresponding subagent profile:

| Shortcut | Trigger Subagent Profile | Target Model Tier | Primary Use Case |
|---|---|---|---|
| `/quick <task>` | `@quick-task` | `flash_lite` (Low) | Git operations (`git status`, `git commit`, `git push`), file reads, quick greps, minor formatting, status checks. |
| `/standard <task>` | `@standard-dev` | `flash` (Medium) | Standard feature implementation, UI updates, unit test creation, build log analysis. |
| `/hard-fix <task>` | `@complex-architect` | `pro` (High) | Multi-file architectural refactoring, deep debugging, complex algorithm design, security audits. |

---

## 🎯 2. Automatic Complexity Classifier (When No Shortcut Specified)

Evaluate incoming prompt requirements against the following complexity thresholds:

### Tier 1: Low Model (`flash_lite`)
* **Triggers**:
  - All Git terminal operations (`git status`, `git add`, `git commit`, `git push`, `git pull`, `git diff`, `git log`, `git checkout`).
  - File reading, directory listing, single-file minor text edits.
  - Pattern searching (`grep_search`), dependency checks, version checks.
  - Script execution (`build.sh`, helper utilities).

### Tier 2: Medium Model (`flash`)
* **Triggers**:
  - Writing or updating unit tests and widget previews.
  - Single-screen Compose UI tweaks and minor component updates.
  - Inspecting and resolving standard runtime exception logs.
  - Refactoring small helper classes or state flows.

### Tier 3: High Model (`pro`)
* **Triggers**:
  - Cross-module or multi-file architectural redesign.
  - Complex bug investigations involving background services, audio engines, or IPC.
  - Security audits, memory leak debugging, or R8/ProGuard configuration overhauls.

---

## 🤖 3. Subagent Profile Definitions

Use the following parameters when invoking subagents via `invoke_subagent`:

```json
{
  "Subagents": [
    {
      "TypeName": "self",
      "Role": "Quick Task Runner (@quick-task)",
      "Model": "flash_lite",
      "Prompt": "<task description>"
    }
  ]
}
```

* **`@quick-task`**: `Model`: `"flash_lite"`, `Role`: `"Quick Task Runner"`
* **`@standard-dev`**: `Model`: `"flash"`, `Role`: `"Standard Developer"`
* **`@complex-architect`**: `Model`: `"pro"`, `Role`: `"Complex Architect"`

---

## 🌐 4. Cross-Project Portability

This rule is fully portable across all projects (e.g. `Milkys-Sound-Booster-EQ`, `Grav AI Chatbot`, `grav-lamp-docker`). 
To apply to any project directory:
Copy `.gemini/rules/model_delegation.md` into the target workspace root directory.
