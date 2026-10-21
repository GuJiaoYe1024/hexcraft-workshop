# -*- coding: utf-8 -*-
"""新增跳出框框的随机/不确定性海克斯（净化/随机馈赠/随机祝福/混沌之王）+ 消息词条"""
import json, io, os

ROOT = r'H:\MC1314\MOD\src\main\resources\data\hexworkshop\hextech'
LANG_ZH = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\zh_cn.json'
LANG_EN = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\en_us.json'

def load(p): return json.load(io.open(p, encoding='utf-8'))
def save(p, d): io.open(p, 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2))
def write_hex(hid, d):
    io.open(os.path.join(ROOT, hid + '.json'), 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2))

NEW = [
    ('hex_cleanse', '净化', 'SILVER', 6, 'cleanse:1', '击杀清除一个随机负面效果（中毒/虚弱/减速等）'),
    ('hex_random_gift', '随机馈赠', 'GOLD', 6, 'random_loot:1', '击杀随机获得一件随机物品（铁锭/面包/经验瓶/书…）'),
    ('hex_blessing', '随机祝福', 'SILVER', 6, 'potion_random:4,food_on_kill:2', '击杀随机获得正面药水并恢复饱食度'),
    ('hex_chaos_king', '混沌之王', 'PRISMATIC', 4, 'potion_random:8,random_loot:1,cleanse:1', '击杀随机药水 8 秒、随机物品、并净化负面状态'),
]

zh = load(LANG_ZH); en = load(LANG_EN)
for hid, name_zh, rarity, weight, event, desc_zh in NEW:
    write_hex(hid, {'rarity': rarity, 'weight': weight, 'event': event})
    zh[f'hexcraft.hextech.{hid}.name'] = name_zh
    zh[f'hexcraft.hextech.{hid}.desc'] = desc_zh
    en[f'hexcraft.hextech.{hid}.name'] = ' '.join(w.capitalize() for w in hid.replace('hex_', '').split('_'))
    en[f'hexcraft.hextech.{hid}.desc'] = event.replace(':', ' ').replace(',', ', ')

zh.update({
    'message.hexcraft.cleanse': '净化！负面状态已清除',
    'message.hexcraft.random_loot': '随机馈赠：获得 %s',
})
en.update({
    'message.hexcraft.cleanse': 'Cleansed! Negative effect removed',
    'message.hexcraft.random_loot': 'Random gift: %s',
})
save(LANG_ZH, zh); save(LANG_EN, en)
print('new hexes:', len(NEW), '| total:', len([f for f in os.listdir(ROOT) if f.endswith('.json')]))
