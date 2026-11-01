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
            {"equip_power", "0.10", "hex_equip_weaver", "hex_equip_count", "hex_forge_power"},                         // 8 全能武装
            {"tf_snow_queen_damage", "0.15", "hex_tf_snow_queen_bane", "hex_tf_snow_queen_ward", "hex_tf_yeti_winter"},// 9 冰雪审判
            {"goety_soul", "0.15", "hex_goety_soul_eater", "hex_goety_soul_harvester", "hex_goety_necromancer_pact"},  // 10 冥府契约
            {"am_mob_damage", "0.10", "hex_am_predator", "hex_am_predator_circle", "hex_am_apex_stalker"},             // 11 猎手本能
            {"blade_damage", "0.15", "hex_blade_iai_flash", "hex_blade_moon_slash", "hex_blade_sakura_storm"},         // 12 拔刀奥义
            {"relic_drop", "0.15", "hex_relic_treasure_hunter", "hex_relic_treasure_map", "hex_artifact_loot"},        // 13 寻宝专家
            {"farmer_drop", "0.20", "hex_farmer_grand_harvest", "hex_farmer_harvest_moon", "hex_farmer_feast"},        // 14 丰收盛宴
            {"spell_mana_max", "0.10", "hex_irons_mana_reservoir", "hex_irons_mana_spring", "hex_irons_mana_weaver"},  // 15 魔力泉涌
            {"cataclysm_damage", "0.15", "hex_cataclysm_boss_mark", "hex_cataclysm_punisher", "hex_cataclysm_wither_soul"}, // 16 灾厄猎手
            // —— v0.9.5：逆原三系 19-24 已由「3 件套×2」重构为分档羁绊（TIERED_COMBOS，2/4/6 按件数激活）——
    };

    /** v0.9.5 逆原分档羁绊（还原逆战·未来原点猎场增幅体系：按件数分档，低档效果高档仍叠加生效）：
     *  record 字段：nameKey 羁绊名 lang 键、members 成员海克斯 id、tierCounts 各档件数（升序）、
     *  effects 各档效果键（复用现键）、values 各档加成值。 */
    public record TieredCombo(String nameKey, String[] members, int[] tierCounts, String[] effects, double[] values) {}

    public static final java.util.List<TieredCombo> TIERED_COMBOS = java.util.List.of(
            // 狂战：近战增伤流（还原 6 狂战损血增伤最高 200%，3 档递增 30/60/100 → 满 6 件合计 +190%）
            new TieredCombo("hexcraft.combo.tier.berserk",
                    new String[]{"hex_niyuan_kuangzhan_1", "hex_niyuan_kuangzhan_2", "hex_niyuan_kuangzhan_3",
                            "hex_niyuan_kuangzhan_4", "hex_niyuan_kuangzhan_5", "hex_niyuan_kuangzhan_6"},
                    new int[]{2, 4, 6}, new String[]{"melee_power", "melee_power", "melee_power"}, new double[]{0.30, 0.60, 1.00}),
            // 游击：机动+远程流（还原 2 移速25% / 4 伤害50% / 6 移动增伤120%，远程增伤合计 120%）
            new TieredCombo("hexcraft.combo.tier.guerrilla",
                    new String[]{"hex_niyuan_youji_1", "hex_niyuan_youji_2", "hex_niyuan_youji_3",
                            "hex_niyuan_youji_4", "hex_niyuan_youji_5", "hex_niyuan_youji_6"},
                    new int[]{2, 4, 6}, new String[]{"mobi_power", "tacz_power", "tacz_power"}, new double[]{0.25, 0.50, 0.70}),
            // 壁垒：防御流（还原 4 壁垒减伤/爆韧，3 档护盾减伤递增）
            new TieredCombo("hexcraft.combo.tier.bulwark",
                    new String[]{"hex_niyuan_bilei_1", "hex_niyuan_bilei_2", "hex_niyuan_bilei_3",
                            "hex_niyuan_bilei_4", "hex_niyuan_bilei_5", "hex_niyuan_bilei_6"},
                    new int[]{2, 4, 6}, new String[]{"overshield", "overshield", "overshield"}, new double[]{0.30, 0.60, 0.40}),
            // 狙击：远程/枪械流（v0.9.5 新增，还原「狙击」词条：弱点/狙击增伤）
            new TieredCombo("hexcraft.combo.tier.sniping",
                    new String[]{"hex_niyuan_sniping_1", "hex_niyuan_sniping_2", "hex_niyuan_sniping_3", "hex_niyuan_sniping_4"},
                    new int[]{2, 4}, new String[]{"tacz_power", "tacz_power"}, new double[]{0.30, 0.60}),
            // 狩猎：近战/猎杀流（v0.9.5 新增，还原「狩猎」词条：暴伤碎片）
            new TieredCombo("hexcraft.combo.tier.hunting",
                    new String[]{"hex_niyuan_hunting_1", "hex_niyuan_hunting_2", "hex_niyuan_hunting_3", "hex_niyuan_hunting_4"},
                    new int[]{2, 4}, new String[]{"melee_power", "melee_power"}, new double[]{0.30, 0.60})
    );

    /** v0.9.6 海克斯大乱斗套装羁绊（还原 LOL ARAM Mayhem 官方 9 个强化符文套装 Augment Sets：
     *  收集同一套装 2-4 个强化符文解锁额外效果 → 本 mod 按云顶式 2/4/6/8 件分档，低档效果高档仍叠加）。
     *  成员 = 本体现有海克斯（可跨套装参与，同云顶棋子多羁绊）+ 每套 2 个新建官方特色海克斯（source 留空=本体）；
     *  档位效果键全部复用现有效果键（无新 hook），服务端/客户端分档结算与渲染沿用 TIERED_COMBOS 同套逻辑。 */
    public static final java.util.List<TieredCombo> ARAM_SETS = java.util.List.of(
            // 1 炮竹 Firecracker：官方弹跳增伤（2 档 40%/4 档 80%）→ 枪械弹幕增伤
            new TieredCombo("hexcraft.combo.tier.aram.firecracker",
                    new String[]{"hex_tacz_crit_chain", "hex_tacz_crit_master", "hex_tacz_multi_shot", "hex_tacz_double_shot",
                            "hex_full_crit", "hex_twin_fire", "hex_aram_firecracker_rocket", "hex_aram_firecracker_bounce"},
                    new int[]{2, 4, 6, 8}, new String[]{"tacz_power", "tacz_power", "tacz_power", "tacz_power"},
                    new double[]{0.15, 0.30, 0.50, 0.75}),
            // 2 叠叠龙 Stackasaurus Rex：官方叠层获得 +50/+100/+200% → 成长层数增幅
            new TieredCombo("hexcraft.combo.tier.aram.stackasaurus",
                    new String[]{"hex_heart_steel", "hex_stack_tank", "hex_tick_damage_wax", "hex_artifact_endurance_stack",
                            "hex_blade_draw_song", "hex_perm_damage_apex", "hex_aram_titan_stack", "hex_aram_ramp_rex"},
                    new int[]{2, 4, 6, 8}, new String[]{"stack_growth", "stack_growth", "stack_growth", "stack_growth"},
                    new double[]{0.25, 0.50, 0.75, 1.00}),
            // 3 大法师 Archmage：官方套装奖励（AP 型）30% → 法术增伤
            new TieredCombo("hexcraft.combo.tier.aram.archmage",
                    new String[]{"hex_irons_archmage", "hex_irons_arcane_focus", "hex_irons_arcane_strike", "hex_irons_archon_judgment",
                            "hex_irons_elemental_surge", "hex_irons_blood_siphon", "hex_aram_archmage_crest", "hex_aram_arcane_pact"},
                    new int[]{2, 4, 6, 8}, new String[]{"spell_power", "spell_power", "spell_power", "spell_power"},
                    new double[]{0.15, 0.30, 0.45, 0.60}),
            // 4 救护车 Wee Woo Wee Woo：官方治疗/护盾强化 → 溢出转盾
            new TieredCombo("hexcraft.combo.tier.aram.weewoo",
                    new String[]{"hex_undying", "hex_eternal_shield", "hex_immortal_wall", "hex_combat_shield_surge",
                            "hex_arcane_cloak", "hex_irons_healing_word", "hex_aram_wee_woo", "hex_aram_ambulance_sirens"},
                    new int[]{2, 4, 6, 8}, new String[]{"overshield", "overshield", "overshield", "overshield"},
                    new double[]{0.10, 0.20, 0.30, 0.40}),
            // 5 豪赌客 High Roller：官方小兵几率掉锻造器/金棱彩掉率 → 晶币加成
            new TieredCombo("hexcraft.combo.tier.aram.highroller",
                    new String[]{"hex_midas_hand", "hex_trade_discount", "hex_golden_bet", "hex_dice_maniac",
                            "hex_opening_forge", "hex_emc_savings_wallet", "hex_aram_high_roller", "hex_aram_lucky_anvil"},
                    new int[]{2, 4, 6, 8}, new String[]{"coin_bonus", "coin_bonus", "coin_bonus", "coin_bonus"},
                    new double[]{0.10, 0.20, 0.30, 0.40}),
            // 6 死亡钟 Deathknell：官方死亡倒计时 -40% → 免死/濒死保护
            new TieredCombo("hexcraft.combo.tier.aram.deathknell",
                    new String[]{"hex_death_defier", "hex_executioner_eternal", "hex_corpse_explosion",
                            "hex_soul_reaper", "hex_am_titan_subdue", "hex_death_dance", "hex_aram_deathknell", "hex_aram_bell_toll"},
                    new int[]{2, 4, 6, 8}, new String[]{"death_defy", "death_defy", "death_defy", "death_defy"},
                    new double[]{0.05, 0.10, 0.15, 0.20}),
            // 7 全自动 Fully Automated：官方自动释放类技能冷却 -30% → 技能回响/自动再施
            new TieredCombo("hexcraft.combo.tier.aram.fullyautomated",
                    new String[]{"hex_irons_echo", "hex_irons_echo_recovery", "hex_echo_blade", "hex_echo_cloak",
                            "hex_echo_rampage", "hex_irons_swiftcast", "hex_aram_auto_cast", "hex_aram_factory_line"},
                    new int[]{2, 4, 6, 8}, new String[]{"echo", "echo", "echo", "echo"},
                    new double[]{0.05, 0.10, 0.15, 0.20}),
            // 8 下雪天 Snowday：官方雪球/冰霜强化 → 冰霜之咬增伤
            new TieredCombo("hexcraft.combo.tier.aram.snowday",
                    new String[]{"hex_tf_snow_queen_bane", "hex_tf_snow_queen_ward", "hex_tf_yeti_winter", "hex_irons_frost_shard",
                            "hex_ice_crown", "hex_frost_crit", "hex_aram_snowball", "hex_aram_ice_day"},
                    new int[]{2, 4, 6, 8}, new String[]{"frost_bite", "frost_bite", "frost_bite", "frost_bite"},
                    new double[]{0.10, 0.20, 0.30, 0.40}),
            // 9 俯冲轰炸 Dive Bomb：官方死亡爆炸/俯冲 → 烟花爆炸/范围
            new TieredCombo("hexcraft.combo.tier.aram.divebomb",
                    new String[]{"hex_firework_king", "hex_all_damage_proc", "hex_storm_breaker",
                            "hex_fire_blade", "hex_nether_fire_lord", "hex_dragon_dance", "hex_aram_dive_bomb", "hex_aram_clown_college"},
                    new int[]{2, 4, 6, 8}, new String[]{"firework", "firework", "firework", "firework"},
                    new double[]{0.10, 0.20, 0.30, 0.40})
    );

    /** 全部分档羁绊（逆原 TIERED_COMBOS + 大乱斗 ARAM_SETS），服务端结算与客户端渲染统一遍历此表 */
    public static java.util.List<TieredCombo> allTiered() {
        return java.util.stream.Stream.concat(TIERED_COMBOS.stream(), ARAM_SETS.stream()).collect(
                java.util.stream.Collectors.toUnmodifiableList());
    }

    /** 玩家已激活档数（0=未激活；拥有件数 ≥ 该档件数即激活；低档效果在高档仍生效） */
    public static int tierOf(TieredCombo tc, java.util.Set<String> owned) {
        int have = 0;
        for (String m : tc.members()) if (owned.contains(m)) have++;
        int tier = 0;
        for (int t = 0; t < tc.tierCounts().length; t++) if (have >= tc.tierCounts()[t]) tier = t + 1;
        return tier;
    }

    public static int tierOf(TieredCombo tc, java.util.List<com.hexcraft.capability.PlayerData.HexEntry> hexes) {
        java.util.Set<String> owned = new java.util.HashSet<>();
        for (com.hexcraft.capability.PlayerData.HexEntry e : hexes) owned.add(e.id);
        return tierOf(tc, owned);
    }

    /** 拥有某分档羁绊成员件数 */
    public static int ownedCount(TieredCombo tc, java.util.Set<String> owned) {
        int have = 0;
        for (String m : tc.members()) if (owned.contains(m)) have++;
        return have;
    }

    /** 分档羁绊总成员数 */
    public static int memberCount(TieredCombo tc) {
        return tc.members().length;
    }

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
