package com.hexcraft.config;

import net.minecraft.world.level.GameRules;

/** 死亡保留规则：由玩家/服主通过 gamerule 决定死亡后是否保留各系统进度（默认全部清零） */
public final class HexGameRules {

    public static final GameRules.Key<GameRules.BooleanValue> KEEP_HEXES =
            GameRules.register("hexworkshopKeepHexes", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> KEEP_FORGES =
            GameRules.register("hexworkshopKeepForges", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> KEEP_EQUIPS =
            GameRules.register("hexworkshopKeepEquips", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    public static final GameRules.Key<GameRules.BooleanValue> KEEP_COINS =
            GameRules.register("hexworkshopKeepCoins", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));

    private HexGameRules() { }

    /** 供配置界面/网络包按索引访问：0=海克斯 1=锻造 2=装备 3=晶币 */
    public static final GameRules.Key<GameRules.BooleanValue>[] KEYS = new GameRules.Key[]{
            KEEP_HEXES, KEEP_FORGES, KEEP_EQUIPS, KEEP_COINS};

    /** 触发类加载以执行静态注册（服务器创建前调用一次即可） */
    public static void touch() { }
}
