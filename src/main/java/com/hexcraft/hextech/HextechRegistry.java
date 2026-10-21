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
    /** 锻造机制词条（非属性，走事件效果；参考海克斯大乱斗/斗魂竞技场强化机制重做） */
    public static final List<String> FORGE_MECHANICS = List.of(
            "fire_aspect", "frost_aspect", "poison_aspect", "leech_aspect", "pierce_aspect",
            "thorn_aspect", "regen_aspect", "gold_aspect",
            "crit_aspect", "execute_aspect", "splash_aspect",
            "lightning_aspect", "stack_aspect", "dance_aspect", "bloodlust_aspect", "reaper_aspect");
    public static final int[] FORGE_TIER_MULT = {1, 2, 3};
    public static final String[] FORGE_TIER_KEYS = {"basic", "gold", "prismatic"};

    static {
        // ---- 白银（内置兜底，其余由数据包定义；数据包优先覆盖）----
        register("hex_berserk", HextechRarity.SILVER, 10, List.of(mod(Attributes.ATTACK_DAMAGE, 1.5)), null);
        register("hex_frenzy", HextechRarity.SILVER, 10, List.of(mod(Attributes.ATTACK_SPEED, 0.8), mod(Attributes.MOVEMENT_SPEED, 0.02)), null);
        register("hex_thunderlord", HextechRarity.SILVER, 10, List.of(mod(Attributes.ATTACK_SPEED, 0.2)), "crit:0.25");

        // ---- 黄金（内置兜底）----
        register("hex_vampire", HextechRarity.GOLD, 6, List.of(), "lifesteal:0.1,kill_heal:2.0");
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

        // ---- 锻造属性池（倍率 1/2/3 对应基础/黄金/棱彩锻造器；无上限锻造 → 数值全部下调防崩坏）----
        FORGE_MODS.put("atk", mod(Attributes.ATTACK_DAMAGE, 0.25));
        FORGE_MODS.put("armor", mod(Attributes.ARMOR, 0.5));
        FORGE_MODS.put("hp", mod(Attributes.MAX_HEALTH, 1.0));
        FORGE_MODS.put("spd", mod(Attributes.MOVEMENT_SPEED, 0.0015));
        FORGE_MODS.put("aspd", mod(Attributes.ATTACK_SPEED, 0.025));
        FORGE_MODS.put("luck", mod(Attributes.LUCK, 0.25));
        FORGE_MODS.put("knockback", mod(Attributes.ATTACK_KNOCKBACK, 0.12));
        FORGE_MODS.put("resist", mod(Attributes.KNOCKBACK_RESISTANCE, 0.025));
        FORGE_MODS.put("tough", mod(Attributes.ARMOR_TOUGHNESS, 0.25));
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

    // ================= 锻造器词条（来源 × 小类：原版词条恒有；TACZ 联动词条按开关/检测启用） =================
    /** TACZ 联动锻造机制词条（棱彩向强词条；无上限锻造 → 每级数值克制） */
    public static final List<String> FORGE_TACZ_MECHANICS = List.of(
            "gun_aspect", "gun_crit_aspect", "ammo_aspect");

    /** 当前环境可锻造词条池（原版 + 已启用的联动词条） */
    public static List<String> forgeStats() {
        ensureForgeExt();
        List<String> out = new ArrayList<>(FORGE_MODS.keySet());
        out.addAll(FORGE_MECHANICS);
        if (isSourceEnabled("tacz", "forge")) out.addAll(FORGE_TACZ_MECHANICS);
        return out;
    }

    /** 全量锻造词条（含未启用联动；供界面/总表统计展示，不用于抽取） */
    public static List<String> forgeStatsAll() {
        ensureForgeExt();
        List<String> out = new ArrayList<>(FORGE_MODS.keySet());
        out.addAll(FORGE_MECHANICS);
        out.addAll(FORGE_TACZ_MECHANICS);
        return out;
    }

    /** 随机一个锻造词条（属性或机制，仅当前可用池），用于「掷骰狂人」免费锻造 */
    public static String randomForgeStat() {
        List<String> pool = forgeStats();
        return pool.get(new Random().nextInt(pool.size()));
    }

    public static HextechDefinition.ModSpec forgeMod(String stat) {
        ensureForgeExt();
        return FORGE_MODS.get(stat);
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
}
