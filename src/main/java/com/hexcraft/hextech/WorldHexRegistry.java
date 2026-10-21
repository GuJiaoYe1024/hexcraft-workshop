package com.hexcraft.hextech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 世界海克斯注册表（内置定义，代码驱动）：
 *  32 个世界海克斯 = 通用 8 + 友好 8 + 中立 8 + 敌对 8；每类白银 2 / 黄金 3 / 棱彩 3。
 *  设计取向：多数提升世界难度（加强生物、诅咒玩家），少数对玩家有利，棱彩最难最强。
 *  v0.4.1 炼狱难度再拉高：全量数值/概率大幅上调（高属性、高百分比加成），机制强度随概率档位升级。
 *  独立运算、独立乘区：效果全部走独立 AttributeModifier UUID 或事件独立倍率，不与其它 mod 冲突。
 */
public class WorldHexRegistry {
    public static final String CAT_GENERAL = "general";
    public static final String CAT_FRIENDLY = "friendly";
    public static final String CAT_NEUTRAL = "neutral";
    public static final String CAT_HOSTILE = "hostile";
    public static final String[] CATEGORIES = {CAT_GENERAL, CAT_FRIENDLY, CAT_NEUTRAL, CAT_HOSTILE};

    private static final Map<String, WorldHexDefinition> DEFINITIONS = new LinkedHashMap<>();

    static {
        // ============ 通用（影响全体玩家/世界）============
        register("whx_xp_friend", CAT_GENERAL, "silver", "player_xp:0.15");                 // 经验亲和（友好，v0.4.1 上调）
        register("whx_coin_tax", CAT_GENERAL, "silver", "player_coin:-0.30");               // 晶币税（诅咒，v0.4.1 加重）
        register("whx_dmg_shortage", CAT_GENERAL, "gold", "player_dmg:-0.25");              // 补给匮乏（诅咒，v0.4.1 加重）
        register("whx_life_echo", CAT_GENERAL, "gold", "player_hp:10.0");                   // 生命共鸣（友好，v0.4.1 上调）
        register("whx_eternal_flame", CAT_GENERAL, "gold", "player_taken:0.30");            // 永燃诅咒（高机制：全体受伤 +30%）
        register("whx_ancient_weakness", CAT_GENERAL, "prismatic", "player_hp:-15.0,player_dmg:-0.18"); // 远古衰弱（大诅咒，v0.4.1 加重）
        register("whx_death_harvest", CAT_GENERAL, "prismatic", "player_dmg:-0.25,death_xp:1.50");       // 死亡收割（高机制：输出-25% + 死亡经验惩罚 2.5 倍）
        register("whx_genesis_pact", CAT_GENERAL, "prismatic", "player_xp:0.50,draw_cost:0.40");        // 创世契约（友好双刃，v0.4.1 上调）

        // ============ 友好生物（被动/驯养）============
        register("whx_pasture_harmony", CAT_FRIENDLY, "silver", "passive_speed:0.15");      // 牧场丰饶（友好，v0.4.1 上调）
        register("whx_herd_watchful", CAT_FRIENDLY, "silver", "passive_no_drop:0.75");      // 家畜警觉（诅咒，v0.4.1 加重）
        register("whx_loyal_guard", CAT_FRIENDLY, "gold", "passive_atk:3.0");               // 忠诚守护（友好，v0.4.1 上调）
        register("whx_flock_shadow", CAT_FRIENDLY, "gold", "passive_hp:-10.0");             // 牧群阴影（诅咒，v0.4.1 加重）
        register("whx_pacifist_oath", CAT_FRIENDLY, "gold", "passive_no_drop:1.00");        // 和平契约（高机制：友好生物 100% 不掉落）
        register("whx_beast_king", CAT_FRIENDLY, "prismatic", "passive_hp:14.0,passive_atk:4.0");      // 万兽之王（难度，v0.4.1 大幅上调）
        register("whx_angel_grace", CAT_FRIENDLY, "prismatic", "tamed_regen:1.5");          // 天使眷顾（友好，v0.4.1 上调）
        register("whx_wrath_of_fields", CAT_FRIENDLY, "prismatic", "passive_berserk:1.00"); // 田野之怒（高机制：友好生物受击 100% 力量+速度反扑）

        // ============ 中立生物（末影人/猪灵/狼等）============
        register("whx_wild_spirit", CAT_NEUTRAL, "silver", "neutral_atk:3.0");              // 野性难驯（诅咒，v0.4.1 加重）
        register("whx_vigilant_eye", CAT_NEUTRAL, "silver", "neutral_follow:0.60");         // 警惕之眼（难度，v0.4.1 上调）
        register("whx_pack_rage", CAT_NEUTRAL, "gold", "neutral_atk:4.5");                  // 兽群狂暴（难度，v0.4.1 加重）
        register("whx_hunter_eye", CAT_NEUTRAL, "gold", "player_dmg_vs_neutral:0.30");      // 猎人之眼（友好，v0.4.1 上调）
        register("whx_wild_instinct", CAT_NEUTRAL, "gold", "neutral_berserk:0.75");         // 野性本能（高机制：中立生物受击 75% 力量+速度反击）
        register("whx_apex_presence", CAT_NEUTRAL, "prismatic", "neutral_hp:24.0,neutral_speed:0.40"); // 终焉威压（难度，v0.4.1 大幅上调）
        register("whx_peace_vision", CAT_NEUTRAL, "prismatic", "player_dmg_vs_neutral:0.55");          // 和平愿景（友好，v0.4.1 上调）
        register("whx_neutral_dominion", CAT_NEUTRAL, "prismatic", "neutral_hp:10.0,neutral_atk:6.0,player_dmg_vs_neutral:-0.45"); // 中立统御（高机制高数值：血攻大加强 + 玩家对其伤害 -45%）

        // ============ 敌对生物（怪物）============
        register("whx_shadow_power", CAT_HOSTILE, "silver", "hostile_hp:6.0");              // 暗影强化（难度，v0.4.1 上调）
        register("whx_fang_claw", CAT_HOSTILE, "silver", "hostile_atk:3.0");                // 尖牙利爪（难度，v0.4.1 上调）
        register("whx_predator_swift", CAT_HOSTILE, "gold", "hostile_speed:0.45");          // 捕食者迅捷（难度，v0.4.1 上调）
        register("whx_armor_shell", CAT_HOSTILE, "gold", "hostile_armor:9.0");              // 装甲外壳（难度，v0.4.1 上调）
        register("whx_undead_march", CAT_HOSTILE, "gold", "hostile_resurrect:0.40");        // 亡灵行军（高机制：敌对生物死亡 40% 召唤同类）
        register("whx_abyss_awaken", CAT_HOSTILE, "prismatic", "hostile_hp:30.0,hostile_atk:9.0,hostile_speed:0.40"); // 深渊觉醒（难度，v0.4.1 大幅上调）
        register("whx_blood_feast", CAT_HOSTILE, "prismatic", "hostile_hp:18.0,mob_coin_bonus:1.00");              // 血腥盛宴（难度双刃，v0.4.1 上调）
        register("whx_demon_contract", CAT_HOSTILE, "prismatic", "hostile_lifesteal:0.40,hostile_debuff:0.40");    // 恶魔契约（高机制：敌对生物 40% 吸血 + 40% 概率对玩家施虚弱/缓慢）
    }

    private static void register(String id, String category, String rarity, String effect) {
        DEFINITIONS.put(id, new WorldHexDefinition(id, category, rarity, effect));
    }

    public static WorldHexDefinition get(String id) { return DEFINITIONS.get(id); }
    public static Collection<WorldHexDefinition> all() { return DEFINITIONS.values(); }
    public static boolean exists(String id) { return DEFINITIONS.containsKey(id); }

    /** 抽取池：全部未激活的世界海克斯（打乱后随机取 3 个；激活后移出池） */
    public static List<String> drawPool(List<String> activeIds) {
        List<String> pool = new ArrayList<>();
        for (WorldHexDefinition d : DEFINITIONS.values()) {
            if (!activeIds.contains(d.id)) pool.add(d.id);
        }
        Collections.shuffle(pool);
        return pool;
    }

    /** 三选一：从池中随机 3 个不同 id（池不足 3 个时取剩余全部） */
    public static List<String> rollOptions(List<String> activeIds) {
        List<String> pool = drawPool(activeIds);
        return pool.subList(0, Math.min(3, pool.size()));
    }

    /** 某分类下当前激活的海克斯（按稀有度白银→黄金→棱彩、再按定义序） */
    public static List<WorldHexDefinition> activeInCategory(List<String> activeIds, String category) {
        List<WorldHexDefinition> out = new ArrayList<>();
        for (String id : activeIds) {
            WorldHexDefinition d = DEFINITIONS.get(id);
            if (d != null && d.category.equals(category)) out.add(d);
        }
        out.sort(Comparator.comparingInt(d -> rarityOrder(d.rarity)));
        return out;
    }

    private static int rarityOrder(String r) {
        return switch (r) {
            case "silver" -> 0;
            case "gold" -> 1;
            default -> 2;
        };
    }
}
