import re
with open("e:/FNOSTVKai/app.js", "r", encoding="utf-8") as f:
    text = f.read()

# find interceptors or request
matches = re.finditer(r'.{0,100}interceptors\.request.{0,500}', text, re.IGNORECASE)
for m in matches:
    print("app.js interceptor:")
    print(m.group(0))
    print("="*50)

with open("e:/FNOSTVKai/app2.js", "r", encoding="utf-8") as f:
    text = f.read()

matches = re.finditer(r'.{0,100}interceptors\.request.{0,500}', text, re.IGNORECASE)
for m in matches:
    print("app2.js interceptor:")
    print(m.group(0))
    print("="*50)
