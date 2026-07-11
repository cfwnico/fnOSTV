import hashlib
import itertools

target = "859ed19b46c7c5f301a2a38e722707c2"
nonce = "876178"
timestamp = "1783792827123"
token = "4d84294c21904083a8bcf1aabebb29b7"
path = "/v/api/v1/mediadb/list"
api_path = "/mediadb/list"
v1_path = "/api/v1/mediadb/list"

elements = {
    "nonce": nonce,
    "timestamp": timestamp,
    "token": token,
    "path": path,
    "api_path": api_path,
    "v1_path": v1_path
}

def check(s):
    if hashlib.md5(s.encode('utf-8')).hexdigest() == target:
        print("FOUND MATCH:", s)
        return True
    return False

# Try permutations of 2 to 4 elements
for r in range(2, 5):
    for keys in itertools.permutations(elements.keys(), r):
        s = "".join([elements[k] for k in keys])
        if check(s): exit(0)
        
        # with common delimiters
        for delim in ["", "&", "-", "_", "|", ":"]:
            s = delim.join([elements[k] for k in keys])
            if check(s): exit(0)

print("Not found with simple permutations.")
