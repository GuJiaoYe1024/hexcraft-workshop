package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.config.HexGameRules;
import com.hexcraft.config.HexConfig;
import net.minecraft.world.level.GameRules;
import com.hexcraft.data.HexDataLoader;
import com.hexcraft.hextech.HextechDefinition;
import com.hexcraft.hextech.HextechRegistry;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 全局游戏事件：击杀入账、事件型海克斯效果、登录/重生同步、数据包重载 */
public class HextechEvents {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static final Map<UUID, Map<String, Long>> LAST_KILLS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_KILL_STREAK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RAMPAGE_UNTIL = new ConcurrentHashMap<>();
    /** 额外伤害重入锁：echo/splash/fire_chain/frost_bite 的额外 hurt 不再触发本类伤害事件 */
    private static volatile boolean EXTRA_BUSY = false;

    // ================= 事件型海克斯效果（event 字段格式：效果名:倍率，逗号分隔）=================

    /** 汇总某玩家某种事件效果的倍率×等级之和（支持成长性 stack_growth：层数倍增）；装备 event 并入 */
    public static double effect(PlayerData data, String effect) {
        if (data == null) return 0;
        double sum = effect(data.getHexes(), effect);
        for (PlayerData.EquipEntry eq : data.getEquipment()) {
            HextechDefinition def = HextechRegistry.getEquip(eq.id);
            if (def == null || def.event == null || def.event.isEmpty()) continue;
            for (String token : def.event.split(",")) {
                int ci = token.indexOf(':');
                if (ci <= 0) continue;
                if (token.substring(0, ci).equals(effect)) {
                    try {
                        sum += Double.parseDouble(token.substring(ci + 1)) * eq.level;
                    } catch (Exception ignored) { }
                }
            }
        }
        return sum;
    }

    public static double effect(List<PlayerData.HexEntry> hexes, String effect) {
        double sum = 0;
        for (PlayerData.HexEntry e : hexes) {
            HextechDefinition def = HextechRegistry.get(e.id);
            if (def == null || def.event == null || def.event.isEmpty()) continue;
            for (String token : def.event.split(",")) {
                int ci = token.indexOf(':');
                if (ci <= 0) continue;
                if (token.substring(0, ci).equals(effect)) {
                    try {
                        double v = Double.parseDouble(token.substring(ci + 1));
                        if ("stack_growth".equals(effect)) {
                            sum += v * e.level * (1 + e.stacks);
                        } else {
                            sum += v * e.level;
                        }
                    } catch (Exception ignored) { }
                }
            }
        }
        return sum;
    }

    /** 锻造词条总等级（同词条多档锻造累加；供无上限锻造机制判定） */
    public static double forgeLevel(PlayerData data, String stat) {
        if (data == null) return 0;
        double v = 0;
        for (PlayerData.ForgeEntry f : data.getForges()) {
            if (f.stat.equals(stat)) v += f.level;
        }
        return v;
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        // 玩家死亡：按 gamerule 决定保留内容，并立即刷新客户端（修复"死亡清零但界面残留旧数据"）
        if (victim instanceof ServerPlayer sp) {
            PlayerData data = ModCapabilities.get(sp);
            if (data != null) {
                GameRules gr = sp.serverLevel().getGameRules();
                if (!gr.getBoolean(HexGameRules.KEEP_HEXES)) data.getHexes().clear();
                if (!gr.getBoolean(HexGameRules.KEEP_FORGES)) data.getForges().clear();
                if (!gr.getBoolean(HexGameRules.KEEP_EQUIPS)) data.getEquipment().clear();
                if (!gr.getBoolean(HexGameRules.KEEP_COINS)) data.setCoins(0);
                sync(sp);
            }
            return;
        }
        DamageSource source = event.getSource();
        if (victim instanceof Mob mob) {
            ServerPlayer killer = resolveKiller(source);
            if (killer == null) return;
            PlayerData data = ModCapabilities.get(killer);
            if (data == null) return;
            int coins = rollCoins(mob, killer);
            coins += (int) Math.floor(coins * effect(data, "coin_bonus"));
            if (coins > 0) data.addCoins(coins);

            double heal = effect(data, "kill_heal");
            if (heal > 0) killer.heal((float) heal);

            double tHeal = effect(data, "tacz_heal");
            if (tHeal > 0 && isTaczSource(source)) killer.heal((float) tHeal);

            // 成长性海克斯：击杀获得层数（stack_growth / stack_attr）；叠角龙自身不叠层，只让其它叠层效果翻倍（100% 加成）
            boolean drake = data.getHexes().stream().anyMatch(e -> e.id.equals("hex_stack_drake"));
            int gain = drake ? 2 : 1;
            boolean grew = false;
            for (PlayerData.HexEntry e : data.getHexes()) {
                if (e.id.equals("hex_stack_drake")) continue; // 叠角龙自身不叠层
                HextechDefinition def = HextechRegistry.get(e.id);
                if (def != null && def.event != null
                        && (def.event.contains("stack_growth") || def.event.contains("stack_attr:"))) {
                    e.stacks += gain;
                    grew = true;
                    // 提示说清楚叠的是哪个海克斯
                    killer.displayClientMessage(Component.translatable("message.hexcraft.stack_named",
                            Component.translatable("hexcraft.hextech." + e.id + ".name"), e.stacks), true);
                }
            }
            // 掷骰狂人：击杀几率免费获得一次随机锻造
            double dice = effect(data, "dice_forge");
            if (dice > 0 && killer.getRandom().nextFloat() < (float) dice) {
                String stat = HextechRegistry.randomForgeStat();
                data.getForges().add(new PlayerData.ForgeEntry(stat, 1, killer.getRandom().nextInt(3)));
                killer.displayClientMessage(Component.translatable("message.hexcraft.dice_forge"), true);
                grew = true;
            }
            if (grew) {
                data.applyModifiers(killer);
            }

            // TACZ 弹药回复：tacz_ammo 稳定补弹比例 / tacz_ammo_full 几率补满；弹药补给锻造词条（ammo_aspect +20%/档）提升回复量
            double ammoBoost = 1.0 + forgeLevel(data, "ammo_aspect") * 0.20;
            double ammo = effect(data, "tacz_ammo");
            if (ammo > 0 && restoreTaczAmmo(killer, ammo * ammoBoost)) {
                killer.displayClientMessage(Component.translatable("message.hexcraft.tacz_ammo"), true);
            }
            double ammoFull = effect(data, "tacz_ammo_full");
            if (ammoFull > 0 && killer.getRandom().nextFloat() < (float) ammoFull && restoreTaczAmmo(killer, Math.min(1.0, 1.0 * ammoBoost))) {
                killer.displayClientMessage(Component.translatable("message.hexcraft.tacz_ammo_full"), true);
            }

            // 连杀狂潮：击杀进入 5 秒狂潮（连杀刷新持续时间）
            double rampage = effect(data, "rampage");
            if (rampage > 0) {
                RAMPAGE_UNTIL.put(killer.getUUID(), System.currentTimeMillis() + 5000);
            }
            // 悬赏猎人：精英/首领额外晶币
            double bounty = effect(data, "bounty");
            if (bounty > 0 && mob.getMaxHealth() >= 40) {
                int bonus = (int) Math.floor(bounty * (mob.getMaxHealth() >= 120 ? 2 : 1));
                if (bonus > 0) {
                    data.addCoins(bonus);
                    killer.displayClientMessage(Component.translatable("message.hexcraft.bounty", bonus), true);
                }
            }
            // 引燃链：燃烧目标死亡爆炸
            double fireChain = effect(data, "fire_chain");
            if (fireChain > 0 && mob.isOnFire()) {
                splashAround(killer, mob, (float) (mob.getMaxHealth() * 0.25 * fireChain), 3.0);
            }
            // 派对炸弹：击杀放烟花（小型无害爆炸 + 特效，娱乐机制）
            double firework = effect(data, "firework");
            if (firework > 0) {
                var lvl = mob.level();
                lvl.explode(null, mob.getX(), mob.getY(), mob.getZ(), 0.4f * (float) firework, false, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
            }
            // 经验贪食：击杀掉落经验球
            double xpDrop = effect(data, "xp_drop");
            if (xpDrop > 0) {
                int xp = 2 + killer.getRandom().nextInt(6);
                var orb = new net.minecraft.world.entity.ExperienceOrb(mob.level(),
                        mob.getX(), mob.getY(), mob.getZ(), (int) Math.floor(xp * xpDrop));
                mob.level().addFreshEntity(orb);
            }
            // 锻造机制：击杀回血 / 击杀晶币
            for (PlayerData.ForgeEntry f : data.getForges()) {
                switch (f.stat) {
                    case "regen_aspect" -> killer.heal(1.0f * f.level);
                    case "gold_aspect" -> data.addCoins(1 * f.level);
                    default -> { }
                }
            }

            // ===== 异想天开机制（v0.3.4）=====
            // 击杀吃食物：恢复饱食度（food_on_kill:量）
            double foodKill = effect(data, "food_on_kill");
            if (foodKill > 0) {
                killer.getFoodData().eat(1 + (int) Math.round(foodKill), (float) foodKill);
                killer.displayClientMessage(Component.translatable("message.hexcraft.food_kill"), true);
            }
            // 击杀药水：potion_strength/potion_speed/potion_regen:秒数
            double pStr = effect(data, "potion_strength");
            if (pStr > 0) applyTo(killer, MobEffects.DAMAGE_BOOST, (int) (pStr * 20), 0);
            double pSpd = effect(data, "potion_speed");
            if (pSpd > 0) applyTo(killer, MobEffects.MOVEMENT_SPEED, (int) (pSpd * 20), 0);
            double pReg = effect(data, "potion_regen");
            if (pReg > 0) applyTo(killer, MobEffects.REGENERATION, (int) (pReg * 20), 0);
            // 影响战利品：loot_bonus 概率额外掉落绿宝石
            double loot = effect(data, "loot_bonus");
            if (loot > 0 && killer.getRandom().nextFloat() < (float) loot) {
                int n = 1 + killer.getRandom().nextInt(2);
                mob.spawnAtLocation(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, n), 0.5F);
            }
            // 随机药水：potion_random 击杀随机获得速度/力量/再生/跳跃提升之一
            double pRand = effect(data, "potion_random");
            if (pRand > 0) {
                net.minecraft.world.effect.MobEffect rand = switch (killer.getRandom().nextInt(4)) {
                    case 0 -> MobEffects.MOVEMENT_SPEED;
                    case 1 -> MobEffects.DAMAGE_BOOST;
                    case 2 -> MobEffects.REGENERATION;
                    default -> MobEffects.JUMP;
                };
                applyTo(killer, rand, (int) (pRand * 20), 0);
                killer.displayClientMessage(Component.translatable("message.hexcraft.potion_random"), true);
            }
            // 豪赌：gold_gamble 击杀 50% 赚 n 晶币、50% 亏 1 晶币（余额不为负）
            double gamble = effect(data, "gold_gamble");
            if (gamble > 0) {
                int win = killer.getRandom().nextFloat() < 0.5f ? (int) Math.round(gamble) : -1;
                data.addCoins(win);
                killer.displayClientMessage(Component.translatable(win >= 0 ? "message.hexcraft.gamble_win" : "message.hexcraft.gamble_lose",
                        Math.abs(win)), true);
                sync(killer);
            }
            // 净化：cleanse 击杀清除随机 1 个负面效果
            double cleanse = effect(data, "cleanse");
            if (cleanse > 0) {
                java.util.List<net.minecraft.world.effect.MobEffectInstance> neg = new java.util.ArrayList<>();
                for (net.minecraft.world.effect.MobEffectInstance ei : killer.getActiveEffects()) {
                    if (!ei.getEffect().isBeneficial()) neg.add(ei);
                }
                if (!neg.isEmpty()) {
                    killer.removeEffect(neg.get(killer.getRandom().nextInt(neg.size())).getEffect());
                    killer.displayClientMessage(Component.translatable("message.hexcraft.cleanse"), true);
                }
            }
            // 随机馈赠：random_loot 击杀随机获得 1 件随机物品（跳出固定掉落，随机性拉满）
            double rl = effect(data, "random_loot");
            if (rl > 0 && killer.getRandom().nextFloat() < (float) Math.min(1.0, rl)) {
                net.minecraft.world.item.Item item = switch (killer.getRandom().nextInt(8)) {
                    case 0 -> net.minecraft.world.item.Items.IRON_INGOT;
                    case 1 -> net.minecraft.world.item.Items.BREAD;
                    case 2 -> net.minecraft.world.item.Items.ARROW;
                    case 3 -> net.minecraft.world.item.Items.GUNPOWDER;
                    case 4 -> net.minecraft.world.item.Items.PAPER;
                    case 5 -> net.minecraft.world.item.Items.BOOK;
                    case 6 -> net.minecraft.world.item.Items.EXPERIENCE_BOTTLE;
                    default -> net.minecraft.world.item.Items.EMERALD;
                };
                int n = 1 + killer.getRandom().nextInt(1 + (int) Math.round(rl));
                net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(item, n);
                if (!killer.getInventory().add(st)) {
                    killer.drop(st, false);
                }
                killer.displayClientMessage(Component.translatable("message.hexcraft.random_loot",
                        st.getHoverName()), true);
            }

            LAST_KILL_STREAK.put(killer.getUUID(), System.currentTimeMillis());
            sync(killer);
        }
    }

    /** 击杀者识别：直接玩家；或 TACZ 等模组弹射物（软联动，反射读取持有者） */
    private static ServerPlayer resolveKiller(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer sp) return sp;
        try {
            var direct = source.getDirectEntity();
            if (direct == null) return null;
            if (!direct.getClass().getName().contains("tacz")) return null;
            for (String getter : new String[]{"getOwner", "getShooter", "getCausingEntity", "getOwnerEntity", "getOwnerPlayer"}) {
                try {
                    java.lang.reflect.Method m = direct.getClass().getMethod(getter);
                    Object owner = m.invoke(direct);
                    if (owner instanceof ServerPlayer sp) return sp;
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        return null;
    }

    /** 伤害来源是否为 TACZ 弹射物（软联动） */
    private static boolean isTaczSource(DamageSource source) {
        var direct = source.getDirectEntity();
        return direct != null && direct.getClass().getName().contains("tacz");
    }

    /** 战利品翻倍：loot_double 使生物自身掉落数量翻倍（范围不限，生物掉什么都翻倍；海克斯/装备通用） */
    @SubscribeEvent
    public static void onLootDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        ServerPlayer killer = resolveKiller(event.getSource());
        if (killer == null) return;
        PlayerData data = ModCapabilities.get(killer);
        if (data == null) return;
        double d = effect(data, "loot_double");
        if (d <= 0) return;
        for (net.minecraft.world.entity.item.ItemEntity ie : event.getDrops()) {
            net.minecraft.world.item.ItemStack st = ie.getItem();
            if (!st.isEmpty()) {
                st.setCount(Math.min(st.getMaxStackSize(), st.getCount() * 2));
            }
        }
    }

    /** 经验掉落加成（求知/博学/收割） */
    @SubscribeEvent
    public static void onXpDrop(LivingExperienceDropEvent event) {
        Player killer = event.getAttackingPlayer();
        if (!(killer instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double bonus = effect(data, "xp_bonus");
        if (bonus > 0) {
            event.setDroppedExperience((int) (event.getDroppedExperience() * (1 + bonus)));
        }
    }

    /** 吸血/暴击/近战/远程/TACZ/条件增伤 + 复合机制海克斯 + 锻造机制词条 */
    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player p)) return;
        if (!(p instanceof ServerPlayer sp)) return;
        if (EXTRA_BUSY) return; // 额外伤害不再递归
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        if (amount <= 0) return;

        // —— 吸血（动态提升：嗜血狂怒 / 满血过载）+ 溢出转盾 ——
        double lifesteal = effect(data, "lifesteal");
        float hpRatio = sp.getHealth() / Math.max(1f, sp.getMaxHealth());
        double vamp = effect(data, "vamp_rage");
        if (vamp > 0 && hpRatio < 0.5f) lifesteal += vamp * 2;
        double surge = effect(data, "surge");
        if (surge > 0 && hpRatio >= 1.0f) lifesteal += surge * 0.5;
        if (lifesteal > 0) healWithOverflow(sp, amount * (float) lifesteal, effect(data, "overshield"));

        float dmgMul = 1.0f;
        double crit = effect(data, "crit");
        if (crit > 0 && sp.getRandom().nextFloat() < (float) crit) {
            dmgMul *= 1.5f * (float) (1 + effect(data, "crit_damage")); // 重磅一击：暴击伤害提升
        }

        // 攻击方式分类：近战 / TACZ 弹射物 / 其它弹射物
        net.minecraft.world.entity.Entity direct = event.getSource().getDirectEntity();
        boolean melee = direct == sp;
        boolean tacz = !melee && direct != null && direct.getClass().getName().contains("tacz");
        boolean ranged = !melee && !tacz && direct != null;
        if (melee) dmgMul *= (float) (1 + effect(data, "melee_power"));
        if (ranged) dmgMul *= (float) (1 + effect(data, "ranged_power"));
        if (tacz) {
            dmgMul *= (float) (1 + effect(data, "tacz_power"));
            double tc = effect(data, "tacz_crit");
            if (tc > 0 && sp.getRandom().nextFloat() < (float) tc) dmgMul *= 1.5f;
            double twin = effect(data, "twin_shot"); // 双子射击：几率双倍枪伤
            if (twin > 0 && sp.getRandom().nextFloat() < (float) twin) dmgMul *= 2;
            // TACZ 联动锻造词条：枪械之刃（枪械伤害 +4%/档）、枪械暴击（+3%/档 1.5x）
            dmgMul *= (float) (1 + forgeLevel(data, "gun_aspect") * 0.04);
            if (sp.getRandom().nextFloat() < forgeLevel(data, "gun_crit_aspect") * 0.03f) dmgMul *= 1.5f;
            if (target != null) {
                double dist = sp.distanceTo(target);
                if (dist < 5) dmgMul *= (float) (1 + effect(data, "tacz_close_power"));
                else if (dist > 15) dmgMul *= (float) (1 + effect(data, "tacz_far_power"));
            }
        }
        // 成长性全伤害加成
        dmgMul *= (float) (1 + effect(data, "stack_growth"));
        // 疾风剑客：移速转伤
        double mobi = effect(data, "mobi_power");
        if (mobi > 0) {
            double speed = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).getValue();
            dmgMul *= (float) (1 + mobi * speed * 10);
        }
        // 刽子手（低血斩杀）/ 屠龙者（精英首领增伤）/ 附魔增伤（亡灵杀手/节肢杀手）
        if (target != null) {
            double tRatio = target.getHealth() / Math.max(1f, target.getMaxHealth());
            if (tRatio < 0.2f) dmgMul *= (float) (1 + effect(data, "execute"));
            if (target.getMaxHealth() >= 40) dmgMul *= (float) (1 + effect(data, "boss_power"));
            if (target.getMobType() == net.minecraft.world.entity.MobType.UNDEAD) dmgMul *= (float) (1 + effect(data, "enchant_smite"));
            if (target.getMobType() == net.minecraft.world.entity.MobType.ARTHROPOD) dmgMul *= (float) (1 + effect(data, "enchant_arthropod"));
        }
        // 条件增伤
        if (hpRatio < 0.35f) dmgMul *= (float) (1 + effect(data, "low_hp_power"));
        if (hpRatio >= 1.0f) {
            double fc = effect(data, "full_hp_crit");
            if (fc > 0 && sp.getRandom().nextFloat() < (float) fc) dmgMul *= 1.5f;
            if (surge > 0) dmgMul *= (float) (1 + surge); // 满血过载
        }
        if (sp.level().isNight()) dmgMul *= (float) (1 + effect(data, "night_power"));
        if (System.currentTimeMillis() - LAST_KILL_STREAK.getOrDefault(sp.getUUID(), 0L) < 3000L) {
            dmgMul *= (float) (1 + effect(data, "kill_streak"));
        }
        // 连杀狂潮（击杀后 5 秒）
        if (System.currentTimeMillis() < RAMPAGE_UNTIL.getOrDefault(sp.getUUID(), 0L)) {
            dmgMul *= (float) (1 + effect(data, "rampage"));
        }
        if (dmgMul != 1.0f) event.setAmount(amount * dmgMul);

        // 锻造机制词条：燃烧固定 2 秒（叠加不延长时间，等级改为加深燃烧伤害）；其余固定时长提升强度
        if (target != null && target.isAlive()) {
            for (PlayerData.ForgeEntry f : data.getForges()) {
                switch (f.stat) {
                    case "fire_aspect" -> {
                        target.setSecondsOnFire(2);
                        if (target.isOnFire()) extraDamage(sp, target, amount * 0.04f * f.level);
                    }
                    case "frost_aspect" -> applyTo(target, MobEffects.MOVEMENT_SLOWDOWN, 2 * 20, f.level - 1);
                    case "poison_aspect" -> applyTo(target, MobEffects.POISON, 2 * 20, f.level - 1);
                    case "leech_aspect" -> sp.heal(amount * 0.025f * f.level);
                    case "pierce_aspect" -> target.hurt(sp.damageSources().magic(), amount * 0.025f * f.level);
                    case "crit_aspect" -> {
                        if (sp.getRandom().nextFloat() < 0.07f * f.level) {
                            extraDamage(sp, target, amount * 0.5f);
                        }
                    }
                    case "execute_aspect" -> {
                        float tRatio = target.getHealth() / Math.max(1f, target.getMaxHealth());
                        if (tRatio < 0.2f) extraDamage(sp, target, amount * 0.2f * f.level);
                    }
                    case "splash_aspect" -> {
                        if (melee) splashAround(sp, target, amount * 0.1f * f.level, 2.0);
                    }
                    // ===== 大乱斗/斗魂竞技场风格重做机制（无上限锻造 → 每级数值下调）=====
                    // 雷电之刃：7%/级 几率连锁闪电劈击附近敌人（30% 伤害）
                    case "lightning_aspect" -> {
                        if (sp.getRandom().nextFloat() < 0.07f * f.level) {
                            chainLightning(sp, target, amount * 0.30f, 2.5);
                        }
                    }
                    // 积聚之刃：命中 50% 叠 1 层（上限 50），每层额外伤害 +1%/级
                    case "stack_aspect" -> {
                        if (f.stacks < 50 && sp.getRandom().nextFloat() < 0.5f) {
                            f.stacks++;
                            sp.displayClientMessage(Component.translatable("message.hexcraft.forge_stack", f.stacks), true);
                            sync(sp);
                        }
                        extraDamage(sp, target, amount * 0.01f * f.level * f.stacks);
                    }
                    // 刀锋之舞：攻速溢出部分按 8%/级 转化为额外伤害（参考重磅一击）
                    case "dance_aspect" -> {
                        AttributeInstance asi = sp.getAttribute(Attributes.ATTACK_SPEED);
                        double aspd = asi == null ? 0 : asi.getValue();
                        if (aspd > 1.0) extraDamage(sp, target, amount * 0.08f * f.level * (float) (aspd - 1.0));
                    }
                    // 血怒之刃：半血以下吸血 2%/级 并增伤 6%/级
                    case "bloodlust_aspect" -> {
                        if (sp.getHealth() < sp.getMaxHealth() * 0.5f) {
                            sp.heal(amount * 0.02f * f.level);
                            extraDamage(sp, target, amount * 0.06f * f.level);
                        }
                    }
                    // 收割之刃：对低于 35% 生命的目标造成 30%/级 额外真实伤害（参考收割）
                    case "reaper_aspect" -> {
                        float tr = target.getHealth() / Math.max(1f, target.getMaxHealth());
                        if (tr < 0.35f) target.hurt(sp.damageSources().magic(), amount * 0.3f * f.level);
                    }
                    default -> { }
                }
            }
            // —— 附魔系海克斯（enchant_*）——
            // 锋利之刃：攻击附加真实伤害（无视护甲）
            double eSharp = effect(data, "enchant_sharp");
            if (eSharp > 0) extraDamage(sp, target, amount * (float) eSharp);
            // 火焰附加：点燃目标（每级更久）
            double eFlame = effect(data, "enchant_flame");
            if (eFlame > 0) target.setSecondsOnFire((int) eFlame);
            // 击退附魔：额外击退
            double eKb = effect(data, "enchant_knockback");
            if (eKb > 0) target.knockback((float) eKb, sp.getX() - target.getX(), sp.getZ() - target.getZ());
            // 冰霜之咬：冰冻 + 额外伤害
            double frostBite = effect(data, "frost_bite");
            if (frostBite > 0) {
                applyTo(target, MobEffects.MOVEMENT_SLOWDOWN, 2 * 20, 0);
                extraDamage(sp, target, amount * (float) frostBite * 0.5f);
            }
            // 回响之刃：几率额外 60% 伤害
            double echo = effect(data, "echo");
            if (echo > 0 && sp.getRandom().nextFloat() < (float) echo) {
                extraDamage(sp, target, amount * 0.6f);
            }
            // 爆裂拳手：近战溅射周围 2 格
            double splash = effect(data, "splash");
            if (splash > 0 && melee) {
                splashAround(sp, target, amount * (float) splash * 0.5f, 2.0);
            }
            // 金币拳：攻击命中概率偷 1 晶币（娱乐机制）
            double goldPunch = effect(data, "gold_punch");
            if (goldPunch > 0 && sp.getRandom().nextFloat() < (float) goldPunch) {
                data.addCoins(1);
                sp.displayClientMessage(Component.translatable("message.hexcraft.gold_punch"), true);
            }
            // 心之钢等：攻击命中概率叠永久属性层（stack_attr_onhit:键:倍率，25% 几率）；叠角龙在场时叠层翻倍——提示带海克斯名称
            boolean onHitDrake = data.getHexes().stream().anyMatch(e -> e.id.equals("hex_stack_drake"));
            int onHitGain = onHitDrake ? 2 : 1;
            boolean attrStacked = false;
            for (PlayerData.HexEntry e : data.getHexes()) {
                if (e.id.equals("hex_stack_drake")) continue; // 叠角龙自身不被命中叠层
                HextechDefinition def = HextechRegistry.get(e.id);
                if (def != null && def.event != null && def.event.contains("stack_attr_onhit:")) {
                    if (sp.getRandom().nextFloat() < 0.25f) {
                        e.stacks += onHitGain;
                        data.applyModifiers(sp);
                        sp.displayClientMessage(Component.translatable("message.hexcraft.stack_named",
                                Component.translatable("hexcraft.hextech." + e.id + ".name"), e.stacks), true);
                        attrStacked = true;
                    }
                }
            }
            // 可叠层装备（心之钢等装备的 stack_attr_onhit）：装备层数独立累计，25% 几率叠 1 层（叠角龙在场翻倍）——提示带装备名称
            for (PlayerData.EquipEntry eq : data.getEquipment()) {
                HextechDefinition def = HextechRegistry.getEquip(eq.id);
                if (def != null && def.event != null && def.event.contains("stack_attr_onhit:")) {
                    if (sp.getRandom().nextFloat() < 0.25f) {
                        eq.stacks += onHitGain;
                        data.applyModifiers(sp);
                        sp.displayClientMessage(Component.translatable("message.hexcraft.equip_stack_named",
                                Component.translatable("hexcraft.equip." + eq.id + ".name"), eq.stacks), true);
                        attrStacked = true;
                    }
                }
            }
            // 命中回饱食度（food_on_hit）
            double foodHit = effect(data, "food_on_hit");
            if (foodHit > 0 && sp.getFoodData().getFoodLevel() < 20) {
                sp.getFoodData().eat(1, (float) (foodHit * 0.5));
            }
            if (goldPunch > 0 || attrStacked) {
                sync(sp); // 金币/成长层数实时同步客户端余额与层数显示
            }
        }
    }

    /** 额外伤害（不归属玩家击杀、不递归触发本类伤害事件） */
    private static void extraDamage(ServerPlayer sp, LivingEntity target, float dmg) {
        if (dmg <= 0 || target == null || target.isRemoved()) return;
        EXTRA_BUSY = true;
        try {
            target.hurt(sp.damageSources().genericKill(), dmg);
        } finally {
            EXTRA_BUSY = false;
        }
    }

    /** 溅射/爆炸：对以 target 为中心、半径 r 内的其它活体敌人造成伤害 */
    private static void splashAround(ServerPlayer sp, LivingEntity center, float dmg, double r) {
        if (dmg <= 0) return;
        for (LivingEntity e : sp.level().getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(r), e -> e != sp && e != center && e.isAlive())) {
            EXTRA_BUSY = true;
            try {
                e.hurt(sp.damageSources().genericKill(), dmg);
            } finally {
                EXTRA_BUSY = false;
            }
        }
    }

    /** 雷电之刃：连锁闪电劈击目标附近其它敌人（魔法伤害） */
    private static void chainLightning(ServerPlayer sp, LivingEntity center, float dmg, double r) {
        if (dmg <= 0) return;
        for (LivingEntity e : sp.level().getEntitiesOfClass(LivingEntity.class,
                center.getBoundingBox().inflate(r), e -> e != sp && e != center && e.isAlive())) {
            EXTRA_BUSY = true;
            try {
                e.hurt(sp.damageSources().magic(), dmg);
            } finally {
                EXTRA_BUSY = false;
            }
        }
    }

    /** 治疗（含溢出转吸收护盾） */
    private static void healWithOverflow(ServerPlayer sp, float amount, double overflowRatio) {
        if (amount <= 0) return;
        float before = sp.getHealth();
        sp.heal(amount);
        if (overflowRatio > 0 && sp.getHealth() >= sp.getMaxHealth()) {
            float excess = Math.max(0f, before + amount - sp.getMaxHealth());
            sp.setAbsorptionAmount(sp.getAbsorptionAmount() + excess * (float) overflowRatio);
        }
    }

    private static void applyTo(LivingEntity target, net.minecraft.world.effect.MobEffect effect, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, duration, Math.max(0, amplifier)));
    }

    /** 荆棘反伤 + 濒死减伤 + 反击风暴 + 法术护盾 + 应急护盾 + 死之舞 + 锻造反伤 */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        // 火免/耐火：火焰伤害先结算（免疫直接取消，防后续减伤穿透）
        double fireWard = effect(data, "fire_warden");
        if (fireWard > 0 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
            // 近战来源攻击者（附带火焰/着火效果反击）被点燃
            if (event.getSource().getEntity() instanceof LivingEntity attacker
                    && attacker != sp && attacker.isAlive()) {
                attacker.setSecondsOnFire(Math.max(1, (int) Math.ceil(fireWard)));
            }
            return;
        }
        double fireResist = effect(data, "fire_resist");
        if (fireResist > 0 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            event.setAmount((float) (event.getAmount() * (1 - Math.min(0.9, fireResist))));
        }
        double guard = effect(data, "last_stand");
        if (guard > 0 && sp.getHealth() / Math.max(1f, sp.getMaxHealth()) < 0.30f) {
            event.setAmount((float) (event.getAmount() * (1 - guard)));
        }
        // 法术护盾：魔法伤害减免（1.19+ 伤害系统重构，按伤害消息标识判定魔法）
        double arcane = effect(data, "arcane_shield");
        if (arcane > 0 && event.getSource().getMsgId().contains("magic")) {
            event.setAmount((float) (event.getAmount() * (1 - arcane)));
        }
        // 死之舞：致命伤害几率免疫并回血
        double defy = effect(data, "death_defy");
        if (defy > 0 && event.getAmount() >= sp.getHealth()
                && sp.getRandom().nextFloat() < (float) defy) {
            event.setCanceled(true);
            sp.heal(sp.getMaxHealth() * 0.30f);
            sp.displayClientMessage(Component.translatable("message.hexcraft.death_defy"), true);
            return;
        }
        // 应急护盾：低血时维持吸收护盾
        double lowShield = effect(data, "shield_low");
        if (lowShield > 0 && sp.getHealth() / Math.max(1f, sp.getMaxHealth()) < 0.35f
                && sp.getAbsorptionAmount() < (float) lowShield) {
            sp.setAbsorptionAmount((float) lowShield);
        }
        // 荆棘反伤 + 反击风暴 + 锻造反伤
        double thorns = effect(data, "thorns");
        double counter = effect(data, "counter");
        double forgeThorn = 0;
        for (PlayerData.ForgeEntry f : data.getForges()) {
            if ("thorn_aspect".equals(f.stat)) forgeThorn += f.level;
        }
        if (thorns <= 0 && counter <= 0 && forgeThorn <= 0) return;
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != sp && attacker.isAlive()) {
            if (counter > 0 && sp.getRandom().nextFloat() < (float) counter) {
                attacker.hurt(sp.damageSources().thorns(sp), event.getAmount());
            }
            float thornDmg = (float) (thorns + forgeThorn * 1.0);
            if (thornDmg > 0) {
                attacker.hurt(sp.damageSources().thorns(sp), thornDmg);
            }
        }
    }

    /** 跳跃高度加成 */
    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double boost = effect(data, "jump");
        if (boost > 0) {
            sp.setDeltaMovement(sp.getDeltaMovement().add(0, boost, 0));
        }
    }

    /** 挖掘速度加成 */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double bonus = effect(data, "dig_speed");
        if (bonus > 0) {
            event.setNewSpeed((float) (event.getNewSpeed() * (1 + bonus)));
        }
    }

    // ================= 附魔系：经验修补（拾取经验球修复主手装备） =================

    @SubscribeEvent
    public static void onPickupXp(net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double m = effect(data, "enchant_mending");
        if (m <= 0) return;
        net.minecraft.world.item.ItemStack main = sp.getMainHandItem();
        if (main == null || main.isEmpty() || !main.isDamageableItem()) return;
        int dmg = main.getDamageValue();
        if (dmg <= 0) return;
        int repair = Math.max(1, (int) (event.getOrb().getValue() * m));
        main.setDamageValue(Math.max(0, dmg - repair));
    }

    // ================= 矿物系：掉落处理 + 矿脉共鸣（共用 BlockEvent.BreakEvent；1.20.1 掉落列表可直接修改） =================

    /** 冶炼之触映射表：原矿/粗矿 → 成品锭（不依赖配方系统） */
    private static final java.util.Map<net.minecraft.world.item.Item, net.minecraft.world.item.Item> SMELT_MAP = java.util.Map.of(
            net.minecraft.world.item.Items.RAW_IRON, net.minecraft.world.item.Items.IRON_INGOT,
            net.minecraft.world.item.Items.RAW_GOLD, net.minecraft.world.item.Items.GOLD_INGOT,
            net.minecraft.world.item.Items.RAW_COPPER, net.minecraft.world.item.Items.COPPER_INGOT,
            net.minecraft.world.item.Items.IRON_ORE, net.minecraft.world.item.Items.IRON_INGOT,
            net.minecraft.world.item.Items.GOLD_ORE, net.minecraft.world.item.Items.GOLD_INGOT,
            net.minecraft.world.item.Items.COPPER_ORE, net.minecraft.world.item.Items.COPPER_INGOT,
            net.minecraft.world.item.Items.DEEPSLATE_IRON_ORE, net.minecraft.world.item.Items.IRON_INGOT,
            net.minecraft.world.item.Items.DEEPSLATE_GOLD_ORE, net.minecraft.world.item.Items.GOLD_INGOT,
            net.minecraft.world.item.Items.DEEPSLATE_COPPER_ORE, net.minecraft.world.item.Items.COPPER_INGOT,
            net.minecraft.world.item.Items.NETHER_GOLD_ORE, net.minecraft.world.item.Items.GOLD_INGOT);

    /** 探矿之眼贵重矿物池 */
    private static final net.minecraft.world.item.Item[] RARE_ORES = {
            net.minecraft.world.item.Items.EMERALD, net.minecraft.world.item.Items.DIAMOND,
            net.minecraft.world.item.Items.GOLD_INGOT, net.minecraft.world.item.Items.IRON_INGOT,
            net.minecraft.world.item.Items.LAPIS_LAZULI, net.minecraft.world.item.Items.REDSTONE,
            net.minecraft.world.item.Items.COAL};

    private static boolean VEIN_BUSY = false; // 连锁防递归

    /** 矿物判定：注册名 *_ore 或远古残骸（兼容其它 mod 的矿物方块，比 instanceof OreBlock 更稳） */
    private static boolean isOreBlock(net.minecraft.world.level.block.Block b) {
        try {
            String path = b.builtInRegistryHolder().key().location().getPath();
            return path.endsWith("_ore") || path.equals("ancient_debris");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 矿物系海克斯（1.20.1 BreakEvent 无掉落列表可改 → 有海克斯时 cancel 原版流程，
     * 手动破坏 + 应用效果生成掉落；无相关海克斯的玩家完全走原版，零开销）。
     * 主方块与「矿脉共鸣」连锁子方块统一走 {@link #processMining}，
     * 保证连锁挖掉的矿石同样享受 熔炼/时运翻倍/磁铁拾取，并吃到原版时运附魔及其它 mod 加成。
     */
    @SubscribeEvent
    public static void onBlockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double fortune = effect(data, "mining_fortune");
        double smelt = effect(data, "mining_smelt");
        double mxp = effect(data, "mining_xp");
        double luck = effect(data, "mining_luck");
        double magnet = effect(data, "mining_magnet");
        // 连锁子方块（VEIN_BUSY=true）复用统一处理但不触发二次连锁
        double vein = VEIN_BUSY ? 0 : effect(data, "mining_vein");
        if (fortune <= 0 && smelt <= 0 && mxp <= 0 && luck <= 0 && magnet <= 0 && vein <= 0) return;
        net.minecraft.world.level.block.state.BlockState state = event.getState();
        boolean ore = isOreBlock(state.getBlock());
        if (!ore && luck <= 0 && mxp <= 0) return; // 非矿物方块只处理 探矿/经验
        processMining(sp, state, event.getPos(), ore,
                fortune, smelt, luck, magnet, mxp, event.getExpToDrop(), vein, event);
    }

    /**
     * 统一矿物处理：取消原版破坏 → 手动破坏（不产掉落）→ 原版掉落（含时运/精准采集/其它 mod 加成）
     * → 海克斯时运翻倍 → 熔炼（静态表快查 + 冶炼配方兜底，兼容其它 mod 矿石）→ 探矿/磁铁 → 生成掉落与经验 → 连锁开采。
     * @param vein 仅主方块 >0；连锁子方块传 0（不二次连锁）
     */
    private static void processMining(ServerPlayer sp, net.minecraft.world.level.block.state.BlockState state,
                                      net.minecraft.core.BlockPos pos, boolean ore,
                                      double fortune, double smelt, double luck, double magnet, double mxp,
                                      int exp, double vein, net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        net.minecraft.server.level.ServerLevel slevel = (net.minecraft.server.level.ServerLevel) sp.level();
        if (event != null) event.setCanceled(true); // 取消原版破坏流程（主方块与连锁子方块统一）
        slevel.destroyBlock(pos, false);

        // 原版掉落（含精准采集/时运附魔/其它 mod 掉落加成）；镐挖矿物时的 self drop 也由 getDrops 处理
        java.util.List<net.minecraft.world.item.ItemStack> drops =
                net.minecraft.world.level.block.Block.getDrops(state, slevel, pos, slevel.getBlockEntity(pos), sp, sp.getMainHandItem());
        // 时运降临：矿物掉落翻倍（连锁子方块同样生效）
        if (fortune > 0 && ore) {
            java.util.List<net.minecraft.world.item.ItemStack> copy = new java.util.ArrayList<>(drops);
            for (net.minecraft.world.item.ItemStack s : copy) {
                net.minecraft.world.item.ItemStack d = s.copy();
                d.setCount(Math.max(1, s.getCount() * (int) fortune));
                drops.add(d);
            }
        }
        // 冶炼之触：原矿自动烧炼成锭（静态表快查，未命中走冶炼配方系统 → 其它 mod 矿石只要有熔炉配方即可熔炼）
        if (smelt > 0 && ore) {
            for (int i = 0; i < drops.size(); i++) {
                net.minecraft.world.item.ItemStack s = drops.get(i);
                net.minecraft.world.item.Item smelted = SMELT_MAP.get(s.getItem());
                if (smelted == null) smelted = smeltByRecipe(slevel, s);
                if (smelted != null) drops.set(i, new net.minecraft.world.item.ItemStack(smelted, s.getCount()));
            }
        }
        // 探矿之眼：挖普通方块几率额外掉贵重矿物
        if (luck > 0 && !ore && sp.getRandom().nextFloat() < (float) luck) {
            drops.add(new net.minecraft.world.item.ItemStack(RARE_ORES[sp.getRandom().nextInt(RARE_ORES.length)], 1));
        }
        // 自动拾取：掉落直接进背包（放不下留在原地）
        if (magnet > 0) {
            for (net.minecraft.world.item.ItemStack s : new java.util.ArrayList<>(drops)) {
                if (sp.getInventory().add(s)) drops.remove(s);
            }
        }
        // 生成掉落
        for (net.minecraft.world.item.ItemStack s : drops) {
            net.minecraft.world.level.block.Block.popResource(slevel, pos, s);
        }
        // 经验：事件已按方块/附魔算好（主方块与连锁子方块事件均提供）
        if (exp > 0) {
            sp.level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(sp.level(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, exp));
        }
        if (mxp > 0) {
            sp.level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(sp.level(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Math.max(1, (int) Math.floor(mxp))));
        }
        // 矿脉共鸣：挖掘矿物几率连锁开采周围同种矿物（最多 8 个）；子方块事件复用 processMining（vein=0）不二次连锁
        if (vein > 0 && ore && sp.getRandom().nextFloat() < (float) vein) {
            VEIN_BUSY = true;
            try {
                java.util.ArrayDeque<net.minecraft.core.BlockPos> queue = new java.util.ArrayDeque<>();
                java.util.Set<Long> visited = new java.util.HashSet<>();
                queue.add(pos);
                visited.add(pos.asLong());
                int mined = 0;
                while (!queue.isEmpty() && mined < 8) {
                    net.minecraft.core.BlockPos cur = queue.poll();
                    if (!cur.equals(pos)) {
                        if (!sp.level().getBlockState(cur).is(state.getBlock())) continue;
                        sp.gameMode.destroyBlock(cur); // 触发 BreakEvent → VEIN_BUSY 分支 → processMining（熔炼/时运/拾取/经验，不连锁）
                        mined++;
                    }
                    for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                        net.minecraft.core.BlockPos nb = cur.relative(dir);
                        if (!visited.contains(nb.asLong()) && sp.level().getBlockState(nb).is(state.getBlock())) {
                            visited.add(nb.asLong());
                            queue.add(nb);
                        }
                    }
                }
            } finally {
                VEIN_BUSY = false;
            }
        }
    }

    /** 冶炼配方兜底：查询熔炉冶炼配方（覆盖其它 mod 矿石，只要原版熔炉能烧） */
    private static net.minecraft.world.item.Item smeltByRecipe(net.minecraft.server.level.ServerLevel level,
                                                               net.minecraft.world.item.ItemStack input) {
        try {
            net.minecraft.world.item.crafting.SmeltingRecipe rec = level.getRecipeManager()
                    .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                            new net.minecraft.world.SimpleContainer(input), level)
                    .orElse(null);
            if (rec != null) {
                net.minecraft.world.item.ItemStack out = rec.getResultItem(level.registryAccess());
                if (out != null && !out.isEmpty()) return out.getItem();
            }
        } catch (Exception ignored) { }
        return null;
    }

    /** 持续回血（再生/不朽）：每 5 秒回复 */
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double regen = effect(data, "regen");
        if (regen > 0 && sp.tickCount % 100 == 0) {
            sp.heal((float) regen);
        }
    }

    // ================= 晶币掉落 =================

    private static int rollCoins(Mob mob, ServerPlayer killer) {
        double hp = mob.getMaxHealth();
        int min, max;
        if (hp >= 120) { min = HexConfig.COIN_BOSS_MIN.get(); max = HexConfig.COIN_BOSS_MAX.get(); }
        else if (hp >= 40) { min = HexConfig.COIN_ELITE_MIN.get(); max = HexConfig.COIN_ELITE_MAX.get(); }
        else { min = HexConfig.COIN_MIN.get(); max = HexConfig.COIN_MAX.get(); }
        int coins = min + killer.getRandom().nextInt(Math.max(1, max - min + 1));

        int window = HexConfig.ANTI_FARM_WINDOW_SEC.get();
        if (window > 0) {
            String type = mob.getType().getDescriptionId();
            long now = System.currentTimeMillis();
            Map<String, Long> m = LAST_KILLS.computeIfAbsent(killer.getUUID(), k -> new ConcurrentHashMap<>());
            Long last = m.get(type);
            m.put(type, now);
            m.entrySet().removeIf(e -> now - e.getValue() > window * 1000L + 30_000L);
            if (last != null && now - last < window * 1000L) {
                coins = (int) Math.max(0, Math.floor(coins * HexConfig.ANTI_FARM_DECAY.get()));
            }
        }
        return coins;
    }

    // ================= 生命周期与同步 =================

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerData data = ModCapabilities.get(sp);
            if (data == null) return;
            data.applyModifiers(sp);
            sync(sp);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        try {
            PlayerData old = ModCapabilities.get(event.getOriginal());
            PlayerData neu = ModCapabilities.get(event.getEntity());
            if (old != null && neu != null) {
                neu.copyFrom(old);
            }
        } catch (Exception e) {
            // 旧实体 capability 异常时不阻断重生；服务端数据以玩家 NBT 为准
        }
        // 无论 copyFrom 是否成功，都强制重算属性并刷新客户端界面（杜绝"死亡清零但面板残留旧界面"）
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerData d = ModCapabilities.get(sp);
            if (d != null) {
                d.applyModifiers(sp);
                sync(sp);
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // 重生完成后再同步一次，覆盖 Clone 事件遗漏的刷新
        if (event.getEntity() instanceof ServerPlayer sp) {
            PlayerData d = ModCapabilities.get(sp);
            if (d != null) {
                d.applyModifiers(sp);
                sync(sp);
            }
        }
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new HexDataLoader());
        event.addListener(new com.hexcraft.data.EquipmentDataLoader());
    }

    /**
     * TACZ 弹药补充（多通道）：
     * ① 手持枪械 NBT 弹药键回填（TACZ 核心键 GunCurrentAmmoCount，读弹容钳制、满弹不补，真正回复当前枪械子弹）
     * ② 耐久路径兜底（旧版本 TACZ 用耐久表示弹药）
     * ③ 背包内 TACZ 弹匣物品回填 + 弹药盒数量补给
     */
    private static final String[] AMMO_KEYS = {
            "GunCurrentAmmoCount", "AmmoCount", "ammoCount", "Ammo", "ammo", "MagAmmoCount",
            "MagazineAmmoCount", "CurrentAmmo", "BulletCount", "AmmoAmount", "ammo_amount"
    };

    private static boolean restoreTaczAmmo(ServerPlayer player, double ratio) {
        boolean any = false;
        // 通道①：主/副手 TACZ 枪械（NBT 弹药键回填）
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HAND) continue;
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl == null || !"tacz".equals(rl.getNamespace())) continue;
            boolean fromNbt = false;
            if (stack.hasTag()) {
                int[] hit = findAmmoInt(stack.getTag(), 0);
                if (hit != null && hit[1] > 0) {
                    int maxAmmo = taczMaxAmmo(player, stack.getTag());
                    if (maxAmmo > 0 && hit[1] >= maxAmmo) {
                        fromNbt = true; // 满弹：本次枪械不补（背包通道仍可补弹药盒）
                    } else {
                        int restored = Math.max(1, (int) Math.floor(hit[1] * ratio));
                        String key = hit[0] >= 0 ? AMMO_KEYS[hit[0]] : "GunCurrentAmmoCount";
                        int target = maxAmmo > 0 ? Math.min(hit[1] + restored, maxAmmo) : hit[1] + restored;
                        stack.getTag().putInt(key, target);
                        any = true;
                        fromNbt = true;
                        LOGGER.debug("Hexcraft TACZ ammo channel-1: {} key={} +{} ({} -> {}/{})",
                                stack.getItem(), key, restored, hit[1], target, maxAmmo > 0 ? maxAmmo : "?");
                    }
                }
            }
            if (!fromNbt) {
                int max = stack.getMaxDamage();
                if (max > 0 && stack.getDamageValue() > 0) {
                    int restore = Math.max(1, (int) Math.floor(max * ratio));
                    stack.setDamageValue(Math.max(0, stack.getDamageValue() - restore));
                    any = true;
                    LOGGER.debug("Hexcraft TACZ ammo channel-2 durability: {} -{}", stack.getItem(), restore);
                }
            }
        }
        // 通道③：背包内 TACZ 弹匣/弹药物品（弹匣 NBT 回填同钳制；弹药盒走数量 +1）
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (rl == null || !"tacz".equals(rl.getNamespace())) continue;
            if (stack.hasTag()) {
                int[] hit = findAmmoInt(stack.getTag(), 0);
                if (hit != null && hit[1] > 0) {
                    int maxAmmo = taczMaxAmmo(player, stack.getTag());
                    if (!(maxAmmo > 0 && hit[1] >= maxAmmo)) {
                        int restored = Math.max(1, (int) Math.floor(hit[1] * ratio));
                        String key = hit[0] >= 0 ? AMMO_KEYS[hit[0]] : "GunCurrentAmmoCount";
                        int target = maxAmmo > 0 ? Math.min(hit[1] + restored, maxAmmo) : hit[1] + restored;
                        stack.getTag().putInt(key, target);
                        any = true;
                        LOGGER.debug("Hexcraft TACZ ammo channel-3 mag: {} key={} +{}", stack.getItem(), key, restored);
                    }
                    continue;
                }
            }
            if (stack.getMaxStackSize() > 1 && stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                any = true;
                LOGGER.debug("Hexcraft TACZ ammo channel-3 item: {} +1", stack.getItem());
            }
        }
        if (any) LOGGER.debug("Hexcraft TACZ ammo restored, ratio={}", ratio);
        return any;
    }

    /** 读取 TACZ 枪械/弹匣的 GunId（NBT 递归 ≤3 层，键 GunId） */
    private static String readTaczGunId(CompoundTag tag, int depth) {
        if (tag == null || depth > 3) return null;
        if (tag.contains("GunId", CompoundTag.TAG_STRING)) return tag.getString("GunId");
        for (String k : tag.getAllKeys()) {
            if (tag.get(k) instanceof CompoundTag sub) {
                String g = readTaczGunId(sub, depth + 1);
                if (g != null) return g;
            }
        }
        return null;
    }

    /** 读取 TACZ 枪械弹容（资源包 data/<ns>/guns/<gun>_data.json，多键名兼容）；失败返回 -1（不钳制） */
    private static int taczMaxAmmo(ServerPlayer player, CompoundTag tag) {
        try {
            String gunIdStr = readTaczGunId(tag, 0);
            if (gunIdStr == null) return -1;
            net.minecraft.resources.ResourceLocation gunId = net.minecraft.resources.ResourceLocation.tryParse(gunIdStr);
            if (gunId == null) return -1;
            var opt = player.serverLevel().getServer().getResourceManager()
                    .getResource(new net.minecraft.resources.ResourceLocation(gunId.getNamespace(), "guns/" + gunId.getPath() + "_data.json"));
            if (opt.isEmpty()) return -1;
            try (var in = opt.get().open()) {
                var obj = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject();
                for (String k : new String[]{"ammo_amount", "ammoAmount", "ammo", "maxAmmo", "ammoCapacity", "ammo_capacity"}) {
                    if (obj.has(k)) return obj.get(k).getAsInt();
                }
            }
        } catch (Exception e) { }
        return -1;
    }

    /** 递归（≤3 层）查找 NBT 中第一个弹药整型键；返回 [keyIndex 或 -1, value] */
    private static int[] findAmmoInt(CompoundTag tag, int depth) {
        if (tag == null || depth > 3) return null;
        for (int i = 0; i < AMMO_KEYS.length; i++) {
            if (tag.contains(AMMO_KEYS[i], CompoundTag.TAG_INT)) {
                return new int[]{i, tag.getInt(AMMO_KEYS[i])};
            }
        }
        for (String k : tag.getAllKeys()) {
            if (tag.get(k) instanceof CompoundTag sub) {
                int[] hit = findAmmoInt(sub, depth + 1);
                if (hit != null) return hit;
            }
        }
        return null;
    }

    public static void sync(ServerPlayer sp) {
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), Packets.S2CSyncData.of(data, sp));
    }
}
