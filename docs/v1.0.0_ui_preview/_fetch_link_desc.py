import json, io
p = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\zh_cn.json"
with open(p, encoding='utf-8') as f:
    j = json.load(f)
prefs = [("tacz","TACZ 枪械"),("irons","铁魔法书"),("iaf","冰与火传说"),("ac","艾利克斯洞穴"),
         ("tf","暮色森林"),("am","艾利克斯生物"),("blade","拔刀剑"),("cataclysm","灾变"),
         ("emc","等价交换"),("farmer","农夫乐事"),("relic","遗物"),("artifact","神器"),
         ("quest","FTB 任务"),("goety","邪恶魔典"),("mowzie","莫瑞兹生物")]
out = []
for pre, label in prefs:
    keys = sorted([k for k in j if k.startswith("hexcraft.hextech.hex_" + pre + "_") and k.endswith(".name")])[:6]
    items = []
    for k in keys:
        stem = k[:-5]  # strip ".name"
        name = j[k]
        desc = j.get(stem + ".desc", "")
        if isinstance(desc, str) and len(desc) > 40:
            desc = desc[:40] + "…"
        items.append(f"{name}§{desc}")
    out.append(f"{label}§{pre}§{'|'.join(items)}")
res = "\n".join(out)
io.open(r"H:\MC1314\MOD\docs\v1.0.0_ui_preview\_link_data.txt", "w", encoding='utf-8').write(res)
print(res)
