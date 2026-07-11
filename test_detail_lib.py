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

libs = request_api("/v/api/v1/mediadb/list")
lib_guid = libs['data'][0]['guid']
print("Fetching items for library:", lib_guid)

items = request_api("/v/api/v1/item/list", f"limit=50&mediadb_guid={lib_guid}&offset=0")
if items and 'data' in items:
    print("Code:", items.get('code'))
    data = items['data']
    if isinstance(data, dict): data = data.get('list', [])
    if len(data) > 0:
        first = data[0]
        guid = first.get('guid')
        print("Item in library:", guid)
        # Try fetching detail
        det = request_api("/v/api/v1/item/detail", f"guid={guid}")
        print("Detail:", det)
