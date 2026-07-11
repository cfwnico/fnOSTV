import urllib.request
import json
import uuid

# Token and device logic from FnosRpcClient
token = "60532134135444c4b1b8d57931cef909"
secret = "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh"

def rpc_request(req_type, files_or_path, keyword=""):
    req = urllib.request.Request(
        "http://192.168.0.103:12561/api/file/search", 
        method="POST",
        headers={
            'Authorization': token,
            'Content-Type': 'application/json'
        },
        data=json.dumps({"req": "file.search", "path": "vol1", "keyword": keyword}).encode('utf-8')
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode('utf-8'))
    except Exception as e:
        print(f"Error: {e}")
        return None

res = rpc_request("file.search", "", "06.mp4")
print(res)
