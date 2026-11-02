# -*- coding: utf-8 -*-
import urllib.request, os
d = r"H:\MC1314\MOD\docs\v1.0.0_ui_preview\_user_shots"
os.makedirs(d, exist_ok=True)
urls = ["https://aka.doubaocdn.com/s/DZXoNlS8xT","https://aka.doubaocdn.com/s/7UQxH2PVmk","https://aka.doubaocdn.com/s/CsdPfJH1j5","https://aka.doubaocdn.com/s/qQwAciWZRb","https://aka.doubaocdn.com/s/ys6qWMYCvi","https://aka.doubaocdn.com/s/6IJxYdyKZ0","https://aka.doubaocdn.com/s/yqeScGdbA9","https://aka.doubaocdn.com/s/JWhZfaHA2S"]
for i,u in enumerate(urls,1):
    p = os.path.join(d, "u%d.png" % i)
    try:
        req = urllib.request.Request(u, headers={"User-Agent":"Mozilla/5.0"})
        data = urllib.request.urlopen(req, timeout=30).read()
        open(p,"wb").write(data)
        print(i, len(data))
    except Exception as e:
        print(i, "ERR", e)
