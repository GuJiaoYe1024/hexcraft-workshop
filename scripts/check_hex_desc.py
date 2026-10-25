import json, io, os, glob, re

BASE = r"H:\MC1314\MOD\src\main\resources"
HEX = os.path.join(BASE, "data", "hexworkshop", "hextech")
zh = json.load(io.open(os.path.join(BASE, "assets", "hexworkshop", "lang", "zh_cn.json"), encoding="utf-8"))
en = json.load(io.open(os.path.join(BASE, "assets", "hexworkshop", "lang", "en_us.json"), encoding="utf-8"))

issues = []
BAD_WORDS = ["空气", "占位", "TODO", "待定", "敬请期待", "None", "null", "未知", "（暂未实现）", "测试", "调试", "\ufffd", "黄金："]

files = sorted(glob.glob(os.path.join(HEX, "*.json")))
for p in files:
    try:
        d = json.load(io.open(p, encoding="utf-8"))
    except Exception as e:
        issues.append(f"{os.path.basename(p)}: JSON 解析失败 {e}")
        continue
    hid = os.path.basename(p)[:-5]
    ev = d.get("event", "")
    rar = d.get("rarity", "?")
    name_key = f"hexcraft.hextech.{hid}.name"
    desc_key = f"hexcraft.hextech.{hid}.desc"
    zname = zh.get(name_key)
    zdesc = zh.get(desc_key)
    ename = en.get(name_key)
    edesc = en.get(desc_key)
    if zname is None: issues.append(f"{hid}: zh name 缺失")
    if ename is None: issues.append(f"{hid}: en name 缺失")
    if zdesc is None: issues.append(f"{hid}: zh desc 缺失")
    if edesc is None: issues.append(f"{hid}: en desc 缺失")
    if zdesc:
        for w in BAD_WORDS:
            if w in zdesc:
                issues.append(f"{hid}: zh desc 含异常字样「{w}」 → {zdesc[:40]}")
                break
    if edesc:
        for w in BAD_WORDS:
            if w in edesc:
                issues.append(f"{hid}: en desc 含异常字样「{w}」 → {edesc[:40]}")
                break
    # 数值一致性启发式：event "key:0.06" → desc 应有 "6%"（或 0.06/6）; "key:1.0" → "100%"/必/"1"
    if ev and zdesc:
        for seg in ev.split(","):
            seg = seg.strip()
            if ":" not in seg: continue
            k, v = seg.split(":", 1)
            try:
                val = float(v)
            except:
                continue
            if val >= 0.98:  # 1.0 语义: 必/100%/必定
                for marker in ["100%", "必定", "必", "2 件", "2件"]:
                    if marker in zdesc: break
                else:
                    issues.append(f"{hid}: event {k}:{v} 但 desc 无「100%/必定」 → {zdesc[:40]}")
            elif 0.005 <= val <= 0.95:
                pct = int(round(val * 100))
                if str(pct) + "%" not in zdesc and (str(pct) not in zdesc):
                    issues.append(f"{hid}: event {k}:{v} 但 desc 无「{pct}%」 → {zdesc[:40]}")

print(f"共检查 {len(files)} 个海克斯 JSON")
if issues:
    print(f"发现 {len(issues)} 条问题：")
    for it in issues: print(" -", it)
else:
    print("全部通过：无缺失/无占位/无异常字样/数值一致性 OK")
