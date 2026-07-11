import urllib.request
import hashlib
import time
import json

base_url = "http://192.168.0.103:12561/v/api/v1/mediadb/list"
token = "4d84294c21904083a8bcf1aabebb29b7" # user's token
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def test_sign(url_path, token_val, o_val):
    s = str(int(time.time() * 1000 % 900000) + 100000)
    c = str(int(time.time() * 1000))
    
    l = f"{secret}_{url_path}_{s}_{c}_{o_val}_{token_val}"
    sign = md5(l)
    authx = f"nonce={s}&timestamp={c}&sign={sign}"
    
    req = urllib.request.Request(base_url, headers={
        'Authorization': token,
        'Accept': 'application/json',
        'Authx': authx
    })
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            print(f"Path '{url_path}', token_val '{token_val}', o_val '{o_val}':", data.get('code'), data.get('msg', 'Success!'))
            if data.get('code') != 5000:
                return True
    except Exception as e:
        print(f"Path '{url_path}', token_val '{token_val}', o_val '{o_val}': ERROR", e)
    return False

paths_to_test = [
    "/v/api/v1/mediadb/list",
    "/api/v1/mediadb/list",
    "http://192.168.0.103:12561/v/api/v1/mediadb/list"
]

tokens_to_test = [token, ""]

o_vals = [
    "",
    md5("")
]

print("Testing live Authx logic exhaustively...")
for p in paths_to_test:
    for t in tokens_to_test:
        for o in o_vals:
            if test_sign(p, t, o):
                print("MATCH FOUND:", p, t, o)
                exit(0)

print("No match found.")
