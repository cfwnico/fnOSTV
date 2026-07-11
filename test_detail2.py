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
    
    # IMPORTANT: The authx signature must include the FULL path, including query string? NO!
    # In JS: `[r, i] = pu(e.url)` => `r` is pathname, `i` is query.
    # `fu(i)` serializes query string, `mu(a)` hashes it!
    # Oh!!! If query string is present, `o` is NOT md5("") !!! It's md5(query)!
    
    if query:
        # We need to construct the sorted query string exactly like JS `fu(i)`
        # JS `fu` parses URLSearchParams and sorts them.
        # "guid=xxx" => "guid=xxx"
        a = query
        o = md5(a)
    else:
        o = md5("")
        
    l = f"{secret}_{url_path}_{s}_{c}_{o}_{t}"
    sign = md5(l)
    authx = f"nonce={s}&timestamp={c}&sign={sign}"
    
    full_url = f"http://192.168.0.103:12561{url_path}" + (f"?{query}" if query else "")
    print("Requesting", full_url)
    
    req = urllib.request.Request(full_url, headers={
        'Authorization': token,
        'Accept': 'application/json',
        'Authx': authx
    })
    try:
        with urllib.request.urlopen(req) as resp:
            data = resp.read().decode('utf-8')
            return json.loads(data)
    except urllib.error.HTTPError as e:
        print("HTTP Error fetching", url_path, e.code, e.read().decode('utf-8'))
        return None
    except Exception as e:
        print("Error fetching", url_path, e)
        return None

recent = request_api("/v/api/v1/play/list")
if recent and 'data' in recent and 'list' in recent['data']:
    items = recent['data']['list']
    if len(items) > 0:
        first = items[0]
        guid = first.get('guid')
        print("Fetching detail for recent item:", guid)
        detail = request_api("/v/api/v1/item/detail", f"guid={guid}")
        print("Detail Code:", detail.get('code') if detail else "None")
        if detail and detail.get('code') == 0:
            print("Successfully fetched detail!")
            print(json.dumps(detail['data'].get('media', {}), indent=2, ensure_ascii=False))
