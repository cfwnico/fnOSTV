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
    if query:
        o = md5(query)
    else:
        o = md5("")
        
    t = "16CCEB3D-AB42-077D-36A1-F355324E4237"
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
if recent and 'data' in recent:
    items = recent['data']
    if isinstance(items, dict) and 'list' in items:
        items = items['list']
        
    if len(items) > 0:
        first = items[0]
        guid = first.get('guid')
        print("Fetching detail for recent item:", guid)
        detail = request_api("/v/api/v1/item/detail", f"guid={guid}")
        print("Detail Code:", detail.get('code') if detail else "None")
        if detail and detail.get('code') == 0:
            print("Successfully fetched detail!")
            
            # Now let's try to get playback source
            media_guid = first.get('media_guid')
            print("Fetching playback source for media_guid:", media_guid)
            source = request_api("/v/api/v1/play/detail", f"media_guid={media_guid}")
            print("Source Code:", source.get('code') if source else "None")
            if source and source.get('code') == 0:
                print(json.dumps(source['data'], indent=2, ensure_ascii=False))

