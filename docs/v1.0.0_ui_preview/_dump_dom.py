import subprocess, io
edge = r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
url = "file:///H:/MC1314/MOD/docs/v1.0.0_ui_preview/v1.0.0%E7%95%8C%E9%9D%A2%E9%A2%84%E8%A7%88.html"
r = subprocess.run([edge, "--headless=new", "--disable-gpu", "--dump-dom",
                    "--virtual-time-budget=2000", "--user-data-dir=C:\\Temp\\hx_dom_prof", url],
                   capture_output=True, timeout=90)
out = (r.stdout or b"") .decode("utf-8", "replace")
io.open(r"H:\MC1314\MOD\docs\v1.0.0_ui_preview\_dom2.txt", "w", encoding="utf-8").write(out)
print("stdout len:", len(out), "stderr len:", len(r.stderr or b""))
