import re
import os

endpoints = set()
for root, dirs, files in os.walk("e:/FNOSTVKai"):
    for file in files:
        if file.endswith(".js"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                text = f.read()
                matches = re.finditer(r'["\']/[a-zA-Z0-9_/]*play[a-zA-Z0-9_/]*["\']', text, re.IGNORECASE)
                for m in matches:
                    endpoints.add(m.group(0))
                matches2 = re.finditer(r'["\'][a-zA-Z0-9_/]*detail["\']', text, re.IGNORECASE)
                for m in matches2:
                    endpoints.add(m.group(0))

for s in endpoints:
    print(s)
