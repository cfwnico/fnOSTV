import urllib.request
import re

base_url = "http://192.168.0.103:12561"

print("Fetching index...")
try:
    with urllib.request.urlopen(f"{base_url}/v/") as response:
        html = response.read().decode('utf-8')
        js_files = re.findall(r'<script[^>]+src="([^"]+)"', html)
        print("JS Files:", js_files)
        
        for js_path in js_files:
            if not js_path.startswith("http"):
                url = f"{base_url}{js_path}" if js_path.startswith("/") else f"{base_url}/{js_path}"
            else:
                url = js_path
            print("Fetching", url)
            try:
                with urllib.request.urlopen(url) as js_resp:
                    js_content = js_resp.read().decode('utf-8')
                    if 'sign' in js_content and 'md5' in js_content.lower():
                        print(f"Found 'sign' and 'md5' in {js_path}")
                        matches = re.findall(r'.{0,100}sign.{0,150}', js_content, re.IGNORECASE)
                        for m in set(matches[:20]):
                            print(m)
            except Exception as e:
                print("Failed to fetch", url, e)
                
except Exception as e:
    print("Failed to fetch index:", e)
