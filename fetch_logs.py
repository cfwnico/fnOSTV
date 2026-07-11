import urllib.request
import json
import zipfile
import io

repo = "cfwnico/fnOSTV"
url = f"https://api.github.com/repos/{repo}/actions/runs?per_page=1"

req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode('utf-8'))
        run_id = data['workflow_runs'][0]['id']
        print(f"Latest run ID: {run_id}")
        
        # get logs url
        logs_url = f"https://api.github.com/repos/{repo}/actions/runs/{run_id}/logs"
        req_logs = urllib.request.Request(logs_url, headers={'User-Agent': 'Mozilla/5.0'})
        try:
            with urllib.request.urlopen(req_logs) as resp_logs:
                with zipfile.ZipFile(io.BytesIO(resp_logs.read())) as z:
                    for filename in z.namelist():
                        if "build" in filename.lower():
                            print(f"--- {filename} ---")
                            print(z.read(filename).decode('utf-8')[-2000:])
        except urllib.error.HTTPError as e:
            print(f"Failed to fetch logs. {e}")
            
except Exception as e:
    print(f"Error: {e}")
