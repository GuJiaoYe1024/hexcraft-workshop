# -*- coding: utf-8 -*-
"""v0.3.4+：新增 6 个趣味/不确定性海克斯（掠夺、战利品翻倍、随机药水、豪赌等）+ 消息词条"""
import json, io, os

ROOT = r'H:\MC1314\MOD\src\main\resources\data\hexworkshop\hextech'
LANG_ZH = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\zh_cn.json'
LANG_EN = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\en_us.json'

def load(p): return json.load(io.open(p, encoding='utf-8'))
def save(p, d): io.open(p, 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2))
def write_hex(hid, d):
    io.open(os.path.join(ROOT, hid + '.json'), 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=2))

NEW = [
    ('hex_looter', '掠夺', 'GOLD', 8, 'loot_bonus:1', '击杀必定额外掉落 1-2 绿宝石'),
    ('hex_double_treasure', '双倍战利品', 'PRISMATIC', 6, 'loot_double:1', '击杀额外掉落翻倍战利品（随机 1-3 件常见掉落）'),
    ('hex_chaos_flask', '混沌药剂', 'SILVER', 7, 'potion_random:4', '击杀随机获得速度/力量/再生/跳跃药水 4 秒'),
    ('hex_golden_bet', '豪赌', 'GOLD', 7, 'gold_gamble:2', '击杀 50% 赚 2 晶币、50% 亏 1 晶币（高风险高回报）'),
    ('hex_loot_king', '掠夺之王', 'PRISMATIC', 5, 'loot_double:1,loot_bonus:1', '击杀战利品翻倍且必定额外掉落绿宝石'),
    ('hex_random_surge', '随机应战', 'SILVER', 6, 'potion_random:6,loot_bonus:0.2', '击杀随机药水 6 秒，并有 20% 概率额外掉绿宝石'),
]

zh = load(LANG_ZH); en = load(LANG_EN)
for hid, name_zh, rarity, weight, event, desc_zh in NEW:
    write_hex(hid, {'rarity': rarity, 'weight': weight, 'event': event})
    zh[f'hexcraft.hextech.{hid}.name'] = name_zh
    zh[f'hexcraft.hextech.{hid}.desc'] = desc_zh
    en[f'hexcraft.hextech.{hid}.name'] = ' '.join(w.capitalize() for w in hid.replace('hex_', '').split('_'))
    en[f'hexcraft.hextech.{hid}.desc'] = event.replace(':', ' ').replace(',', ', ')

zh.update({
    'message.hexcraft.potion_random': '随机药水生效！',
    'message.hexcraft.gamble_win': '豪赌成功：晶币 +%s',
    'message.hexcraft.gamble_lose': '豪赌失败：晶币 -%s',
})
en.update({
    'message.hexcraft.potion_random': 'Random potion!',
    'message.hexcraft.gamble_win': 'Bet won: +%s coins',
    'message.hexcraft.gamble_lose': 'Bet lost: -%s coins',
})
save(LANG_ZH, zh); save(LANG_EN, en)
print('new fun hexes:', len(NEW), '| total:', len([f for f in os.listdir(ROOT) if f.endswith('.json')]))
