import hashlib

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

target_sign = "859ed19b46c7c5f301a2a38e722707c2"
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"
s = "876178"
c = "1783792827123"

r_candidates = [
    "/v/api/v1/mediadb/list",
    "/api/v1/mediadb/list",
    "v/api/v1/mediadb/list",
    "api/v1/mediadb/list",
    "/mediadb/list",
    "mediadb/list",
    "http://192.168.0.103:12561/v/api/v1/mediadb/list",
    "http://192.168.0.103:12561/api/v1/mediadb/list"
]

o_candidates = [
    "",
    md5(""),
    "{}",
    md5("{}")
]

t_candidates = [
    "4d84294c21904083a8bcf1aabebb29b7",
    "",
    "web",
    "616",
    "trimemedia-web"
]

def check():
    for r in r_candidates:
        for o in o_candidates:
            for t in t_candidates:
                l = f"{secret}_{r}_{s}_{c}_{o}_{t}"
                if md5(l) == target_sign:
                    print("FOUND!")
                    print(f"r: '{r}'")
                    print(f"o: '{o}'")
                    print(f"t: '{t}'")
                    print(f"l: '{l}'")
                    return True
    return False

if check():
    exit(0)
    
print("Phase 1 failed. Trying what if pt.default.hash is not MD5, but we know it's 32 chars...")
