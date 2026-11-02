# -*- coding: utf-8 -*-
import io, json
src = io.open(r"H:\MC1314\MOD\docs\v1.0.0_ui_preview\_link_data.txt", encoding='utf-8').read()
counts = {"tacz":46,"irons":39,"iaf":30,"ac":30,"tf":28,"am":26,"blade":20,"cataclysm":19,
          "emc":19,"farmer":19,"relic":19,"artifact":19,"quest":19,"goety":19,"mowzie":19}
okmap = {"tacz":True,"irons":True,"iaf":True,"ac":True,"tf":True,"am":True,"blade":False,"cataclysm":True,
         "emc":True,"farmer":False,"relic":False,"artifact":False,"quest":False,"goety":False,"mowzie":False}
lines = []
for ln in src.strip().split("\n"):
    label, pre, items = ln.split("§", 2)
    names = []
    for it in items.split("|"):
        nm, _, d = it.partition("§")
        nm = nm.replace("'", "\\'"); d = d.replace("'", "\\'").strip()
        names.append("{n:'%s',d:'%s'}" % (nm, d))
    lines.append("{n:'%s',pre:'%s',count:%d,ok:%s,names:[%s]}" % (label, pre, counts[pre], "true" if okmap[pre] else "false", ",".join(names)))
block = "var FUTURE=[\n  " + ",\n  ".join(lines) + "\n];"
io.open(r"H:\MC1314\MOD\docs\v1.0.0_ui_preview\_future_data.js", "w", encoding='utf-8').write(block)
print(len(lines), "mods")
