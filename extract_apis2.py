import re
import os

all_matches = set()
for root, dirs, files in os.walk('e:/FNOSTVKai/js_chunks'):
    for f in files:
        if f.endswith('.js'):
            text = open(os.path.join(root, f), 'r', encoding='utf-8', errors='ignore').read()
            matches = re.findall(r'`/[^`]*`', text)
            matches.extend(re.findall(r"\'/[^\']*\'", text))
            matches.extend(re.findall(r'\"/[^\"]*\"', text))
            for m in matches:
                all_matches.add(m)

for m in sorted(list(all_matches)):
    if 'play' in m.lower() or 'sync' in m.lower() or 'record' in m.lower() or 'ts' in m.lower() or 'report' in m.lower() or 'history' in m.lower():
        print(m)
