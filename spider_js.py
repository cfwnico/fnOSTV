import urllib.request
import re
import os

base_url = "http://192.168.0.103:12561/v/"

os.makedirs("e:/FNOSTVKai/js_chunks", exist_ok=True)
downloaded = set()
to_download = [
    "assets/d77cd48b8cf8ff8e2058f7ffcaf1709c-CXETYg3G.js",
    "assets/1bc04b5291c26a46d918139138b992d2-DsK_BRP3.js"
]

endpoints = set()

while to_download:
    url_path = to_download.pop(0)
    if url_path in downloaded:
        continue
    downloaded.add(url_path)
    print("Downloading", url_path)
    
    try:
        real_url = base_url + url_path.split("/")[-1] if "assets/" in url_path and "/" not in url_path.replace("assets/", "") else base_url + url_path
        if url_path.startswith("assets/") and url_path.count("/") == 1:
            real_url = base_url + url_path
        elif url_path.startswith("/v/assets/"):
            real_url = "http://192.168.0.103:12561" + url_path
        elif url_path.startswith("assets/"):
            real_url = base_url + url_path
        else:
            real_url = base_url + "assets/" + url_path

        with urllib.request.urlopen(real_url) as resp:
            js = resp.read().decode('utf-8')
            
            # Save it
            local_path = os.path.join("e:/FNOSTVKai/js_chunks", url_path.split("/")[-1])
            with open(local_path, "w", encoding="utf-8") as f:
                f.write(js)
            
            # Find new JS files
            js_files = re.findall(r'["\']([^"\']+\.js)["\']', js)
            for j in js_files:
                if j not in downloaded and j not in to_download:
                    to_download.append(j)
                    
            matches = re.finditer(r'["\'](/[a-zA-Z0-9_/-]*detail[a-zA-Z0-9_/-]*)["\']', js, re.IGNORECASE)
            for m in matches: endpoints.add(m.group(1))
            matches2 = re.finditer(r'["\'](/[a-zA-Z0-9_/-]*play[a-zA-Z0-9_/-]*)["\']', js, re.IGNORECASE)
            for m in matches2: endpoints.add(m.group(1))
                
    except Exception as e:
        print("Failed", url_path, e)

print("Found endpoints:")
for ep in sorted(list(endpoints)):
    if ep.startswith('/'):
        print(ep)
