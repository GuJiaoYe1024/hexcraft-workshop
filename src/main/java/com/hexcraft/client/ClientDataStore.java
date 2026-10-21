package com.hexcraft.client;

import com.hexcraft.capability.PlayerData;
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

    public static void apply(Packets.S2CSyncData msg) {
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
    }
}
