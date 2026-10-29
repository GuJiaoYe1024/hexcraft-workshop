package com.hexcraft.client.gui;

import com.hexcraft.client.ClientDataStore;
import com.hexcraft.config.HexConfig;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import java.util.List;
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
        hudCategory(b, eb); // v0.6.16 HUD 优化：开关类条目
        layoutCategory(b, eb); // v0.7.1 界面布局：位置坐标 + 全屏布局编辑器
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

    // v0.8.0 子分类支持：以下 *EntryEntry 返回条目对象（供 startSubCategory 嵌套），不直接 addEntry
    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> intEntryEntry(
            ConfigEntryBuilder eb, String field, ForgeConfigSpec.IntValue spec, int min, int max, int def) {
        return eb.startIntField(name(field), spec.get())
                .setSaveConsumer(v -> setInt(spec, v))
                .setDefaultValue(def)
                .setMin(min).setMax(max)
                .setTooltip(tip(field))
                .build();
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> doubleEntryEntry(
            ConfigEntryBuilder eb, String field, ForgeConfigSpec.DoubleValue spec, double min, double max, double def) {
        return eb.startDoubleField(name(field), spec.get())
                .setSaveConsumer(v -> setDouble(spec, v))
                .setDefaultValue(def)
                .setMin(min).setMax(max)
                .setTooltip(tip(field))
                .build();
    }

    private static me.shedaniel.clothconfig2.api.AbstractConfigListEntry<?> boolEntryEntry(
            ConfigEntryBuilder eb, String field, ForgeConfigSpec.BooleanValue spec) {
        return eb.startBooleanToggle(name(field), spec.get())
                .setSaveConsumer(v -> {
                    spec.set(v);
                    spec.save();
                })
                .setDefaultValue(spec.getDefault())
                .setTooltip(tip(field))
                .build();
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
        intEntry(eb, cat, "draw.hexLimit", HexConfig.HEX_SLOT_COUNT, 0, 9999, 16);
        intEntry(eb, cat, "draw.silverWeight", HexConfig.WEIGHT_SILVER, 0, 1000, 40);
        intEntry(eb, cat, "draw.goldWeight", HexConfig.WEIGHT_GOLD, 0, 1000, 35);
        intEntry(eb, cat, "draw.prismWeight", HexConfig.WEIGHT_PRISMATIC, 0, 1000, 25);
        intEntry(eb, cat, "draw.pityGoldDraws", HexConfig.PITY_GOLD, 0, 100, 4);
        intEntry(eb, cat, "draw.pityPrismDraws", HexConfig.PITY_PRISMATIC, 0, 200, 8);
        intEntry(eb, cat, "draw.maxHextechLevel", HexConfig.MAX_HEXTECH_LEVEL, 1, 10, 3);
    }

    /** v0.8.0 子分类重组：击杀掉落 / 防刷机制 / 锻造价格与退款，避免 16 项平铺难找 */
    private static void economyCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.economy"));
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.drop"), List.of(
                intEntryEntry(eb, "eco.coinMin", HexConfig.COIN_MIN, 0, 100, 1),
                intEntryEntry(eb, "eco.coinMax", HexConfig.COIN_MAX, 0, 100, 3),
                intEntryEntry(eb, "eco.coinEliteMin", HexConfig.COIN_ELITE_MIN, 0, 1000, 5),
                intEntryEntry(eb, "eco.coinEliteMax", HexConfig.COIN_ELITE_MAX, 0, 1000, 10),
                intEntryEntry(eb, "eco.coinBossMin", HexConfig.COIN_BOSS_MIN, 0, 10000, 20),
                intEntryEntry(eb, "eco.coinBossMax", HexConfig.COIN_BOSS_MAX, 0, 10000, 50)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.antifarm"), List.of(
                intEntryEntry(eb, "eco.antiFarmWindowSec", HexConfig.ANTI_FARM_WINDOW_SEC, 0, 600, 30),
                doubleEntryEntry(eb, "eco.antiFarmDecay", HexConfig.ANTI_FARM_DECAY, 0.0, 1.0, 0.2)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.price"), List.of(
                intEntryEntry(eb, "eco.forgeBasicPrice", HexConfig.FORGE_BASIC_PRICE, 0, 100000, 50),
                intEntryEntry(eb, "eco.forgeGoldPrice", HexConfig.FORGE_GOLD_PRICE, 0, 100000, 150),
                intEntryEntry(eb, "eco.forgePrismaticPrice", HexConfig.FORGE_PRISMATIC_PRICE, 0, 100000, 400),
                intEntryEntry(eb, "eco.forgeLimit", HexConfig.MAX_FORGES, 0, 100000, 0),
                intEntryEntry(eb, "eco.refundSilver", HexConfig.REFUND_SILVER, 0, 10000, 10),
                intEntryEntry(eb, "eco.refundGold", HexConfig.REFUND_GOLD, 0, 10000, 30),
                intEntryEntry(eb, "eco.refundPrismatic", HexConfig.REFUND_PRISMATIC, 0, 10000, 80)
        )).build());
    }

    /** v0.8.0 子分类重组：按联动 mod 分组（每组 4 开关：总开关/海克斯/锻造/装备），避免 24 项平铺 */
    private static void linkedCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.linked"));
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.tacz"), List.of(
                boolEntryEntry(eb, "linked.enableTaczContent", HexConfig.ENABLE_TACZ_CONTENT),
                boolEntryEntry(eb, "linked.enableTaczHextech", HexConfig.ENABLE_TACZ_HEXTECH),
                boolEntryEntry(eb, "linked.enableTaczForge", HexConfig.ENABLE_TACZ_FORGE),
                boolEntryEntry(eb, "linked.enableTaczEquip", HexConfig.ENABLE_TACZ_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.irons"), List.of(
                boolEntryEntry(eb, "linked.enableIronsContent", HexConfig.ENABLE_IRONS_CONTENT),
                boolEntryEntry(eb, "linked.enableIronsHextech", HexConfig.ENABLE_IRONS_HEXTECH),
                boolEntryEntry(eb, "linked.enableIronsForge", HexConfig.ENABLE_IRONS_FORGE),
                boolEntryEntry(eb, "linked.enableIronsEquip", HexConfig.ENABLE_IRONS_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.iaf"), List.of(
                boolEntryEntry(eb, "linked.enableIafContent", HexConfig.ENABLE_IAF_CONTENT),
                boolEntryEntry(eb, "linked.enableIafHextech", HexConfig.ENABLE_IAF_HEXTECH),
                boolEntryEntry(eb, "linked.enableIafForge", HexConfig.ENABLE_IAF_FORGE),
                boolEntryEntry(eb, "linked.enableIafEquip", HexConfig.ENABLE_IAF_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.ac"), List.of(
                boolEntryEntry(eb, "linked.enableAcContent", HexConfig.ENABLE_AC_CONTENT),
                boolEntryEntry(eb, "linked.enableAcHextech", HexConfig.ENABLE_AC_HEXTECH),
                boolEntryEntry(eb, "linked.enableAcForge", HexConfig.ENABLE_AC_FORGE),
                boolEntryEntry(eb, "linked.enableAcEquip", HexConfig.ENABLE_AC_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.tf"), List.of(
                boolEntryEntry(eb, "linked.enableTfContent", HexConfig.ENABLE_TF_CONTENT),
                boolEntryEntry(eb, "linked.enableTfHextech", HexConfig.ENABLE_TF_HEXTECH),
                boolEntryEntry(eb, "linked.enableTfForge", HexConfig.ENABLE_TF_FORGE),
                boolEntryEntry(eb, "linked.enableTfEquip", HexConfig.ENABLE_TF_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.am"), List.of(
                boolEntryEntry(eb, "linked.enableAmContent", HexConfig.ENABLE_AM_CONTENT),
                boolEntryEntry(eb, "linked.enableAmHextech", HexConfig.ENABLE_AM_HEXTECH),
                boolEntryEntry(eb, "linked.enableAmForge", HexConfig.ENABLE_AM_FORGE),
                boolEntryEntry(eb, "linked.enableAmEquip", HexConfig.ENABLE_AM_EQUIP)
        )).build());
        // v0.8.4 第二批联动逐组独立子分组（每组 3 开关：总开关/海克斯/装备；对齐 TACZ/法术/冰火等 4 开关样式，装备由 v0.8.4 新增）
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.cataclysm"), List.of(
                boolEntryEntry(eb, "linked.enableCataclysmContent", HexConfig.ENABLE_CATACLYSM_CONTENT),
                boolEntryEntry(eb, "linked.enableCataclysmHextech", HexConfig.ENABLE_CATACLYSM_HEXTECH),
                boolEntryEntry(eb, "linked.enableCataclysmEquip", HexConfig.ENABLE_CATACLYSM_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.goety"), List.of(
                boolEntryEntry(eb, "linked.enableGoetyContent", HexConfig.ENABLE_GOETY_CONTENT),
                boolEntryEntry(eb, "linked.enableGoetyHextech", HexConfig.ENABLE_GOETY_HEXTECH),
                boolEntryEntry(eb, "linked.enableGoetyEquip", HexConfig.ENABLE_GOETY_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.mowziesmobs"), List.of(
                boolEntryEntry(eb, "linked.enableMowziesmobsContent", HexConfig.ENABLE_MOWZIESMOBS_CONTENT),
                boolEntryEntry(eb, "linked.enableMowziesmobsHextech", HexConfig.ENABLE_MOWZIESMOBS_HEXTECH),
                boolEntryEntry(eb, "linked.enableMowziesmobsEquip", HexConfig.ENABLE_MOWZIESMOBS_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.artifacts"), List.of(
                boolEntryEntry(eb, "linked.enableArtifactsContent", HexConfig.ENABLE_ARTIFACTS_CONTENT),
                boolEntryEntry(eb, "linked.enableArtifactsHextech", HexConfig.ENABLE_ARTIFACTS_HEXTECH),
                boolEntryEntry(eb, "linked.enableArtifactsEquip", HexConfig.ENABLE_ARTIFACTS_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.relics"), List.of(
                boolEntryEntry(eb, "linked.enableRelicsContent", HexConfig.ENABLE_RELICS_CONTENT),
                boolEntryEntry(eb, "linked.enableRelicsHextech", HexConfig.ENABLE_RELICS_HEXTECH),
                boolEntryEntry(eb, "linked.enableRelicsEquip", HexConfig.ENABLE_RELICS_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.slashblade"), List.of(
                boolEntryEntry(eb, "linked.enableSlashbladeContent", HexConfig.ENABLE_SLASHBLADE_CONTENT),
                boolEntryEntry(eb, "linked.enableSlashbladeHextech", HexConfig.ENABLE_SLASHBLADE_HEXTECH),
                boolEntryEntry(eb, "linked.enableSlashbladeEquip", HexConfig.ENABLE_SLASHBLADE_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.farmersdelight"), List.of(
                boolEntryEntry(eb, "linked.enableFarmersdelightContent", HexConfig.ENABLE_FARMERSDELIGHT_CONTENT),
                boolEntryEntry(eb, "linked.enableFarmersdelightHextech", HexConfig.ENABLE_FARMERSDELIGHT_HEXTECH),
                boolEntryEntry(eb, "linked.enableFarmersdelightEquip", HexConfig.ENABLE_FARMERSDELIGHT_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.ftbquests"), List.of(
                boolEntryEntry(eb, "linked.enableFtbquestsContent", HexConfig.ENABLE_FTBQUESTS_CONTENT),
                boolEntryEntry(eb, "linked.enableFtbquestsHextech", HexConfig.ENABLE_FTBQUESTS_HEXTECH),
                boolEntryEntry(eb, "linked.enableFtbquestsEquip", HexConfig.ENABLE_FTBQUESTS_EQUIP)
        )).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.emc"), List.of(
                boolEntryEntry(eb, "linked.enableEmcContent", HexConfig.ENABLE_EMC_CONTENT),
                boolEntryEntry(eb, "linked.enableEmcHextech", HexConfig.ENABLE_EMC_HEXTECH)
        )).build());
    }

    private static void worldCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.world"));
        boolEntry(eb, cat, "world.enabled", HexConfig.WORLD_ENABLED);
        intEntry(eb, cat, "world.drawXp", HexConfig.WORLD_DRAW_XP, 10, 1000000, 2000);
        intEntry(eb, cat, "world.cancelCoin", HexConfig.WORLD_CANCEL_COIN, 0, 100000, 800);
        intEntry(eb, cat, "world.maxPerCategory", HexConfig.WORLD_MAX_PER_CATEGORY, 0, 9999, 16);
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
        // v0.6.3 抽取/锻造完成后返回工坊界面（玩家可选：继续连抽 or 直接关闭）
        boolEntry(eb, cat, "client.keepWorkshopOpen", HexConfig.KEEP_WORKSHOP_OPEN);
        // v0.6.3 三选一「选中→确认」（防手滑）
        boolEntry(eb, cat, "client.confirmSelect", HexConfig.CONFIRM_SELECT);
    }

    /** v0.6.1：Cloth-Config 挂载 v060 分组；v0.8.0 子分类重组（护甲/锻造/叠层/装备/法术/联动倍率/防卡顿），
     *  避免 24 项杂糅难找。所有项服务端生效，未安装对应 mod 时无实际效果 */
    private static void v060Category(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.v060"));
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.armor"), List.of(
                doubleEntryEntry(eb, "v060.extraArmorThreshold", HexConfig.EXTRA_ARMOR_THRESHOLD, 0.0, 200.0, 25.0),
                doubleEntryEntry(eb, "v060.extraArmorPerPoint", HexConfig.EXTRA_ARMOR_PER_POINT, 0.0, 5.0, 0.5),
                doubleEntryEntry(eb, "v060.extraArmorCap", HexConfig.EXTRA_ARMOR_CAP, 0.0, 80.0, 25.0)
        )).setExpanded(true).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.forge"), List.of(
                eb.startEnumSelector(name("v060.forgeDuplicateRule"), HexConfig.DuplicateRule.class, HexConfig.FORGE_DUPLICATE_RULE.get())
                        .setSaveConsumer(v -> {
                            HexConfig.FORGE_DUPLICATE_RULE.set(v);
                            HexConfig.COMMON_SPEC.save();
                        })
                        .setDefaultValue(HexConfig.DuplicateRule.STACK)
                        .setTooltip(tip("v060.forgeDuplicateRule"))
                        .build(),
                doubleEntryEntry(eb, "v060.forgeMultSilver", HexConfig.FORGE_MULT_SILVER, 0.1, 20.0, 1.0),
                doubleEntryEntry(eb, "v060.forgeMultGold", HexConfig.FORGE_MULT_GOLD, 0.1, 20.0, 2.0),
                doubleEntryEntry(eb, "v060.forgeMultPrismatic", HexConfig.FORGE_MULT_PRISMATIC, 0.1, 20.0, 3.0),
                intEntryEntry(eb, "v060.forgeSlotMax", HexConfig.FORGE_SLOT_MAX, 0, 9999, 99)
        )).setExpanded(true).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.stack"), List.of(
                doubleEntryEntry(eb, "v060.permanentStackMult", HexConfig.PERMANENT_STACK_MULT, 0.1, 10.0, 1.0),
                intEntryEntry(eb, "v060.permanentStackCap", HexConfig.PERMANENT_STACK_CAP, 0, 9999, 200),
                doubleEntryEntry(eb, "v060.stackDrakeMult", HexConfig.STACK_DRAKE_MULT, 0.0, 3.0, 0.75)
        )).setExpanded(true).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.equipval"), List.of(
                intEntryEntry(eb, "v060.equipGoldCost", HexConfig.EQUIP_GOLD_COST, 0, 1000000, 2500),
                intEntryEntry(eb, "v060.equipPrismaticCost", HexConfig.EQUIP_PRISMATIC_COST, 0, 1000000, 4000),
                doubleEntryEntry(eb, "v060.equipCountMult", HexConfig.EQUIP_COUNT_MULT, 0.0, 10.0, 1.0)
        )).setExpanded(true).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.ironsval"), List.of(
                doubleEntryEntry(eb, "v060.ironsEffectMult", HexConfig.IRONS_EFFECT_MULT, 0.0, 10.0, 1.0),
                doubleEntryEntry(eb, "v060.ironsCritMult", HexConfig.IRONS_CRIT_MULT, 1.0, 5.0, 1.75),
                doubleEntryEntry(eb, "v060.ironsExecuteThreshold", HexConfig.IRONS_EXECUTE_THRESHOLD, 0.01, 1.0, 0.30),
                doubleEntryEntry(eb, "v060.ironsCastHaste", HexConfig.IRONS_CAST_HASTE, 0.0, 1.0, 0.08),
                doubleEntryEntry(eb, "v060.ironsManaMax", HexConfig.IRONS_MANA_MAX, 0.0, 1000.0, 4.0),
                doubleEntryEntry(eb, "v060.ironsManaRegen", HexConfig.IRONS_MANA_REGEN, 0.0, 100.0, 0.5)
        )).setExpanded(true).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.linkmult"), List.of(
                doubleEntryEntry(eb, "v060.iafEffectMult", HexConfig.IAF_EFFECT_MULT, 0.0, 10.0, 1.0),
                doubleEntryEntry(eb, "v060.acEffectMult", HexConfig.AC_EFFECT_MULT, 0.0, 10.0, 1.0),
                doubleEntryEntry(eb, "v060.tfEffectMult", HexConfig.TF_EFFECT_MULT, 0.0, 10.0, 1.0),
                doubleEntryEntry(eb, "v060.amEffectMult", HexConfig.AM_EFFECT_MULT, 0.0, 10.0, 1.0)
        )).setExpanded(true).build());
        cat.addEntry(eb.startSubCategory(Component.translatable("hexcraft.config.sub.perf"), List.of(
                boolEntryEntry(eb, "v060.perfTipThrottle", HexConfig.PERF_TIP_THROTTLE)
        )).setExpanded(true).build());
    }

    /** v0.6.16 HUD 优化分组：开关类条目（滚动提示开关 / 拖动模式 / 队列上限 / 合并 / 血条开关 / 属性安全钳）；
     *  v0.7.1 位置类条目（TIP/HP/TAB 坐标 + 布局编辑器）已拆分到独立「界面布局」分组，避免本页条目拥挤。 */
    private static void hudCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.v060hud"));
        // 生命值显示优化（默认 0=关闭，与其它 mod 零冲突；开启后钳玩家自身最大生命/护甲/韧性到该值，
        // 防高数值整合包血量 HUD 渲染溢出——屏幕中央黄/红异常竖条）
        intEntry(eb, cat, "v060.attrDisplayCap", HexConfig.ATTR_DISPLAY_CAP, 0, 100000000, 0);
        // 滚动提示（随机馈赠/入账等滚动消息）：总开关 + 拖动调整模式
        boolEntry(eb, cat, "v060.tipEnabled", HexConfig.TIP_ENABLED);
        boolEntry(eb, cat, "v060.tipAdjustMode", HexConfig.TIP_ADJUST_MODE);
        // v0.6.22 防卡顿（客户端渲染端生效）：队列上限 + 同文本合并 + v0.7.6 同屏条数
        intEntry(eb, cat, "v060.perfTipCap", HexConfig.PERF_TIP_CAP, 0, 100, 6);
        boolEntry(eb, cat, "v060.perfTipDedup", HexConfig.PERF_TIP_DEDUP);
        intEntry(eb, cat, "v060.perfTipMax", HexConfig.PERF_TIP_MAX, 1, 8, 5);
        // 海克斯风简易血条（v0.7.0 三合一：生命/护甲/饱食；默认关闭，开启后取代原版三原条）
        boolEntry(eb, cat, "v060.hpEnabled", HexConfig.HP_ENABLED);
    }

    /** v0.7.1 界面布局分组：滚动提示 / 状态栏 / 背包页签 的位置坐标（可直接手填精确值）。
     *  v0.7.2 拖动改走游戏内按键（默认 L 弹半透明画布），本页不再放按钮条目。 */
    private static void layoutCategory(ConfigBuilder b, ConfigEntryBuilder eb) {
        ConfigCategory cat = b.getOrCreateCategory(Component.translatable("hexcraft.config.cat.v070layout"));
        // 滚动提示位置
        intEntry(eb, cat, "v060.tipX", HexConfig.TIP_X, 0, 4000, 8);
        intEntry(eb, cat, "v060.tipY", HexConfig.TIP_Y, 0, 4000, 34);
        // 海克斯状态栏位置（三合一：生命/护甲/饱食）
        intEntry(eb, cat, "v060.hpX", HexConfig.HP_X, 0, 4000, 0);
        intEntry(eb, cat, "v060.hpY", HexConfig.HP_Y, 0, 4000, 0);
        // 背包/合成界面页签位置（v0.7.1 新增配置化）
        intEntry(eb, cat, "v070.tabX", HexConfig.TAB_X, 0, 4000, 0);
        intEntry(eb, cat, "v070.tabY", HexConfig.TAB_Y, 0, 4000, 0);
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
