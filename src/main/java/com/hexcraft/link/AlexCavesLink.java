package com.hexcraft.link;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * v0.6.21 Alex's Caves 联动监听层。
 * 只在安装 alexscaves 时由 Hexcraft 主类初始化；判定全部走 EntityType 注册键 / 伤害源 msgId /
 * 生物群系 ResourceKey（不引用 AC 类，零编译依赖、零类加载风险）。
 *
 * 消费通道（HextechEvents.effect(data, key)，× AC_EFFECT_MULT）：
 *  ac_radiation_damage      对辐射区生物增伤
 *  ac_toxic_damage          对毒性区生物增伤
 *  ac_dark_damage           对遗忘（黑暗）区生物增伤
 *  ac_deep_damage           对深渊/深海生物增伤
 *  ac_prehistoric_damage    对史前（恐龙）生物增伤
 *  ac_candy_damage          对糖果区生物增伤
 *  ac_magnetic_damage       对磁力区生物增伤
 *  ac_tremorzilla_damage    对巨兽（Tremorzilla/Tremorsaurus）增伤
 *  ac_brainiac_damage       对智核体增伤
 *  ac_radiation_resist      受辐射伤害减免
 *  ac_toxic_resist          受酸性/毒性伤害减免
 *  ac_magnetic_resist       受磁力/闪电伤害减免
 *  ac_deep_resist           受深渊生物伤害减免
 *  ac_deep_breath           深渊群系水下呼吸（≥1 生效）
 *  ac_dark_vision           遗忘群系夜视（≥1 生效）
 *  ac_candy_heal            糖果群系持续回血（每级回复点/5s）
 *  ac_prehistoric_kill_heal 击杀史前生物回血
 */
public class AlexCavesLink {

    private static boolean registered = false;

    /** AC 生物 EntityType 注册键（path 判定，namespace 在事件内校验） */
    private static final Set<String> PREHISTORIC = Set.of(
            "tremorzilla", "tremorsaurus", "luxtructosaurus", "relicheirus", "atlatitan",
            "subterranodon", "vallumraptor", "grottoceratops", "trilocaris", "dinosaur_spirit");
    private static final Set<String> MAGNETIC = Set.of(
            "magnetron", "boundroid", "boundroid_winch", "ferrouslime", "teletor", "notor");
    private static final Set<String> RADIATION = Set.of(
            "nucleeper", "radgill", "gammaroach", "brainiac", "raycat");
    private static final Set<String> TOXIC = Set.of(
            "corrodent", "underzealot", "watcher", "forsaken", "gossamer_worm", "gossamer_worm_part");
    private static final Set<String> DARK = Set.of(
            "vesper", "watcher", "forsaken", "gloomoth", "corrodent");
    private static final Set<String> DEEP = Set.of(
            "deep_one", "deep_one_knight", "deep_one_mage", "hullbreaker", "lanternfish",
            "tripodfish", "sea_pig", "mine_guardian");
    private static final Set<String> CANDY = Set.of(
            "candicorn", "caniac", "caramel_cube", "gummy_bear", "gum_worm", "gum_worm_segment",
            "gingerbread_man", "gumbeeper", "sweetish_fish", "licowitch");
    private static final Set<String> MEGA = Set.of("tremorzilla", "tremorsaurus");
    private static final Set<String> BRAINIAC = Set.of("brainiac");

    /** AC 六洞穴群系 ResourceKey location path（环境判定：呼吸/夜视/回血） */
    private static final String BIOME_ABYSSAL = "abyssal_chasm";
    private static final String BIOME_FORLORN = "forlorn_hollows";
    private static final String BIOME_CANDY = "candy_cavity";

    /** 击杀史前生物回血记录（无需缓存，直接事件内处理） */
    private static final Map<UUID, Long> KILL_HEAL_CD = new ConcurrentHashMap<>();

    /** 主类守卫调用：仅 isLoaded("alexscaves") 为真时执行 */
    public static void init() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(AlexCavesLink.class);
        org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
        LOGGER.info("Hexcraft: Alex's Caves 联动已挂载（20 海克斯 / 辐射·毒性·深渊·磁力·糖果·遗忘六系克制 / 锻造词条）");
    }

    // ================= 工具 =================

    private static double eff(Player p, String key) {
        PlayerData data = ModCapabilities.get(p);
        return HextechEvents.effect(data, key) * HexConfig.AC_EFFECT_MULT.get();
    }

    /** AC 生物分组判定（namespace=alexscaves 且 path 命中；返回组名，空串=非 AC 生物） */
    private static String groupOf(LivingEntity target) {
        EntityType<?> t = target.getType();
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(t);
        if (key == null || !"alexscaves".equals(key.getNamespace())) return "";
        String path = key.getPath();
        if (MEGA.contains(path)) return "mega";
        if (BRAINIAC.contains(path)) return "brainiac";
        if (PREHISTORIC.contains(path)) return "prehistoric";
        if (MAGNETIC.contains(path)) return "magnetic";
        if (RADIATION.contains(path)) return "radiation";
        if (TOXIC.contains(path)) return "toxic";
        if (DARK.contains(path)) return "dark";
        if (DEEP.contains(path)) return "deep";
        if (CANDY.contains(path)) return "candy";
        return "";
    }

    /** 玩家当前生物群系 location path（保护空引用） */
    private static String biomePath(Player p) {
        try {
            Holder<Biome> h = p.level().getBiome(p.blockPosition());
            ResourceKey<Biome> k = h.unwrapKey().orElse(null);
            return k == null ? "" : k.location().getPath();
        } catch (Throwable ignored) {
            return "";
        }
    }

    /** 玩家是否在水下（深渊呼吸生效条件之一） */
    private static boolean underwater(Player p) {
        return p.isUnderWater() || p.isInWater();
    }

    // ================= 玩家造成伤害：六系克制增伤 =================

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity().isRemoved()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        String group = groupOf(event.getEntity());
        if (group.isEmpty()) return;
        double mul = switch (group) {
            case "prehistoric", "mega" -> eff(sp, "ac_prehistoric_damage") + eff(sp, "ac_tremorzilla_damage");
            case "magnetic" -> eff(sp, "ac_magnetic_damage");
            case "radiation" -> eff(sp, "ac_radiation_damage");
            case "toxic" -> eff(sp, "ac_toxic_damage");
            case "dark" -> eff(sp, "ac_dark_damage");
            case "deep" -> eff(sp, "ac_deep_damage");
            case "candy" -> eff(sp, "ac_candy_damage");
            case "brainiac" -> eff(sp, "ac_brainiac_damage") + eff(sp, "ac_radiation_damage");
            default -> 0;
        };
        if (mul > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 + Math.min(5.0, mul)));
        }
    }

    // ================= 玩家受到伤害：辐射/酸毒/磁力减免 =================

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer vp)) return;
        DamageSource src = event.getSource();
        String msg = src.getMsgId();
        double resist = 0;
        // 辐射：AC 辐射伤害源 msgId 含 radiation
        if (msg.contains("radiation")) resist += eff(vp, "ac_radiation_resist");
        // 酸/毒：msgId 含 acid / toxic
        if (msg.contains("acid") || msg.contains("toxic")) resist += eff(vp, "ac_toxic_resist");
        // 磁力/闪电：磁力洞穴磁暴与落雷
        if (msg.contains("magnetic") || msg.contains("lightning")) resist += eff(vp, "ac_magnetic_resist");
        // 深渊生物伤害（非环境类，按 msgId 无明确标记 → 按来源生物判定）
        if (resist == 0) {
            LivingEntity srcEnt = src.getEntity() instanceof LivingEntity le ? le : null;
            if (srcEnt != null && "deep".equals(groupOf(srcEnt))) resist += eff(vp, "ac_deep_resist");
        }
        if (resist > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 - Math.min(0.9, resist)));
        }
    }

    // ================= 呼吸：深渊群系水下呼吸 =================

    @SubscribeEvent
    public static void onBreathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!underwater(sp)) return;
        if (eff(sp, "ac_deep_breath") > 0 && BIOME_ABYSSAL.equals(biomePath(sp))) {
            event.setCanBreathe(true);
        }
    }

    // ================= 击杀：史前生物回血 =================

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        String group = groupOf(event.getEntity());
        if (!"prehistoric".equals(group) && !"mega".equals(group)) return;
        double heal = eff(sp, "ac_prehistoric_kill_heal");
        if (heal > 0 && sp.isAlive()) sp.heal((float) heal);
    }

    // ================= tick：夜视 / 糖果回血 =================

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        String biome = biomePath(sp);
        // 夜视（遗忘群系）：每 60 tick 刷新一次，防止效果过期闪烁
        if (BIOME_FORLORN.equals(biome) && eff(sp, "ac_dark_vision") > 0 && sp.tickCount % 60 == 0) {
            sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.NIGHT_VISION, 320, 0, false, false));
        }
        // 糖果回血（糖果群系）：每 200 tick 回血
        if (BIOME_CANDY.equals(biome) && sp.tickCount % 200 == 0) {
            double heal = eff(sp, "ac_candy_heal");
            if (heal > 0 && sp.isAlive()) sp.heal((float) heal);
        }
    }
}
