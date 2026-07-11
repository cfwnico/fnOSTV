import hashlib

def md5(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

target_sign = "859ed19b46c7c5f301a2a38e722707c2"
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"
s = "876178"
c = "1783792827123"
t_vals = ["4d84294c21904083a8bcf1aabebb29b7", "", "undefined"]

paths = [
    "/v/api/v1/mediadb/list",
    "/api/v1/mediadb/list",
    "/mediadb/list",
    "http://192.168.0.103:12561/v/api/v1/mediadb/list",
    "v/api/v1/mediadb/list",
    "api/v1/mediadb/list"
]

body_hashes = [
    "", 
    md5(""),
    "{}",
    md5("{}"),
    "null",
    "undefined",
    md5("undefined"),
    md5("null")
]

for r in paths:
    for o in body_hashes:
        for t in t_vals:
            # try normal join
            l = f"{secret}_{r}_{s}_{c}_{o}_{t}"
            if md5(l) == target_sign:
                print("FOUND!")
                print(l)
                exit(0)
            
            # What if there are trailing underscores when t or o is empty?
            # join() in JS on an array [sec, r, s, c, o, t] will produce exactly `sec_r_s_c_o_t` 
            # even if some elements are empty strings, e.g. "a__b"
            
print("Still not found.")
