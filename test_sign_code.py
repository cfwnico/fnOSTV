import urllib.request
import hashlib
import time
import json
import urllib.parse

token = "4d84294c21904083a8bcf1aabebb29b7" # The user's token from screenshot
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def request_api(url_path, query="", wrong_sign=False):
    s = str(int(time.time() * 1000 % 900000) + 100000)
    c = str(int(time.time() * 1000))
    if query:
        o = md5(query)
    else:
        o = md5("")
    t = "16CCEB3D-AB42-077D-36A1-F355324E4237"
    l = f"{secret}_{url_path}_{s}_{c}_{o}_{t}"
    sign = md5(l)
    if wrong_sign:
        sign = md5(l + "bad")
    authx = f"nonce={s}&timestamp={c}&sign={sign}"
    
    full_url = f"http://192.168.0.103:12561{url_path}" + (f"?{query}" if query else "")
    
    req = urllib.request.Request(full_url, headers={
        'Authorization': token,
        'Accept': 'application/json',
        'Authx': authx
    })
    try:
        with urllib.request.urlopen(req) as resp:
            data = resp.read().decode('utf-8')
            return json.loads(data)
    except Exception as e:
        return None

res1 = request_api("/v/api/v1/mediadb/list", wrong_sign=False)
print("Correct sign (mediadb/list):", res1.get('code'))

res2 = request_api("/v/api/v1/mediadb/list", wrong_sign=True)
print("Wrong sign (mediadb/list):", res2.get('code') if res2 else "None")

res3 = request_api("/v/api/v1/item/list", "limit=50&mediadb_guid=2ca6c43d7fc14f5991f863e0a36e7f94&offset=0", wrong_sign=True)
print("Wrong sign (item/list):", res3.get('code') if res3 else "None")
