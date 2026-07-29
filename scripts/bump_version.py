#!/usr/bin/env python3
import os
import re
import datetime

def bump_version(env_path=".env"):
    if not os.path.exists(env_path):
        print(f"[!] {env_path} not found.")
        return

    with open(env_path, "r", encoding="utf-8") as f:
        content = f.read()

    # 1. VERSION_CODE Bump (YYMMDDxx format)
    today_prefix = datetime.datetime.now().strftime("%y%m%d")

    code_match = re.search(r"^VERSION_CODE=(\d+)", content, re.MULTILINE)
    if code_match:
        old_code_str = code_match.group(1)
        if old_code_str.startswith(today_prefix) and len(old_code_str) == 8:
            new_code = int(old_code_str) + 1
        else:
            new_code = int(f"{today_prefix}01")
            if new_code <= int(old_code_str):
                new_code = int(old_code_str) + 1
    else:
        new_code = int(f"{today_prefix}01")

    # 2. VERSION_NAME Bump (X.Y.Z format)
    name_match = re.search(r'^VERSION_NAME=["\']?([0-9]+(?:\.[0-9]+)*)["\']?', content, re.MULTILINE)
    if name_match:
        old_name = name_match.group(1)
        parts = old_name.split(".")
        if len(parts) >= 1:
            parts[-1] = str(int(parts[-1]) + 1)
            new_name = ".".join(parts)
        else:
            new_name = "0.1.1"
    else:
        new_name = "0.1.1"

    # Update .env
    new_content = re.sub(r"^VERSION_CODE=.*$", f"VERSION_CODE={new_code}", content, flags=re.MULTILINE)
    new_content = re.sub(r"^VERSION_NAME=.*$", f"VERSION_NAME={new_name}", new_content, flags=re.MULTILINE)

    with open(env_path, "w", encoding="utf-8") as f:
        f.write(new_content)

    print(f"[+] Automatically bumped version in {env_path}:")
    print(f"    VERSION_CODE : {old_code_str if code_match else 'N/A'} -> {new_code}")
    print(f"    VERSION_NAME : {old_name if name_match else 'N/A'} -> {new_name}")

if __name__ == "__main__":
    bump_version()
