# -*- coding: utf-8 -*-
"""v0.6.21 三联动（冰与火/Alex's Caves/暮色森林）翻译批量写入 zh_cn/en_us"""
import json, io, os

LANG = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang"

# ============ 海克斯文案：id -> (中文名, 中文desc, 英文名, 英文desc) ============
HEX = {
# ---- IAF 白银 ----
"hex_iaf_dragon_apprentice": ("屠龙学徒","每级 对冰与火龙族生物伤害 +8%（冰与火之歌联动）","Dragon Apprentice","Per level: +8% damage vs Ice and Fire dragons (Ice and Fire)" ),
"hex_iaf_scales": ("龙鳞护体","每级 受到龙息（火/冰/雷）伤害 -12%（冰与火之歌联动）","Dragon Scales","Per level: -12% dragon breath (fire/ice/lightning) damage taken (Ice and Fire)"),
"hex_iaf_hydra_bane": ("海德拉克星","每级 对海德拉伤害 +15%（冰与火之歌联动）","Hydra Bane","Per level: +15% damage vs hydra (Ice and Fire)"),
"hex_iaf_sea_hunter": ("深海猎手","每级 对海蛇伤害 +15%（冰与火之歌联动）","Sea Hunter","Per level: +15% damage vs sea serpents (Ice and Fire)"),
"hex_iaf_cyclops_bane": ("独眼克星","每级 对独眼巨人伤害 +15%（冰与火之歌联动）","Cyclops Bane","Per level: +15% damage vs cyclops (Ice and Fire)"),
"hex_iaf_dread_slayer": ("亡灵猎手","每级 对恐惧亡灵（Dread 系列）伤害 +15%（冰与火之歌联动）","Dread Slayer","Per level: +15% damage vs dread undead (Ice and Fire)"),
# ---- IAF 黄金 ----
"hex_iaf_dragon_slayer": ("屠龙者","每级 对冰与火龙族生物伤害 +25%（冰与火之歌联动）","Dragon Slayer","Per level: +25% damage vs Ice and Fire dragons (Ice and Fire)"),
"hex_iaf_fire_dragon": ("火龙屠戮者","每级 对火龙伤害 +20%（冰与火之歌联动）","Fire Dragon Slayer","Per level: +20% damage vs fire dragons (Ice and Fire)"),
"hex_iaf_ice_dragon": ("冰龙屠戮者","每级 对冰龙伤害 +20%（冰与火之歌联动）","Ice Dragon Slayer","Per level: +20% damage vs ice dragons (Ice and Fire)"),
"hex_iaf_lightning_dragon": ("雷龙屠戮者","每级 对雷龙伤害 +20%（冰与火之歌联动）","Lightning Dragon Slayer","Per level: +20% damage vs lightning dragons (Ice and Fire)"),
"hex_iaf_dragon_heart": ("龙血之心","每级 击杀冰与火龙族生物回复 4 点生命（冰与火之歌联动）","Dragon Heart","Per level: heal 4 HP on killing Ice and Fire dragons (Ice and Fire)"),
"hex_iaf_gorgon_bane": ("蛇发克星","每级 对蛇发女妖伤害 +25%，且受到石化伤害 -50%（冰与火之歌联动）","Gorgon Bane","Per level: +25% damage vs gorgons, -50% petrify damage taken (Ice and Fire)"),
"hex_iaf_myrmex_bane": ("虫族歼灭者","每级 对蚁族生物伤害 +20%（冰与火之歌联动）","Myrmex Exterminator","Per level: +20% damage vs myrmex (Ice and Fire)"),
"hex_iaf_amphithere_bane": ("翼蛇猎手","每级 对翼蛇伤害 +20%（冰与火之歌联动）","Amphithere Bane","Per level: +20% damage vs amphitheres (Ice and Fire)"),
# ---- IAF 棱彩 ----
"hex_iaf_dragon_lord": ("龙语者","每级 对冰与火龙族生物伤害 +40%，受到龙息伤害 -30%（冰与火之歌联动）","Dragon Lord","Per level: +40% damage vs Ice and Fire dragons, -30% dragon breath taken (Ice and Fire)"),
"hex_iaf_dragon_fury": ("龙之怒","每级 击杀冰与火龙族后 30 秒内全伤害 +30%（冰与火之歌联动）","Dragon's Fury","Per level: +30% all damage for 30s after slaying a dragon (Ice and Fire)"),
"hex_iaf_dragon_blood": ("龙血淬炼","每级 击杀龙族回复 8 点生命，且对龙族伤害 +20%（冰与火之歌联动）","Dragon Blood","Per level: heal 8 HP and +20% dragon damage after slaying a dragon (Ice and Fire)"),
"hex_iaf_hydra_blood": ("海德拉之血","每级 对海德拉伤害 +35%，受到龙息伤害 -20%（冰与火之歌联动）","Hydra Blood","Per level: +35% damage vs hydra, -20% dragon breath taken (Ice and Fire)"),
"hex_iaf_ghost_bane": ("灵体猎手","每级 对鬼魂（Ghost）伤害 +30%（冰与火之歌联动）","Ghost Bane","Per level: +30% damage vs ghosts (Ice and Fire)"),
"hex_iaf_breath_master": ("龙息掌握","每级 对龙族伤害 +30%，受到龙息伤害 -50%（冰与火之歌联动）","Breath Master","Per level: +30% damage vs dragons, -50% dragon breath taken (Ice and Fire)"),
# ---- AC 白银 ----
"hex_ac_radiation_guard": ("辐射防护","每级 受到辐射伤害 -15%（Alex's Caves 联动）","Radiation Guard","Per level: -15% radiation damage taken (Alex's Caves)"),
"hex_ac_toxic_guard": ("毒素防护","每级 受到酸性/毒性伤害 -15%（Alex's Caves 联动）","Toxin Guard","Per level: -15% acid/toxin damage taken (Alex's Caves)"),
"hex_ac_deep_hunter": ("深渊猎手","每级 对深渊/深海生物伤害 +15%（Alex's Caves 联动）","Abyssal Hunter","Per level: +15% damage vs abyssal creatures (Alex's Caves)"),
"hex_ac_prehistoric_hunter": ("史前猎手","每级 对史前生物（恐龙）伤害 +15%（Alex's Caves 联动）","Prehistoric Hunter","Per level: +15% damage vs prehistoric creatures (Alex's Caves)"),
"hex_ac_candy_lover": ("糖果甜心","每级 对糖果区生物伤害 +15%，且在糖果洞穴每 5 秒回复 1 点生命（Alex's Caves 联动）","Candy Lover","Per level: +15% damage vs candy creatures, heal 1 HP per 5s in Candy Cavity (Alex's Caves)"),
"hex_ac_magnetic_guard": ("磁力防护","每级 受到磁力/闪电伤害 -15%（Alex's Caves 联动）","Magnetic Guard","Per level: -15% magnetic/lightning damage taken (Alex's Caves)"),
# ---- AC 黄金 ----
"hex_ac_radiation_hunter": ("辐射克星","每级 对辐射区生物伤害 +25%（Alex's Caves 联动）","Radiation Hunter","Per level: +25% damage vs radiation creatures (Alex's Caves)"),
"hex_ac_toxic_hunter": ("毒素克星","每级 对毒性区生物伤害 +25%（Alex's Caves 联动）","Toxin Hunter","Per level: +25% damage vs toxic creatures (Alex's Caves)"),
"hex_ac_dark_hunter": ("暗域猎手","每级 对遗忘（黑暗）区生物伤害 +25%（Alex's Caves 联动）","Dark Hunter","Per level: +25% damage vs forlorn creatures (Alex's Caves)"),
"hex_ac_abyssal_breath": ("深渊呼吸","在深渊峡谷群系水下不消耗氧气（每级 1 级，Alex's Caves 联动）","Abyssal Breath","No oxygen consumed underwater in the Abyssal Chasm (per level, Alex's Caves)"),
"hex_ac_dark_vision": ("暗影视觉","在遗忘洞穴群系获得夜视（每级 1 级，Alex's Caves 联动）","Darkvision","Gain night vision in the Forlorn Hollows (per level, Alex's Caves)"),
"hex_ac_prehistoric_tamer": ("恐龙驯服者","每级 对史前生物伤害 +25%，击杀史前生物回复 2 点生命（Alex's Caves 联动）","Prehistoric Tamer","Per level: +25% damage vs prehistoric creatures, heal 2 HP on killing them (Alex's Caves)"),
"hex_ac_magnetic_hunter": ("磁暴猎手","每级 对磁力区生物伤害 +25%（Alex's Caves 联动）","Magnetic Hunter","Per level: +25% damage vs magnetic creatures (Alex's Caves)"),
"hex_ac_deep_guard": ("深渊护体","每级 受到深渊生物伤害 -20%（Alex's Caves 联动）","Abyssal Ward","Per level: -20% damage from abyssal creatures (Alex's Caves)"),
# ---- AC 棱彩 ----
"hex_ac_radiation_immune": ("辐射免疫","每级 受到辐射伤害 -60%，对辐射区生物伤害 +30%（Alex's Caves 联动）","Radiation Immunity","Per level: -60% radiation taken, +30% damage vs radiation creatures (Alex's Caves)"),
"hex_ac_toxic_immune": ("毒素免疫","每级 受到酸性/毒性伤害 -60%，对毒性区生物伤害 +30%（Alex's Caves 联动）","Toxin Immunity","Per level: -60% acid/toxin taken, +30% damage vs toxic creatures (Alex's Caves)"),
"hex_ac_tremorzilla_bane": ("巨兽克星","每级 对巨兽（Tremorzilla/恐雷兽）伤害 +35%（Alex's Caves 联动）","Megafauna Bane","Per level: +35% damage vs Tremorzilla/Tremorsaurus (Alex's Caves)"),
"hex_ac_brainiac_bane": ("智核猎手","每级 对智核体伤害 +35%（Alex's Caves 联动）","Brainiac Bane","Per level: +35% damage vs brainiacs (Alex's Caves)"),
"hex_ac_underzealot_bane": ("狂信者猎手","每级 对遗忘区生物伤害 +35%，并在遗忘洞穴获得夜视（Alex's Caves 联动）","Zealot Bane","Per level: +35% damage vs forlorn creatures, night vision in Forlorn Hollows (Alex's Caves)"),
"hex_ac_cave_lord": ("洞穴领主","每级 对史前/深渊/遗忘区生物伤害各 +30%（Alex's Caves 联动）","Cave Lord","Per level: +30% damage vs prehistoric/abyssal/forlorn creatures (Alex's Caves)"),
# ---- TF 白银 ----
"hex_tf_twilight_walker": ("暮色旅者","在暮色维度中全伤害 +10%（每级，暮色森林联动）","Twilight Walker","In the Twilight Forest dimension: +10% all damage (per level)"),
"hex_tf_naga_bane": ("纳迦克星","每级 对纳迦伤害 +15%（暮色森林联动）","Naga Bane","Per level: +15% damage vs nagas (Twilight Forest)"),
"hex_tf_lich_bane": ("巫妖克星","每级 对巫妖伤害 +15%（暮色森林联动）","Lich Bane","Per level: +15% damage vs liches (Twilight Forest)"),
"hex_tf_minoshroom_bane": ("牛头克星","每级 对牛头怪（Minoshroom）伤害 +15%（暮色森林联动）","Minoshroom Bane","Per level: +15% damage vs minoshrooms (Twilight Forest)"),
"hex_tf_ghast_bane": ("恶魂克星","每级 对黄昏恶魂（Ur-Ghast）伤害 +15%（暮色森林联动）","Ghast Bane","Per level: +15% damage vs Ur-Ghasts (Twilight Forest)"),
"hex_tf_knight_bane": ("骑士克星","每级 对幻影骑士伤害 +15%（暮色森林联动）","Knight Bane","Per level: +15% damage vs Knight Phantoms (Twilight Forest)"),
# ---- TF 黄金 ----
"hex_tf_boss_hunter": ("逐王猎手","每级 对全部暮色 BOSS 伤害 +20%（暮色森林联动）","Boss Hunter","Per level: +20% damage vs all Twilight Forest bosses"),
"hex_tf_yeti_bane": ("雪人克星","每级 对雪人（Yeti/Alpha Yeti）伤害 +20%（暮色森林联动）","Yeti Bane","Per level: +20% damage vs yetis (Twilight Forest)"),
"hex_tf_snow_queen_bane": ("冰雪女王克星","每级 对冰雪女王/冰晶伤害 +20%（暮色森林联动）","Snow Queen Bane","Per level: +20% damage vs Snow Queens and ice crystals (Twilight Forest)"),
"hex_tf_hydra_bane": ("九头蛇克星","每级 对暮色九头蛇伤害 +20%（暮色森林联动）","Hydra Bane","Per level: +20% damage vs Twilight hydras (Twilight Forest)"),
"hex_tf_troll_bane": ("巨魔克星","每级 对巨魔/巨人伤害 +20%（暮色森林联动）","Troll Bane","Per level: +20% damage vs trolls and giants (Twilight Forest)"),
"hex_tf_boss_resist": ("王威抵御","每级 受到暮色 BOSS 伤害 -20%（暮色森林联动）","Boss Ward","Per level: -20% damage from Twilight Forest bosses"),
"hex_tf_dimension_ward": ("暮色守护","在暮色维度中受到伤害 -20%（每级，暮色森林联动）","Twilight Ward","In the Twilight Forest dimension: -20% damage taken (per level)"),
"hex_tf_boss_kill_heal": ("王殒回生","每级 击杀暮色 BOSS 回复 4 点生命（暮色森林联动）","Kingfall Heal","Per level: heal 4 HP on killing a Twilight Forest boss"),
# ---- TF 棱彩 ----
"hex_tf_boss_slayer": ("弑王者","每级 对全部暮色 BOSS 伤害 +40%，受到 BOSS 伤害 -30%（暮色森林联动）","Boss Slayer","Per level: +40% damage vs all Twilight bosses, -30% boss damage taken"),
"hex_tf_final_hunter": ("终焉猎手","每级 对终焉 BOSS（PlateauBoss）伤害 +35%（暮色森林联动）","Final Hunter","Per level: +35% damage vs the Plateau Boss (Twilight Forest)"),
"hex_tf_twilight_lord": ("暮色主宰","在暮色维度中全伤害 +30%、受到伤害 -30%（每级，暮色森林联动）","Twilight Lord","In the Twilight Forest dimension: +30% damage, -30% damage taken (per level)"),
"hex_tf_boss_fury": ("王殒之怒","每级 击杀暮色 BOSS 后 30 秒内全伤害 +35%（暮色森林联动）","Kingfall Fury","Per level: +35% all damage for 30s after slaying a Twilight boss"),
}

# ============ 装备文案 ============
EQUIP = {
"equip_iaf_hexdrinker": ("饮魔刀·龙鳞","对龙族伤害 +15%，受到龙息伤害 -35%（每级；冰与火之歌联动）","Hexdrinker (Dragon Scale)","+15% damage vs dragons, -35% dragon breath taken per level (Ice and Fire)"),
"equip_iaf_dragon_scale": ("龙鳞战甲","对龙族伤害 +25%，受到龙息伤害 -25%（每级；冰与火之歌联动）","Dragon Scale Armor","+25% damage vs dragons, -25% dragon breath taken per level (Ice and Fire)"),
"equip_ac_hazmat": ("辐射防护服","受到辐射伤害 -50%、酸性/毒性伤害 -50%（每级；Alex's Caves 联动）","Hazmat Suit","-50% radiation, -50% acid/toxin damage taken per level (Alex's Caves)"),
"equip_ac_abyssal": ("深渊潜航器","在深渊峡谷群系水下呼吸，且对深渊生物伤害 +30%（每级；Alex's Caves 联动）","Abyssal Diver","No oxygen consumed in the Abyssal Chasm, +30% damage vs abyssal creatures per level (Alex's Caves)"),
"equip_tf_moonshard": ("月晶碎片","对暮色 BOSS 伤害 +15%，在暮色维度受到伤害 -15%（每级；暮色森林联动）","Moonshard","+15% damage vs Twilight bosses, -15% damage taken in Twilight per level"),
"equip_tf_amulet": ("暮色护符","受到暮色 BOSS 伤害 -25%，对 BOSS 伤害 +10%（每级；暮色森林联动）","Twilight Amulet","-25% boss damage taken, +10% damage vs bosses per level (Twilight Forest)"),
}

# ============ 锻造词条 ============
FORGE = {
"dragon_bane": ("屠龙者","锻造·对冰与火龙族生物伤害 +5%/级（冰与火之歌联动，需安装 iceandfire）","Dragon Bane","Forge: +5% damage vs Ice and Fire dragons per level (requires Ice and Fire)"),
"radiation_ward": ("辐射防护","锻造·受到辐射伤害 -5%/级（Alex's Caves 联动，需安装 alexscaves）","Radiation Ward","Forge: -5% radiation damage taken per level (requires Alex's Caves)"),
"deep_breath": ("深渊呼吸","锻造·深渊峡谷群系水下呼吸 1 级/级（Alex's Caves 联动，需安装 alexscaves）","Deep Breath","Forge: no oxygen consumed in the Abyssal Chasm per level (requires Alex's Caves)"),
"boss_bane": ("弑王","锻造·对暮色 BOSS 伤害 +5%/级（暮色森林联动，需安装 twilightforest）","Boss Bane","Forge: +5% damage vs Twilight Forest bosses per level (requires Twilight Forest)"),
}

# ============ 配置开关与数值 ============
CONFIG = {
"linked.enableIafContent": ("冰与火联动：总开关","启用 冰与火之歌（Ice and Fire）联动内容（需安装 iceandfire；关闭后联动海克斯/锻造词条/装备全部不进入抽取池，已拥有项保留显示）","Ice and Fire: Master","Enable Ice and Fire link content (requires iceandfire; disables linked hexes/forge/equipment from pools)"),
"linked.enableIafHextech": ("冰与火联动：海克斯","控制 20 个龙族/BOSS 战斗海克斯是否进入抽取池","Ice and Fire: Hextech","Control 20 dragon/BOSS combat hexes in the pool"),
"linked.enableIafForge": ("冰与火联动：锻造器","控制对龙增伤锻造词条是否可被锻造","Ice and Fire: Forge","Control the dragon bane forge entry"),
"linked.enableIafEquip": ("冰与火联动：装备","控制联动装备（饮魔刀/龙鳞甲）是否进入装备抽取池","Ice and Fire: Equipment","Control linked equipment in the equip pool"),
"linked.enableAcContent": ("Alex's Caves 联动：总开关","启用 Alex's Caves 联动内容（需安装 alexscaves；关闭后联动内容全部不进入抽取池）","Alex's Caves: Master","Enable Alex's Caves link content (requires alexscaves)"),
"linked.enableAcHextech": ("Alex's Caves 联动：海克斯","控制 20 个洞穴克制流海克斯是否进入抽取池","Alex's Caves: Hextech","Control 20 cave-counter hexes in the pool"),
"linked.enableAcForge": ("Alex's Caves 联动：锻造器","控制辐射防护/深渊呼吸锻造词条是否可被锻造","Alex's Caves: Forge","Control radiation ward/deep breath forge entries"),
"linked.enableAcEquip": ("Alex's Caves 联动：装备","控制联动装备（辐射防护服/深渊潜航器）是否进入装备抽取池","Alex's Caves: Equipment","Control linked equipment in the equip pool"),
"linked.enableTfContent": ("暮色森林联动：总开关","启用 暮色森林（Twilight Forest）联动内容（需安装 twilightforest；关闭后联动内容全部不进入抽取池）","Twilight Forest: Master","Enable Twilight Forest link content (requires twilightforest)"),
"linked.enableTfHextech": ("暮色森林联动：海克斯","控制 18 个 BOSS 进度/维度海克斯是否进入抽取池","Twilight Forest: Hextech","Control 18 boss/dimension hexes in the pool"),
"linked.enableTfForge": ("暮色森林联动：锻造器","控制对 BOSS 增伤锻造词条是否可被锻造","Twilight Forest: Forge","Control the boss bane forge entry"),
"linked.enableTfEquip": ("暮色森林联动：装备","控制联动装备（月晶碎片/暮色护符）是否进入装备抽取池","Twilight Forest: Equipment","Control linked equipment in the equip pool"),
"v060.iafEffectMult": ("冰与火联动效果倍率","所有冰与火海克斯/装备事件效果 × 该倍率（默认 1.0）","Ice and Fire Effect Multiplier","All Ice and Fire hex/equipment event effects x this multiplier (default 1.0)"),
"v060.acEffectMult": ("Alex's Caves 联动效果倍率","所有 Alex's Caves 海克斯/装备事件效果 × 该倍率（默认 1.0）","Alex's Caves Effect Multiplier","All Alex's Caves hex/equipment event effects x this multiplier (default 1.0)"),
"v060.tfEffectMult": ("暮色森林联动效果倍率","所有暮色森林海克斯/装备事件效果 × 该倍率（默认 1.0）","Twilight Forest Effect Multiplier","All Twilight Forest hex/equipment event effects x this multiplier (default 1.0)"),
}

def main():
    for fn, zh_idx, en_idx in (("zh_cn.json", 0, 2), ("en_us.json", 1, 3)):
        path = os.path.join(LANG, fn)
        d = json.load(open(path, encoding="utf-8"))
        before = len(d)
        for hid, (zname, zdesc, ename, edesc) in HEX.items():
            d["hexcraft.hextech.%s.name" % hid] = zname if fn.startswith("zh") else ename
            d["hexcraft.hextech.%s.desc" % hid] = zdesc if fn.startswith("zh") else edesc
        for eid, (zname, zdesc, ename, edesc) in EQUIP.items():
            d["hexcraft.equip.%s.name" % eid] = zname if fn.startswith("zh") else ename
            d["hexcraft.equip.%s.desc" % eid] = zdesc if fn.startswith("zh") else edesc
        for stat, (zname, zdesc, ename, edesc) in FORGE.items():
            d["hexcraft.forge.%s.name" % stat] = zname if fn.startswith("zh") else ename
            d["hexcraft.forge.%s.desc" % stat] = zdesc if fn.startswith("zh") else edesc
        for cfg, (zname, zdesc, ename, edesc) in CONFIG.items():
            if cfg.startswith("linked."):
                key = "hexcraft.config.linked." + cfg[7:]
                tip = "hexcraft.config.tip.linked." + cfg[7:]
            else:
                key = "hexcraft.config." + cfg
                tip = "hexcraft.config.tip." + cfg
            d[key] = zname if fn.startswith("zh") else ename
            d[tip] = zdesc if fn.startswith("zh") else edesc
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(json.dumps(d, ensure_ascii=False, indent=2))
        print(fn, "keys:", before, "->", len(d), "+%d" % (len(d) - before))

if __name__ == "__main__":
    main()
