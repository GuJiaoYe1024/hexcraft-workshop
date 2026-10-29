package com.hexcraft.hextech;

import com.hexcraft.capability.PlayerData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 海克斯与锻造属性注册表（内置定义；数据包通过 HexDataLoader 覆盖/新增） */
public class HextechRegistry {

    private static final Map<String, HextechDefinition> DEFINITIONS = new LinkedHashMap<>();
    /** 装备注册表（数据驱动；内置由 gen_equips.py 生成，加载器装载） */
    private static final Map<String, HextechDefinition> EQUIPMENTS = new LinkedHashMap<>();

    // ================= 内容来源分类（原版 / 联动 mod 内容，自动检测 + 配置开关） =================
    /** 海克斯 id 前缀 → 依赖 modid（联动来源注册表；未来联动 mod 内容在此注册，自动检测、默认启用、可配置关闭）。
     *  必须在静态注册块之前初始化，避免类初始化期间 HextechDefinition 构造触发判源时读到 null */
    private static final Map<String, String> SOURCE_PREFIXES = new LinkedHashMap<>();
    static {
        // 内容 id 前缀 → 依赖 modid。命名规约：hex_<源>_* / equip_<源>_*，同时兼容裸前缀（tacz_）。
        SOURCE_PREFIXES.put("tacz_", "tacz");
        SOURCE_PREFIXES.put("hex_tacz_", "tacz");
        SOURCE_PREFIXES.put("equip_tacz_", "tacz");
        // v0.6.20 Iron's Spells 'n Spellbooks 法术联动内容前缀（自动检测 + 配置开关）
        SOURCE_PREFIXES.put("hex_irons_", "irons_spellbooks");
        SOURCE_PREFIXES.put("equip_irons_", "irons_spellbooks");
        // v0.6.21 冰与火之歌（Ice and Fire CE）联动内容前缀（自动检测 + 配置开关；未安装 iceandfire 时自动隐藏）
        SOURCE_PREFIXES.put("hex_iaf_", "iceandfire");
        SOURCE_PREFIXES.put("equip_iaf_", "iceandfire");
        // v0.6.21 Alex's Caves 联动内容前缀（未安装 alexscaves 时自动隐藏）
        SOURCE_PREFIXES.put("hex_ac_", "alexscaves");
        SOURCE_PREFIXES.put("equip_ac_", "alexscaves");
        // v0.6.21 暮色森林（Twilight Forest）联动内容前缀（未安装 twilightforest 时自动隐藏）
        SOURCE_PREFIXES.put("hex_tf_", "twilightforest");
        SOURCE_PREFIXES.put("equip_tf_", "twilightforest");
        // v0.8.3 第二批联动内容前缀（末地灾变/巫术/Mowzie/神器/遗物/拔刀剑/农夫乐事/FTB Quests/ProjectE）
        SOURCE_PREFIXES.put("hex_cataclysm_", "cataclysm");
        SOURCE_PREFIXES.put("hex_goety_", "goety");
        SOURCE_PREFIXES.put("hex_mowzie_", "mowziesmobs");
        SOURCE_PREFIXES.put("hex_artifact_", "artifacts");
        SOURCE_PREFIXES.put("hex_relic_", "relics");
        SOURCE_PREFIXES.put("hex_blade_", "slashblade");
        SOURCE_PREFIXES.put("hex_farmer_", "farmersdelight");
        SOURCE_PREFIXES.put("hex_quest_", "ftbquests");
        SOURCE_PREFIXES.put("hex_emc_", "projecte");
    }

    /** 注册联动来源：id 前缀 → 依赖 modid（如 registerSourcePrefix("exm_", "exm")）。调用方负责同步 HexConfig 开关 */
    public static void registerSourcePrefix(String prefix, String modid) {
        SOURCE_PREFIXES.put(prefix, modid);
    }

    /** 依据 id 前缀判定内容来源 modid（未注册前缀默认 vanilla，即本 mod 原版内容） */
    public static String sourceOf(String id) {
        if (id == null) return "vanilla";
        for (Map.Entry<String, String> e : SOURCE_PREFIXES.entrySet()) {
            if (id.startsWith(e.getKey())) return e.getValue();
        }
        return "vanilla";
    }
    private static final Map<String, HextechDefinition.ModSpec> FORGE_MODS = new LinkedHashMap<>();
    /** v0.6.21 事件型锻造词条：stat → [事件效果键, 每级倍率]。效果在 HextechEvents.effect() 按词条汇总
     *  （不注册属性，避免属性应用层 NPE；档位倍率 × 事件倍率，与属性词条同规格可叠加）。 */
    private static final Map<String, String[]> FORGE_EVENT_MODS = new LinkedHashMap<>();
    /** v0.6.0 锻造机制词条迁出（全部海克斯化）：fire/frost/poison/leech/pierce/crit/execute/splash/
     *  lightning/stack/dance/bloodlust/reaper/thorn/gold/regen → 对应海克斯（含 TACZ gun/gun_crit/ammo）；
     *  锻造器仅保留静态属性，强度靠档位倍率区分。以下常量保留供历史/总表统计，不再进入锻造池。 */
    @Deprecated
    public static final List<String> FORGE_MECHANICS = List.of(
            "fire_aspect", "frost_aspect", "poison_aspect", "leech_aspect", "pierce_aspect",
            "thorn_aspect", "regen_aspect", "gold_aspect",
            "crit_aspect", "execute_aspect", "splash_aspect",
            "lightning_aspect", "stack_aspect", "dance_aspect", "bloodlust_aspect", "reaper_aspect");
    @Deprecated
    public static final List<String> FORGE_TACZ_MECHANICS = List.of(
            "gun_aspect", "gun_crit_aspect", "ammo_aspect");
    public static final int[] FORGE_TIER_MULT = {1, 2, 3};
    public static final String[] FORGE_TIER_KEYS = {"basic", "gold", "prismatic"};

    /** v0.6.1 锻造档位倍率（配置化，服务端生效；默认 ×1/×2/×3，Cloth 可改——例 1/2/4 爽玩）。
     *  服务端属性计算与客户端 tooltip 展示统一走这里，保证显示=实际加成。非法档位钳制回 1.0。 */
    public static double tierMult(int tier) {
        if (tier == 1) return com.hexcraft.config.HexConfig.FORGE_MULT_GOLD.get();
        if (tier == 2) return com.hexcraft.config.HexConfig.FORGE_MULT_PRISMATIC.get();
        if (tier == 0) return com.hexcraft.config.HexConfig.FORGE_MULT_SILVER.get();
        return 1.0;
    }

    static {
        // ---- 白银（内置兜底，其余由数据包定义；数据包优先覆盖）----
        register("hex_berserk", HextechRarity.SILVER, 10, List.of(mod(Attributes.ATTACK_DAMAGE, 1.5)), null);
        register("hex_frenzy", HextechRarity.SILVER, 10, List.of(mod(Attributes.ATTACK_SPEED, 0.8), mod(Attributes.MOVEMENT_SPEED, 0.02)), null);
        register("hex_thunderlord", HextechRarity.SILVER, 10, List.of(mod(Attributes.ATTACK_SPEED, 0.2)), "crit:0.25");

        // ---- 黄金（内置兜底）----
        register("hex_swiftblade", HextechRarity.GOLD, 8, List.of(mod(Attributes.ATTACK_SPEED, 0.25), mod(Attributes.ATTACK_DAMAGE, 0.6)), null);
        register("hex_tracker", HextechRarity.GOLD, 8, List.of(mod(Attributes.MOVEMENT_SPEED, 0.015), mod(Attributes.ATTACK_DAMAGE, 0.6)), null);
        register("hex_reaper", HextechRarity.GOLD, 6, List.of(), "xp_bonus:0.3,coin_bonus:0.25");

        // ---- 棱彩（6）----
        register("hex_colossus", HextechRarity.PRISMATIC, 8, List.of(mod(Attributes.MAX_HEALTH, 12.0), mod(Attributes.ARMOR, 4.0)), null);
        register("hex_titan", HextechRarity.PRISMATIC, 6, List.of(mod(Attributes.ATTACK_DAMAGE, 2.5), mod(Attributes.MAX_HEALTH, 6.0)), null);
        register("hex_goldmine", HextechRarity.PRISMATIC, 6, List.of(), "coin_bonus:0.6");
        register("hex_scholar", HextechRarity.PRISMATIC, 6, List.of(), "xp_bonus:0.6");
        register("hex_legend", HextechRarity.PRISMATIC, 5, List.of(
                mod(Attributes.ATTACK_DAMAGE, 1.0), mod(Attributes.ARMOR, 2.0), mod(Attributes.MAX_HEALTH, 4.0),
                mod(Attributes.MOVEMENT_SPEED, 0.008), mod(Attributes.ATTACK_SPEED, 0.15)), null);
        register("hex_warden", HextechRarity.PRISMATIC, 5, List.of(
                mod(Attributes.ARMOR, 8.0), mod(Attributes.KNOCKBACK_RESISTANCE, 0.2), mod(Attributes.MAX_HEALTH, 8.0)), null);

        // ---- v0.6.15 栏位扩展海克斯（此前 lang 文案已备好但注册表漏注册，玩家永远抽不到）----
        // 棱彩专属、低权重（权重 3，与棱彩锻造器 slot_boost 词条权重一致）：抽到即解锁 1 个海克斯栏位，
        // 不占普通栏位（occupiedSlots 排除）、可叠级（每级 +1，hexSlotLimit 计入等级）；
        // 无属性 mods、无事件，纯解锁用途。
        register("hex_slot_boost", HextechRarity.PRISMATIC, 3, List.of(), null);

        // ---- 锻造属性池（倍率 1/2/3 对应基础/黄金/棱彩锻造器；无上限锻造 → 数值全部下调防崩坏）----
        // v0.5.0 数值上调：基础生命/攻击/防御多给一点，配得上三档价格
        FORGE_MODS.put("atk", mod(Attributes.ATTACK_DAMAGE, 0.5));
        FORGE_MODS.put("armor", mod(Attributes.ARMOR, 1.0));
        FORGE_MODS.put("hp", mod(Attributes.MAX_HEALTH, 2.0));
        FORGE_MODS.put("spd", mod(Attributes.MOVEMENT_SPEED, 0.0015));
        FORGE_MODS.put("aspd", mod(Attributes.ATTACK_SPEED, 0.025));
        FORGE_MODS.put("luck", mod(Attributes.LUCK, 0.25));
        FORGE_MODS.put("knockback", mod(Attributes.ATTACK_KNOCKBACK, 0.12));
        FORGE_MODS.put("resist", mod(Attributes.KNOCKBACK_RESISTANCE, 0.025));
        FORGE_MODS.put("tough", mod(Attributes.ARMOR_TOUGHNESS, 0.25));
        // v0.5.0 棱彩百分比词条（乘法叠加；applyModifiers 对这两个 stat 走 MULTIPLY_BASE）
        // 数值上调：棱彩每档一次 +5% 生命 / +3% 攻速（原 2%/1.5% 太少）
        FORGE_MODS.put("hppct", mod(Attributes.MAX_HEALTH, 0.05));        // 每级最大生命 +5%
        FORGE_MODS.put("aspdpct", mod(Attributes.ATTACK_SPEED, 0.03));    // 每级攻击速度 +3%
    }

    /** Forge 扩展属性（ENTITY_REACH/STEP_HEIGHT）在注册完成后惰性加入锻造池，避免静态块时机问题 */
    private static boolean forgeExtReady = false;
    private static void ensureForgeExt() {
        if (forgeExtReady) return;
        forgeExtReady = true;
        try {
            FORGE_MODS.put("reach", mod(net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(), 0.3));      // 攻击距离
            FORGE_MODS.put("step", mod(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get(), 0.2)); // 迈步高度
        } catch (Exception ignored) { }
        // v0.6.20 Iron's Spells 法术锻造词条（属性型；未安装该 mod 时属性注册表不存在 → 自动剔除，词条不进池）
        try {
            FORGE_MODS.put("spell_power", mod(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER.get(), 0.5));      // 总法术强度 +0.5/级
            FORGE_MODS.put("max_mana", mod(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get(), 3.0));           // 魔力上限 +3/级
            FORGE_MODS.put("mana_regen", mod(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN.get(), 0.5));       // 魔力回复 +0.5/级
            FORGE_MODS.put("spell_cd", mod(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION.get(), 0.02)); // 冷却缩减 +2%/级
            FORGE_MODS.put("cast_haste", mod(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION.get(), 0.02)); // 施法时间 -2%/级
        } catch (Throwable ignored) { }
        // v0.6.21 三联动事件型锻造词条（效果键进 HextechEvents.effect() 按等级×档位倍率汇总；
        //  未安装对应 mod 时由 forgePool 按 isSourceEnabled("forge") 自动过滤，不进抽取池）
        try {
            FORGE_EVENT_MODS.put("dragon_bane", new String[]{"iaf_dragon_damage", "0.05"});    // 屠龙者：对龙族（三龙/海德拉）增伤 5%/级
            FORGE_EVENT_MODS.put("radiation_ward", new String[]{"ac_radiation_resist", "0.05"}); // 辐射防护：受辐射伤害 -5%/级
            FORGE_EVENT_MODS.put("deep_breath", new String[]{"ac_deep_breath", "1"});           // 深渊呼吸：深渊群系水下呼吸 1 级/级
            FORGE_EVENT_MODS.put("boss_bane", new String[]{"tf_boss_damage", "0.05"});         // 弑王：对暮色 BOSS 增伤 5%/级
            // v0.6.22 Alex's Mobs 事件型锻造词条
            FORGE_EVENT_MODS.put("beast_bane", new String[]{"am_mob_damage", "0.05"});         // 猎兽：对 Alex's Mobs 生物增伤 5%/级
        } catch (Throwable ignored) { }
    }

    private static HextechDefinition.ModSpec mod(Attribute attr, double perLevel) {
        return new HextechDefinition.ModSpec(attr, perLevel);
    }

    private static void register(String id, HextechRarity rarity, int weight, List<HextechDefinition.ModSpec> mods, String event) {
        DEFINITIONS.put(id, new HextechDefinition(id, rarity, weight, mods, event));
    }

    public static HextechDefinition get(String id) { return DEFINITIONS.get(id); }
    public static Collection<HextechDefinition> all() { return DEFINITIONS.values(); }
    public static void put(String id, HextechDefinition def) { DEFINITIONS.put(id, def); }

    /** 来源是否启用（按内容小类细分）：原版恒可用；联动来源 = 已安装该 mod 且对应小类开关开启（总开关 && 细分开关） */
    public static boolean isSourceEnabled(String modid, String contentType) {
        if (modid == null || "vanilla".equals(modid)) return true;
        if (!net.minecraftforge.fml.ModList.get().isLoaded(modid)) return false; // 自动检测：未安装 → 不启用
        if ("tacz".equals(modid)) {
            boolean master = com.hexcraft.config.HexConfig.ENABLE_TACZ_CONTENT.get();
            boolean sub = switch (contentType) {
                case "hex" -> com.hexcraft.config.HexConfig.ENABLE_TACZ_HEXTECH.get();
                case "forge" -> com.hexcraft.config.HexConfig.ENABLE_TACZ_FORGE.get();
                case "equip" -> com.hexcraft.config.HexConfig.ENABLE_TACZ_EQUIP.get();
                default -> true;
            };
            return master && sub;
        }
        // v0.6.20 Iron's Spells 联动来源：总开关 + 内容小类细分（同 TACZ 结构）
        if ("irons_spellbooks".equals(modid)) {
            boolean master = com.hexcraft.config.HexConfig.ENABLE_IRONS_CONTENT.get();
            boolean sub = switch (contentType) {
                case "hex" -> com.hexcraft.config.HexConfig.ENABLE_IRONS_HEXTECH.get();
                case "forge" -> com.hexcraft.config.HexConfig.ENABLE_IRONS_FORGE.get();
                case "equip" -> com.hexcraft.config.HexConfig.ENABLE_IRONS_EQUIP.get();
                default -> true;
            };
            return master && sub;
        }
        // v0.6.21 冰与火之歌联动来源
        if ("iceandfire".equals(modid)) {
            boolean master = com.hexcraft.config.HexConfig.ENABLE_IAF_CONTENT.get();
            boolean sub = switch (contentType) {
                case "hex" -> com.hexcraft.config.HexConfig.ENABLE_IAF_HEXTECH.get();
                case "forge" -> com.hexcraft.config.HexConfig.ENABLE_IAF_FORGE.get();
                case "equip" -> com.hexcraft.config.HexConfig.ENABLE_IAF_EQUIP.get();
                default -> true;
            };
            return master && sub;
        }
        // v0.6.21 Alex's Caves 联动来源
        if ("alexscaves".equals(modid)) {
            boolean master = com.hexcraft.config.HexConfig.ENABLE_AC_CONTENT.get();
            boolean sub = switch (contentType) {
                case "hex" -> com.hexcraft.config.HexConfig.ENABLE_AC_HEXTECH.get();
                case "forge" -> com.hexcraft.config.HexConfig.ENABLE_AC_FORGE.get();
                case "equip" -> com.hexcraft.config.HexConfig.ENABLE_AC_EQUIP.get();
                default -> true;
            };
            return master && sub;
        }
        // v0.6.21 暮色森林联动来源
        if ("twilightforest".equals(modid)) {
            boolean master = com.hexcraft.config.HexConfig.ENABLE_TF_CONTENT.get();
            boolean sub = switch (contentType) {
                case "hex" -> com.hexcraft.config.HexConfig.ENABLE_TF_HEXTECH.get();
                case "forge" -> com.hexcraft.config.HexConfig.ENABLE_TF_FORGE.get();
                case "equip" -> com.hexcraft.config.HexConfig.ENABLE_TF_EQUIP.get();
                default -> true;
            };
            return master && sub;
        }
        // v0.6.22 Alex's Mobs 联动来源（同结构）
        if ("alexsmobs".equals(modid)) {
            boolean master = com.hexcraft.config.HexConfig.ENABLE_AM_CONTENT.get();
            boolean sub = switch (contentType) {
                case "hex" -> com.hexcraft.config.HexConfig.ENABLE_AM_HEXTECH.get();
                case "forge" -> com.hexcraft.config.HexConfig.ENABLE_AM_FORGE.get();
                case "equip" -> com.hexcraft.config.HexConfig.ENABLE_AM_EQUIP.get();
                default -> true;
            };
            return master && sub;
        }
        // v0.8.4 第二批联动来源（8 组已装 mod，每组独立开关：总开关 + 海克斯 + 装备；未装 mod 时 isLoaded 已过滤）
        Boolean moreMaster = switch (modid) {
            case "cataclysm" -> com.hexcraft.config.HexConfig.ENABLE_CATACLYSM_CONTENT.get();
            case "goety" -> com.hexcraft.config.HexConfig.ENABLE_GOETY_CONTENT.get();
            case "mowziesmobs" -> com.hexcraft.config.HexConfig.ENABLE_MOWZIESMOBS_CONTENT.get();
            case "artifacts" -> com.hexcraft.config.HexConfig.ENABLE_ARTIFACTS_CONTENT.get();
            case "relics" -> com.hexcraft.config.HexConfig.ENABLE_RELICS_CONTENT.get();
            case "slashblade" -> com.hexcraft.config.HexConfig.ENABLE_SLASHBLADE_CONTENT.get();
            case "farmersdelight" -> com.hexcraft.config.HexConfig.ENABLE_FARMERSDELIGHT_CONTENT.get();
            case "ftbquests" -> com.hexcraft.config.HexConfig.ENABLE_FTBQUESTS_CONTENT.get();
            default -> null;
        };
        if (moreMaster != null) {
            Boolean moreHex = switch (modid) {
                case "cataclysm" -> com.hexcraft.config.HexConfig.ENABLE_CATACLYSM_HEXTECH.get();
                case "goety" -> com.hexcraft.config.HexConfig.ENABLE_GOETY_HEXTECH.get();
                case "mowziesmobs" -> com.hexcraft.config.HexConfig.ENABLE_MOWZIESMOBS_HEXTECH.get();
                case "artifacts" -> com.hexcraft.config.HexConfig.ENABLE_ARTIFACTS_HEXTECH.get();
                case "relics" -> com.hexcraft.config.HexConfig.ENABLE_RELICS_HEXTECH.get();
                case "slashblade" -> com.hexcraft.config.HexConfig.ENABLE_SLASHBLADE_HEXTECH.get();
                case "farmersdelight" -> com.hexcraft.config.HexConfig.ENABLE_FARMERSDELIGHT_HEXTECH.get();
                default -> com.hexcraft.config.HexConfig.ENABLE_FTBQUESTS_HEXTECH.get();
            };
            Boolean moreEquip = switch (modid) {
                case "cataclysm" -> com.hexcraft.config.HexConfig.ENABLE_CATACLYSM_EQUIP.get();
                case "goety" -> com.hexcraft.config.HexConfig.ENABLE_GOETY_EQUIP.get();
                case "mowziesmobs" -> com.hexcraft.config.HexConfig.ENABLE_MOWZIESMOBS_EQUIP.get();
                case "artifacts" -> com.hexcraft.config.HexConfig.ENABLE_ARTIFACTS_EQUIP.get();
                case "relics" -> com.hexcraft.config.HexConfig.ENABLE_RELICS_EQUIP.get();
                case "slashblade" -> com.hexcraft.config.HexConfig.ENABLE_SLASHBLADE_EQUIP.get();
                case "farmersdelight" -> com.hexcraft.config.HexConfig.ENABLE_FARMERSDELIGHT_EQUIP.get();
                default -> com.hexcraft.config.HexConfig.ENABLE_FTBQUESTS_EQUIP.get();
            };
            boolean sub = switch (contentType) {
                case "hex" -> moreHex;
                case "equip" -> moreEquip;
                default -> true; // 第二批联动无锻造词条
            };
            return moreMaster && sub;
        }
        // v0.8.3 ProjectE 等价交换联动来源（EMC A+C）
        if ("projecte".equals(modid)) {
            boolean master = com.hexcraft.config.HexConfig.ENABLE_EMC_CONTENT.get();
            boolean sub = switch (contentType) {
                case "hex" -> com.hexcraft.config.HexConfig.ENABLE_EMC_HEXTECH.get();
                default -> true;
            };
            return master && sub;
        }
        return true; // 未来联动来源默认启用（按需扩展配置）
    }

    /** 定义在当前环境是否可用（来源启用判定；contentType: hex / equip） */
    public static boolean isEnabled(HextechDefinition def, String contentType) {
        return def != null && isSourceEnabled(def.source, contentType);
    }

    // ================= 装备 =================
    public static HextechDefinition getEquip(String id) { return EQUIPMENTS.get(id); }
    public static Collection<HextechDefinition> allEquips() { return EQUIPMENTS.values(); }
    public static void putEquip(String id, HextechDefinition def) { EQUIPMENTS.put(id, def); }

    public static List<String> equipPool(HextechRarity rarity) {
        return EQUIPMENTS.values().stream()
                .filter(d -> d.rarity == rarity && isEnabled(d, "equip"))
                .sorted(Comparator.comparingInt(d -> -d.weight))
                .map(d -> d.id)
                .collect(java.util.stream.Collectors.toList());
    }

    /** 装备三选一：pool 全池（2500）或棱彩池（4000）随机 3 件（不重复、不去重已有；来源未启用项已由 equipPool 过滤） */
    public static List<String> rollEquipOptions(HextechRarity pool) {
        List<String> poolIds = new ArrayList<>(equipPool(pool));
        Collections.shuffle(poolIds);
        return poolIds.subList(0, Math.min(3, poolIds.size()));
    }

    public static List<String> pool(HextechRarity rarity) {
        return DEFINITIONS.values().stream()
                .filter(d -> d.rarity == rarity && isEnabled(d, "hex"))
                .sorted(Comparator.comparingInt(d -> -d.weight))
                .map(d -> d.id)
                .collect(java.util.stream.Collectors.toList());
    }

    // ================= 锻造器词条（v0.6.0 重构：仅静态属性，机制全部海克斯化） =================
    /** v0.6.0 单总词条池：全部静态属性（13 项）。三档共用同一池，强度由档位倍率（×1/×2/×3）区分；
     *  高段位词条（reach/step/hppct/aspdpct）仅在低档位压低抽取权重（不为 0，理论可抽出），
     *  黄金/棱彩档位提升其权重。原三档隔离池（白银=纯数值/黄金=数值+机制/棱彩=百分比+强机制）
     *  因机制词条全部迁入海克斯而取消——机制强度改由海克斯品质档位表达。 */

    /** 当前环境可锻造词条池（属性 + 事件型；未安装联动 mod 的对应词条自动过滤） */
    public static List<String> forgeStats() {
        ensureForgeExt();
        List<String> all = new ArrayList<>(FORGE_MODS.keySet());
        for (Map.Entry<String, String[]> e : FORGE_EVENT_MODS.entrySet()) {
            if (forgeEventSourceEnabled(e.getKey())) all.add(e.getKey());
        }
        return all;
    }

    /** v0.6.0 校准版：锻造词条单总池——三档一律返回全量 13 条（无独占词条）；
     *  档位强度由 FORGE_TIER_MULT 倍率（×1/×2/×3）与 forgeWeight 权重共同区分。
     *  v0.6.9 棱彩档额外加入「栏位扩展」特殊词条（slot_boost，非属性，选中解锁 1 个海克斯栏位）。 */
    public static List<String> forgePool(int tier) {
        ensureForgeExt();
        List<String> pool = new ArrayList<>(FORGE_MODS.keySet());
        for (Map.Entry<String, String[]> e : FORGE_EVENT_MODS.entrySet()) {
            if (forgeEventSourceEnabled(e.getKey())) pool.add(e.getKey());
        }
        if (tier == 2) pool.add("slot_boost"); // 棱彩锻造器专属解锁词条
        return pool;
    }

    /** 事件型锻造词条来源是否启用（按对应 mod 的 forge 细分开关；词条本身仅在 mod 已安装时注册，双重守卫） */
    private static boolean forgeEventSourceEnabled(String stat) {
        return switch (stat) {
            case "dragon_bane" -> com.hexcraft.config.HexConfig.ENABLE_IAF_CONTENT.get() && com.hexcraft.config.HexConfig.ENABLE_IAF_FORGE.get();
            case "radiation_ward", "deep_breath" -> com.hexcraft.config.HexConfig.ENABLE_AC_CONTENT.get() && com.hexcraft.config.HexConfig.ENABLE_AC_FORGE.get();
            case "boss_bane" -> com.hexcraft.config.HexConfig.ENABLE_TF_CONTENT.get() && com.hexcraft.config.HexConfig.ENABLE_TF_FORGE.get();
            case "beast_bane" -> com.hexcraft.config.HexConfig.ENABLE_AM_CONTENT.get() && com.hexcraft.config.HexConfig.ENABLE_AM_FORGE.get();
            default -> true;
        };
    }

    /** v0.6.0 校准版：词条抽取权重（按档位；白银档压低高段位词条权重，但全部 >0 理论可出）
     *  v0.6.9 slot_boost 仅棱彩档可出（权重 3，低概率稀有解锁）
     *  v0.6.20 法术词条（spell_power/max_mana/mana_regen/spell_cd/cast_haste）：白银档压低，黄金/棱彩提权
     *  v0.6.21 三联动事件词条（dragon_bane/radiation_ward/deep_breath/boss_bane）：同法术词条档位权重策略 */
    public static int forgeWeight(int tier, String stat) {
        if ("slot_boost".equals(stat)) return tier == 2 ? 3 : 0; // 棱彩专属
        boolean event = "dragon_bane".equals(stat) || "radiation_ward".equals(stat) || "deep_breath".equals(stat)
                || "boss_bane".equals(stat) || "beast_bane".equals(stat);
        boolean spell = stat.startsWith("spell_") || stat.equals("max_mana") || stat.equals("mana_regen") || stat.equals("cast_haste");
        boolean hi = "reach".equals(stat) || "step".equals(stat);
        boolean pct = "hppct".equals(stat) || "aspdpct".equals(stat);
        if (!hi && !pct && !spell && !event) return 10;           // 基础 9 条：三档同权
        if (tier <= 0) return spell || event ? 2 : 1;              // 白银档：法术/事件/高段位词条权重压低（>0 可出）
        if (tier == 1) return spell || event ? 8 : 6;              // 黄金档：法术/事件词条提权
        return spell || event ? 10 : (hi ? 8 : 10);                // 棱彩档：法术/事件词条高位权重
    }

    /** 全量锻造词条（属性 + 事件型；供界面/总表统计展示，不用于抽取） */
    public static List<String> forgeStatsAll() {
        ensureForgeExt();
        List<String> all = new ArrayList<>(FORGE_MODS.keySet());
        all.addAll(FORGE_EVENT_MODS.keySet());
        return all;
    }

    /** 词条显示规格：属性词条返回注册 ModSpec；事件型词条返回仅含 perLevel 的展示规格（attr 为 null，
     *  属性应用层不消费；实际效果由 HextechEvents.effect() 按事件键汇总）。 */
    public static HextechDefinition.ModSpec forgeMod(String stat) {
        ensureForgeExt();
        HextechDefinition.ModSpec s = FORGE_MODS.get(stat);
        if (s != null) return s;
        String[] ev = FORGE_EVENT_MODS.get(stat);
        if (ev != null) {
            try {
                return new HextechDefinition.ModSpec(null, Double.parseDouble(ev[1]));
            } catch (Exception ignored) { }
        }
        return null;
    }

    /** 事件型锻造词条 → 事件效果键（属性词条返回 null） */
    public static String forgeEventKey(String stat) {
        ensureForgeExt();
        String[] ev = FORGE_EVENT_MODS.get(stat);
        return ev == null ? null : ev[0];
    }

    /** 随机档位（锻造随机抽取）：按海克斯稀有度权重 60/30/10 */
    public static int randomTier() {
        int wSilver = com.hexcraft.config.HexConfig.WEIGHT_SILVER.get();
        int wGold = com.hexcraft.config.HexConfig.WEIGHT_GOLD.get();
        int wPrism = com.hexcraft.config.HexConfig.WEIGHT_PRISMATIC.get();
        int roll = new Random().nextInt(Math.max(1, wSilver + wGold + wPrism));
        if (roll < wPrism) return 2;
        if (roll < wPrism + wGold) return 1;
        return 0;
    }

    // ================= v0.4.8 赌博海克斯随机抽取（选中即一次性转化）=================
    private static final Random RNG = new Random();

    /** 指定稀有度池内随机 1 个海克斯（可排除指定 id；池空返回 null） */
    public static String randomOf(HextechRarity rarity, String exclude) {
        List<String> p = new ArrayList<>(pool(rarity));
        if (exclude != null) p.remove(exclude);
        if (p.isEmpty()) return null;
        return p.get(RNG.nextInt(p.size()));
    }

    /** 按稀有度权重（60/30/10）随机稀有度后随机 1 个海克斯（可排除指定 id） */
    public static String randomAny(String exclude) {
        HextechRarity r = switch (randomTier()) {
            case 2 -> HextechRarity.PRISMATIC;
            case 1 -> HextechRarity.GOLD;
            default -> HextechRarity.SILVER;
        };
        String pick = randomOf(r, exclude);
        if (pick != null) return pick;
        // 该档池空则降档补位
        for (HextechRarity fallback : new HextechRarity[]{HextechRarity.SILVER, HextechRarity.GOLD, HextechRarity.PRISMATIC}) {
            if (fallback == r) continue;
            String alt = randomOf(fallback, exclude);
            if (alt != null) return alt;
        }
        return null;
    }
}
