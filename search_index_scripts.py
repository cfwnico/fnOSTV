import urllib.request
import re

url = "http://192.168.0.103:12561/v/"
try:
    with urllib.request.urlopen(url) as resp:
        html = resp.read().decode('utf-8')
        scripts = re.findall(r'<script[^>]+src=["\']([^"\']+)["\']', html)
        for s in scripts:
            print("Script:", s)
            if s.startswith('/'):
                script_url = f"http://192.168.0.103:12561{s}"
            else:
                script_url = f"http://192.168.0.103:12561/v/{s}"
            with urllib.request.urlopen(script_url) as s_resp:
                js = s_resp.read().decode('utf-8')
                matches = re.finditer(r'.{0,50}api/v1/[a-zA-Z0-9_/]*detail.{0,50}', js, re.IGNORECASE)
                for m in matches:
                    print(f"[{s}] MATCH 1: {m.group(0)}")
                matches2 = re.finditer(r'.{0,50}api/v1/[a-zA-Z0-9_/]*play.{0,50}', js, re.IGNORECASE)
                for m in matches2:
                    print(f"[{s}] MATCH 2: {m.group(0)}")
except Exception as e:
    print(e)
