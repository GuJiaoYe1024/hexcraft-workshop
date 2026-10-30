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
 *  v0.4.6 设计规范（按稀有度分层重新整理设计）：
 *    白银 = 数值类（纯属性 / 纯倍率数值，无机制）；
 *    黄金 = 数值 + 机制（保留炼狱主数值，附加一类机制效果）；
 *    棱彩 = 百分比数值 + 机制（百分比主数值 + 机制，最难最强）。
 *  v0.4.7 数值形态再分层（用户追认）：
 *    白银 = 纯数字数值（固定点数，如 +6 生命 / +3 攻击 / 固定经验 / 固定晶币，无百分比）；
 *    黄金 = 数值 + 少量百分比（主数值保留，百分比 ≤20%，如输出 -15% 等）；
 *    棱彩 = 百分比 / 倍数数值（百分比生命、百分比攻击、百分比移速或经验倍率，30% 起步，
 *           部分可达 ×2 / ×3 的倍数级效果）。
 *  机制效果类型：受击反扑（berserk）、死亡召唤（resurrect）、吸血（lifesteal）、
 *    施加虚弱/缓慢（debuff）、驯服回血（regen）、掉落抑制（no_drop）、
 *    击杀晶币加成（mob_coin_bonus）、抽取折扣（draw_cost）、死亡经验惩罚（death_xp）。
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
        // 白银：纯数字数值（固定经验 / 固定晶币，无百分比）
        register("whx_xp_friend", CAT_GENERAL, "silver", "player_xp_flat:3");                 // 经验亲和：击杀敌对生物额外 +3 经验
        register("whx_coin_tax", CAT_GENERAL, "silver", "player_coin_flat:-3");               // 晶币税：击杀生物获得的晶币 -3
        // 黄金：数值 + 少量百分比
        register("whx_dmg_shortage", CAT_GENERAL, "gold", "player_dmg:-0.15,mob_coin_bonus:0.10"); // 补给匮乏：输出 -15%，击杀敌对晶币 +10%
        register("whx_eternal_flame", CAT_GENERAL, "gold", "player_taken:0.15,player_xp:0.10");    // 永燃诅咒：受伤 +15%，经验 +10%
        register("whx_life_echo", CAT_GENERAL, "gold", "player_hp:8.0,tamed_regen:0.6");           // 生命共鸣：全体玩家 +8 血，驯服生物持续回血
        // 棱彩：百分比 / 倍数数值 + 机制
        register("whx_ancient_weakness", CAT_GENERAL, "prismatic", "player_dmg:-0.30,player_hp_pct:-0.15,death_xp:0.50"); // 远古衰弱：输出 -30%、最大生命 -15%、死亡经验惩罚 +50%
        register("whx_death_harvest", CAT_GENERAL, "prismatic", "player_dmg:-0.30,death_xp:2.00");       // 死亡收割：输出 -30% + 死亡经验惩罚 ×3
        register("whx_genesis_pact", CAT_GENERAL, "prismatic", "player_xp:0.75,draw_cost:-0.55");       // 创世契约：经验 +75%，抽取消耗 -55%

        // ============ 友好生物（被动/驯养）============
        // 白银：纯数字数值
        register("whx_pasture_harmony", CAT_FRIENDLY, "silver", "passive_speed:0.10");      // 牧场丰饶：友好生物移速 +10%
        register("whx_herd_watchful", CAT_FRIENDLY, "silver", "passive_hp:6.0");            // 家畜警觉：友好生物最大生命 +6
        // 黄金：数值 + 机制（少量百分比）
        register("whx_loyal_guard", CAT_FRIENDLY, "gold", "passive_atk:3.0,tamed_regen:0.5"); // 忠诚守护：友好攻击 +3，驯服生物回血
        register("whx_flock_shadow", CAT_FRIENDLY, "gold", "passive_hp:-10.0,passive_no_drop:0.30"); // 牧群阴影：生命 -10 + 30% 不掉落
        register("whx_pacifist_oath", CAT_FRIENDLY, "gold", "passive_no_drop:1.00,passive_speed:0.05"); // 和平契约：100% 不掉落 + 移速 +5%
        // 棱彩：百分比数值 + 机制
        register("whx_beast_king", CAT_FRIENDLY, "prismatic", "passive_hp_pct:0.30,passive_atk_pct:0.20,passive_berserk:0.35"); // 万兽之王：生命 +30%、攻击 +20% + 受击 35% 狂暴反扑
        register("whx_angel_grace", CAT_FRIENDLY, "prismatic", "tamed_regen:2.0,passive_speed:0.30"); // 天使眷顾：驯服回血 2 + 友好移速 +30%
        register("whx_wrath_of_fields", CAT_FRIENDLY, "prismatic", "passive_berserk:1.00,passive_speed:0.30"); // 田野之怒：100% 狂暴反扑 + 移速 +30%

        // ============ 中立生物（末影人/猪灵/狼等）============
        // 白银：纯数字数值
        register("whx_wild_spirit", CAT_NEUTRAL, "silver", "neutral_atk:3.0");              // 野性难驯：中立攻击 +3
        register("whx_vigilant_eye", CAT_NEUTRAL, "silver", "neutral_follow:0.40");         // 警惕之眼：中立索敌 +40%
        // 黄金：数值 + 少量百分比
        register("whx_pack_rage", CAT_NEUTRAL, "gold", "neutral_atk:4.5,neutral_berserk:0.25"); // 兽群狂暴：攻击 +4.5 + 受击 25% 反扑
        register("whx_hunter_eye", CAT_NEUTRAL, "gold", "player_dmg_vs_neutral:0.20,player_xp:0.10"); // 猎人之眼：对中立伤害 +20%，猎杀经验 +10%
        register("whx_wild_instinct", CAT_NEUTRAL, "gold", "neutral_berserk:0.75,neutral_speed:0.10"); // 野性本能：75% 反扑 + 移速 +10%
        // 棱彩：百分比数值 + 机制
        register("whx_apex_presence", CAT_NEUTRAL, "prismatic", "neutral_hp_pct:0.35,neutral_atk_pct:0.25,neutral_berserk:0.30"); // 终焉威压：生命 +35%、攻击 +25% + 受击 30% 反扑
        register("whx_peace_vision", CAT_NEUTRAL, "prismatic", "player_dmg_vs_neutral:0.60,draw_cost:-0.55"); // 和平愿景：对中立伤害 +60%，抽取消耗 -55%
        register("whx_neutral_dominion", CAT_NEUTRAL, "prismatic", "neutral_hp_pct:0.30,neutral_atk_pct:0.25,player_dmg_vs_neutral:-0.45,neutral_berserk:0.20"); // 中立统御：生命 +30%、攻击 +25% + 玩家对其 -45% + 受击反扑

        // ============ 敌对生物（怪物）============
        // 白银：纯数字数值
        register("whx_shadow_power", CAT_HOSTILE, "silver", "hostile_hp:6.0");              // 暗影强化：敌对生命 +6
        register("whx_fang_claw", CAT_HOSTILE, "silver", "hostile_atk:3.0");                // 尖牙利爪：敌对攻击 +3
        // 黄金：数值 + 少量百分比
        register("whx_predator_swift", CAT_HOSTILE, "gold", "hostile_speed:0.20,hostile_lifesteal:0.10"); // 捕食者迅捷：移速 +20% + 攻击吸血 10%
        register("whx_armor_shell", CAT_HOSTILE, "gold", "hostile_armor:9.0,hostile_debuff:0.25");       // 装甲外壳：护甲 +9 + 攻击 25% 施虚弱/缓慢
        register("whx_undead_march", CAT_HOSTILE, "gold", "hostile_resurrect:0.40,hostile_hp:4.0");      // 亡灵行军：死亡 40% 召唤同类 + 生命 +4
        // 棱彩：百分比 / 倍数数值 + 机制
        register("whx_abyss_awaken", CAT_HOSTILE, "prismatic", "hostile_hp_pct:0.40,hostile_atk_pct:0.30,hostile_speed:0.40,hostile_debuff:0.30"); // 深渊觉醒：生命 +40%、攻击 +30%、移速 +40% + 攻击 30% 施虚弱/缓慢
        register("whx_blood_feast", CAT_HOSTILE, "prismatic", "hostile_hp_pct:0.35,hostile_lifesteal:0.20,mob_coin_bonus:0.60"); // 血腥盛宴：生命 +35% + 吸血 20% + 击杀晶币 +60%
        register("whx_demon_contract", CAT_HOSTILE, "prismatic", "hostile_lifesteal:0.40,hostile_debuff:0.40"); // 恶魔契约：吸血 40% + 40% 施虚弱/缓慢

        // ============ v0.6.2 叠层相关世界海克斯（通用）============
        // 黄金：数值 + 少量百分比；棱彩：百分比 / 倍数数值 + 机制
        register("whx_world_perm_grow_boost", CAT_GENERAL, "prismatic", "perm_stack_gain:0.50");             // 永恒增殖：永久叠层获取 +50%
        register("whx_world_combat_decay_fast", CAT_GENERAL, "gold", "combat_stack_decay:0.50");              // 战消：战斗叠层脱战衰减速度 +50%（更早清零）
        register("whx_world_combat_decay_slow", CAT_GENERAL, "gold", "combat_stack_decay:-0.30");             // 久战：战斗叠层脱战衰减速度 -30%（叠层留存更久）
        register("whx_world_timed_stack_short", CAT_GENERAL, "gold", "timed_stack_short:-0.50");              // 转瞬：计时叠层时长减半（到期更快消失）

        // ============ v0.8.4 联动主题世界海克斯（全部复用现有事件键，不引入新机制）============
        // 黄金：数值 + 少量百分比（对齐 v0.4.7 分层规范）
        register("whx_link_emc_tax", CAT_GENERAL, "gold", "mob_coin_bonus:0.25,player_dmg:-0.10");            // 等价交换税：全服击杀晶币 +25%，输出 -10%（EMC 联动主题）
        register("whx_link_farm_bounty", CAT_FRIENDLY, "gold", "passive_hp:8.0,tamed_regen:1.0");            // 丰收祝福：友好生物生命 +8，驯服生物回血 1（农夫乐事联动主题）
        register("whx_link_soul_harvest", CAT_NEUTRAL, "gold", "neutral_atk:3.5,neutral_berserk:0.30");      // 灵魂收割：中立攻击 +3.5，受击 30% 反扑（巫术联动主题）
        register("whx_link_cataclysm_omen", CAT_HOSTILE, "gold", "hostile_hp:8.0,hostile_debuff:0.25");      // 灾变前兆：敌对生命 +8，攻击 25% 施虚弱/缓慢（末地灾变联动主题）

        // ============ v0.8.8 经验税世界海克斯（玩家抽取海克斯经验需求提高）============
        // 棱彩：百分比数值（纯诅咒，全体玩家抽取成本 +50%）
        register("whx_draw_tax", CAT_GENERAL, "prismatic", "player_draw_cost:0.50");      // 抽卡税：玩家抽取海克斯经验需求 +50%
    }

    private static void register(String id, String category, String rarity, String effect) {
        DEFINITIONS.put(id, new WorldHexDefinition(id, category, rarity, effect));
    }

    public static WorldHexDefinition get(String id) { return DEFINITIONS.get(id); }
    public static Collection<WorldHexDefinition> all() { return DEFINITIONS.values(); }
    public static boolean exists(String id) { return DEFINITIONS.containsKey(id); }

    /** v0.6.0 数据包开放：data/hexworkshop/world_hexes/*.json 覆盖或新增世界海克斯（WorldHexDataLoader 调用） */
    public static void put(String id, WorldHexDefinition def) { DEFINITIONS.put(id, def); }

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
