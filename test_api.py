import urllib.request
import json

base_url = "http://192.168.0.103:12561/v/api/v1"
username = "cfw"
password = "cf19970727+=a"

# 1. Login
print("--- Login ---")
login_data = json.dumps({"username": username, "password": password, "app_name": "trimemedia-web"}).encode('utf-8')
req = urllib.request.Request(f"{base_url}/login", data=login_data, headers={'Content-Type': 'application/json', 'Accept': 'application/json'})
try:
    with urllib.request.urlopen(req) as response:
        login_resp = json.loads(response.read().decode('utf-8'))
        print(login_resp)
        token = login_resp.get("data", {}).get("token") or login_resp.get("token") or login_resp.get("data", "")
        if isinstance(token, dict):
            token = token.get("token", "")
        print("Token:", token)
except Exception as e:
    print("Login failed:", e)
    token = None

if token:
    # 2. Get Media Libraries
    print("\n--- Media Libraries ---")
    req = urllib.request.Request(f"{base_url}/mediadb/list", headers={'Authorization': token, 'Accept': 'application/json'})
    try:
        with urllib.request.urlopen(req) as response:
            print(json.loads(response.read().decode('utf-8')))
    except Exception as e:
        print("Media libraries failed:", e)

    # 3. Get Recent Items
    print("\n--- Recent Items ---")
    req = urllib.request.Request(f"{base_url}/play/list", headers={'Authorization': token, 'Accept': 'application/json'})
    try:
        with urllib.request.urlopen(req) as response:
            print(json.loads(response.read().decode('utf-8')))
    except Exception as e:
        print("Recent items failed:", e)
