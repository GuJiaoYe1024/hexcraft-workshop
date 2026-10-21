# -*- coding: utf-8 -*-
"""为 HexConfigScreen（Cloth 版）追加配置界面翻译：分类 5 + 配置项 name 26 + tooltip 26 = 57 keys/语言"""
import json, io, collections

ZH = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\zh_cn.json"
EN = r"H:\MC1314\MOD\src\main\resources\assets\hexworkshop\lang\en_us.json"

# (field, zh_name, zh_tip, en_name, en_tip)
items = [
    # 分类
    ("cat.draw", "海克斯抽取", None, "Hextech Drawing", None),
    ("cat.economy", "经济系统", None, "Economy", None),
    ("cat.linked", "联动内容", None, "Linked Content", None),
    ("cat.client", "客户端", None, "Client", None),
    ("cat.rules", "游戏规则", None, "Game Rules", None),
    # 玩法
    ("draw.xpCostPerDraw", "抽取经验消耗", "每次海克斯抽取消耗的经验点数（约 19 级为 500 点）", "Draw XP Cost", "Experience points consumed per draw (~500 at level 19)"),
    ("draw.rerollCost", "刷新晶币消耗", "三选一刷新一次消耗的晶币", "Reroll Coin Cost", "Coins consumed to reroll the 3-choice offer"),
    ("draw.hexLimit", "海克斯槽位上限", "海克斯槽位上限（0=无限制）", "Hextech Slot Limit", "Max hextech slots (0 = unlimited)"),
    ("draw.silverWeight", "白银权重", "白银稀有度抽取权重", "Silver Weight", "Draw weight of silver rarity"),
    ("draw.goldWeight", "黄金权重", "黄金稀有度抽取权重", "Gold Weight", "Draw weight of gold rarity"),
    ("draw.prismWeight", "棱彩权重", "棱彩稀有度抽取权重", "Prismatic Weight", "Draw weight of prismatic rarity"),
    ("draw.pityGoldDraws", "黄金保底次数", "连续多少次未出黄金及以上则保底（0=关闭）", "Gold Pity Draws", "Pity after this many draws without gold+ (0 = off)"),
    ("draw.pityPrismDraws", "棱彩保底次数", "连续多少次未出棱彩则保底（0=关闭）", "Prismatic Pity Draws", "Pity after this many draws without prismatic (0 = off)"),
    ("draw.maxHextechLevel", "海克斯最高等级", "同名海克斯最高叠加等级", "Max Hextech Level", "Max stack level of the same hextech"),
    # 经济
    ("eco.coinMin", "普通怪晶币下限", "普通怪击杀掉落晶币下限", "Normal Coin Min", "Min coins from normal mob kills"),
    ("eco.coinMax", "普通怪晶币上限", "普通怪击杀掉落晶币上限", "Normal Coin Max", "Max coins from normal mob kills"),
    ("eco.coinEliteMin", "精英怪晶币下限", "精英怪（生命≥40）晶币掉落下限", "Elite Coin Min", "Min coins from elite mobs (HP >= 40)"),
    ("eco.coinEliteMax", "精英怪晶币上限", "精英怪（生命≥40）晶币掉落上限", "Elite Coin Max", "Max coins from elite mobs (HP >= 40)"),
    ("eco.coinBossMin", "首领怪晶币下限", "首领怪（生命≥120）晶币掉落下限", "Boss Coin Min", "Min coins from boss mobs (HP >= 120)"),
    ("eco.coinBossMax", "首领怪晶币上限", "首领怪（生命≥120）晶币掉落上限", "Boss Coin Max", "Max coins from boss mobs (HP >= 120)"),
    ("eco.antiFarmWindowSec", "反刷窗口(秒)", "反刷窗口：窗口内重复击杀同种怪掉落衰减（0=关闭）", "Anti-Farm Window (s)", "Kill window for drop decay on repeated same-mob kills (0 = off)"),
    ("eco.antiFarmDecay", "反刷衰减系数", "反刷衰减系数（0.2=掉落降至 20%）", "Anti-Farm Decay", "Decay factor (0.2 = drops reduced to 20%)"),
    ("eco.forgeBasicPrice", "基础锻造器价格", "基础锻造器价格（晶币）", "Basic Forger Price", "Basic forger price in coins"),
    ("eco.forgeGoldPrice", "黄金锻造器价格", "黄金锻造器价格（晶币）", "Gold Forger Price", "Gold forger price in coins"),
    ("eco.forgePrismaticPrice", "棱彩锻造器价格", "棱彩锻造器价格（晶币）", "Prismatic Forger Price", "Prismatic forger price in coins"),
    ("eco.forgeLimit", "锻造次数上限", "锻造总次数上限（0=无限制）", "Forge Limit", "Total forge limit (0 = unlimited)"),
    ("eco.refundSilver", "白银补偿", "满级重复抽到白银海克斯的晶币补偿", "Silver Refund", "Coin refund for maxed duplicate silver hextech"),
    ("eco.refundGold", "黄金补偿", "满级重复抽到黄金海克斯的晶币补偿", "Gold Refund", "Coin refund for maxed duplicate gold hextech"),
    ("eco.refundPrismatic", "棱彩补偿", "满级重复抽到棱彩海克斯的晶币补偿", "Prismatic Refund", "Coin refund for maxed duplicate prismatic hextech"),
    # 联动
    ("linked.enableTaczContent", "TACZ 联动总开关", "启用 TACZ（永恒枪械工坊）联动内容（需安装 TACZ 才生效）", "TACZ Linked Master", "Enable TACZ (Timeless and Classics Zero) linked content (requires TACZ installed)"),
    ("linked.enableTaczHextech", "TACZ 联动·海克斯", "TACZ 联动海克斯是否进入抽取池", "TACZ Linked Hextech", "Include TACZ hextech in the draw pool"),
    ("linked.enableTaczForge", "TACZ 联动·属性锻造器", "TACZ 联动锻造词条（枪械伤害/枪械暴击/弹药回复）是否可被锻造", "TACZ Linked Forge", "Allow forging TACZ forge traits (gun damage / gun crit / ammo regen)"),
    ("linked.enableTaczEquip", "TACZ 联动·装备", "TACZ 联动装备是否进入装备抽取池", "TACZ Linked Equip", "Include TACZ equipment in the equip pool"),
    # 客户端
    ("client.uiScale", "界面缩放", "海克斯界面 UI 缩放倍率", "UI Scale", "Hexcraft UI scale multiplier"),
    ("client.particlesEnabled", "特效开关", "抽取/入账特效开关", "Particles", "Enable draw / gain particles"),
]

zh_add = {}
en_add = {}
for f, zn, zt, en, et in items:
    if zn is not None:
        zh_add["hexcraft.config." + f] = zn
        en_add["hexcraft.config." + f] = en
    if zt is not None:
        zh_add["hexcraft.config.tip." + f] = zt
        en_add["hexcraft.config.tip." + f] = et

def merge(path, add):
    d = json.load(io.open(path, encoding="utf-8"), object_pairs_hook=collections.OrderedDict)
    dup = [k for k in add if k in d]
    d.update(add)
    with io.open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(d, f, ensure_ascii=False, indent=2)
    return len(d), dup

nz, dupz = merge(ZH, zh_add)
ne, dupe = merge(EN, en_add)
print("zh:", nz, "dup:", dupz)
print("en:", ne, "dup:", dupe)
