import re

with open("e:/FNOSTVKai/target.js", "r", encoding="utf-8") as f:
    text = f.read()

matches = re.finditer(r'["\']/api/v1/[a-z/]+detail["\']', text, re.IGNORECASE)
found = set()
for m in matches:
    found.add(m.group(0))

print("Endpoints found:")
for s in found:
    print(s)

matches2 = re.finditer(r'["\']/api/v1/play/[a-z/]+["\']', text, re.IGNORECASE)
found2 = set()
for m in matches2:
    found2.add(m.group(0))

print("Play endpoints found:")
for s in found2:
    print(s)
