# -*- coding: utf-8 -*-
import json, glob, re, os
base = r'H:\MC1314\MOD\src\main'
json_keys = set()
for pat in ['resources/data/hexworkshop/hextech/hex_iaf_*.json','resources/data/hexworkshop/hextech/hex_ac_*.json','resources/data/hexworkshop/hextech/hex_tf_*.json','resources/data/hexworkshop/equipment/equip_iaf_*.json','resources/data/hexworkshop/equipment/equip_ac_*.json','resources/data/hexworkshop/equipment/equip_tf_*.json']:
    for p in glob.glob(os.path.join(base, pat)):
        d = json.load(open(p, encoding='utf-8'))
        if 'event' in d:
            for tok in d['event'].split(','):
                json_keys.add(tok.split(':')[0])

link_files = ['java/com/hexcraft/link/IceAndFireLink.java','java/com/hexcraft/link/AlexCavesLink.java','java/com/hexcraft/link/TwilightLink.java']
link_keys = set()
for lf in link_files:
    src = open(os.path.join(base, lf), encoding='utf-8').read()
    link_keys.update(re.findall(r'"(iaf_[a-z_]+|ac_[a-z_]+|tf_[a-z_]+)"', src))

hev = open(os.path.join(base, 'java/com/hexcraft/event/HextechEvents.java'), encoding='utf-8').read()
hev_keys = set(re.findall(r'"(iaf_[a-z_]+|ac_[a-z_]+|tf_[a-z_]+)"', hev))

print('JSON keys (%d):' % len(json_keys)); print(sorted(json_keys))
print()
print('JSON keys NOT in Link code:', sorted(json_keys - link_keys))
print('Link keys not in JSON:', sorted(link_keys - json_keys))
print()
print('JSON keys NOT in HextechEvents:', sorted(json_keys - hev_keys))
print()
# 装备 event 键 vs 海克斯 event 键重复检查（无重复语义）
from collections import Counter
allk = Counter()
for pat in ['resources/data/hexworkshop/hextech/hex_iaf_*.json','resources/data/hexworkshop/hextech/hex_ac_*.json','resources/data/hexworkshop/hextech/hex_tf_*.json']:
    for p in glob.glob(os.path.join(base, pat)):
        d = json.load(open(p, encoding='utf-8'))
        for tok in d['event'].split(','):
            allk[tok.split(':')[0]] += 1
print('event keys >1 use (multi-key hexes are intended):', {k:v for k,v in allk.items() if v>1})
