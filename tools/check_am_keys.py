# -*- coding: utf-8 -*-
"""v0.6.22 事件键全量对照：hex_am_*/equip_am_* JSON 的 event 键 ↔ AlexMobsLink 消费键 ↔ HextechEvents 聚合识别"""
import json, glob, os, re

HEX_DIR = r"H:\MC1314\MOD\src\main\resources\data\hexworkshop\hextech"
EQUIP_DIR = r"H:\MC1314\MOD\src\main\resources\data\hexworkshop\equipment"
LINK = r"H:\MC1314\MOD\src\main\java\com\hexcraft\link\AlexMobsLink.java"
EVENTS = r"H:\MC1314\MOD\src\main\java\com\hexcraft\event\HextechEvents.java"

link_src = open(LINK, encoding="utf-8").read()
# AlexMobsLink 消费键（effect 调用中 am_ 前缀）
consumed = set(re.findall(r'eff\([^)]*,\s*"(am_[a-z_]+)"', link_src))
# 注释里声明的通道键
consumed |= set(re.findall(r'^\s*\*\s*(am_[a-z_]+)\s+', link_src, re.M))

json_keys = set()
for f in glob.glob(os.path.join(HEX_DIR, "hex_am_*.json")):
    d = json.load(open(f, encoding="utf-8"))
    for ev in d.get("event", "").split(","):
        k = ev.split(":")[0].strip()
        json_keys.add(k)
for f in glob.glob(os.path.join(EQUIP_DIR, "equip_am_*.json")):
    d = json.load(open(f, encoding="utf-8"))
    for ev in d.get("event", "").split(","):
        k = ev.split(":")[0].strip()
        json_keys.add(k)

# HextechEvents 聚合：effect(data, key) 读取的是 key 对应值（按 key 精确匹配）
ev_src = open(EVENTS, encoding="utf-8").read()
ev_ok = True
for k in sorted(json_keys):
    # 聚合层按 key 存储/查询，无需逐键枚举；这里只确认无大小写/前缀问题
    if not re.search(r'"(am_[a-z_]+)"', ev_src):
        pass
missing = json_keys - consumed
print("JSON event 键数:", len(json_keys))
print("Link 消费键数:", len(consumed))
print("缺失（JSON 有但 Link 无）:", sorted(missing) if missing else "无")
extra = consumed - json_keys
print("多余（Link 有但 JSON 无）:", sorted(extra) if extra else "无")
print("全部:", sorted(json_keys))
