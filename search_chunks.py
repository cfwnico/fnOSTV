import urllib.request
import re

base_url = "http://192.168.0.103:12561/v"
js_pattern = re.compile(r'[/a-zA-Z0-9_-]+\.js')

with open("e:/FNOSTVKai/app.js", "r", encoding="utf-8") as f:
    text = f.read()

# find all .js strings in the file
js_files = set(js_pattern.findall(text))
print("Found possible chunks:", len(js_files))

for js_path in js_files:
    if "chunk" in js_path or js_path.startswith("assets/"):
        url = f"{base_url}/{js_path}"
        try:
            with urllib.request.urlopen(url) as js_resp:
                content = js_resp.read().decode('utf-8')
                if 'Authx' in content or 'nonce' in content:
                    print(f"!!! FOUND in {url} !!!")
                    m = re.search(r'.{0,100}Authx.{0,100}', content, re.IGNORECASE)
                    if m: print(m.group(0))
                    m2 = re.search(r'.{0,100}nonce.{0,200}timestamp.{0,100}', content, re.IGNORECASE)
                    if m2: print(m2.group(0))
        except Exception as e:
            pass

with open("e:/FNOSTVKai/app2.js", "r", encoding="utf-8") as f:
    text = f.read()

js_files = set(js_pattern.findall(text))
for js_path in js_files:
    if "chunk" in js_path or js_path.startswith("assets/"):
        url = f"{base_url}/{js_path}"
        try:
            with urllib.request.urlopen(url) as js_resp:
                content = js_resp.read().decode('utf-8')
                if 'Authx' in content or 'nonce' in content:
                    print(f"!!! FOUND in {url} !!!")
                    m = re.search(r'.{0,100}Authx.{0,100}', content, re.IGNORECASE)
                    if m: print(m.group(0))
                    m2 = re.search(r'.{0,100}nonce.{0,200}timestamp.{0,100}', content, re.IGNORECASE)
                    if m2: print(m2.group(0))
        except Exception as e:
            pass
