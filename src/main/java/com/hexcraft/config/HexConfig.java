package com.hexcraft.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class HexConfig {

    // ---- Common (server-authoritative) ----
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue XP_COST_PER_DRAW;
    public static final ForgeConfigSpec.IntValue REROLL_COST;
    public static final ForgeConfigSpec.IntValue HEX_SLOT_COUNT;
    public static final ForgeConfigSpec.IntValue WEIGHT_SILVER;
    public static final ForgeConfigSpec.IntValue WEIGHT_GOLD;
    public static final ForgeConfigSpec.IntValue WEIGHT_PRISMATIC;
    public static final ForgeConfigSpec.IntValue PITY_GOLD;
    public static final ForgeConfigSpec.IntValue PITY_PRISMATIC;
    public static final ForgeConfigSpec.IntValue COIN_MIN;
    public static final ForgeConfigSpec.IntValue COIN_MAX;
    public static final ForgeConfigSpec.IntValue COIN_ELITE_MIN;
    public static final ForgeConfigSpec.IntValue COIN_ELITE_MAX;
    public static final ForgeConfigSpec.IntValue COIN_BOSS_MIN;
    public static final ForgeConfigSpec.IntValue COIN_BOSS_MAX;
    public static final ForgeConfigSpec.IntValue ANTI_FARM_WINDOW_SEC;
    public static final ForgeConfigSpec.DoubleValue ANTI_FARM_DECAY;
    public static final ForgeConfigSpec.IntValue FORGE_BASIC_PRICE;
    public static final ForgeConfigSpec.IntValue FORGE_GOLD_PRICE;
    public static final ForgeConfigSpec.IntValue FORGE_PRISMATIC_PRICE;
    public static final ForgeConfigSpec.IntValue MAX_FORGES;
    public static final ForgeConfigSpec.IntValue REFUND_SILVER;
    public static final ForgeConfigSpec.IntValue REFUND_GOLD;
    public static final ForgeConfigSpec.IntValue REFUND_PRISMATIC;
    public static final ForgeConfigSpec.IntValue MAX_HEXTECH_LEVEL;

    // ---- 联动内容（mod 自动检测；安装后默认开启，玩家可自行关闭；按 来源大类×内容小类 细分控制） ----
    public static final ForgeConfigSpec.BooleanValue ENABLE_TACZ_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TACZ_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TACZ_FORGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TACZ_EQUIP;
    // v0.6.20 Iron's Spells 'n Spellbooks 法术联动（按内容小类细分；未安装该 mod 时全部不生效）
    public static final ForgeConfigSpec.BooleanValue ENABLE_IRONS_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IRONS_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IRONS_FORGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IRONS_EQUIP;
    // v0.6.21 冰与火之歌 / Alex's Caves / 暮色森林 三联动（同结构：总开关 + 内容小类细分）
    public static final ForgeConfigSpec.BooleanValue ENABLE_IAF_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IAF_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IAF_FORGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_IAF_EQUIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AC_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AC_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AC_FORGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AC_EQUIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TF_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TF_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TF_FORGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TF_EQUIP;
    // v0.6.22 Alex's Mobs 联动（同结构：总开关 + 内容小类细分）
    public static final ForgeConfigSpec.BooleanValue ENABLE_AM_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AM_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AM_FORGE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_AM_EQUIP;
    // v0.8.4 第二批联动（8 组已装 mod）逐组开关：总开关 / 海克斯 / 装备（v0.8.3 为共享 MORE 开关，v0.8.4 拆分为每组独立）
    public static final ForgeConfigSpec.BooleanValue ENABLE_CATACLYSM_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CATACLYSM_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CATACLYSM_EQUIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GOETY_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GOETY_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_GOETY_EQUIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MOWZIESMOBS_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MOWZIESMOBS_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_MOWZIESMOBS_EQUIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ARTIFACTS_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ARTIFACTS_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_ARTIFACTS_EQUIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RELICS_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RELICS_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RELICS_EQUIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLASHBLADE_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLASHBLADE_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_SLASHBLADE_EQUIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FARMERSDELIGHT_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FARMERSDELIGHT_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FARMERSDELIGHT_EQUIP;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FTBQUESTS_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FTBQUESTS_HEXTECH;
    public static final ForgeConfigSpec.BooleanValue ENABLE_FTBQUESTS_EQUIP;
    // v0.8.3 ProjectE 等价交换联动（EMC A：充值页 EMC⇄晶币；EMC C：等价交换海克斯）
    public static final ForgeConfigSpec.BooleanValue ENABLE_EMC_CONTENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EMC_HEXTECH;

    // ---- 世界海克斯（v0.3.21；全服共享的全局增强/诅咒体系） ----
    public static final ForgeConfigSpec.BooleanValue WORLD_ENABLED;
    public static final ForgeConfigSpec.IntValue WORLD_DRAW_XP;
    public static final ForgeConfigSpec.IntValue WORLD_CANCEL_COIN;
    public static final ForgeConfigSpec.IntValue WORLD_MAX_PER_CATEGORY;

    // ---- v0.6.1 锻造档位倍率（服务端生效，防作弊；客户端展示读取同配置，单机/开服同进程一致） ----
    public static final ForgeConfigSpec.DoubleValue FORGE_MULT_SILVER;
    public static final ForgeConfigSpec.DoubleValue FORGE_MULT_GOLD;
    public static final ForgeConfigSpec.DoubleValue FORGE_MULT_PRISMATIC;
    // ---- v0.6.2 永久叠层全局倍率/上限（服务端生效） ----
    public static final ForgeConfigSpec.DoubleValue PERMANENT_STACK_MULT;
    public static final ForgeConfigSpec.IntValue PERMANENT_STACK_CAP;
    // ---- v0.6.16 配置表扩展：装备费用 / 叠角龙倍率 / 栏位扩展上限 / 六神装加成（服务端生效，防作弊） ----
    public static final ForgeConfigSpec.IntValue EQUIP_GOLD_COST;
    public static final ForgeConfigSpec.IntValue EQUIP_PRISMATIC_COST;
    public static final ForgeConfigSpec.DoubleValue STACK_DRAKE_MULT;
    public static final ForgeConfigSpec.IntValue FORGE_SLOT_MAX;
    public static final ForgeConfigSpec.DoubleValue EQUIP_COUNT_MULT;
    /** v0.6.16 玩家属性显示安全钳（默认 0=关闭）：高数值整合包中玩家 MAX_HEALTH/护甲/韧性被本 mod
     *  加成堆到极大时，部分血量 HUD mod 渲染会溢出（画面中央黄/红异常竖条）。开启后把玩家自身
     *  这三项属性最终值钳到该上限（不改变伤害/减伤公式，仅防显示溢出）；默认关闭=与其它 mod 零冲突。 */
    public static final ForgeConfigSpec.IntValue ATTR_DISPLAY_CAP;
    // ---- v0.6.20 法术联动数值（Iron's Spells 联动效果可调，服务端生效） ----
    /** 法术联动效果全局倍率：所有 spell_* 事件效果基础值 × 该倍率（默认 1.0，范围 0~10） */
    public static final ForgeConfigSpec.DoubleValue IRONS_EFFECT_MULT;
    /** 法术暴击倍率（暴击符文/影焰等触发暴击时的伤害倍率，默认 1.75） */
    public static final ForgeConfigSpec.DoubleValue IRONS_CRIT_MULT;
    /** 斩杀阈值：目标当前生命低于最大生命该比例时触发斩杀增伤（默认 0.30=30%） */
    public static final ForgeConfigSpec.DoubleValue IRONS_EXECUTE_THRESHOLD;
    /** 迅捷咏唱每级施法速度（注入 Iron's CAST_TIME_REDUCTION 属性，默认 0.08=8%/级） */
    public static final ForgeConfigSpec.DoubleValue IRONS_CAST_HASTE;
    /** 魔力储备每级魔力上限（注入 Iron's MAX_MANA 属性，默认 4/级） */
    public static final ForgeConfigSpec.DoubleValue IRONS_MANA_MAX;
    /** 法力泉涌每级魔力回复（注入 Iron's MANA_REGEN 属性，默认 0.5/级） */
    public static final ForgeConfigSpec.DoubleValue IRONS_MANA_REGEN;
    // ---- v0.6.21 三联动效果数值（冰与火 / Alex's Caves / 暮色森林，服务端生效） ----
    /** 冰与火联动效果全局倍率（默认 1.0） */
    public static final ForgeConfigSpec.DoubleValue IAF_EFFECT_MULT;
    /** Alex's Caves 联动效果全局倍率（默认 1.0） */
    public static final ForgeConfigSpec.DoubleValue AC_EFFECT_MULT;
    /** 暮色森林联动效果全局倍率（默认 1.0） */
    public static final ForgeConfigSpec.DoubleValue TF_EFFECT_MULT;
    // ---- v0.6.22 Alex's Mobs 联动效果数值（服务端生效） ----
    /** Alex's Mobs 联动效果全局倍率（默认 1.0） */
    public static final ForgeConfigSpec.DoubleValue AM_EFFECT_MULT;
    // ---- v0.8.3 ProjectE EMC 数值（服务端生效） ----
    /** EMC 折算晶币汇率：多少 EMC 换 1 晶币（默认 4：与充值页钻石等价——钻石 EMC 8192 / 4 = 2048 晶币 = 充值页钻石兑换价） */
    public static final ForgeConfigSpec.DoubleValue EMC_COIN_RATE;
    // ---- v0.6.22 防卡顿机制（服务端：提示广播节流） ----
    /** 服务端滚动提示广播节流（默认开启：同一玩家同一文本 500ms 内只发 1 次，狂刷的击杀/入账提示自动去重合并，降低网络与客户端渲染压力） */
    public static final ForgeConfigSpec.BooleanValue PERF_TIP_THROTTLE;
    // ---- HUD 优化（客户端）----
    /** v0.6.16 滚动提示总开关 */
    public static final ForgeConfigSpec.BooleanValue TIP_ENABLED;
    /** v0.6.16 滚动提示拖动调整模式：开启后在游戏内拖动提示位置，松开自动保存 */
    public static final ForgeConfigSpec.BooleanValue TIP_ADJUST_MODE;
    /** v0.6.16 滚动提示左上角 X 偏移（屏幕像素） */
    public static final ForgeConfigSpec.IntValue TIP_X;
    /** v0.6.16 滚动提示左上角 Y 偏移（屏幕像素） */
    public static final ForgeConfigSpec.IntValue TIP_Y;
    /** v0.6.19 海克斯风简易血条开关（默认关闭：取代原版生命条，开启后原版不再显示；关闭=与其它 mod 零冲突） */
    public static final ForgeConfigSpec.BooleanValue HP_ENABLED;
    /** v0.6.19 海克斯风血条 X 偏移（0=固定原版血条位置；>0 用配置值手动定位） */
    public static final ForgeConfigSpec.IntValue HP_X;
    /** v0.6.19 海克斯风血条 Y 偏移（0=固定原版血条位置；>0 用配置值手动定位） */
    public static final ForgeConfigSpec.IntValue HP_Y;
    // ---- v0.6.22 防卡顿机制（客户端：提示队列限流与合并，渲染端生效） ----
    /** 滚动提示队列上限（默认 6 条；0=不限制。超过上限的旧提示自动清出，狂刷提示时只保留最近 N 条） */
    public static final ForgeConfigSpec.IntValue PERF_TIP_CAP;
    /** 滚动提示同文本合并（默认开启：连续相同的提示合并为「文本 ×N」，不再逐条刷屏） */
    public static final ForgeConfigSpec.BooleanValue PERF_TIP_DEDUP;
    /** v0.7.6 滚动提示同屏最大条数（默认 5；1~8。队列中同时显示在屏幕上的最多条数） */
    public static final ForgeConfigSpec.IntValue PERF_TIP_MAX;
    // ---- v0.7.1 背包/合成界面页签位置（0=自动贴容器右上；>0 自定义，布局编辑器可拖动） ----
    public static final ForgeConfigSpec.IntValue TAB_X;
    public static final ForgeConfigSpec.IntValue TAB_Y;

    // ---- Client ----
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue UI_SCALE;
    // v0.6.3 抽取/锻造/购买完成后返回工坊界面（玩家可选：继续连抽 or 直接关闭）
    public static final ForgeConfigSpec.BooleanValue KEEP_WORKSHOP_OPEN;
    // v0.6.3 三选一「选中→确认」：true=点卡片仅选中，点「确认选择」才真正生效（防手滑）；false=点卡片直接选择
    public static final ForgeConfigSpec.BooleanValue CONFIRM_SELECT;
    public static final ForgeConfigSpec.BooleanValue PARTICLES_ENABLED;
    // v0.6.0 锻造/叠层规则（服务端生效）
    public static final ForgeConfigSpec.DoubleValue EXTRA_ARMOR_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue EXTRA_ARMOR_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue EXTRA_ARMOR_CAP;
    public static final ForgeConfigSpec.EnumValue<DuplicateRule> FORGE_DUPLICATE_RULE;
    // v0.6.5 随机馈赠低价值过滤（服务端生效；默认关闭=保持全物品池）
    public static final ForgeConfigSpec.BooleanValue RANDOM_GIFT_FILTER_LOW;
    // ---- 死亡保留（以客户端配置为主，v0.4.11）：主菜单即可设置并保存，进世界即生效 ----
    public static final ForgeConfigSpec.BooleanValue KEEP_DEATH_HEXES;
    public static final ForgeConfigSpec.BooleanValue KEEP_DEATH_FORGES;
    public static final ForgeConfigSpec.BooleanValue KEEP_DEATH_EQUIPS;
    public static final ForgeConfigSpec.BooleanValue KEEP_DEATH_COINS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("海克斯抽取").translation("hexcraft.config.cat.draw").push("draw");
        XP_COST_PER_DRAW = b.comment("每次海克斯抽取消耗的经验点数（≈19级为500xp）").translation("hexcraft.config.draw.xpCostPerDraw").defineInRange("xpCostPerDraw", 500, 10, 1000000);
        REROLL_COST = b.comment("三选一刷新一次消耗的晶币").translation("hexcraft.config.draw.rerollCost").defineInRange("rerollCost", 20, 0, 10000);
        // v0.6.16 海克斯基础上限拉高+范围放宽：默认 16（原 8），范围 0~9999（0=无限制）
        HEX_SLOT_COUNT = b.comment("海克斯槽位基础上限（0=无限制；默认 16；满员后通过『栏位扩展』海克斯或棱彩锻造器解锁栏位；已超上限的存量玩家不受限）").translation("hexcraft.config.draw.hexLimit").defineInRange("hexLimit", 16, 0, 9999);
        WEIGHT_SILVER = b.comment("白银稀有度权重").translation("hexcraft.config.draw.silverWeight").defineInRange("silverWeight", 40, 0, 1000);
        WEIGHT_GOLD = b.comment("黄金稀有度权重").translation("hexcraft.config.draw.goldWeight").defineInRange("goldWeight", 35, 0, 1000);
        WEIGHT_PRISMATIC = b.comment("棱彩稀有度权重").translation("hexcraft.config.draw.prismWeight").defineInRange("prismWeight", 25, 0, 1000);
        PITY_GOLD = b.comment("连续多少次未出黄金+则保底（0=关闭）").translation("hexcraft.config.draw.pityGoldDraws").defineInRange("pityGoldDraws", 4, 0, 100);
        PITY_PRISMATIC = b.comment("连续多少次未出棱彩则保底（0=关闭）").translation("hexcraft.config.draw.pityPrismDraws").defineInRange("pityPrismDraws", 8, 0, 200);
        MAX_HEXTECH_LEVEL = b.comment("同名海克斯最高等级").translation("hexcraft.config.draw.maxHextechLevel").defineInRange("maxHextechLevel", 3, 1, 10);
        b.pop();

        b.comment("经济系统").translation("hexcraft.config.cat.economy").push("economy");
        COIN_MIN = b.comment("普通怪击杀掉落晶币下限").translation("hexcraft.config.eco.coinMin").defineInRange("coinMin", 1, 0, 100);
        COIN_MAX = b.comment("普通怪击杀掉落晶币上限").translation("hexcraft.config.eco.coinMax").defineInRange("coinMax", 3, 0, 100);
        COIN_ELITE_MIN = b.comment("精英怪(生命≥40)掉落下限").translation("hexcraft.config.eco.coinEliteMin").defineInRange("coinEliteMin", 5, 0, 1000);
        COIN_ELITE_MAX = b.comment("精英怪(生命≥40)掉落上限").translation("hexcraft.config.eco.coinEliteMax").defineInRange("coinEliteMax", 10, 0, 1000);
        COIN_BOSS_MIN = b.comment("首领怪(生命≥120)掉落下限").translation("hexcraft.config.eco.coinBossMin").defineInRange("coinBossMin", 20, 0, 10000);
        COIN_BOSS_MAX = b.comment("首领怪(生命≥120)掉落上限").translation("hexcraft.config.eco.coinBossMax").defineInRange("coinBossMax", 50, 0, 10000);
        ANTI_FARM_WINDOW_SEC = b.comment("反刷窗口：窗口内重复击杀同种怪掉落衰减（0=关闭）").translation("hexcraft.config.eco.antiFarmWindowSec").defineInRange("antiFarmWindowSec", 30, 0, 600);
        ANTI_FARM_DECAY = b.comment("反刷衰减系数（0.2=掉落降至20%）").translation("hexcraft.config.eco.antiFarmDecay").defineInRange("antiFarmDecay", 0.2, 0.0, 1.0);
        FORGE_BASIC_PRICE = b.comment("基础锻造器价格(晶币)").translation("hexcraft.config.eco.forgeBasicPrice").defineInRange("forgeBasicPrice", 50, 0, 100000);
        FORGE_GOLD_PRICE = b.comment("黄金锻造器价格(晶币)").translation("hexcraft.config.eco.forgeGoldPrice").defineInRange("forgeGoldPrice", 150, 0, 100000);
        FORGE_PRISMATIC_PRICE = b.comment("棱彩锻造器价格(晶币)").translation("hexcraft.config.eco.forgePrismaticPrice").defineInRange("forgePrismaticPrice", 400, 0, 100000);
        MAX_FORGES = b.comment("锻造总次数上限（0=无限制，默认无限制）").translation("hexcraft.config.eco.forgeLimit").defineInRange("forgeLimit", 0, 0, 100000);
        REFUND_SILVER = b.comment("满级重复抽到白银海克斯的晶币补偿").translation("hexcraft.config.eco.refundSilver").defineInRange("refundSilver", 10, 0, 10000);
        REFUND_GOLD = b.comment("满级重复抽到黄金海克斯的晶币补偿").translation("hexcraft.config.eco.refundGold").defineInRange("refundGold", 30, 0, 10000);
        REFUND_PRISMATIC = b.comment("满级重复抽到棱彩海克斯的晶币补偿").translation("hexcraft.config.eco.refundPrismatic").defineInRange("refundPrismatic", 80, 0, 10000);
        b.pop();

        b.comment("联动内容：添加了对应 mod 后本 mod 自动检测并默认开启联动内容，可在此自行关闭；总开关 + 按内容小类（海克斯/锻造器/装备）细分控制，方便按需裁剪").translation("hexcraft.config.cat.linked").push("linked");
        ENABLE_TACZ_CONTENT = b.comment("总开关：启用 TACZ（永恒枪械工坊）联动内容（需安装 TACZ 才生效；关闭后联动海克斯/锻造词条/装备全部不进入抽取池，已拥有项保留显示）").translation("hexcraft.config.linked.enableTaczContent").define("enableTaczContent", true);
        ENABLE_TACZ_HEXTECH = b.comment("TACZ 联动·海克斯：控制 TACZ 联动海克斯是否进入抽取池").translation("hexcraft.config.linked.enableTaczHextech").define("enableTaczHextech", true);
        ENABLE_TACZ_FORGE = b.comment("TACZ 联动·属性锻造器：控制 TACZ 联动锻造词条（枪械伤害/枪械暴击/弹药回复）是否可被锻造").translation("hexcraft.config.linked.enableTaczForge").define("enableTaczForge", true);
        ENABLE_TACZ_EQUIP = b.comment("TACZ 联动·装备：控制 TACZ 联动装备是否进入装备抽取池").translation("hexcraft.config.linked.enableTaczEquip").define("enableTaczEquip", true);
        // v0.6.20 Iron's Spells 'n Spellbooks 法术联动（未安装该 mod 时全部不生效；安装后默认开启）
        ENABLE_IRONS_CONTENT = b.comment("总开关：启用 Iron's Spells 'n Spellbooks（法术与魔法书）联动内容（需安装 irons_spellbooks 才生效；关闭后联动海克斯/锻造词条/装备全部不进入抽取池，已拥有项保留显示）").translation("hexcraft.config.linked.enableIronsContent").define("enableIronsContent", true);
        ENABLE_IRONS_HEXTECH = b.comment("法术联动·海克斯：控制 29 个法术海克斯（施法/魔力/法术伤害/学校流派）是否进入抽取池").translation("hexcraft.config.linked.enableIronsHextech").define("enableIronsHextech", true);
        ENABLE_IRONS_FORGE = b.comment("法术联动·属性锻造器：控制法术词条（法术强度/魔力上限/魔力回复/冷却缩减/施法速度）是否可被锻造").translation("hexcraft.config.linked.enableIronsForge").define("enableIronsForge", true);
        ENABLE_IRONS_EQUIP = b.comment("法术联动·装备：控制法术联动装备（卢登/影焰/巫妖之祸及 LOL 法强装法术效果）是否进入装备抽取池").translation("hexcraft.config.linked.enableIronsEquip").define("enableIronsEquip", true);
        // v0.6.21 冰与火之歌（未安装时全部不生效；安装后默认开启）
        ENABLE_IAF_CONTENT = b.comment("总开关：启用 冰与火之歌（Ice and Fire）联动内容（需安装 iceandfire 才生效；关闭后联动海克斯/锻造词条/装备全部不进入抽取池，已拥有项保留显示）").translation("hexcraft.config.linked.enableIafContent").define("enableIafContent", true);
        ENABLE_IAF_HEXTECH = b.comment("冰与火·海克斯：控制 20 个龙族/BOSS 战斗海克斯（屠龙/海德拉/亡灵/各族增伤）是否进入抽取池").translation("hexcraft.config.linked.enableIafHextech").define("enableIafHextech", true);
        ENABLE_IAF_FORGE = b.comment("冰与火·锻造器：控制对龙增伤锻造词条是否可被锻造").translation("hexcraft.config.linked.enableIafForge").define("enableIafForge", true);
        ENABLE_IAF_EQUIP = b.comment("冰与火·装备：控制联动装备（饮魔刀/龙鳞甲）是否进入装备抽取池").translation("hexcraft.config.linked.enableIafEquip").define("enableIafEquip", true);
        // v0.6.21 Alex's Caves（未安装时全部不生效）
        ENABLE_AC_CONTENT = b.comment("总开关：启用 Alex's Caves 联动内容（需安装 alexscaves 才生效；关闭后联动海克斯/锻造词条/装备全部不进入抽取池）").translation("hexcraft.config.linked.enableAcContent").define("enableAcContent", true);
        ENABLE_AC_HEXTECH = b.comment("Alex's Caves·海克斯：控制 20 个洞穴克制流海克斯（辐射/毒性/深渊/史前/糖果/暗域）是否进入抽取池").translation("hexcraft.config.linked.enableAcHextech").define("enableAcHextech", true);
        ENABLE_AC_FORGE = b.comment("Alex's Caves·锻造器：控制辐射防护/深渊呼吸锻造词条是否可被锻造").translation("hexcraft.config.linked.enableAcForge").define("enableAcForge", true);
        ENABLE_AC_EQUIP = b.comment("Alex's Caves·装备：控制联动装备（辐射防护服/深渊潜航器）是否进入装备抽取池").translation("hexcraft.config.linked.enableAcEquip").define("enableAcEquip", true);
        // v0.6.21 暮色森林（未安装时全部不生效）
        ENABLE_TF_CONTENT = b.comment("总开关：启用 暮色森林（Twilight Forest）联动内容（需安装 twilightforest 才生效；关闭后联动海克斯/锻造词条/装备全部不进入抽取池）").translation("hexcraft.config.linked.enableTfContent").define("enableTfContent", true);
        ENABLE_TF_HEXTECH = b.comment("暮色森林·海克斯：控制 18 个 BOSS 进度/维度海克斯（弑王/逐王/暮色主宰）是否进入抽取池").translation("hexcraft.config.linked.enableTfHextech").define("enableTfHextech", true);
        ENABLE_TF_FORGE = b.comment("暮色森林·锻造器：控制对 BOSS 增伤锻造词条是否可被锻造").translation("hexcraft.config.linked.enableTfForge").define("enableTfForge", true);
        ENABLE_TF_EQUIP = b.comment("暮色森林·装备：控制联动装备（月晶碎片/暮色护符）是否进入装备抽取池").translation("hexcraft.config.linked.enableTfEquip").define("enableTfEquip", true);
        // v0.6.22 Alex's Mobs（未安装时全部不生效；安装后默认开启）
        ENABLE_AM_CONTENT = b.comment("总开关：启用 Alex's Mobs（Alex 的生物）联动内容（需安装 alexsmobs 才生效；关闭后联动海克斯/锻造词条/装备全部不进入抽取池，已拥有项保留显示）").translation("hexcraft.config.linked.enableAmContent").define("enableAmContent", true);
        ENABLE_AM_HEXTECH = b.comment("Alex's Mobs·海克斯：控制 16 个野兽克制/猎兽流海克斯（走兽/毒物/天禽/水生/巨兽/首领）是否进入抽取池").translation("hexcraft.config.linked.enableAmHextech").define("enableAmHextech", true);
        ENABLE_AM_FORGE = b.comment("Alex's Mobs·锻造器：控制猎兽锻造词条是否可被锻造").translation("hexcraft.config.linked.enableAmForge").define("enableAmForge", true);
        ENABLE_AM_EQUIP = b.comment("Alex's Mobs·装备：控制联动装备（兽骨项坠/荒野图腾）是否进入装备抽取池").translation("hexcraft.config.linked.enableAmEquip").define("enableAmEquip", true);
        // v0.8.4 第二批联动逐组开关（8 组已装 mod：末地灾变/巫术/Mowzie's Mobs/神器/遗物/拔刀剑/农夫乐事/FTB Quests；
        // 每组 = 总开关 + 海克斯 + 装备；未安装对应 mod 时自动隐藏不进入抽取池）
        ENABLE_CATACLYSM_CONTENT = b.comment("总开关：启用 Cataclysm（末地灾变）联动内容（需安装 cataclysm 才生效，已拥有项保留显示）").translation("hexcraft.config.linked.enableCataclysmContent").define("enableCataclysmContent", true);
        ENABLE_CATACLYSM_HEXTECH = b.comment("Cataclysm·海克斯：控制末地灾变克制/掠夺海克斯是否进入抽取池").translation("hexcraft.config.linked.enableCataclysmHextech").define("enableCataclysmHextech", true);
        ENABLE_CATACLYSM_EQUIP = b.comment("Cataclysm·装备：控制末地灾变主题装备是否进入装备抽取池").translation("hexcraft.config.linked.enableCataclysmEquip").define("enableCataclysmEquip", true);
        ENABLE_GOETY_CONTENT = b.comment("总开关：启用 Goety（巫术）联动内容（需安装 goety 才生效，已拥有项保留显示）").translation("hexcraft.config.linked.enableGoetyContent").define("enableGoetyContent", true);
        ENABLE_GOETY_HEXTECH = b.comment("Goety·海克斯：控制巫术系克制/灵魂海克斯是否进入抽取池").translation("hexcraft.config.linked.enableGoetyHextech").define("enableGoetyHextech", true);
        ENABLE_GOETY_EQUIP = b.comment("Goety·装备：控制巫术主题装备是否进入装备抽取池").translation("hexcraft.config.linked.enableGoetyEquip").define("enableGoetyEquip", true);
        ENABLE_MOWZIESMOBS_CONTENT = b.comment("总开关：启用 Mowzie's Mobs 联动内容（需安装 mowziesmobs 才生效，已拥有项保留显示）").translation("hexcraft.config.linked.enableMowziesmobsContent").define("enableMowziesmobsContent", true);
        ENABLE_MOWZIESMOBS_HEXTECH = b.comment("Mowzie's Mobs·海克斯：控制猎兽/冰霜克制海克斯是否进入抽取池").translation("hexcraft.config.linked.enableMowziesmobsHextech").define("enableMowziesmobsHextech", true);
        ENABLE_MOWZIESMOBS_EQUIP = b.comment("Mowzie's Mobs·装备：控制猎兽主题装备是否进入装备抽取池").translation("hexcraft.config.linked.enableMowziesmobsEquip").define("enableMowziesmobsEquip", true);
        ENABLE_ARTIFACTS_CONTENT = b.comment("总开关：启用 Artifacts（神器）联动内容（需安装 artifacts 才生效，已拥有项保留显示）").translation("hexcraft.config.linked.enableArtifactsContent").define("enableArtifactsContent", true);
        ENABLE_ARTIFACTS_HEXTECH = b.comment("Artifacts·海克斯：控制神器狂暴/汲血/伪箱海克斯是否进入抽取池").translation("hexcraft.config.linked.enableArtifactsHextech").define("enableArtifactsHextech", true);
        ENABLE_ARTIFACTS_EQUIP = b.comment("Artifacts·装备：控制神器主题装备是否进入装备抽取池").translation("hexcraft.config.linked.enableArtifactsEquip").define("enableArtifactsEquip", true);
        ENABLE_RELICS_CONTENT = b.comment("总开关：启用 Relics（遗物）联动内容（需安装 relics 才生效，已拥有项保留显示）").translation("hexcraft.config.linked.enableRelicsContent").define("enableRelicsContent", true);
        ENABLE_RELICS_HEXTECH = b.comment("Relics·海克斯：控制遗物共鸣/守护/猎人海克斯是否进入抽取池").translation("hexcraft.config.linked.enableRelicsHextech").define("enableRelicsHextech", true);
        ENABLE_RELICS_EQUIP = b.comment("Relics·装备：控制遗物主题装备是否进入装备抽取池").translation("hexcraft.config.linked.enableRelicsEquip").define("enableRelicsEquip", true);
        ENABLE_SLASHBLADE_CONTENT = b.comment("总开关：启用 SlashBlade（拔刀剑）联动内容（需安装 slashblade 才生效，已拥有项保留显示）").translation("hexcraft.config.linked.enableSlashbladeContent").define("enableSlashbladeContent", true);
        ENABLE_SLASHBLADE_HEXTECH = b.comment("SlashBlade·海克斯：控制拔刀术/居合/刀魂海克斯是否进入抽取池").translation("hexcraft.config.linked.enableSlashbladeHextech").define("enableSlashbladeHextech", true);
        ENABLE_SLASHBLADE_EQUIP = b.comment("SlashBlade·装备：控制拔刀主题装备是否进入装备抽取池").translation("hexcraft.config.linked.enableSlashbladeEquip").define("enableSlashbladeEquip", true);
        ENABLE_FARMERSDELIGHT_CONTENT = b.comment("总开关：启用农夫乐事（FarmersDelight）联动内容（需安装 farmersdelight 才生效，已拥有项保留显示）").translation("hexcraft.config.linked.enableFarmersdelightContent").define("enableFarmersdelightContent", true);
        ENABLE_FARMERSDELIGHT_HEXTECH = b.comment("农夫乐事·海克斯：控制丰收掠夺/盛宴/农家小灶海克斯是否进入抽取池").translation("hexcraft.config.linked.enableFarmersdelightHextech").define("enableFarmersdelightHextech", true);
        ENABLE_FARMERSDELIGHT_EQUIP = b.comment("农夫乐事·装备：控制农夫主题装备是否进入装备抽取池").translation("hexcraft.config.linked.enableFarmersdelightEquip").define("enableFarmersdelightEquip", true);
        ENABLE_FTBQUESTS_CONTENT = b.comment("总开关：启用 FTB Quests（任务）联动内容（需安装 ftbquests 才生效，已拥有项保留显示）").translation("hexcraft.config.linked.enableFtbquestsContent").define("enableFtbquestsContent", true);
        ENABLE_FTBQUESTS_HEXTECH = b.comment("FTB Quests·海克斯：控制任务达人/任务之神/任务补给海克斯是否进入抽取池").translation("hexcraft.config.linked.enableFtbquestsHextech").define("enableFtbquestsHextech", true);
        ENABLE_FTBQUESTS_EQUIP = b.comment("FTB Quests·装备：控制任务主题装备是否进入装备抽取池").translation("hexcraft.config.linked.enableFtbquestsEquip").define("enableFtbquestsEquip", true);
        // v0.8.3 ProjectE 等价交换联动（EMC A：充值页 EMC⇄晶币 折算通道；EMC C：3 个等价交换海克斯）
        ENABLE_EMC_CONTENT = b.comment("总开关：启用 ProjectE 等价交换联动（需安装 projecte 才生效；EMC 充值通道 + 3 个等价交换海克斯；未安装时充值页不显示 EMC 通道、海克斯不进入抽取池）").translation("hexcraft.config.linked.enableEmcContent").define("enableEmcContent", true);
        ENABLE_EMC_HEXTECH = b.comment("ProjectE·海克斯：控制等价交换海克斯（点石成金/商贾之道/财富之力）是否进入抽取池").translation("hexcraft.config.linked.enableEmcHextech").define("enableEmcHextech", true);
        b.pop();

        b.comment("世界海克斯：全服共享的全局增强/诅咒体系（背包第二页签进入；抽取消耗经验、取消消耗晶币）").translation("hexcraft.config.cat.world").push("world");
        WORLD_ENABLED = b.comment("世界海克斯总开关（关闭后无法抽取/取消，已生效的保留）").translation("hexcraft.config.world.enabled").define("enabled", true);
        WORLD_DRAW_XP = b.comment("世界海克斯抽取一次消耗的经验点数（创世契约等 draw_cost 效果可再打折）").translation("hexcraft.config.world.drawXp").defineInRange("drawXp", 2000, 10, 1000000);
        WORLD_CANCEL_COIN = b.comment("取消一个世界海克斯消耗的晶币代价").translation("hexcraft.config.world.cancelCoin").defineInRange("cancelCoin", 800, 0, 100000);
        WORLD_MAX_PER_CATEGORY = b.comment("每个分类（通用/友好/中立/敌对）同时最多激活的世界海克斯数量（0=无限制；v0.6.16 起默认 16，范围放宽 0~9999）").translation("hexcraft.config.world.maxPerCategory").defineInRange("maxPerCategory", 16, 0, 9999);
        b.pop();

        b.comment("v0.6.0 锻造与叠层规则（服务端生效）").translation("hexcraft.config.cat.v060").push("v060");
        EXTRA_ARMOR_THRESHOLD = b.comment("额外护甲减伤阈值：护甲超过该值（原版满减伤约 25 点）的部分才享受额外减伤").translation("hexcraft.config.v060.extraArmorThreshold").defineInRange("extraArmorThreshold", 25.0, 0.0, 200.0);
        EXTRA_ARMOR_PER_POINT = b.comment("额外护甲减伤：超出阈值的每点护甲提供 X% 减伤（独立乘区，与护甲曲线相乘）").translation("hexcraft.config.v060.extraArmorPerPoint").defineInRange("extraArmorPerPoint", 0.5, 0.0, 5.0);
        EXTRA_ARMOR_CAP = b.comment("额外护甲减伤上限（%）").translation("hexcraft.config.v060.extraArmorCap").defineInRange("extraArmorCap", 25.0, 0.0, 80.0);
        FORGE_DUPLICATE_RULE = b.comment("同词条锻造叠加规则：STACK=无上限叠加（默认）；BEST_ONLY=同名词条仅保留最高档位（低档重复自动回收）").translation("hexcraft.config.v060.forgeDuplicateRule").defineEnum("forgeDuplicateRule", DuplicateRule.STACK);
        // v0.6.5 随机馈赠低价值过滤（服务端生效；默认关闭保持全物品池）
        RANDOM_GIFT_FILTER_LOW = b.comment("随机馈赠过滤低价值物品（服务端生效，默认关闭=保持原版+全部模组全物品池；开启后过滤泥土/圆石/原木/羊毛/陶瓦等建材装饰类原版低价值物品，模组物品不判定）").translation("hexcraft.config.v060.randomGiftFilterLow").define("randomGiftFilterLow", false);
        // v0.6.7 倍率上限收紧：原上限 20 倍会让「锻造条数 × 档位倍率 × 增幅乘区」相乘轻松上千，属数值爆炸源；
        // 收紧到 5 倍（已改超限配置会被 Forge 自动钳制到新上限），默认值 1/2/3 不变
        FORGE_MULT_SILVER = b.comment("白银锻造倍率（服务端生效，默认 1.0；修改后 tooltip 实际加成随之变化）").translation("hexcraft.config.v060.forgeMultSilver").defineInRange("forgeMultiplierSilver", 1.0, 0.1, 5.0);
        FORGE_MULT_GOLD = b.comment("黄金锻造倍率（服务端生效，默认 2.0；修改后 tooltip 实际加成随之变化）").translation("hexcraft.config.v060.forgeMultGold").defineInRange("forgeMultiplierGold", 2.0, 0.1, 5.0);
        FORGE_MULT_PRISMATIC = b.comment("棱彩锻造倍率（服务端生效，默认 3.0；修改后 tooltip 实际加成随之变化）").translation("hexcraft.config.v060.forgeMultPrismatic").defineInRange("forgeMultiplierPrismatic", 3.0, 0.1, 5.0);
        // v0.6.2 永久叠层全局倍率/上限（服务端权威，客户端修改不生效防作弊）
        PERMANENT_STACK_MULT = b.comment("永久叠层全局获取倍率（服务端生效，默认 1.0；叠角龙/世界海克斯等永久叠层海克斯击杀获取层数 × 该倍率）").translation("hexcraft.config.v060.permanentStackMult").defineInRange("permanentStackGlobalMultiplier", 1.0, 0.1, 10.0);
        PERMANENT_STACK_CAP = b.comment("永久叠层全局最大上限（服务端生效，默认 200；0=无限制；超过该值的永久叠层被钳制，防无上限叠层失控）").translation("hexcraft.config.v060.permanentStackCap").defineInRange("permanentStackGlobalCap", 200, 0, 9999);
        // v0.6.16 配置表扩展（服务端生效防作弊）
        EQUIP_GOLD_COST = b.comment("黄金装备购买费用（晶币；原硬编码 2500，现可配置）").translation("hexcraft.config.v060.equipGoldCost").defineInRange("equipGoldCost", 2500, 0, 1000000);
        EQUIP_PRISMATIC_COST = b.comment("棱彩装备购买费用（晶币；原硬编码 4000，现可配置）").translation("hexcraft.config.v060.equipPrismaticCost").defineInRange("equipPrismaticCost", 4000, 0, 1000000);
        STACK_DRAKE_MULT = b.comment("叠角龙每级永久叠层加成（默认 0.75 = 每级 +75%，1 级 1.75×/2 级 2.5×/3 级 3.25×；原硬编码 0.75，现可配置）").translation("hexcraft.config.v060.stackDrakeMult").defineInRange("stackDrakeMult", 0.75, 0.0, 3.0);
        FORGE_SLOT_MAX = b.comment("棱彩锻造器『栏位扩展』词条可叠加上限（默认 99，范围 0~9999；原硬编码 99，现可配置）").translation("hexcraft.config.v060.forgeSlotMax").defineInRange("forgeSlotMax", 99, 0, 9999);
        EQUIP_COUNT_MULT = b.comment("六神装加成倍率：装备海克斯按已装备件数给予的全局攻击/护甲/生命加成系数（默认 1.0；原硬编码，现可配置）").translation("hexcraft.config.v060.equipCountMult").defineInRange("equipCountMult", 1.0, 0.0, 10.0);
        // v0.6.16 玩家属性显示安全钳（默认 0=关闭）：高数值整合包中玩家属性被堆到极大时部分血量 HUD mod
        // 渲染溢出（屏幕中央黄/红异常竖条），开启后钳玩家自身 MAX_HEALTH/护甲/韧性到该值；0=完全关闭
        ATTR_DISPLAY_CAP = b.comment("玩家属性显示安全钳（默认 0=关闭；开启后钳玩家自身最大生命/护甲/韧性到该值，防整合包血量 HUD 渲染溢出；仅防显示问题，不影响减伤公式）").translation("hexcraft.config.v060.attrDisplayCap").defineInRange("attrDisplayCap", 0, 0, 100000000);
        // v0.6.20 法术联动数值（Iron's Spells 联动效果可调，服务端生效）
        IRONS_EFFECT_MULT = b.comment("法术联动效果全局倍率（所有法术海克斯/装备事件效果基础值 × 该倍率；默认 1.0，调大更爽）").translation("hexcraft.config.v060.ironsEffectMult").defineInRange("ironsEffectMult", 1.0, 0.0, 10.0);
        IRONS_CRIT_MULT = b.comment("法术暴击倍率（暴击符文/影焰等触发法术暴击时的伤害倍率，默认 1.75）").translation("hexcraft.config.v060.ironsCritMult").defineInRange("ironsCritMult", 1.75, 1.0, 5.0);
        IRONS_EXECUTE_THRESHOLD = b.comment("斩杀阈值（目标当前生命低于最大生命该比例时触发斩杀增伤，默认 0.30=30%）").translation("hexcraft.config.v060.ironsExecuteThreshold").defineInRange("ironsExecuteThreshold", 0.30, 0.01, 1.0);
        IRONS_CAST_HASTE = b.comment("迅捷咏唱每级施法速度（注入 Iron's 施法时间缩减属性，默认 0.08=每级 -8% 施法时间）").translation("hexcraft.config.v060.ironsCastHaste").defineInRange("ironsCastHaste", 0.08, 0.0, 1.0);
        IRONS_MANA_MAX = b.comment("魔力储备每级魔力上限（注入 Iron's 魔力上限属性，默认 4/级）").translation("hexcraft.config.v060.ironsManaMax").defineInRange("ironsManaMax", 4.0, 0.0, 1000.0);
        IRONS_MANA_REGEN = b.comment("法力泉涌每级魔力回复（注入 Iron's 魔力回复属性，默认 0.5/级）").translation("hexcraft.config.v060.ironsManaRegen").defineInRange("ironsManaRegen", 0.5, 0.0, 100.0);
        // v0.6.21 三联动效果倍率（服务端生效；对应 mod 未安装时无实际效果）
        IAF_EFFECT_MULT = b.comment("冰与火联动效果全局倍率（所有冰与火海克斯/装备事件效果 × 该倍率，默认 1.0）").translation("hexcraft.config.v060.iafEffectMult").defineInRange("iafEffectMult", 1.0, 0.0, 10.0);
        AC_EFFECT_MULT = b.comment("Alex's Caves 联动效果全局倍率（默认 1.0）").translation("hexcraft.config.v060.acEffectMult").defineInRange("acEffectMult", 1.0, 0.0, 10.0);
        TF_EFFECT_MULT = b.comment("暮色森林联动效果全局倍率（默认 1.0）").translation("hexcraft.config.v060.tfEffectMult").defineInRange("tfEffectMult", 1.0, 0.0, 10.0);
        // v0.6.22 Alex's Mobs 联动效果倍率（服务端生效；未安装该 mod 时无实际效果）
        AM_EFFECT_MULT = b.comment("Alex's Mobs 联动效果全局倍率（默认 1.0）").translation("hexcraft.config.v060.amEffectMult").defineInRange("amEffectMult", 1.0, 0.0, 10.0);
        // v0.8.3 ProjectE EMC 数值（服务端生效）
        EMC_COIN_RATE = b.comment("EMC 折算晶币汇率：多少 EMC 换 1 晶币（默认 4；钻石 EMC 8192 ÷ 4 = 2048 晶币，与充值页钻石兑换价一致。EMC 通道与等价交换海克斯共用此汇率）").translation("hexcraft.config.v060.emcCoinRate").defineInRange("emcCoinRate", 4.0, 1.0, 1000000.0);
        // v0.6.22 防卡顿：服务端滚动提示广播节流（默认开启：同玩家同文本 500ms 内只广播 1 次，
        // 狂刷提示自动去重合并，降低联机网络与客户端渲染压力）
        PERF_TIP_THROTTLE = b.comment("提示广播节流（服务端生效，默认开启：同玩家同文本 500ms 内只发 1 条，击杀/入账狂刷时自动去重，防联机卡顿）").translation("hexcraft.config.v060.perfTipThrottle").define("perfTipThrottle", true);
        b.pop();

        COMMON_SPEC = b.build();
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        UI_SCALE = b.comment("UI缩放倍率").translation("hexcraft.config.client.uiScale").defineInRange("uiScale", 1.0, 0.5, 2.0);
        PARTICLES_ENABLED = b.comment("抽取/入账特效开关").translation("hexcraft.config.client.particlesEnabled").define("particlesEnabled", true);
        // v0.6.3 抽取/锻造/购买完成后：true=返回工坊对应页面继续抽取（连抽方便）；false=直接关闭整个界面
        KEEP_WORKSHOP_OPEN = b.comment("抽取/锻造完成后返回工坊界面（默认开启，方便连续抽取；关闭则完成后直接关闭界面）").translation("hexcraft.config.client.keepWorkshopOpen").define("keepWorkshopOpen", true);
        CONFIRM_SELECT = b.comment("三选一需确认选择（默认开启，防手滑：点卡片仅选中，点确认选择才生效；关闭=点卡片直接选择）").translation("hexcraft.config.client.confirmSelect").define("confirmSelect", true);
        b.comment("死亡保留（客户端配置为主：主菜单即可设置并保存，进世界即生效；服务端 gamerule 任一开启即保留）").translation("hexcraft.config.cat.keepOnDeath").push("keepOnDeath");
        KEEP_DEATH_HEXES = b.comment("死亡保留海克斯").translation("hexcraft.config.client.keepDeathHexes").define("keepDeathHexes", false);
        KEEP_DEATH_FORGES = b.comment("死亡保留锻造属性").translation("hexcraft.config.client.keepDeathForges").define("keepDeathForges", false);
        KEEP_DEATH_EQUIPS = b.comment("死亡保留装备").translation("hexcraft.config.client.keepDeathEquips").define("keepDeathEquips", false);
        KEEP_DEATH_COINS = b.comment("死亡保留晶币").translation("hexcraft.config.client.keepDeathCoins").define("keepDeathCoins", false);
        b.pop();
        // v0.6.16 HUD 优化：滚动提示开关 / 位置 / 拖动调整模式（客户端生效，保存后进世界即用）
        b.comment("HUD 优化：滚动提示显示与位置（客户端生效；可在配置界面的 HUD 优化分组调整）").translation("hexcraft.config.cat.v060hud").push("v060hud");
        TIP_ENABLED = b.comment("滚动提示总开关（默认开启：随机馈赠/入账等滚动消息；关闭后不再显示）").translation("hexcraft.config.v060.tipEnabled").define("tipEnabled", true);
        TIP_ADJUST_MODE = b.comment("提示拖动调整模式（默认关闭：开启后回到游戏，鼠标按住提示区域即可拖动位置，松开自动保存；调好后再关闭本项）").translation("hexcraft.config.v060.tipAdjustMode").define("tipAdjustMode", false);
        TIP_X = b.comment("滚动提示左上角 X 偏移（屏幕像素，默认 8；也可用拖动调整模式修改）").translation("hexcraft.config.v060.tipX").defineInRange("tipX", 8, 0, 4000);
        TIP_Y = b.comment("滚动提示左上角 Y 偏移（屏幕像素，默认 34；也可用拖动调整模式修改）").translation("hexcraft.config.v060.tipY").defineInRange("tipY", 34, 0, 4000);
        // v0.6.16 海克斯风简易血条：方框内显示「当前生命/生命上限」（取代原版血条；默认关闭）
        HP_ENABLED = b.comment("海克斯风简易血条（默认关闭；开启后取代原版血条——原版生命条不再显示，屏幕显示「当前生命/生命上限」方框）").translation("hexcraft.config.v060.hpEnabled").define("hpEnabled", false);
        HP_X = b.comment("血条 X 偏移（默认 0=自动右上角；填正数用自定义值）").translation("hexcraft.config.v060.hpX").defineInRange("hpX", 0, 0, 4000);
        HP_Y = b.comment("血条 Y 偏移（默认 0=自动；填正数用自定义值）").translation("hexcraft.config.v060.hpY").defineInRange("hpY", 0, 0, 4000);
        // v0.6.22 防卡顿（客户端渲染端生效）：提示队列上限 + 同文本合并
        PERF_TIP_CAP = b.comment("滚动提示队列上限（默认 6；0=不限制。超过上限自动清出最旧提示，狂刷时只保留最近 N 条，防提示堆叠卡顿）").translation("hexcraft.config.v060.perfTipCap").defineInRange("perfTipCap", 6, 0, 100);
        PERF_TIP_DEDUP = b.comment("滚动提示同文本合并（默认开启：连续相同的提示合并为「文本 ×N」，不再逐条刷屏）").translation("hexcraft.config.v060.perfTipDedup").define("perfTipDedup", true);
        PERF_TIP_MAX = b.comment("滚动提示同屏最大条数（默认 5；1~8。队列中同时显示在屏幕上的最多条数）").translation("hexcraft.config.v060.perfTipMax").defineInRange("perfTipMax", 5, 1, 8);
        // v0.7.1 背包/合成界面页签位置（默认 0=自动右上角：海克斯「海」guiLeft+124、世界海克斯「世」guiLeft+150，y=guiTop-28）
        TAB_X = b.comment("背包页签 X 偏移（默认 0=自动贴容器右上；填正数用自定义值，可用界面布局编辑器拖动）").translation("hexcraft.config.v070.tabX").defineInRange("tabX", 0, 0, 4000);
        TAB_Y = b.comment("背包页签 Y 偏移（默认 0=自动容器上方 28px；填正数向下移，用界面布局编辑器拖动）").translation("hexcraft.config.v070.tabY").defineInRange("tabY", 0, 0, 4000);
        b.pop();
        CLIENT_SPEC = b.build();
    }

    /** v0.6.0 同词条叠加规则（服务端）：STACK=无上限叠加（默认，延续无上限锻造）；
     *  BEST_ONLY=同名词条仅保留最高档位/最高等级（低档重复锻造自动回收为晶币） */
    public enum DuplicateRule { STACK, BEST_ONLY }
}
