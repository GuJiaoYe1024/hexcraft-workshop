package com.hexcraft.link;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Set;

/**
 * v0.6.22 Alex's Mobs 联动监听层。
 * 只在安装 alexsmobs 时由 Hexcraft 主类初始化；判定全部走 EntityType 注册键
 * （不引用 AM 类，零编译依赖、零类加载风险）。机制方向：野兽克制流——走兽/毒物/天禽/
 * 水生/巨兽/首领分类增伤 + 受击减免 + 击杀回复/增益，顶部棱彩为万兽之王式全维度压制。
 *
 * 消费通道（HextechEvents.effect(data, key)，× AM_EFFECT_MULT）：
 *  am_mob_damage      对全部 Alex's Mobs 生物增伤
 *  am_mob_resist      受 Alex's Mobs 生物伤害减免
 *  am_beast_damage    对走兽类增伤（虎狮象河马犀牛鹿狼熊等）
 *  am_venom_damage    对毒系生物增伤（蜈蚣/洞螈/科莫多/蟾蜍/白蚁/蚊等）
 *  am_flying_damage   对飞行类增伤（鸦/鹫/鹰/鸵鸟/蜂鸟/蜻蜓等）
 *  am_aquatic_damage  对水生类增伤（鲨/鲸/鱿/海豹/海獭等）
 *  am_titan_damage    对巨兽类增伤（象/犀/河马/鲸/巨鲨）
 *  am_boss_damage     对高生命首领级生物（≥100HP）增伤
 *  am_kill_heal       击杀 Alex's Mobs 生物回血
 *  am_kill_buff       击杀后获得 5s 力量（攻击提升）增益
 *  am_kill_speed      击杀后获得 5s 速度增益
 *  am_mob_master      对 AM 增伤 + 受 AM 减免（双向驯兽）
 *  am_apex            对全部 AM 增伤 + 受 AM 减免（万兽之王）
 *  am_wild_lord       对 AM 增伤 + 击杀回血（荒野之主）
 *  am_predator        对 AM 增伤 + 击杀吸血（顶级掠食者）
 *  am_totem           受 AM 伤害减免（兽灵庇护）
 */
public class AlexMobsLink {

    private static boolean registered = false;

    /** AM 生物 EntityType 注册键 path（namespace 在事件内校验） */
    private static final Set<String> BEAST = Set.of(
            "gorilla", "crocodile", "capuchin_monkey", "tiger", "lion", "hippo", "rhino",
            "elephant", "moose", "warped_toad", "komodo_dragon", "grizzly_bear", "black_bear",
            "polar_bear", "snow_leopard", "maned_wolf", "raccoon", "red_panda", "mungus",
            "platypus", "seal", "sea_bear", "urial", "emu", "gazelle", "bison", "squirrel",
            "skunk", "badger", "tusklin", "panda_bear");
    private static final Set<String> VENOM = Set.of(
            "cave_centipede", "cave_crawler", "komodo_dragon", "warped_toad", "tarantula_hawk",
            "cockroach", "termite", "mosquito", "crimson_mosquito", "fly", "scorpion", "centipede");
    private static final Set<String> FLYING = Set.of(
            "dragonfly", "crow", "vulture", "bald_eagle", "roadrunner", "ostrich", "sunbird",
            "toucan", "hummingbird", "blue_jay", "cardinal", "fly", "crimson_mosquito",
            "tarantula_hawk", "skelewag", "great_hammerhead");
    private static final Set<String> AQUATIC = Set.of(
            "orca", "shark", "hammerhead_shark", "great_whale", "cachalot_whale", "giant_squid",
            "mimic_octopus", "lobster", "seal", "sea_bear", "catfish", "flying_fish",
            "mudskipper", "shoebill", "squid", "deep_sea_creature");
    private static final Set<String> TITAN = Set.of(
            "elephant", "rhino", "hippo", "great_whale", "cachalot_whale", "orca",
            "shark", "hammerhead_shark", "giant_squid");

    /** 主类守卫调用：仅 isLoaded("alexsmobs") 为真时执行 */
    public static void init() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(AlexMobsLink.class);
        org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
        LOGGER.info("Hexcraft: Alex's Mobs 联动已挂载（16 海克斯 / 走兽·毒物·天禽·水生·巨兽·首领六系克制 / 锻造词条）");
    }

    // ================= 工具 =================

    private static double eff(Player p, String key) {
        PlayerData data = ModCapabilities.get(p);
        return HextechEvents.effect(data, key) * HexConfig.AM_EFFECT_MULT.get();
    }

    /** 是否 Alex's Mobs 生物（namespace=alexsmobs） */
    private static boolean isAm(LivingEntity target) {
        EntityType<?> t = target.getType();
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(t);
        return key != null && "alexsmobs".equals(key.getNamespace());
    }

    /** AM 生物分组判定（返回组名，空串=非 AM 生物；mob/apex 等全体键不依赖分组） */
    private static String groupOf(LivingEntity target) {
        EntityType<?> t = target.getType();
        net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(t);
        if (key == null || !"alexsmobs".equals(key.getNamespace())) return "";
        String path = key.getPath();
        if (TITAN.contains(path)) return "titan";
        if (VENOM.contains(path)) return "venom";
        if (FLYING.contains(path)) return "flying";
        if (AQUATIC.contains(path)) return "aquatic";
        if (BEAST.contains(path)) return "beast";
        return "other"; // AM 生物但未细分（全体键仍生效）
    }

    // ================= 玩家造成伤害：六系克制增伤 =================

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntity().isRemoved()) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (!isAm(event.getEntity())) return;
        String group = groupOf(event.getEntity());
        double mul = eff(sp, "am_mob_damage") + eff(sp, "am_mob_master")
                + eff(sp, "am_apex") + eff(sp, "am_wild_lord") + eff(sp, "am_predator");
        // 分组特化（在全体增伤之上叠加）
        switch (group) {
            case "beast" -> mul += eff(sp, "am_beast_damage");
            case "venom" -> mul += eff(sp, "am_venom_damage");
            case "flying" -> mul += eff(sp, "am_flying_damage");
            case "aquatic" -> mul += eff(sp, "am_aquatic_damage");
            case "titan" -> mul += eff(sp, "am_titan_damage");
            default -> { }
        }
        // 首领级（高生命目标）特化增伤
        if (event.getEntity().getMaxHealth() >= 100.0F) {
            mul += eff(sp, "am_boss_damage");
        }
        if (mul > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 + Math.min(5.0, mul)));
        }
    }

    // ================= 玩家受到 AM 生物伤害：兽皮减免 =================

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer vp)) return;
        DamageSource src = event.getSource();
        LivingEntity srcEnt = src.getEntity() instanceof LivingEntity le ? le : null;
        if (srcEnt == null || !isAm(srcEnt)) return;
        double resist = eff(vp, "am_mob_resist") + eff(vp, "am_mob_master")
                + eff(vp, "am_apex") + eff(vp, "am_wild_lord") + eff(vp, "am_totem");
        if (resist > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 - Math.min(0.9, resist)));
        }
    }

    // ================= 击杀 AM 生物：回血 / 力量 / 速度 =================

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (!isAm(event.getEntity())) return;
        if (!sp.isAlive()) return;
        double heal = eff(sp, "am_kill_heal") + eff(sp, "am_wild_lord") + eff(sp, "am_predator");
        if (heal > 0) sp.heal((float) heal);
        double buffLvl = eff(sp, "am_kill_buff");
        if (buffLvl > 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, (int) Math.min(5, buffLvl - 1), false, true));
        }
        double speedLvl = eff(sp, "am_kill_speed");
        if (speedLvl > 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, (int) Math.min(5, speedLvl - 1), false, true));
        }
    }
}
