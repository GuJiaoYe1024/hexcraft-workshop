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
import java.util.Set;
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

    /** v0.6.2 流血 DoT（独立实例，独立计时不互相覆盖）：目标UUID → [dmgPerTick, remainingTicks] */
    private static final Map<UUID, List<double[]>> BLEEDS = new ConcurrentHashMap<>();
    /** v0.6.2 猎手标记（3.5 秒）：目标UUID → 标记来源玩家UUID */
    private static final Map<UUID, UUID> MARKS = new ConcurrentHashMap<>();
    // v0.6.16 新增机制状态：风暴专注连击计数 / 以血还血受击窗口 / 稳定握持命中窗口
    private static final Map<UUID, Integer> COMBO_COUNT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> VENGE_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> STEADY_UNTIL = new ConcurrentHashMap<>();
    // v0.6.16 架枪射击：玩家位置跟踪（持有 tacz_bipod 时才记录，登出/失去自动清理）
    private static final Map<UUID, double[]> LAST_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_MOVE = new ConcurrentHashMap<>();
    // v0.6.16 快速装填（弹量 0→正=换弹完成）/ 切枪强化（主手物品变化）状态
    private static final Map<UUID, Integer> LAST_TACZ_AMMO = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RELOAD_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, net.minecraft.world.item.Item> LAST_MAIN_HAND = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SWAP_UNTIL = new ConcurrentHashMap<>();
    // v0.6.16 持续射击（命中链）/ 棱彩追踪（同目标印记）/ 暴击链 / 多重射击（防递归）状态
    private static final Map<UUID, Integer> BAR_HITS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> BAR_LAST_HIT = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<UUID, long[]>> TACZ_TRACK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> CRIT_CHAIN_UNTIL = new ConcurrentHashMap<>();
    private static final Set<UUID> MULTI_BUSY = ConcurrentHashMap.newKeySet();

    /** 施加流血实例（每 tick 伤害 / 存活 tick）；独立计时，多次命中叠加多个实例 */
    private static void applyBleed(LivingEntity target, float dmgPerTick, int ticks) {
        if (dmgPerTick <= 0 || target == null || target.isRemoved()) return;
        BLEEDS.computeIfAbsent(target.getUUID(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(new double[]{dmgPerTick, ticks});
    }

    /** 施加猎手标记（70 tick = 3.5 秒） */
    private static void applyMark(ServerPlayer sp, LivingEntity target) {
        if (target == null || target.isRemoved()) return;
        MARKS.put(target.getUUID(), sp.getUUID());
    }

    /** v0.6.0 校准：stack_attr_onhit 叠层概率数据化——JSON event 中带 chance:xxx 则读取（0~1），
     *  缺省回退 0.25f（兼容旧数据包，如心之钢 v0.5.0 前无 chance 字段）。概率不随等级缩放。 */
    public static float stackOnHitChance(HextechDefinition def) {
        if (def == null || def.event == null) return 0.25f;
        for (String token : def.event.split(",")) {
            int ci = token.indexOf(':');
            if (ci <= 0) continue;
            if (token.substring(0, ci).equals("chance")) {
                try {
                    float v = Float.parseFloat(token.substring(ci + 1));
                    if (v > 0 && v <= 1f) return v;
                } catch (Exception ignored) { }
            }
        }
        return 0.25f;
    }

    /** v0.6.16 叠角龙统一倍率：每级 +75%（对齐 lang 描述"多获得 75%"），无叠角龙 = 1。
     *  应用范围：击杀叠层(stack_growth)、命中叠层(stack_attr_onhit)、受击叠层(stack_attr_onhurt)、
     *  onKill 触发器叠层(StackSpec)——此前受击叠层完全不吃叠角龙、且叠角龙固定 ×2 不看等级，已统一。 */
    public static double drakeMult(PlayerData data) {
        if (data == null) return 1;
        for (PlayerData.HexEntry e : data.getHexes()) {
            if (e.id.equals("hex_stack_drake")) return 1 + com.hexcraft.config.HexConfig.STACK_DRAKE_MULT.get() * e.level;
        }
        return 1;
    }

    /** 按叠层倍率生成整数层数增量：整数部分固定 + 小数部分按概率进位（长期期望 = 倍率），叠层字段为 int 时的精确表达 */
    private static int gainWithMult(double mult, net.minecraft.util.RandomSource rng) {
        if (mult <= 0) return 0;
        int base = (int) Math.floor(mult);
        double frac = mult - base;
        return base + (rng.nextFloat() < frac ? 1 : 0);
    }

    /** v0.6.16 叠层安全累加：int 防溢出（无上限配置 + 高数值整合包长期累积可能溢出成负数 → 属性异常）；
     *  钳到 Integer.MAX_VALUE 保持数值只增不减的语义 */
    private static int safeStackAdd(int cur, int add) {
        if (cur >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        long v = (long) cur + add;
        return (int) Math.min(v, Integer.MAX_VALUE);
    }

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
        // v0.6.21 事件型锻造词条：效果键匹配时按 等级×档位倍率 汇总（属性词条不在此列）
        for (PlayerData.ForgeEntry f : data.getForges()) {
            String ev = HextechRegistry.forgeEventKey(f.stat);
            if (ev != null && ev.equals(effect)) {
                try {
                    double per = HextechRegistry.forgeMod(f.stat).perLevel;
                    sum += per * f.level * forgeTierMult(f);
                } catch (Exception ignored) { }
            }
        }
        return sum;
    }

    public static double effect(List<PlayerData.HexEntry> hexes, String effect) {
        double sum = 0;
        for (PlayerData.HexEntry e : hexes) {
            HextechDefinition def = HextechRegistry.get(e.id);
            // v0.6.4 联机客户端兜底：客户端 Registry 只有 static 内置（数据包仅服务端加载），缺失时读服务端同步缓存
            if (def == null) {
                try { def = com.hexcraft.client.ClientDataStore.hexDef(e.id); } catch (Throwable ignored) { }
            }
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

    /** v0.5.0 段位强度：锻造词条档位倍率（白银×1 / 黄金×2 / 棱彩×3，钳制非法档位；v0.6.1 起读运行时配置） */
    public static double forgeTierMult(PlayerData.ForgeEntry f) {
        if (f == null) return 1;
        return com.hexcraft.hextech.HextechRegistry.tierMult(f.tier);
    }

    /** v0.5.0 段位加权总等级：Σ 等级×档位倍率（TACZ 枪械/暴击/弹药词条统一按档位加权，配得上棱彩档） */
    public static double forgeLevelMult(PlayerData data, String stat) {
        if (data == null) return 0;
        double v = 0;
        for (PlayerData.ForgeEntry f : data.getForges()) {
            if (f.stat.equals(stat)) v += f.level * forgeTierMult(f);
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
                // v0.4.11：死亡保留以客户端配置为主（主菜单即可设置并保存，进世界即生效）；
                // 服务端 gamerule（创建世界界面 / 服务端指令）任一开启即保留，两链互不阻塞
                boolean keepH = gr.getBoolean(HexGameRules.KEEP_HEXES) || com.hexcraft.config.HexConfig.KEEP_DEATH_HEXES.get();
                boolean keepF = gr.getBoolean(HexGameRules.KEEP_FORGES) || com.hexcraft.config.HexConfig.KEEP_DEATH_FORGES.get();
                boolean keepE = gr.getBoolean(HexGameRules.KEEP_EQUIPS) || com.hexcraft.config.HexConfig.KEEP_DEATH_EQUIPS.get();
                boolean keepC = gr.getBoolean(HexGameRules.KEEP_COINS) || com.hexcraft.config.HexConfig.KEEP_DEATH_COINS.get();
                // v0.4.12：死亡决策日志（诊断"开了保留仍被清"用：打印 gamerule 与客户端配置各自实际值）
                com.hexcraft.Hexcraft.LOGGER.info("[Hexcraft] 玩家 {} 死亡：gamerule={}/{}/{}/{} 客户端配置={}/{}/{}/{} → 实际保留={}/{}/{}/{}",
                        sp.getName().getString(),
                        gr.getBoolean(HexGameRules.KEEP_HEXES), gr.getBoolean(HexGameRules.KEEP_FORGES),
                        gr.getBoolean(HexGameRules.KEEP_EQUIPS), gr.getBoolean(HexGameRules.KEEP_COINS),
                        com.hexcraft.config.HexConfig.KEEP_DEATH_HEXES.get(), com.hexcraft.config.HexConfig.KEEP_DEATH_FORGES.get(),
                        com.hexcraft.config.HexConfig.KEEP_DEATH_EQUIPS.get(), com.hexcraft.config.HexConfig.KEEP_DEATH_COINS.get(),
                        keepH, keepF, keepE, keepC);
                if (!keepH) data.getHexes().clear();
                if (!keepF) data.getForges().clear();
                if (!keepE) data.getEquipment().clear();
                if (!keepC) data.setCoins(0);
                // v0.6.0 战斗叠层：死亡清空（积聚之刃等战斗叠层不跨死亡保留；永久叠层保留）
                data.clearCombatStacks();
                // v0.4.13：死亡瞬间把 PlayerData 备份进 persistentData（随玩家 NBT 保存 & 随重生 copyFrom 复制），
                // onClone 双通道恢复——capability 在死亡/重生过程中失效也不丢数据；
                // v0.4.14：同步写全量持久化快照 hexcraft_data（登出/自动保存后重进也可恢复）
                try {
                    CompoundTag bak = data.serializeNBT();
                    sp.getPersistentData().put("hexcraft_keep", bak);
                    sp.getPersistentData().put("hexcraft_data", bak);
                } catch (Exception e) {
                    // 备份失败不影响主流程
                }
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
            // v0.8.0 维度专属海克斯：下界贪欲（仅下界击杀生效）
            if (inNether(killer)) coins += (int) Math.floor(coins * effect(data, "nether_coin"));
            // 世界海克斯独立乘区：全体玩家晶币获取倍率 + 击杀敌对生物晶币加成 + 固定晶币税（独立于玩家海克斯，不与其它 mod 冲突）
            if (mob.level() instanceof net.minecraft.server.level.ServerLevel whLevel) {
                double whCoin = com.hexcraft.event.WorldHexEvents.coinMult(whLevel);
                if (whCoin != 0) coins += (int) Math.floor(coins * whCoin);
                double whMob = com.hexcraft.event.WorldHexEvents.mobCoinBonus(whLevel, mob);
                if (whMob != 0) coins += (int) Math.floor(coins * whMob);
                coins += (int) Math.floor(com.hexcraft.event.WorldHexEvents.coinFlat(whLevel)); // 晶币税（白银纯数字固定值）
            }
            if (coins > 0) data.addCoins(coins);

            double heal = effect(data, "kill_heal");
            if (heal > 0) killer.heal((float) heal);

            double tHeal = effect(data, "tacz_heal");
            if (tHeal > 0 && isTaczSource(source)) killer.heal((float) tHeal);

            // 成长性海克斯：击杀获得层数（stack_growth / stack_attr）；叠角龙自身不叠层，只让其它叠层效果翻倍（每级 +75%）
            boolean drake = data.getHexes().stream().anyMatch(e -> e.id.equals("hex_stack_drake"));
            double dMult = drakeMult(data);
            int gain = drake ? gainWithMult(dMult, killer.getRandom()) : 1;
            boolean grew = false;
            for (PlayerData.HexEntry e : data.getHexes()) {
                if (e.id.equals("hex_stack_drake")) continue; // 叠角龙自身不叠层
                HextechDefinition def = HextechRegistry.get(e.id);
                if (def != null && def.event != null
                        && (def.event.contains("stack_growth") || def.event.contains("stack_attr:"))) {
                    e.stacks = safeStackAdd(e.stacks, gain);
                    grew = true;
                    // 提示说清楚叠的是哪个海克斯
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.stack_named",
                            Component.translatable("hexcraft.hextech." + e.id + ".name"), e.stacks), 0);
                }
            }
            // 掷骰狂人：击杀几率免费获得一次随机锻造（档位与词条同步随机；tier 记录实际档位 → 强度倍率配档）
            double dice = effect(data, "dice_forge");
            if (dice > 0 && killer.getRandom().nextFloat() < (float) dice) {
                int t = HextechRegistry.randomTier();
                // v0.6.0 单总池：加权抽取（与锻造三选一同口径）
                String stat = com.hexcraft.hextech.HextechRoller.rollForgeOne(t);
                // v0.6.0 同词条叠加规则（STACK 默认 / BEST_ONLY 最高档保留+回收）
                int refund = data.addForge(stat, 1, t);
                if (refund > 0) {
                    data.addCoins(refund);
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.forge_refund",
                            Component.translatable("hexcraft.forge." + stat + ".name"), refund), 0);
                }
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.dice_forge"), 0);
                grew = true;
            }
            if (grew) {
                data.applyModifiers(killer);
            }

            // ===== v0.6.2 击杀触发叠层（def.stack.onKill=true）=====
            // 击杀获取 1 层（叠角龙在场翻倍；永续成长世界海克斯 +50%）；永久叠层吃全局倍率/上限（服务端配置）
            boolean killStacked = false;
            for (PlayerData.HexEntry e : data.getHexes()) {
                HextechDefinition sdef = HextechRegistry.get(e.id);
                if (sdef == null || sdef.stack == null || !sdef.stack.onKill) continue;
                if (data.getStack(sdef.stack.key) == null) {
                    data.putStack(new PlayerData.StackEntry(sdef.stack.key, sdef.stack.cap, sdef.stack.combat,
                            sdef.stack.decayDelay, sdef.stack.decayRate));
                }
                double g = 1.0;
                if (drake) g *= dMult;
                if (killer.level() instanceof net.minecraft.server.level.ServerLevel whKL) {
                    g *= (1 + com.hexcraft.event.WorldHexEvents.permStackGainBonus(whKL));
                }
                if (!sdef.stack.combat) {
                    g *= com.hexcraft.config.HexConfig.PERMANENT_STACK_MULT.get();
                }
                data.addStack(sdef.stack.key, g, 1);
                PlayerData.StackEntry s = data.getStack(sdef.stack.key);
                // 永久叠层全局上限钳制（v0.6.16 配置 0=无限制时跳过）
                int pCap = com.hexcraft.config.HexConfig.PERMANENT_STACK_CAP.get();
                if (pCap > 0 && !sdef.stack.combat && s != null && s.value > pCap) {
                    s.value = pCap;
                }
                if (s != null) {
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.stack_named",
                            Component.translatable("hexcraft.hextech." + e.id + ".name"), (int) s.value), 0);
                }
                killStacked = true;
            }
            // ===== v0.6.2 流血扩散（bleed_spread）：目标死亡时若处流血状态，向周围 2.5 格敌对生物扩散流血 =====
            double spread = effect(data, "bleed_spread");
            if (spread > 0) {
                java.util.List<double[]> insts = BLEEDS.remove(mob.getUUID());
                if (insts != null && !insts.isEmpty()) {
                    double maxDmg = 0;
                    for (double[] d : insts) maxDmg = Math.max(maxDmg, d[0]);
                    if (maxDmg > 0) {
                        for (LivingEntity other : killer.level().getEntitiesOfClass(LivingEntity.class,
                                mob.getBoundingBox().inflate(2.5),
                                o -> o != killer && o != mob && o.isAlive() && o instanceof net.minecraft.world.entity.monster.Monster)) {
                            applyBleed(other, (float) (maxDmg * (1 + spread)), 100);
                        }
                        com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.bleed_spread"), 0);
                    }
                }
            }
            if (killStacked) sync(killer);

            // TACZ 弹药回复：tacz_ammo 稳定补弹比例 / tacz_ammo_full 几率补满（v0.6.0 弹药补给锻造词条 ammo_aspect 迁出，回复量由海克斯等级直接驱动）
            double ammoBoost = 1.0;
            double ammo = effect(data, "tacz_ammo");
            if (ammo > 0 && restoreTaczAmmo(killer, ammo * ammoBoost)) {
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.tacz_ammo"), 0);
            }
            double ammoFull = effect(data, "tacz_ammo_full");
            if (ammoFull > 0 && killer.getRandom().nextFloat() < (float) ammoFull && restoreTaczAmmo(killer, Math.min(1.0, 1.0 * ammoBoost))) {
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.tacz_ammo_full"), 0);
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
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.bounty", bonus), 0);
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
            // v0.6.0 机制海克斯化：锻造不再产出击杀回血/击杀晶币（已有 regen/gold_punch 等海克斯覆盖）

            // ===== 异想天开机制（v0.3.4）=====
            // 击杀吃食物：恢复饱食度（food_on_kill:量）
            double foodKill = effect(data, "food_on_kill");
            if (foodKill > 0) {
                killer.getFoodData().eat(1 + (int) Math.round(foodKill), (float) foodKill);
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.food_kill"), 0);
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
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.potion_random"), 0);
            }
            // 豪赌：gold_gamble 击杀 50% 赚 n 晶币、50% 亏 1 晶币（余额不为负）
            double gamble = effect(data, "gold_gamble");
            if (gamble > 0) {
                int win = killer.getRandom().nextFloat() < 0.5f ? (int) Math.round(gamble) : -1;
                data.addCoins(win);
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable(win >= 0 ? "message.hexcraft.gamble_win" : "message.hexcraft.gamble_lose",
                        Math.abs(win)), 0);
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
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, Component.translatable("message.hexcraft.cleanse"), 0);
                }
            }
            // 随机馈赠：random_loot 击杀随机获得 1 件随机物品（全物品池：原版+全部模组物品，过滤空气与技术物品）
            double rl = effect(data, "random_loot");
            if (rl > 0 && killer.getRandom().nextFloat() < (float) Math.min(1.0, rl)) {
                java.util.List<net.minecraft.world.item.Item> pool = randomGiftPool();
                if (!pool.isEmpty()) {
                    // v0.6.19 防御性重 roll：池内偶发空气/占位条目时重抽（最多 8 次），实例 + 注册路径 + hoverName 三重检查
                    net.minecraft.world.item.Item item = null;
                    for (int tries = 0; tries < 8; tries++) {
                        net.minecraft.world.item.Item cand = pool.get(killer.getRandom().nextInt(pool.size()));
                        if (cand == null || isAirLike(cand)) continue;
                        item = cand;
                        break;
                    }
                    if (item != null) {
                        // v0.6.19 终极兜底：送物品/发提示前再核一次，任何"空气类"条目一律跳过该次馈赠——
                        // 宁可少送一次也绝不让"空气"字样进入任何提示文本
                        if (isAirLike(item)) {
                            LOGGER.warn("[Hexcraft][Gift] skipped air-like item {}", net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item));
                        } else {
                            int n = 1 + killer.getRandom().nextInt(1 + (int) Math.round(rl));
                            net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(item, n);
                            // v0.7.0 馈赠改掉落物发放（不再直接塞入玩家背包）：
                            // 在击杀者脚边生成真实物品实体，由玩家自己走近拾取（自动吸入背包）。
                            // 根治"直接入包路径下提示文本显示空气"的问题——掉落物形式文本永远携带真实物品名；
                            // 背包满时物品自然留在原地等待拾取，不再丢失。
                            net.minecraft.world.entity.item.ItemEntity ent = new net.minecraft.world.entity.item.ItemEntity(
                                    killer.level(), killer.getX(), killer.getY() + 0.5, killer.getZ(), st);
                            ent.setPickUpDelay(10); // 短暂拾取延迟，防止与击杀同帧重复叠加
                            ent.setNoGravity(false);
                            killer.level().addFreshEntity(ent);
                            // v0.6.16 取证日志：记录实际物品注册 id 与 hoverName（再遇"空气"可从日志精确定位）
                            try {
                                net.minecraft.resources.ResourceLocation rl2 = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item);
                                LOGGER.info("[Hexcraft][Gift] dropped {} x{} near {} (hoverName='{}')",
                                        rl2 == null ? "?" : rl2.toString(), n, killer.getName().getString(),
                                        st.getHoverName().getString());
                            } catch (Exception ignored) { }
                            // v0.7.0 提示走滚动通道（gift 蓝），带实际物品名与数量 + 掉落拾取说明
                            // v0.7.5 物品名改纯文本参数：hoverName 组件序列化后客户端 fromJson 偶发失败
                            // （mod 物品自定义 chat 类型）→ 整条提示静默丢失（"馈赠文本没了"根因候选）；
                            // 用服务端已翻译的纯文本字符串参数，彻底规避序列化/反序列化问题。
                            net.minecraft.network.chat.MutableComponent tipMsg = net.minecraft.network.chat.Component.translatable(
                                    "message.hexcraft.random_loot",
                                    net.minecraft.network.chat.Component.literal(st.getHoverName().getString()), n);
                            tipMsg.append(net.minecraft.network.chat.Component.translatable("message.hexcraft.random_loot_dropped"));
                            com.hexcraft.network.Packets.S2CTipMessage.sendTo(killer, tipMsg, 2); // 2=馈赠(gift)蓝
                        }
                    }
                }
            }

            LAST_KILL_STREAK.put(killer.getUUID(), System.currentTimeMillis());
            sync(killer);
        }
    }

    /** 随机馈赠全物品池：ForgeRegistries.ITEMS 全量（原版+全部模组物品），过滤空气与技术物品，懒加载缓存。
     *  v0.6.5 低价值过滤开关（服务端配置 randomGiftFilterLow）：开启后额外过滤原版建材装饰类低价值物品；
     *  池缓存带配置状态，改配置后下次击杀自动重建（不重启生效）。 */
    private static java.util.List<net.minecraft.world.item.Item> GIFT_POOL;
    private static boolean GIFT_POOL_LOW_FILTER;

    /** v0.6.5 原版低价值建材/装饰类 id 集合（仅判定 minecraft 命名空间；模组物品价值无法判定，一律不主动过滤） */
    private static final java.util.Set<String> LOW_VALUE_IDS = new java.util.HashSet<>();

    static {
        LOW_VALUE_IDS.addAll(java.util.List.of(
                "dirt", "grass_block", "sand", "red_sand", "gravel", "cobblestone", "stone", "granite", "diorite", "andesite",
                "deepslate", "tuff", "cobbled_deepslate", "sandstone", "red_sandstone", "clay", "terracotta", "glass", "glass_pane",
                "grass", "tall_grass", "fern", "large_fern", "dead_bush", "vine", "cobweb", "snow_block", "mud", "dripstone_block",
                "poppy", "dandelion", "azure_bluet", "allium", "oxeye_daisy", "cornflower", "lily_of_the_valley", "wither_rose",
                "blue_orchid", "sunflower", "lilac", "rose_bush", "peony", "smooth_stone", "smooth_sandstone", "smooth_quartz",
                "bone_block", "calcite", "end_stone", "purpur_block", "prismarine", "dark_prismarine", "prismarine_bricks",
                "sea_lantern", "glowstone", "sea_grass", "kelp", "bamboo", "bamboo_block", "sugar_cane"
        ));
        for (String w : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "crimson", "warped"}) {
            LOW_VALUE_IDS.add(w + "_planks"); LOW_VALUE_IDS.add(w + "_log"); LOW_VALUE_IDS.add(w + "_leaves"); LOW_VALUE_IDS.add(w + "_sapling");
        }
        for (String c : new String[]{"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"}) {
            LOW_VALUE_IDS.add(c + "_wool"); LOW_VALUE_IDS.add(c + "_carpet"); LOW_VALUE_IDS.add(c + "_terracotta");
            LOW_VALUE_IDS.add(c + "_glazed_terracotta"); LOW_VALUE_IDS.add(c + "_concrete"); LOW_VALUE_IDS.add(c + "_concrete_powder");
            LOW_VALUE_IDS.add(c + "_glass"); LOW_VALUE_IDS.add(c + "_glass_pane"); LOW_VALUE_IDS.add(c + "_stained_glass");
        }
    }

    private static java.util.List<net.minecraft.world.item.Item> randomGiftPool() {
        boolean lowFilter = com.hexcraft.config.HexConfig.RANDOM_GIFT_FILTER_LOW.get();
        if (GIFT_POOL == null || GIFT_POOL_LOW_FILTER != lowFilter) {
            java.util.List<net.minecraft.world.item.Item> pool = new java.util.ArrayList<>();
            for (net.minecraft.world.item.Item it : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues()) {
                if (it == null || it == net.minecraft.world.item.Items.AIR) continue;
                net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(it);
                if (key == null) continue;
                String p = key.getPath();
                // 过滤技术/调试类物品：屏障、命令方块、结构方块/虚空、拼图方块、调试棒、知识书、刷怪笼等
                if (p.contains("barrier") || p.contains("command_block") || p.contains("structure_block")
                        || p.contains("structure_void") || p.contains("jigsaw") || p.contains("debug_stick")
                        || p.contains("knowledge_book") || p.contains("spawner")) continue;
                // v0.6.16 空气根因修复：部分 mod 会注册自己的占位空气物品（如 xxx:air），其注册路径
                // 是 "air" 但实例 ≠ Items.AIR，引用相等过滤不掉 → 玩家抽到后显示/掉落"空气"。
                // v0.6.19 收紧：注册路径中任意位置含 "air" 的一律过滤（覆盖 xxx:air / flowing_air / empty 等全部占位变体）
                if (p.contains("air") || p.contains("empty")) continue;
                // v0.6.16 双保险：个别 mod 占位物品注册路径不含 air 但 hoverName 写死"空气/Air"，
                // 服务端 getString()（英文环境）等于 Air 的一并过滤
                try {
                    String hover = new net.minecraft.world.item.ItemStack(it).getHoverName().getString();
                    if (hover != null && (hover.equalsIgnoreCase("air") || hover.equals("空气"))) continue;
                } catch (Exception ignored) { }
                // v0.6.5 低价值过滤开关：仅判定原版建材装饰类（模组物品不过滤）
                if (lowFilter && "minecraft".equals(key.getNamespace()) && LOW_VALUE_IDS.contains(p)) continue;
                pool.add(it);
            }
            GIFT_POOL = pool;
            GIFT_POOL_LOW_FILTER = lowFilter;
        }
        return GIFT_POOL;
    }

    /** v0.6.19 空气类判定（随机馈赠专用）：实例为 AIR、注册路径含 air/empty、hoverName 含 air（不区分大小写）
     *  任一命中即视为空气占位（v0.6.19 加宽：hoverName 从精确等于"Air"放宽为包含，覆盖 "xx air" 等变体） */
    private static boolean isAirLike(net.minecraft.world.item.Item it) {
        if (it == null || it == net.minecraft.world.item.Items.AIR) return true;
        try {
            net.minecraft.resources.ResourceLocation k = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(it);
            if (k != null) {
                String p = k.getPath();
                if (p.contains("air") || p.contains("empty")) return true;
            }
            String hn = new net.minecraft.world.item.ItemStack(it).getHoverName().getString();
            if (hn != null && hn.toLowerCase(java.util.Locale.ROOT).contains("air")) return true;
        } catch (Exception ignored) { }
        return false;
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

    // ================= v0.8.0 维度专属海克斯：仅对应维度生效 =================

    /** 是否在末地（任意层级：主世界存档的子末地也算末地维度） */
    private static boolean inEnd(net.minecraft.world.entity.Entity e) {
        return e != null && e.level() != null && e.level().dimension().equals(net.minecraft.world.level.Level.END);
    }

    /** 是否在下界 */
    private static boolean inNether(net.minecraft.world.entity.Entity e) {
        return e != null && e.level() != null && e.level().dimension().equals(net.minecraft.world.level.Level.NETHER);
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
        if (MULTI_BUSY.contains(sp.getUUID())) return; // v0.6.16 多重射击的额外伤害不再递归结算机制
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        LivingEntity target = event.getEntity();
        float amount = event.getAmount();
        if (amount <= 0) return;

        // v0.4.7 伤害机制分层（参考 LOL 海克斯大乱斗）：
        // ① 反伤（荆棘/反伤锻造/反击）默认是"纯反伤"——只反弹伤害，不触发任何攻击特效
        //   （吸血 / 暴击 / 增伤 / 锻造词条 / 附魔 / 叠层 / 命中回粮等一律不生效）；
        // ② 仅当玩家持有棱彩海克斯「万伤皆刃」(all_damage_proc) 时，归于玩家的所有伤害
        //   （含反伤）都能触发全部攻击特效——叠层、吸血、词条照常结算。
        boolean thornsHit = event.getSource().getMsgId().contains("thorns");
        if (thornsHit && effect(data, "all_damage_proc") <= 0) return;

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
        // v0.8.0 维度专属海克斯：末地珍珠掌控暴击率并入常规暴击判定
        if (inEnd(sp)) crit += effect(data, "end_crit");
        if (crit > 0 && sp.getRandom().nextFloat() < (float) crit) {
            dmgMul *= 1.5f * (float) (1 + effect(data, "crit_damage")); // 重磅一击：暴击伤害提升
        }
        // v0.6.16 风暴专注：连续命中 4 击后第 4 击必定暴击（确定性连击，区别于概率暴击）
        double comboCrit = effect(data, "combo_crit");
        if (comboCrit > 0) {
            int cc = COMBO_COUNT.merge(sp.getUUID(), 1, Integer::sum);
            if (cc >= 4) {
                COMBO_COUNT.put(sp.getUUID(), 0);
                dmgMul *= 1.5f * (float) (1 + effect(data, "crit_damage"));
            }
        } else {
            COMBO_COUNT.remove(sp.getUUID());
        }

        // 攻击方式分类：近战 / TACZ 弹射物 / 其它弹射物
        net.minecraft.world.entity.Entity direct = event.getSource().getDirectEntity();
        boolean melee = direct == sp;
        boolean tacz = !melee && direct != null && direct.getClass().getName().contains("tacz");
        boolean ranged = !melee && !tacz && direct != null;
        if (melee) dmgMul *= (float) (1 + effect(data, "melee_power"));
        if (ranged) dmgMul *= (float) (1 + effect(data, "ranged_power"));
        // v0.8.0 维度专属海克斯：末地/下界攻击伤害提升（仅对应维度生效）
        if (inEnd(sp)) dmgMul *= (float) (1 + effect(data, "end_damage"));
        else if (inNether(sp)) dmgMul *= (float) (1 + effect(data, "nether_damage"));
        if (tacz) {
            dmgMul *= (float) (1 + effect(data, "tacz_power"));
            double tc = effect(data, "tacz_crit");
            // v0.6.16 暴击链：暴击后 3 秒窗内暴击率提升（连续暴击窗口，区别于命中窗/操作窗）
            double critChain = effect(data, "tacz_crit_chain");
            double chainBonus = 0;
            if (critChain > 0 && System.currentTimeMillis() < CRIT_CHAIN_UNTIL.getOrDefault(sp.getUUID(), 0L)) {
                chainBonus = critChain;
            }
            boolean isCrit = (tc + chainBonus) > 0 && sp.getRandom().nextFloat() < (float) (tc + chainBonus);
            // v0.6.16 末弹必暴：弹匣剩 1 发时命中必定暴击（与绝地弹匣 +40% 叠加成"最后一发必暴爆发"）
            double lastCrit = effect(data, "tacz_last_crit");
            if (lastCrit > 0 && currentTaczAmmo(sp) == 1) isCrit = true;
            if (isCrit) {
                dmgMul *= 1.5f;
                if (critChain > 0) CRIT_CHAIN_UNTIL.put(sp.getUUID(), System.currentTimeMillis() + 3000);
            }
            // v0.6.16 棱彩双发射击：暴击时必定额外造成一次同额伤害（棱彩级双段爆发；与白银多重射击的
            // 几率双段区分——本机制是暴击联动，防递归同 MULTI_BUSY；v0.6.16 修正传实际同额伤害 amount*dmgMul）
            double doubleShot = effect(data, "tacz_double_shot");
            if (isCrit && doubleShot > 0 && target != null && target.isAlive()
                    && !MULTI_BUSY.contains(sp.getUUID())) {
                MULTI_BUSY.add(sp.getUUID());
                try {
                    if (target.isAlive()) target.hurt(event.getSource(), amount * dmgMul);
                } catch (Exception ignored) { }
                MULTI_BUSY.remove(sp.getUUID());
            }
            double twin = effect(data, "twin_shot"); // 双子射击：几率双倍枪伤
            if (twin > 0 && sp.getRandom().nextFloat() < (float) twin) dmgMul *= 2;
            // v0.6.0 机制海克斯化：枪械之刃/枪械暴击已并入 tacz_power/tacz_crit 海克斯（原 gun_aspect/gun_crit_aspect 锻造词条迁出）
            if (target != null) {
                double dist = sp.distanceTo(target);
                if (dist < 5) dmgMul *= (float) (1 + effect(data, "tacz_close_power"));
                else if (dist > 15) dmgMul *= (float) (1 + effect(data, "tacz_far_power"));
            }
            // v0.6.16 绝地弹匣：弹匣剩余 1 发时该发伤害大增（读主手枪械当前弹药）
            double lastShot = effect(data, "tacz_last_shot");
            if (lastShot > 0 && currentTaczAmmo(sp) == 1) dmgMul *= (float) (1 + lastShot);
            // v0.6.16 稳定握持：TACZ 命中后 4 秒窗内枪伤加成（持续命中刷新；窗从首次命中起算）
            double steady = effect(data, "tacz_steady");
            if (steady > 0) {
                long now = System.currentTimeMillis();
                Long su = STEADY_UNTIL.get(sp.getUUID());
                if (su != null && now < su) dmgMul *= (float) (1 + steady);
                STEADY_UNTIL.put(sp.getUUID(), now + 4000);
            } else {
                STEADY_UNTIL.remove(sp.getUUID());
            }
            // v0.6.16 卧姿射击：蹲伏时枪伤（姿态条件，区别于距离/击杀/命中类机制）
            double crouch = effect(data, "tacz_crouch");
            if (crouch > 0 && sp.isCrouching()) dmgMul *= (float) (1 + crouch);
            // v0.6.16 凌空射击：未落地时枪伤
            double airborne = effect(data, "tacz_airborne");
            if (airborne > 0 && !sp.onGround()) dmgMul *= (float) (1 + airborne);
            // v0.6.16 架枪射击：原地不动 ≥1 秒后枪伤大增（移动即重置）
            double bipod = effect(data, "tacz_bipod");
            if (bipod > 0 && System.currentTimeMillis() - LAST_MOVE.getOrDefault(sp.getUUID(), 0L) >= 1000L) {
                dmgMul *= (float) (1 + bipod);
            }
            // v0.6.16 低弹增压：当前弹药 ≤ 弹容 30% 时枪伤大增（弹药压力型，区别于绝地弹匣的仅剩 1 发）
            double lowAmmo = effect(data, "tacz_low_ammo");
            if (lowAmmo > 0) {
                ItemStack gun = sp.getMainHandItem();
                if (!gun.isEmpty() && gun.hasTag()) {
                    int[] ammo = findAmmoInt(gun.getTag(), 0);
                    if (ammo != null && ammo[1] > 0) {
                        int cur = sanitizeAmmo(ammo[1]);
                        int max = taczMaxAmmo(sp, gun.getTag());
                        if (max > 0 && cur * 10 <= max * 3) dmgMul *= (float) (1 + lowAmmo);
                    }
                }
            }
            // v0.6.16 弹匣虹吸：命中时有几率回复 1 发弹药（命中回弹，区别于击杀回弹；单发少量不破坏弹药平衡）
            double siphon = effect(data, "tacz_siphon");
            if (siphon > 0 && sp.getRandom().nextFloat() < (float) siphon) {
                addOneTaczAmmo(sp);
            }
            // v0.6.16 快速装填：换弹完成后 4 秒窗内枪伤大增（操作窗，区别于命中/击杀窗）
            double reloadHaste = effect(data, "tacz_reload_haste");
            if (reloadHaste > 0 && System.currentTimeMillis() < RELOAD_UNTIL.getOrDefault(sp.getUUID(), 0L)) {
                dmgMul *= (float) (1 + reloadHaste);
            }
            // v0.6.16 切枪强化：切换主手武器后 3 秒窗内枪伤大增
            double swapStrike = effect(data, "tacz_swap_strike");
            if (swapStrike > 0 && System.currentTimeMillis() < SWAP_UNTIL.getOrDefault(sp.getUUID(), 0L)) {
                dmgMul *= (float) (1 + swapStrike);
            }
            // v0.6.16 满弹增压：弹匣全满时枪伤大增（与低弹增压形成弹药经济两极）
            double fullAmmo = effect(data, "tacz_full_ammo");
            if (fullAmmo > 0) {
                ItemStack gun = sp.getMainHandItem();
                if (!gun.isEmpty() && gun.hasTag()) {
                    int[] ammo = findAmmoInt(gun.getTag(), 0);
                    if (ammo != null && ammo[1] > 0) {
                        int cur = sanitizeAmmo(ammo[1]);
                        int max = taczMaxAmmo(sp, gun.getTag());
                        if (max > 0 && cur >= max) dmgMul *= (float) (1 + fullAmmo);
                    }
                }
            }
            // v0.6.16 走射专精：1 秒内移动过时枪伤大增（与架枪站桩形成姿态两极，共用位置跟踪）
            double strafe = effect(data, "tacz_strafe");
            if (strafe > 0 && System.currentTimeMillis() - LAST_MOVE.getOrDefault(sp.getUUID(), 0L) < 1000L) {
                dmgMul *= (float) (1 + strafe);
            }
            // v0.6.16 双枪大师：主副手都持 TACZ 枪械时主手枪伤大增
            double dual = effect(data, "tacz_dual_wield");
            if (dual > 0) {
                ItemStack off = sp.getOffhandItem();
                if (!off.isEmpty()) {
                    ResourceLocation offRl = ForgeRegistries.ITEMS.getKey(off.getItem());
                    if (offRl != null && "tacz".equals(offRl.getNamespace())) dmgMul *= (float) (1 + dual);
                }
            }
            // v0.6.16 致盲弹：命中时几率使目标失明 3 秒（Debuff 维度，区别于减速/点燃/流血）
            double blind = effect(data, "tacz_blind");
            if (blind > 0 && target != null && sp.getRandom().nextFloat() < (float) blind) {
                target.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 60, 0));
            }
            // v0.6.16 持续射击：持续射击（连续命中）期间每命中一枪叠 1 层，每层枪伤递增（叠的是"连续射击层数"，
            // 停止射击 1.5 秒后清零重来；上限 15 层）
            double barrage = effect(data, "tacz_barrage");
            if (barrage > 0) {
                long nowB = System.currentTimeMillis();
                Integer hits = BAR_HITS.get(sp.getUUID());
                long lastHit = BAR_LAST_HIT.getOrDefault(sp.getUUID(), 0L);
                hits = (nowB - lastHit < 1500) ? (hits == null ? 1 : Math.min(hits + 1, 15)) : 1;
                BAR_HITS.put(sp.getUUID(), hits);
                BAR_LAST_HIT.put(sp.getUUID(), nowB);
                dmgMul *= (float) (1 + barrage * hits);
            }
            // v0.6.16 棱彩追踪：命中同一目标叠加印记（每层该目标枪伤 +10%/级，上限 5 层；换目标或 5 秒未命中重置）
            double tracking = effect(data, "tacz_tracking");
            if (tracking > 0 && target != null) {
                long nowT = System.currentTimeMillis();
                Map<UUID, long[]> tr = TACZ_TRACK.computeIfAbsent(sp.getUUID(), k -> new ConcurrentHashMap<>());
                long[] st = tr.get(target.getUUID());
                int tkHits = 1;
                if (st != null && nowT - st[1] < 5000) tkHits = Math.min((int) st[0] + 1, 5);
                tr.put(target.getUUID(), new long[]{tkHits, nowT});
                dmgMul *= (float) (1 + tracking * tkHits);
            }
            // v0.6.16 穿甲弹：目标护甲越高枪伤越高（护甲 50 点达满额 +30%/级，护甲 0 无加成）
            double pierce = effect(data, "tacz_pierce");
            if (pierce > 0 && target != null) {
                double armor = 0;
                net.minecraft.world.entity.ai.attributes.AttributeInstance ai = target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
                if (ai != null) armor = Math.max(0, ai.getValue());
                dmgMul *= (float) (1 + Math.min(armor / 50.0, 1.0) * pierce);
            }
        }
        // 成长性全伤害加成
        dmgMul *= (float) (1 + effect(data, "stack_growth"));
        // 疾风剑客：移速转伤
        double mobi = effect(data, "mobi_power");
        if (mobi > 0) {
            AttributeInstance spd = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
            if (spd != null) { // v0.5.0 判空防御
                double speed = spd.getValue();
                dmgMul *= (float) (1 + mobi * speed * 10);
            }
        }
        // 刽子手（低血斩杀）/ 屠龙者（精英首领增伤）/ 附魔增伤（亡灵杀手/节肢杀手）
        if (target != null) {
            double tRatio = target.getHealth() / Math.max(1f, target.getMaxHealth());
            if (tRatio < HexConfig.EXECUTE_THRESHOLD.get().floatValue()) dmgMul *= (float) (1 + effect(data, "execute"));
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
        // 世界海克斯独立乘区：全体玩家造成伤害倍率 + 对中立生物伤害倍率（独立于玩家海克斯，不与其它 mod 冲突）
        if (sp.level() instanceof net.minecraft.server.level.ServerLevel whLevel) {
            double whDmg = com.hexcraft.event.WorldHexEvents.playerDamageMult(whLevel);
            if (whDmg != 0) dmgMul *= (float) (1 + whDmg);
            if (target != null && com.hexcraft.event.WorldHexEvents.isNeutral(target)) {
                double whVsNeutral = com.hexcraft.event.WorldHexEvents.playerDmgVsNeutral(whLevel);
                if (whVsNeutral != 0) dmgMul *= (float) (1 + whVsNeutral);
            }
        }
        // v0.6.16 以血还血：受击后 5 秒内下一次攻击伤害大增（触发一次即消耗）
        double vg = effect(data, "vengeance");
        if (vg > 0) {
            Long until = VENGE_UNTIL.get(sp.getUUID());
            if (until != null && System.currentTimeMillis() < until) {
                dmgMul *= (float) (1 + vg);
                VENGE_UNTIL.remove(sp.getUUID());
            }
        }
        if (dmgMul != 1.0f) event.setAmount(amount * dmgMul);

        // v0.6.16 多重射击：命中时几率额外造成一次同额伤害（多段打击；防递归：额外伤害事件入口直接短路，
        // 不再套整套机制倍率；v0.6.16 修正传 event.getAmount() 实际伤害而非倍率）
        double multi = effect(data, "tacz_multi_shot");
        if (multi > 0 && target != null && target.isAlive() && !MULTI_BUSY.contains(sp.getUUID())
                && sp.getRandom().nextFloat() < (float) multi) {
            MULTI_BUSY.add(sp.getUUID());
            try {
                if (target.isAlive()) target.hurt(event.getSource(), event.getAmount());
            } catch (Exception ignored) { }
            MULTI_BUSY.remove(sp.getUUID());
        }

        // v0.6.0 机制海克斯化：原锻造机制词条（fire/frost/poison/leech/pierce/crit/execute/splash/lightning/
        // stack/dance/bloodlust/reaper/thorn/gold/regen/TACZ）全部迁入海克斯池（锻造器仅剩静态属性）。
        // 已有海克斯机制键覆盖的（frost_bite/lifesteal/crit/execute/splash/gold_punch/regen/tacz_power 等）不再保留锻造版本；
        // 新机制海克斯（fire_blade/poison_blade/pierce_blade/lightning_blade/dance_blade/bloodlust_blade/reaper_blade/crescendo）在本段结算。
        if (target != null && target.isAlive()) {
            // 叠角龙在场时命中叠层翻倍（每级 +75%，v0.6.16 统一倍率；受击叠层同样生效）
            boolean onHitDrake = data.getHexes().stream().anyMatch(e -> e.id.equals("hex_stack_drake"));
            double onHitMult = drakeMult(data);
            boolean attrStacked = false;
            // 火淬之刃：点燃 2 秒 + 燃烧额外伤害（等级加深）
            double fireBlade = effect(data, "fire_blade");
            if (fireBlade > 0) {
                target.setSecondsOnFire(2);
                if (target.isOnFire()) extraDamage(sp, target, amount * 0.04f * (float) fireBlade);
            }
            // 积聚之刃每层伤害倍率（共振彩蛋与通用叠层结算共用）
            double crescendoMult = effect(data, "crescendo");
            // 剧毒之刃：中毒（等级加深）
            double poisonBlade = effect(data, "poison_blade");
            if (poisonBlade > 0) applyTo(target, MobEffects.POISON, 3 * 20, Math.max(0, (int) poisonBlade - 1));
            // 破甲之刃：无视护甲真伤（等级加深）
            double pierceBlade = effect(data, "pierce_blade");
            if (pierceBlade > 0) extraDamage(sp, target, amount * 0.03f * (float) pierceBlade);
            // 雷电之刃：15%/级 几率连锁闪电劈击附近敌人（30% 伤害）
            double lightningBlade = effect(data, "lightning_blade");
            if (lightningBlade > 0 && sp.getRandom().nextFloat() < Math.min(1f, 0.15f * (float) lightningBlade)) {
                chainLightning(sp, target, amount * 0.30f, 2.5);
            }
            // 刀锋之舞：攻速溢出部分按 8%/级 转化为额外伤害
            double danceBlade = effect(data, "dance_blade");
            if (danceBlade > 0) {
                AttributeInstance asi = sp.getAttribute(Attributes.ATTACK_SPEED);
                double aspd = asi == null ? 0 : asi.getValue();
                if (aspd > 1.0) extraDamage(sp, target, amount * 0.08f * (float) danceBlade * (float) (aspd - 1.0));
            }
            // 血怒之刃：半血以下吸血 2%/级 并增伤 6%/级
            double bloodlustBlade = effect(data, "bloodlust_blade");
            if (bloodlustBlade > 0 && sp.getHealth() < sp.getMaxHealth() * 0.5f) {
                sp.heal(amount * 0.02f * (float) bloodlustBlade);
                extraDamage(sp, target, amount * 0.06f * (float) bloodlustBlade);
            }
            // 收割之刃：对低于 35% 生命的目标造成 30%/级 额外真实伤害
            double reaperBlade = effect(data, "reaper_blade");
            if (reaperBlade > 0) {
                float tr = target.getHealth() / Math.max(1f, target.getMaxHealth());
                if (tr < 0.35f) extraDamage(sp, target, amount * 0.3f * (float) reaperBlade);
            }
            // v0.8.8 百分比削血（execute_pct）：命中按目标最大生命% + 当前生命% 追加伤害（不直接斩杀，
            // 高血怪按比例削血、低血怪由斩杀/普通伤害补刀；两部分权重进配置 EXECUTE_PCT_MAX/Cur_WEIGHT）
            double execPct = effect(data, "execute_pct");
            if (execPct > 0) {
                float pctDmg = (float) (target.getMaxHealth() * execPct * HexConfig.EXECUTE_PCT_MAX_WEIGHT.get()
                        + target.getHealth() * execPct * HexConfig.EXECUTE_PCT_CUR_WEIGHT.get());
                if (pctDmg > 0) extraDamage(sp, target, pctDmg);
            }
            // v0.6.0 通用叠层管理器（JSON 驱动）：命中叠层海克斯（def.stack，onKill=false）命中即叠 1 层，
            // 迅战之角（stack_gain 叠速 / stack_cap 上限乘区）与世界海克斯「厚积战场」（叠层上限 +50%）自动生效；
            // 每层伤害加成 = event 中与 stack.key 同名的键值 × 等级
            // 积聚之刃（crescendo）：棱彩 200 层上限、脱战 6 秒后每 tick 衰减 2 层、每层额外伤害 +1%×等级
            boolean hexStacked = false;
            double whCapBonus = sp.level() instanceof net.minecraft.server.level.ServerLevel whLvl2
                    ? com.hexcraft.event.WorldHexEvents.combatStackCapBonus(whLvl2) : 0;
            double whPermGain = sp.level() instanceof net.minecraft.server.level.ServerLevel whLvl3
                    ? com.hexcraft.event.WorldHexEvents.permStackGainBonus(whLvl3) : 0;
            for (PlayerData.HexEntry e : data.getHexes()) {
                HextechDefinition sdef = HextechRegistry.get(e.id);
                if (sdef == null || sdef.stack == null) continue;
                // v0.6.2 onKill=true 的叠层（击杀触发）：命中时不获取层数（击杀时获取），但仍按层数结算每层伤害
                boolean killStack = sdef.stack.onKill;
                if (!killStack) {
                    if (data.getStack(sdef.stack.key) == null) {
                        data.putStack(new PlayerData.StackEntry(sdef.stack.key, sdef.stack.cap, sdef.stack.combat, sdef.stack.decayDelay, sdef.stack.decayRate));
                    }
                    data.addStack(sdef.stack.key, 1 + effect(data, "stack_gain"), 1 + effect(data, "stack_cap") + whCapBonus);
                }
                double perStack = 0;
                for (String token : sdef.event.split(",")) {
                    int ci = token.indexOf(':');
                    if (ci > 0 && token.substring(0, ci).equals(sdef.stack.key)) {
                        try { perStack += Double.parseDouble(token.substring(ci + 1)) * e.level; } catch (Exception ignored) { }
                    }
                }
                PlayerData.StackEntry s = data.getStack(sdef.stack.key);
                if (perStack > 0 && s != null && s.value > 0) {
                    extraDamage(sp, target, amount * (float) perStack * (float) s.value);
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.stack_named",
                            Component.translatable("hexcraft.hextech." + e.id + ".name"), (int) s.value), 0);
                }
                hexStacked = true;
            }
            // v0.6.0 世界海克斯「永续成长」：永久叠层获取速度 +50%（命中叠层概率提升 chance×(1+gain)）。
            // v0.6.16 修复：此前 whPermGain>0 时本段与下方命中叠层段重复执行（一次命中两次独立判定、可叠 2 层），
            // 已合并——本段不再单独叠层，概率加成统一并入下方 stack_attr_onhit 循环的 hitChance。
            // 共振彩蛋：积聚之刃 + 刀锋之舞 → 奔涌攻势（叠层期间攻速溢出额外伤害，层数越多越强，上限 2 倍）
            if (crescendoMult > 0 && danceBlade > 0) {
                AttributeInstance asi2 = sp.getAttribute(Attributes.ATTACK_SPEED);
                double aspd2 = asi2 == null ? 0 : asi2.getValue();
                PlayerData.StackEntry cs = data.getStack("crescendo");
                if (aspd2 > 1.0 && cs != null && cs.value > 0) {
                    extraDamage(sp, target, amount * 0.08f * (float) crescendoMult * (float) (aspd2 - 1.0)
                            * (float) Math.min(2.0, cs.value / 100.0));
                }
            }
            // ===== v0.6.2 实装预留事件：流血 / 减速 / 标记 / 处决（bleed_damage / slow_on_hit / mark_target / mark_execute）=====
            // 流血：命中施加 DoT（每 tick 伤害 = 本次伤害 × 倍率×等级 × 0.1，持续 5 秒；独立实例不互相覆盖，多次命中叠加多个实例）
            double bleedHit = effect(data, "bleed_damage");
            if (bleedHit > 0) {
                applyBleed(target, amount * (float) bleedHit * 0.1f, 100);
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.bleed_apply"), 0);
            }
            // 减速：命中施加减速（持续 4 秒，等级加深最高 3 级）
            double slowHit = effect(data, "slow_on_hit");
            if (slowHit > 0) {
                int amp = Math.min(3, Math.max(0, (int) Math.floor(slowHit * 4)));
                applyTo(target, MobEffects.MOVEMENT_SLOWDOWN, 80, amp);
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.slow_hit", amp + 1), 0);
            }
            // 处决者（mark_execute）：命中自动标记目标（3.5 秒）；目标生命 ≤ 20% 最大生命时立即处决（最大生命 × 35% 魔法伤害，不触发反伤/不递归）
            double exec = effect(data, "mark_execute");
            if (exec > 0) {
                applyMark(sp, target);
                float thr = target.getMaxHealth() * 0.20f;
                if (target.getHealth() <= thr) {
                    target.hurt(sp.damageSources().magic(), target.getMaxHealth() * 0.35f);
                    MARKS.remove(target.getUUID());
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.mark_execute"), 0);
                } else {
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.mark_target"), 0);
                }
            }
            // 猎手标记（mark_target）：命中概率标记目标（3.5 秒；配合处决者消耗）
            double markT = effect(data, "mark_target");
            if (markT > 0 && sp.getRandom().nextFloat() < (float) Math.min(1.0, markT)) {
                applyMark(sp, target);
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.mark_target"), 0);
            }
            // 命中刷新战斗叠层脱战计时
            data.refreshCombatStacks();
            if (hexStacked) sync(sp); // 叠层变化实时同步客户端显示
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
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.gold_punch"), 0);
            }
            // 心之钢等：攻击命中概率叠永久属性层（stack_attr_onhit:键:倍率，25% 几率）；叠角龙在场时叠层翻倍——提示带海克斯名称
            // v0.4.3 修复：荆棘/反伤伤害不再无脑触发叠层；v0.4.7 起：反伤默认在 onDamageDealt 开头已短路（纯反伤），
            // 持有「万伤皆刃」棱彩海克斯时反伤也触发叠层（到达此处时 thorns 只可能是 all_damage_proc 放行或非反伤来源）
            for (PlayerData.HexEntry e : data.getHexes()) {
                if (e.id.equals("hex_stack_drake")) continue; // 叠角龙自身不被命中叠层
                HextechDefinition def = HextechRegistry.get(e.id);
                if (def != null && def.event != null && def.event.contains("stack_attr_onhit:")) {
                    // v0.6.16 永续成长概率加成并入此处（原独立循环已删除，杜绝双重判定）
                    float hitChance = Math.min(1f, stackOnHitChance(def) * (float) (1 + whPermGain));
                    if (sp.getRandom().nextFloat() < hitChance) {
                        e.stacks = safeStackAdd(e.stacks, gainWithMult(onHitMult, sp.getRandom()));
                        data.applyModifiers(sp);
                        com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.stack_named",
                                Component.translatable("hexcraft.hextech." + e.id + ".name"), e.stacks), 0);
                        attrStacked = true;
                    }
                }
            }
            // 可叠层装备（心之钢等装备的 stack_attr_onhit）：装备层数独立累计，chance 概率叠 1 层（叠角龙在场翻倍）——提示带装备名称
            for (PlayerData.EquipEntry eq : data.getEquipment()) {
                HextechDefinition def = HextechRegistry.getEquip(eq.id);
                if (def != null && def.event != null && def.event.contains("stack_attr_onhit:")) {
                    float hitChance = Math.min(1f, stackOnHitChance(def) * (float) (1 + whPermGain));
                    if (sp.getRandom().nextFloat() < hitChance) {
                        eq.stacks = safeStackAdd(eq.stacks, gainWithMult(onHitMult, sp.getRandom()));
                        data.applyModifiers(sp);
                        com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.equip_stack_named",
                                Component.translatable("hexcraft.equip." + eq.id + ".name"), eq.stacks), 0);
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
        // v0.6.16 以血还血：持有者受击开启 5 秒复仇窗口（下一次攻击结算时消费）
        if (effect(data, "vengeance") > 0) {
            VENGE_UNTIL.put(sp.getUUID(), System.currentTimeMillis() + 5000);
        }
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
        // v0.8.0 维度专属海克斯：末地摔落/虚空减伤、下界火焰/岩浆减伤（仅对应维度生效，独立乘区）
        if (inEnd(sp)) {
            double fallRed = effect(data, "end_fall_reduce");
            if (fallRed > 0 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
                event.setAmount((float) (event.getAmount() * (1 - Math.min(0.95, fallRed))));
            }
            double voidRed = effect(data, "end_void_reduce");
            if (voidRed > 0 && event.getSource().getMsgId().contains("outOfWorld")) {
                event.setAmount((float) (event.getAmount() * (1 - Math.min(0.95, voidRed))));
            }
        } else if (inNether(sp)) {
            double lavaRed = effect(data, "nether_lava_reduce");
            if (lavaRed > 0 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                    && event.getSource().getMsgId().contains("lava")) {
                event.setAmount((float) (event.getAmount() * (1 - Math.min(0.95, lavaRed))));
            }
            double nFireRed = effect(data, "nether_fire_reduce");
            if (nFireRed > 0 && event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                    && !event.getSource().getMsgId().contains("lava")) {
                event.setAmount((float) (event.getAmount() * (1 - Math.min(0.95, nFireRed))));
            }
        }
        // v0.6.0 额外护甲减伤（独立乘区，突破原版护甲减伤天花板）：护甲超过阈值后每点再减 X%（上限 Y%）
        // 原版 1.20.1 护甲公式在 LivingHurtEvent 之后结算，此处乘区与之相乘 → 高护甲无上限锻造仍有收益
        if (com.hexcraft.config.HexConfig.EXTRA_ARMOR_CAP.get() > 0) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance armorAttr = sp.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            if (armorAttr != null) {
                double armorVal = armorAttr.getValue();
                double threshold = com.hexcraft.config.HexConfig.EXTRA_ARMOR_THRESHOLD.get();
                if (armorVal > threshold) {
                    double red = Math.min(com.hexcraft.config.HexConfig.EXTRA_ARMOR_CAP.get() * 0.01,
                            (armorVal - threshold) * com.hexcraft.config.HexConfig.EXTRA_ARMOR_PER_POINT.get() * 0.01);
                    if (red > 0) event.setAmount((float) (event.getAmount() * (1 - red)));
                }
            }
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
            com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.death_defy"), 0);
            return;
        }
        // 应急护盾：低血时维持吸收护盾
        double lowShield = effect(data, "shield_low");
        if (lowShield > 0 && sp.getHealth() / Math.max(1f, sp.getMaxHealth()) < 0.35f
                && sp.getAbsorptionAmount() < (float) lowShield) {
            sp.setAbsorptionAmount((float) lowShield);
        }
        // ===== v0.6.2 受击叠层（on_hurt_stack：event 含 hurt_gain 键的海克斯，受击叠 1 层）=====
        boolean hurtStacked = false;
        double whCapBonusHurt = sp.level() instanceof net.minecraft.server.level.ServerLevel whLv2
                ? com.hexcraft.event.WorldHexEvents.combatStackCapBonus(whLv2) : 0;
        for (PlayerData.HexEntry e : data.getHexes()) {
            if (e.id.equals("hex_stack_drake")) continue; // 叠角龙自身不受击叠层
            HextechDefinition sdef = HextechRegistry.get(e.id);
            if (sdef == null || sdef.stack == null || sdef.stack.onKill || !sdef.event.contains("hurt_gain")) continue;
            if (data.getStack(sdef.stack.key) == null) {
                data.putStack(new PlayerData.StackEntry(sdef.stack.key, sdef.stack.cap, sdef.stack.combat,
                        sdef.stack.decayDelay, sdef.stack.decayRate));
            }
            data.addStack(sdef.stack.key, 1 + effect(data, "stack_gain"), 1 + effect(data, "stack_cap") + whCapBonusHurt);
            PlayerData.StackEntry s = data.getStack(sdef.stack.key);
            if (s != null) {
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.stack_named",
                        Component.translatable("hexcraft.hextech." + e.id + ".name"), (int) s.value), 0);
            }
            hurtStacked = true;
        }
        // ===== v0.6.2 受击永久属性层（stack_attr_onhurt：受击概率叠永久属性层，死亡保留）=====
        // v0.6.16 双叠层修复：受击叠层写入独立 hurtStacks（不再与命中叠层共层）；叠角龙在场按每级 +75% 加成
        boolean attrHurtStacked = false;
        double hurtMult = drakeMult(data);
        for (PlayerData.HexEntry e : data.getHexes()) {
            if (e.id.equals("hex_stack_drake")) continue;
            HextechDefinition def = HextechRegistry.get(e.id);
            if (def != null && def.event != null && def.event.contains("stack_attr_onhurt:")) {
                if (sp.getRandom().nextFloat() < stackOnHitChance(def)) {
                    e.hurtStacks = safeStackAdd(e.hurtStacks, gainWithMult(hurtMult, sp.getRandom()));
                    data.applyModifiers(sp);
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.stack_named",
                            Component.translatable("hexcraft.hextech." + e.id + ".name"), e.hurtStacks), 0);
                    attrHurtStacked = true;
                }
            }
        }
        for (PlayerData.EquipEntry eq : data.getEquipment()) {
            HextechDefinition def = HextechRegistry.getEquip(eq.id);
            if (def != null && def.event != null && def.event.contains("stack_attr_onhurt:")) {
                if (sp.getRandom().nextFloat() < stackOnHitChance(def)) {
                    eq.stacks = safeStackAdd(eq.stacks, 1);
                    data.applyModifiers(sp);
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.equip_stack_named",
                            Component.translatable("hexcraft.equip." + eq.id + ".name"), eq.stacks), 0);
                    attrHurtStacked = true;
                }
            }
        }
        // ===== v0.6.2 荆棘点燃（thorns_fire）：被近战攻击概率点燃攻击者 4 秒 =====
        double thornFire = effect(data, "thorns_fire");
        if (thornFire > 0 && event.getSource().getDirectEntity() instanceof LivingEntity directAtk
                && directAtk == event.getSource().getEntity()
                && directAtk != sp && directAtk.isAlive()) {
            if (sp.getRandom().nextFloat() < (float) Math.min(1.0, thornFire)) {
                directAtk.setSecondsOnFire(4);
                com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.thorns_fire"), 0);
            }
        }
        if (hurtStacked || attrHurtStacked) sync(sp);
        // 荆棘反伤 + 反击风暴（v0.6.0 反伤之刃锻造词条迁出，已有 thorns/counter 海克斯覆盖反伤）
        double thorns = effect(data, "thorns");
        double counter = effect(data, "counter");
        if (thorns <= 0 && counter <= 0) return;
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && attacker != sp && attacker.isAlive()) {
            if (counter > 0 && sp.getRandom().nextFloat() < (float) counter) {
                attacker.hurt(sp.damageSources().thorns(sp), event.getAmount());
            }
            if (thorns > 0) {
                attacker.hurt(sp.damageSources().thorns(sp), (float) thorns);
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
     * 矿物系海克斯（v0.8.1 接入点重构）。
     * 1.20.1 Forge 无破坏后掉落事件（BlockDropItemEvent 为 NeoForge 才有），原实现「setCanceled + 手动破坏重算掉落」会
     * 打断第三方连锁挖矿 mod 的流程、且 getDrops 异常时掉落凭空消失。现改为：
     *   ① BreakEvent 中不取消原版破坏，仅登记掉落增强上下文；
     *   ② 掉落物实体加入世界（EntityJoinLevelEvent，与原版 popResource 同一调用栈）时按海克斯应用效果：
     *      时运翻倍 / 冶炼 / 探矿 / 磁铁拾取 / 挖矿经验 / 矿脉共鸣连锁。
     * 原版流程、第三方连锁、其它 mod 掉落全保留；getDrops 无结果时也由原版兜底，不再出现"挖掘不掉落物"。
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
        double vein = VEIN_BUSY ? 0 : effect(data, "mining_vein");
        if (fortune <= 0 && smelt <= 0 && mxp <= 0 && luck <= 0 && magnet <= 0 && vein <= 0) return;
        net.minecraft.world.level.block.state.BlockState state = event.getState();
        boolean ore = isOreBlock(state.getBlock());
        if (!ore && luck <= 0 && mxp <= 0) return; // 非矿物方块仅探矿/经验生效
        if (!(sp.level() instanceof net.minecraft.server.level.ServerLevel slevel)) return;
        MINING_CTX.put(event.getPos().asLong(), new MiningCtx(sp, state, ore,
                fortune, smelt, luck, magnet, mxp, vein, slevel.getGameTime()));
    }

    /** 矿物掉落增强上下文（v0.8.1）：BreakEvent 登记 → 掉落物 join 时消费；按世界 tick 判过期，防泄漏 */
    private static final class MiningCtx {
        final ServerPlayer player;
        final net.minecraft.world.level.block.state.BlockState state;
        final boolean ore;
        final double fortune, smelt, luck, magnet, mxp, vein;
        final long tick;
        boolean prospected; // 探矿之眼每方块仅触发一次
        boolean expDone;    // 挖矿经验每方块仅补发一次
        boolean veined;     // 矿脉共鸣每方块仅连锁一次
        MiningCtx(ServerPlayer player, net.minecraft.world.level.block.state.BlockState state, boolean ore,
                  double fortune, double smelt, double luck, double magnet, double mxp, double vein, long tick) {
            this.player = player;
            this.state = state;
            this.ore = ore;
            this.fortune = fortune;
            this.smelt = smelt;
            this.luck = luck;
            this.magnet = magnet;
            this.mxp = mxp;
            this.vein = vein;
            this.tick = tick;
        }
    }

    /** BreakEvent → 原版破坏 → 掉落物实体加入世界（同一调用栈）。登记表按 pos 关联，仅消费本次破坏生成的掉落。 */
    private static final Map<Long, MiningCtx> MINING_CTX = new ConcurrentHashMap<>();

    /** 矿物掉落增强：掉落物落地瞬间应用 时运翻倍/冶炼/探矿/磁铁/经验/矿脉共鸣（v0.8.1） */
    @SubscribeEvent
    public static void onItemEntityJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity ie)) return;
        if (!(ie.level() instanceof net.minecraft.server.level.ServerLevel slevel)) return;
        if (MINING_CTX.isEmpty()) return;
        long posKey = ie.blockPosition().asLong();
        MiningCtx ctx = MINING_CTX.get(posKey);
        if (ctx == null) return;
        long now = slevel.getGameTime();
        if (now - ctx.tick > 2) { // 过期：不是本次破坏的掉落（如玩家扔出），忽略并清理
            MINING_CTX.remove(posKey);
            return;
        }
        net.minecraft.world.item.ItemStack st = ie.getItem();
        if (st == null || st.isEmpty()) return;
        ServerPlayer sp = ctx.player;
        // 时运降临：矿物掉落翻倍（每件掉落均翻倍，防堆叠上限溢出）
        if (ctx.fortune > 0 && ctx.ore) {
            int mult = Math.max(1, (int) ctx.fortune);
            if (mult > 1) st.setCount(Math.min(st.getMaxStackSize(), st.getCount() * mult));
        }
        // 冶炼之触：原矿自动烧炼成锭（静态表快查，未命中走冶炼配方 → 兼容其它 mod 矿石）
        if (ctx.smelt > 0 && ctx.ore) {
            net.minecraft.world.item.Item smelted = SMELT_MAP.get(st.getItem());
            if (smelted == null) smelted = smeltByRecipe(slevel, st);
            if (smelted != null) ie.setItem(new net.minecraft.world.item.ItemStack(smelted, st.getCount()));
        }
        // 自动拾取：掉落优先存入存储 mod（超越维度→精妙背包→精致存储→背包），剩余留原地（v0.8.2）
        if (ctx.magnet > 0) {
            ItemStack remain = com.hexcraft.link.StorageLink.insert(sp, st);
            if (remain == null || remain.isEmpty()) {
                ie.discard();
            } else if (remain.getCount() < st.getCount()) {
                ie.setItem(remain); // 部分存入存储，剩余留在原地
            }
        }
        // 探矿之眼：挖普通方块几率额外掉贵重矿物（每方块一次）
        if (ctx.luck > 0 && !ctx.ore && !ctx.prospected) {
            ctx.prospected = true;
            if (sp.getRandom().nextFloat() < (float) ctx.luck) {
                net.minecraft.world.level.block.Block.popResource(slevel, ie.blockPosition(),
                        new net.minecraft.world.item.ItemStack(RARE_ORES[sp.getRandom().nextInt(RARE_ORES.length)], 1));
            }
        }
        // 挖矿经验：海克斯额外经验（每方块一次；原版矿物经验由原版流程正常生成）
        if (ctx.mxp > 0 && !ctx.expDone) {
            ctx.expDone = true;
            slevel.addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(slevel,
                    ie.getX(), ie.getY(), ie.getZ(), Math.max(1, (int) Math.floor(ctx.mxp))));
        }
        // 矿脉共鸣：主方块掉落落地时连锁开采周围同种矿物（最多 8 个）；子方块 BreakEvent 因 VEIN_BUSY 不再二次连锁
        if (ctx.vein > 0 && ctx.ore && !ctx.veined) {
            ctx.veined = true;
            chainVein(sp, slevel, ie.blockPosition(), ctx.state);
        }
    }

    /** 矿脉共鸣连锁：BFS 邻域同种矿物，逐个走原版破坏流程（每个子方块同样享受掉落增强，VEIN_BUSY 防二次连锁） */
    private static void chainVein(ServerPlayer sp, net.minecraft.server.level.ServerLevel slevel,
                                  net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
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
                    if (!slevel.getBlockState(cur).is(state.getBlock())) continue;
                    sp.gameMode.destroyBlock(cur); // 触发 BreakEvent → 登记 ctx → 原版破坏 → join 增强（不二次连锁）
                    mined++;
                }
                for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                    net.minecraft.core.BlockPos nb = cur.relative(dir);
                    if (!visited.contains(nb.asLong()) && slevel.getBlockState(nb).is(state.getBlock())) {
                        visited.add(nb.asLong());
                        queue.add(nb);
                    }
                }
            }
        } finally {
            VEIN_BUSY = false;
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
        // v0.8.1 矿物掉落增强上下文低频清理（每 128 tick 清一次超过 40 tick 的过期登记，防泄漏）
        if ((sp.tickCount & 127) == 0 && !MINING_CTX.isEmpty()) {
            long gt = sp.level().getGameTime();
            MINING_CTX.entrySet().removeIf(e -> gt - e.getValue().tick > 40);
        }
        // v0.6.16 TACZ 状态跟踪（仅持有对应海克斯的玩家记录，失去即清理，无界增长免疫）：
        // 架枪射击/走射专精共用位置跟踪；快速装填检测弹量 0→正（换弹完成）；切枪强化检测主手物品变化
        UUID uid = sp.getUUID();
        double bipodEff = effect(data, "tacz_bipod");
        double strafeEff = effect(data, "tacz_strafe");
        double reloadEff = effect(data, "tacz_reload_haste");
        double swapEff = effect(data, "tacz_swap_strike");
        long now = System.currentTimeMillis();
        if (bipodEff > 0 || strafeEff > 0) {
            net.minecraft.world.phys.Vec3 pos = sp.position();
            double[] last = LAST_POS.get(uid);
            if (last == null) {
                LAST_POS.put(uid, new double[]{pos.x, pos.y, pos.z});
                LAST_MOVE.put(uid, now);
            } else {
                double dx = pos.x - last[0], dy = pos.y - last[1], dz = pos.z - last[2];
                if (dx * dx + dy * dy + dz * dz > 0.0009) {
                    LAST_POS.put(uid, new double[]{pos.x, pos.y, pos.z});
                    LAST_MOVE.put(uid, now);
                }
            }
        } else {
            LAST_POS.remove(uid);
            LAST_MOVE.remove(uid);
        }
        if (reloadEff > 0) {
            int cur = currentTaczAmmo(sp);
            int lastAmmo = LAST_TACZ_AMMO.getOrDefault(uid, -1);
            if (cur > 0 && lastAmmo == 0) RELOAD_UNTIL.put(uid, now + 4000); // 换弹完成（弹量 0→正）
            LAST_TACZ_AMMO.put(uid, cur);
        } else {
            LAST_TACZ_AMMO.remove(uid);
            RELOAD_UNTIL.remove(uid);
        }
        if (swapEff > 0) {
            net.minecraft.world.item.Item main = sp.getMainHandItem().getItem();
            net.minecraft.world.item.Item lastItem = LAST_MAIN_HAND.get(uid);
            if (lastItem != null && lastItem != main) SWAP_UNTIL.put(uid, now + 3000); // 切主手武器
            LAST_MAIN_HAND.put(uid, main);
        } else {
            LAST_MAIN_HAND.remove(uid);
            SWAP_UNTIL.remove(uid);
        }
        // v0.6.16 持续射击 / 棱彩追踪 / 暴击链状态清理（仅持有者保留，失去即清，无界增长免疫）
        if (effect(data, "tacz_barrage") <= 0) { BAR_HITS.remove(uid); BAR_LAST_HIT.remove(uid); }
        if (effect(data, "tacz_tracking") <= 0) TACZ_TRACK.remove(uid);
        if (effect(data, "tacz_crit_chain") <= 0) CRIT_CHAIN_UNTIL.remove(uid);
        double regen = effect(data, "regen");
        if (regen > 0 && sp.tickCount % 100 == 0) {
            sp.heal((float) regen);
        }
        // v0.6.0 通用叠层管理器：每 tick 结算战斗叠层脱战衰减（每 20 tick 结算一次，降低开销）
        // 世界海克斯「攻势易散」加速衰减（独立乘区）
        if (sp.tickCount % 20 == 0) {
            double decayBonus = sp.level() instanceof net.minecraft.server.level.ServerLevel whLvl
                    ? com.hexcraft.event.WorldHexEvents.combatStackDecayBonus(whLvl) : 0;
            data.tickStacks(1 + decayBonus);
            // v0.6.2 计时叠层：固定时长到期自动消失
            data.tickTimedStacks();
            // v0.6.2 流血 DoT 结算（每 20 tick；魔法伤害不归属玩家，不触发反伤/不再进 onDamageDealt）
            if (!BLEEDS.isEmpty()) {
                java.util.Iterator<Map.Entry<UUID, List<double[]>>> it = BLEEDS.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, List<double[]>> e = it.next();
                    net.minecraft.world.entity.Entity ent = sp.serverLevel().getEntity(e.getKey());
                    if (!(ent instanceof LivingEntity target) || target.isRemoved() || !target.isAlive()) {
                        it.remove();
                        continue;
                    }
                    float total = 0;
                    for (double[] d : e.getValue()) {
                        total += d[0];
                        d[1] -= 20;
                    }
                    // P0 修复：BLEEDS 值为 CopyOnWriteArrayList，其迭代器不支持 remove()（抛 UnsupportedOperationException），
                    // 改为遍历后 removeIf 清理到期实例（语义不变：到期这次仍结算伤害）
                    e.getValue().removeIf(d -> d[1] <= 0);
                    if (total > 0) {
                        target.hurt(sp.damageSources().magic(), total);
                    }
                    if (e.getValue().isEmpty()) it.remove();
                }
            }
            // v0.6.2 猎手标记过期清理（70 tick 已到即移除）
            if (!MARKS.isEmpty()) {
                MARKS.entrySet().removeIf(en -> {
                    net.minecraft.world.entity.Entity t = sp.serverLevel().getEntity(en.getKey());
                    return !(t instanceof LivingEntity livingT) || livingT.isRemoved() || !livingT.isAlive();
                });
            }
        }
        // v0.6.2 周期叠层（on_tick_stack：tick_gain 键海克斯，每 5 秒自动叠 1 层；计时叠层刷新剩余时长，
        // 世界海克斯「转瞬」缩短时长为 ×(1+timed_stack_short)）
        if (sp.tickCount % 100 == 0) {
            double shortMult = sp.level() instanceof net.minecraft.server.level.ServerLevel whTL
                    ? com.hexcraft.event.WorldHexEvents.timedStackShort(whTL) : 0;
            boolean tickStacked = false;
            for (PlayerData.HexEntry e : data.getHexes()) {
                HextechDefinition sdef = HextechRegistry.get(e.id);
                if (sdef == null || sdef.stack == null || sdef.stack.onKill || !sdef.event.contains("tick_gain")) continue;
                if (data.getStack(sdef.stack.key) == null) {
                    data.putStack(new PlayerData.StackEntry(sdef.stack.key, sdef.stack.cap, sdef.stack.combat,
                            sdef.stack.decayDelay, sdef.stack.decayRate));
                }
                data.addStack(sdef.stack.key, 1 + effect(data, "stack_gain"), 1 + effect(data, "stack_cap"));
                PlayerData.StackEntry s = data.getStack(sdef.stack.key);
                if (s != null) {
                    s.timedTicks = data.timedBaseTicks(shortMult); // 刷新计时叠层剩余时长（到期自动消失）
                    com.hexcraft.network.Packets.S2CTipMessage.sendTo(sp, Component.translatable("message.hexcraft.stack_named",
                            Component.translatable("hexcraft.hextech." + e.id + ".name"), (int) s.value), 0);
                }
                tickStacked = true;
            }
            if (tickStacked) sync(sp);
        }
        // v0.6.2 叠层护盾（shield_gain 键：固垒/鏖战屏障等——每层提供吸收护盾 量×等级，随叠层增长补满护盾）
        if (sp.tickCount % 40 == 0) {
            for (PlayerData.HexEntry e : data.getHexes()) {
                HextechDefinition sdef = HextechRegistry.get(e.id);
                if (sdef == null || sdef.stack == null || !sdef.event.contains("shield_gain")) continue;
                PlayerData.StackEntry s = data.getStack(sdef.stack.key);
                if (s == null || s.value <= 0) continue;
                double per = 0;
                for (String token : sdef.event.split(",")) {
                    int ci = token.indexOf(':');
                    if (ci > 0 && token.substring(0, ci).equals("shield_gain")) {
                        try { per += Double.parseDouble(token.substring(ci + 1)) * e.level; } catch (Exception ignored) { }
                    }
                }
                if (per > 0 && sp.getAbsorptionAmount() < s.value * per) {
                    sp.setAbsorptionAmount((float) (s.value * per));
                }
            }
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
            // v0.4.12：客户端配置为主——进服时把客户端死亡保留配置应用到服务端 gamerule（任一开启即保留；
            // 单机 integrated server 同进程可读客户端配置；联机专用服务端无客户端配置则跳过，以房主/服务端 gamerule 为准）
            applyClientKeepRules(sp);
            // v0.4.13/0.4.14：登录恢复兜底——capability 为空时优先从死亡备份恢复，其次从全量快照恢复（死亡后登出重进也不丢）
            CompoundTag bak = sp.getPersistentData().getCompound("hexcraft_keep");
            if (bak.isEmpty()) bak = sp.getPersistentData().getCompound("hexcraft_data");
            if (!bak.isEmpty() && data.getHexes().isEmpty() && data.getForges().isEmpty()
                    && data.getEquipment().isEmpty() && data.getCoins() == 0) {
                data.deserializeNBT(bak);
                com.hexcraft.Hexcraft.LOGGER.info("[Hexcraft] 玩家 {} 登录：从 persistentData 恢复备份 海克斯={} 锻造={} 装备={} 晶币={}",
                        sp.getName().getString(), data.getHexes().size(), data.getForges().size(),
                        data.getEquipment().size(), data.getCoins());
            }
            data.applyModifiers(sp);
            sync(sp);
            // v0.6.4 登录同步全量 Registry 定义（联机客户端 HextechRegistry 无数据包加载，海克斯页/属性页依赖此缓存）
            com.hexcraft.network.Packets.S2CSyncRegistry.sendTo(sp);
            // 世界海克斯（v0.3.21）：登录时同步全服列表（所有玩家都能看到已生效的世界海克斯）
            com.hexcraft.network.Packets.broadcastWorldHexes(sp.server);
        }
    }

    /** v0.4.12：把客户端配置的死亡保留项应用到服务端 gamerule（只开不关，尊重服务端显式设置） */
    private static void applyClientKeepRules(ServerPlayer sp) {
        try {
            GameRules gr = sp.serverLevel().getGameRules();
            if (com.hexcraft.config.HexConfig.KEEP_DEATH_HEXES.get() && !gr.getBoolean(HexGameRules.KEEP_HEXES))
                gr.getRule(HexGameRules.KEEP_HEXES).set(true, sp.server);
            if (com.hexcraft.config.HexConfig.KEEP_DEATH_FORGES.get() && !gr.getBoolean(HexGameRules.KEEP_FORGES))
                gr.getRule(HexGameRules.KEEP_FORGES).set(true, sp.server);
            if (com.hexcraft.config.HexConfig.KEEP_DEATH_EQUIPS.get() && !gr.getBoolean(HexGameRules.KEEP_EQUIPS))
                gr.getRule(HexGameRules.KEEP_EQUIPS).set(true, sp.server);
            if (com.hexcraft.config.HexConfig.KEEP_DEATH_COINS.get() && !gr.getBoolean(HexGameRules.KEEP_COINS))
                gr.getRule(HexGameRules.KEEP_COINS).set(true, sp.server);
        } catch (Exception e) {
            // 联机专用服务端无客户端配置，跳过即可
        }
    }

    @SubscribeEvent
    public static void onSaveToFile(PlayerEvent.SaveToFile event) {
        // v0.4.14：全量持久化——玩家数据写盘前把 PlayerData 快照写入 persistentData（随玩家 NBT 保存，登出/重进不丢）
        try {
            PlayerData d = ModCapabilities.get(event.getEntity());
            if (d != null) event.getEntity().getPersistentData().put("hexcraft_data", d.serializeNBT());
        } catch (Exception e) {
            // 忽略
        }
    }

    @SubscribeEvent
    public static void onLoadFromFile(PlayerEvent.LoadFromFile event) {
        // v0.4.14：全量持久化——玩家文件加载后，capability 为空但有历史快照时恢复（登出重进数据保留）
        try {
            CompoundTag saved = event.getEntity().getPersistentData().getCompound("hexcraft_data");
            if (saved.isEmpty()) return;
            PlayerData d = ModCapabilities.get(event.getEntity());
            if (d != null && d.getHexes().isEmpty() && d.getForges().isEmpty()
                    && d.getEquipment().isEmpty() && d.getCoins() == 0) {
                d.deserializeNBT(saved);
                com.hexcraft.Hexcraft.LOGGER.info("[Hexcraft] 玩家 {} 文件加载：从 hexcraft_data 恢复 海克斯={} 锻造={} 装备={} 晶币={}",
                        event.getEntity().getName().getString(), d.getHexes().size(), d.getForges().size(),
                        d.getEquipment().size(), d.getCoins());
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        String way = "NONE";
        try {
            PlayerData neu = ModCapabilities.get(event.getEntity());
            PlayerData old = ModCapabilities.get(event.getOriginal());
            // v0.4.13：死亡保留双通道恢复——通道 1：死亡瞬间写入 persistentData 的备份（最稳，capability 失效也能恢复）；
            // 通道 2：capability copyFrom 兜底（旧实体 capability 仍有效时复制）
            CompoundTag backup = event.getOriginal().getPersistentData().getCompound("hexcraft_keep");
            if (neu != null && !backup.isEmpty()) {
                neu.deserializeNBT(backup);
                way = "persistentData备份";
            }
            if (neu != null && old != null) {
                neu.copyFrom(old);
                way = way.equals("NONE") ? "copyFrom" : "persistentData备份+copyFrom";
            }
            if (neu != null) {
                com.hexcraft.Hexcraft.LOGGER.info("[Hexcraft] 玩家重生恢复：方式={} 海克斯={} 锻造={} 装备={} 晶币={}",
                        way, neu.getHexes().size(), neu.getForges().size(), neu.getEquipment().size(), neu.getCoins());
            } else {
                com.hexcraft.Hexcraft.LOGGER.warn("[Hexcraft] 玩家重生恢复：方式={} (capability 为空!)", way);
            }
        } catch (Exception e) {
            com.hexcraft.Hexcraft.LOGGER.warn("[Hexcraft] 玩家重生恢复异常：{}", e.toString());
        }
        // 无论恢复是否成功，都强制重算属性并刷新客户端界面（杜绝"死亡清零但面板残留旧界面"）
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
        // v0.6.2 DLC 外接包：先扫描 hexworkshop_dlc/packs（启动与数据包重载均触发），
        // 随后各 loader apply 阶段把启用包的 data/hex、data/equipment、data/world_hex 注册进本体注册表
        com.hexcraft.dlc.DlcManager.scan();
        event.addListener(new HexDataLoader());
        event.addListener(new com.hexcraft.data.EquipmentDataLoader());
        // v0.6.0 世界海克斯数据包开放：data/hexworkshop/world_hexes/*.json（覆盖/新增世界海克斯）
        event.addListener(new com.hexcraft.data.WorldHexDataLoader());
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

    /** v0.6.16 读取主手枪械当前弹药（供「绝地弹匣」条件；读不到/非 TACZ 返回 -1，未命中条件） */
    private static int currentTaczAmmo(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.hasTag()) return -1;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !"tacz".equals(rl.getNamespace())) return -1;
        int[] hit = findAmmoInt(stack.getTag(), 0);
        return (hit != null && hit[1] > 0) ? hit[1] : -1;
    }

    /** v0.6.16 弹匣虹吸：主手枪械回复 1 发弹药（long+钳制，满弹/非 TACZ 无操作） */
    private static void addOneTaczAmmo(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty() || !stack.hasTag()) return;
        ResourceLocation rl = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (rl == null || !"tacz".equals(rl.getNamespace())) return;
        int[] hit = findAmmoInt(stack.getTag(), 0);
        if (hit == null || hit[1] <= 0) return;
        int cur = sanitizeAmmo(hit[1]);
        int maxAmmo = taczMaxAmmo(player, stack.getTag());
        if (maxAmmo > 0 && cur >= maxAmmo) return; // 满弹不补
        int cap = maxAmmo > 0 ? Math.min(maxAmmo, AMMO_CAP) : AMMO_CAP;
        int target = (int) Math.min((long) cur + 1, cap);
        String key = hit[0] >= 0 ? AMMO_KEYS[hit[0]] : "GunCurrentAmmoCount";
        stack.getTag().putInt(key, target);
        LOGGER.debug("Hexcraft TACZ siphon +1 ({} -> {}/{})", cur, target, maxAmmo > 0 ? maxAmmo : "?");
    }

    /** v0.6.4 主弹夹弹药硬上限（用户要求）：任何通道回填钳制 ≤9999，
     *  并全程 long 运算——此前 hit[1]+restored 在叠高 tacz_ammo 后整数溢出（实测 -16 亿）。 */
    private static final int AMMO_CAP = 9999;

    /** 计算回填后弹量：long 运算防溢出；上限取 min(枪械弹容, 9999)（读不到弹容则直接用 9999） */
    private static int ammoTarget(long current, double ratio, int maxAmmo) {
        long restored = Math.max(1L, (long) Math.floor((double) current * ratio));
        long target = current + restored;
        long cap = maxAmmo > 0 ? Math.min(maxAmmo, (long) AMMO_CAP) : (long) AMMO_CAP;
        return (int) Math.min(target, cap);
    }

    /** v0.6.4 损坏弹药值钳制：NBT 中弹量为负数（溢出残留）或超上限时先归位（负数归 0，超限归上限） */
    private static int sanitizeAmmo(int value) {
        if (value < 0) return 0;
        return Math.min(value, AMMO_CAP);
    }

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
                    int cur = sanitizeAmmo(hit[1]); // v0.6.4 损坏弹药值（负数/超上限残留）先归位
                    if (cur != hit[1]) stack.getTag().putInt(hit[0] >= 0 ? AMMO_KEYS[hit[0]] : "GunCurrentAmmoCount", cur);
                    int maxAmmo = taczMaxAmmo(player, stack.getTag());
                    if (maxAmmo > 0 && cur >= maxAmmo) {
                        fromNbt = true; // 满弹：本次枪械不补（背包通道仍可补弹药盒）
                    } else {
                        int target = ammoTarget(cur, ratio, maxAmmo); // v0.6.4 long 运算 + 9999 上限
                        String key = hit[0] >= 0 ? AMMO_KEYS[hit[0]] : "GunCurrentAmmoCount";
                        stack.getTag().putInt(key, target);
                        any = true;
                        fromNbt = true;
                        LOGGER.debug("Hexcraft TACZ ammo channel-1: {} key={} +{} ({} -> {}/{})",
                                stack.getItem(), key, target - cur, cur, target, maxAmmo > 0 ? maxAmmo : "?");
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
                    int cur = sanitizeAmmo(hit[1]); // v0.6.4 损坏弹药值先归位（弹匣同理）
                    if (cur != hit[1]) stack.getTag().putInt(hit[0] >= 0 ? AMMO_KEYS[hit[0]] : "GunCurrentAmmoCount", cur);
                    int maxAmmo = taczMaxAmmo(player, stack.getTag());
                    if (!(maxAmmo > 0 && cur >= maxAmmo)) {
                        int target = ammoTarget(cur, ratio, maxAmmo); // v0.6.4 long 运算 + 9999 上限
                        String key = hit[0] >= 0 ? AMMO_KEYS[hit[0]] : "GunCurrentAmmoCount";
                        stack.getTag().putInt(key, target);
                        any = true;
                        LOGGER.debug("Hexcraft TACZ ammo channel-3 mag: {} key={} +{}", stack.getItem(), key, target - cur);
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
