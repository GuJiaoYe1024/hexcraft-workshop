package com.hexcraft.link;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * v0.8.2 存储 mod 联动：磁铁拾取（mining_magnet）的掉落优先存入第三方存储，放不下才进玩家背包。
 * 插入顺序：超越维度（Beyond Dimensions 玩家主维度网）→ 精妙背包（Sophisticated Backpacks，curios 背带槽 + 物品栏/装备）
 *          → 精致存储（Refined Storage，玩家 32 格内最近的网络）→ 玩家背包 → 剩余留原地。
 *
 * 全部经运行时反射调用第三方 API，零编译期依赖：未安装对应 mod 时自动跳过，任何异常静默回退到背包，
 * 绝不打断掉落流程（与 v0.8.1 接入点重构"原版/第三方流程不被打断"原则一致）。
 */
public final class StorageLink {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final int RS_RANGE = 32; // 精致存储网络半径（格）

    // ================= 反射句柄缓存（首次成功解析后复用，失败置 null 永久跳过） =================

    // --- Beyond Dimensions ---
    private static Method bdGetNet;      // DimensionsNet.getPrimaryNetFromPlayer(Player)
    private static Method bdGetUnified;  // DimensionsNet.getUnifiedStorage()
    private static Constructor<?> bdKeyCtor;   // ItemStackKey(ItemStack)
    private static Method bdGetByKey;    // UnifiedStorage.getStackByKey(IStackKey) -> KeyAmount
    private static Method bdInsert;      // UnifiedStorage.insert(IStackKey, long, boolean) -> KeyAmount
    private static Method bdKeyAmount;   // KeyAmount.amount()

    // --- Sophisticated Backpacks ---
    private static Object sbCapability;  // Capability<IBackpackWrapper>
    private static Method sbWrapperHandler; // IStorageWrapper.getInventoryForInputOutput()
    private static Method sbHandlerInsert;  // InventoryHandler.insertItem(ItemStack, boolean) -> ItemStack

    // --- Refined Storage ---
    private static Method rsApiInstance; // API.instance()
    private static Method rsGetNetworkManager; // IRSAPI.getNetworkManager(ServerLevel)
    private static Method rsNmAll;        // INetworkManager.all()
    private static Method rsNetPos;       // INetwork.getPosition()
    private static Method rsNetLevel;     // INetwork.getLevel()
    private static Method rsInsert;       // INetwork.insertItem(ItemStack, int, Action)
    private static Object rsActionPerform; // Action.PERFORM

    private static boolean bdOk = false, bdFailed = false;
    private static boolean sbOk = false, sbFailed = false;
    private static boolean rsOk = false, rsFailed = false;

    private StorageLink() { }

    /** 磁铁拾取入口：依次尝试各存储，最后回落玩家背包；返回仍未放下的剩余（调用方留在原地）。 */
    public static ItemStack insert(ServerPlayer sp, ItemStack stack) {
        if (sp == null || stack == null || stack.isEmpty()) return stack;
        ItemStack remain = stack;
        // 1) 超越维度（玩家主维度网络，无位置限制）
        if (!remain.isEmpty()) remain = insertBeyondDimensions(sp, remain);
        // 2) 精妙背包（随身背包，curios + 物品栏/装备）
        if (!remain.isEmpty()) remain = insertSophisticatedBackpacks(sp, remain);
        // 3) 精致存储（玩家附近 32 格内网络）
        if (!remain.isEmpty()) remain = insertRefinedStorage(sp, remain);
        // 4) 玩家背包（尽力放入，剩余 shrink 后返回）
        if (!remain.isEmpty() && sp.getInventory().add(remain)) {
            remain = ItemStack.EMPTY;
        }
        return remain;
    }

    // ================= 超越维度 =================

    private static ItemStack insertBeyondDimensions(ServerPlayer sp, ItemStack stack) {
        if (bdFailed) return stack;
        try {
            if (!bdOk) {
                if (!net.minecraftforge.fml.ModList.get().isLoaded("beyonddimensions")) { bdFailed = true; return stack; }
                Class<?> netCls = Class.forName("com.wintercogs.beyonddimensions.api.dimensionnet.DimensionsNet");
                bdGetNet = netCls.getMethod("getPrimaryNetFromPlayer", net.minecraft.world.entity.player.Player.class);
                bdGetUnified = netCls.getMethod("getUnifiedStorage");
                Class<?> keyCls = Class.forName("com.wintercogs.beyonddimensions.api.storage.key.impl.ItemStackKey");
                bdKeyCtor = keyCls.getConstructor(net.minecraft.world.item.ItemStack.class);
                Class<?> usCls = Class.forName("com.wintercogs.beyonddimensions.api.storage.handler.impl.StackHandler");
                Class<?> keyItf = Class.forName("com.wintercogs.beyonddimensions.api.storage.key.IStackKey");
                bdGetByKey = usCls.getMethod("getStackByKey", keyItf);
                bdInsert = usCls.getMethod("insert", keyItf, long.class, boolean.class);
                bdKeyAmount = Class.forName("com.wintercogs.beyonddimensions.api.storage.key.KeyAmount").getMethod("amount");
                bdOk = true;
            }
            Object net = bdGetNet.invoke(null, sp);
            if (net == null) return stack; // 玩家没有主维度网络
            Object us = bdGetUnified.invoke(net);
            if (us == null) return stack;
            Object key = bdKeyCtor.newInstance(stack.copy());
            long before = amountOf(bdGetByKey.invoke(us, key));
            bdInsert.invoke(us, key, stack.getCount(), Boolean.FALSE);
            long after = amountOf(bdGetByKey.invoke(us, key));
            long inserted = Math.max(0, after - before);
            if (inserted >= stack.getCount()) return ItemStack.EMPTY;
            if (inserted > 0) return stack.copyWithCount((int) (stack.getCount() - inserted));
            return stack;
        } catch (Throwable t) {
            bdFailed = true;
            LOGGER.warn("Hexcraft StorageLink: Beyond Dimensions 联动失败，已回退（原因：{}）", t.toString());
            return stack;
        }
    }

    private static long amountOf(Object keyAmount) {
        if (keyAmount == null) return 0;
        try { return (Long) bdKeyAmount.invoke(keyAmount); } catch (Exception e) { return 0; }
    }

    // ================= 精妙背包 =================

    private static ItemStack insertSophisticatedBackpacks(ServerPlayer sp, ItemStack stack) {
        if (sbFailed) return stack;
        try {
            if (!sbOk) {
                if (!net.minecraftforge.fml.ModList.get().isLoaded("sophisticatedbackpacks")
                        || !net.minecraftforge.fml.ModList.get().isLoaded("sophisticatedcore")) { sbFailed = true; return stack; }
                Class<?> capCls = Class.forName("net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper");
                Field f = capCls.getField("BACKPACK_WRAPPER_CAPABILITY");
                sbCapability = f.get(null);
                Class<?> wrapperItf = Class.forName("net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper");
                sbWrapperHandler = wrapperItf.getMethod("getInventoryForInputOutput");
                Class<?> handlerCls = Class.forName("net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler");
                sbHandlerInsert = handlerCls.getMethod("insertItem", net.minecraft.world.item.ItemStack.class, boolean.class);
                sbOk = true;
            }
            ItemStack remain = stack;
            // 来源 A：curios 背带槽（getEquippedCurios → Forge IItemHandlerModifiable）
            remain = insertFromCurios(sp, remain);
            if (remain.isEmpty()) return remain;
            // 来源 B：物品栏 + 装备 + 副手（Inventory 即 IItemHandlerModifiable）
            net.minecraft.world.entity.player.Inventory inv = sp.getInventory();
            int size = inv.getContainerSize();
            for (int i = 0; i < size && !remain.isEmpty(); i++) {
                ItemStack slot = inv.getItem(i);
                if (slot.isEmpty()) continue;
                ItemStack rest = tryInsertIntoBackpack(slot, remain);
                if (rest.getCount() < remain.getCount()) remain = rest;
            }
            return remain;
        } catch (Throwable t) {
            sbFailed = true;
            LOGGER.warn("Hexcraft StorageLink: Sophisticated Backpacks 联动失败，已回退（原因：{}）", t.toString());
            return stack;
        }
    }

    private static ItemStack insertFromCurios(ServerPlayer sp, ItemStack stack) throws Exception {
        if (stack.isEmpty()) return stack;
        Class<?> curiosApi = null;
        try { curiosApi = Class.forName("top.theillusivec4.curios.api.CuriosApi"); } catch (ClassNotFoundException e) { return stack; }
        Method getCurios = curiosApi.getMethod("getCuriosInventory", net.minecraft.world.entity.LivingEntity.class);
        Object lazy = getCurios.invoke(null, sp);
        if (lazy == null) return stack;
        Method resolve = lazy.getClass().getMethod("resolve");
        Object opt = resolve.invoke(lazy);
        if (opt == null) return stack;
        boolean present = (Boolean) opt.getClass().getMethod("isPresent").invoke(opt);
        if (!present) return stack;
        Object handler = opt.getClass().getMethod("get").invoke(opt);
        Method getEquipped = handler.getClass().getMethod("getEquippedCurios");
        Object equipped = getEquipped.invoke(handler);
        if (!(equipped instanceof net.minecraftforge.items.IItemHandlerModifiable mod)) return stack;
        ItemStack remain = stack;
        int slots = mod.getSlots();
        for (int s = 0; s < slots && !remain.isEmpty(); s++) {
            ItemStack slot = mod.getStackInSlot(s);
            if (slot.isEmpty()) continue;
            ItemStack rest = tryInsertIntoBackpack(slot, remain);
            if (rest.getCount() < remain.getCount()) remain = rest;
        }
        return remain;
    }

    /** 尝试把 stack 塞进"背包物品"的存储（item.getCapability(BACKPACK_WRAPPER_CAPABILITY) → wrapper → insertItem） */
    private static ItemStack tryInsertIntoBackpack(ItemStack backpackItem, ItemStack stack) {
        try {
            Object lazy = backpackItem.getCapability((net.minecraftforge.common.capabilities.Capability<?>) sbCapability);
            if (lazy == null) return stack;
            Object opt = lazy.getClass().getMethod("resolve").invoke(lazy);
            if (opt == null) return stack;
            boolean present = (Boolean) opt.getClass().getMethod("isPresent").invoke(opt);
            if (!present) return stack;
            Object wrapper = opt.getClass().getMethod("get").invoke(opt);
            Object handler = sbWrapperHandler.invoke(wrapper);
            if (handler == null) return stack;
            Object rest = sbHandlerInsert.invoke(handler, stack, Boolean.FALSE);
            return rest instanceof ItemStack is ? is : stack;
        } catch (Throwable t) {
            return stack;
        }
    }

    // ================= 精致存储 =================

    private static ItemStack insertRefinedStorage(ServerPlayer sp, ItemStack stack) {
        if (rsFailed) return stack;
        try {
            if (!rsOk) {
                if (!net.minecraftforge.fml.ModList.get().isLoaded("refinedstorage")) { rsFailed = true; return stack; }
                Class<?> apiCls = Class.forName("com.refinedmods.refinedstorage.apiimpl.API");
                rsApiInstance = apiCls.getMethod("instance");
                Class<?> apiItf = Class.forName("com.refinedmods.refinedstorage.api.IRSAPI");
                rsGetNetworkManager = apiItf.getMethod("getNetworkManager", net.minecraft.server.level.ServerLevel.class);
                rsNmAll = Class.forName("com.refinedmods.refinedstorage.api.network.INetworkManager").getMethod("all");
                Class<?> netItf = Class.forName("com.refinedmods.refinedstorage.api.network.INetwork");
                rsNetPos = netItf.getMethod("getPosition");
                rsNetLevel = netItf.getMethod("getLevel");
                rsInsert = netItf.getMethod("insertItem", net.minecraft.world.item.ItemStack.class, int.class,
                        Class.forName("com.refinedmods.refinedstorage.api.util.Action"));
                Class<?> actionCls = Class.forName("com.refinedmods.refinedstorage.api.util.Action");
                rsActionPerform = Enum.valueOf((Class<? extends Enum>) actionCls, "PERFORM");
                rsOk = true;
            }
            Object api = rsApiInstance.invoke(null);
            Object nm = rsGetNetworkManager.invoke(api, (net.minecraft.server.level.ServerLevel) sp.level());
            if (nm == null) return stack;
            Object all = rsNmAll.invoke(nm);
            if (!(all instanceof java.util.Collection<?> coll) || coll.isEmpty()) return stack;
            Level playerLevel = sp.level();
            BlockPos ppos = sp.blockPosition();
            ItemStack remain = stack;
            for (Object net : coll) {
                if (remain.isEmpty()) break;
                Level netLevel = (Level) rsNetLevel.invoke(net);
                if (netLevel != playerLevel) continue;
                BlockPos npos = (BlockPos) rsNetPos.invoke(net);
                if (npos == null) continue;
                double distSq = ppos.distSqr(npos);
                if (distSq > (long) RS_RANGE * RS_RANGE) continue;
                Object rest = rsInsert.invoke(net, remain, remain.getCount(), rsActionPerform);
                if (rest instanceof ItemStack is) remain = is;
            }
            return remain;
        } catch (Throwable t) {
            rsFailed = true;
            LOGGER.warn("Hexcraft StorageLink: Refined Storage 联动失败，已回退（原因：{}）", t.toString());
            return stack;
        }
    }
}
