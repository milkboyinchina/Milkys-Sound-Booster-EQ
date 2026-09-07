#!/usr/bin/env python3
"""
Delegation Orchestrator Utility for Antigravity
Evaluates prompt complexity, handles /quick, /standard, /hard-fix shortcuts,
and formats subagent payloads across Low (muse-spark-1.3 free via opencode zen), Medium/High (muse-spark-1.3 contributor via opencode go) model tiers.
"""

import sys
import argparse
import json
import re

LOW_MODEL = "opencode-zen/muse-spark-1.3-free"
MEDIUM_MODEL = "opencode-go/muse-spark-1.3-contributor"
HIGH_MODEL = "opencode-go/muse-spark-1.3-contributor"

def classify_prompt(prompt_text):
    text = prompt_text.strip()

    # 1. Zero-Delay Slash Shortcuts
    if text.startswith("/quick"):
        task = re.sub(r"^/quick\s*", "", text)
        return {
            "tier": "Low",
            "model": LOW_MODEL,
            "profile": "@quick-task",
            "role": "Quick Task Runner",
            "shortcut": "/quick",
            "task": task or text
        }
    elif text.startswith("/standard"):
        task = re.sub(r"^/standard\s*", "", text)
        return {
            "tier": "Medium",
            "model": MEDIUM_MODEL,
            "profile": "@standard-dev",
            "role": "Standard Developer",
            "shortcut": "/standard",
            "task": task or text
        }
    elif text.startswith("/hard-fix-sonnet") or text.startswith("/hard-fix"):
        shortcut_name = "/hard-fix-sonnet" if text.startswith("/hard-fix-sonnet") else "/hard-fix"
        task = re.sub(r"^/(hard-fix-sonnet|hard-fix)\s*", "", text)
        return {
            "tier": "High",
            "model": HIGH_MODEL,
            "profile": "@complex-architect",
            "role": "Complex Architect (Claude Sonnet)",
            "shortcut": shortcut_name,
            "task": task or text
        }

    # 2. Heuristic Classifier based on keywords & actions
    lower_text = text.lower()
    git_keywords = ["git ", "commit", "push", "pull", "status", "checkout", "branch", "diff", "log"]
    low_keywords = ["read", "find", "grep", "search", "list", "format", "check", "version"]
    high_keywords = ["architecture", "refactor", "redesign", "memory leak", "security", "optimize", "deep debug"]

    if any(k in lower_text for k in git_keywords) or any(k in lower_text for k in low_keywords):
        return {
            "tier": "Low",
            "model": LOW_MODEL,
            "profile": "@quick-task",
            "role": "Quick Task Runner",
            "shortcut": None,
            "task": text
        }
    elif any(k in lower_text for k in high_keywords):
        return {
            "tier": "High",
            "model": HIGH_MODEL,
            "profile": "@complex-architect",
            "role": "Complex Architect",
            "shortcut": None,
            "task": text
        }
    else:
        return {
            "tier": "Medium",
            "model": MEDIUM_MODEL,
            "profile": "@standard-dev",
            "role": "Standard Developer",
            "shortcut": None,
            "task": text
        }

def format_subagent_payload(classification):
    return {
        "Subagents": [
            {
                "TypeName": "self",
                "Role": f"{classification['role']} ({classification['profile']})",
                "Model": classification["model"],
                "Prompt": classification["task"]
            }
        ]
    }

def run_tests():
    test_cases = [
        ("/quick git push", LOW_MODEL, "/quick"),
        ("/hard-fix refactor audio engine architecture", HIGH_MODEL, "/hard-fix"),
        ("/hard-fix-sonnet refactor audio engine", HIGH_MODEL, "/hard-fix-sonnet"),
        ("/standard update button style", MEDIUM_MODEL, "/standard"),
        ("git status", LOW_MODEL, None),
        ("implement new equalizer preset screen", MEDIUM_MODEL, None),
        ("analyze memory leak and architectural deadlock", HIGH_MODEL, None),
    ]

    print("[*] Running Delegation Orchestrator Tests...")
    passed = 0
    for prompt, expected_model, expected_shortcut in test_cases:
        res = classify_prompt(prompt)
        assert res["model"] == expected_model, f"Expected {expected_model}, got {res['model']} for '{prompt}'"
        assert res["shortcut"] == expected_shortcut, f"Expected shortcut {expected_shortcut}, got {res['shortcut']} for '{prompt}'"
        print(f"  [PASS] '{prompt}' -> Tier: {res['tier']} | Model: {res['model']} | Profile: {res['profile']}")
        passed += 1

    print(f"\n[+] All {passed}/{len(test_cases)} tests passed successfully!")

def main():
    parser = argparse.ArgumentParser(description="Antigravity Delegation Orchestrator")
    parser.add_argument("--test", action="store_true", help="Run self-tests")
    parser.add_argument("prompt", nargs="*", help="Prompt text to classify")

    args = parser.parse_args()

    if args.test:
        run_tests()
        return

    prompt_str = " ".join(args.prompt).strip()
    if not prompt_str:
        parser.print_help()
        return

    result = classify_prompt(prompt_str)
    payload = format_subagent_payload(result)

    print("=== Classification Result ===")
    print(f"Tier    : {result['tier']}")
    print(f"Model   : {result['model']}")
    print(f"Profile : {result['profile']}")
    print(f"Shortcut: {result['shortcut'] or 'Auto-classified'}")
    print("\n=== Subagent Payload ===")
    print(json.dumps(payload, indent=2))

if __name__ == "__main__":
    main()
