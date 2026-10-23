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
 * 六个分类：海克斯抽取 / 经济系统 / 联动内容 / 世界海克斯 / 客户端 / 游戏规则。
 * - 前五类直接绑定 HexConfig 的 ForgeConfigSpec 静态字段（即时保存 toml）；
 * - 游戏规则（死亡保留）v0.4.11 起以客户端配置为主：开关读写 HexConfig.KEEP_DEATH_*（主菜单即可保存，
 *   不依赖网络、不崩溃），已连接世界时额外推送服务端 gamerule；死亡处理 = 客户端配置 OR 服务端 gamerule 任一开启即保留。
 * 依赖 Cloth Config v10 API（cloth_config，正式客户端已安装 11.1.136）。
 */
public class HexConfigScreen {

    private HexConfigScreen() {}

    /** 死亡保留游戏规则 lang key 后缀 */
    private static final String[] RULE_KEYS = {"Hexes", "Forges", "Equips", "Coins"};

    /** 读取"生效值"（v0.4.11：客户端配置 OR 服务端 gamerule，任一开启即保留；主菜单时服务端同步值为默认，显示即客户端配置） */
    private static boolean ruleValue(int i) {
        switch (i) {
            case 0: return HexConfig.KEEP_DEATH_HEXES.get() || ClientDataStore.keepHexes;
            case 1: return HexConfig.KEEP_DEATH_FORGES.get() || ClientDataStore.keepForges;
            case 2: return HexConfig.KEEP_DEATH_EQUIPS.get() || ClientDataStore.keepEquips;
            default: return HexConfig.KEEP_DEATH_COINS.get() || ClientDataStore.keepCoins;
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
        v060Category(b, eb);
        dlcCategory(b, eb); // v0.6.2 DLC 外接内容包顶层页面
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

    /** v0.6.2 DLC 外接内容包顶层页面：扫描 hexworkshop_dlc/packs 展示包名/简介/版本/依赖/启用开关；
     *  isCodeDlc=true 的包不提供开关（置灰），提示放入 mods 文件夹并重启——本 DLC 系统不加载 Java 类代码 */
    private static void dlcCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.dlc"));
        // 客户端也扫描一次本地 packs 目录（服务端在 AddReloadListenerEvent 扫描）
        com.hexcraft.dlc.DlcManager.scan();
        cat.addEntry(eb.startTextDescription(Component.translatable("hexcraft.config.dlc.hint")).build());
        for (com.hexcraft.dlc.DlcManager.PackInfo p : com.hexcraft.dlc.DlcManager.packs()) {
            String title = p.name + (p.version.isEmpty() ? "" : "  v" + p.version);
            if (!p.error.isEmpty()) {
                cat.addEntry(eb.startTextDescription(Component.literal(title + "  [⚠ " + p.error + "]")
                        .withStyle(c -> c.withColor(0xFFFF6B6B))).build());
                continue;
            }
            if (p.isCodeDlc) {
                // 置灰提示：包含 Java 代码扩展，必须放入 mods 文件夹并重启游戏
                cat.addEntry(eb.startTextDescription(Component.literal(title)
                                .append(Component.literal("\n" + p.description).withStyle(c -> c.withColor(0xFF8FA6C8)))
                                .append(Component.translatable("hexcraft.config.dlc.code_hint").withStyle(c -> c.withColor(0xFFFFD27E))))
                        .build());
                continue;
            }
            cat.addEntry(eb.startBooleanToggle(
                            Component.translatable("hexcraft.config.dlc.toggle", title),
                            p.enabled)
                    .setSaveConsumer(v -> com.hexcraft.dlc.DlcManager.setEnabled(p.id, v))
                    .setDefaultValue(false)
                    .setTooltip(Component.literal(p.description),
                            p.dependencies.isEmpty() ? null :
                                    Component.translatable("hexcraft.config.dlc.deps", String.join(", ", p.dependencies)),
                            Component.translatable("hexcraft.config.dlc.note"))
                    .build());
        }
        if (com.hexcraft.dlc.DlcManager.packs().isEmpty()) {
            cat.addEntry(eb.startTextDescription(Component.translatable("hexcraft.config.dlc.empty")).build());
        }
    }

    private static void clientCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.client"));
        doubleEntry(eb, cat, "client.uiScale", HexConfig.UI_SCALE, 0.5, 2.0, 1.0);
        boolEntry(eb, cat, "client.particlesEnabled", HexConfig.PARTICLES_ENABLED);
    }

    /** v0.6.1：Cloth-Config 挂载 v060 分组（v0.6.0 的 4 项此前只存在于 toml、UI 未注册渲染节点）——
     *  额外护甲减伤阈值/每点减伤/封顶 + 同词条叠加规则 + v0.6.1 锻造档位倍率（服务端生效） */
    private static void v060Category(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.v060"));
        doubleEntry(eb, cat, "v060.extraArmorThreshold", HexConfig.EXTRA_ARMOR_THRESHOLD, 0.0, 200.0, 25.0);
        doubleEntry(eb, cat, "v060.extraArmorPerPoint", HexConfig.EXTRA_ARMOR_PER_POINT, 0.0, 5.0, 0.5);
        doubleEntry(eb, cat, "v060.extraArmorCap", HexConfig.EXTRA_ARMOR_CAP, 0.0, 80.0, 25.0);
        cat.addEntry(eb.startEnumSelector(name("v060.forgeDuplicateRule"), HexConfig.DuplicateRule.class, HexConfig.FORGE_DUPLICATE_RULE.get())
                .setSaveConsumer(v -> {
                    HexConfig.FORGE_DUPLICATE_RULE.set(v);
                    HexConfig.COMMON_SPEC.save();
                })
                .setDefaultValue(HexConfig.DuplicateRule.STACK)
                .setTooltip(tip("v060.forgeDuplicateRule"))
                .build());
        doubleEntry(eb, cat, "v060.forgeMultSilver", HexConfig.FORGE_MULT_SILVER, 0.1, 20.0, 1.0);
        doubleEntry(eb, cat, "v060.forgeMultGold", HexConfig.FORGE_MULT_GOLD, 0.1, 20.0, 2.0);
        doubleEntry(eb, cat, "v060.forgeMultPrismatic", HexConfig.FORGE_MULT_PRISMATIC, 0.1, 20.0, 3.0);
        // v0.6.2 永久叠层全局倍率/上限（服务端生效，防作弊）
        doubleEntry(eb, cat, "v060.permanentStackMult", HexConfig.PERMANENT_STACK_MULT, 0.1, 10.0, 1.0);
        intEntry(eb, cat, "v060.permanentStackCap", HexConfig.PERMANENT_STACK_CAP, 10, 10000, 200);
    }

    private static void rulesCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.rules"));
        for (int i = 0; i < RULE_KEYS.length; i++) {
            final int idx = i;
            cat.addEntry(eb.startBooleanToggle(
                            Component.translatable("gamerule.hexworkshopKeep" + RULE_KEYS[i]),
                            ruleValue(idx))
                    .setSaveConsumer(v -> {
                        // v0.4.11：以客户端配置为主——先写本地配置（主菜单也能保存、不依赖网络、不崩溃）
                        switch (idx) {
                            case 0: HexConfig.KEEP_DEATH_HEXES.set(v); break;
                            case 1: HexConfig.KEEP_DEATH_FORGES.set(v); break;
                            case 2: HexConfig.KEEP_DEATH_EQUIPS.set(v); break;
                            default: HexConfig.KEEP_DEATH_COINS.set(v); break;
                        }
                        // 已连接世界时额外推送服务端 gamerule（世界内点击即时生效、联机同步）；
                        // 主菜单无连接时仅本地生效（getConnection()==null 时直接 sendToServer 会 NPE，v0.4.10 崩溃根因）
                        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                        if (mc.getConnection() != null) {
                            Packets.C2SSetGameRule p = new Packets.C2SSetGameRule();
                            p.ruleIndex = idx;
                            p.value = v;
                            PacketHandler.CHANNEL.sendToServer(p);
                        } else if (mc.player != null) {
                            mc.player.displayClientMessage(
                                    Component.translatable("message.hexcraft.gamerule_local_only"), true);
                        }
                    })
                    .setTooltip(
                            Component.translatable("gamerule.hexworkshopKeep" + RULE_KEYS[i] + ".description"),
                            Component.translatable("message.hexcraft.gamerule_client_primary"),
                            Component.translatable("message.hexcraft.gamerule_mp_note"))
                    .build());
        }
    }
}
