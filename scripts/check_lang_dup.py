import json, io, re, os

for f in ["zh_cn.json", "en_us.json"]:
    p = os.path.join(r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang", f)
    txt = io.open(p, encoding="utf-8").read()
    keys = re.findall(r'"([^"]+)"\s*:', txt)
    seen = set()
    dups = []
    for k in keys:
        if k in seen:
            dups.append(k)
        seen.add(k)
    print(f, "重复键:", dups if dups else "无")
