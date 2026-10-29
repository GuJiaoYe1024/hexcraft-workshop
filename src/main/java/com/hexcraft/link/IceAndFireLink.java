package com.hexcraft.link;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.github.alexthe666.iceandfire.misc.IafDamageRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
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
 * v0.6.21 冰与火之歌（Ice and Fire CE）联动监听层。
 * 只在安装 iceandfire 时由 Hexcraft 主类初始化（ModList.isLoaded 守卫）；未安装时本类不会被加载。
 *
 * 消费通道（HextechEvents.effect(data, key)，× IAF_EFFECT_MULT）：
 *  iaf_dragon_damage         对龙族（三龙）增伤
 *  iaf_fire/ice/lightning_dragon_damage  分系龙增伤（并入龙族判定，分键支持）
 *  iaf_dragon_resist         受龙息（火/冰/雷）伤害减免
 *  iaf_hydra_damage          对海德拉增伤
 *  iaf_sea_serpent_damage    对海蛇增伤
 *  iaf_cyclops_damage        对独眼巨人增伤
 *  iaf_dread_damage          对亡灵（Dread 系列）增伤
 *  iaf_gorgon_damage         对蛇发女妖增伤
 *  iaf_gorgon_resist         抗石化（蛇发女妖石化伤害减免）
 *  iaf_myrmex_damage         对蚁族增伤
 *  iaf_amphithere_damage     对翼蛇增伤
 *  iaf_ghost_damage          对鬼魂增伤
 *  iaf_dragon_kill_heal      击杀龙族回血（每级点数）
 *  iaf_dragon_kill_buff      击杀龙族后 30 秒全伤害增幅
 */
public class IceAndFireLink {

    private static boolean registered = false;

    /** 龙族 EntityType 集（三龙） */
    private static final Set<EntityType<?>> DRAGONS = new HashSet<>();
    private static final Set<EntityType<?>> HYDRA = new HashSet<>();
    private static final Set<EntityType<?>> SEA_SERPENT = new HashSet<>();
    private static final Set<EntityType<?>> CYCLOPS = new HashSet<>();
    private static final Set<EntityType<?>> GORGON = new HashSet<>();
    private static final Set<EntityType<?>> MYRMEX = new HashSet<>();
    private static final Set<EntityType<?>> AMPHITHERE = new HashSet<>();
    private static final Set<EntityType<?>> GHOST = new HashSet<>();
    private static final Set<EntityType<?>> DREAD = new HashSet<>();

    /** 击杀龙族后的爆发增伤 buff（UUID → 过期 tick） */
    private static final Map<UUID, Long> DRAGON_KILL_BUFF = new ConcurrentHashMap<>();

    /** 主类守卫调用：仅 isLoaded("iceandfire") 为真时执行 */
    public static void init() {
        if (registered) return;
        registered = true;
        try {
            // 实体常量（compileOnly 引用；类加载失败时整块跳过，联动自动失效）
            DRAGONS.add(IafEntityRegistry.FIRE_DRAGON.get());
            DRAGONS.add(IafEntityRegistry.ICE_DRAGON.get());
            DRAGONS.add(IafEntityRegistry.LIGHTNING_DRAGON.get());
            HYDRA.add(IafEntityRegistry.HYDRA.get());
            SEA_SERPENT.add(IafEntityRegistry.SEA_SERPENT.get());
            CYCLOPS.add(IafEntityRegistry.CYCLOPS.get());
            GORGON.add(IafEntityRegistry.GORGON.get());
            AMPHITHERE.add(IafEntityRegistry.AMPHITHERE.get());
            GHOST.add(IafEntityRegistry.GHOST.get());
            GHOST.add(IafEntityRegistry.GHOST_SWORD.get());
            MYRMEX.add(IafEntityRegistry.MYRMEX_QUEEN.get());
            MYRMEX.add(IafEntityRegistry.MYRMEX_ROYAL.get());
            MYRMEX.add(IafEntityRegistry.MYRMEX_SOLDIER.get());
            MYRMEX.add(IafEntityRegistry.MYRMEX_WORKER.get());
            MYRMEX.add(IafEntityRegistry.MYRMEX_SENTINEL.get());
            MYRMEX.add(IafEntityRegistry.MYRMEX_SWARMER.get());
            DREAD.add(IafEntityRegistry.DREAD_THRALL.get());
            DREAD.add(IafEntityRegistry.DREAD_GHOUL.get());
            DREAD.add(IafEntityRegistry.DREAD_BEAST.get());
            DREAD.add(IafEntityRegistry.DREAD_SCUTTLER.get());
            DREAD.add(IafEntityRegistry.DREAD_LICH.get());
            DREAD.add(IafEntityRegistry.DREAD_KNIGHT.get());
            DREAD.add(IafEntityRegistry.DREAD_HORSE.get());
        } catch (Throwable t) {
            registered = false;
            return;
        }
        MinecraftForge.EVENT_BUS.register(IceAndFireLink.class);
        org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
        LOGGER.info("Hexcraft: 冰与火之歌（Ice and Fire）联动已挂载（20 海克斯 / 龙族·各族克制 / 锻造词条）");
    }

    // ================= 工具 =================

    /** 玩家某冰与火效果总值（海克斯+装备+锻造词条 event，× IAF_EFFECT_MULT） */
    private static double eff(Player p, String key) {
        PlayerData data = ModCapabilities.get(p);
        return HextechEvents.effect(data, key) * HexConfig.IAF_EFFECT_MULT.get();
    }

    /** 目标分类：返回目标命中的族（dragon/hydra/sea/cyclops/gorgon/myrmex/amphithere/ghost/dread），不匹配返回空 */
    private static String familyOf(LivingEntity target) {
        EntityType<?> t = target.getType();
        if (DRAGONS.contains(t)) return "dragon";
        // 龙族本体（部件 multipart 不是 LivingEntity，不会触发 LivingEvent，无需单独判定）
        if (target instanceof EntityDragonBase) return "dragon";
        if (HYDRA.contains(t)) return "hydra";
        if (SEA_SERPENT.contains(t)) return "sea";
        if (CYCLOPS.contains(t)) return "cyclops";
        if (GORGON.contains(t)) return "gorgon";
        if (MYRMEX.contains(t)) return "myrmex";
        if (AMPHITHERE.contains(t)) return "amphithere";
        if (GHOST.contains(t)) return "ghost";
        if (DREAD.contains(t)) return "dread";
        return "";
    }

    /** 是否为 IAF 龙族本体（击杀回血/buff 判定） */
    private static boolean isDragon(LivingEntity target) {
        return target instanceof EntityDragonBase || DRAGONS.contains(target.getType());
    }

    // ================= 玩家造成伤害：各族增伤 =================

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity().isRemoved()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        LivingEntity target = event.getEntity();
        String fam = familyOf(target);
        if (fam.isEmpty()) return;
        double mul = 0;
        switch (fam) {
            case "dragon" -> {
                mul += eff(sp, "iaf_dragon_damage");
                // 分系：火/冰/雷龙（部件时无法细分，只算总键）
                if (target.getType().equals(IafEntityRegistry.FIRE_DRAGON.get())) mul += eff(sp, "iaf_fire_dragon_damage");
                else if (target.getType().equals(IafEntityRegistry.ICE_DRAGON.get())) mul += eff(sp, "iaf_ice_dragon_damage");
                else if (target.getType().equals(IafEntityRegistry.LIGHTNING_DRAGON.get())) mul += eff(sp, "iaf_lightning_dragon_damage");
            }
            case "hydra" -> mul += eff(sp, "iaf_hydra_damage");
            case "sea" -> mul += eff(sp, "iaf_sea_serpent_damage");
            case "cyclops" -> mul += eff(sp, "iaf_cyclops_damage");
            case "gorgon" -> mul += eff(sp, "iaf_gorgon_damage");
            case "myrmex" -> mul += eff(sp, "iaf_myrmex_damage");
            case "amphithere" -> mul += eff(sp, "iaf_amphithere_damage");
            case "ghost" -> mul += eff(sp, "iaf_ghost_damage");
            case "dread" -> mul += eff(sp, "iaf_dread_damage");
            default -> { }
        }
        // 击杀龙族爆发 buff（dragon_kill_buff）：30 秒内全伤害增幅
        Long expire = DRAGON_KILL_BUFF.get(sp.getUUID());
        if (expire != null && expire > sp.level().getGameTime()) {
            mul += eff(sp, "iaf_dragon_kill_buff");
        }
        if (mul > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 + Math.min(5.0, mul)));
        }
    }

    // ================= 玩家受到伤害：龙息减免 / 石化减免 =================

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer vp)) return;
        DamageSource src = event.getSource();
        double resist = 0;
        // 龙息（火/冰/雷三系伤害类型）
        if (src.is(IafDamageRegistry.DRAGON_FIRE_TYPE) || src.is(IafDamageRegistry.DRAGON_ICE_TYPE)
                || src.is(IafDamageRegistry.DRAGON_LIGHTNING_TYPE)) {
            resist += eff(vp, "iaf_dragon_resist");
        }
        // 石化（蛇发女妖凝视）
        if (src.is(IafDamageRegistry.GORGON_DMG_TYPE)) {
            resist += eff(vp, "iaf_gorgon_resist");
        }
        if (resist > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 - Math.min(0.9, resist)));
        }
    }

    // ================= 击杀：回血 / 爆发 buff =================

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        LivingEntity dead = event.getEntity();
        if (!isDragon(dead)) return;
        double heal = eff(sp, "iaf_dragon_kill_heal");
        if (heal > 0 && sp.isAlive()) sp.heal((float) heal);
        if (eff(sp, "iaf_dragon_kill_buff") > 0) {
            DRAGON_KILL_BUFF.put(sp.getUUID(), sp.level().getGameTime() + 600L); // 30 秒
            if (DRAGON_KILL_BUFF.size() > 256) {
                long now = sp.level().getGameTime();
                DRAGON_KILL_BUFF.entrySet().removeIf(e -> e.getValue() <= now);
            }
        }
    }

    // ================= tick：buff 过期清理 =================

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 40 != 0) return;
        long now = sp.level().getGameTime();
        DRAGON_KILL_BUFF.entrySet().removeIf(e -> e.getValue() <= now);
    }
}
