import test_detail
import json

endpoints = [
    '/v/api/v1/play/sync',
    '/v/api/v1/play/report',
    '/v/api/v1/play/record',
    '/v/api/v1/play/progress',
    '/v/api/v1/item/sync',
    '/v/api/v1/item/progress',
    '/v/api/v1/item/record',
    '/v/api/v1/play/watched'
]

guid = '657b470317364889a51c777064e667ba'

for endp in endpoints:
    body = json.dumps({'item_guid': guid, 'ts': 200, 'duration': 2322})
    print(f"Testing {endp}")
    res = test_detail.request_api('POST', endp, body)
    if res and res.get('code') == 0:
        print(f"SUCCESS: {endp}")
        break

