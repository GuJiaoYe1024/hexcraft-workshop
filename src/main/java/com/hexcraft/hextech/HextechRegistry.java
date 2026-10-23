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

    // ================= 锻造器词条（v0.6.0 重构：仅静态属性，机制全部海克斯化） =================
    /** v0.6.0 单总词条池：全部静态属性（13 项）。三档共用同一池，强度由档位倍率（×1/×2/×3）区分；
     *  高段位词条（reach/step/hppct/aspdpct）仅在低档位压低抽取权重（不为 0，理论可抽出），
     *  黄金/棱彩档位提升其权重。原三档隔离池（白银=纯数值/黄金=数值+机制/棱彩=百分比+强机制）
     *  因机制词条全部迁入海克斯而取消——机制强度改由海克斯品质档位表达。 */

    /** 当前环境可锻造词条池（仅静态属性；v0.6.0 机制词条全部迁出） */
    public static List<String> forgeStats() {
        ensureForgeExt();
        return new ArrayList<>(FORGE_MODS.keySet());
    }

    /** v0.6.0 校准版：锻造词条单总池——三档一律返回全量 13 条（无独占词条）；
     *  档位强度由 FORGE_TIER_MULT 倍率（×1/×2/×3）与 forgeWeight 权重共同区分。 */
    public static List<String> forgePool(int tier) {
        ensureForgeExt();
        return new ArrayList<>(FORGE_MODS.keySet());
    }

    /** v0.6.0 校准版：词条抽取权重（按档位；白银档压低高段位词条权重，但全部 >0 理论可出） */
    public static int forgeWeight(int tier, String stat) {
        boolean hi = "reach".equals(stat) || "step".equals(stat);
        boolean pct = "hppct".equals(stat) || "aspdpct".equals(stat);
        if (!hi && !pct) return 10;                     // 基础 9 条：三档同权
        if (tier <= 0) return hi ? 1 : 1;               // 白银档：reach/step/hppct/aspdpct 权重压低（>0 可出）
        if (tier == 1) return hi ? 6 : 3;               // 黄金档：机制型提权，百分比型次之
        return hi ? 8 : 10;                             // 棱彩档：全部高位权重（百分比词条更契合棱彩）
    }

    /** 全量锻造词条（仅静态属性；供界面/总表统计展示，不用于抽取） */
    public static List<String> forgeStatsAll() {
        ensureForgeExt();
        return new ArrayList<>(FORGE_MODS.keySet());
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
