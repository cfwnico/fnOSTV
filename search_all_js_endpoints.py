import re
import os

endpoints = set()
for root, dirs, files in os.walk("e:/FNOSTVKai"):
    for file in files:
        if file.endswith(".js"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                text = f.read()
                matches = re.finditer(r'["\'][a-zA-Z0-9_/]*api/v1/[a-zA-Z0-9_/]+["\']', text)
                for m in matches:
                    endpoints.add(m.group(0))

for s in endpoints:
    print(s)
