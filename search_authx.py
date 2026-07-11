import re

with open("e:/FNOSTVKai/app.js", "r", encoding="utf-8") as f:
    text = f.read()

matches = re.finditer(r'.{0,150}Authx.{0,150}', text, re.IGNORECASE)
for m in matches:
    print("app.js Authx match:")
    print(m.group(0))
    print("="*50)

with open("e:/FNOSTVKai/app2.js", "r", encoding="utf-8") as f:
    text = f.read()

matches = re.finditer(r'.{0,150}Authx.{0,150}', text, re.IGNORECASE)
for m in matches:
    print("app2.js Authx match:")
    print(m.group(0))
    print("="*50)
