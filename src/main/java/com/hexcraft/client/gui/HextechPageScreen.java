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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 海克斯主界面（海克斯科技风格）：
 * 左侧竖排导航（属性 / 海克斯 / 锻造器 / 装备），右侧内容区；深蓝紫渐变 + 霓虹光效。
 */
public class HextechPageScreen extends Screen {
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
    /** 效果属性（隐藏属性）：effect key → lang key；percent=true 按百分比显示 */
    private static final String[][] EFFECT_KEYS = {
            {"crit", "crit", "1"}, {"melee_power", "melee_power", "1"}, {"ranged_power", "ranged_power", "1"},
            {"lifesteal", "lifesteal", "1"}, {"thorns", "thorns", "0"}, {"jump", "jump", "0"},
            {"dig_speed", "dig_speed", "0"}, {"regen", "regen", "0"}, {"kill_heal", "kill_heal", "0"},
            {"coin_bonus", "coin_bonus", "1"}, {"xp_bonus", "xp_bonus", "1"},
            {"low_hp_power", "low_hp_power", "1"}, {"full_hp_crit", "full_hp_crit", "1"},
            {"night_power", "night_power", "1"}, {"kill_streak", "kill_streak", "1"},
            {"last_stand", "last_stand", "1"}, {"stack_growth", "stack_growth", "1"},
            // —— 复合机制海克斯（v0.1.11 新增）——
            {"echo", "echo", "1"}, {"execute", "execute", "1"}, {"splash", "splash", "1"},
            {"counter", "counter", "1"}, {"shield_low", "shield_low", "0"}, {"rampage", "rampage", "1"},
            {"mobi_power", "mobi_power", "1"}, {"overshield", "overshield", "1"}, {"surge", "surge", "1"},
            {"arcane_shield", "arcane_shield", "1"}, {"bounty", "bounty", "0"}, {"fire_chain", "fire_chain", "0"},
            {"vamp_rage", "vamp_rage", "1"}, {"twin_shot", "twin_shot", "1"},
            {"boss_power", "boss_power", "1"}, {"frost_bite", "frost_bite", "1"}, {"death_defy", "death_defy", "1"},
            // —— 娱乐机制（v0.3.0 新增）——
            {"firework", "firework", "0"}, {"xp_drop", "xp_drop", "0"}, {"gold_punch", "gold_punch", "0"}};
    /** TACZ 联动效果：仅安装 TACZ 时显示 */
    private static final String[][] TACZ_KEYS = {
            {"tacz_power", "tacz_power", "1"}, {"tacz_crit", "tacz_crit", "1"},
            {"tacz_close_power", "tacz_close_power", "1"}, {"tacz_far_power", "tacz_far_power", "1"},
            {"tacz_heal", "tacz_heal", "0"}};
    private static final String[] TABS = {"overview", "hexes", "forge", "equip", "recharge"};
    private static final int ACCENT = 0xFF4FD8FF;
    private static final int ACCENT_VIOLET = 0xFF7A6BFF;

    private int tab;
    private int hexScroll;
    private int attrScroll;
    private int forgeScroll;
    /** 海克斯页分类过滤：0=全部 1=战斗 2=生存 3=成长 4=经济 5=枪械 6=附魔 7=矿物 8=趣味 */
    private int hexCat = 0;
    private static final String[] HEX_CATS = {"all", "combat", "survive", "growth", "economy", "gun", "enchant", "mining", "fun"};
    private static final int[] HEX_CAT_COLORS = {
            0xFF4FD8FF, 0xFFFF6B6B, 0xFF55FF88, 0xFFFFD700, 0xFF8AB4FF,
            0xFFC77DFF, 0xFF6BE4FF, 0xFFFFC94A, 0xFFFF9FE0};
    private int[] navY = new int[5];
    private final java.util.List<float[]> parts = new java.util.ArrayList<>();
    private int pTick;
    private boolean partsInit;
    private static final java.util.Random P_RAND = new java.util.Random();

    public HextechPageScreen() {
        super(Component.translatable("hexcraft.gui.page_title"));
    }

    @Override
    protected void init() {
        int x = (this.width - W) / 2;
        int y = (this.height - H) / 2;

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
        else if (tab == 2) initForge();
        else if (tab == 3) initEquip();
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

    /** 充值材质表：0绿宝石 1铜 2铁 3金 4钻石 5下界合金；每行标签 = 材质 ×单枚晶币 */
    private static final int[] RECHARGE_RATE = {30, 1, 2, 4, 8, 64};
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
    }

    private void sendExchange(int dir, int material, int count) {
        Packets.C2SExchangeCoin p = new Packets.C2SExchangeCoin();
        p.dir = dir;
        p.material = material;
        p.count = count;
        PacketHandler.CHANNEL.sendToServer(p);
    }

    private void initHexes() {
        this.addRenderableWidget(new HexButton(cx() + 4, cy() + 36, 226, 15,
                Component.translatable("hexcraft.gui.draw", HexConfig.XP_COST_PER_DRAW.get()),
                ACCENT, b -> PacketHandler.CHANNEL.sendToServer(new Packets.C2SRequestDraw())));
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
        int[] prices = {HexConfig.FORGE_BASIC_PRICE.get(), HexConfig.FORGE_GOLD_PRICE.get(), HexConfig.FORGE_PRISMATIC_PRICE.get()};
        // 三档按钮颜色：白银 / 黄金 / 棱彩，从低到高易区分
        int[] tierColors = {0xFFD8D8D8, 0xFFFFC94A, 0xFFC77DFF};
        for (int i = 0; i < 3; i++) {
            final int t = i;
            this.addRenderableWidget(new HexButton(cx() + 4 + i * (bw + 5), cy() + 44, bw, 14,
                    Component.translatable("hexcraft.gui.buy_forge",
                            Component.translatable("hexcraft.forge.tier." + TIER_KEYS[t]), prices[i]),
                    tierColors[i], b -> {
                        Packets.C2SBuyForge p = new Packets.C2SBuyForge();
                        p.tier = t;
                        PacketHandler.CHANNEL.sendToServer(p);
                    }));
        }
        this.addRenderableWidget(new HexButton(cx() + 4 + (bw + 5), cy() + 61, bw * 2 - 5, 14,
                Component.translatable("hexcraft.gui.forge_random"), ACCENT,
                b -> PacketHandler.CHANNEL.sendToServer(new Packets.C2SRandomForge())));
    }

    private void initEquip() {
        // 购买按钮：2500 随机装备池 / 4000 随机棱彩装备池（都是三选一）
        this.addRenderableWidget(new HexButton(cx() + 4, cy() + 48, 111, 15,
                Component.translatable("hexcraft.gui.buy_equip", 2500), 0xFFFFC94A,
                b -> { Packets.C2SBuyEquip p = new Packets.C2SBuyEquip(); p.price = 2500; PacketHandler.CHANNEL.sendToServer(p); }));
        this.addRenderableWidget(new HexButton(cx() + 119, cy() + 48, 111, 15,
                Component.translatable("hexcraft.gui.buy_equip_prismatic", 4000), 0xFFC77DFF,
                b -> { Packets.C2SBuyEquip p = new Packets.C2SBuyEquip(); p.price = 4000; PacketHandler.CHANNEL.sendToServer(p); }));
        // 6 槽位卖出按钮（无装备时置灰不生效）
        for (int i = 0; i < PlayerData.MAX_EQUIP; i++) {
            final int idx = i;
            this.addRenderableWidget(new HexButton(cx() + 152, cy() + 91 + i * 16, 58, 12,
                    Component.translatable("hexcraft.gui.equip_sell"), 0xFF8A93A8,
                    b -> { Packets.C2SSellEquip p = new Packets.C2SSellEquip(); p.index = idx; PacketHandler.CHANNEL.sendToServer(p); }));
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
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int x = (this.width - W) / 2;
        int y = (this.height - H) / 2;

        // 深蓝紫渐变底板
        g.fillGradient(x, y, x + W, y + H, 0xF40D1224, 0xE8241836);
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

        super.render(g, mx, my, pt);

        // 标题（顶左上，发光双层）+ 余额（同行右侧，不超界面）
        String title = Component.translatable("hexcraft.gui.page_title").getString();
        g.drawString(this.font, title, x + 12, y + 9, 0xFF1A2340);
        g.drawString(this.font, title, x + 11, y + 8, 0xFFE8F6FF);
        g.drawCenteredString(this.font, Component.translatable("hexcraft.gui.coins", ClientDataStore.coins),
                cx() + 116, y + 10, 0xFFFFD700);
        // 余额光点装饰
        g.fill(cx() + 104, y + 12, cx() + 106, y + 14, 0xFFFFD700);

        if (tab == 0) renderOverview(g, y);
        else if (tab == 1) renderHexes(g, y);
        else if (tab == 2) renderForge(g, y);
        else if (tab == 3) renderEquip(g, y);
        else renderRecharge(g, y);
    }

    private void renderOverview(GuiGraphics g, int y) {
        g.drawString(this.font, Component.translatable("hexcraft.gui.overview_title"), cx() + 4, y + 36, 0xFF9FE4FF);
        // 全部内容（基础/隐藏/叠层三框）整体滚动，保证显示全、不出边框
        g.enableScissor(cx() + 2, y + 46, cx() + 232, cy() + H - 34);
        int top = y + 48 - attrScroll;
        int idx;
        // ===== 框1 基础属性（标题紧凑：间隔 8px、框底留白 6px）=====
        int baseRows = (ATTR_KEYS.length + 1) / 2;
        int box1H = 14 + baseRows * ROW_H + 4;
        drawGroupBox(g, top, box1H);
        drawSectionTitle(g, top + 2, 0, Component.translatable("hexcraft.gui.group_base"));
        idx = 2;
        int inner = top + 4;
        for (int i = 0; i < ATTR_KEYS.length; i++, idx++) {
            String key = ATTR_KEYS[i];
            Double val = ClientDataStore.attrs.get(key);
            drawAttrRow(g, inner, idx, Component.translatable("hexcraft.stat." + key),
                    val == null ? "--" : fmt(val));
        }
        top += box1H + 4;
        // ===== 框2 隐藏属性 =====
        List<String[]> hiddenRows = collectHiddenRows();
        if (!hiddenRows.isEmpty()) {
            int rows = 1 + (hiddenRows.size() + 1) / 2;
            int box2H = 14 + rows * ROW_H + 4;
            drawGroupBox(g, top, box2H);
            drawSectionTitle(g, top + 2, 0, Component.translatable("hexcraft.gui.group_hidden"));
            idx = 2;
            inner = top + 4;
            for (String[] r : hiddenRows) {
                drawAttrRow(g, inner, idx++, Component.translatable("hexcraft.effect." + r[0]), r[1]);
            }
            top += box2H + 4;
        }
        // ===== 框3 成长叠层：所有可叠层海克斯与装备全部展示（含 0 层）=====
        List<PlayerData.HexEntry> stkHexes = stackableHexes();
        List<PlayerData.EquipEntry> stkEqs = stackableEquips();
        if (!stkHexes.isEmpty() || !stkEqs.isEmpty()) {
            int hexRows = stkHexes.isEmpty() ? 0 : 1 + (stkHexes.size() + 1) / 2;
            int eqRows = stkEqs.isEmpty() ? 0 : 1 + (stkEqs.size() + 1) / 2;
            int box3H = 14 + (hexRows + eqRows) * ROW_H + 4;
            drawGroupBox(g, top, box3H);
            drawSectionTitle(g, top + 2, 0, Component.translatable("hexcraft.gui.group_stack"));
            idx = 2;
            inner = top + 4;
            if (!stkHexes.isEmpty()) {
                drawSectionTitle(g, inner, idx, Component.translatable("hexcraft.gui.stack_hex_title"));
                idx += 2;
                for (PlayerData.HexEntry h : stkHexes) {
                    drawAttrRow(g, inner, idx++, Component.translatable("hexcraft.hextech." + h.id + ".name"),
                            "×" + h.stacks + "层");
                }
            }
            if (!stkEqs.isEmpty()) {
                if (idx % 2 == 1) idx++; // 条目为奇数时补到整行，副标题绝不错位
                drawSectionTitle(g, inner, idx, Component.translatable("hexcraft.gui.stack_equip_title"));
                idx += 2;
                for (PlayerData.EquipEntry e : stkEqs) {
                    drawAttrRow(g, inner, idx++, Component.translatable("hexcraft.equip." + e.id + ".name"),
                            "×" + e.stacks + "层");
                }
            }
        }
        g.disableScissor();
        if (maxAttrScroll() > 0) {
            g.drawString(this.font, Component.translatable("hexcraft.gui.scroll_hint"), cx() + 150, cy() + H - 40, 0xFF5A6A90);
        }
    }

    /** 隐藏属性行集合（值 > 0 才显示）：暴击伤害 + 效果属性 + TACZ */
    private List<String[]> collectHiddenRows() {
        List<String[]> rows = new java.util.ArrayList<>();
        double anyCrit = HextechEvents.effect(ClientDataStore.hexes, "crit")
                + HextechEvents.effect(ClientDataStore.hexes, "full_hp_crit")
                + HextechEvents.effect(ClientDataStore.hexes, "tacz_crit");
        if (anyCrit > 0) rows.add(new String[]{"crit_damage", "150%"});
        for (String[] e : EFFECT_KEYS) {
            double v = HextechEvents.effect(ClientDataStore.hexes, e[0]);
            if (v <= 0) continue;
            rows.add(new String[]{e[1], fmtEffect(v, "1".equals(e[2]))});
        }
        if (ModList.get().isLoaded("tacz")) {
            for (String[] e : TACZ_KEYS) {
                double v = HextechEvents.effect(ClientDataStore.hexes, e[0]);
                if (v <= 0) continue;
                rows.add(new String[]{e[1], fmtEffect(v, "1".equals(e[2]))});
            }
        }
        return rows;
    }

    /** 可叠层海克斯：event 含 stack_growth / stack_attr: / stack_attr_onhit:（全部展示，含 0 层） */
    private static List<PlayerData.HexEntry> stackableHexes() {
        List<PlayerData.HexEntry> out = new java.util.ArrayList<>();
        for (PlayerData.HexEntry h : ClientDataStore.hexes) {
            HextechDefinition def = HextechRegistry.get(h.id);
            if (def != null && def.event != null
                    && (def.event.contains("stack_growth") || def.event.contains("stack_attr:")
                    || def.event.contains("stack_attr_onhit:"))) {
                out.add(h);
            }
        }
        return out;
    }

    /** 可叠层装备：event 含 stack_attr: / stack_attr_onhit:（全部展示，含 0 层） */
    private static List<PlayerData.EquipEntry> stackableEquips() {
        List<PlayerData.EquipEntry> out = new java.util.ArrayList<>();
        for (PlayerData.EquipEntry e : ClientDataStore.equipment) {
            HextechDefinition def = HextechRegistry.getEquip(e.id);
            if (def != null && def.event != null
                    && (def.event.contains("stack_attr:") || def.event.contains("stack_attr_onhit:"))) {
                out.add(e);
            }
        }
        return out;
    }

    /** 分组框：半透明深色背景 + 细边框（代替横线，绝不挡字） */
    private void drawGroupBox(GuiGraphics g, int y, int height) {
        g.fill(cx() + 2, y, cx() + 230, y + height, 0x180A1A2E);
        g.fill(cx() + 2, y, cx() + 230, y + 1, 0xFF30466B);
        g.fill(cx() + 2, y + height - 1, cx() + 230, y + height, 0xFF30466B);
        g.fill(cx() + 2, y, cx() + 3, y + height, 0xFF30466B);
        g.fill(cx() + 229, y, cx() + 230, y + height, 0xFF30466B);
    }

    /** 充值页渲染：6 材质两行式（标签在上、按钮在下，按钮按金属配色）+ 晶币→绿宝石行 + 规则提示 */
    private void renderRecharge(GuiGraphics g, int y) {
        g.drawString(this.font, Component.translatable("hexcraft.gui.recharge_title"), cx() + 4, y + 36, 0xFF9FE4FF);
        // 汇率合并单行（短名+比例，不超界面）
        g.drawString(this.font, Component.translatable("hexcraft.gui.recharge_rate"), cx() + 4, y + 48, 0xFF6FA8FF);
        // 分隔线
        g.fill(cx() + 4, y + 58, cx() + 228, y + 59, 0x33505F88);
        // 6 材质标签（每材质块 30px：标签在上，按钮在下；标签用对应金属色）
        int[] tcols = {0xFF55FF55, 0xFFFF8C00, 0xFFD8D8D8, 0xFFFFD700, 0xFF4DE6FF, 0xFFC77DFF};
        int ly = y + 64;
        for (int m = 0; m < 6; m++) {
            g.drawString(this.font, Component.translatable("hexcraft.gui.recharge_m" + m), cx() + 4, ly, tcols[m]);
            ly += 30;
        }
        // 晶币 → 绿宝石 标签（金色；按钮行在其下 8px）
        g.drawString(this.font, Component.translatable("hexcraft.gui.recharge_coin_label"), cx() + 4, y + 64 + 6 * 30 + 6, 0xFFFFD700);
        // 规则提示（短版，单行不超界面）
        g.drawString(this.font, Component.translatable("hexcraft.gui.recharge_hint"), cx() + 4, y + 64 + 6 * 30 + 34, 0xFF5A6A90);
    }

    private void drawAttrRow(GuiGraphics g, int top, int idx, Component name, String value) {
        int colX = cx() + 6 + (idx % 2 == 0 ? 0 : 112);
        int row = idx / 2;
        g.drawString(this.font, name, colX, top + row * ROW_H, 0xFFAAB6D8);
        g.drawString(this.font, value, colX + 66, top + row * ROW_H, 0xFFFFFFFF);
    }

    /** 区块标题（跨两列居中） */
    private void drawSectionTitle(GuiGraphics g, int top, int idx, Component name) {
        int row = idx / 2;
        g.drawString(this.font, name, cx() + 6, top + row * ROW_H, 0xFF9FE4FF);
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

    private int maxAttrScroll() {
        int scrollH = 0;
        int baseRows = (ATTR_KEYS.length + 1) / 2;
        scrollH += 14 + baseRows * ROW_H + 4 + 4;
        List<String[]> hiddenRows = collectHiddenRows();
        if (!hiddenRows.isEmpty()) {
            scrollH += 14 + (1 + (hiddenRows.size() + 1) / 2) * ROW_H + 4 + 4;
        }
        List<PlayerData.HexEntry> stkHexes = stackableHexes();
        List<PlayerData.EquipEntry> stkEqs = stackableEquips();
        if (!stkHexes.isEmpty() || !stkEqs.isEmpty()) {
            int hexRows = stkHexes.isEmpty() ? 0 : 1 + (stkHexes.size() + 1) / 2;
            int eqRows = stkEqs.isEmpty() ? 0 : 1 + (stkEqs.size() + 1) / 2;
            scrollH += 14 + (hexRows + eqRows) * ROW_H + 4;
        }
        int viewH = H - 34 - 46 - 4;
        return Math.max(0, scrollH - viewH);
    }

    private void renderHexes(GuiGraphics g, int y) {
        g.drawString(this.font, Component.translatable("hexcraft.gui.owned"), cx() + 4, y + 68, 0xFF9FE4FF);
        int top = y + 78;
        int bottom = y + 256;
        java.util.List<PlayerData.HexEntry> list = visibleHexes();
        int ly = top - hexScroll;
        for (var h : list) {
            if (HextechRegistry.get(h.id) == null) { ly += HEX_ROW_H; continue; } // 旧档残留（已删除海克斯）不显示
            if (ly + 8 < top) { ly += HEX_ROW_H; continue; }
            if (ly > bottom) break;
            String rk = HextechRegistry.get(h.id).rarity.key;
            Component name = Component.translatable("hexcraft.hextech." + h.id + ".name")
                    .copy().append("  Lv." + h.level);
            if (h.stacks > 0) name = name.copy().append("  ×" + h.stacks + "层");
            g.drawString(this.font, name, cx() + 6, ly, rarityColor(rk));
            // 来源角标：联动 mod 内容（原版不标）——TACZ 青色 / 未来联动金色，一眼区分内容来源
            String src = HextechRegistry.sourceOf(h.id);
            if (!"vanilla".equals(src)) {
                g.drawString(this.font, src.toUpperCase(java.util.Locale.ROOT), cx() + 186, ly,
                        "tacz".equals(src) ? 0xFF4DE6FF : 0xFFFFD700);
            }
            ly += HEX_ROW_H;
        }
        if (list.isEmpty()) {
            g.drawString(this.font, Component.translatable("hexcraft.gui.cat_empty"), cx() + 6, top + 4, 0xFF5A6A90);
        }
        if (maxHexScroll() > 0) {
            g.drawString(this.font, Component.translatable("hexcraft.gui.scroll_hint"), cx() + 150, bottom - 10, 0xFF5A6A90);
        }
    }

    /** 当前分类过滤后的海克斯列表（全部时直接返回原列表） */
    private java.util.List<PlayerData.HexEntry> visibleHexes() {
        if (hexCat == 0) return ClientDataStore.hexes;
        java.util.List<PlayerData.HexEntry> out = new java.util.ArrayList<>();
        for (var h : ClientDataStore.hexes) {
            com.hexcraft.hextech.HextechDefinition def = HextechRegistry.get(h.id);
            if (def == null) continue;
            if (hexCategoryOf(h.id, def) == hexCat) out.add(h);
        }
        return out;
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
                case "xp_bonus", "xp_drop", "jump", "mobi_power", "stack_growth" -> 3;
                // 经济
                case "coin_bonus", "bounty", "gold_gamble", "gold_punch", "loot_bonus", "loot_double" -> 4;
                // 趣味
                case "firework", "dice_forge", "potion_random", "random_loot" -> 8;
                default -> -1;
            };
            if (c > 0) return c;
            // 叠层成长属性特判：stack_attr:atk→战斗 / stack_attr:hp→生存
            if ("stack_attr".equals(ek)) {
                int ci2 = tok.indexOf(':', ci + 1);
                String attr = ci2 > 0 ? tok.substring(ci + 1, ci2) : "";
                if ("hp".equals(attr)) return 2;
                if ("atk".equals(attr)) return 1;
            }
        }
        return 1; // 兜底战斗
    }

    private int maxHexScroll() {
        return Math.max(0, visibleHexes().size() * HEX_ROW_H - (256 - 78));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tab == 2) {
            forgeScroll = Math.max(0, Math.min(maxForgeScroll(), forgeScroll - (int) Math.round(delta * 12)));
            return true;
        }
        if (tab == 1) {
            hexScroll = Math.max(0, Math.min(maxHexScroll(), hexScroll - (int) Math.round(delta * HEX_ROW_H)));
            return true;
        }
        if (tab == 0) {
            attrScroll = Math.max(0, Math.min(maxAttrScroll(), attrScroll - (int) Math.round(delta * ROW_H)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void renderForge(GuiGraphics g, int y) {
        String forgeText = ClientDataStore.maxForges <= 0
                ? Component.translatable("hexcraft.gui.forge_done", ClientDataStore.forgesUsed).getString()
                : Component.translatable("hexcraft.gui.forge_count", ClientDataStore.forgesUsed, ClientDataStore.maxForges).getString();
        g.drawString(this.font, forgeText, cx() + 4, y + 36, 0xFF9FE4FF);
        g.drawString(this.font, Component.translatable("hexcraft.gui.forged"), cx() + 4, y + 81, 0xFF9FE4FF);
        // 已锻造词条按来源档位分组展示：白银 / 黄金 / 棱彩，从低到高配色；组标题独占一行，词条两列；TACZ 联动词条行尾青色角标
        java.util.List<Object[]> rows = forgeRows();
        if (rows.isEmpty()) {
            g.drawString(this.font, "--", cx() + 6, y + 91, 0xFF3A4668);
            return;
        }
        int top = y + 88;
        int bottom = y + 234;
        int rowH = 12;
        int ly = top - forgeScroll;
        for (int i = 0; i < rows.size(); i++) {
            if (ly + 8 < top) { ly += rowH; continue; }
            if (ly > bottom) break;
            Object[] r = rows.get(i);
            int type = (Integer) r[0];
            // 标题行独占一行居左；词条行按全局行号奇偶分左右两列
            int colX = cx() + 4;
            if (type == 1 && (i % 2 == 1)) colX = cx() + 118;
            g.drawString(this.font, (String) r[1], colX, ly, (Integer) r[2]);
            // 来源角标：TACZ 联动锻造词条行尾青色「TACZ」（原版词条不标）
            if (type == 1 && r.length > 3 && Boolean.TRUE.equals(r[3])) {
                g.drawString(this.font, "TACZ", colX + 100, ly, 0xFF4DE6FF);
            }
            ly += rowH;
        }
        if (maxForgeScroll() > 0) {
            g.drawString(this.font, Component.translatable("hexcraft.gui.scroll_hint"), cx() + 150, bottom - 10, 0xFF5A6A90);
        }
    }

    private int maxForgeScroll() {
        return Math.max(0, forgeRowCount() * 12 - (234 - 88));
    }

    private int forgeRowCount() {
        java.util.List<Object[]> rows = forgeRows();
        return rows.size();
    }

    private java.util.List<Object[]> forgeRows() {
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        int[] tierColors = {0xFFD8D8D8, 0xFFFFC94A, 0xFFC77DFF};
        String[] tierKeys = {"basic", "gold", "prismatic"};
        for (int t = 0; t < 3; t++) {
            Map<String, Integer> agg = new LinkedHashMap<>();
            for (PlayerData.ForgeEntry f : ClientDataStore.forges)
                if (f.tier == t) agg.merge(f.stat, 1, Integer::sum);
            if (agg.isEmpty()) continue;
            rows.add(new Object[]{0, Component.translatable("hexcraft.forge.tier." + tierKeys[t]).getString(), tierColors[t]});
            for (Map.Entry<String, Integer> e : agg.entrySet()) {
                String name = Component.translatable("hexcraft.forge." + e.getKey() + ".name").getString();
                boolean tacz = com.hexcraft.hextech.HextechRegistry.FORGE_TACZ_MECHANICS.contains(e.getKey());
                rows.add(new Object[]{1, name + "  ×" + e.getValue(), tierColors[t], tacz});
            }
        }
        return rows;
    }

    private void renderEquip(GuiGraphics g, int y) {
        g.drawString(this.font, Component.translatable("hexcraft.gui.equipped"), cx() + 4, y + 70, 0xFF9FE4FF);
        int ly = y + 91;
        for (int i = 0; i < PlayerData.MAX_EQUIP; i++) {
            g.fill(cx() + 3, ly - 1, cx() + 228, ly + 11, 0x22305080);
            if (i < ClientDataStore.equipment.size()) {
                PlayerData.EquipEntry eq = ClientDataStore.equipment.get(i);
                com.hexcraft.hextech.HextechDefinition def = HextechRegistry.getEquip(eq.id);
                String rk = def != null ? def.rarity.key : "gold";
                String icon = def != null && def.icon != null ? def.icon : "◆";
                String nm = Component.translatable("hexcraft.equip." + eq.id + ".name").getString();
                g.drawString(this.font, icon + " " + nm, cx() + 6, ly, rarityColor(rk));
                // 来源角标：TACZ 联动装备行尾青色「TACZ」（原版不标）
                String src = HextechRegistry.sourceOf(eq.id);
                if (!"vanilla".equals(src)) {
                    g.drawString(this.font, src.toUpperCase(java.util.Locale.ROOT), cx() + 182, ly, 0xFF4DE6FF);
                }
            } else {
                g.drawString(this.font, "--", cx() + 6, ly, 0xFF3A4668);
            }
            ly += 16;
        }
        g.drawString(this.font, Component.translatable("hexcraft.gui.equip_hint"), cx() + 4, y + 200, 0xFF5A6A90);
    }

    // ================= 工具 =================

    private int cx() { return (this.width - W) / 2 + NAV_W + 12; }
    private int cy() { return (this.height - H) / 2; }

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
            boolean hot = this.isHoveredOrFocused();
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
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                    this.getMessage(), getX() + width / 2, getY() + (height - 8) / 2, col);
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
            boolean hot = this.isHoveredOrFocused();
            int border = hot ? 0xFFFFFFFF : accent;
            if (hot) { // 悬停柔光外圈
                g.fill(getX() - 2, getY() - 2, getX() + width + 2, getY() + height + 2, 0x3330A8E0);
            }
            g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, border);
            g.fillGradient(getX(), getY(), getX() + width, getY() + height,
                    hot ? 0x5530A8E0 : 0x331E6FE8, 0xCC10182E);
            int col = hot ? 0xFFFFFFFF : 0xFFC8D8F0;
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                    this.getMessage(), getX() + width / 2, getY() + (height - 8) / 2, col);
        }
    }
}
