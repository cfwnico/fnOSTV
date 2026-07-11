import re
import os

text = open('e:/FNOSTVKai/js_chunks/524bbf6eac93a5c6ade03004a9d88d33-pIVYE9f8.js', 'r', encoding='utf-8', errors='ignore').read()
matches = re.findall(r'`/[^`]*`', text)
matches.extend(re.findall(r"\'/[^\']*\'", text))
matches.extend(re.findall(r'\"/[^\"]*\"', text))

for m in sorted(list(set(matches))):
    if 'play' in m.lower() or 'sync' in m.lower() or 'record' in m.lower() or 'ts' in m.lower():
        print(m)
