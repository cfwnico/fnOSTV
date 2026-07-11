import hashlib

def get_hash(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

target_sign = "859ed19b46c7c5f301a2a38e722707c2"
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"
s = "876178"
c = "1783792827123"

# Maybe apiKey `t` is something else? Let's check without t
print(get_hash(f"{secret}_/v/api/v1/mediadb/list_{s}_{c}__"))
print(get_hash(f"{secret}_/api/v1/mediadb/list_{s}_{c}__"))
print(get_hash(f"{secret}_/v/api/v1/mediadb/list_{s}_{c}_d41d8cd98f00b204e9800998ecf8427e_"))
print(get_hash(f"{secret}_/api/v1/mediadb/list_{s}_{c}_d41d8cd98f00b204e9800998ecf8427e_"))

# What if `t` is undefined?
print(get_hash(f"{secret}_/v/api/v1/mediadb/list_{s}_{c}__undefined"))

# What if URL is just `api/v1/mediadb/list`?
print(get_hash(f"{secret}_api/v1/mediadb/list_{s}_{c}__"))

# What if we just print all variations and check if one matches?
paths = [
    "/v/api/v1/mediadb/list",
    "/api/v1/mediadb/list",
    "/mediadb/list",
    "v/api/v1/mediadb/list",
    "api/v1/mediadb/list",
    "mediadb/list",
    "http://192.168.0.103:12561/v/api/v1/mediadb/list",
    "/v/api/v1/mediadb/list/"
]

body_hashes = [
    "", 
    get_hash(""),
    get_hash("{}"),
    "{}"
]

tokens = [
    "4d84294c21904083a8bcf1aabebb29b7", 
    "", 
    "undefined", 
    "null"
]

for p in paths:
    for bh in body_hashes:
        for tv in tokens:
            l = f"{secret}_{p}_{s}_{c}_{bh}_{tv}"
            if get_hash(l) == target_sign:
                print("FOUND MATCH:", l)
                exit(0)

print("Still not found.")
