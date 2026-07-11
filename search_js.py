import re
with open("e:/FNOSTVKai/app2.js", "r", encoding="utf-8") as f:
    text = f.read()
    
# look for 'sign:' or 'sign=' or '.sign'
matches = re.finditer(r'.{0,150}(?:sign|invalid sign|Authorization).{0,150}', text, re.IGNORECASE)
found = set()
for m in matches:
    found.add(m.group(0))
    if len(found) > 30: break

for s in found:
    print(s)
    print("-" * 50)
