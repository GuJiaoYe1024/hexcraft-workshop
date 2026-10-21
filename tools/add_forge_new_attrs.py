# -*- coding: utf-8 -*-
"""补锻造新属性词条翻译（jump/reach/build）"""
import json, io

base = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang'
ADD_ZH = {
    'hexcraft.forge.jump.name': '跳跃',
    'hexcraft.forge.jump.desc': '每级 跳跃高度 +8%（重复锻造可叠加）',
    'hexcraft.forge.reach.name': '长臂',
    'hexcraft.forge.reach.desc': '每级 攻击距离 +0.6 格（重复锻造可叠加）',
    'hexcraft.forge.build.name': '建造',
    'hexcraft.forge.build.desc': '每级 建造距离 +0.6 格（重复锻造可叠加）',
}
ADD_EN = {
    'hexcraft.forge.jump.name': 'Jump',
    'hexcraft.forge.jump.desc': '+8% jump height per level (stacks)',
    'hexcraft.forge.reach.name': 'Reach',
    'hexcraft.forge.reach.desc': '+0.6 attack range per level (stacks)',
    'hexcraft.forge.build.name': 'Build',
    'hexcraft.forge.build.desc': '+0.6 build range per level (stacks)',
}
for fn, add in (('zh_cn.json', ADD_ZH), ('en_us.json', ADD_EN)):
    d = json.load(io.open(base + '\\' + fn, encoding='utf-8'))
    d.update(add)
    io.open(base + '\\' + fn, 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2))
print('added 6 keys')
