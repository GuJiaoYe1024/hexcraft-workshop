package com.hexcraft.client.gui;

import com.hexcraft.client.ClientDataStore;
import com.hexcraft.config.HexConfig;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 配置界面（Cloth Config 标准样式）：在 Mods 列表 → 本 Mod → Config 按钮打开。
 * 五个分类：海克斯抽取 / 经济系统 / 联动内容 / 客户端 / 游戏规则。
 * - 前四类直接绑定 HexConfig 的 ForgeConfigSpec 静态字段（即时保存 toml）；
 * - 游戏规则为服务端权威：toggle 点击发送 C2SSetGameRule，客户端显示服务端同步值。
 * 依赖 Cloth Config v10 API（cloth_config，正式客户端已安装 11.1.136）。
 */
public class HexConfigScreen {

    private HexConfigScreen() {}

    /** 死亡保留游戏规则 lang key 后缀 */
    private static final String[] RULE_KEYS = {"Hexes", "Forges", "Equips", "Coins"};

    /** 动态读取服务端同步的当前值（v0.4.10：去掉 static final 快照，界面打开时显示真实 gamerule，避免显示"是"但服务端为"否"） */
    private static boolean ruleValue(int i) {
        switch (i) {
            case 0: return ClientDataStore.keepHexes;
            case 1: return ClientDataStore.keepForges;
            case 2: return ClientDataStore.keepEquips;
            default: return ClientDataStore.keepCoins;
        }
    }

    public static Screen create(Screen parent) {
        ConfigBuilder b = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("screen.hexcraft.config.title"))
                .setSavingRunnable(() -> {
                    HexConfig.COMMON_SPEC.save();
                    HexConfig.CLIENT_SPEC.save();
                });
        ConfigEntryBuilder eb = b.entryBuilder();

        drawCategory(b, eb);
        economyCategory(b, eb);
        linkedCategory(b, eb);
        worldCategory(b, eb);
        clientCategory(b, eb);
        rulesCategory(b, eb);

        return b.build();
    }

    // ---------- 工具 ----------

    private static Component name(String field) {
        return Component.translatable("hexcraft.config." + field);
    }

    private static Component tip(String field) {
        return Component.translatable("hexcraft.config.tip." + field);
    }

    private static void intEntry(ConfigEntryBuilder eb, ConfigCategory cat,
                                 String field, ForgeConfigSpec.IntValue spec,
                                 int min, int max, int def) {
        cat.addEntry(eb.startIntField(name(field), spec.get())
                .setSaveConsumer(v -> setInt(spec, v))
                .setDefaultValue(def)
                .setMin(min).setMax(max)
                .setTooltip(tip(field))
                .build());
    }

    private static void doubleEntry(ConfigEntryBuilder eb, ConfigCategory cat,
                                    String field, ForgeConfigSpec.DoubleValue spec,
                                    double min, double max, double def) {
        cat.addEntry(eb.startDoubleField(name(field), spec.get())
                .setSaveConsumer(v -> setDouble(spec, v))
                .setDefaultValue(def)
                .setMin(min).setMax(max)
                .setTooltip(tip(field))
                .build());
    }

    private static void boolEntry(ConfigEntryBuilder eb, ConfigCategory cat,
                                  String field, ForgeConfigSpec.BooleanValue spec) {
        cat.addEntry(eb.startBooleanToggle(name(field), spec.get())
                .setSaveConsumer(v -> {
                    spec.set(v);
                    spec.save();
                })
                .setDefaultValue(spec.getDefault())
                .setTooltip(tip(field))
                .build());
    }

    private static void setInt(ForgeConfigSpec.IntValue spec, int v) {
        spec.set(v);
        spec.save();
    }

    private static void setDouble(ForgeConfigSpec.DoubleValue spec, double v) {
        spec.set(v);
        spec.save();
    }

    // ---------- 分类 ----------

    private static void drawCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.draw"));
        intEntry(eb, cat, "draw.xpCostPerDraw", HexConfig.XP_COST_PER_DRAW, 10, 1000000, 500);
        intEntry(eb, cat, "draw.rerollCost", HexConfig.REROLL_COST, 0, 10000, 20);
        intEntry(eb, cat, "draw.hexLimit", HexConfig.HEX_SLOT_COUNT, 0, 30, 0);
        intEntry(eb, cat, "draw.silverWeight", HexConfig.WEIGHT_SILVER, 0, 1000, 40);
        intEntry(eb, cat, "draw.goldWeight", HexConfig.WEIGHT_GOLD, 0, 1000, 35);
        intEntry(eb, cat, "draw.prismWeight", HexConfig.WEIGHT_PRISMATIC, 0, 1000, 25);
        intEntry(eb, cat, "draw.pityGoldDraws", HexConfig.PITY_GOLD, 0, 100, 4);
        intEntry(eb, cat, "draw.pityPrismDraws", HexConfig.PITY_PRISMATIC, 0, 200, 8);
        intEntry(eb, cat, "draw.maxHextechLevel", HexConfig.MAX_HEXTECH_LEVEL, 1, 10, 3);
    }

    private static void economyCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.economy"));
        intEntry(eb, cat, "eco.coinMin", HexConfig.COIN_MIN, 0, 100, 1);
        intEntry(eb, cat, "eco.coinMax", HexConfig.COIN_MAX, 0, 100, 3);
        intEntry(eb, cat, "eco.coinEliteMin", HexConfig.COIN_ELITE_MIN, 0, 1000, 5);
        intEntry(eb, cat, "eco.coinEliteMax", HexConfig.COIN_ELITE_MAX, 0, 1000, 10);
        intEntry(eb, cat, "eco.coinBossMin", HexConfig.COIN_BOSS_MIN, 0, 10000, 20);
        intEntry(eb, cat, "eco.coinBossMax", HexConfig.COIN_BOSS_MAX, 0, 10000, 50);
        intEntry(eb, cat, "eco.antiFarmWindowSec", HexConfig.ANTI_FARM_WINDOW_SEC, 0, 600, 30);
        doubleEntry(eb, cat, "eco.antiFarmDecay", HexConfig.ANTI_FARM_DECAY, 0.0, 1.0, 0.2);
        intEntry(eb, cat, "eco.forgeBasicPrice", HexConfig.FORGE_BASIC_PRICE, 0, 100000, 50);
        intEntry(eb, cat, "eco.forgeGoldPrice", HexConfig.FORGE_GOLD_PRICE, 0, 100000, 150);
        intEntry(eb, cat, "eco.forgePrismaticPrice", HexConfig.FORGE_PRISMATIC_PRICE, 0, 100000, 400);
        intEntry(eb, cat, "eco.forgeLimit", HexConfig.MAX_FORGES, 0, 100000, 0);
        intEntry(eb, cat, "eco.refundSilver", HexConfig.REFUND_SILVER, 0, 10000, 10);
        intEntry(eb, cat, "eco.refundGold", HexConfig.REFUND_GOLD, 0, 10000, 30);
        intEntry(eb, cat, "eco.refundPrismatic", HexConfig.REFUND_PRISMATIC, 0, 10000, 80);
    }

    private static void linkedCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.linked"));
        boolEntry(eb, cat, "linked.enableTaczContent", HexConfig.ENABLE_TACZ_CONTENT);
        boolEntry(eb, cat, "linked.enableTaczHextech", HexConfig.ENABLE_TACZ_HEXTECH);
        boolEntry(eb, cat, "linked.enableTaczForge", HexConfig.ENABLE_TACZ_FORGE);
        boolEntry(eb, cat, "linked.enableTaczEquip", HexConfig.ENABLE_TACZ_EQUIP);
    }

    private static void worldCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.world"));
        boolEntry(eb, cat, "world.enabled", HexConfig.WORLD_ENABLED);
        intEntry(eb, cat, "world.drawXp", HexConfig.WORLD_DRAW_XP, 10, 1000000, 2000);
        intEntry(eb, cat, "world.cancelCoin", HexConfig.WORLD_CANCEL_COIN, 0, 100000, 800);
        intEntry(eb, cat, "world.maxPerCategory", HexConfig.WORLD_MAX_PER_CATEGORY, 0, 24, 4);
    }

    private static void clientCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.client"));
        doubleEntry(eb, cat, "client.uiScale", HexConfig.UI_SCALE, 0.5, 2.0, 1.0);
        boolEntry(eb, cat, "client.particlesEnabled", HexConfig.PARTICLES_ENABLED);
    }

    private static void rulesCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.rules"));
        for (int i = 0; i < RULE_KEYS.length; i++) {
            final int idx = i;
            cat.addEntry(eb.startBooleanToggle(
                            Component.translatable("gamerule.hexworkshopKeep" + RULE_KEYS[i]),
                            ruleValue(idx))
                    .setSaveConsumer(v -> {
                        // 服务端权威：点击后发送"直接设置目标值"请求（v0.4.10 起不再翻转，界面显示与服务端一一对应）
                        Packets.C2SSetGameRule p = new Packets.C2SSetGameRule();
                        p.ruleIndex = idx;
                        p.value = v;
                        PacketHandler.CHANNEL.sendToServer(p);
                    })
                    .setTooltip(Component.translatable("gamerule.hexworkshopKeep" + RULE_KEYS[i] + ".description"))
                    .build());
        }
    }
}
