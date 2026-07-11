import urllib.request
import hashlib
import time
import json
import urllib.parse

token = "4d84294c21904083a8bcf1aabebb29b7" # The user's token from screenshot
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def request_api(method, url_path, query="", body=""):
    s = str(int(time.time() * 1000 % 900000) + 100000)
    c = str(int(time.time() * 1000))
    if query:
        parsed_q = urllib.parse.parse_qsl(query)
        parsed_q.sort(key=lambda x: x[0])
        sorted_q = urllib.parse.urlencode(parsed_q)
        o = md5(sorted_q)
        query = sorted_q
    else:
        o = md5(body) if body else md5("")
        
    t = "16CCEB3D-AB42-077D-36A1-F355324E4237"
    l = f"{secret}_{url_path}_{s}_{c}_{o}_{t}"
    sign = md5(l)
    authx = f"nonce={s}&timestamp={c}&sign={sign}"
    
    full_url = f"http://192.168.0.103:12561{url_path}" + (f"?{query}" if query else "")
    
    req = urllib.request.Request(full_url, headers={
        'Authorization': token,
        'Accept': 'application/json',
        'Authx': authx
    }, method=method)
    
    if method == "POST" and body:
        req.add_header('Content-Type', 'application/json')
        req.data = body.encode('utf-8')
        
    try:
        with urllib.request.urlopen(req) as resp:
            data = resp.read().decode('utf-8')
            return json.loads(data)
    except Exception as e:
        return None

guid = "aef573ed10aa4c64ba5032bb4a732aed" # A TV Show
lib_guid = "2ca6c43d7fc14f5991f863e0a36e7f94" # The library it belongs to

endpoints = [
    ("/v/api/v1/item/detail", "GET", f"guid={guid}"),
    ("/v/api/v1/item/detail", "GET", f"media_guid={guid}"),
    ("/v/api/v1/movie/detail", "GET", f"guid={guid}"),
    ("/v/api/v1/tv/detail", "GET", f"guid={guid}"),
    ("/v/api/v1/play/info", "GET", f"media_guid={guid}"),
    ("/v/api/v1/play/source", "GET", f"media_guid={guid}"),
    ("/v/api/v1/play/url", "GET", f"media_guid={guid}"),
    ("/v/api/v1/mediadb/movie/detail", "GET", f"guid={guid}"),
    ("/v/api/v1/mediadb/tv/detail", "GET", f"guid={guid}"),
    ("/v/api/v1/mediadb/item/detail", "GET", f"guid={guid}"),
    ("/v/api/v1/tv/episode/list", "GET", f"guid={guid}"),
    ("/v/api/v1/tv/season/list", "GET", f"guid={guid}"),
]

for ep, method, q in endpoints:
    res = request_api(method, ep, q, "")
    if res:
        print(f"{method} {ep}?{q} -> Code: {res.get('code')} Msg: {res.get('msg')}")
    else:
        print(f"{method} {ep}?{q} -> Failed")
