package com.hexcraft.link;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * v0.8.3 ProjectE 等价交换联动（EMC A：充值页 EMC⇄晶币 折算通道；EMC C：等价交换主题海克斯）。
 * 零编译期依赖：所有 ProjectE API 调用走反射（ProjectEAPI.getEMCProxy().getValue(ItemStack)，
 * 1.20.1 Forge PE1.1.0 稳定 API），未安装 projecte 时由 Hexcraft 主类守卫不挂载、前缀自动隐藏。
 *
 * 消费通道（HextechEvents.effect 键）：
 *  emc_transmute  击杀时掉落物总 EMC 按比例折算为晶币（额外获得，不销毁掉落）
 *  emc_bargain    EMC 兑换通道汇率加成（充值页换算与服内折算共用）
 *  emc_wealth     主手物品每 2000 EMC 值提供攻击增伤（每级 +1%，上限 +20%/级）
 */
public final class EmcLink {

    private static boolean registered = false;
    private static final long EMC_WEALTH_STEP = 2000;

    private EmcLink() { }

    public static void init() {
        if (registered) return;
        registered = true;
        MinecraftForge.EVENT_BUS.register(EmcLink.class);
        org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
        LOGGER.info("Hexcraft: ProjectE 等价交换联动已挂载（EMC A+C）");
    }

    // ================= ProjectE 反射入口 =================

    /** 查询物品 EMC 值；未装 projecte 或查询失败返回 0 */
    public static long getEMCValue(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        try {
            Class<?> api = Class.forName("moze_intel.projecte.api.ProjectEAPI");
            java.lang.reflect.Method getProxy = api.getMethod("getEMCProxy");
            Object proxy = getProxy.invoke(null);
            java.lang.reflect.Method getValue = proxy.getClass().getMethod("getValue", ItemStack.class);
            Object v = getValue.invoke(proxy, stack);
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    // ================= v0.9.8 玩家 EMC 余额直充（替代主手物品折算） =================
    // 接口已实查（ProjectE-1.20.1-PE1.0.1）：ProjectEAPI.getTransmutationProxy()
    //   -> ITransmutationProxy.getKnowledgeProviderFor(UUID)
    //   -> IKnowledgeProvider.getEmc() : BigInteger / setEmc(BigInteger) / syncEmc(ServerPlayer)

    /** 获取玩家 EMC 知识提供者（IKnowledgeProvider）；未装 projecte 或反射失败返回 null */
    private static Object knowledgeFor(ServerPlayer sp) {
        if (sp == null) return null;
        try {
            Class<?> api = Class.forName("moze_intel.projecte.api.ProjectEAPI");
            java.lang.reflect.Method getProxy = api.getMethod("getTransmutationProxy");
            Object proxy = getProxy.invoke(null);
            java.lang.reflect.Method getKp = proxy.getClass().getMethod("getKnowledgeProviderFor", java.util.UUID.class);
            return getKp.invoke(proxy, sp.getUUID());
        } catch (Throwable t) {
            return null;
        }
    }

    /** 玩家当前 EMC 余额（未装 projecte / 获取失败返回 -1 表示不可用） */
    public static long getPlayerEMC(ServerPlayer sp) {
        Object kp = knowledgeFor(sp);
        if (kp == null) return -1;
        try {
            Object v = kp.getClass().getMethod("getEmc").invoke(kp);
            if (v instanceof java.math.BigInteger bi) {
                return bi.signum() > 0 ? bi.longValue() : 0;
            }
            return v instanceof Number n ? n.longValue() : 0;
        } catch (Throwable t) {
            return -1;
        }
    }

    /** 从玩家 EMC 余额扣减 amount（余额不足返回 false，不扣）；成功后同步 ProjectE 客户端显示 */
    public static boolean spendPlayerEMC(ServerPlayer sp, long amount) {
        Object kp = knowledgeFor(sp);
        if (kp == null || amount <= 0) return false;
        try {
            Object v = kp.getClass().getMethod("getEmc").invoke(kp);
            java.math.BigInteger emc = v instanceof java.math.BigInteger bi ? bi : java.math.BigInteger.ZERO;
            java.math.BigInteger need = java.math.BigInteger.valueOf(amount);
            if (emc.compareTo(need) < 0) return false;
            java.math.BigInteger rest = emc.subtract(need);
            kp.getClass().getMethod("setEmc", java.math.BigInteger.class).invoke(kp, rest);
            kp.getClass().getMethod("syncEmc", ServerPlayer.class).invoke(kp, sp);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /** EMC 折算晶币（向下取整）：emc / EMC_COIN_RATE × (1 + emc_bargain) */
    public static int coinsForEMC(ServerPlayer sp, long emc) {
        if (emc <= 0) return 0;
        double rate = Math.max(1.0, HexConfig.EMC_COIN_RATE.get());
        PlayerData data = ModCapabilities.get(sp);
        double bonus = data == null ? 0 : HextechEvents.effect(data, "emc_bargain");
        return (int) Math.floor((double) emc / rate * (1.0 + Math.min(5.0, bonus)));
    }

    private static double eff(ServerPlayer sp, String key) {
        PlayerData data = ModCapabilities.get(sp);
        return data == null ? 0 : HextechEvents.effect(data, key);
    }

    /** 炼金术士：EMC 折算计数（每 5 次触发一次双倍入账） */
    private static final java.util.Map<java.util.UUID, Integer> EMC_CONVERT = new java.util.concurrent.ConcurrentHashMap<>();

    // ================= EMC C：等价交换海克斯 =================

    /** 击杀掉落折算：掉落物总 EMC × 每级比例 → 晶币（额外，不销毁） */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        double ratio = eff(sp, "emc_transmute");
        if (ratio <= 0) return;
        long total = 0;
        for (ItemEntity ie : event.getDrops()) {
            if (ie != null && !ie.getItem().isEmpty()) {
                total += getEMCValue(ie.getItem()) * ie.getItem().getCount();
            }
        }
        if (total <= 0) return;
        int coins = (int) Math.floor(total * Math.min(1.0, ratio) / Math.max(1.0, HexConfig.EMC_COIN_RATE.get()));
        if (coins > 0) {
            // 炼金术士：每 5 次折算触发一次双倍入账
            if (eff(sp, "class_emc_double") > 0) {
                int n = EMC_CONVERT.getOrDefault(sp.getUUID(), 0) + 1;
                if (n >= 5) {
                    coins *= 2;
                    n = 0;
                }
                EMC_CONVERT.put(sp.getUUID(), n);
            }
            PlayerData data = ModCapabilities.get(sp);
            if (data != null) data.addCoins(coins);
        }
    }

    /** 财富增伤：主手物品 EMC 每 2000 → 每级 +1%（上限 20%/级） */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        double power = eff(sp, "emc_wealth");
        if (power <= 0) return;
        long emc = getEMCValue(sp.getMainHandItem());
        if (emc < EMC_WEALTH_STEP) return;
        double bonus = Math.min(0.20, (double) (emc / EMC_WEALTH_STEP) * 0.01) * power;
        if (bonus > 0) {
            event.setAmount(event.getAmount() * (float) (1.0 + bonus));
        }
    }
}
