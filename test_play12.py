import urllib.request
import hashlib
import time
import json
import urllib.parse

token = "60532134135444c4b1b8d57931cef909"
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

recent = request_api("GET", "/v/api/v1/play/list")
if not recent:
    print("Could not get recent list")
    exit()

first = recent['data'][0]
media_guid = first.get('media_guid')
print(f"media_guid: {media_guid}")

print("Trying /play/info with POST...")
payload = json.dumps({"media_guid": media_guid})
print(f"Payload length: {len(payload)}")
res = request_api("POST", "/v/api/v1/play/info", body=payload)
if res: print(json.dumps(res, ensure_ascii=False)[:300])

print("Trying /play/stream with GET...")
res2 = request_api("GET", "/v/api/v1/play/stream", query=f"media_guid={media_guid}")
if res2: print(json.dumps(res2, ensure_ascii=False)[:300])
