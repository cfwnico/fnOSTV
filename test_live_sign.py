import urllib.request
import hashlib
import time
import json

base_url = "http://192.168.0.103:12561/v/api/v1/mediadb/list"
token = "4d84294c21904083a8bcf1aabebb29b7" # The user's token from screenshot
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def test_sign(path, body_hash, token_val):
    nonce = "123456"
    timestamp = str(int(time.time() * 1000))
    l = f"{secret}_{path}_{nonce}_{timestamp}_{body_hash}_{token_val}"
    sign = md5(l)
    authx = f"nonce={nonce}&timestamp={timestamp}&sign={sign}"
    
    req = urllib.request.Request(base_url, headers={
        'Authorization': token,
        'Accept': 'application/json',
        'Authx': authx
    })
    try:
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode('utf-8'))
            if data.get('code') != 5000:
                print(f"SUCCESS! path={path}, body_hash={body_hash}, token_val={token_val}")
                print(data)
                return True
    except Exception as e:
        # print("Error:", e)
        pass
    return False

paths = [
    "/v/api/v1/mediadb/list",
    "/api/v1/mediadb/list",
    "/mediadb/list",
    "http://192.168.0.103:12561/v/api/v1/mediadb/list"
]

body_hashes = [
    "", 
    md5(""),
    "{}",
    md5("{}")
]

tokens = [token, ""]

print("Testing live API...")
for p in paths:
    for bh in body_hashes:
        for tv in tokens:
            if test_sign(p, bh, tv):
                print("Found working combination!")
                exit(0)

print("None of the combinations worked.")
