import re

with open("e:/FNOSTVKai/target.js", "r", encoding="utf-8") as f:
    text = f.read()

# Let's just find strings containing '/api/v1/'
matches = re.finditer(r'["\'][a-zA-Z0-9_/]*api/v1/[a-zA-Z0-9_/]+["\']', text)
found = set()
for m in matches:
    found.add(m.group(0))

for s in found:
    print(s)
