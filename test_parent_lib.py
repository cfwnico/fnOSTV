import urllib.request
import hashlib
import time
import json

token = "4d84294c21904083a8bcf1aabebb29b7" # The user's token from screenshot
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def request_api(method, url_path, query="", body=""):
    s = str(int(time.time() * 1000 % 900000) + 100000)
    c = str(int(time.time() * 1000))
    o = md5(body) if body else md5("")
    t = "16CCEB3D-AB42-077D-36A1-F355324E4237"
    l = f"{secret}_{url_path}_{s}_{c}_{o}_{t}"
    sign = md5(l)
    authx = f"nonce={s}&timestamp={c}&sign={sign}"
    
    full_url = f"http://192.168.0.103:12561{url_path}"
    
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

body_json = json.dumps({
    "parent_guid": "2ca6c43d7fc14f5991f863e0a36e7f94",
    "sort_type": "DESC",
    "sort_column": "create_time",
    "page": 1,
    "page_size": 50
})
items = request_api("POST", "/v/api/v1/item/list", body=body_json)

if items and 'data' in items and items['data']:
    data = items['data']
    if isinstance(data, dict): data = data.get('list', [])
    print(f"Found {len(data)} items")
    for d in data:
        print("Child:", d.get('title'), d.get('type'))
