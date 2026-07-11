import hashlib

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

target_sign = "859ed19b46c7c5f301a2a38e722707c2"
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"
s = "876178"
c = "1783792827123"
t = "4d84294c21904083a8bcf1aabebb29b7"

paths = [
    "/v/api/v1/mediadb/list",
    "/api/v1/mediadb/list",
    "/mediadb/list",
    "http://192.168.0.103:12561/v/api/v1/mediadb/list"
]

body_hashes = [
    "", # empty string
    md5(""), # md5 of empty string
    "{}", # empty json
    md5("{}")
]

tokens = [t, ""] # try with and without token

for r in paths:
    for o in body_hashes:
        for token_val in tokens:
            l = f"{secret}_{r}_{s}_{c}_{o}_{token_val}"
            hash_val = md5(l)
            if hash_val == target_sign:
                print("BINGO!!!")
                print("String:", l)
                print("r =", r)
                print("o =", o)
                print("token =", token_val)
                exit(0)

print("Not found.")
