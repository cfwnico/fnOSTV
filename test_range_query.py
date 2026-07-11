import urllib.request
import hashlib
import time
import json
import urllib.parse

token = "60532134135444c4b1b8d57931cef909"
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def request_api(method, url_path, query="", headers_extra={}):
    s = str(int(time.time() * 1000 % 900000) + 100000)
    c = str(int(time.time() * 1000))
    if query:
        parsed_q = urllib.parse.parse_qsl(query)
        parsed_q.sort(key=lambda x: x[0])
        sorted_q = urllib.parse.urlencode(parsed_q)
        o = md5(sorted_q)
        query = sorted_q
    else:
        o = md5("")
        
    t = "16CCEB3D-AB42-077D-36A1-F355324E4237"
    l = f"{secret}_{url_path}_{s}_{c}_{o}_{t}"
    sign = md5(l)
    authx = f"nonce={s}&timestamp={c}&sign={sign}"
    
    # PUT auth in query!
    full_url = f"http://192.168.0.103:12561{url_path}?" + (f"{query}&" if query else "") + f"token={token}&Authx={urllib.parse.quote(authx)}"
    
    req = urllib.request.Request(full_url, headers=headers_extra, method=method)
    
    try:
        with urllib.request.urlopen(req) as resp:
            print(f"{method} {full_url} -> {resp.status}")
            print(resp.headers)
            return resp.read(100)
    except urllib.error.HTTPError as e:
        print(f"HTTP Error: {e.code}")
        return None
    except Exception as e:
        print(f"Exception: {e}")
        return None

request_api("GET", "/v/api/v1/media/range/69baac0a08b848a6858b467b56013784", headers_extra={'Range': 'bytes=0-100'})
