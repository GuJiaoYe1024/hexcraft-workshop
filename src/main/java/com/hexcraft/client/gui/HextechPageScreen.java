package com.hexcraft.client.gui;

import com.hexcraft.capability.PlayerData;
import com.hexcraft.client.ClientDataStore;
import com.hexcraft.config.HexConfig;
import com.hexcraft.event.HextechEvents;
import com.hexcraft.hextech.HextechDefinition;
import com.hexcraft.hextech.HextechRegistry;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 海克斯主界面（海克斯科技风格）：
 * 左侧竖排导航（属性 / 海克斯 / 锻造器 / 装备），右侧内容区；深蓝紫渐变 + 霓虹光效。
 * v0.4.4：接入 UI 缩放（配置「界面缩放」整体放大/缩小）；修复锻造器页文字重叠（标题/按钮/词条行距）。
 */
public class HextechPageScreen extends HexScaledScreen {
    private static final int W = 304;
    private static final int H = 300;
    private static final int NAV_W = 54;
    /** 属性页行高（14px 防文字挤压）与海克斯页行高 */
    private static final int ROW_H = 14;
    private static final int HEX_ROW_H = 13;
    private static final String[] TIER_KEYS = {"basic", "gold", "prismatic"};
    private static final String[] ATTR_KEYS = {
            "max_health", "armor", "tough", "attack_damage", "attack_speed",
            "movement_speed", "luck", "knockback_resistance", "attack_knockback"};
    /** v0.9.12 基础属性里的不常用次要项（幸运、击退抗性、击退），可折叠收起 */
    private static final String[] MINOR_KEYS = {"luck", "knockback_resistance", "attack_knockback"};
    /** v0.9.12 顶部锚点对应的四个框标题 lang 键 */
    private static final String[] ANCHOR_GROUP_KEYS = {"group_base", "group_hidden", "group_stack", "group_combo"};
    /** 效果属性（隐藏属性）：effect key → lang key；percent=true 按百分比显示 */
    private static final String[][] EFFECT_KEYS = {
            {"crit", "crit", "1"}, {"melee_power", "melee_power", "1"}, {"ranged_power", "ranged_power", "1"},
            {"lifesteal", "lifesteal", "1"}, {"thorns", "thorns", "0"}, {"jump", "jump", "0"},
            {"dig_speed", "dig_speed", "0"}, {"regen", "regen", "0"}, {"kill_heal", "kill_heal", "0"},
            {"coin_bonus", "coin_bonus", "1"}, {"xp_bonus", "xp_bonus", "1"},
            {"low_hp_power", "low_hp_power", "1"}, {"full_hp_crit", "full_hp_crit", "1"},
            {"night_power", "night_power", "1"}, {"kill_streak", "kill_streak", "1"},
            {"last_stand", "last_stand", "1"}, {"stack_growth", "stack_growth", "1"},
            {"stack_chance", "stack_chance", "1"},
            // —— 复合机制海克斯（v0.1.11 新增）——
            {"echo", "echo", "1"}, {"execute", "execute", "1"}, {"splash", "splash", "1"},
            {"counter", "counter", "1"}, {"shield_low", "shield_low", "0"}, {"rampage", "rampage", "1"},
            {"mobi_power", "mobi_power", "1"}, {"overshield", "overshield", "1"}, {"surge", "surge", "1"},
            {"arcane_shield", "arcane_shield", "1"}, {"bounty", "bounty", "0"}, {"fire_chain", "fire_chain", "0"},
            {"vamp_rage", "vamp_rage", "1"}, {"twin_shot", "twin_shot", "1"},
            {"boss_power", "boss_power", "1"}, {"frost_bite", "frost_bite", "1"}, {"death_defy", "death_defy", "1"},
            // —— 娱乐机制（v0.3.0 新增）——
            {"firework", "firework", "0"}, {"xp_drop", "xp_drop", "0"}, {"gold_punch", "gold_punch", "0"},
            // —— v0.9.7 大乱斗套装/逆原羁绊机制键（属性页显示）——
            {"relic_drop", "relic_drop", "1"}, {"equip_power", "equip_power", "1"},
            // —— v0.9.12 数据化效果键（六神装 / 栏位扩展）——
            {"equip_count_atk", "equip_count_atk", "0"}, {"equip_count_armor", "equip_count_armor", "0"},
            {"equip_count_hp", "equip_count_hp", "0"}, {"slot_boost", "slot_boost", "0"},
            {"spell_mana_max", "spell_mana_max", "1"}, {"tf_snow_queen_damage", "tf_snow_queen_damage", "1"},
            {"stand_power", "stand_power", "0"}};
    /** TACZ 联动效果：仅安装 TACZ 时显示 */
    private static final String[][] TACZ_KEYS = {
            {"tacz_power", "tacz_power", "1"}, {"tacz_crit", "tacz_crit", "1"},
            {"tacz_close_power", "tacz_close_power", "1"}, {"tacz_far_power", "tacz_far_power", "1"},
            {"tacz_heal", "tacz_heal", "0"}};
    private static final String[] TABS = {"overview", "hexes", "bonds", "forge", "equip", "recharge"};
    /** v0.9.12 修正：页签索引具名化。v0.9.7 在 index 2 插入「羁绊」页签后，
     *  抽取/锻造/装备三个界面返回工坊时仍用旧硬编码索引（锻造回 2=羁绊页、装备回 3=锻造页），
     *  改为常量化避免页签顺序再调整时再次错位。 */
    public static final int TAB_OVERVIEW = 0;
    public static final int TAB_HEXES = 1;
    public static final int TAB_BONDS = 2;
    public static final int TAB_FORGE = 3;
    public static final int TAB_EQUIP = 4;
    public static final int TAB_RECHARGE = 5;
    private static final int ACCENT = 0xFF4FD8FF;
    private static final int ACCENT_VIOLET = 0xFF7A6BFF;
    /** v0.9.7 v2-A：羁绊页签组头颜色（原生/新流派/叠层专家/逆原/大乱斗/叠层之源） */
    private static final int[] GROUP_COLORS = {0xFF4FD8FF, 0xFFFFD700, 0xFFC77DFF, 0xFF8AB4FF, 0xFFFF9FE0, 0xFF7AFFB2};

    private int tab;
    private int hexScroll;
    private int attrScroll;
    private int forgeScroll;
    // v0.6.5 装备页图鉴模式：equipGallery=true 时装备页显示全部装备图鉴（可滚动浏览未拥有装备）
    private boolean equipGallery;
    private int galleryScroll;
    /** 自绘列表 hover 提示：鼠标位置与当前命中行的提示内容（render 每帧重置，末尾绘制） */
    private int hoverX;
    private int hoverY;
    private java.util.List<Object> hoverTip = null;
    /** v0.9.12 hover 可视区裁剪：行滚出这个区间后不再响应悬停（每帧按内容区重设） */
    private int hoverClipTop = Integer.MIN_VALUE;
    private int hoverClipBottom = Integer.MAX_VALUE;
    /** 海克斯页分类过滤：0=全部 1=战斗 2=生存 3=成长 4=经济 5=枪械 6=附魔 7=矿物 8=趣味 */
    private int hexCat = 0;
    /** 分类过滤缓存：分类或数据版本变化时重建，避免每帧重复过滤（hexCat=0 直接返回原列表，不缓存） */
    private int cachedVisCat = -1;
    private int cachedVisVersion = -1;
    private java.util.List<PlayerData.HexEntry> cachedVisible = null;
    /** 图鉴总数缓存：额外校验注册表大小，覆盖注册表同步但 dataVersion 未递增的场景 */
    private int cachedTotalCat = -1;
    private int cachedTotalVersion = -1;
    private int cachedTotalRegSize = -1;
    private int cachedTotal = 0;
    private static final String[] HEX_CATS = {"all", "combat", "survive", "growth", "economy", "gun", "enchant", "mining", "fun"};
    private static final int[] HEX_CAT_COLORS = {
            0xFF4FD8FF, 0xFFFF6B6B, 0xFF55FF88, 0xFFFFD700, 0xFF8AB4FF,
            0xFFC77DFF, 0xFF6BE4FF, 0xFFFFC94A, 0xFFFF9FE0};
    private int navY[] = new int[6];
    /** v0.9.7 v2-A：羁绊页签滚动（47 条全量图鉴） */
    private int bondScroll;
    /** v0.9.12 四个框折叠状态：0 基础 1 隐藏 2 叠层 3 羁绊 */
    private final boolean[] boxCollapsed = new boolean[4];
    /** v0.9.12 基础属性框内不常用次要组折叠状态 */
    private boolean minorCollapsed;
    /** v0.9.12 各框标题栏点击区（每帧渲染时记录，鼠标点击时判定）；每行 {左, 上, 右, 下, 框序号} */
    private final java.util.List<int[]> titleHitboxes = new java.util.ArrayList<>();
    /** v0.9.12 次要组标题点击区 */
    private int[] minorHitbox;
    /** v0.9.12 顶部四个锚点点击区：每行 {左, 上, 右, 下, 框序号} */
    private final java.util.List<int[]> anchorHitboxes = new java.util.ArrayList<>();
    private final java.util.List<float[]> parts = new java.util.ArrayList<>();
    private int pTick;
    /** v0.6.3 上次渲染的费用快照版本（tick 检测变化重建按钮） */
    private int lastCostVersion = -1;
    private boolean partsInit;
    private static final java.util.Random P_RAND = new java.util.Random();

    public HextechPageScreen() {
        super(Component.translatable("hexcraft.gui.page_title"));
    }

    /** v0.6.0 数据热刷新：重建界面时保持当前页签 */
    public int currentTab() { return tab; }

    /** v0.6.0 数据热刷新：重建界面时恢复原页签（init 前设置） */
    public void setInitialTab(int t) { tab = t; }

    /** v0.9.12 数据自动刷新：重建界面时完整保留视图状态（页签 + 各页滚动 + 分类筛选 + 图鉴模式）。
     *  旧实现只保留页签，数据一变（叠层增长/抽取/锻造完成）界面就弹回顶部、分类筛选可能重置，
     *  玩家正在查看的位置被打断。改为一次性复制全部视图字段。 */
    public void copyViewStateFrom(HextechPageScreen o) {
        if (o == null) return;
        this.tab = o.tab;
        this.hexScroll = o.hexScroll;
        this.attrScroll = o.attrScroll;
        this.forgeScroll = o.forgeScroll;
        this.galleryScroll = o.galleryScroll;
        this.equipGallery = o.equipGallery;
        this.hexCat = o.hexCat;
    }

    @Override
    protected void init() {
        int x = (this.guiW() - W) / 2;
        int y = (this.guiH() - H) / 2;

        // 左侧导航（竖排五页签：充值 / 属性 / 海克斯 / 锻造器 / 装备；界面加高后拉大间距）
        for (int i = 0; i < TABS.length; i++) {
            navY[i] = y + 26 + i * 38;
            final int t = i;
            this.addRenderableWidget(new NavButton(x + 5, navY[i], NAV_W, 26,
                    Component.translatable("hexcraft.gui.tab." + TABS[i]), t == tab, b -> switchTab(t)));
        }

        // 返回
        this.addRenderableWidget(new HexButton(x + W - 64, y + H - 24, 58, 18,
                Component.translatable("gui.back"), 0xFF8A93A8, b -> this.onClose()));

        if (tab == 0) initOverview();
        else if (tab == 1) initHexes();
        else if (tab == 2) initBonds(); // v0.9.7 v2-A 羁绊页签（纯自绘列表，无控件）
        else if (tab == 3) initForge();
        else if (tab == 4) initEquip();
        else initRecharge();
    }

    private void switchTab(int t) {
        if (tab == t) return;
        tab = t;
        this.rebuildWidgets(); // 关键：清除旧页签控件，避免残留
    }

    // ================= 各页控件 =================

    private void initOverview() {
        // 属性页不再内嵌兑换按钮（充值已独立为最左侧「充值」页签）
    }

    /** v0.9.7 v2-A：羁绊页签（纯自绘列表，无控件） */
    private void initBonds() {
    }

    /** 充值材质表：0绿宝石 1铜 2铁 3金 4钻石 5下界合金；每行标签 = 材质 ×单枚晶币
     *  v0.9.9 用户拍板按 EMC 价值锚定（1 晶币 = 1 铁锭 256 EMC）：绿64 铜2 铁1 金8 钻32 下界224；晶币→绿宝石同锚 64 */
    private static final int[] RECHARGE_RATE = {64, 2, 1, 8, 32, 224};
    private static final int[] RECHARGE_COUNTS = {1, 32, 64, 128, 256};

    /** 充值页：6 种材质（绿宝石+全部金属锭）→ 晶币，晶币 → 绿宝石；五档批量 ×1/×32/×64/×128/×256 */
    private void initRecharge() {
        // 6 材质两行式：每块 30px 行距（标签在上、按钮在下留出间距；按钮按对应金属锭配色）
        int[] cols = {0xFF55FF55, 0xFFFF8C00, 0xFFD8D8D8, 0xFFFFD700, 0xFF4DE6FF, 0xFFC77DFF};
        int by = cy() + 64;
        for (int m = 0; m < 6; m++) {
            final int mat = m;
            int col = cols[m];
            for (int i = 0; i < RECHARGE_COUNTS.length; i++) {
                final int c = RECHARGE_COUNTS[i];
                this.addRenderableWidget(new HexButton(cx() + 4 + i * 40, by + 15, 36, 12,
                        Component.translatable("hexcraft.gui.recharge_gem", c), col,
                        b -> sendExchange(0, mat, c)));
            }
            by += 30;
        }
        // 晶币 → 绿宝石（与上面材质行对齐；标签与按钮间距 8px）
        int cy2 = cy() + 64 + 6 * 30 + 16;
        for (int i = 0; i < RECHARGE_COUNTS.length; i++) {
            final int c = RECHARGE_COUNTS[i];
            this.addRenderableWidget(new HexButton(cx() + 4 + i * 40, cy2, 36, 12,
                    Component.translatable("hexcraft.gui.recharge_coin", c), 0xFFFFD700,
                    b -> sendExchange(1, 0, c)));
        }
        // v0.9.9 EMC 通道（ProjectE 已装时显示）：玩家 EMC 余额直充——档位 100 晶币起步（100/200/400/800），
        // 点按从玩家自身 EMC 余额按汇率 256（=1 铁锭）扣减加晶币（不再折算/销毁主手物品）；tooltip 显示所需 EMC
        if (net.minecraftforge.fml.ModList.get().isLoaded("projecte")) {
            int ey = cy2 + 22;
            int[] emcCoins = {100, 200, 400, 800};
            double rate = Math.max(1.0, com.hexcraft.config.HexConfig.EMC_COIN_RATE.get());
            for (int i = 0; i < emcCoins.length; i++) {
                final int c = emcCoins[i];
                long needEmc = (long) Math.ceil((double) c * rate);
                HexButton eb = new HexButton(cx() + 4 + i * 40, ey + 12, 36, 12,
                        Component.translatable("hexcraft.gui.recharge_coin", c), 0xFF7F7FFF,
                        b -> sendExchange(0, 6, c));
                eb.setTooltip(Tooltip.create(Component.translatable("hexcraft.gui.emc_need", needEmc)));
                this.addRenderableWidget(eb);
            }
        }
    }

    private void sendExchange(int dir, int material, int count) {
        Packets.C2SExchangeCoin p = new Packets.C2SExchangeCoin();
        p.dir = dir;
        p.material = material;
        p.count = count;
        PacketHandler.CHANNEL.sendToServer(p);
    }

    private void initHexes() {
        // v0.9.3 来源分类抽取：全部 / 原版 / 联动 / DLC（同费同保底，服务端按来源过滤抽取池）
        int[] flt = {0, 1, 2, 3};
        String[] lbl = {"hexcraft.gui.draw.all", "hexcraft.gui.draw.vanilla",
                "hexcraft.gui.draw.linked", "hexcraft.gui.draw.dlc"};
        int[] col = {ACCENT, 0xFF6FB1FF, 0xFF7FBF7F, 0xFFC77DFF};
        for (int i = 0; i < 4; i++) {
            final int f = flt[i];
            // 按钮文字限 4 字以内（54px 放不下长文案，费用提示挂 all 按钮 tooltip）
            HexButton w = new HexButton(cx() + 4 + i * 57, cy() + 36, 54, 15,
                    Component.translatable(lbl[i]), col[i], b -> {
                        Packets.C2SRequestDraw p = new Packets.C2SRequestDraw();
                        p.filter = f;
                        PacketHandler.CHANNEL.sendToServer(p);
                    });
            if (i == 0) {
                w.setTooltip(Tooltip.create(Component.translatable(lbl[i] + ".tip", HexConfig.XP_COST_PER_DRAW.get())));
            }
            this.addRenderableWidget(w);
        }
        // 分类过滤栏（9 类，横向一排；选中高亮）
        int bw = 24;
        for (int i = 0; i < HEX_CATS.length; i++) {
            final int c = i;
            this.addRenderableWidget(new HexButton(cx() + 4 + i * bw, cy() + 54, bw - 1, 12,
                    Component.translatable("hexcraft.gui.cat." + HEX_CATS[i]),
                    i == hexCat ? ACCENT : 0xFF8A93A8,
                    b -> { hexCat = c; hexScroll = 0; this.rebuildWidgets(); }));
        }
    }

    private void initForge() {
        int bw = 72;
        // v0.6.3 费用实时刷新：读服务端权威费用快照（未收到前用配置兜底）
        int[] prices = {
                ClientDataStore.forgeCosts[0] > 0 ? ClientDataStore.forgeCosts[0] : HexConfig.FORGE_BASIC_PRICE.get(),
                ClientDataStore.forgeCosts[1] > 0 ? ClientDataStore.forgeCosts[1] : HexConfig.FORGE_GOLD_PRICE.get(),
                ClientDataStore.forgeCosts[2] > 0 ? ClientDataStore.forgeCosts[2] : HexConfig.FORGE_PRISMATIC_PRICE.get()};
        // 三档按钮颜色：白银 / 黄金 / 棱彩，从低到高易区分
        int[] tierColors = {0xFFD8D8D8, 0xFFFFC94A, 0xFFC77DFF};
        for (int i = 0; i < 3; i++) {
            final int t = i;
            // v0.4.4：价格按钮下移 2px，避免与上方「已锻造 N 次」文字贴边重叠
            this.addRenderableWidget(new HexButton(cx() + 4 + i * (bw + 5), cy() + 46, bw, 14,
                    Component.translatable("hexcraft.gui.buy_forge",
                            Component.translatable("hexcraft.forge.tier." + TIER_KEYS[t]), prices[i]),
                    tierColors[i], b -> {
                        Packets.C2SBuyForge p = new Packets.C2SBuyForge();
                        p.tier = t;
                        PacketHandler.CHANNEL.sendToServer(p);
                    }));
        }
        this.addRenderableWidget(new HexButton(cx() + 4 + (bw + 5), cy() + 63, bw * 2 - 5, 14,
                Component.translatable("hexcraft.gui.forge_random"), ACCENT,
                b -> PacketHandler.CHANNEL.sendToServer(new Packets.C2SRandomForge())));
    }

    private void initEquip() {
        // v0.6.3 费用实时刷新：读服务端权威费用快照（未收到前用配置原价兜底）
        // v0.6.16 装备费用配置化：兜底与发送价都改读 HexConfig（服务端同配置校验）
        int cfgGold = com.hexcraft.config.HexConfig.EQUIP_GOLD_COST.get();
        int cfgPrism = com.hexcraft.config.HexConfig.EQUIP_PRISMATIC_COST.get();
        int goldPrice = ClientDataStore.equipGoldCost > 0 ? ClientDataStore.equipGoldCost : cfgGold;
        int prismPrice = ClientDataStore.equipPrismaticCost > 0 ? ClientDataStore.equipPrismaticCost : cfgPrism;
        // 购买按钮：黄金装备费用随机装备池 / 棱彩装备费用随机棱彩装备池（都是三选一）
        this.addRenderableWidget(new HexButton(cx() + 4, cy() + 48, 111, 15,
                Component.translatable("hexcraft.gui.buy_equip", goldPrice), 0xFFFFC94A,
                b -> { Packets.C2SBuyEquip p = new Packets.C2SBuyEquip(); p.price = cfgGold; PacketHandler.CHANNEL.sendToServer(p); }));
        this.addRenderableWidget(new HexButton(cx() + 119, cy() + 48, 111, 15,
                Component.translatable("hexcraft.gui.buy_equip_prismatic", prismPrice), 0xFFC77DFF,
                b -> { Packets.C2SBuyEquip p = new Packets.C2SBuyEquip(); p.price = cfgPrism; PacketHandler.CHANNEL.sendToServer(p); }));
        // v0.6.5 装备图鉴切换按钮（文案=点击后进入的模式；图鉴模式隐藏卖出按钮防误操作）
        this.addRenderableWidget(new HexButton(cx() + 4, cy() + 70, 112, 13,
                Component.translatable(equipGallery ? "hexcraft.gui.equip_view_equipped" : "hexcraft.gui.equip_gallery"), 0xFF4DE6FF,
                b -> { equipGallery = !equipGallery; galleryScroll = 0; this.rebuildWidgets(); }));
        // 6 槽位卖出按钮（无装备时置灰不生效；图鉴模式不显示）
        if (!equipGallery) {
            for (int i = 0; i < PlayerData.MAX_EQUIP; i++) {
                final int idx = i;
                this.addRenderableWidget(new HexButton(cx() + 152, cy() + 91 + i * 16, 58, 12,
                        Component.translatable("hexcraft.gui.equip_sell"), 0xFF8A93A8,
                        b -> { Packets.C2SSellEquip p = new Packets.C2SSellEquip(); p.index = idx; PacketHandler.CHANNEL.sendToServer(p); }));
            }
        }
    }

    @Override
    public void tick() {
        pTick++;
        ensureParts();
        for (float[] p : parts) {
            p[1] -= p[2];
            if (p[1] < -6f) { p[1] = H + 6f; p[0] = P_RAND.nextFloat() * W; }
        }
        // v0.6.3 费用实时刷新：服务端推送的费用快照变化 → 重建按钮（折扣/世界乘区变更即时生效，不闪屏）
        if (ClientDataStore.costVersion != lastCostVersion) {
            lastCostVersion = ClientDataStore.costVersion;
            this.rebuildWidgets();
        }
    }

    private void ensureParts() {
        if (partsInit) return;
        partsInit = true;
        for (int i = 0; i < 22; i++) {
            parts.add(new float[]{P_RAND.nextFloat() * W, P_RAND.nextFloat() * H,
                    0.10f + P_RAND.nextFloat() * 0.15f, P_RAND.nextFloat() * 6.28f,
                    1f + P_RAND.nextFloat() * 1.5f});
        }
    }

    // ================= 渲染 =================

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        int x = (this.guiW() - W) / 2;
        int y = (this.guiH() - H) / 2;

        // 深蓝紫渐变底板
        g.fillGradient(x, y, x + W, y + H, 0xE80D1224, 0xD8241836);
        // 顶部霓虹光条（双色渐变 + 柔光层）
        g.fillGradient(x, y, x + W, y + 2, 0xFF4FD8FF, 0xFF7A6BFF);
        g.fill(x, y + 2, x + W, y + 3, 0x557A6BFF);
        g.fill(x - 1, y - 1, x + W + 1, y, 0x334FD8FF);

        // 左侧导航面板
        g.fill(x + 3, y + 6, x + NAV_W + 8, y + H - 8, 0x22000000);
        g.fill(x + NAV_W + 8, y + 6, x + NAV_W + 9, y + H - 8, 0x3D4FD8FF);

        // 内容区面板（玻璃质感：底部渐变 + 顶部亮条）
        g.fill(cx() - 2, cy() + 4, cx() + 232, cy() + H - 34, 0x14000000);
        g.fillGradient(cx() - 2, cy() + 4, cx() + 232, cy() + 5, 0x334FD8FF, 0x087A6BFF);
        g.fill(cx() - 2, cy() + 5, cx() + 232, cy() + 6, 0x227A6BFF);

        // 背景粒子（青色光点缓慢上浮）
        ensureParts();
        for (float[] p : parts) {
            int px = x + (int) p[0];
            int py = y + (int) p[1];
            int a = (int) ((0.15f + 0.10f * (float) Math.sin(pTick * 0.08f + p[3])) * 255);
            if (a <= 8) continue;
            int sz = (int) p[4];
            g.fill(px - 1, py - 1, px + sz + 1, py + sz + 1, (a / 3) << 24 | 0x007A6BFF);
            g.fill(px, py, px + sz, py + sz, a << 24 | 0x009FE4FF);
        }

        this.renderWidgets(g, mx, my, pt);

        // 标题（顶左上，发光双层）+ 余额（右对齐内容区右缘，不压标题、不超界面）
        String title = Component.translatable("hexcraft.gui.page_title").getString();
        g.drawString(HexFonts.ui(), title, x + 12, y + 9, 0xFF1A2340, false);
        g.drawString(HexFonts.ui(), title, x + 11, y + 8, 0xFFE8F6FF, false);
        Component coinText = Component.translatable("hexcraft.gui.coins", ClientDataStore.coins);
        int coinW = HexFonts.ui().width(coinText);
        int coinRight = cx() + 226;                    // 内容区右缘
        int coinX = Math.max(x + 90, coinRight - coinW); // 不侵入标题区（窄屏右对齐自适应）
        g.drawString(HexFonts.ui(), coinText, coinX, y + 10, 0xFFFFD700, false);
        // 余额光点装饰（文字右侧留 5px，不再压住文字）
        g.fill(coinX + coinW + 4, y + 12, coinX + coinW + 6, y + 14, 0xFFFFD700);

        hoverX = mx;
        hoverY = my;
        hoverTip = null;
        // v0.9.12 内容可视区：列表行只有落在这个区间内才允许悬停弹提示
        hoverClipTop = cy() + 60;
        hoverClipBottom = cy() + H - 34;
        if (tab == 0) renderOverview(g, y);
        else if (tab == 1) renderHexes(g, y);
        else if (tab == 2) renderBonds(g, y);
        else if (tab == 3) renderForge(g, y);
        else if (tab == 4) renderEquip(g, y);
        else renderRecharge(g, y);
        // 自绘列表 hover 提示：最后绘制，盖在列表上方
        if (hoverTip != null && !hoverTip.isEmpty()) {
            // 缩放界面下鼠标与内容是逻辑坐标，边界也用逻辑宽高
            HexFonts.tooltip(g, hoverTip, mx, my, this.guiW(), this.guiH());
        }
    }

    /** v0.9.12 顶部概览行展示的核心属性键（前 4 项为基础属性，crit 为隐藏暴击） */
    private static final String[] OVERVIEW_KEYS = {"max_health", "attack_damage", "armor", "movement_speed", "crit"};

    /** v0.9.12 顶部固定概览行：生命/攻击/护甲/速度/暴击一屏速览，固定不随滚动移动 */
    private void drawOverviewBar(GuiGraphics g, int y) {
        net.minecraft.client.gui.Font font = HexFonts.ui();
        int barY = y + 46;
        g.fill(cx() + 2, barY, cx() + 228, barY + 20, 0x33000000);
        g.fill(cx() + 2, barY, cx() + 228, barY + 1, 0x444FD8FF);
        int slotW = 226 / OVERVIEW_KEYS.length;
        for (int i = 0; i < OVERVIEW_KEYS.length; i++) {
            String key = OVERVIEW_KEYS[i];
            int sx = cx() + 4 + i * slotW;
            Component label = Component.translatable("hexcraft.gui.overview." + key);
            String value;
            if ("crit".equals(key)) {
                double cv = HextechEvents.effect(ClientDataStore.hexes, "crit");
                value = cv > 0 ? fmtEffect(cv, true) : "--";
            } else {
                Double av = ClientDataStore.attrs.get(key);
                value = av == null ? "--" : fmt(av);
            }
            g.drawString(font, label, sx, barY + 3, 0xFF9FE4FF, false);
            g.drawString(font, value, sx, barY + 12, 0xFFFFFFFF, false);
        }
    }

    private void renderOverview(GuiGraphics g, int y) {
        // v0.9.12 每帧重置点击区，避免上一帧残留
        titleHitboxes.clear();
        anchorHitboxes.clear();
        minorHitbox = null;
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.overview_title"), cx() + 4, y + 36, 0xFF9FE4FF, false);
        // v0.9.12 顶部四个锚点（点击快速定位到对应框）
        drawAnchors(g, y);
        // v0.9.12 顶部固定概览行：核心属性一屏速览，不随滚动移动
        drawOverviewBar(g, y);
        // 全部内容（基础/隐藏/叠层三框）整体滚动，保证显示全、不出边框
        double s = uiScale();
        g.enableScissor((int) ((cx() + 2) * s), (int) ((y + 68) * s),
                (int) ((cx() + 232) * s), (int) ((cy() + H - 34) * s));
        int top = y + 68 - attrScroll;
        int idx;
        int inner;
        // ===== 框1 基础属性（v0.9.12：常驻主属性 + 可折叠次要组；整框可折叠）=====
        int mainCount = ATTR_KEYS.length - MINOR_KEYS.length;
        int box1H = boxHeight(0);
        drawGroupBox(g, top, box1H);
        drawBoxHeader(g, top, 0, Component.translatable("hexcraft.gui.group_base"));
        if (!boxCollapsed[0]) {
            idx = 2;
            inner = top + 4;
            for (int i = 0; i < mainCount; i++, idx++) {
                String key = ATTR_KEYS[i];
                Double val = ClientDataStore.attrs.get(key);
                drawAttrRow(g, inner, idx, Component.translatable("hexcraft.stat." + key),
                        val == null ? "--" : fmt(val));
                int rowY = inner + (idx / 2) * ROW_H;
                int hc = hoverColumn(hoverX, hoverY, rowY);
                boolean hitOwnCol = (idx % 2 == 0) ? hc == 1 : hc == 2;
                if (hc != 0 && hitOwnCol) {
                    java.util.List<Object> tip = new java.util.ArrayList<>();
                    tip.add(Component.translatable("hexcraft.stat." + key).withStyle(c -> c.withColor(0xFF9FE4FF)));
                    tip.add(Component.translatable("hexcraft.stat.hint." + key).withStyle(c -> c.withColor(0xFFAAB6D8)));
                    hoverTip = tip;
                }
            }
            if (!minorCollapsed) {
                // 次要组标题（点击可折叠次要组）
                drawSectionTitle(g, inner, idx, Component.translatable("hexcraft.gui.group_minor"));
                int mrow = idx / 2;
                minorHitbox = new int[]{cx() + 2, inner + mrow * ROW_H, cx() + 230, inner + (mrow + 1) * ROW_H};
                idx += 2;
                for (int mi = 0; mi < MINOR_KEYS.length; mi++, idx++) {
                    String key = MINOR_KEYS[mi];
                    Double val = ClientDataStore.attrs.get(key);
                    drawAttrRow(g, inner, idx, Component.translatable("hexcraft.stat." + key),
                            val == null ? "--" : fmt(val));
                    int rowY = inner + (idx / 2) * ROW_H;
                    int hc = hoverColumn(hoverX, hoverY, rowY);
                    boolean hitOwnCol = (idx % 2 == 0) ? hc == 1 : hc == 2;
                    if (hc != 0 && hitOwnCol) {
                        java.util.List<Object> tip = new java.util.ArrayList<>();
                        tip.add(Component.translatable("hexcraft.stat." + key).withStyle(c -> c.withColor(0xFF9FE4FF)));
                        tip.add(Component.translatable("hexcraft.stat.hint." + key).withStyle(c -> c.withColor(0xFFAAB6D8)));
                        hoverTip = tip;
                    }
                }
            }
        }
        top += box1H + 4;
        // ===== 框2 隐藏属性（v0.9.12：整框可折叠）=====
        List<String[]> hiddenRows = hiddenRows();
        if (!hiddenRows.isEmpty()) {
            int box2H = boxHeight(1);
            drawGroupBox(g, top, box2H);
            drawBoxHeader(g, top, 1, Component.translatable("hexcraft.gui.group_hidden"));
            if (!boxCollapsed[1]) {
                idx = 2;
                inner = top + 4;
                for (String[] r : hiddenRows) {
                    drawAttrRow(g, inner, idx, Component.translatable("hexcraft.effect." + r[0]), r[1]);
                    int rowY = inner + (idx / 2) * ROW_H;
                    int hc = hoverColumn(hoverX, hoverY, rowY);
                    boolean hitOwnCol = (idx % 2 == 0) ? hc == 1 : hc == 2;
                    if (hc != 0 && hitOwnCol) {
                        java.util.List<Object> tip = new java.util.ArrayList<>();
                        tip.add(Component.translatable("hexcraft.effect." + r[0]).withStyle(c -> c.withColor(0xFF9FE4FF)));
                        // v0.9.12 修复：hint 文案自带 + / -，这里传入去掉前导符号的裸数值，避免「++318%」
                        tip.add(Component.translatable("hexcraft.effect.hint." + r[0], stripSign(r[1])).withStyle(c -> c.withColor(0xFFAAB6D8)));
                        hoverTip = tip;
                    }
                    idx++;
                }
            }
            top += box2H + 4;
        }
        // ===== 框3 成长叠层：所有可叠层海克斯与装备全部展示（含 0 层）=====
        List<PlayerData.HexEntry> stkHexes = stackableHexes();
        List<PlayerData.EquipEntry> stkEqs = stackableEquips();
        // v0.6.0 战斗叠层（积聚之刃等通用叠层管理器）：combat 叠层脱战衰减/死亡清空，单独小节展示
        List<PlayerData.StackEntry> battleStacks = new java.util.ArrayList<>();
        for (PlayerData.StackEntry bs : ClientDataStore.stacks) if (bs.combat) battleStacks.add(bs);
        boolean hasBattle = !battleStacks.isEmpty();
        if (!stkHexes.isEmpty() || !stkEqs.isEmpty() || hasBattle) {
            int box3H = boxHeight(2);
            drawGroupBox(g, top, box3H);
            drawBoxHeader(g, top, 2, Component.translatable("hexcraft.gui.group_stack"));
            if (!boxCollapsed[2]) {
                idx = 2;
                inner = top + 4;
                if (!stkHexes.isEmpty()) {
                    drawSectionTitle(g, inner, idx, Component.translatable("hexcraft.gui.stack_hex_title"));
                    idx += 2;
                    for (PlayerData.HexEntry h : stkHexes) {
                        drawAttrRow(g, inner, idx, Component.translatable("hexcraft.hextech." + h.id + ".name"),
                                stackTextOf(h));
                        int rowY = inner + (idx / 2) * ROW_H;
                        int hc = hoverColumn(hoverX, hoverY, rowY);
                        boolean hitOwnCol = (idx % 2 == 0) ? hc == 1 : hc == 2;
                        if (hc != 0 && hitOwnCol) {
                            java.util.List<Object> tip = new java.util.ArrayList<>();
                            tip.add(Component.translatable("hexcraft.hextech." + h.id + ".name").withStyle(c -> c.withColor(0xFF9FE4FF)));
                            tip.add(Component.translatable("hexcraft.hextech." + h.id + ".desc").withStyle(c -> c.withColor(0xFFAAB6D8)));
                            // v0.9.23：只在真实存在层数时才写对应词条。全 0 的件（如靠 hasScionKey 进列表、
                            // 本身没有 stack 块的锻造神子）不再出现「命中层 0 层，受击层 0 层」。
                            if (h.stacks > 0 || h.hurtStacks > 0) {
                                StringBuilder sb = new StringBuilder();
                                if (h.stacks > 0) sb.append(Component.translatable("hexcraft.unit.stack_hit", h.stacks).getString());
                                if (h.hurtStacks > 0) {
                                    if (sb.length() > 0) sb.append(Component.translatable("hexcraft.unit.sep").getString());
                                    sb.append(Component.translatable("hexcraft.unit.stack_hurt", h.hurtStacks).getString());
                                }
                                tip.add(Component.literal(sb.toString()).withStyle(c -> c.withColor(0xFFFFD28C)));
                            }
                            hoverTip = tip;
                        }
                        idx++;
                    }
                }
                if (!stkEqs.isEmpty()) {
                    if (idx % 2 == 1) idx++; // 条目为奇数时补到整行，副标题绝不错位
                    drawSectionTitle(g, inner, idx, Component.translatable("hexcraft.gui.stack_equip_title"));
                    idx += 2;
                    for (PlayerData.EquipEntry e : stkEqs) {
                        drawAttrRow(g, inner, idx, Component.translatable("hexcraft.equip." + e.id + ".name"),
                                Component.translatable("hexcraft.unit.stack_x", e.stacks).getString());
                        int rowY = inner + (idx / 2) * ROW_H;
                        int hc = hoverColumn(hoverX, hoverY, rowY);
                        boolean hitOwnCol = (idx % 2 == 0) ? hc == 1 : hc == 2;
                        if (hc != 0 && hitOwnCol) {
                            java.util.List<Object> tip = new java.util.ArrayList<>();
                            tip.add(Component.translatable("hexcraft.equip." + e.id + ".name").withStyle(c -> c.withColor(0xFF9FE4FF)));
                            tip.add(Component.translatable("hexcraft.equip." + e.id + ".desc").withStyle(c -> c.withColor(0xFFAAB6D8)));
                            tip.add(Component.literal(Component.translatable("hexcraft.unit.stack_current", e.stacks).getString()).withStyle(c -> c.withColor(0xFFFFD28C)));
                            hoverTip = tip;
                        }
                        idx++;
                    }
                }
                if (hasBattle) {
                    if (idx % 2 == 1) idx++; // 补到整行
                    drawSectionTitle(g, inner, idx, Component.translatable("hexcraft.gui.stack_battle_title"));
                    idx += 2;
                    for (PlayerData.StackEntry bs : battleStacks) {
                        Component bname = Component.literal(stackNameOf(bs.key));
                        drawAttrRow(g, inner, idx, bname, Component.translatable("hexcraft.unit.stack_ratio", (int) bs.value, capText((int) bs.cap)).getString());
                        int rowY = inner + (idx / 2) * ROW_H;
                        int hc = hoverColumn(hoverX, hoverY, rowY);
                        boolean hitOwnCol = (idx % 2 == 0) ? hc == 1 : hc == 2;
                        if (hc != 0 && hitOwnCol) {
                            String bid = stackIdOf(bs.key); // v0.9.24：与 stackNameOf 共用 id 解析（含 _blade 兜底）
                            java.util.List<Object> tip = new java.util.ArrayList<>();
                            tip.add(Component.literal(stackNameOf(bs.key)).withStyle(c -> c.withColor(0xFF9FE4FF)));
                            if (bid != null) {
                                tip.add(Component.translatable("hexcraft.hextech." + bid + ".desc").withStyle(c -> c.withColor(0xFFAAB6D8)));
                            }
                            tip.add(Component.literal(Component.translatable("hexcraft.unit.stack_current_cap", (int) bs.value, capText((int) bs.cap)).getString()).withStyle(c -> c.withColor(0xFFFFD28C)));
                            hoverTip = tip;
                        }
                        idx++;
                    }
                }
            }
            top += box3H + 4; // v0.9.3 修复：框3 结束后推进 top，否则框4 组合羁绊与框3 成长叠层重叠
        }
        // ===== 框4 组合羁绊（v0.9.3）：拥有 ≥1 成员的羁绊行；激活=已激活、未凑齐=x/y；hover 展示成员与效果 =====
        java.util.List<ComboRow> comboRows = comboRows();
        int comboStacks = comboStackCount();
        boolean showStackTier = comboStacks >= 2;
        if (!comboRows.isEmpty() || showStackTier) {
            int box4H = boxHeight(3);
            drawGroupBox(g, top, box4H);
            drawBoxHeader(g, top, 3, Component.translatable("hexcraft.gui.group_combo"));
            idx = 2;
            inner = top + 4;
            if (!boxCollapsed[3]) for (ComboRow cr : comboRows) {
                if (cr.tiered()) {
                    // v0.9.5 逆原分档羁绊（2/4/6 按件数激活）：满档金色、已激活浅金、未激活常规
                    // v0.9.7 优化：大乱斗套装（source=aram）行名紫色区分来源；hover 补成员列表
                    // v0.9.7 v2 棱彩：每条羁绊满员（全部成员凑齐）→ 整行棱彩紫 + 状态「棱彩」+ 全效果 ×2（巨大提升）；
                    // 档位色阶梯（云顶式）：档1白 → 档2银 → 档3浅金 → 档4深金 → 满员棱彩紫
                    boolean maxTier = cr.tier() >= cr.tierCounts().length;
                    // v0.9.24：上限解锁已迁到「叠层之源」，特判与文案跟着走
                    boolean stackSource = "hexcraft.combo.v2.stacksource".equals(cr.nameKey());
                    // v0.9.12：只有 prismaticMult>1 的羁绊满档才叫"棱彩"并享受翻倍；单档固定羁绊满员即普通满档
                    boolean prismaticUnlocked = maxTier && cr.prismaticMult() > 1.0;
                    String status = prismaticUnlocked
                            ? Component.translatable("hexcraft.gui.combo.prismatic").getString()
                            : cr.have() + "/" + cr.total();
                    Component tierName = Component.translatable(cr.nameKey());
                    if ("aram".equals(cr.source())) tierName = tierName.copy().withStyle(t -> t.withColor(0xFFC77DFF));
                    int rowColor = prismaticUnlocked ? 0xFFC77DFF
                            : TIER_COLORS[Math.max(0, Math.min(cr.tier() - 1, TIER_COLORS.length - 1))];
                    drawAttrRow(g, inner, idx, tierName, status, rowColor);
                    // hover：成员列表 + 各档效果 + 当前档 + 下一档缺数
                    int rowY = inner + (idx / 2) * ROW_H;
                    int hc = hoverColumn(hoverX, hoverY, rowY);
                    boolean hitOwnCol = (idx % 2 == 0) ? hc == 1 : hc == 2;
                    if (hc != 0 && hitOwnCol) {
                        java.util.List<Object> tip = new java.util.ArrayList<>();
                        tip.add(Component.translatable(cr.nameKey()).withStyle(t -> t.withColor(0xFFFFD28C)));
                        if (cr.members() != null && cr.members().length > 0) {
                            // v0.9.10 成员行拆行：每行 4 个名字（用户拍板 2026-11-03），收窄悬浮窗宽度
                            StringBuilder members = new StringBuilder();
                            int per = 0;
                            for (int mi = 0; mi < cr.members().length; mi++) {
                                if (per > 0) members.append("  ");
                                members.append(Component.translatable("hexcraft.hextech." + cr.members()[mi] + ".name").getString());
                                per++;
                                if (per == 4) {
                                    tip.add(Component.literal(members.toString()).withStyle(t -> t.withColor(0xFFAAB6D8)));
                                    members.setLength(0);
                                    per = 0;
                                }
                            }
                            if (members.length() > 0) {
                                tip.add(Component.literal(members.toString()).withStyle(t -> t.withColor(0xFFAAB6D8)));
                            }
                        }
                        for (int t = 0; t < cr.tierCounts().length; t++) {
                            boolean capTier = "stack_cap_tier".equals(cr.effects()[t]);
                            String en = Component.translatable("hexcraft.effect." + cr.effects()[t]).getString();
                            int pct = (int) Math.round(cr.values()[t] * 100);
                            // v0.9.24：叠层之源的档位值存的是「解锁到多少层」，不是百分比
                            String val = capTier ? capTierText(cr.values()[t]) : signPct(pct);
                            int tierColor = t < cr.tier() ? 0xFFFFD700 : 0xFFAAB6D8;
                            // v0.9.10 分栏对齐：档号/件数/效果名自动列对齐，数值右对齐分栏区右缘；件数不带括号
                            tip.add(new HexFonts.ColLine(
                                    new String[]{Component.translatable("hexcraft.gui.tier_no", t + 1).getString(),
                                            Component.translatable("hexcraft.gui.tier_pieces", cr.tierCounts()[t]).getString(),
                                            en, val},
                                    tierColor));
                        }
                        if (!maxTier) tip.add(Component.literal(Component.translatable("hexcraft.gui.tier_next",
                                        cr.tierCounts()[cr.tier()] - cr.have(), cr.tier() + 1).getString())
                                .withStyle(t -> t.withColor(0xFFFF6B6B)));
                        if (stackSource) tip.add(Component.translatable("hexcraft.gui.stackcap_ladder")
                                .withStyle(t -> t.withColor(0xFF9FE4FF)));
                        if (maxTier && stackSource) tip.add(Component.translatable("hexcraft.gui.stackcap_unlocked").withStyle(t -> t.withColor(0xFFC77DFF)));
                        if (prismaticUnlocked) tip.add(Component.literal(Component.translatable("hexcraft.gui.combo.prismatic_bonus").getString())
                                .withStyle(t -> t.withColor(0xFFC77DFF)));
                        hoverTip = tip;
                    }
                }
                idx++;
            }
            if (showStackTier && !boxCollapsed[3]) {
                if (idx % 2 == 1) idx++;
                String tierKey = comboStacks >= 4 ? "hexcraft.gui.combo.stack4" : "hexcraft.gui.combo.stack2";
                // v0.9.3 修正7：lang 文案含 "+20%" 的 % 会让 Minecraft 的 translatable 格式化器（String.format）抛非法格式异常并原样返回 %s，
                // 故改用无参 getString + 手动 replace 替换 %s（无参时不触发格式化，安全）
                String tierVal = Component.translatable(tierKey).getString().replace("%s", String.valueOf(comboStacks));
                // v0.9.5：叠层档位行高亮（4 件档金色、2 件档浅金）
                drawAttrRow(g, inner, idx, Component.translatable("hexcraft.gui.combo.stack_tier"), tierVal,
                        comboStacks >= 4 ? 0xFFFFD700 : 0xFFE8C36A);
                // hover：叠层档位完整机制（行内为短文案，完整说明进 tooltip）
                int rowY = inner + (idx / 2) * ROW_H;
                int hc = hoverColumn(hoverX, hoverY, rowY); // v0.9.5 hover 列判定去重
                boolean hitOwnCol = (idx % 2 == 0) ? hc == 1 : hc == 2;
                if (hc != 0 && hitOwnCol) {
                    java.util.List<Object> tip = new java.util.ArrayList<>();
                    tip.add(Component.translatable("hexcraft.gui.combo.stack_tier").withStyle(t -> t.withColor(0xFF9FE4FF)));
                    tip.add(Component.literal(Component.translatable("hexcraft.gui.combo.stack_owned").getString()
                            .replace("{n}", String.valueOf(comboStacks))
                            .replace("{p}", comboStacks >= 4 ? "+40%" : "+20%")).withStyle(t -> t.withColor(0xFFAAB6D8)));
                    hoverTip = tip;
                }
            }
            top += box4H + 4;
        }
        g.disableScissor();
        // v0.9.7 恢复属性页滚动提示：内容超高且未滚到底时，可视区底部右缘显示「▽」提示可继续滚动。
        // v0.9.3 曾整体移除（怕叠压文字），但用户反馈属性页内容多时误以为「显示范围顶死、不往后显示」，
        // 现改为仅未滚到底时显示、滚到底自动消失，浅色小字叠压风险可接受（与海克斯/装备页 ▽ 同款）。
        int ms = maxAttrScroll();
        if (ms > 0 && attrScroll < ms) {
            g.drawString(HexFonts.ui(), "\u25BC", cx() + 218, cy() + H - 34 - 10, 0xFF5A6A90, false);
        }
    }

    /** 隐藏属性行集合（值 > 0 才显示）：暴击伤害 + 效果属性 + TACZ
     *  v0.9.12 性能：本方法每帧被 render 与 boxHeight 调用多次，而每个效果键都要全量汇总一次海克斯与 48 条羁绊
     *  （约 60 个键 × 48 条羁绊）。改为 250ms 缓存 + 持有数量变化立即失效，打开界面时的每帧开销从数十次全量扫描降到一次。 */
    private static List<String[]> hiddenRowCache;
    private static long hiddenRowCacheAt = Long.MIN_VALUE;
    private static int hiddenRowCacheSig = -1;

    private List<String[]> hiddenRows() {
        int sig = ClientDataStore.hexes.size() * 131 + ClientDataStore.equipment.size() * 17 + ClientDataStore.forges.size();
        long now = System.currentTimeMillis();
        if (hiddenRowCache != null && sig == hiddenRowCacheSig && now - hiddenRowCacheAt < 250) return hiddenRowCache;
        hiddenRowCache = collectHiddenRows();
        hiddenRowCacheSig = sig;
        hiddenRowCacheAt = now;
        return hiddenRowCache;
    }

    private List<String[]> collectHiddenRows() {
        boolean tacz = ModList.get().isLoaded("tacz");
        java.util.List<String> keys = new java.util.ArrayList<>();
        keys.add("crit");
        keys.add("full_hp_crit");
        keys.add("tacz_crit");
        for (String[] e : EFFECT_KEYS) keys.add(e[0]);
        if (tacz) for (String[] e : TACZ_KEYS) keys.add(e[0]);
        // v0.9.24 性能：一次批量汇总替代原来约 50 次单键 effect()（旧写法每个键都是一次全量遍历）
        java.util.Map<String, Double> eff = HextechEvents.effectBatch(ClientDataStore.hexes, keys);
        List<String[]> rows = new java.util.ArrayList<>();
        double anyCrit = eff.getOrDefault("crit", 0.0) + eff.getOrDefault("full_hp_crit", 0.0)
                + eff.getOrDefault("tacz_crit", 0.0);
        if (anyCrit > 0) rows.add(new String[]{"crit_damage", "150%"});
        for (String[] e : EFFECT_KEYS) {
            double v = eff.getOrDefault(e[0], 0.0);
            if (v <= 0) continue;
            rows.add(new String[]{e[1], fmtEffect(v, "1".equals(e[2]))});
        }
        if (tacz) {
            for (String[] e : TACZ_KEYS) {
                double v = eff.getOrDefault(e[0], 0.0);
                if (v <= 0) continue;
                rows.add(new String[]{e[1], fmtEffect(v, "1".equals(e[2]))});
            }
        }
        return rows;
    }

    /** v0.6.0 战斗叠层对应的真实海克斯 id：优先 _blade 后缀（crescendo → hex_crescendo_blade），
     *  再退回同名 id（战伤突进 → hex_combat_damage_spur）。找不到返回 null。
     *  v0.9.24 修复：属性页 tooltip 的 desc 判定原先单独拼 "hex_"+key，漏掉 _blade 兜底，
     *  导致「积聚之刃」这类带后缀的海克斯在属性页只有名字与层数、没有介绍。现统一走本方法。 */
    private static String stackIdOf(String key) {
        String candidate = "hex_" + key + "_blade";
        if (ClientDataStore.hexDef(candidate) != null) return candidate;
        String probe = "hex_" + key;
        if (ClientDataStore.hexDef(probe) != null) return probe;
        return null;
    }

    /** v0.6.0 战斗叠层显示名：取对应海克斯名，无则用键名 */
    private static String stackNameOf(String key) {
        String id = stackIdOf(key);
        if (id != null) return Component.translatable("hexcraft.hextech." + id + ".name").getString();
        return key;
    }

    /** 可叠层海克斯：仅列真正有每层属性收益的（三段式 stack_attr / stack_attr_onhit / stack_attr_onhurt）。
     *  v0.9.12：纯 stack_growth 增幅类与两段无效键不再计入——旧实现把它们当"成长成果"展示，
     *  实测会出现 ×94 层这种没有任何收益的空转层数。受击叠层（stack_attr_onhurt）此前漏收，现一并纳入。 */
    private static List<PlayerData.HexEntry> stackableHexes() {
        List<PlayerData.HexEntry> out = new java.util.ArrayList<>();
        for (PlayerData.HexEntry h : ClientDataStore.hexes) {
            HextechDefinition def = ClientDataStore.hexDef(h.id); // v0.6.4 联机缓存兜底
            if (def != null && def.selfStack && com.hexcraft.event.HextechEvents.hasAnyStackAttr3(def)) {
                out.add(h);
            }
        }
        return out;
    }

    /** 成长叠层框的层数文本：命中层为主、受击层并列（双叠层海克斯）；均无层时显示 0 便于识别"已持有未成长"。
     *  数值列宽度有限（44px），双叠层用 ×命中/受受击 的紧凑写法，完整分解放在 hover 提示里。 */
    private static String stackTextOf(PlayerData.HexEntry h) {
        if (h.stacks > 0 && h.hurtStacks > 0) return Component.translatable("hexcraft.unit.stack_compact_both", h.stacks, h.hurtStacks).getString();
        if (h.stacks > 0) return Component.translatable("hexcraft.unit.stack_x", h.stacks).getString();
        if (h.hurtStacks > 0) return Component.translatable("hexcraft.unit.stack_hurt_x", h.hurtStacks).getString();
        return Component.translatable("hexcraft.unit.stack_zero").getString();
    }

    /** 可叠层装备：仅列真正有每层属性收益的（三段式 stack_attr / stack_attr_onhit / stack_attr_onhurt） */
    private static List<PlayerData.EquipEntry> stackableEquips() {
        List<PlayerData.EquipEntry> out = new java.util.ArrayList<>();
        for (PlayerData.EquipEntry e : ClientDataStore.equipment) {
            HextechDefinition def = ClientDataStore.equipDef(e.id); // v0.6.4 联机缓存兜底
            if (def != null && com.hexcraft.event.HextechEvents.hasAnyStackAttr3(def)) {
                out.add(e);
            }
        }
        return out;
    }

    /** v0.9.5 组合羁绊/叠层档位缓存：依赖 ClientDataStore 数据版本号，同步到达才重算（原每帧全遍历 COMBOS+海克斯） */
    private static int cachedComboVer = Integer.MIN_VALUE;
    private static java.util.List<ComboRow> cachedComboRows = java.util.List.of();
    /** v0.9.7 v2-A：羁绊页签全量图鉴缓存（47 条，含未拥有；与 comboRows 同版本号同源重建） */
    private static java.util.List<BondRow> cachedBonds = java.util.List.of();
    private static int cachedComboStack;

    /** v0.9.7 v2 档位颜色阶梯（云顶式）：档1 白 → 档2 银 → 档3 浅金 → 档4 深金 → 满员棱彩紫（满档单独判） */
    private static final int[] TIER_COLORS = {0xFFFFFFFF, 0xFFC0C8D0, 0xFFE8C36A, 0xFFFFB347, 0xFFC77DFF};

    /** v0.9.5 组合羁绊展示行（tiered=false 三件套布尔；tiered=true 分档羁绊 2/4/6，tier=当前激活档数）。
     *  v0.9.7 优化：新增 source（"niyuan"/"aram"/null=本体）与 members（分档羁绊成员 id 列表，hover 展示）。
     *  v0.9.7 v2：三件套布尔行已废弃，全部 47 条羁绊均为 tiered=true。 */
    private record ComboRow(boolean tiered, int rowIdx, int have, int total, int tier,
                            int[] tierCounts, String[] effects, double[] values, String nameKey,
                            String source, String[] members, double prismaticMult) {}

    /** v0.9.7 v2-A：羁绊页签图鉴行（group：0 原生/1 新流派/2 叠层专家/3 逆原/4 大乱斗）；
     *  v0.9.12 prismaticMult 用于区分"满档是否真的翻倍"——单档固定羁绊（叠层之巅）满员不翻倍。 */
    private record BondRow(String nameKey, String[] members, int[] tierCounts, String[] effects,
                           double[] values, int have, int tier, int group, double prismaticMult) {}

    /** v0.9.7 v2-A：羁绊页签全量图鉴（47 条，含未拥有 0/总） */
    private static java.util.List<BondRow> bondRows() {
        if (ClientDataStore.dataVersion != cachedComboVer) rebuildComboCache();
        return cachedBonds;
    }

    /** v0.9.3 属性页「组合羁绊」框数据：COMBOS 中拥有 ≥1 成员的行 + 逆原分档羁绊行 */
    private static java.util.List<ComboRow> comboRows() {
        if (ClientDataStore.dataVersion != cachedComboVer) rebuildComboCache();
        return cachedComboRows;
    }

    /** 拥有叠层类海克斯数量（v0.9.3 属性页叠层档位展示；服务端效果判定见 HextechEvents.countStackHexes） */
    private static int comboStackCount() {
        if (ClientDataStore.dataVersion != cachedComboVer) rebuildComboCache();
        return cachedComboStack;
    }

    private static void rebuildComboCache() {
        cachedComboVer = ClientDataStore.dataVersion;
        java.util.List<ComboRow> out = new java.util.ArrayList<>();
        java.util.Set<String> owned = new java.util.HashSet<>();
        for (PlayerData.HexEntry h : ClientDataStore.hexes) owned.add(h.id);
        // v0.9.12：统一遍历有效羁绊视图（联动成员已过滤、档位已重算；纯联动无可用成员者整条不出现）
        java.util.List<com.hexcraft.hextech.ComboRegistry.ResolvedCombo> rcs =
                com.hexcraft.hextech.ComboRegistry.resolvedAll();
        java.util.List<BondRow> bonds = new java.util.ArrayList<>();
        for (com.hexcraft.hextech.ComboRegistry.ResolvedCombo rc : rcs) {
            int have = com.hexcraft.hextech.ComboRegistry.ownedCount(rc, owned);
            int tier = com.hexcraft.hextech.ComboRegistry.tierOf(rc, owned);
            // source 标记：group 3=逆原 niyuan、group 4=大乱斗 aram，其余为本体 null
            String source = rc.group() == 4 ? "aram" : (rc.group() == 3 ? "niyuan" : null);
            if (have > 0) out.add(new ComboRow(true, 0, have, rc.members().length,
                    tier, rc.tierCounts(), rc.effects(), rc.values(), rc.nameKey(),
                    source, rc.members(), rc.prismaticMult()));
            // 羁绊页签全量行（含未拥有）；group 直接取有效视图（隐藏后 index 已不可靠）
            bonds.add(new BondRow(rc.nameKey(), rc.members(), rc.tierCounts(), rc.effects(), rc.values(),
                    have, tier, rc.group(), rc.prismaticMult()));
        }
        cachedComboRows = out;
        cachedBonds = bonds;
        int n = 0;
        for (PlayerData.HexEntry h : ClientDataStore.hexes) if (com.hexcraft.hextech.ComboRegistry.STACK_HEXES.contains(h.id)) n++;
        cachedComboStack = n;
    }

    /** 分组框：半透明深色背景 + 细边框（代替横线，绝不挡字） */
    private void drawGroupBox(GuiGraphics g, int y, int height) {
        g.fill(cx() + 2, y, cx() + 230, y + height, 0x180A1A2E);
        g.fill(cx() + 2, y, cx() + 230, y + 1, 0xFF30466B);
        g.fill(cx() + 2, y + height - 1, cx() + 230, y + height, 0xFF30466B);
        g.fill(cx() + 2, y, cx() + 3, y + height, 0xFF30466B);
        g.fill(cx() + 229, y, cx() + 230, y + height, 0xFF30466B);
    }

    /** v0.9.7 v2-A：羁绊页签可视高（标题 2 行 26px + 列表区 y+62 ~ y+H-40 ≈ 186px） */
    private int maxBondScroll() {
        java.util.List<BondRow> rows = bondRows();
        // v0.9.24：新增「叠层之源」分组后最多 6 个分组头，预留数从 5 改 6，否则滚动范围少一行
        int lineCount = rows.size() + 6;
        return Math.max(0, lineCount * ROW_H - 186);
    }

    /** v0.9.12：羁绊页签——有效羁绊全量图鉴（含未拥有；纯联动无可用成员者整条隐藏），单列滚动；
     *  组头 原生/新流派/叠层专家/逆原/大乱斗；行色=档位色阶（白银浅金深金），满员=棱彩紫 */
    private void renderBonds(GuiGraphics g, int y) {
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.bonds_title"), cx() + 4, y + 36, 0xFF9FE4FF, false);
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.bonds_hint"), cx() + 4, y + 48, 0xFF6FA8FF, false);
        int top = y + 62;
        java.util.List<BondRow> rows = bondRows();
        int ms = maxBondScroll();
        if (bondScroll > ms) bondScroll = ms;
        int line = 0;
        int lastGroup = -1;
        for (BondRow br : rows) {
            if (br.group != lastGroup) {
                int gy = top + line * ROW_H - bondScroll;
                lastGroup = br.group;
                if (gy >= top - 14 && gy <= top + 186) {
                    g.fill(cx() + 4, gy, cx() + 226, gy + 13, 0x33000000);
                    g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.bonds_group" + br.group),
                            cx() + 6, gy + 1, GROUP_COLORS[br.group], false);
                }
                line++;
            }
            int ry = top + line * ROW_H - bondScroll;
            line++;
            if (ry < top - 14 || ry > top + 186) continue;
            boolean maxTier = br.tier >= br.tierCounts().length;
            boolean prismaticRow = maxTier && br.prismaticMult() > 1.0;
            boolean ownedAny = br.have > 0;
            String status = prismaticRow
                    ? Component.translatable("hexcraft.gui.combo.prismatic").getString()
                    : br.have + "/" + br.members().length;
            Component name = Component.translatable(br.nameKey());
            if (prismaticRow) name = name.copy().withStyle(t -> t.withColor(0xFFC77DFF));
            else if (!ownedAny) name = name.copy().withStyle(t -> t.withColor(0xFF5A6A90));
            else name = name.copy().withStyle(t -> t.withColor(
                    TIER_COLORS[Math.max(0, Math.min(br.tier - 1, TIER_COLORS.length - 1))]));
            // 名称（档位色：未拥有灰、已激活白→银→浅金→深金、满员棱彩紫）+ 状态右对齐
            String nameStr = Component.translatable(br.nameKey()).getString();
            while (HexFonts.ui().width(nameStr) > 150 && nameStr.length() > 1) nameStr = nameStr.substring(0, nameStr.length() - 1);
            int rowColor = prismaticRow ? 0xFFC77DFF
                    : (ownedAny ? TIER_COLORS[Math.max(0, Math.min(br.tier - 1, TIER_COLORS.length - 1))] : 0xFF5A6A90);
            g.drawString(HexFonts.ui(), nameStr, cx() + 6, ry + 1, rowColor, false);
            g.drawString(HexFonts.ui(), status, cx() + 226 - HexFonts.ui().width(status), ry + 1,
                    prismaticRow ? 0xFFC77DFF : (ownedAny ? 0xFFE8C36A : 0xFF5A6A90), false);
            // hover：成员 + 各档 + 下一档缺数 + 棱彩
            int hc = hoverColumn(hoverX, hoverY, ry);
            if (hc != 0) {
                java.util.List<Object> tip = new java.util.ArrayList<>();
                tip.add(Component.translatable(br.nameKey()).withStyle(t -> t.withColor(0xFFFFD28C)));
                if (br.members().length > 0) {
                    // v0.9.10 成员行拆行：每行 4 个名字（用户拍板 2026-11-03），收窄悬浮窗宽度
                    StringBuilder members = new StringBuilder();
                    int per = 0;
                    for (int mi = 0; mi < br.members().length; mi++) {
                        if (per > 0) members.append("  ");
                        members.append(Component.translatable("hexcraft.hextech." + br.members()[mi] + ".name").getString());
                        per++;
                        if (per == 4) {
                            tip.add(Component.literal(members.toString()).withStyle(t -> t.withColor(0xFFAAB6D8)));
                            members.setLength(0);
                            per = 0;
                        }
                    }
                    if (members.length() > 0) {
                        tip.add(Component.literal(members.toString()).withStyle(t -> t.withColor(0xFFAAB6D8)));
                    }
                }
                for (int t = 0; t < br.tierCounts().length; t++) {
                    boolean capTier = "stack_cap_tier".equals(br.effects()[t]);
                    String en = Component.translatable("hexcraft.effect." + br.effects()[t]).getString();
                    int pct = (int) Math.round(br.values()[t] * 100);
                    // v0.9.24：叠层之源的档位值存的是「解锁到多少层」，不是百分比
                    String val = capTier ? capTierText(br.values()[t]) : signPct(pct);
                    int slotColor = t < br.tier ? 0xFFFFD700 : 0xFFAAB6D8;
                    // v0.9.10 分栏对齐：档号/件数/效果名自动列对齐，数值右对齐分栏区右缘；件数不带括号
                    tip.add(new HexFonts.ColLine(
                            new String[]{Component.translatable("hexcraft.gui.tier_no", t + 1).getString(),
                                    Component.translatable("hexcraft.gui.tier_pieces", br.tierCounts()[t]).getString(),
                                    en, val},
                            slotColor));
                }
                if (!maxTier) tip.add(Component.literal(Component.translatable("hexcraft.gui.tier_next",
                                br.tierCounts()[br.tier()] - br.have, br.tier() + 1).getString())
                        .withStyle(t -> t.withColor(0xFFFF6B6B)));
                if (prismaticRow) tip.add(Component.literal(Component.translatable("hexcraft.gui.combo.prismatic_bonus").getString())
                        .withStyle(t -> t.withColor(0xFFC77DFF)));
                hoverTip = tip;
            }
        }
    }

    /** 充值页渲染：6 材质两行式（标签在上、按钮在下，按钮按金属配色）+ 晶币→绿宝石行 + 规则提示 */
    private void renderRecharge(GuiGraphics g, int y) {
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.recharge_title"), cx() + 4, y + 36, 0xFF9FE4FF, false);
        // 汇率合并单行（短名+比例，不超界面）
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.recharge_rate"), cx() + 4, y + 48, 0xFF6FA8FF, false);
        // 分隔线
        g.fill(cx() + 4, y + 58, cx() + 228, y + 59, 0x33505F88);
        // 6 材质标签（每材质块 30px：标签在上，按钮在下；标签用对应金属色）
        int[] tcols = {0xFF55FF55, 0xFFFF8C00, 0xFFD8D8D8, 0xFFFFD700, 0xFF4DE6FF, 0xFFC77DFF};
        int ly = y + 64;
        for (int m = 0; m < 6; m++) {
            g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.recharge_m" + m), cx() + 4, ly, tcols[m], false);
            ly += 30;
        }
        // 晶币 → 绿宝石 标签（金色；按钮行在其下 8px）
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.recharge_coin_label"), cx() + 4, y + 64 + 6 * 30 + 6, 0xFFFFD700, false);
        // 规则提示（短版，单行不超界面）
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.recharge_hint"), cx() + 4, y + 64 + 6 * 30 + 34, 0xFF5A6A90, false);
    }

    /** v0.9.5：自绘列表 hover 列判定统一（0=未命中行 / 1=左列 / 2=右列），替换 7 处重复行列判断；
     *  左列 cx+4~<115、右列 cx+115~226、行高 14px（rowY-1 ~ rowY+13），与原判定完全等价 */
    private int hoverColumn(int mx, int my, int rowY) {
        // v0.9.12 修复：滚出内容视口的行不再响应悬停——此前鼠标停在底部返回按钮上，
        // 仍会命中一条被裁掉的列表行，弹出未显示的羁绊/属性介绍
        if (my < hoverClipTop || my > hoverClipBottom) return 0;
        if (rowY < hoverClipTop || rowY + 13 > hoverClipBottom) return 0;
        if (my < rowY - 1 || my > rowY + 13) return 0;
        if (mx >= cx() + 4 && mx < cx() + 115) return 1;
        if (mx >= cx() + 115 && mx <= cx() + 226) return 2;
        return 0;
    }

    /** v0.9.12 数值符号归一：去掉前导 + 号，供自带符号的提示文案使用 */
    private static String stripSign(String s) {
        if (s == null) return "";
        return s.startsWith("+") ? s.substring(1) : s;
    }

    private void drawAttrRow(GuiGraphics g, int top, int idx, Component name, String value) {
        drawAttrRow(g, top, idx, name, value, 0xFFFFFFFF);
    }

    /** v0.9.12 长名称防护：文本超出可用宽度时截断并加省略号，避免与数值或相邻列重叠 */
    private Component ellipsize(net.minecraft.client.gui.Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        int ellW = font.width("\u2026");
        net.minecraft.network.chat.FormattedText clipped = font.substrByWidth(text, maxWidth - ellW);
        return Component.literal(clipped.getString() + "\u2026");
    }

    /** v0.9.5 显示优化：可指定数值列颜色（组合羁绊已激活行用金色高亮）；名称与数值均做截断防护 */
    private void drawAttrRow(GuiGraphics g, int top, int idx, Component name, String value, int valueColor) {
        net.minecraft.client.gui.Font font = HexFonts.ui();
        int colX = cx() + 6 + (idx % 2 == 0 ? 0 : 112);
        int row = idx / 2;
        Component safeName = ellipsize(font, name, 62);
        Component safeValue = ellipsize(font, Component.literal(value), 44);
        g.drawString(font, safeName, colX, top + row * ROW_H, 0xFFAAB6D8, false);
        g.drawString(font, safeValue, colX + 66, top + row * ROW_H, valueColor, false);
    }

    /** 区块标题（跨两列居中） */
    private void drawSectionTitle(GuiGraphics g, int top, int idx, Component name) {
        int row = idx / 2;
        g.drawString(HexFonts.ui(), name, cx() + 6, top + row * ROW_H, 0xFF9FE4FF, false);
    }

    /** v0.9.12 框标题：标题 + 右侧折叠指示符号，并记录整行标题点击区（点击标题折叠或展开该框） */
    private void drawBoxHeader(GuiGraphics g, int top, int boxIdx, Component name) {
        g.drawString(HexFonts.ui(), name, cx() + 6, top + 3, 0xFF9FE4FF, false);
        String indicator = boxCollapsed[boxIdx] ? "\u25B6" : "\u25BC";
        g.drawString(HexFonts.ui(), indicator, cx() + 216, top + 3, 0xFF9FE4FF, false);
        titleHitboxes.add(new int[]{cx() + 2, top, cx() + 230, top + 14, boxIdx});
    }

    /** v0.9.12 顶部四个锚点：标题行右侧，点击快速定位到对应框；折叠中的框用暗色区分 */
    private void drawAnchors(GuiGraphics g, int y) {
        net.minecraft.client.gui.Font font = HexFonts.ui();
        int anchorY = y + 36;
        int slotW = 44;
        int startX = cx() + 48;
        for (int i = 0; i < ANCHOR_GROUP_KEYS.length; i++) {
            int ax = startX + i * slotW;
            Component label = Component.translatable("hexcraft.gui." + ANCHOR_GROUP_KEYS[i]);
            int color = boxCollapsed[i] ? 0xFF8A94B0 : 0xFF9FE4FF;
            g.drawString(font, label, ax, anchorY, color, false);
            anchorHitboxes.add(new int[]{ax, anchorY - 2, ax + slotW, anchorY + 10, i});
        }
    }

    /** v0.9.12 统一计算各框当前高度（含折叠态）；不显示的框返回 0。渲染、滚动、锚点定位共用同一口径 */
    private int boxHeight(int i) {
        switch (i) {
            case 0: {
                if (boxCollapsed[0]) return 14;
                int contentRows = (ATTR_KEYS.length - MINOR_KEYS.length) / 2;
                if (!minorCollapsed) contentRows += 1 + (MINOR_KEYS.length + 1) / 2;
                return 14 + contentRows * ROW_H + 4;
            }
            case 1: {
                List<String[]> hidden = hiddenRows();
                if (hidden.isEmpty()) return 0;
                if (boxCollapsed[1]) return 14;
                int rows = 1 + (hidden.size() + 1) / 2;
                return 14 + rows * ROW_H + 4;
            }
            case 2: {
                List<PlayerData.HexEntry> hx = stackableHexes();
                List<PlayerData.EquipEntry> eq = stackableEquips();
                boolean battle = battleStackCount() > 0;
                if (hx.isEmpty() && eq.isEmpty() && !battle) return 0;
                if (boxCollapsed[2]) return 14;
                int hexRows = hx.isEmpty() ? 0 : 1 + (hx.size() + 1) / 2;
                int eqRows = eq.isEmpty() ? 0 : 1 + (eq.size() + 1) / 2;
                int bRows = battle ? 1 + (battleStackCount() + 1) / 2 : 0;
                return 14 + (hexRows + eqRows + bRows) * ROW_H + 4;
            }
            default: {
                java.util.List<ComboRow> cr = comboRows();
                boolean tier = comboStackCount() >= 2;
                if (cr.isEmpty() && !tier) return 0;
                if (boxCollapsed[3]) return 14;
                // v0.9.13 修复：成员数为奇数时最后一条独占半行，叠层档位行必须另起一行；
                // 旧公式 ((成员数+档位)+1)/2 在奇数时少算一整行，导致档位行画到框外
                int memberLines = (cr.size() + 1) / 2;
                int tierLines = tier ? 1 : 0;
                return 14 + (memberLines + tierLines) * ROW_H + 4;
            }
        }
    }

    /** v0.9.12 锚点定位：计算让指定框顶部对齐可视区顶部所需的滚动量，并夹取到合法范围 */
    private void scrollToBox(int i) {
        int acc = 0;
        for (int k = 0; k < i; k++) {
            int h = boxHeight(k);
            if (h > 0) acc += h + 4;
        }
        attrScroll = Math.max(0, Math.min(acc, maxAttrScroll()));
    }

    /** v0.9.12 数值符号归一：正数前缀 +，负数直接用负号，已有符号不再重复加，
     *  避免界面出现「+-85%」「++85%」这类重复符号 */
    private static String signNum(String s) {
        if (s == null || s.isEmpty()) return "+0";
        if (s.startsWith("+") || s.startsWith("-")) return s;
        return s.startsWith("-") ? s : "+" + s;
    }

    private static String signPct(int pct) { return signNum(String.valueOf(pct)) + "%"; }

    /** v0.9.24 叠层之源档位值渲染：400/600/800/1000 直接写层数，0 表示无上限写成 ∞ */
    private static String capTierText(double v) {
        if (v <= 0) return Component.translatable("hexcraft.unit.cap_inf").getString();
        return Component.translatable("hexcraft.unit.cap_tier", (int) v).getString();
    }

    private String fmtEffect(double v, boolean percent) {
        if (v <= 0) return "--";
        if (percent) {
            double p = v * 100;
            String s = String.format("%.1f", p);
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
            return "+" + s + "%";
        }
        return "+" + fmt(v);
    }

    /** v0.9.7 修复轮：叠层上限文本——个人解锁无上限后服务端下发 Integer.MAX_VALUE，显示 ∞ */
    private static String capText(int cap) {
        return cap >= Integer.MAX_VALUE ? "\u221E" : String.valueOf(cap);
    }

    private int maxAttrScroll() {
        // v0.9.12 统一用 boxHeight 口径累加（含折叠态与框间隔），保证渲染与滚动严格一致
        int scrollH = 0;
        for (int i = 0; i < 4; i++) {
            int h = boxHeight(i);
            if (h > 0) scrollH += h + 4;
        }
        int viewH = H - 34 - 68;
        return Math.max(0, scrollH - viewH);
    }

    private int battleStackCount() {
        int n = 0;
        for (PlayerData.StackEntry bs : ClientDataStore.stacks) if (bs.combat) n++;
        return n;
    }

    private void renderHexes(GuiGraphics g, int y) {
        // v0.6.11 已拥有数量 + 栏位占用/上限（服务端权威 occSlots/hexLimit；满员红色提示）
        // v0.8.0 图鉴统计条：已拥有 / 当前分类可获取总数
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.hex_count", ClientDataStore.hexes.size(), totalHexCount()), cx() + 4, y + 68, 0xFF9FE4FF, false);
        String limStr = ClientDataStore.hexLimit >= Integer.MAX_VALUE ? "\u221E" : String.valueOf(ClientDataStore.hexLimit);
        Component slotText = Component.translatable("hexcraft.gui.hex_slot_bar", ClientDataStore.occSlots, limStr);
        boolean full = ClientDataStore.hexLimit < Integer.MAX_VALUE && ClientDataStore.occSlots == ClientDataStore.hexLimit;
        g.drawString(HexFonts.ui(), slotText, cx() + 226 - HexFonts.ui().width(slotText), y + 68, full ? 0xFFFF6B6B : 0xFFFFD28C, false);
        int top = y + 78;
        int bottom = y + 256;
        java.util.List<PlayerData.HexEntry> list = visibleHexes();
        int ly = top - hexScroll;
        for (var h : list) {
            HextechDefinition def = ClientDataStore.hexDef(h.id); // v0.6.4 联机缓存兜底
            if (def == null) { ly += HEX_ROW_H; continue; } // 旧档残留（已删除海克斯）不显示
            if (ly + 8 < top) { ly += HEX_ROW_H; continue; }
            if (ly > bottom) break;
            String rk = def.rarity.key;
            Component name = Component.translatable("hexcraft.hextech." + h.id + ".name")
                    .copy().append("  Lv." + h.level);
            // v0.6.16 双叠层显示：命中层 ×N + 受击层（独立 hurtStacks）；超出服务端叠层上限时标注「生效层数」，
            // 避免"显示 764 层但加成只按上限算"造成的层数/属性对不上困惑
            if (h.stacks > 0 || h.hurtStacks > 0) {
                StringBuilder st = new StringBuilder();
                if (h.stacks > 0) {
                    st.append(Component.translatable("hexcraft.unit.stack_inline_x", h.stacks).getString());
                    if (ClientDataStore.permStackCap < h.stacks) st.append(Component.translatable("hexcraft.unit.effective_cap", ClientDataStore.permStackCap).getString());
                }
                if (h.hurtStacks > 0) {
                    st.append(Component.translatable("hexcraft.unit.stack_inline_hurt", h.hurtStacks).getString());
                    if (ClientDataStore.permStackCap < h.hurtStacks) st.append(Component.translatable("hexcraft.unit.effective_cap", ClientDataStore.permStackCap).getString());
                }
                name = name.copy().append(st.toString());
            }
            // v0.9.27 P3：行首档位徽标（单条 / 方框 / 双框），不依赖颜色也能分辨稀有度
            RollScreen.CardButton.drawRarityGlyph(g, cx() + 6, ly + 2, rarityColor(rk), 255, rk);
            g.drawString(HexFonts.ui(), name, cx() + 16, ly, rarityColor(rk), false);
            // 来源角标：联动 mod 内容（原版不标）——TACZ 青色 / 其它联动金色，一眼区分内容来源
            // v0.8.4：角标动态定位（名字后 4px），超右缘则不显示——第二批联动 modid 较长（CATACLYSM/FARMERSDELIGHT 等）固定 x 会溢出
            String src = HextechRegistry.sourceOf(h.id);
            if (!"vanilla".equals(src)) {
                int tagX = cx() + 16 + HexFonts.ui().width(name) + 4;
                Component tag = HextechRegistry.sourceDisplayName(src);
                if (tagX + HexFonts.ui().width(tag) <= cx() + 226) {
                    g.drawString(HexFonts.ui(), tag, tagX, ly,
                            "tacz".equals(src) ? 0xFF4DE6FF : 0xFFFFD700, false);
                }
            }
            // hover 提示：悬停查看海克斯具体效果（名称 + 稀有度 + 描述 + 来源）
            if (hoverX >= cx() + 4 && hoverX <= cx() + 210 && hoverY >= ly - 1 && hoverY <= ly + HEX_ROW_H - 2) {
                java.util.List<Object> tip = new java.util.ArrayList<>();
                tip.add(name.copy().withStyle(c -> c.withColor(rarityColor(rk))));
                // v0.8.5 稀有度行（白银/黄金/棱彩），一眼看出品质档位
                tip.add(Component.translatable("hexcraft.rarity." + rk).withStyle(c -> c.withColor(rarityColor(rk))));
                tip.add(Component.translatable("hexcraft.hextech." + h.id + ".desc"));
                // v0.6.2 叠层标签后缀：tooltip 运行时自动拼接【叠层：xxx｜xxx｜xxx】（新增海克斯仅改 JSON stackTag 数组）
                String tagSuffix = com.hexcraft.hextech.StackTagKeys.tooltipSuffix(
                        ClientDataStore.hexDef(h.id), net.minecraft.client.Minecraft.getInstance().options.languageCode.startsWith("zh"));
                if (tagSuffix != null) {
                    tip.add(Component.literal(tagSuffix).withStyle(c -> c.withColor(0xFF8FA6C8)));
                }
                if (!"vanilla".equals(src)) {
                    tip.add(Component.translatable("hexcraft.gui.source_tag",
                            HextechRegistry.sourceDisplayName(src)).withStyle(c -> c.withColor(0xFF4DE6FF)));
                }
                hoverTip = tip;
            }
            ly += HEX_ROW_H;
        }
        if (list.isEmpty()) {
            g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.cat_empty"), cx() + 6, top + 4, 0xFF5A6A90, false);
        }
        // v0.9.3 修正6：滚动提示统一移除（底部不再有「滚动查看更多」文字叠压内容）
    }

    /** 当前分类过滤后的海克斯列表（全部时直接返回原列表；其余分类缓存，分类或数据版本变化时重建） */
    private java.util.List<PlayerData.HexEntry> visibleHexes() {
        if (hexCat == 0) return ClientDataStore.hexes;
        if (this.cachedVisible != null && this.cachedVisCat == hexCat
                && this.cachedVisVersion == ClientDataStore.dataVersion) {
            return this.cachedVisible;
        }
        java.util.List<PlayerData.HexEntry> out = new java.util.ArrayList<>();
        for (var h : ClientDataStore.hexes) {
            com.hexcraft.hextech.HextechDefinition def = ClientDataStore.hexDef(h.id); // v0.6.4 联机缓存兜底
            if (def == null) continue;
            if (hexCategoryOf(h.id, def) == hexCat) out.add(h);
        }
        this.cachedVisible = out;
        this.cachedVisCat = hexCat;
        this.cachedVisVersion = ClientDataStore.dataVersion;
        return out;
    }

    /** v0.8.0 图鉴统计条：当前分类可获取海克斯总数（注册表同步缓存；分类/数据版本/注册表大小变化时重建） */
    private int totalHexCount() {
        if (hexCat == 0) return ClientDataStore.hexRegistryCache.size();
        int regSize = ClientDataStore.hexRegistryCache.size();
        if (this.cachedTotalCat == hexCat && this.cachedTotalVersion == ClientDataStore.dataVersion
                && this.cachedTotalRegSize == regSize) {
            return this.cachedTotal;
        }
        int n = 0;
        for (com.hexcraft.hextech.HextechDefinition d : ClientDataStore.hexRegistryCache.values()) {
            if (hexCategoryOf(d.id, d) == hexCat) n++;
        }
        this.cachedTotal = n;
        this.cachedTotalCat = hexCat;
        this.cachedTotalVersion = ClientDataStore.dataVersion;
        this.cachedTotalRegSize = regSize;
        return n;
    }

    /** 海克斯分类判定：优先按 ID 关键词（tacz→枪械、enchant→附魔、mining/miner→矿物、firework→趣味），
     *  其次按事件 key 归类（矿工智慧 mining_xp→矿物、时运/冶炼/磁吸→矿物、TACZ 词条→枪械、附魔词条→附魔）；
     *  stack_attr 特判第二段 atk→战斗 / hp→生存；未识别默认战斗 */
    private static int hexCategoryOf(String id, com.hexcraft.hextech.HextechDefinition def) {
        String idl = id.toLowerCase(java.util.Locale.ROOT);
        if (idl.contains("tacz")) return 5;        // 枪械（含 hex_tacz_*）
        if (idl.contains("enchant")) return 6;     // 附魔（含 hex_enchant_*）
        if (idl.contains("mining") || idl.contains("miner")) return 7; // 矿物（含 hex_mining_*、矿工智慧）
        if (idl.contains("firework")) return 8;    // 趣味（烟花之王/烟花盛典）
        String ev = def != null && def.event != null ? def.event : "";
        for (String tok : ev.split(",")) {
            int ci = tok.indexOf(':');
            String ek = ci > 0 ? tok.substring(0, ci) : tok;
            if (ek.startsWith("enchant_")) return 6;
            if (ek.startsWith("mining_")) return 7;
            if (ek.startsWith("tacz_")) return 5;
            int c = switch (ek) {
                // 战斗
                case "crit", "crit_damage", "full_hp_crit", "melee_power", "ranged_power", "execute",
                     "echo", "fire_chain", "frost_bite", "rampage", "twin_shot", "counter", "thorns",
                     "kill_streak", "low_hp_power", "night_power", "boss_power", "vamp_rage",
                     "potion_strength", "splash", "fire_resist", "fire_warden" -> 1;
                // 生存
                case "regen", "kill_heal", "lifesteal", "overshield", "shield_low", "death_defy",
                     "arcane_shield", "surge", "cleanse", "food_on_kill", "food_on_hit", "last_stand" -> 2;
                // 成长
                case "xp_bonus", "xp_drop", "jump", "mobi_power", "stack_growth", "stack_chance",
                     "equip_count_atk", "equip_count_armor", "equip_count_hp", "slot_boost" -> 3;
                // 经济
                case "coin_bonus", "bounty", "gold_gamble", "gold_punch", "loot_bonus", "loot_double" -> 4;
                // 趣味
                case "firework", "dice_forge", "potion_random", "random_loot" -> 8;
                default -> -1;
            };
            if (c > 0) return c;
            // 叠层成长属性特判：stack_attr / stack_attr_onhit / stack_attr_onhurt 的键 hp、max_health → 生存，atk、attack_damage → 战斗
            if (ek.startsWith("stack_attr")) {
                int ci2 = tok.indexOf(':', ci + 1);
                String attr = ci2 > 0 ? tok.substring(ci + 1, ci2) : "";
                if ("hp".equals(attr) || "max_health".equals(attr)) return 2;
                if ("atk".equals(attr) || "attack_damage".equals(attr)) return 1;
            }
        }
        return 1; // 兜底战斗
    }

    private int maxHexScroll() {
        return Math.max(0, visibleHexes().size() * HEX_ROW_H - (256 - 78));
    }

    /** v0.9.12 判定点是否落在命中区 {左, 上, 右, 下} */
    private static boolean insideHitbox(int x, int y, int[] hb) {
        return x >= hb[0] && x <= hb[2] && y >= hb[1] && y <= hb[3];
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (tab == 0 && btn == 0) {
            double s = uiScale();
            int lx = (int) (mx / s), ly = (int) (my / s);
            // 顶部锚点：点击快速定位到对应框
            for (int[] hb : anchorHitboxes) {
                if (insideHitbox(lx, ly, hb)) {
                    scrollToBox(hb[4]);
                    return true;
                }
            }
            // 次要组标题：点击折叠或展开不常用项
            if (minorHitbox != null && insideHitbox(lx, ly, minorHitbox)) {
                minorCollapsed = !minorCollapsed;
                return true;
            }
            // 框标题：点击折叠或展开整框
            for (int[] hb : titleHitboxes) {
                if (insideHitbox(lx, ly, hb)) {
                    boxCollapsed[hb[4]] = !boxCollapsed[hb[4]];
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == 3) {
            forgeScroll = Math.max(0, Math.min(maxForgeScroll(), forgeScroll - (int) Math.round(delta * 12)));
            return true;
        }
        if (tab == 1) {
            hexScroll = Math.max(0, Math.min(maxHexScroll(), hexScroll - (int) Math.round(delta * HEX_ROW_H)));
            return true;
        }
        if (tab == 2) {
            bondScroll = Math.max(0, Math.min(maxBondScroll(), bondScroll - (int) Math.round(delta * ROW_H)));
            return true;
        }
        if (tab == 0) {
            attrScroll = Math.max(0, Math.min(maxAttrScroll(), attrScroll - (int) Math.round(delta * ROW_H)));
            return true;
        }
        // v0.6.5 装备图鉴滚动（仅图鉴模式）
        if (tab == 4 && equipGallery) {
            galleryScroll = Math.max(0, Math.min(maxGalleryScroll(), galleryScroll - (int) Math.round(delta * 13)));
            return true;
        }
        return super.mouseScrolled(mouseX / uiScale(), mouseY / uiScale(), delta);
    }

    /** 锻造器分类组：标题条 + 组内词条（词条按组内索引分两列，标题不占列，v0.4.3 修复错位） */
    private static class ForgeGroup {
        final String tierKey;
        final int color;
        final int tierIdx;
        final java.util.List<Object[]> items = new java.util.ArrayList<>();

        ForgeGroup(String tierKey, int color, int tierIdx) {
            this.tierKey = tierKey;
            this.color = color;
            this.tierIdx = tierIdx;
        }
    }

    private void renderForge(GuiGraphics g, int y) {
        String forgeText = ClientDataStore.maxForges <= 0
                ? Component.translatable("hexcraft.gui.forge_done", ClientDataStore.forgesUsed).getString()
                : Component.translatable("hexcraft.gui.forge_count", ClientDataStore.forgesUsed, ClientDataStore.maxForges).getString();
        g.drawString(HexFonts.ui(), forgeText, cx() + 4, y + 36, 0xFF9FE4FF, false);
        // v0.4.4：标题上移 5px（y+81→y+76），避免与下方「白银」分组框顶部重叠
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.forged"), cx() + 4, y + 76, 0xFF9FE4FF, false);
        // v0.4.3：分类分组框（科技风：框体 + 档位色条 + 组名 + 计数），组内两列排布
        java.util.List<ForgeGroup> groups = forgeGroups();
        if (groups.isEmpty()) {
            g.drawString(HexFonts.ui(), "--", cx() + 6, y + 91, 0xFF3A4668, false);
            return;
        }
        int top = y + 88;
        int bottom = y + 234;
        // v0.4.5：滚动区裁剪——分组框/标题条滚出可视区时被裁掉，不再溢出遮挡「已锻造属性」与价格按钮
        double s = uiScale();
        g.enableScissor((int) ((cx() + 2) * s), (int) ((y + 76) * s),
                (int) ((cx() + 232) * s), (int) ((y + 238) * s));
        int ly = top - forgeScroll;
        for (ForgeGroup grp : groups) {
            int rows = (grp.items.size() + 1) / 2;
            int groupH = 16 + rows * 13 + 4;
            if (ly + groupH < top) { ly += groupH + 6; continue; } // 整组在可视区上方
            if (ly > bottom) break;                                 // 已滚过可视区
            // 分组框：半透明底 + 细边框
            g.fill(cx() + 2, ly, cx() + 230, ly + groupH, 0x180A1A2E);
            g.fill(cx() + 2, ly, cx() + 230, ly + 1, 0xFF30466B);
            g.fill(cx() + 2, ly + groupH - 1, cx() + 230, ly + groupH, 0xFF30466B);
            g.fill(cx() + 2, ly, cx() + 3, ly + groupH, 0xFF30466B);
            g.fill(cx() + 229, ly, cx() + 230, ly + groupH, 0xFF30466B);
            // 标题条：左侧档位色条 + 组名（档位色）+ v0.6.5 档位倍率提示 + 右侧词条计数
            g.fill(cx() + 3, ly + 3, cx() + 5, ly + 13, grp.color);
            Component tierNameC = Component.translatable("hexcraft.forge.tier." + grp.tierKey);
            g.drawString(HexFonts.ui(), tierNameC, cx() + 8, ly + 3, grp.color, false);
            // v0.6.5 档位倍率：组名右侧显示实际加成倍率（读运行时配置，与 tooltip 数值一致）
            double tierMultV = com.hexcraft.hextech.HextechRegistry.tierMult(grp.tierIdx);
            String multStr = "×" + fmt(tierMultV);
            g.drawString(HexFonts.ui(), Component.literal(multStr),
                    cx() + 8 + HexFonts.ui().width(tierNameC) + 5, ly + 3, 0xFF7FD8FF, false);
            g.drawString(HexFonts.ui(), Component.literal(Component.translatable("hexcraft.unit.count_items", grp.items.size()).getString()), cx() + 178, ly + 3, 0xFF6A7B96, false);
            // 组内词条两列（组内索引分列，不跨组，标题不再挤占列位）；v0.4.4 行距 12→13 防文字挤压
            for (int i = 0; i < grp.items.size(); i++) {
                Object[] it = grp.items.get(i);
                int colX = cx() + 6 + (i % 2) * 112;
                int iy = ly + 17 + (i / 2) * 13;
                if (iy < top) continue;
                if (iy > bottom) break;
                String nameStr = (String) it[0];
                g.drawString(HexFonts.ui(), nameStr, colX, iy, 0xFFAAB6D8, false);
                // v0.4.5：来源角标按词条名实际宽度动态定位（名字后 4px），不再固定 colX+100 侵入右列词条
                // v0.9.0 C4：角标改为通用来源 tag（TACZ/IRONS/IAF…），不再写死 TACZ
                String tagStr = (String) it[1];
                if (tagStr != null) {
                    Component tagC = HextechRegistry.sourceDisplayName(tagStr);
                    int tagX = colX + HexFonts.ui().width(nameStr) + 4;
                    if (tagX + HexFonts.ui().width(tagC) <= colX + 110) {
                        g.drawString(HexFonts.ui(), tagC, tagX, iy, 0xFF4DE6FF, false);
                    }
                }
                // hover 提示：悬停查看锻造词条具体效果（名称 + 实际加成 + 档位 + 描述 + 来源）
                // v0.6.2：lang 模板占位符动态渲染——{name}/{mult}/{displayValue}/{tierName} 运行时按档位倍率填充，
                // 修改 forgeMultiplierSilver/Gold/Prismatic 配置后 tooltip 数值自动跟随（无需改 lang）
                if (hoverX >= colX && hoverX <= colX + 110 && hoverY >= iy - 1 && hoverY <= iy + 12) {
                    java.util.List<Object> tip = new java.util.ArrayList<>();
                    String stat = (String) it[2];
                    String real = forgeValueText(stat, grp.tierIdx);
                    String tierName = Component.translatable("hexcraft.forge.tier." + grp.tierKey).getString();
                    double mult = com.hexcraft.hextech.HextechRegistry.tierMult(grp.tierIdx);
                    String multText = "×" + String.format(java.util.Locale.ROOT, "%.1f", mult).replaceFirst("\\.0$", "");
                    String nameTpl = Component.translatable("hexcraft.forge." + stat + ".name").getString();
                    String descTpl = Component.translatable("hexcraft.forge." + stat + ".desc").getString();
                    tip.add(Component.literal(nameTpl
                            .replace("{name}", nameStr)
                            .replace("{mult}", multText)
                            .replace("{displayValue}", real)
                            .replace("{tierName}", tierName)).withStyle(c -> c.withColor(grp.color)));
                    tip.add(Component.literal(descTpl
                            .replace("{tierName}", tierName)
                            .replace("{displayValue}", real)));
                    if (it[1] != null) {
                        tip.add(Component.translatable("hexcraft.gui.source_tag",
                                HextechRegistry.sourceDisplayName((String) it[1])).withStyle(c -> c.withColor(0xFF4DE6FF)));
                    }
                    hoverTip = tip;
                }
            }
            ly += groupH + 6;
        }
        g.disableScissor();
        // v0.9.3 修正6：锻造页滚动提示统一移除
    }

    private int maxForgeScroll() {
        return Math.max(0, forgeGroupHeight() - (234 - 88));
    }

    /** v0.6.1 锻造词条实际加成文本：基础值 × 档位倍率（读取运行时配置）；百分比词条转百分号展示
     *  v0.9.0 C4：百分比判定并入统一 isPercentForgeStat（hppct/aspdpct/atkpct/armorpct/luckpct） */
    private String forgeValueText(String stat, int tier) {
        com.hexcraft.hextech.HextechDefinition.ModSpec m = com.hexcraft.hextech.HextechRegistry.forgeMod(stat);
        if (m == null) return "?";
        double v = m.perLevel * com.hexcraft.hextech.HextechRegistry.tierMult(tier);
        if (com.hexcraft.hextech.HextechRegistry.isPercentForgeStat(stat)) {
            String p = String.format(java.util.Locale.ROOT, "%.2f", v * 100.0).replaceFirst("\\.?0+$", "");
            return signNum(p) + "%";
        }
        String s = String.format(java.util.Locale.ROOT, "%.4f", v);
        s = s.replaceFirst("0+$", "");
        if (s.endsWith(".")) s += "0";
        return signNum(s);
    }

    /** 全部分组总高度（含组间距 6） */
    private int forgeGroupHeight() {
        int h = 0;
        for (ForgeGroup grp : forgeGroups()) {
            int rows = (grp.items.size() + 1) / 2;
            h += 16 + rows * 13 + 4 + 6;
        }
        return h;
    }

    /** 已锻造词条按实际锻造档位分组（v0.5.0：档位即强度，白银 ×1 / 黄金 ×2 / 棱彩 ×3——在哪档抽的显示在哪组） */
    private java.util.List<ForgeGroup> forgeGroups() {
        java.util.List<ForgeGroup> groups = new java.util.ArrayList<>();
        int[] tierColors = {0xFFD8D8D8, 0xFFFFC94A, 0xFFC77DFF};
        String[] tierKeys = {"basic", "gold", "prismatic"};
        for (int t = 0; t < 3; t++) {
            Map<String, Integer> agg = new LinkedHashMap<>();
            for (PlayerData.ForgeEntry f : ClientDataStore.forges)
                if (f.tier == t) agg.merge(f.stat, 1, Integer::sum);
            if (agg.isEmpty()) continue;
            ForgeGroup grp = new ForgeGroup(tierKeys[t], tierColors[t], t);
            for (Map.Entry<String, Integer> e : agg.entrySet()) {
                String name = Component.translatable("hexcraft.forge." + e.getKey() + ".name").getString();
                // v0.9.0 C4：来源角标通用化——任意事件型联动词条按来源 mod 显示角标（原版属性词条 null 不标）
                String tag = com.hexcraft.hextech.HextechRegistry.forgeStatSourceTag(e.getKey());
                grp.items.add(new Object[]{name + "  ×" + e.getValue(), tag, e.getKey()});
            }
            groups.add(grp);
        }
        return groups;
    }

    private void renderEquip(GuiGraphics g, int y) {
        // v0.6.5 图鉴模式：标题由切换按钮承担，此处直接分流
        if (equipGallery) { renderEquipGallery(g, y); return; }
        int ly = y + 91;
        for (int i = 0; i < PlayerData.MAX_EQUIP; i++) {
            g.fill(cx() + 3, ly - 1, cx() + 228, ly + 11, 0x22305080);
            if (i < ClientDataStore.equipment.size()) {
                PlayerData.EquipEntry eq = ClientDataStore.equipment.get(i);
                com.hexcraft.hextech.HextechDefinition def = ClientDataStore.equipDef(eq.id); // v0.6.4 联机缓存兜底
                String rk = def != null ? def.rarity.key : "gold";
                String icon = def != null && def.icon != null ? def.icon : "◆";
                String nm = Component.translatable("hexcraft.equip." + eq.id + ".name").getString();
                // v0.9.27 P3：行首档位徽标（单条 / 方框 / 双框），不依赖颜色也能分辨稀有度
                RollScreen.CardButton.drawRarityGlyph(g, cx() + 6, ly + 2, rarityColor(rk), 255, rk);
                g.drawString(HexFonts.ui(), icon + " " + nm, cx() + 16, ly, rarityColor(rk), false);
                // 来源角标：联动装备行尾角标（原版不标）；v0.8.4 动态定位防长 modid 溢出
                String src = HextechRegistry.sourceOf(eq.id);
                if (!"vanilla".equals(src)) {
                    int tagX = cx() + 16 + HexFonts.ui().width(icon + " " + nm) + 4;
                    Component tag = HextechRegistry.sourceDisplayName(src);
                    if (tagX + HexFonts.ui().width(tag) <= cx() + 228) {
                        g.drawString(HexFonts.ui(), tag, tagX, ly, 0xFF4DE6FF, false);
                    }
                }
                // hover 提示：悬停查看装备具体效果（名称 + 描述 + 卖价 + 来源）
                if (def != null && hoverX >= cx() + 3 && hoverX <= cx() + 228 && hoverY >= ly - 1 && hoverY <= ly + 13) {
                    net.minecraft.network.chat.MutableComponent eqName = Component.translatable("hexcraft.equip." + eq.id + ".name").copy();
                    if (eq.level > 0) eqName = eqName.append("  Lv." + eq.level);
                    if (eq.stacks > 0) {
                        eqName = eqName.append(Component.translatable("hexcraft.unit.stack_inline_x", eq.stacks).getString());
                        if (ClientDataStore.permStackCap < eq.stacks) eqName = eqName.append(Component.translatable("hexcraft.unit.effective_cap", ClientDataStore.permStackCap).getString());
                    }
                    java.util.List<Object> tip = new java.util.ArrayList<>();
                    tip.add(eqName.withStyle(c -> c.withColor(rarityColor(rk))));
                    tip.add(Component.translatable("hexcraft.equip." + eq.id + ".desc"));
                    // v0.6.2 装备叠层标签后缀（心之钢等）
                    String eqTag = com.hexcraft.hextech.StackTagKeys.tooltipSuffix(def,
                            net.minecraft.client.Minecraft.getInstance().options.languageCode.startsWith("zh"));
                    if (eqTag != null) {
                        tip.add(Component.literal(eqTag).withStyle(c -> c.withColor(0xFF8FA6C8)));
                    }
                    if (def.sell > 0) tip.add(Component.translatable("hexcraft.gui.sell_hint", def.sell));
                    if (!"vanilla".equals(src)) {
                        tip.add(Component.translatable("hexcraft.gui.source_tag",
                                HextechRegistry.sourceDisplayName(src)).withStyle(c -> c.withColor(0xFF4DE6FF)));
                    }
                    hoverTip = tip;
                }
            } else {
                g.drawString(HexFonts.ui(), "--", cx() + 6, ly, 0xFF3A4668, false);
            }
            ly += 16;
        }
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.equip_hint"), cx() + 4, y + 200, 0xFF5A6A90, false);
    }

    /** v0.6.5 装备图鉴：全部装备（含未拥有）列表，可滚动；列=名称/已拥有 Lv/卖价，来源在悬停提示 */
    private void renderEquipGallery(GuiGraphics g, int y) {
        java.util.List<String> ids = allEquipIds();
        // v0.8.0 图鉴统计条：已拥有 / 总数（右对齐与图鉴按钮同行，不占列表空间）
        int ownedCnt = 0;
        for (String id : ids) if (ownedEquip(id) != null) ownedCnt++;
        Component gs = Component.translatable("hexcraft.gui.gallery_stat", ownedCnt, ids.size());
        g.drawString(HexFonts.ui(), gs, cx() + 226 - HexFonts.ui().width(gs), y + 72, 0xFF9FE4FF, false);
        int top = y + 91;
        int bottom = y + 234;
        double s = uiScale();
        g.enableScissor((int) ((cx() + 1) * s), (int) ((top - 2) * s), (int) ((cx() + 230) * s), (int) ((bottom + 1) * s));
        int ly = top - galleryScroll;
        for (String id : ids) {
            if (ly + 12 < top) { ly += 13; continue; }
            if (ly > bottom) break;
            com.hexcraft.hextech.HextechDefinition def = ClientDataStore.equipDef(id);
            String rk = def != null ? def.rarity.key : "gold";
            String icon = def != null && def.icon != null ? def.icon : "◆";
            String nm = Component.translatable("hexcraft.equip." + id + ".name").getString();
            g.fill(cx() + 3, ly - 1, cx() + 228, ly + 11, 0x18305080);
            // v0.9.27 P3：行首档位徽标（单条 / 方框 / 双框），不依赖颜色也能分辨稀有度
            RollScreen.CardButton.drawRarityGlyph(g, cx() + 6, ly + 2, rarityColor(rk), 255, rk);
            g.drawString(HexFonts.ui(), icon + " " + nm, cx() + 16, ly, rarityColor(rk), false);
            // 已拥有 Lv 标识
            PlayerData.EquipEntry owned = ownedEquip(id);
            if (owned != null) {
                g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.owned_tag", owned.level).getString(),
                        cx() + 96, ly, 0xFFFFD28C, false);
            }
            // 卖价
            if (def != null && def.sell > 0) {
                g.drawString(HexFonts.ui(), def.sell + "◆", cx() + 190, ly, 0xFF8FA6C8, false);
            }
            // hover 提示：名称 + 描述 + 来源 + 卖价 + 叠层标签
            if (def != null && hoverX >= cx() + 3 && hoverX <= cx() + 228 && hoverY >= ly - 1 && hoverY <= ly + 13) {
                net.minecraft.network.chat.MutableComponent eqName = Component.translatable("hexcraft.equip." + id + ".name").copy();
                if (owned != null) {
                    if (owned.level > 0) eqName = eqName.append("  Lv." + owned.level);
                    if (owned.stacks > 0) eqName = eqName.append(Component.translatable("hexcraft.unit.stack_inline_x", owned.stacks).getString());
                }
                java.util.List<Object> tip = new java.util.ArrayList<>();
                tip.add(eqName.withStyle(c -> c.withColor(rarityColor(rk))));
                tip.add(Component.translatable("hexcraft.equip." + id + ".desc"));
                String eqTag = com.hexcraft.hextech.StackTagKeys.tooltipSuffix(def,
                        net.minecraft.client.Minecraft.getInstance().options.languageCode.startsWith("zh"));
                if (eqTag != null) {
                    tip.add(Component.literal(eqTag).withStyle(c -> c.withColor(0xFF8FA6C8)));
                }
                if (def.sell > 0) tip.add(Component.translatable("hexcraft.gui.sell_hint", def.sell));
                String src = HextechRegistry.sourceOf(id);
                if (!"vanilla".equals(src)) {
                    tip.add(Component.translatable("hexcraft.gui.source_tag",
                            HextechRegistry.sourceDisplayName(src)).withStyle(c -> c.withColor(0xFF4DE6FF)));
                }
                hoverTip = tip;
            }
            ly += 13;
        }
        g.disableScissor();
        // 滚动提示（仅内容超高时）
        if (ids.size() * 13 > bottom - top) {
            g.drawString(HexFonts.ui(), "▽", cx() + 218, bottom - 10, 0xFF5A6A90, false);
        }
    }

    /** v0.6.5 图鉴数据源：内置 44 装备定义 + 联机同步缓存（去重保序，联机可用） */
    private static java.util.List<String> allEquipIds() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (com.hexcraft.hextech.HextechDefinition d : com.hexcraft.hextech.HextechRegistry.allEquips()) ids.add(d.id);
        ids.addAll(ClientDataStore.equipRegistryCache.keySet());
        return new java.util.ArrayList<>(ids);
    }

    /** v0.6.5 已拥有装备查询（客户端缓存，仅显示用） */
    private static PlayerData.EquipEntry ownedEquip(String id) {
        for (PlayerData.EquipEntry e : ClientDataStore.equipment) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }

    private int maxGalleryScroll() {
        return Math.max(0, allEquipIds().size() * 13 - (234 - 91));
    }

    // ================= 工具 =================

    private int cx() { return (this.guiW() - W) / 2 + NAV_W + 12; }
    private int cy() { return (this.guiH() - H) / 2; }

    public static String fmt(double v) {
        String s = String.format("%.2f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static int rarityColor(String key) {
        return switch (key) {
            case "gold" -> 0xFFFFC94A;
            case "prismatic" -> 0xFFC77DFF;
            default -> 0xFFD8D8D8;
        };
    }

    /** 左侧导航按钮：选中态发光指示条 + 蓝色渐变 */
    static class NavButton extends Button {
        private final boolean selected;
        private final int accent;

        NavButton(int x, int y, int w, int h, Component label, boolean selected, OnPress onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
            this.selected = selected;
            this.accent = selected ? ACCENT : 0xFF6A7490;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hot = isHover(mx, my, this);
            int border = selected ? ACCENT : hot ? 0xFFC8D0E8 : 0xFF3A4668;
            int from = selected ? 0x664FD8FF : 0x22283250;
            int to = selected ? 0x22153A66 : 0x180E1424;
            g.fill(getX(), getY(), getX() + width, getY() + height, border & 0x44FFFFFF | 0x22000000);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, 0x00000000);
            g.fillGradient(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, from, to);
            if (selected) {
                g.fill(getX() - 3, getY() + 2, getX() - 1, getY() + height - 2, ACCENT); // 指示条
                g.fillGradient(getX() + 1, getY() + height - 1, getX() + width - 1, getY() + height, 0xFF4FD8FF, 0x227A6BFF); // 底部光带
            }
            int col = selected ? 0xFFFFFFFF : hot ? 0xFFDDE6FF : 0xFF9AA6C8;
            HexFonts.centered(g,
                    this.getMessage(), getX() + width / 2, getY() + (height - 8) / 2, col, false);
        }
    }

    /** 内容按钮：渐变底 + 霓虹描边，悬停发亮 */
    static class HexButton extends Button {
        private final int accent;

        HexButton(int x, int y, int w, int h, Component label, int accent, OnPress onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
            this.accent = accent;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hot = isHover(mx, my, this);
            int border = hot ? 0xFFFFFFFF : accent;
            if (hot) { // 悬停柔光外圈
                g.fill(getX() - 2, getY() - 2, getX() + width + 2, getY() + height + 2, 0x3330A8E0);
            }
            g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, border);
            g.fillGradient(getX(), getY(), getX() + width, getY() + height,
                    hot ? 0x5530A8E0 : 0x331E6FE8, 0xCC10182E);
            int col = hot ? 0xFFFFFFFF : 0xFFC8D8F0;
            HexFonts.centered(g,
                    this.getMessage(), getX() + width / 2, getY() + (height - 8) / 2, col, false);
        }
    }
}