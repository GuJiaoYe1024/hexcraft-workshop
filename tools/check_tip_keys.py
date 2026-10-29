# -*- coding: utf-8 -*-
import json, io, os
langdir = r'H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang'
zh = json.load(io.open(os.path.join(langdir, 'zh_cn.json'), encoding='utf-8'))
en = json.load(io.open(os.path.join(langdir, 'en_us.json'), encoding='utf-8'))
missing = []
for c in ('enableIronsContent', 'enableIronsHextech', 'enableIronsForge', 'enableIronsEquip'):
    k = 'hexcraft.config.tip.linked.%s' % c
    if k not in zh or k not in en:
        missing.append('linked ' + k)
for c in ('ironsEffectMult', 'ironsCritMult', 'ironsExecuteThreshold', 'ironsCastHaste', 'ironsManaMax', 'ironsManaRegen'):
    k = 'hexcraft.config.tip.v060.%s' % c
    if k not in zh or k not in en:
        missing.append('v060 ' + k)
print('missing new tip keys:', len(missing))
for m in missing:
    print(' ', m)
bad = [k for k in zh if 'config.tip' in k and k not in en]
print('tip keys missing in en:', len(bad))
print('CHECK DONE')
