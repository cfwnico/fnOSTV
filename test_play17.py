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
        'Authx': authx,
        'Accept': '*/*'
    }, method=method)
    
    if method == "POST" and body:
        req.add_header('Content-Type', 'application/json')
        req.data = body.encode('utf-8')
        
    try:
        with urllib.request.urlopen(req) as resp:
            print(f"{method} {full_url} -> {resp.status}")
            print(resp.headers)
            return resp.read()
    except urllib.error.HTTPError as e:
        print(f"HTTP Error: {e.code}")
        print(e.headers)
        return None
    except Exception as e:
        print(f"Exception: {e}")
        return None

recent = request_api("GET", "/v/api/v1/play/list")
recent_json = json.loads(recent.decode('utf-8'))
item_guid = recent_json['data'][0].get('guid')

body = json.dumps({"item_guid": item_guid}, separators=(',', ':'))
info = request_api("POST", "/v/api/v1/play/info", body=body)
info_json = json.loads(info.decode('utf-8'))
video_guid = info_json['data']['video_guid']

print("Testing media...")
request_api("GET", f"/v/api/v1/media/{video_guid}")
request_api("GET", f"/v/api/v1/play/{video_guid}")
request_api("GET", f"/v/api/v1/video/{video_guid}")
request_api("GET", f"/v/api/v1/media/stream", query=f"guid={video_guid}")
