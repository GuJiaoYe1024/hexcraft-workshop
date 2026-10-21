# -*- coding: utf-8 -*-
"""v0.3.4：新增 8 个趣味机制海克斯（回饱食度/击杀吃食物/药水效果/影响战利品）+ 语言词条"""
import json, io, os

ROOT = r'H:\MC1314\MOD\src\main\resources\data\hexworkshop\hextech'
LANG_ZH = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\zh_cn.json'
LANG_EN = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\en_us.json'

def load(p): return json.load(io.open(p, encoding='utf-8'))
def save(p, d): io.open(p, 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2))
def write_hex(hid, d):
    io.open(os.path.join(ROOT, hid + '.json'), 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2))

NEW = [
    ('hex_snack_thief', '觅食者', 'SILVER', 8, 'food_on_hit:1', '攻击命中回复 1 点饥饿值（饱食度不足时生效）'),
    ('hex_gourmand', '饕餮', 'SILVER', 8, 'food_on_kill:2', '击杀后大吃一顿：恢复饥饿与饱和度'),
    ('hex_potion_junkie', '药水瘾君子', 'SILVER', 7, 'potion_speed:3', '击杀获得 3 秒速度药水'),
    ('hex_war_cry', '战争咆哮', 'GOLD', 8, 'potion_strength:5,kill_heal:1', '击杀获得 5 秒力量药水并回复 1 颗心'),
    ('hex_lucky_feast', '幸运盛宴', 'GOLD', 8, 'loot_bonus:0.5,food_on_kill:1.5', '击杀 50% 概率额外掉落绿宝石，并恢复饱食度'),
    ('hex_regen_brew', '再生酿造', 'GOLD', 8, 'potion_regen:4', '击杀获得 4 秒再生药水'),
    ('hex_buff_master', '增益大师', 'PRISMATIC', 6, 'potion_strength:8,potion_speed:8,potion_regen:8', '击杀同时获得力量、速度、再生三药水各 8 秒'),
    ('hex_dragon_feast', '龙之盛宴', 'PRISMATIC', 5, 'loot_bonus:1,food_on_kill:4,bounty:10', '击杀必掉绿宝石、饱食拉满，精英怪额外 +10 晶币'),
]

zh = load(LANG_ZH); en = load(LANG_EN)
for hid, name_zh, rarity, weight, event, desc_zh in NEW:
    write_hex(hid, {'rarity': rarity, 'weight': weight, 'event': event})
    zh[f'hexcraft.hextech.{hid}.name'] = name_zh
    zh[f'hexcraft.hextech.{hid}.desc'] = desc_zh
    en[f'hexcraft.hextech.{hid}.name'] = ' '.join(w.capitalize() for w in hid.replace('hex_', '').split('_'))
    en[f'hexcraft.hextech.{hid}.desc'] = event.replace(':', ' ').replace(',', ', ')

# 界面分组标题与叠层提示（带具体名称）
zh.update({
    'hexcraft.gui.group_base': '基础属性',
    'hexcraft.gui.group_hidden': '隐藏属性',
    'hexcraft.gui.group_stack': '成长叠层',
    'hexcraft.gui.stack_hex_title': '海克斯',
    'hexcraft.gui.stack_equip_title': '装备',
    'message.hexcraft.stack_named': '【%s】层数 +1（当前 %s 层）',
    'message.hexcraft.equip_stack_named': '【%s】装备层数 +1（当前 %s 层）',
    'message.hexcraft.food_kill': '饱餐一顿，饥饿值回复',
})
en.update({
    'hexcraft.gui.group_base': 'Base Stats',
    'hexcraft.gui.group_hidden': 'Hidden Stats',
    'hexcraft.gui.group_stack': 'Growth Stacks',
    'hexcraft.gui.stack_hex_title': 'Hexes',
    'hexcraft.gui.stack_equip_title': 'Equips',
    'message.hexcraft.stack_named': '[%s] stack +1 (now %s)',
    'message.hexcraft.equip_stack_named': '[%s] equip stack +1 (now %s)',
    'message.hexcraft.food_kill': 'Feast! Hunger restored.',
})
save(LANG_ZH, zh); save(LANG_EN, en)
print('new hexes:', len(NEW))
