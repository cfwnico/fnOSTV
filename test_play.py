import urllib.request
import hashlib
import time
import json

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

# media_guid is the ACTUAL movie/episode guid
media_guid = first.get('media_guid')
print("Media GUID:", media_guid)

# Try fetching detail using media_guid as the guid parameter
res = request_api("/v/api/v1/item/detail", f"guid={media_guid}")
if res:
    print("Item Detail Code:", res.get('code'))
    print(json.dumps(res.get('data', {}).get('media', {}).get('title'), ensure_ascii=False))

# Now try getting playback sources!
res2 = request_api("/v/api/v1/play/info", f"media_guid={media_guid}")
if res2:
    print("Play Info Code:", res2.get('code'))

res3 = request_api("/v/api/v1/play/media", f"media_guid={media_guid}")
if res3:
    print("Play Media Code:", res3.get('code'))

res4 = request_api("/v/api/v1/play/url", f"media_guid={media_guid}")
if res4:
    print("Play URL Code:", res4.get('code'))

res5 = request_api("/v/api/v1/item/play", f"guid={media_guid}")
if res5:
    print("Item Play Code:", res5.get('code'))

res6 = request_api("/v/api/v1/play/detail", f"guid={media_guid}")
if res6:
    print("Play Detail Code:", res6.get('code'))
