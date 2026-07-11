import re

for filename in ["e:/FNOSTVKai/app.js", "e:/FNOSTVKai/app2.js"]:
    with open(filename, "r", encoding="utf-8") as f:
        text = f.read()
    
    matches = re.finditer(r'.{0,50}md5.{0,50}', text, re.IGNORECASE)
    found = set()
    for m in matches:
        found.add(m.group(0))
    print(f"--- {filename} MD5 matches ---")
    for s in list(found)[:10]:
        print(s.strip())
        
    matches = re.finditer(r'.{0,50}\.sign.{0,50}', text, re.IGNORECASE)
    found = set()
    for m in matches:
        found.add(m.group(0))
    print(f"--- {filename} .sign matches ---")
    for s in list(found)[:10]:
        print(s.strip())
