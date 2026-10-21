# -*- coding: utf-8 -*-
"""生成海克斯工坊内容总表（海克斯/锻造/装备），供后续添加修改。"""
import json, io, glob, os
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill, Alignment
from openpyxl.utils import get_column_letter

ROOT = r'H:\MC1314\MOD\src\main\resources\data\hexworkshop'
LANG = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\zh_cn.json'

lang = json.load(io.open(LANG, encoding='utf-8'))

def rarity_cn(key):
    return {'SILVER': '白银', 'GOLD': '黄金', 'PRISMATIC': '棱彩'}.get(key, key)

def source_of(hid, d):
    src = d.get('source', '')
    if src:
        return src
    return 'tacz' if hid.startswith('tacz_') else 'vanilla'

def source_cn(src):
    return {'vanilla': '原版', 'tacz': 'TACZ联动'}.get(src, f'{src}联动')

def fmt_mods(mods):
    if not mods:
        return ''
    parts = []
    for m in mods:
        attr = m.get('attr', '')
        lv = m.get('perLevel', 0)
        parts.append(f'{attr} +{lv}/级')
    return '；'.join(parts)

# ============ 海克斯 ============
hexes = []
for p in sorted(glob.glob(ROOT + r'\hextech\*.json')):
    d = json.load(io.open(p, encoding='utf-8'))
    hid = os.path.basename(p)[:-5]
    name = lang.get(f'hexcraft.hextech.{hid}.name', '')
    desc = lang.get(f'hextech.{hid}.desc', lang.get(f'hexcraft.hextech.{hid}.desc', ''))
    hexes.append({
        'id': hid, 'name': name, 'rarity': rarity_cn(d.get('rarity', 'SILVER')),
        'weight': d.get('weight', 0), 'event': d.get('event', '') or '',
        'mods': fmt_mods(d.get('mods', [])), 'desc': desc,
        'source': source_cn(source_of(hid, d)),
    })
hexes.sort(key=lambda h: h['rarity'])

# ============ 装备 ============
equips = []
for p in sorted(glob.glob(ROOT + r'\equipment\*.json')):
    d = json.load(io.open(p, encoding='utf-8'))
    eid = os.path.basename(p)[:-5]
    name = lang.get(f'hexcraft.equip.{eid}.name', '')
    desc = lang.get(f'hexcraft.equip.{eid}.desc', '')
    equips.append({
        'id': eid, 'name': name, 'rarity': rarity_cn(d.get('rarity', 'GOLD')),
        'weight': d.get('weight', 0), 'icon': d.get('icon', ''), 'sell': d.get('sell', 0),
        'event': d.get('event', '') or '', 'mods': fmt_mods(d.get('mods', [])), 'desc': desc,
        'source': source_cn(source_of(eid, d)),
    })
equips.sort(key=lambda e: e['rarity'])

# ============ 锻造词条（lang 中 forge.*.name/desc + tier 档位） ============
TACZ_FORGE = {'gun_aspect', 'gun_crit_aspect', 'ammo_aspect'}
forge = []
for k in sorted(lang):
    if k.startswith('hexcraft.forge.') and k.endswith('.name'):
        fid = k[len('hexcraft.forge.'):-len('.name')]
        if '.' in fid:  # 跳过 forge.tier.* / forge.forge_title 等
            continue
        desc = lang.get(f'hexcraft.forge.{fid}.desc', '')
        forge.append({'id': fid, 'name': lang[k], 'desc': desc})
# 属性词条（数据 JSON 里没有，取自 lang：forge 前缀非机制的即属性）
MECH = {'flame', 'frost', 'poison', 'lifesteal', 'armor_break', 'thorns', 'regen',
        'gold_aspect', 'crit_aspect', 'vamp_aspect', 'execute_aspect', 'splash_aspect'}
for f in forge:
    f['type'] = '机制' if f['id'] in MECH else '属性'
    f['source'] = source_cn('tacz') if f['id'] in TACZ_FORGE else source_cn('vanilla')

# ============ 写 xlsx ============
wb = Workbook()
hdr_fill = PatternFill('solid', fgColor='1E6FE8')
hdr_font = Font(color='FFFFFF', bold=True)
silver_fill = PatternFill('solid', fgColor='E8EEF6')
gold_fill = PatternFill('solid', fgColor='FFF3D6')
prism_fill = PatternFill('solid', fgColor='F3E6FF')
wrap = Alignment(wrap_text=True, vertical='top')

def fill_sheet(ws, headers, rows, rarity_col):
    for c, h in enumerate(headers, 1):
        cell = ws.cell(1, c, h)
        cell.fill = hdr_fill; cell.font = hdr_font
        cell.alignment = Alignment(vertical='center')
    for r, row in enumerate(rows, 2):
        for c, v in enumerate(row, 1):
            ws.cell(r, c, v).alignment = wrap
        rk = str(row[rarity_col])
        if rk == '白银': ws.cell(r, rarity_col + 1).fill = silver_fill
        elif rk == '黄金': ws.cell(r, rarity_col + 1).fill = gold_fill
        elif rk == '棱彩': ws.cell(r, rarity_col + 1).fill = prism_fill
    for c in range(1, len(headers) + 1):
        width = max(len(str(headers[c-1])) + 2, max((len(str(row[c-1])) for row in rows), default=8) + 2)
        ws.column_dimensions[get_column_letter(c)].width = min(width, 60)
    ws.freeze_panes = 'A2'

ws1 = wb.active; ws1.title = f'海克斯({len(hexes)})'
fill_sheet(ws1, ['ID', '名称', '稀有度', '来源', '权重', '事件效果(event)', '属性加成(mods)', '效果描述(desc)'],
           [[h['id'], h['name'], h['rarity'], h['source'], h['weight'], h['event'], h['mods'], h['desc']] for h in hexes], 2)

ws2 = wb.create_sheet(f'装备({len(equips)})')
fill_sheet(ws2, ['ID', '名称', '稀有度', '来源', '权重', '图标', '卖出价', '事件效果(event)', '属性加成(mods)', '效果描述(desc)'],
           [[e['id'], e['name'], e['rarity'], e['source'], e['weight'], e['icon'], e['sell'], e['event'], e['mods'], e['desc']] for e in equips], 2)

ws3 = wb.create_sheet('锻造词条')
fill_sheet(ws3, ['ID', '名称', '类型(属性/机制)', '来源', '效果描述(desc)'],
           [[f['id'], f['name'], f['type'], f['source'], f['desc']] for f in forge], 0)

out = r'H:\MC1314\MOD\docs\海克斯工坊内容总表.xlsx'
os.makedirs(os.path.dirname(out), exist_ok=True)
wb.save(out)
print('hexes:', len(hexes), '| equips:', len(equips), '| forge:', len(forge))
print('saved:', out)
