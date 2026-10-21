package com.hexcraft.hextech;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 世界海克斯注册表（内置定义，代码驱动）：
 *  24 个世界海克斯 = 通用 6 + 友好 6 + 中立 6 + 敌对 6；每类白银 2 / 黄金 2 / 棱彩 2。
 *  设计取向：多数提升世界难度（加强生物、诅咒玩家），少数对玩家有利，棱彩最难最强。
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
        register("whx_xp_friend", CAT_GENERAL, "silver", "player_xp:0.10");                 // 经验亲和（友好）
        register("whx_coin_tax", CAT_GENERAL, "silver", "player_coin:-0.10");               // 晶币税（诅咒）
        register("whx_dmg_shortage", CAT_GENERAL, "gold", "player_dmg:-0.10");              // 补给匮乏（诅咒）
        register("whx_life_echo", CAT_GENERAL, "gold", "player_hp:4.0");                    // 生命共鸣（友好）
        register("whx_ancient_weakness", CAT_GENERAL, "prismatic", "player_hp:-6.0,player_dmg:-0.08"); // 远古衰弱（大诅咒）
        register("whx_genesis_pact", CAT_GENERAL, "prismatic", "player_xp:0.20,draw_cost:0.50");        // 创世契约（友好双刃）

        // ============ 友好生物（被动/驯养）============
        register("whx_pasture_harmony", CAT_FRIENDLY, "silver", "passive_speed:0.10");      // 牧场丰饶（友好）
        register("whx_herd_watchful", CAT_FRIENDLY, "silver", "passive_no_drop:0.30");      // 家畜警觉（诅咒）
        register("whx_loyal_guard", CAT_FRIENDLY, "gold", "passive_atk:2.0");               // 忠诚守护（友好）
        register("whx_flock_shadow", CAT_FRIENDLY, "gold", "passive_hp:-4.0");              // 牧群阴影（诅咒）
        register("whx_beast_king", CAT_FRIENDLY, "prismatic", "passive_hp:8.0,passive_atk:2.0");      // 万兽之王（难度）
        register("whx_angel_grace", CAT_FRIENDLY, "prismatic", "tamed_regen:0.5");          // 天使眷顾（友好）

        // ============ 中立生物（末影人/猪灵/狼等）============
        register("whx_wild_spirit", CAT_NEUTRAL, "silver", "neutral_atk:1.0");              // 野性难驯（诅咒）
        register("whx_vigilant_eye", CAT_NEUTRAL, "silver", "neutral_follow:0.30");         // 警惕之眼（难度）
        register("whx_pack_rage", CAT_NEUTRAL, "gold", "neutral_atk:2.0");                  // 兽群狂暴（难度）
        register("whx_hunter_eye", CAT_NEUTRAL, "gold", "player_dmg_vs_neutral:0.15");      // 猎人之眼（友好）
        register("whx_apex_presence", CAT_NEUTRAL, "prismatic", "neutral_hp:10.0,neutral_speed:0.15"); // 终焉威压（难度）
        register("whx_peace_vision", CAT_NEUTRAL, "prismatic", "player_dmg_vs_neutral:0.30");          // 和平愿景（友好）

        // ============ 敌对生物（怪物）============
        register("whx_shadow_power", CAT_HOSTILE, "silver", "hostile_hp:2.0");              // 暗影强化（难度）
        register("whx_fang_claw", CAT_HOSTILE, "silver", "hostile_atk:1.0");                // 尖牙利爪（难度）
        register("whx_predator_swift", CAT_HOSTILE, "gold", "hostile_speed:0.20");          // 捕食者迅捷（难度）
        register("whx_armor_shell", CAT_HOSTILE, "gold", "hostile_armor:4.0");              // 装甲外壳（难度）
        register("whx_abyss_awaken", CAT_HOSTILE, "prismatic", "hostile_hp:12.0,hostile_atk:4.0,hostile_speed:0.15"); // 深渊觉醒（难度）
        register("whx_blood_feast", CAT_HOSTILE, "prismatic", "hostile_hp:8.0,mob_coin_bonus:0.50");              // 血腥盛宴（难度双刃）
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
