import urllib.request
import hashlib
import time
import json
import urllib.parse

token = "4d84294c21904083a8bcf1aabebb29b7" # The user's token from screenshot
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def request_api(url_path, query=""):
    s = str(int(time.time() * 1000 % 900000) + 100000)
    c = str(int(time.time() * 1000))
    o = md5("") # d41d8cd98f00b204e9800998ecf8427e
    t = "16CCEB3D-AB42-077D-36A1-F355324E4237"
    
    l = f"{secret}_{url_path}_{s}_{c}_{o}_{t}"
    sign = md5(l)
    authx = f"nonce={s}&timestamp={c}&sign={sign}"
    
    full_url = f"http://192.168.0.103:12561{url_path}" + (f"?{query}" if query else "")
    
    req = urllib.request.Request(full_url, headers={
        'Authorization': token,
        'Accept': 'application/json',
        'Authx': authx
    })
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode('utf-8'))
    except Exception as e:
        print("Error fetching", url_path, e)
        return None

print("Fetching libraries...")
libs = request_api("/v/api/v1/mediadb/list")
if libs and 'data' in libs:
    data = libs['data']
    if isinstance(data, dict) and 'list' in data:
        items = data['list']
    else:
        items = data
    for item in items:
        print("Library:", item.get('name'), "GUID:", item.get('guid'))
        
print("\nFetching ALL items (/item/list)...")
all_items = request_api("/v/api/v1/item/list")
if all_items:
    print("ALL Items code:", all_items.get('code'))
    
