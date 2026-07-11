import hashlib

target = "859ed19b46c7c5f301a2a38e722707c2"
token = "4d84294c21904083a8bcf1aabebb29b7"
nonce = "876178"
timestamp = "1783792827123"
url = "http://192.168.0.103:12561/v/api/v1/mediadb/list"
path = "/v/api/v1/mediadb/list"
api_path = "/mediadb/list"
method = "GET"

elements = [token, nonce, timestamp, url, path, api_path, method, "trimemedia-web", "trimemedia", "web"]
separators = ["", "&", "=", "_", "-", "|", ":", "?"]

def check(s):
    if hashlib.md5(s.encode('utf-8')).hexdigest() == target:
        print("FOUND! String:", repr(s))
        return True
    return False

import itertools

print("Brute forcing up to 3 elements with separators...")
for r in range(1, 4):
    for keys in itertools.permutations(elements, r):
        for sep in separators:
            s = sep.join(keys)
            if check(s): exit(0)
            
print("Brute forcing combinations with param names...")
params = [f"nonce={nonce}", f"timestamp={timestamp}", f"token={token}", f"Authorization={token}"]
for r in range(1, 4):
    for keys in itertools.permutations(params, r):
        for sep in separators:
            s = sep.join(keys)
            if check(s): exit(0)

print("Not found.")
