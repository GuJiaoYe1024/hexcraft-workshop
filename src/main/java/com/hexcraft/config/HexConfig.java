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

    // ---- 世界海克斯（v0.3.21；全服共享的全局增强/诅咒体系） ----
    public static final ForgeConfigSpec.BooleanValue WORLD_ENABLED;
    public static final ForgeConfigSpec.IntValue WORLD_DRAW_XP;
    public static final ForgeConfigSpec.IntValue WORLD_CANCEL_COIN;
    public static final ForgeConfigSpec.IntValue WORLD_MAX_PER_CATEGORY;

    // ---- v0.6.1 锻造档位倍率（服务端生效，防作弊；客户端展示读取同配置，单机/开服同进程一致） ----
    public static final ForgeConfigSpec.DoubleValue FORGE_MULT_SILVER;
    public static final ForgeConfigSpec.DoubleValue FORGE_MULT_GOLD;
    public static final ForgeConfigSpec.DoubleValue FORGE_MULT_PRISMATIC;

    // ---- Client ----
    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.DoubleValue UI_SCALE;
    public static final ForgeConfigSpec.BooleanValue PARTICLES_ENABLED;
    // v0.6.0 锻造/叠层规则（服务端生效）
    public static final ForgeConfigSpec.DoubleValue EXTRA_ARMOR_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue EXTRA_ARMOR_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue EXTRA_ARMOR_CAP;
    public static final ForgeConfigSpec.EnumValue<DuplicateRule> FORGE_DUPLICATE_RULE;
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
        HEX_SLOT_COUNT = b.comment("海克斯槽位上限（0=无限制，默认无限制）").translation("hexcraft.config.draw.hexLimit").defineInRange("hexLimit", 0, 0, 30);
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
        b.pop();

        b.comment("世界海克斯：全服共享的全局增强/诅咒体系（背包第二页签进入；抽取消耗经验、取消消耗晶币）").translation("hexcraft.config.cat.world").push("world");
        WORLD_ENABLED = b.comment("世界海克斯总开关（关闭后无法抽取/取消，已生效的保留）").translation("hexcraft.config.world.enabled").define("enabled", true);
        WORLD_DRAW_XP = b.comment("世界海克斯抽取一次消耗的经验点数（创世契约等 draw_cost 效果可再打折）").translation("hexcraft.config.world.drawXp").defineInRange("drawXp", 2000, 10, 1000000);
        WORLD_CANCEL_COIN = b.comment("取消一个世界海克斯消耗的晶币代价").translation("hexcraft.config.world.cancelCoin").defineInRange("cancelCoin", 800, 0, 100000);
        WORLD_MAX_PER_CATEGORY = b.comment("每个分类（通用/友好/中立/敌对）同时最多激活的世界海克斯数量（0=无限制；v0.4.0 起默认 8）").translation("hexcraft.config.world.maxPerCategory").defineInRange("maxPerCategory", 8, 0, 32);
        b.pop();

        b.comment("v0.6.0 锻造与叠层规则（服务端生效）").translation("hexcraft.config.cat.v060").push("v060");
        EXTRA_ARMOR_THRESHOLD = b.comment("额外护甲减伤阈值：护甲超过该值（原版满减伤约 25 点）的部分才享受额外减伤").translation("hexcraft.config.v060.extraArmorThreshold").defineInRange("extraArmorThreshold", 25.0, 0.0, 200.0);
        EXTRA_ARMOR_PER_POINT = b.comment("额外护甲减伤：超出阈值的每点护甲提供 X% 减伤（独立乘区，与护甲曲线相乘）").translation("hexcraft.config.v060.extraArmorPerPoint").defineInRange("extraArmorPerPoint", 0.5, 0.0, 5.0);
        EXTRA_ARMOR_CAP = b.comment("额外护甲减伤上限（%）").translation("hexcraft.config.v060.extraArmorCap").defineInRange("extraArmorCap", 25.0, 0.0, 80.0);
        FORGE_DUPLICATE_RULE = b.comment("同词条锻造叠加规则：STACK=无上限叠加（默认）；BEST_ONLY=同名词条仅保留最高档位（低档重复自动回收）").translation("hexcraft.config.v060.forgeDuplicateRule").defineEnum("forgeDuplicateRule", DuplicateRule.STACK);
        FORGE_MULT_SILVER = b.comment("白银锻造倍率（服务端生效，默认 1.0；修改后 tooltip 实际加成随之变化）").translation("hexcraft.config.v060.forgeMultSilver").defineInRange("forgeMultiplierSilver", 1.0, 0.1, 20.0);
        FORGE_MULT_GOLD = b.comment("黄金锻造倍率（服务端生效，默认 2.0；修改后 tooltip 实际加成随之变化）").translation("hexcraft.config.v060.forgeMultGold").defineInRange("forgeMultiplierGold", 2.0, 0.1, 20.0);
        FORGE_MULT_PRISMATIC = b.comment("棱彩锻造倍率（服务端生效，默认 3.0；修改后 tooltip 实际加成随之变化）").translation("hexcraft.config.v060.forgeMultPrismatic").defineInRange("forgeMultiplierPrismatic", 3.0, 0.1, 20.0);
        b.pop();

        COMMON_SPEC = b.build();
    }

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        UI_SCALE = b.comment("UI缩放倍率").translation("hexcraft.config.client.uiScale").defineInRange("uiScale", 1.0, 0.5, 2.0);
        PARTICLES_ENABLED = b.comment("抽取/入账特效开关").translation("hexcraft.config.client.particlesEnabled").define("particlesEnabled", true);
        b.comment("死亡保留（客户端配置为主：主菜单即可设置并保存，进世界即生效；服务端 gamerule 任一开启即保留）").translation("hexcraft.config.cat.keepOnDeath").push("keepOnDeath");
        KEEP_DEATH_HEXES = b.comment("死亡保留海克斯").translation("hexcraft.config.client.keepDeathHexes").define("keepDeathHexes", false);
        KEEP_DEATH_FORGES = b.comment("死亡保留锻造属性").translation("hexcraft.config.client.keepDeathForges").define("keepDeathForges", false);
        KEEP_DEATH_EQUIPS = b.comment("死亡保留装备").translation("hexcraft.config.client.keepDeathEquips").define("keepDeathEquips", false);
        KEEP_DEATH_COINS = b.comment("死亡保留晶币").translation("hexcraft.config.client.keepDeathCoins").define("keepDeathCoins", false);
        b.pop();
        CLIENT_SPEC = b.build();
    }

    /** v0.6.0 同词条叠加规则（服务端）：STACK=无上限叠加（默认，延续无上限锻造）；
     *  BEST_ONLY=同名词条仅保留最高档位/最高等级（低档重复锻造自动回收为晶币） */
    public enum DuplicateRule { STACK, BEST_ONLY }
}
