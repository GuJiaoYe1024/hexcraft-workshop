package com.hexcraft.link;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.event.HextechEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.8.3 第二批 mod 联动（8 组）：末地灾变 / 巫术 / Mowzie's Mobs / 神器 / 遗物 / 拔刀剑 / 农夫乐事 / FTB Quests。
 * 与 AlexCavesLink 同模式：仅对应 mod 安装时由 Hexcraft 主类挂载；判定全部走 EntityType 注册键 namespace /
 * 物品注册键 namespace / 主手判定（零编译期依赖，未安装零副作用）。
 *
 * 消费通道（HextechEvents.effect(data, key)，值 = 每级数值）：
 *  cataclysm_damage / cataclysm_resist / cataclysm_drop   末地灾变
 *  goety_damage / goety_soul / goety_drop                 巫术
 *  mowzie_damage / mowzie_resist / mowzie_drop            Mowzie's Mobs
 *  artifact_rage / artifact_vamp / artifact_drop          神器（curios/背包持有时生效）
 *  relic_power / relic_ward / relic_drop                  遗物（curios/背包持有时生效）
 *  blade_damage / blade_burst / blade_drop                拔刀剑（主手持有时生效）
 *  farmer_drop / farmer_chef                              农夫乐事（击杀掉食物 / 进食回血）
 *  quest 系列（ftb_quest 前缀）复用 goldmine/scholar/random_loot 现有键，见 JSON。
 */
public final class MoreLink {

    private static boolean registered = false;

    // ================= 实体注册键 path 集合（namespace 在事件内校验 = modid） =================

    /** 末地灾变（cataclysm）：主力战斗生物（排除投射物/环境体） */
    private static final Set<String> CATACLYSM = Set.of(
            "ignis", "ancient_remnant", "ancient_ancient_remnant", "leviathan", "aptrgangr",
            "coralssus", "ignited_berserker", "ignited_revenant", "deepling", "deepling_brute",
            "deepling_angler", "deepling_priest", "deepling_warlock", "draugr", "elite_draugr",
            "cindaria", "clawdian", "koboleton", "kobolediator", "coral_golem", "ender_golem",
            "ender_guardian", "endermaptera", "maledictus", "amethyst_crab", "lionfish",
            "hippocamtus", "modern_remnant", "black_steel_golem");
    /** 巫术（goety）：亡灵/仆从/巫术生物（去除投射物、药水云等无实体战斗体） */
    private static final Set<String> GOETY = Set.of(
            "apostle", "black_beast", "black_wolf", "hostile_black_wolf", "bone_lord",
            "necromancer", "cairn_necromancer", "mossy_necromancer", "brood_mother",
            "warlock", "heretic", "maverick", "crone", "frayed", "rattled", "reaper",
            "wraith", "border_wraith", "muck_wraith", "crypt_slime", "web_spider",
            "icy_spider", "bone_spider", "watchling", "blastling", "snareling",
            "endersent", "ender_keeper", "carrion_fly", "carrion_maggot", "conquillager",
            "berserk_fungus", "blast_fungus", "bound_evoker", "bound_geomancer",
            "bound_iceologer", "bound_cryologer", "bound_storm_caster", "bound_wind_caller",
            "crusher", "damned", "doppelganger", "zombie_servant", "zombie_villager_servant",
            "husk_servant", "drowned_servant", "frozen_zombie_servant", "jungle_zombie_servant",
            "blackguard_servant", "skeleton_servant", "stray_servant", "wither_skeleton_servant",
            "mossy_skeleton_servant", "sunken_skeleton_servant", "reaper_servant",
            "wraith_servant", "border_wraith_servant", "muck_wraith_servant",
            "necromancer_servant", "cairn_necromancer_servant", "mossy_necromancer_servant",
            "drowned_necromancer_servant", "wither_necromancer_servant", "vanguard_servant",
            "blaze_servant", "cave_spider_servant", "bone_spider_servant", "blastling_servant",
            "crypt_slime_servant", "bear_servant", "phantom_servant", "haunted_armor",
            "geomancer_servant", "iceologer_servant", "wind_caller_servant", "storm_caster_servant");
    /** Mowzie's Mobs（mowziesmobs） */
    private static final Set<String> MOWZIE = Set.of(
            "ferrous_wroughtnaut", "frostmaw", "grottol", "foliaath", "baby_foliaath",
            "naga", "umvuthana", "umvuthi", "sculptor", "lantern", "umvuthana_raptor");
    /** 神器（artifacts）：实体仅 mimic；"持有神器"按物品 namespace 判定 */
    private static final Set<String> ARTIFACT_ITEMS = Set.of(
            "anglers_hat", "antidote_vessel", "aqua_dashers", "bunny_hoppers", "charm_of_sinking",
            "chorus_totem", "cloud_in_a_bottle", "cowboy_hat", "cross_necklace", "crystal_heart",
            "digging_claws", "eternal_steak", "everlasting_beef", "feral_claws", "fire_gauntlet",
            "flame_pendant", "flippers", "golden_hook", "helium_flamingo", "kitty_slippers",
            "lucky_scarf", "night_vision_goggles", "novelty_drinking_hat", "obsidian_skull",
            "onion_ring", "panic_necklace", "pickaxe_heater", "plastic_drinking_hat", "pocket_piston",
            "power_glove", "rooted_boots", "running_shoes", "scarf_of_invisibility", "shock_pendant",
            "snorkel", "snowshoes", "steadfast_spikes", "superstitious_hat", "thorn_pendant",
            "umbrella", "universal_attractor", "vampiric_glove", "villager_hat", "whoopee_cushion");
    /** 遗物（relics）：物品 namespace = relics */
    private static final List<String> RELIC_DROPS = List.of(
            "relic_experience_bottle", "shadow_glaive", "magic_mirror", "rage_glove",
            "holy_locket", "infinity_ham", "chorus_inhibitor", "endless_star");

    /** 掉落表：modid → (物品 path, 权重) */
    private static final Map<String, List<String[]>> DROPS = new java.util.LinkedHashMap<>();
    static {
        DROPS.put("cataclysm", java.util.List.of(
                new String[]{"witherite_ingot", "2"}, new String[]{"ancient_metal_ingot", "2"}));
        DROPS.put("goety", java.util.List.<String[]>of(new String[]{"animation_core", "3"}));
        DROPS.put("mowziesmobs", java.util.List.<String[]>of(new String[]{"naga_fang", "3"}));
        DROPS.put("slashblade", java.util.List.<String[]>of(new String[]{"proud_soul", "3"}));
        DROPS.put("farmersdelight", java.util.List.of(
                new String[]{"bacon", "2"}, new String[]{"cabbage", "1"}));
    }

    /** 拔刀剑连击（blade_burst）：击杀后 5 秒内下一次攻击增伤 */
    private static final Map<UUID, Long> BLADE_BURST = new ConcurrentHashMap<>();
    private static final long BURST_TICKS = 100;

    private MoreLink() { }

    /** 主类守卫调用：对应 mod 安装时才执行（由 Hexcraft 按 isLoaded 逐个判定） */
    public static void init() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(MoreLink.class);
        org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
        LOGGER.info("Hexcraft: 第二批联动已挂载（末地灾变/巫术/Mowzie/神器/遗物/拔刀剑/农夫乐事/FTB Quests）");
    }

    // ================= 工具 =================

    private static double eff(Player p, String key) {
        PlayerData data = ModCapabilities.get(p);
        return data == null ? 0 : HextechEvents.effect(data, key);
    }

    /** 实体所属 mod 分组：命中返回 modid，否则空串 */
    private static String groupOf(LivingEntity target) {
        EntityType<?> t = target.getType();
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(t);
        if (key == null) return "";
        String ns = key.getNamespace();
        String path = key.getPath();
        if ("cataclysm".equals(ns) && CATACLYSM.contains(path)) return "cataclysm";
        if ("goety".equals(ns) && GOETY.contains(path)) return "goety";
        if ("mowziesmobs".equals(ns) && MOWZIE.contains(path)) return "mowziesmobs";
        if ("artifacts".equals(ns) && "mimic".equals(path)) return "artifacts";
        return "";
    }

    /** 玩家是否携带指定 namespace 的物品（curios 槽 + 主副手 + 背包 + 装备） */
    private static boolean carrying(Player p, String ns, Set<String> paths) {
        // curios 背带/饰品槽
        try {
            Class<?> curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            java.lang.reflect.Method getCurios = curiosApi.getMethod("getCuriosInventory", LivingEntity.class);
            Object lazy = getCurios.invoke(null, p);
            if (lazy != null) {
                java.lang.reflect.Method resolve = lazy.getClass().getMethod("resolve");
                Object opt = resolve.invoke(lazy);
                if (opt != null && (Boolean) opt.getClass().getMethod("isPresent").invoke(opt)) {
                    Object handler = opt.getClass().getMethod("get").invoke(opt);
                    Object equipped = handler.getClass().getMethod("getEquippedCurios").invoke(handler);
                    if (equipped instanceof net.minecraftforge.items.IItemHandlerModifiable mod) {
                        for (int s = 0; s < mod.getSlots(); s++) {
                            if (match(mod.getStackInSlot(s), ns, paths)) return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) { }
        // 背包 + 装备 + 副手
        net.minecraft.world.entity.player.Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (match(inv.getItem(i), ns, paths)) return true;
        }
        return false;
    }

    private static boolean match(ItemStack st, String ns, Set<String> paths) {
        if (st == null || st.isEmpty()) return false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(st.getItem());
        return key != null && ns.equals(key.getNamespace()) && (paths == null || paths.isEmpty() || paths.contains(key.getPath()));
    }

    /** 击杀时按掉落表给奖励物品（掉落表按目标 mod 分组） */
    private static void rollDrop(ServerPlayer sp, LivingEntity dead, String modid, double chance) {
        if (chance <= 0) return;
        List<String[]> table = DROPS.get(modid);
        if (table == null) return;
        ServerLevel level = (ServerLevel) sp.level();
        BlockPos pos = dead.blockPosition();
        for (String[] row : table) {
            if (sp.getRandom().nextFloat() < (float) Math.min(1.0, chance)) {
                ResourceLocation itemKey = ResourceLocation.fromNamespaceAndPath(modid, row[0]);
                Item item = ForgeRegistries.ITEMS.getValue(itemKey);
                if (item == null || item == net.minecraft.world.item.Items.AIR) continue;
                int n = row[1].equals("2") && sp.getRandom().nextFloat() < 0.35f ? 2 : 1;
                net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(item, n));
            }
        }
    }

    // ================= 玩家造成伤害 =================

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (event.getEntity().isRemoved()) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double mul = 0;
        String group = groupOf(event.getEntity());
        switch (group) {
            case "cataclysm" -> mul += eff(sp, "cataclysm_damage");
            case "goety" -> mul += eff(sp, "goety_damage");
            case "mowziesmobs" -> mul += eff(sp, "mowzie_damage");
            case "artifacts" -> mul += eff(sp, "artifact_drop") * 0; // mimic 掉落单独在击杀处理
            default -> { }
        }
        // 神器低血增伤（携带神器时，生命 < 50%）
        if (carrying(sp, "artifacts", ARTIFACT_ITEMS)) {
            double rage = eff(sp, "artifact_rage");
            if (rage > 0 && sp.getHealth() < sp.getMaxHealth() * 0.5f) mul += rage;
            mul += eff(sp, "artifact_vamp") * 0; // 吸血在击杀处理
        }
        // 遗物增伤（携带遗物时）
        if (carrying(sp, "relics", null)) {
            mul += eff(sp, "relic_power");
        }
        // 拔刀剑增伤 / 连击（主手持拔刀剑）
        if (isBladeHeld(sp)) {
            mul += eff(sp, "blade_damage");
            Long until = BLADE_BURST.get(sp.getUUID());
            if (until != null && sp.tickCount < until) {
                mul += eff(sp, "blade_burst");
                BLADE_BURST.remove(sp.getUUID());
            }
        }
        if (mul > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 + Math.min(5.0, mul)));
        }
    }

    private static boolean isBladeHeld(ServerPlayer sp) {
        ItemStack main = sp.getMainHandItem();
        if (main.isEmpty()) return false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(main.getItem());
        return key != null && "slashblade".equals(key.getNamespace());
    }

    // ================= 玩家受到伤害 =================

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer vp)) return;
        String group = event.getSource().getEntity() instanceof LivingEntity le ? groupOf(le) : "";
        double resist = 0;
        if ("cataclysm".equals(group)) resist += eff(vp, "cataclysm_resist");
        if ("mowziesmobs".equals(group)) resist += eff(vp, "mowzie_resist");
        // 巫术系生物伤害减免（亡灵/仆从）
        if ("goety".equals(group)) resist += eff(vp, "goety_resist");
        if (carrying(vp, "relics", null)) resist += eff(vp, "relic_ward");
        if (resist > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 - Math.min(0.9, resist)));
        }
    }

    // ================= 击杀：回血 / 连击 / 掉落 =================

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        String group = groupOf(event.getEntity());
        if (group.isEmpty()) return;
        double heal = 0;
        switch (group) {
            case "cataclysm" -> {
                rollDrop(sp, event.getEntity(), "cataclysm", eff(sp, "cataclysm_drop"));
            }
            case "goety" -> {
                double soul = eff(sp, "goety_soul");
                if (soul > 0) {
                    PlayerData data = ModCapabilities.get(sp);
                    if (data != null) data.addCoins((int) Math.floor(20 * soul));
                    if (sp.isAlive()) sp.heal(1.0f * (float) soul);
                }
                rollDrop(sp, event.getEntity(), "goety", eff(sp, "goety_drop"));
            }
            case "mowziesmobs" -> {
                rollDrop(sp, event.getEntity(), "mowziesmobs", eff(sp, "mowzie_drop"));
            }
            case "artifacts" -> {
                rollArtifact(sp, eff(sp, "artifact_drop"));
            }
            default -> { }
        }
        // 神器吸血（携带神器时击杀回血）
        if (carrying(sp, "artifacts", ARTIFACT_ITEMS)) {
            double vamp = eff(sp, "artifact_vamp");
            if (vamp > 0 && sp.isAlive()) sp.heal(2.0f * (float) vamp);
        }
        // 拔刀剑：击杀触发连击窗口
        if (isBladeHeld(sp) && eff(sp, "blade_burst") > 0) {
            BLADE_BURST.put(sp.getUUID(), sp.tickCount + BURST_TICKS);
        }
        // 遗物击杀掉落（任意生物，低概率）
        if (carrying(sp, "relics", null)) {
            rollRelic(sp, eff(sp, "relic_drop"));
        }
        // 农夫乐事：击杀任意生物掉食物
        rollDrop(sp, event.getEntity(), "farmersdelight", eff(sp, "farmer_drop"));
    }

    /** 神器掉落：从神器物品表随机给 1 件（击杀 mimic 专属，仅 artifacts 前缀海克斯） */
    private static void rollArtifact(ServerPlayer sp, double chance) {
        if (chance <= 0) return;
        if (sp.getRandom().nextFloat() >= (float) Math.min(1.0, chance)) return;
        List<String> pool = ARTIFACT_ITEMS.stream().toList();
        String path = pool.get(sp.getRandom().nextInt(pool.size()));
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("artifacts", path));
        if (item != null && item != net.minecraft.world.item.Items.AIR) {
            net.minecraft.world.level.block.Block.popResource(sp.level(), sp.blockPosition(), new ItemStack(item));
        }
    }

    /** 遗物掉落：从遗物表随机给 1 件 */
    private static void rollRelic(ServerPlayer sp, double chance) {
        if (chance <= 0) return;
        if (sp.getRandom().nextFloat() >= (float) Math.min(1.0, chance)) return;
        String path = RELIC_DROPS.get(sp.getRandom().nextInt(RELIC_DROPS.size()));
        Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("relics", path));
        if (item != null && item != net.minecraft.world.item.Items.AIR) {
            net.minecraft.world.level.block.Block.popResource(sp.level(), sp.blockPosition(), new ItemStack(item));
        }
    }

    // ================= 农夫乐事：进食回血 =================

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ItemStack stack = event.getItem();
        if (stack.isEmpty() || !stack.isEdible()) return;
        double chef = eff(sp, "farmer_chef");
        if (chef > 0 && sp.isAlive()) {
            FoodProperties food = stack.getItem().getFoodProperties(stack, sp);
            if (food != null) sp.heal(2.0f * (float) chef);
        }
    }
}
