import urllib.request
import re
import os

base_url = "http://192.168.0.103:12561/v/"

# Let's get the index.html first
try:
    with urllib.request.urlopen(base_url) as resp:
        html = resp.read().decode('utf-8')
        print("Fetched index.html")
except Exception as e:
    print("Error fetching index:", e)
    exit(1)

# Find all script src
scripts = re.findall(r'src="([^"]+)"', html)
# also look for modulepreload or other links
links = re.findall(r'href="([^"]+\.js)"', html)

all_js = set(scripts + links)
print("Found js files in index:", all_js)

for js in all_js:
    if js.startswith('/'):
        url = "http://192.168.0.103:12561" + js
    else:
        url = base_url + js
        
    try:
        with urllib.request.urlopen(url) as resp:
            content = resp.read().decode('utf-8')
            if "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh" in content:
                print(f"!!! FOUND SECRET IN {url} !!!")
                with open("e:/FNOSTVKai/target.js", "w", encoding="utf-8") as f:
                    f.write(content)
                exit(0)
    except Exception as e:
        print(f"Error fetching {url}:", e)

print("Not found in index.html references.")
