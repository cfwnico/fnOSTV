import os

for root, dirs, files in os.walk("e:/FNOSTVKai"):
    for file in files:
        if file.endswith(".js"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
                if "NDzZTVxnRKP8Z0jXg1VAMonaG8akvh" in content:
                    print("FOUND IN:", path)
