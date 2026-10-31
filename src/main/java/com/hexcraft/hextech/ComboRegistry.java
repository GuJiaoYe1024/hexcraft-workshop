package com.hexcraft.hextech;

/**
 * v0.9.3 组合羁绊共享表（单源防漂移）：
 * 服务端 HextechEvents 效果结算 + 客户端 HextechPageScreen 属性页展示共用此表。
 * 行格式：{效果键, 加成值, 组内海克斯 id...}（同时拥有组内全部 → 该效果键追加加成）。
 * 注：处决者/叠层之巅为代码特殊组合（非本表行），由各处独立判断。
 */
public final class ComboRegistry {

    private ComboRegistry() {}

    public static final String[][] COMBOS = {
            {"melee_power", "0.15", "hex_warmaster", "hex_berserker_soul", "hex_bloodlust_blade"},                    // 1 近战无双
            {"tacz_power", "0.15", "hex_tacz_overlord", "hex_tacz_deadeye", "hex_tacz_crit_chain"},                    // 2 枪林弹雨
            {"spell_power", "0.15", "hex_irons_archmage", "hex_irons_arcane_focus", "hex_irons_echo"},                // 3 奥术主宰
            {"iaf_dragon_damage", "0.15", "hex_iaf_dragon_lord", "hex_iaf_dragon_blood", "hex_iaf_dragon_fury"},      // 4 龙裔之力
            {"ac_deep_resist", "0.10", "hex_ac_deep_guard", "hex_ac_abyssal_breath", "hex_ac_underworld_archon"},     // 5 深渊守望
            {"coin_bonus", "0.15", "hex_midas_hand", "hex_trade_discount", "hex_golden_bet"},                          // 6 生财有道
            {"overshield", "0.10", "hex_undying", "hex_eternal_shield", "hex_immortal_wall"},                          // 7 不灭之躯
            {"equip_power", "0.10", "hex_equip_weaver", "hex_equip_count", "hex_forge_power"},                         // 10 全能武装
            {"tf_snow_queen_damage", "0.15", "hex_tf_snow_queen_bane", "hex_tf_snow_queen_ward", "hex_tf_yeti_winter"},// 11 冰雪审判
            {"goety_soul", "0.15", "hex_goety_soul_eater", "hex_goety_soul_harvester", "hex_goety_necromancer_pact"},  // 12 冥府契约
            {"am_mob_damage", "0.10", "hex_am_predator", "hex_am_predator_circle", "hex_am_apex_stalker"},             // 13 猎手本能
            {"blade_damage", "0.15", "hex_blade_iai_flash", "hex_blade_moon_slash", "hex_blade_sakura_storm"},         // 14 拔刀奥义
            {"relic_drop", "0.15", "hex_relic_treasure_hunter", "hex_relic_treasure_map", "hex_artifact_loot"},        // 15 寻宝专家
            {"farmer_drop", "0.20", "hex_farmer_grand_harvest", "hex_farmer_harvest_moon", "hex_farmer_feast"},        // 16 丰收盛宴
            {"spell_mana_max", "0.10", "hex_irons_mana_reservoir", "hex_irons_mana_spring", "hex_irons_mana_weaver"},  // 17 魔力泉涌
            {"cataclysm_damage", "0.15", "hex_cataclysm_boss_mark", "hex_cataclysm_punisher", "hex_cataclysm_wither_soul"}, // 18 灾厄猎手
            // —— v0.9.2 逆原 DLC 三系羁绊（狂战/游击/壁垒；每组 3 件套，效果键复用现键）——
            {"melee_power", "0.60", "hex_niyuan_kuangzhan_1", "hex_niyuan_kuangzhan_2", "hex_niyuan_kuangzhan_3"},      // 19 逆原·狂战·初阵
            {"melee_power", "0.60", "hex_niyuan_kuangzhan_4", "hex_niyuan_kuangzhan_5", "hex_niyuan_kuangzhan_6"},      // 20 逆原·狂战·暴走
            {"mobi_power", "0.50", "hex_niyuan_youji_1", "hex_niyuan_youji_2", "hex_niyuan_youji_3"},                    // 21 逆原·游击·疾行
            {"mobi_power", "0.50", "hex_niyuan_youji_4", "hex_niyuan_youji_5", "hex_niyuan_youji_6"},                    // 22 逆原·游击·连击
            {"overshield", "0.50", "hex_niyuan_bilei_1", "hex_niyuan_bilei_2", "hex_niyuan_bilei_3"},                    // 23 逆原·壁垒·坚守
            {"overshield", "0.50", "hex_niyuan_bilei_4", "hex_niyuan_bilei_5", "hex_niyuan_bilei_6"},                    // 24 逆原·壁垒·不动
    };

    /** 叠层类海克斯全集（叠层联动档位判定：拥有 ≥2 → 叠层获取/上限 +20%，≥4 → +40%；上限额外 ×1.3 组合为代码特殊判定） */
    public static final java.util.Set<String> STACK_HEXES = java.util.Set.of(
            "hex_tick_damage_wax", "hex_timed_multi_aspect", "hex_artifact_endurance_stack",
            "hex_blade_draw_song", "hex_blade_scabbard_flow", "hex_blade_soul_flurry",
            "hex_combat_perm_mix", "hex_heart_steel", "hex_hurt_speed_boost", "hex_kill_speed_perm",
            "hex_mowzie_feral_instinct", "hex_mowzie_frenzy_claw", "hex_perm_damage_apex",
            "hex_stack_tank", "hex_swift_horn_quick");

    /** 羁绊展示名 lang 键（1-based 行号） */
    public static String nameKey(int index1) {
        return "hexcraft.combo.c" + index1;
    }

    /** 羁绊成员数（效果键之后） */
    public static int memberCount(String[] row) {
        return row.length - 2;
    }

    /** 效果键 lang 键 */
    public static String effectKeyOf(String[] row) {
        return "hexcraft.effect." + row[0];
    }
}
