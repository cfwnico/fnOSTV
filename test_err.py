import urllib.request
import hashlib
import time
import json
import urllib.parse

token = "4d84294c21904083a8bcf1aabebb29b7" # The user's token from screenshot

req = urllib.request.Request("http://192.168.0.103:12561/v/api/v1/item/detail?guid=657b470317364889a51c777064e667ba", headers={
    'Authorization': token,
    'Accept': 'application/json',
    'Authx': "nonce=123&timestamp=123&sign=bad"
})
try:
    with urllib.request.urlopen(req) as resp:
        data = resp.read().decode('utf-8')
        print(data)
except urllib.error.HTTPError as e:
    print("HTTP Error", e.code, e.read().decode('utf-8'))
