# -*- coding: utf-8 -*-
import json, io, re

BASE = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang'
d = json.load(io.open(BASE + '\\zh_cn.json', encoding='utf-8'))
# 检查海克斯/装备/锻造 desc 中不含数字的（提示：描述无具体数值）
pat = re.compile(r'\d')
no_num = []
for k, v in d.items():
    if (k.startswith('hexcraft.hextech.') or k.startswith('hexcraft.equip.') or k.startswith('hexcraft.forge.')) and k.endswith('.desc'):
        if not pat.search(v):
            no_num.append((k, v))
print('desc without numbers:', len(no_num))
for k, v in no_num:
    print(k, '=', v)
