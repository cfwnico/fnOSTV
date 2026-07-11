import urllib.request
import hashlib
import time
import json

base_url = "http://192.168.0.103:12561/v/api/v1/mediadb/list"
token = "4d84294c21904083a8bcf1aabebb29b7" # The user's token from screenshot
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def test_sign():
    s = str(int(time.time() * 1000 % 900000) + 100000)
    c = str(int(time.time() * 1000))
    url_path = "/v/api/v1/mediadb/list"
    o = md5("") # d41d8cd98f00b204e9800998ecf8427e
    t = "16CCEB3D-AB42-077D-36A1-F355324E4237"
    
    l = f"{secret}_{url_path}_{s}_{c}_{o}_{t}"
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
            print("Response Code:", data.get('code'))
            print("Message:", data.get('msg', 'Success!'))
            if data.get('code') != 5000:
                print("Successfully fetched live data!")
    except urllib.error.HTTPError as e:
        print(f"HTTPError: {e.code}")
        print(e.read())
    except Exception as e:
        print("Error", e)

print("Testing final live Authx logic...")
test_sign()
