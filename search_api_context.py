import re
import os

for root, dirs, files in os.walk("e:/FNOSTVKai"):
    for file in files:
        if file.endswith(".js"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                text = f.read()
                matches = re.finditer(r'.{0,50}api/v1/.{0,50}', text, re.IGNORECASE)
                for m in matches:
                    print(f"[{file}] {m.group(0)}")
