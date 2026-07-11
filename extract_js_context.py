import re

with open("e:/FNOSTVKai/target.js", "r", encoding="utf-8") as f:
    text = f.read()
    
# Find the gu function body and surrounding lines
# Since we know `NDzZTVxnRKP8Z0jXg1VAMonaG8akvh` is in there
idx = text.find("NDzZTVxnRKP8Z0jXg1VAMonaG8akvh")
if idx != -1:
    start = max(0, idx - 1000)
    end = min(len(text), idx + 1000)
    print("=== CONTEXT AROUND SECRET ===")
    print(text[start:end])
