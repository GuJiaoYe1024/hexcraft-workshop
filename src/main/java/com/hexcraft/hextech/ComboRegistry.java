package com.hexcraft.hextech;

/**
 * v0.9.7 组合羁绊体系 v2（重设计，用户拍板档位按池子定）：
 * 47 条分档羁绊统一为 TieredCombo，单源供服务端 HextechEvents 结算 + 客户端 HextechPageScreen 展示。
 * 表构成：
 *  - BASE_COMBOS：三件套 16 条升级（12 成员 3/6/8/10/12 五档，池大高需求）
 *  - NEW_COMBOS：新增流派 11 条（10 成员 2/4/6/8/10 五档）
 *  - STACK_MASTER：叠层专家（10 成员 2/4/6/8/10，个人叠层上限逐步解锁 400/600/800/1000/∞，C 核心）
 *  - TIERED_COMBOS：逆原 10 条升级（9 成员 1/3/5/7/9 五档奇数档）
 *  - ARAM_SETS：大乱斗 9 套（官方还原 2/4/6/8，死亡系 10 成员）
 * 规则：拥有件数 ≥ 档位件数即激活该档，低档效果高档仍叠加生效（云顶式）。
 * 成员可跨羁绊复用（同云顶棋子多羁绊）。引用带 [NEW] 的 id 为本轮新建海克斯（v2-B1/B2 交付，未建前不影响构建）。
 */
public final class ComboRegistry {

    private ComboRegistry() {}

    /** v0.9.5 逆原分档羁绊（还原逆战·未来原点猎场增幅体系：按件数分档，低档效果高档仍叠加生效）：
     *  record 字段：nameKey 羁绊名 lang 键、members 成员海克斯 id、tierCounts 各档件数（升序）、
     *  effects 各档效果键（复用现键）、values 各档加成值。 */
    public record TieredCombo(String nameKey, String[] members, int[] tierCounts, String[] effects, double[] values,
                              double prismaticMult, String owner) {
        /** 6 参重载：不指定 owner 即视为本体羁绊，不受任何内置包开关影响。 */
        public TieredCombo(String nameKey, String[] members, int[] tierCounts, String[] effects, double[] values,
                           double prismaticMult) {
            this(nameKey, members, tierCounts, effects, values, prismaticMult, null);
        }
    }

    /** ================= 三件套 16 升级（12 成员 3/6/8/10/12）================= */
    public static final java.util.List<TieredCombo> BASE_COMBOS = java.util.List.of(
            // 1 近战无双（近战/物理：流血+斩杀+暴击+吸血）
            new TieredCombo("hexcraft.combo.c1",
                    new String[]{"hex_berserker_soul", "hex_bloodlust_blade", "hex_bleed_avatar", "hex_bleed_flesh",
                            "hex_executioner", "hex_executioner_eternal", "hex_fatal_wound", "hex_collector",
                            "hex_lightning_blade", "hex_fire_blade", "hex_poison_blade", "hex_duelist"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"melee_power", "melee_power", "rampage", "melee_power", "vamp_rage"},
                    new double[]{0.025, 0.05, 0.025, 0.125, 0.0375}, 1.0),
            // 2 枪林弹雨（枪械/远程：弹幕+连发+暴击）
            new TieredCombo("hexcraft.combo.c2",
                    new String[]{"hex_tacz_overlord", "hex_tacz_deadeye", "hex_tacz_crit_chain", "hex_tacz_multi_shot",
                            "hex_tacz_double_shot", "hex_tacz_ammo", "hex_tacz_crit_master", "hex_tacz_barrage",
                            "hex_tacz_fatal", "hex_tacz_burst_volley", "hex_tacz_airborne", "hex_marksman"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"tacz_power", "tacz_power", "twin_shot", "tacz_power", "tacz_crit"},
                    new double[]{0.025, 0.05, 0.025, 0.125, 0.15}, 1.0),
            // 3 奥术主宰（法术：回响+元素+魔力）
            new TieredCombo("hexcraft.combo.c3",
                    new String[]{"hex_irons_archmage", "hex_irons_arcane_focus", "hex_irons_echo",
                            "hex_irons_archon_judgment", "hex_irons_elemental_surge", "hex_irons_blood_siphon", "hex_irons_evocation",
                            "hex_irons_eldritch", "hex_irons_ender", "hex_apocalypse", "hex_arcane_cloak"},
                    new int[]{3, 6, 8, 10, 11}, new String[]{"spell_power", "spell_power", "echo", "spell_power", "spell_mana_max"},
                    new double[]{0.02, 0.04, 0.1, 0.1125, 0.05}, 1.0),
            // 4 龙裔之力（冰火雷龙特攻）
            new TieredCombo("hexcraft.combo.c4",
                    new String[]{"hex_iaf_dragon_lord", "hex_iaf_dragon_blood", "hex_iaf_dragon_fury", "hex_iaf_dragon_ancient",
                            "hex_iaf_dragon_ascendant", "hex_iaf_dragon_heart", "hex_iaf_dragon_slayer", "hex_iaf_fire_dragon",
                            "hex_iaf_ice_dragon", "hex_iaf_lightning_dragon", "hex_iaf_breath_master", "hex_iaf_hydra_blood"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"iaf_dragon_damage", "iaf_dragon_damage", "full_hp_crit", "iaf_dragon_damage", "crit_damage"},
                    new double[]{0.0375, 0.075, 0.025, 0.1875, 0.05}, 1.0),
            // 5 深渊守望（AC 深渊特攻，+6 本体新建补员 hex_abyss_*：不依赖 AC mod 也能凑档）
            new TieredCombo("hexcraft.combo.c5",
                    new String[]{"hex_ac_deep_guard", "hex_ac_abyssal_breath", "hex_ac_underworld_archon", "hex_ac_deep_diver",
                            "hex_ac_deep_hunter", "hex_ac_dark_vision",
                            "hex_abyss_warden", "hex_abyss_soul", "hex_abyss_pact", "hex_abyss_tide",
                            "hex_abyss_void", "hex_abyss_guardian"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"ac_dark_damage", "ac_dark_damage", "ac_dark_damage", "last_stand", "ascend"},
                    new double[]{0.0375, 0.075, 0.125, 0.05, 0.0625}, 1.0),
            // 6 生财有道（晶币/掉落经济）
            new TieredCombo("hexcraft.combo.c6",
                    new String[]{"hex_midas_hand", "hex_trade_discount", "hex_golden_bet", "hex_goldmine",
                            "hex_gold_rush", "hex_bounty_hunter", "hex_loot_coin", "hex_dragon_coin",
                            "hex_frost_coin", "hex_nether_gold_greed", "hex_reaper", "hex_sell_bonus"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"coin_bonus", "coin_bonus", "bounty", "coin_bonus", "coin_bonus"},
                    new double[]{0.15, 0.3, 3, 0.75, 1}, 1.0),
            // 7 不灭之躯（护盾/回复/免死）
            new TieredCombo("hexcraft.combo.c7",
                    new String[]{"hex_undying", "hex_eternal_shield", "hex_immortal_wall", "hex_death_defier",
                            "hex_death_dance", "hex_paladin", "hex_rock_body", "hex_warden",
                            "hex_colossus", "hex_giant_soul", "hex_dragonheart", "hex_thorn_king"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"overshield", "overshield", "shield_low", "last_stand", "ironbody"},
                    new double[]{0.025, 0.05, 2, 0.05, 0.075}, 1.0),
            // 8 全能武装（装备/锻造共鸣，+2 新建补员）
            new TieredCombo("hexcraft.combo.c8",
                    new String[]{"hex_equip_weaver", "hex_forge_power", "hex_hex_power", "hex_equip_count",
                            "hex_artifact_relic_boost", "hex_relic_overseer", "hex_relic_power", "hex_artifact_equip_resonance",
                            "hex_relic_attunement", "hex_relic_synergy",
                            "hex_equip_mastery", "hex_equip_resonance_core"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"equip_power", "equip_power", "coin_bonus", "equip_power", "relic_drop"},
                    new double[]{0.02, 0.04, 0.15, 0.1125, 0.2}, 1.0),
            // 9 冰雪审判（冰雪/霜冻特攻）
            new TieredCombo("hexcraft.combo.c9",
                    new String[]{"hex_tf_snow_queen_bane", "hex_tf_snow_queen_ward", "hex_tf_yeti_winter", "hex_tf_yeti_bane",
                            "hex_ice_crown", "hex_frost_crit", "hex_frost_empire", "hex_frost_bite",
                            "hex_frost_step", "hex_irons_frost_shard", "hex_mowzie_frost_domain", "hex_aram_ice_day"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"tf_snow_queen_damage", "tf_snow_queen_damage", "frost_bite", "mobi_power", "tf_snow_queen_damage"},
                    new double[]{0.0375, 0.075, 0.025, 0.025, 0.1875}, 1.0),
            // 10 冥府契约（灵魂/死亡系）
            new TieredCombo("hexcraft.combo.c10",
                    new String[]{"hex_goety_soul_eater", "hex_goety_soul_harvester", "hex_goety_necromancer_pact", "hex_goety_soul",
                            "hex_goety_soul_tithe", "hex_goety_black_ritual", "hex_goety_lich_conclave", "hex_goety_soul_bulwark",
                            "hex_goety_curse_dart", "hex_goety_undead_slayer", "hex_death_dance", "hex_soul_reaper"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"goety_soul", "goety_soul", "kill_heal", "goety_soul", "death_defy"},
                    new double[]{0.0375, 0.075, 2, 0.1875, 0.05}, 1.0),
            // 11 猎手本能（生物/图腾猎杀）
            new TieredCombo("hexcraft.combo.c11",
                    new String[]{"hex_am_predator", "hex_am_predator_circle", "hex_am_apex_stalker", "hex_am_apex",
                            "hex_am_totem", "hex_am_wild_lord", "hex_am_titan_damage", "hex_am_boss_damage",
                            "hex_am_flying_hunter", "hex_am_venom_sting", "hex_am_mob_master", "hex_hunter_mark"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"am_mob_damage", "am_mob_damage", "execute", "am_mob_damage", "am_mob_damage"},
                    new double[]{0.025, 0.05, 0.025, 0.125, 0.175}, 1.0),
            // 12 拔刀奥义（拔刀剑连段）
            new TieredCombo("hexcraft.combo.c12",
                    new String[]{"hex_blade_iai_flash", "hex_blade_moon_slash", "hex_blade_sakura_storm", "hex_blade_reaper_form",
                            "hex_blade_soul_flurry", "hex_blade_thunder_clash", "hex_blade_phantom", "hex_blade_burst",
                            "hex_blade_katana", "hex_blade_of_glory", "hex_blade_storm_burst", "hex_blade_bleed_fang"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"blade_damage", "blade_damage", "echo", "blade_damage", "blade_damage"},
                    new double[]{0.0375, 0.075, 0.1, 0.1875, 0.25}, 1.0),
            // 13 寻宝专家（遗物/战利品）
            new TieredCombo("hexcraft.combo.c13",
                    new String[]{"hex_relic_treasure_hunter", "hex_relic_treasure_map", "hex_artifact_loot", "hex_relic_guardian",
                            "hex_relic_overseer", "hex_relic_pathfinder", "hex_relic_power", "hex_relic_attunement",
                            "hex_artifact_antique_power", "hex_artifact_blood_sigil", "hex_loot_king", "hex_chaos_king"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"relic_drop", "relic_drop", "coin_bonus", "relic_drop", "relic_drop"},
                    new double[]{0.1, 0.2, 0.2, 0.35, 0.7}, 1.0),
            // 14 丰收盛宴（农耕/食物）
            new TieredCombo("hexcraft.combo.c14",
                    new String[]{"hex_farmer_grand_harvest", "hex_farmer_harvest_moon", "hex_farmer_feast", "hex_farmer_banquet",
                            "hex_farmer_chef_mastery", "hex_farmer_cuisine_legend", "hex_farmer_garden_master", "hex_farmer_garden_bounty",
                            "hex_farmer_harvest", "hex_farmer_lush_field", "hex_dragon_feast", "hex_gourmand"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"farmer_drop", "farmer_drop", "regen", "farmer_drop", "farmer_drop"},
                    new double[]{0.2, 0.4, 2, 0.9, 1.2}, 1.0),
            // 15 魔力泉涌（法力/施法资源）
            new TieredCombo("hexcraft.combo.c15",
                    new String[]{"hex_irons_mana_reservoir", "hex_irons_mana_spring", "hex_irons_mana_weaver", "hex_irons_mana_flow",
                            "hex_irons_mana_reserve", "hex_irons_archmage", "hex_irons_arcane_focus",
                            "hex_irons_coolmaster", "hex_irons_haste_casting", "hex_irons_spellvamp", "hex_arcane_cloak"},
                    new int[]{3, 6, 8, 10, 11}, new String[]{"spell_mana_max", "spell_mana_max", "echo", "spell_mana_max", "spell_mana_max"},
                    new double[]{0.025, 0.05, 0.1, 0.125, 0.175}, 1.0),
            // 16 灾厄猎手（灾厄/凋灵特攻）
            new TieredCombo("hexcraft.combo.c16",
                    new String[]{"hex_cataclysm_boss_mark", "hex_cataclysm_punisher", "hex_cataclysm_wither_soul", "hex_cataclysm_abyssal_gaze",
                            "hex_cataclysm_hellbarrier", "hex_cataclysm_primordial_wrath", "hex_cataclysm_ancient_fury", "hex_cataclysm_bane",
                            "hex_cataclysm_vanguard", "hex_cataclysm_void_guard", "hex_cataclysm_netherite_bulk", "hex_cataclysm_ruin_power"},
                    new int[]{3, 6, 8, 10, 12}, new String[]{"cataclysm_damage", "cataclysm_damage", "execute", "cataclysm_damage", "chain_lightning"},
                    new double[]{0.0375, 0.075, 0.0375, 0.1875, 0.075}, 1.0),
            // 17 叠层之巅（叠层上限特化：坦克引擎 + 迅战之角·速 + 时之焚刃 三件齐备 → 叠层上限 +30%）
            // v0.9.12：本条前身为 HextechEvents.stackCapMultiplier 里的硬编码三件套判定，迁入羁绊体系后
            // 图鉴可见、档位可查、数值随 effect(stack_cap) 汇总，不再是玩家看不见的隐藏规则。
            // prismaticMult 取 1.0：只有一档且凑齐即满档，保留原 ×1.3 强度，不做满员棱彩翻倍。
            new TieredCombo("hexcraft.combo.c17",
                    new String[]{"hex_stack_tank", "hex_swift_horn_quick", "hex_tick_damage_wax"},
                    new int[]{3}, new String[]{"stack_cap"}, new double[]{0.3}, 1.0)
    );

    /** ================= 新增流派 11（10 成员 2/4/6/8/10）================= */
    public static final java.util.List<TieredCombo> NEW_COMBOS = java.util.List.of(
            // 1 连击流（攻速/连发）
            new TieredCombo("hexcraft.combo.v2.combo1",
                    new String[]{"hex_twin_shot", "hex_twin_fire", "hex_crit_cascade", "hex_swiftblade",
                            "hex_swift_feet", "hex_frenzy", "hex_kill_speed_perm", "hex_hurt_speed_boost",
                            "hex_swift_horn_quick", "hex_tacz_multi_shot"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"twin_shot", "twin_shot", "rampage", "twin_shot", "twin_shot"},
                    new double[]{0.0125, 0.025, 0.025, 0.05, 0.0625}, 1.0),
            // 2 暴击流（暴击/暴伤）
            new TieredCombo("hexcraft.combo.v2.combo2",
                    new String[]{"hex_full_crit", "hex_mark_judge", "hex_warmaster", "hex_all_in",
                            "hex_all_out", "hex_frost_crit", "hex_thunderlord", "hex_crit_leech",
                            "hex_hunter_mark", "hex_storm_focus"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"crit_damage", "crit_damage", "full_hp_crit", "crit_damage", "crit_damage"},
                    new double[]{0.025, 0.05, 0.025, 0.125, 0.175}, 1.0),
            // 3 燃烧流（火焰/点燃链）
            new TieredCombo("hexcraft.combo.v2.combo3",
                    new String[]{"hex_fire_blade", "hex_fire_rage", "hex_fire_oath", "hex_fire_warden",
                            "hex_firework_king", "hex_burning_rebound", "hex_nether_fire_lord", "hex_twin_fire",
                            "hex_party_big", "hex_apocalypse"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"fire_chain", "fire_chain", "fire_chain", "fire_chain", "lifesteal"},
                    new double[]{0.025, 0.05, 0.0875, 0.125, 0.0375}, 1.0),
            // 4 护盾流（护盾/应急）
            new TieredCombo("hexcraft.combo.v2.combo4",
                    new String[]{"hex_overshield", "hex_small_shield", "hex_bloodthirst", "hex_combat_shield_surge",
                            "hex_tick_shield_gain", "hex_eternal_shield", "hex_arcane_cloak", "hex_arcane_vamp",
                            "hex_shield_bash", "hex_immortal_wall"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"overshield", "overshield", "shield_low", "overshield", "overshield"},
                    new double[]{0.025, 0.05, 2, 0.0875, 0.175}, 1.0),
            // 5 治疗流（击杀回血/回复）
            new TieredCombo("hexcraft.combo.v2.combo5",
                    new String[]{"hex_blood_artist", "hex_prism_vamp", "hex_undying", "hex_soul_siphon",
                            "hex_soul_reaper", "hex_vampire", "hex_streak_king", "hex_war_cry",
                            "hex_paladin", "hex_black_candle"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"kill_heal", "kill_heal", "lifesteal", "kill_heal", "regen"},
                    new double[]{1, 2, 0.025, 5, 3}, 1.0),
            // 6 召唤流（图腾/仆从/守卫，+4 新建补员；剔除 cataclysm 联动件对齐 10 成员，纯 vanilla 可凑满）
            new TieredCombo("hexcraft.combo.v2.combo6",
                    new String[]{"hex_am_totem", "hex_am_totem_keeper", "hex_goety_warding_wisp", "hex_goety_soul_bulwark",
                            "hex_relic_guardian", "hex_relic_overseer",
                            "hex_summon_overlord", "hex_summon_army", "hex_summon_pact", "hex_summon_totem_master"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"goety_soul", "goety_soul", "shield_low", "goety_soul", "goety_soul"},
                    new double[]{0.025, 0.05, 2, 0.125, 0.175}, 1.0),
            // 7 机动流（移速/机动）
            new TieredCombo("hexcraft.combo.v2.combo7",
                    new String[]{"hex_mobi_blade", "hex_arena_sprint", "hex_frost_step", "hex_swift_feet",
                            "hex_surge_boots", "hex_jump_master", "hex_slow_pursue", "hex_night_hunter",
                            "hex_end_void_walker", "hex_dragon_dance"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"mobi_power", "mobi_power", "rampage", "mobi_power", "mobi_power"},
                    new double[]{0.025, 0.05, 0.025, 0.0875, 0.175}, 1.0),
            // 8 法力流（法力/冷却，+3 新建补员）
            new TieredCombo("hexcraft.combo.v2.combo8",
                    new String[]{"hex_irons_mana_reservoir", "hex_irons_mana_spring", "hex_irons_mana_weaver", "hex_irons_mana_flow",
                            "hex_irons_mana_reserve", "hex_irons_coolmaster", "hex_irons_thrifty",
                            "hex_mana_overload", "hex_mana_tide", "hex_mana_echo"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"spell_mana_max", "spell_mana_max", "echo", "spell_mana_max", "spell_mana_max"},
                    new double[]{0.025, 0.05, 0.1, 0.0875, 0.175}, 1.0),
            // 9 经济流（晶币/掉落）
            new TieredCombo("hexcraft.combo.v2.combo9",
                    new String[]{"hex_goldmine", "hex_gold_rush", "hex_bounty_hunter", "hex_loot_coin",
                            "hex_frost_coin", "hex_dragon_coin", "hex_nether_gold_greed", "hex_reaper",
                            "hex_random_gift", "hex_luck_dice"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"coin_bonus", "coin_bonus", "bounty", "coin_bonus", "coin_bonus"},
                    new double[]{0.1, 0.2, 3, 0.35, 0.7}, 1.0),
            // 10 斩杀流（处决/斩杀线）；v0.9.12 新增破限处决为第 11 名成员，档位仍按 2/4/6/8/10 件计，不要求全员满档
            new TieredCombo("hexcraft.combo.v2.combo10",
                    new String[]{"hex_collector", "hex_executioner", "hex_executioner_eternal", "hex_execute_gold",
                            "hex_execute_haste", "hex_execute_shadow", "hex_fatal_wound", "hex_mark_judge",
                            "hex_tacz_fatal", "hex_irons_execute", "hex_execute_breach"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"execute", "execute", "rampage", "execute", "execute"},
                    new double[]{0.0125, 0.025, 0.025, 0.05, 0.05}, 1.0),
            // 11 生存流（免死/濒死/减伤）
            new TieredCombo("hexcraft.combo.v2.combo11",
                    new String[]{"hex_laststand", "hex_rock_body", "hex_paladin", "hex_death_defier",
                            "hex_death_dance", "hex_end_void_protect", "hex_end_void_walker", "hex_immortal_wall",
                            "hex_black_candle", "hex_thorn_shell"},
                    new int[]{2, 4, 6, 8, 10}, new String[]{"last_stand", "last_stand", "death_defy", "last_stand", "last_stand"},
                    new double[]{0.025, 0.05, 0.05, 0.125, 0.175}, 1.0)
    );

    /** ================= 叠层专家（C 核心：个人叠层上限逐步解锁 2/4/6/8/10 → 400/600/800/1000/∞）================= */
    public static final TieredCombo STACK_MASTER = new TieredCombo("hexcraft.combo.v2.stackmaster",
            new String[]{"hex_heart_steel", "hex_stack_tank", "hex_stack_drake", "hex_combat_perm_mix",
                    "hex_perm_damage_apex", "hex_timed_multi_aspect", "hex_growth_spear", "hex_tick_damage_wax",
                    "hex_mixed_damage_armor", "hex_kill_speed_perm"},
            new int[]{2, 4, 6, 8, 10}, new String[]{"stack_growth", "stack_growth", "stack_growth", "stack_growth", "stack_growth"},
            new double[]{0.05, 0.1, 0.15, 0.2, 0.25}, 1.0);

    /** ================= 叠层之源（v0.9.24：从叠层专家接手上限解锁，5 档 2/4/6/8/10 → 400/600/800/1000/∞）=================
     *  背景：v0.9.12 之前，个人叠层上限由「叠层专家」的件数档位在代码里硬编码给出，而叠层专家的效果声明全是
     *  stack_growth，图鉴上只能看到"叠层变快"、看不到"解锁了多少层"。v0.9.24 把上限解锁独立成这条羁绊：
     *  叠层专家只保留叠层获取，叠层之源负责上限，图鉴可见。
     *  成员 10 件 = 叠层专家前 6 件 + 4 件补充（不朽回响 / 受击永久护盾 / 战斗伤害尖刺 / 渐强之刃），
     *  覆盖永久攻击、永久生命、短时攻击、长期成长四类叠层。
     *  values 存「解锁到多少层」，最后一档 0 表示无上限，渲染层单独翻译成 ∞。 */
    public static final TieredCombo STACK_SOURCE = new TieredCombo("hexcraft.combo.v2.stacksource",
            new String[]{"hex_heart_steel", "hex_stack_tank", "hex_stack_drake", "hex_combat_perm_mix",
                    "hex_perm_damage_apex", "hex_timed_multi_aspect",
                    "hex_immortal_echo", "hex_hurt_perm_shield", "hex_combat_damage_spur", "hex_crescendo_blade"},
            new int[]{2, 4, 6, 8, 10},
            new String[]{"stack_cap_tier", "stack_cap_tier", "stack_cap_tier", "stack_cap_tier", "stack_cap_tier"},
            new double[]{400, 600, 800, 1000, 0}, 1.0);

    /** ================= 逆原 10 条升级（9 成员 1/3/5/7/9 奇数档）================= */
    public static final java.util.List<TieredCombo> TIERED_COMBOS = java.util.List.of(
            // 狂战：近战增伤流（9 件奇数档，满档 +70%）
            new TieredCombo("hexcraft.combo.tier.berserk",
                    new String[]{"hex_niyuan_kuangzhan_1", "hex_niyuan_kuangzhan_2", "hex_niyuan_kuangzhan_3",
                            "hex_niyuan_kuangzhan_4", "hex_niyuan_kuangzhan_5", "hex_niyuan_kuangzhan_6",
                            "hex_niyuan_kuangzhan_7", "hex_niyuan_kuangzhan_8", "hex_niyuan_kuangzhan_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"melee_power", "melee_power", "vamp_rage", "melee_power", "melee_power"},
                    new double[]{0.025, 0.05, 0.0375, 0.125, 0.175}, 1.0, "dlc_niyuan"),
            // 游击：机动+远程流
            new TieredCombo("hexcraft.combo.tier.guerrilla",
                    new String[]{"hex_niyuan_youji_1", "hex_niyuan_youji_2", "hex_niyuan_youji_3",
                            "hex_niyuan_youji_4", "hex_niyuan_youji_5", "hex_niyuan_youji_6",
                            "hex_niyuan_youji_7", "hex_niyuan_youji_8", "hex_niyuan_youji_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"mobi_power", "tacz_power", "twin_shot", "mobi_power", "tacz_power"},
                    new double[]{0.0375, 0.0625, 0.025, 0.1, 0.1875}, 1.0, "dlc_niyuan"),
            // 壁垒：防御流
            new TieredCombo("hexcraft.combo.tier.bulwark",
                    new String[]{"hex_niyuan_bilei_1", "hex_niyuan_bilei_2", "hex_niyuan_bilei_3",
                            "hex_niyuan_bilei_4", "hex_niyuan_bilei_5", "hex_niyuan_bilei_6",
                            "hex_niyuan_bilei_7", "hex_niyuan_bilei_8", "hex_niyuan_bilei_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"overshield", "overshield", "shield_low", "overshield", "thorn_armor"},
                    new double[]{0.0375, 0.0625, 2, 0.1375, 0.2}, 1.0, "dlc_niyuan"),
            // 狙击：远程/枪械流（v0.9.5 新增，4→9 成员）
            new TieredCombo("hexcraft.combo.tier.sniping",
                    new String[]{"hex_niyuan_sniping_1", "hex_niyuan_sniping_2", "hex_niyuan_sniping_3", "hex_niyuan_sniping_4",
                            "hex_niyuan_sniping_5", "hex_niyuan_sniping_6", "hex_niyuan_sniping_7", "hex_niyuan_sniping_8", "hex_niyuan_sniping_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"tacz_power", "tacz_power", "tacz_crit", "tacz_power", "tacz_power"},
                    new double[]{0.025, 0.05, 0.2, 0.125, 0.175}, 1.0, "dlc_niyuan"),
            // 狩猎：近战/猎杀流（v0.9.5 新增，4→9 成员）
            new TieredCombo("hexcraft.combo.tier.hunting",
                    new String[]{"hex_niyuan_hunting_1", "hex_niyuan_hunting_2", "hex_niyuan_hunting_3", "hex_niyuan_hunting_4",
                            "hex_niyuan_hunting_5", "hex_niyuan_hunting_6", "hex_niyuan_hunting_7", "hex_niyuan_hunting_8", "hex_niyuan_hunting_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"melee_power", "melee_power", "execute", "melee_power", "melee_power"},
                    new double[]{0.025, 0.05, 0.025, 0.125, 0.175}, 1.0, "dlc_niyuan"),
            // 力场：站桩增伤流（专属 hook stand_power）
            new TieredCombo("hexcraft.combo.tier.forcefield",
                    new String[]{"hex_niyuan_licheng_1", "hex_niyuan_licheng_2", "hex_niyuan_licheng_3",
                            "hex_niyuan_licheng_4", "hex_niyuan_licheng_5", "hex_niyuan_licheng_6",
                            "hex_niyuan_licheng_7", "hex_niyuan_licheng_8", "hex_niyuan_licheng_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"stand_power", "stand_power", "stand_power", "stand_power", "stand_power"},
                    new double[]{0.5, 1.0, 1.5, 2.0, 2.5}, 1.0, "dlc_niyuan"),
            // 瞬暴：暴击爆发流
            new TieredCombo("hexcraft.combo.tier.burst",
                    new String[]{"hex_niyuan_shunbao_1", "hex_niyuan_shunbao_2", "hex_niyuan_shunbao_3",
                            "hex_niyuan_shunbao_4", "hex_niyuan_shunbao_5", "hex_niyuan_shunbao_6",
                            "hex_niyuan_shunbao_7", "hex_niyuan_shunbao_8", "hex_niyuan_shunbao_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"full_hp_crit", "crit_damage", "crit_damage", "crit_damage", "crit_damage"},
                    new double[]{0.02, 0.0375, 0.0625, 0.0875, 0.125}, 1.0, "dlc_niyuan"),
            // 技战：技能战流
            new TieredCombo("hexcraft.combo.tier.techwar",
                    new String[]{"hex_niyuan_jizhan_1", "hex_niyuan_jizhan_2", "hex_niyuan_jizhan_3",
                            "hex_niyuan_jizhan_4", "hex_niyuan_jizhan_5", "hex_niyuan_jizhan_6",
                            "hex_niyuan_jizhan_7", "hex_niyuan_jizhan_8", "hex_niyuan_jizhan_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"spell_power", "fire_chain", "spell_mana_max", "spell_power", "fire_chain"},
                    new double[]{0.025, 0.0375, 0.05, 0.0625, 0.075}, 1.0, "dlc_niyuan"),
            // 共振：装备共鸣流（v0.9.7 新增，4→9 成员）
            new TieredCombo("hexcraft.combo.tier.resonance",
                    new String[]{"hex_niyuan_gongzhen_1", "hex_niyuan_gongzhen_2", "hex_niyuan_gongzhen_3", "hex_niyuan_gongzhen_4",
                            "hex_niyuan_gongzhen_5", "hex_niyuan_gongzhen_6", "hex_niyuan_gongzhen_7", "hex_niyuan_gongzhen_8", "hex_niyuan_gongzhen_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"equip_power", "equip_power", "coin_bonus", "equip_power", "equip_power"},
                    new double[]{0.02, 0.0375, 0.15, 0.0875, 0.125}, 1.0, "dlc_niyuan"),
            // 叠叠乐：叠层增幅流
            new TieredCombo("hexcraft.combo.tier.stackloop",
                    new String[]{"hex_niyuan_diediele_1", "hex_niyuan_diediele_2", "hex_niyuan_diediele_3",
                            "hex_niyuan_diediele_4", "hex_niyuan_diediele_5", "hex_niyuan_diediele_6",
                            "hex_niyuan_diediele_7", "hex_niyuan_diediele_8", "hex_niyuan_diediele_9"},
                    new int[]{1, 3, 5, 7, 9}, new String[]{"stack_growth", "stack_growth", "stack_chance", "stack_growth", "stack_growth"},
                    new double[]{0.0375, 0.0625, 0.025, 0.1, 0.175}, 1.0, "dlc_niyuan")
            // v0.9.12 修正：叠叠乐第 3 档原为 tick_gain，该键是海克斯叠层定义的标记字段，不是可汇总的数值效果，
            // 凑到 5 件时这一档完全空转。现改为 stack_chance，叠层触发几率，与本羁绊主题一致。

    );

    /** ================= 大乱斗 9 套（官方还原 2/4/6/8，v0.9.7 机制化，保留不动）================= */
    public static final java.util.List<TieredCombo> ARAM_SETS = java.util.List.of(
            new TieredCombo("hexcraft.combo.tier.aram.firecracker",
                    new String[]{"hex_tacz_crit_chain", "hex_tacz_crit_master", "hex_tacz_multi_shot", "hex_tacz_double_shot",
                            "hex_full_crit", "hex_twin_fire", "hex_aram_firecracker_rocket", "hex_aram_firecracker_bounce"},
                    new int[]{2, 4, 6, 8}, new String[]{"twin_shot", "twin_shot", "tacz_power", "tacz_power"},
                    new double[]{0.025, 0.05, 0.1, 0.125}, 1.0, "dlc_aram"),
            new TieredCombo("hexcraft.combo.tier.aram.stackasaurus",
                    new String[]{"hex_heart_steel", "hex_stack_tank", "hex_tick_damage_wax", "hex_artifact_endurance_stack",
                            "hex_blade_draw_song", "hex_perm_damage_apex", "hex_aram_titan_stack", "hex_aram_ramp_rex"},
                    new int[]{2, 4, 6, 8}, new String[]{"stack_growth", "stack_growth", "stack_growth", "stack_growth"},
                    new double[]{0.0625, 0.125, 0.1875, 0.25}, 1.0, "dlc_aram"),
            new TieredCombo("hexcraft.combo.tier.aram.archmage",
                    new String[]{"hex_irons_archmage", "hex_irons_arcane_focus", "hex_irons_archon_judgment",
                            "hex_irons_elemental_surge", "hex_irons_blood_siphon", "hex_aram_archmage_crest", "hex_aram_arcane_pact"},
                    new int[]{2, 4, 6, 7}, new String[]{"echo", "echo", "spell_power", "spell_power"},
                    new double[]{0.05, 0.10, 0.075, 0.1125}, 1.0, "dlc_aram"),
            new TieredCombo("hexcraft.combo.tier.aram.weewoo",
                    new String[]{"hex_undying", "hex_eternal_shield", "hex_immortal_wall", "hex_combat_shield_surge",
                            "hex_arcane_cloak", "hex_irons_healing_word", "hex_aram_wee_woo", "hex_aram_ambulance_sirens"},
                    new int[]{2, 4, 6, 8}, new String[]{"shield_low", "shield_low", "overshield", "overshield"},
                    new double[]{2.0, 4.0, 0.075, 0.125}, 1.0, "dlc_aram"),
            new TieredCombo("hexcraft.combo.tier.aram.highroller",
                    new String[]{"hex_midas_hand", "hex_trade_discount", "hex_golden_bet", "hex_dice_maniac",
                            "hex_opening_forge", "hex_emc_savings_wallet", "hex_aram_high_roller", "hex_aram_lucky_anvil"},
                    new int[]{2, 4, 6, 8}, new String[]{"relic_drop", "relic_drop", "coin_bonus", "coin_bonus"},
                    new double[]{0.10, 0.20, 0.20, 0.30}, 1.0, "dlc_aram"),
            new TieredCombo("hexcraft.combo.tier.aram.deathknell",
                    new String[]{"hex_death_defier", "hex_executioner_eternal", "hex_corpse_explosion",
                            "hex_soul_reaper", "hex_am_titan_subdue", "hex_death_dance", "hex_aram_deathknell", "hex_aram_bell_toll",
                            "hex_aram_dive_bomb", "hex_aram_clown_college"},
                    new int[]{2, 4, 6, 8}, new String[]{"death_defy", "death_defy", "last_stand", "last_stand"},
                    new double[]{0.05, 0.10, 0.0375, 0.05}, 1.0, "dlc_aram"),
            new TieredCombo("hexcraft.combo.tier.aram.fullyautomated",
                    new String[]{"hex_irons_echo", "hex_irons_echo_recovery", "hex_echo_blade", "hex_echo_cloak",
                            "hex_echo_rampage", "hex_irons_swiftcast", "hex_aram_auto_cast", "hex_aram_factory_line"},
                    new int[]{2, 4, 6, 8}, new String[]{"rampage", "rampage", "spell_power", "spell_power"},
                    new double[]{0.025, 0.05, 0.075, 0.1125}, 1.0, "dlc_aram"),
            new TieredCombo("hexcraft.combo.tier.aram.snowday",
                    new String[]{"hex_tf_snow_queen_bane", "hex_tf_snow_queen_ward", "hex_tf_yeti_winter", "hex_irons_frost_shard",
                            "hex_ice_crown", "hex_frost_crit", "hex_aram_snowball", "hex_aram_ice_day"},
                    new int[]{2, 4, 6, 8}, new String[]{"frost_bite", "frost_bite", "mobi_power", "tf_snow_queen_damage"},
                    new double[]{0.025, 0.05, 0.075, 0.1}, 1.0, "dlc_aram"),
            new TieredCombo("hexcraft.combo.tier.aram.makeitrain",
                    new String[]{"hex_midas_hand", "hex_trade_discount", "hex_golden_bet", "hex_opening_forge",
                            "hex_emc_savings_wallet", "hex_dice_maniac", "hex_aram_make_it_rain", "hex_aram_coin_trail"},
                    new int[]{2, 4, 6, 8}, new String[]{"bounty", "bounty", "coin_bonus", "coin_bonus"},
                    new double[]{3.0, 6.0, 0.20, 0.30}, 1.0, "dlc_aram")
    );

    /** 全部分档羁绊（三件套 16 + 新增流派 11 + 叠层专家 1 + 逆原 10 + 大乱斗 9 = 47 条），服务端结算与客户端渲染统一遍历此表 */
    public static java.util.List<TieredCombo> allTiered() {
        java.util.List<TieredCombo> out = new java.util.ArrayList<>(BASE_COMBOS.size() + NEW_COMBOS.size() + 1
                + TIERED_COMBOS.size() + ARAM_SETS.size());
        out.addAll(BASE_COMBOS);
        out.addAll(NEW_COMBOS);
        out.add(STACK_MASTER);
        out.addAll(TIERED_COMBOS);
        out.addAll(ARAM_SETS);
        return java.util.List.copyOf(out);
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

    /** 叠层专家当前档位（0-5）：v2-C 个人叠层上限逐步解锁依据（档 1→400 / 2→600 / 3→800 / 4→1000 / 5→∞） */
    public static int stackMasterTier(java.util.Set<String> owned) {
        return tierOf(STACK_MASTER, owned);
    }

    /** v0.9.12 叠层专家的有效视图（group=2；若其成员全部被过滤则返回 null）。 */
    public static ResolvedCombo resolvedStackMaster() {
        for (ResolvedCombo rc : resolvedAll()) if (rc.group() == 2) return rc;
        return null;
    }

    /** v0.9.24 叠层之源的有效视图（group=5；负责个人叠层上限解锁）。 */
    public static ResolvedCombo resolvedStackSource() {
        for (ResolvedCombo rc : resolvedAll()) if (rc.group() == 5) return rc;
        return null;
    }

    // ================= v0.9.12 运行时有效羁绊视图（联动成员过滤 + 档位重算，数据驱动） =================
    /** 解析后的羁绊：成员仅含当前环境启用的海克斯；tierCounts 按可用成员重算。
     *  originalTotal = 原始成员数（参考）；group = 所属分组（0 原生/1 新流派/2 叠层专家/3 逆原/4 大乱斗）。 */
    public record ResolvedCombo(String nameKey, String[] members, int[] tierCounts, String[] effects, double[] values,
                                double prismaticMult, int originalTotal, int group) {}

    private static volatile java.util.List<ResolvedCombo> resolvedCache = null;

    /** 全部有效羁绊（已剔除"无任何可用成员"的纯联动羁绊）；带缓存，配置/包变更时由 invalidateResolved 失效。 */
    public static java.util.List<ResolvedCombo> resolvedAll() {
        java.util.List<ResolvedCombo> c = resolvedCache;
        if (c == null) {
            synchronized (ComboRegistry.class) {
                c = resolvedCache;
                if (c == null) {
                    java.util.List<ResolvedCombo> out = new java.util.ArrayList<>();
                    for (TieredCombo tc : BASE_COMBOS) addResolved(out, tc, 0);
                    for (TieredCombo tc : NEW_COMBOS) addResolved(out, tc, 1);
                    addResolved(out, STACK_MASTER, 2);
                    addResolved(out, STACK_SOURCE, 5);
                    for (TieredCombo tc : TIERED_COMBOS) addResolved(out, tc, 3);
                    for (TieredCombo tc : ARAM_SETS) addResolved(out, tc, 4);
                    c = java.util.List.copyOf(out);
                    resolvedCache = c;
                }
            }
        }
        return c;
    }

    private static void addResolved(java.util.List<ResolvedCombo> out, TieredCombo tc, int group) {
        ResolvedCombo rc = resolve(tc, group);
        if (rc != null) out.add(rc);
    }

    /** 解析单条羁绊：过滤未启用成员 → 重算档位；无可用成员返回 null（整条隐藏）。 */
    public static ResolvedCombo resolve(TieredCombo tc, int group) {
        // v0.9.16 包归属：所属内置 DLC 停用时整条羁绊不参与结算与展示（成员级过滤之外的第二道闸）
        if (tc.owner() != null && !com.hexcraft.hextech.HextechRegistry.isSourceEnabled(tc.owner(), "hex")) return null;
        String[] orig = tc.members();
        int m = orig.length;
        // 1) 过滤当前环境启用的成员（contentType=hex；未装联动 mod / 开关关闭 / 所属 DLC 包停用均自动剔除）
        java.util.List<String> valid = new java.util.ArrayList<>();
        for (String id : orig) {
            // v0.9.12 统一解析：联机客户端可回退到同步缓存（resolveDef），避免把同步成员误判缺失
            HextechDefinition d = HextechRegistry.resolveDef(id);
            if (d != null && HextechRegistry.isEnabled(d, "hex")) valid.add(id);
        }
        int n = valid.size();
        if (n == 0) return null;
        String[] members = valid.toArray(new String[0]);
        // 2) 实际档位数 = min(原档数, 可用成员数)
        int k = tc.tierCounts().length;
        int kp = Math.min(k, n);
        int[] counts = new int[kp];
        String[] effects = new String[kp];
        double[] values = new double[kp];
        for (int j = 0; j < kp; j++) {
            // 原档位索引均匀映射：kp=1 取基础档；kp=k 时与原档一一对应（仅用于取效果/数值）
            int srcIdx = (kp == 1) ? 0 : Math.round((float) j * (k - 1) / (kp - 1));
            effects[j] = tc.effects()[srcIdx];
            values[j] = tc.values()[srcIdx];
            counts[j] = (int) Math.round((double) tc.tierCounts()[srcIdx] * n / m);
        }
        // 3) 满档 = 集齐全部可用成员；再用夹逼 + 双向规整保证各档严格递增且都可达成。
        // 旧实现只在末尾做一次 +1，剔除比例较大时会把高档推到超过可用成员数 n，导致该档永远无法达成。
        counts[kp - 1] = n;
        // 夹逼：下界 j+1（第 j 档至少需要 j+1 件），上界 n-(kp-1-j)（给后续档位留出递增空间）
        for (int j = 0; j < kp; j++) {
            int lo = j + 1;
            int hi = n - (kp - 1 - j);
            counts[j] = Math.min(Math.max(counts[j], lo), hi);
        }
        // 正向保证严格递增，再反向压回可行上界，二者共同保证 1<=...<n 且末档恰为 n
        for (int j = 1; j < kp; j++) {
            counts[j] = Math.max(counts[j], counts[j - 1] + 1);
        }
        for (int j = kp - 2; j >= 0; j--) {
            counts[j] = Math.min(counts[j], counts[j + 1] - 1);
        }
        return new ResolvedCombo(tc.nameKey(), members, counts, effects, values, tc.prismaticMult(), m, group);
    }

    /** 失效解析缓存（在 HextechRegistry.invalidatePoolCache 中联动调用，配置/包/环境变更即时重建）。 */
    public static void invalidateResolved() { resolvedCache = null; }

    /** 玩家在某有效羁绊的当前档数（0=未激活；拥有件数 ≥ 档件数即激活）。 */
    public static int tierOf(ResolvedCombo rc, java.util.Set<String> owned) {
        int have = 0;
        for (String id : rc.members()) if (owned.contains(id)) have++;
        int tier = 0;
        for (int t = 0; t < rc.tierCounts().length; t++) if (have >= rc.tierCounts()[t]) tier = t + 1;
        return tier;
    }

    /** 玩家在某有效羁绊拥有的成员件数。 */
    public static int ownedCount(ResolvedCombo rc, java.util.Set<String> owned) {
        int have = 0;
        for (String id : rc.members()) if (owned.contains(id)) have++;
        return have;
    }

    public static int tierOf(ResolvedCombo rc, java.util.List<com.hexcraft.capability.PlayerData.HexEntry> hexes) {
        java.util.Set<String> owned = new java.util.HashSet<>();
        for (com.hexcraft.capability.PlayerData.HexEntry e : hexes) owned.add(e.id);
        return tierOf(rc, owned);
    }

    /** 叠层类海克斯全集（叠层联动档位判定：拥有 ≥2 → 叠层获取/上限 +20%，≥4 → +40%；上限额外 ×1.3 组合为代码特殊判定） */
    public static final java.util.Set<String> STACK_HEXES = java.util.Set.of(
            "hex_tick_damage_wax", "hex_timed_multi_aspect", "hex_artifact_endurance_stack",
            "hex_blade_draw_song", "hex_blade_scabbard_flow", "hex_blade_soul_flurry",
            "hex_combat_perm_mix", "hex_heart_steel", "hex_hurt_speed_boost", "hex_kill_speed_perm",
            "hex_mowzie_feral_instinct", "hex_mowzie_frenzy_claw", "hex_perm_damage_apex",
            "hex_stack_tank", "hex_swift_horn_quick");
}
