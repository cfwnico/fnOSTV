import re

with open("e:/FNOSTVKai/target.js", "r", encoding="utf-8") as f:
    text = f.read()
    
# Find '_h=' or '_h ='
matches = re.finditer(r'.{0,50}_h\s*=.?', text)
found = set()
for m in matches:
    found.add(m.group(0))

for s in found:
    print(s)
    print("-" * 50)
