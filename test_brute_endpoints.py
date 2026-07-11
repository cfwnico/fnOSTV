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

recent = request_api("/v/api/v1/play/list")
items = recent['data']
if isinstance(items, dict): items = items.get('list', [])
first = items[0]
guid = first.get('guid')
media_guid = first.get('media_guid')
video_guid = first.get('video_guid')

endpoints_to_test = [
    ("/v/api/v1/mediadb/movie/detail", f"guid={guid}"),
    ("/v/api/v1/mediadb/tv/detail", f"guid={guid}"),
    ("/v/api/v1/mediadb/item/detail", f"guid={guid}"),
    ("/v/api/v1/mediadb/detail", f"guid={guid}"),
    ("/v/api/v1/play/detail", f"guid={guid}"),
    ("/v/api/v1/play/source", f"guid={guid}"),
    ("/v/api/v1/item/detail", f"guid={guid}"),
    ("/v/api/v1/item/detail", f"media_guid={media_guid}"),
    ("/v/api/v1/play/detail", f"media_guid={media_guid}"),
]

for ep, q in endpoints_to_test:
    res = request_api(ep, q)
    if res:
        print(f"Endpoint: {ep}?{q} -> Code: {res.get('code')} Msg: {res.get('msg')}")
    else:
        print(f"Endpoint: {ep}?{q} -> Failed")
