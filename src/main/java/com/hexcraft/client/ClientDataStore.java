package com.hexcraft.client;

import com.hexcraft.capability.PlayerData;
import com.hexcraft.hextech.HextechDefinition;
import com.hexcraft.hextech.HextechRegistry;
import com.hexcraft.network.Packets;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 客户端本地缓存（由 S2CSyncData 更新，供界面展示） */
public class ClientDataStore {
    public static int coins;
    public static int forgesUsed;
    public static int maxForges;
    public static int pityGold;
    public static int pityPrismatic;
    /** 死亡保留游戏规则当前值（服务端权威，同步到客户端供配置界面显示/切换） */
    public static boolean keepHexes;
    public static boolean keepForges;
    public static boolean keepEquips;
    public static boolean keepCoins;
    public static final List<PlayerData.HexEntry> hexes = new ArrayList<>();
    public static final List<PlayerData.ForgeEntry> forges = new ArrayList<>();
    public static final List<PlayerData.EquipEntry> equipment = new ArrayList<>();
    public static final Map<String, Double> attrs = new LinkedHashMap<>();
    // v0.6.0 通用叠层（战斗叠层/永久叠层）客户端缓存（属性页展示）
    public static final List<PlayerData.StackEntry> stacks = new ArrayList<>();

    // v0.6.3 服务端权威费用快照（界面显示与实扣一致；折扣/世界乘区变化后服务端推送新快照）
    public static int rerollCost;
    public static final int[] forgeCosts = new int[3];
    public static int equipGoldCost;
    public static int equipPrismaticCost;
    public static int worldDrawCost;
    /** 费用快照版本号：每次 apply 递增，打开中的界面 tick 检测到变化即重建按钮（刷新显示） */
    public static int costVersion;

    /** v0.6.0 服务端是否已装载本 mod：收到过 S2CSyncData 视为已装载（单机/联机服务端装 mod=true；服务端未装=false，隐藏 H/K 入口） */
    public static boolean serverPresent;

    /** v0.6.3 打开海克斯工坊主界面时置 true：收到下一条 S2CSyncData 后用最新数据重建界面一次（联机抽取后界面为空的兜底修复） */
    public static boolean requestPendingSync;

    // ---- v0.6.4 服务端 Registry 定义同步缓存（联机客户端修复）----
    // 根因：HexDataLoader/EquipmentDataLoader 注册在服务端 AddReloadListenerEvent，纯客户端进程不触发，
    // 客户端 HextechRegistry 只有 static 兜底（13 个海克斯），其余 171 海克斯 + 44 装备定义缺失，
    // 导致联机玩家海克斯页/属性页全部为空（抽取界面用服务端 msg 数据所以正常）。
    // 方案：服务端把全部定义（渲染所需字段）随 S2CSyncRegistry 下发，客户端存这里；界面与
    // effect() 统一从 hexDef()/equipDef() 读取（优先内置 Registry，其次本缓存）。
    public static final Map<String, HextechDefinition> hexRegistryCache = new LinkedHashMap<>();
    public static final Map<String, HextechDefinition> equipRegistryCache = new LinkedHashMap<>();

    /** 读取海克斯定义：内置 Registry 优先（单机同进程已由数据包加载），缺失回退服务端同步缓存（联机） */
    public static HextechDefinition hexDef(String id) {
        HextechDefinition d = HextechRegistry.get(id);
        return d != null ? d : hexRegistryCache.get(id);
    }

    /** 读取装备定义：同 hexDef 策略 */
    public static HextechDefinition equipDef(String id) {
        HextechDefinition d = HextechRegistry.getEquip(id);
        return d != null ? d : equipRegistryCache.get(id);
    }

    public static void applyRegistry(Packets.S2CSyncRegistry msg) {
        hexRegistryCache.clear();
        hexRegistryCache.putAll(msg.hexes);
        equipRegistryCache.clear();
        equipRegistryCache.putAll(msg.equips);
    }

    // ---- 世界海克斯（v0.3.21）：全服列表客户端缓存 ----
    public static final List<String> worldHexes = new ArrayList<>();
    public static int worldMaxPerCategory;
    public static int worldDrawXp;
    public static int worldCancelCoin;

    public static void apply(Packets.S2CSyncData msg) {
        serverPresent = true;
        coins = msg.coins;
        forgesUsed = msg.forgesUsed;
        maxForges = msg.maxForges;
        pityGold = msg.pityGold;
        pityPrismatic = msg.pityPrismatic;
        keepHexes = msg.keepHexes;
        keepForges = msg.keepForges;
        keepEquips = msg.keepEquips;
        keepCoins = msg.keepCoins;
        hexes.clear();
        hexes.addAll(msg.hexes);
        forges.clear();
        forges.addAll(msg.forges);
        equipment.clear();
        equipment.addAll(msg.equipment);
        attrs.clear();
        attrs.putAll(msg.attrs);
        stacks.clear();
        stacks.addAll(msg.stacks);
        // v0.6.3 费用快照 + 版本递增（打开中的界面据此重建费用显示）
        rerollCost = msg.rerollCost;
        for (int i = 0; i < 3; i++) forgeCosts[i] = msg.forgeCosts[i];
        equipGoldCost = msg.equipGoldCost;
        equipPrismaticCost = msg.equipPrismaticCost;
        worldDrawCost = msg.worldDrawCost;
        costVersion++;
    }

    public static void applyWorld(Packets.S2CSyncWorldHexes msg) {
        worldHexes.clear();
        worldHexes.addAll(msg.active);
        worldMaxPerCategory = msg.maxPerCategory;
        worldDrawXp = msg.drawXp;
        worldCancelCoin = msg.cancelCoin;
        costVersion++;
    }
}
