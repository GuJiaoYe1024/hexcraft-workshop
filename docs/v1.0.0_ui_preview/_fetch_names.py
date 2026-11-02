import json, re
p = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\zh_cn.json"
with open(p, encoding='utf-8') as f:
    j = json.load(f)
prefs = [("tacz","TACZ 枪械"),("irons","铁魔法书"),("iaf","冰与火传说"),("ac","艾利克斯洞穴"),
         ("tf","暮色森林"),("am","艾利克斯生物"),("blade","拔刀剑"),("cataclysm","灾变"),
         ("emc","等价交换"),("farmer","农夫乐事"),("relic","遗物"),("artifact","神器"),
         ("quest","FTB 任务"),("goety","邪恶魔典"),("mowzie","莫瑞兹生物")]
out = []
for pre, label in prefs:
    keys = [k for k in j if k.startswith("hexcraft.hextech.hex_" + pre + "_") and k.endswith(".name")]
    names = [j[k] for k in keys[:8]]
    out.append(f"{label} => {' / '.join(names)}")
res = "\n".join(out)
with open(r"H:\MC1314\MOD\docs\v1.0.0_ui_preview\_src_names.txt", "w", encoding='utf-8') as f:
    f.write(res)
print(res)
