package com.hexcraft.client.gui;

import com.hexcraft.client.ClientDataStore;
import com.hexcraft.hextech.WorldHexDefinition;
import com.hexcraft.hextech.WorldHexRegistry;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 世界海克斯界面（背包第二页签 / K 键进入）：
 *  - 顶部抽取按钮（消耗经验，三选一）；
 *  - 左侧按钮栏：第一位"生物"（当前页），"更多"点击打开敬请期待页（v0.4.2）；
 *  - 下方四大类（通用/友好/中立/敌对）2×2 等分网格（v0.4.5 重新排版：区块同宽同高、条目行高等分铺满，
 *    任意 GUI Scale 下不再超屏缺角；条目只显示名字，鼠标悬停才显示介绍）。
 *  v0.4.5 设计口径：白银=数值类、黄金=数值+机制、棱彩=百分比数值+机制（悬停提示中标注）。
 */
public class WorldHexScreen extends HexScaledScreen {
    private static final Button.CreateNarration NARR = s -> s.get();

    /** 科技风按钮：深蓝紫渐变底 + 霓虹顶边 + 悬停亮框（与海克斯工坊主界面风格统一） */
    static class WBtn extends Button {
        /** v0.8.5 当前页高亮：金色边框 + 白字 + 金色顶边（用于左侧「生物」当前页标识） */
        private final boolean highlight;

        WBtn(int x, int y, int w, int h, Component label, OnPress onPress) {
            this(x, y, w, h, label, onPress, false);
        }

        WBtn(int x, int y, int w, int h, Component label, OnPress onPress, boolean highlight) {
            super(x, y, w, h, label, onPress, NARR);
            this.highlight = highlight;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hover = isHover(mx, my, this);
            int border = hover ? 0xFF8BD4FF : (highlight ? 0xFFFFC94A : 0xFF1E6FE8);
            int text = hover ? 0xFFFFFFFF : (highlight ? 0xFFFFE08A : 0xFF9FB6FF);
            g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, border);
            g.fillGradient(getX(), getY(), getX() + width, getY() + height, 0xFF1E3A66, 0xFF0B1530);
            g.fill(getX(), getY(), getX() + width, getY() + 1, highlight ? 0xFFFFC94A : 0xFF4AC7FF);
            // 当前页左侧竖亮条
            if (highlight) g.fill(getX(), getY() + 3, getX() + 2, getY() + height - 3, 0xFFFFC94A);
            g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, text);
        }
    }

    /** 左侧按钮栏 */
    private static final int SIDEBAR_X = 34;
    private static final int SIDEBAR_W = 96;
    /** 内容区起点（左侧栏右侧留 6px 间隙） */
    private static final int CONTENT_X = 140;
    /** 内容区右侧留白 */
    private static final int CONTENT_R_MARGIN = 20;
    /** 2×2 网格间距 */
    private static final int GRID_GAP = 14;
    /** 区块标题条高（分类名 + x/上限） */
    private static final int TITLE_H = 18;
    /** 区块上下内边距（标题条下 + 条目区下） */
    private static final int BLOCK_PAD = 12;
    /** 取消按钮宽 */
    private static final int BTN_W = 56;
    /** 条目内名字区最小宽度（名字 + 间隙） */
    private static final int NAME_MIN_W = 46;

    private int rowH() { return this.guiH() < 300 ? 12 : this.guiH() < 460 ? 18 : 22; }
    /** 区块高：按可用内容区高度等分（两排），保证 2×2 网格上下对称 */
    private int blockH(int contentH) { return Math.max(12 + 4 * rowH() + TITLE_H, (contentH - GRID_GAP) / 2); }
    /** 内容区宽（右侧留白） */
    private int contentW() { return Math.max(200, this.guiW() - CONTENT_X - CONTENT_R_MARGIN); }
    /** 区块宽：两列等分 */
    private int blockW() { return (contentW() - GRID_GAP) / 2; }
    /** 条目宽：区块内两列等分 */
    private int itemW() { return (blockW() - 8 - 8) / 2; }
    /** 名字区宽（条目宽 - 取消按钮 - 间隙） */
    private int nameW() { return Math.max(NAME_MIN_W, itemW() - BTN_W - 6); }
    /** 区块内条目区起点（条目在区块内垂直居中，保证整齐） */
    private int entryTop(int blockY, int contentH) {
        int inner = blockH(contentH) - TITLE_H - BLOCK_PAD - 4 * rowH();
        return blockY + TITLE_H + 4 + Math.max(0, inner / 2);
    }

    /** 内容顶部（抽取按钮下方） */
    private int contentTop() { return this.guiH() < 300 ? 60 : 72; }
    /** 内容底部（界面底部留白） */
    private int contentBottom() { return this.guiH() - 26; }
    /** 内容区高 */
    private int contentH() { return Math.max(40, contentBottom() - contentTop()); }
    /** 全部内容高度（含网格间距，用于极矮屏滚动） */
    private int totalContentH() { return contentTop() + 2 * blockH(contentH()) + GRID_GAP; }

    private int scrollY;
    private Button drawBtn;
    private final List<Button> cancelBtns = new ArrayList<>();
    private int lastCostVer = -1;

    public WorldHexScreen() {
        super(Component.translatable("hexcraft.gui.world_hex_title"));
    }

    /** 重建全部按钮（init 与滚动时共用） */
    private void buildWidgets() {
        this.clearWidgets();
        this.cancelBtns.clear();
        int centerX = this.guiW() / 2;
        int top = this.contentTop() - this.scrollY;

        // 顶部抽取按钮（内容区居中，科技风；v0.4.3：随滚动偏移，不再固定遮挡下方内容）
        drawBtn = new WBtn(centerX - 160, 34 - this.scrollY, 320, 22,
                Component.translatable("hexcraft.gui.world_draw", ClientDataStore.worldDrawXp),
                b -> {
                    // v0.6.16 防连点：抽取请求间隔内禁用按钮（服务端三选一期间重复点击会刷请求）
                    b.active = false;
                    PacketHandler.CHANNEL.sendToServer(new Packets.C2SRequestWorldDraw());
                    java.util.Timer t = new java.util.Timer();
                    t.schedule(new java.util.TimerTask() { public void run() { b.active = true; } }, 800);
                });
        this.addRenderableWidget(drawBtn);

        // 左侧按钮栏：第一位"生物"（当前页，高亮）
        this.addRenderableWidget(new WBtn(SIDEBAR_X, top + 8, SIDEBAR_W, 24,
                Component.translatable("hexcraft.worldhex.cat_btn.bio"), b -> {}, true));
        // "联动总览"（v0.4.2 敬请期待页 → v0.8.4 实装为联动统计页）
        this.addRenderableWidget(new WBtn(SIDEBAR_X, top + 38, SIDEBAR_W, 24,
                Component.translatable("hexcraft.gui.world_more"),
                b -> Minecraft.getInstance().setScreen(new FutureScreen())));

        // 四大类区块：2×2 等分网格，每条目右侧带取消按钮
        int blockX0 = CONTENT_X;
        int blockY0 = top;
        for (int i = 0; i < 4; i++) {
            String cat = WorldHexRegistry.CATEGORIES[i];
            int bx = blockX0 + (i % 2) * (blockW() + GRID_GAP);
            int by = blockY0 + (i / 2) * (blockH(contentH()) + GRID_GAP);
            int eTop = entryTop(by, contentH());
            List<WorldHexDefinition> list = WorldHexRegistry.activeInCategory(ClientDataStore.worldHexes, cat);
            int row = 0;
            for (WorldHexDefinition d : list) {
                int x = bx + 8 + (row % 2) * (itemW() + 8);
                int y = eTop + (row / 2) * rowH();
                String id = d.id;
                int bw = Math.min(BTN_W, itemW() - NAME_MIN_W - 4);
                cancelBtns.add(new WBtn(x + itemW() - bw, y, bw, Math.min(20, rowH()),
                        Component.translatable("hexcraft.gui.world_cancel", ClientDataStore.worldCancelCoin),
                        b -> {
                            Packets.C2SCancelWorldHex p = new Packets.C2SCancelWorldHex();
                            p.id = id;
                            PacketHandler.CHANNEL.sendToServer(p);
                        }));
                row++;
            }
        }
        for (Button b : cancelBtns) this.addRenderableWidget(b);
    }

    @Override
    protected void init() {
        this.scrollY = 0;
        buildWidgets();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        double s = uiScale();
        int maxScroll = Math.max(0, totalContentH() - this.guiH() + 20);
        if (maxScroll > 0) {
            int ns = (int) (this.scrollY - delta * 24);
            ns = Math.max(0, Math.min(ns, maxScroll));
            if (ns != this.scrollY) {
                this.scrollY = ns;
                buildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mx / s, my / s, delta);
    }

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        // v0.6.16 数据变化（列表/费用/上限任意一项）→ 重建按钮（即时反映，含费用实时刷新）
        if (ClientDataStore.costVersion != lastCostVer) {
            lastCostVer = ClientDataStore.costVersion;
            this.scrollY = 0;
            buildWidgets();
        }
        int centerX = this.guiW() / 2;
        int top = this.contentTop() - this.scrollY;

        // 标题
        Component title = Component.translatable("hexcraft.gui.world_hex_title");
        g.drawCenteredString(this.font, title, centerX, 12, 0xFFFFFF);
        g.fillGradient(centerX - 120, 24, centerX + 120, 25, 0xFFFFC94A, 0xFFC77DFF);

        // v0.6.16 顶部状态行：右上角显示已激活总数/每类上限（抽取/取消后即时刷新）
        int actTotal = ClientDataStore.worldHexes.size();
        int wMax = ClientDataStore.worldMaxPerCategory;
        Component actText = Component.translatable("hexcraft.gui.world_active_count",
                actTotal, wMax > 0 ? wMax : "\u221E");
        g.drawString(this.font, actText, this.guiW() - 30 - this.font.width(actText), 16, 0xFF9FB6FF);

        // 左侧栏装饰
        g.fill(SIDEBAR_X - 8, top + 2, SIDEBAR_X + SIDEBAR_W + 8, top + 70, 0xAA0B1530);
        g.fill(SIDEBAR_X - 8, top + 2, SIDEBAR_X - 7, top + 70, 0xFF1E6FE8);

        // 2×2 等分网格：四大类区块（同宽同高，对称工整）
        int blockY0 = top;
        for (int i = 0; i < 4; i++) {
            String cat = WorldHexRegistry.CATEGORIES[i];
            int bx = CONTENT_X + (i % 2) * (blockW() + GRID_GAP);
            int by = blockY0 + (i / 2) * (blockH(contentH()) + GRID_GAP);
            int eTop = entryTop(by, contentH());
            List<WorldHexDefinition> list = WorldHexRegistry.activeInCategory(ClientDataStore.worldHexes, cat);
            int max = ClientDataStore.worldMaxPerCategory;

            // 区块背景（统一尺寸，空分类同样完整显示）
            g.fill(bx, by, bx + blockW(), by + blockH(contentH()), 0x550B1530);
            g.fill(bx, by, bx + 1, by + blockH(contentH()), 0xFF2A5DB0);
            g.fill(bx, by + blockH(contentH()) - 1, bx + blockW(), by + blockH(contentH()), 0x882A5DB0);
            // 标题条：分类名（左）+ 激活计数（右）
            Component catTitle = Component.translatable("hexcraft.worldhex.category." + cat);
            g.drawString(this.font, catTitle, bx + 6, by + 4, 0xFF9FB6FF);
            g.drawString(this.font, Component.literal(list.size() + "/" + (max > 0 ? max : "∞")),
                    bx + blockW() - 8 - this.font.width(Component.literal("00/00")), by + 4, 0xFF6A7B96);

            // 条目：只显示名字（稀有度色），悬停才显示介绍（v0.4.5）
            int row = 0;
            for (WorldHexDefinition d : list) {
                int x = bx + 8 + (row % 2) * (itemW() + 8);
                int y = eTop + (row / 2) * rowH();
                int color = rarityColor(d.rarity);
                Component name = Component.translatable("hexcraft.worldhex." + d.id + ".name");
                String nstr = name.getString();
                if (this.font.width(nstr) > nameW()) {
                    nstr = this.font.plainSubstrByWidth(nstr, nameW()) + "…";
                }
                g.drawString(this.font, nstr, x, y + 3, color);
                // hover 提示：名字 + 稀有度类型 + 描述
                if (mx >= x - 2 && mx <= x + itemW() && my >= y - 1 && my <= y + rowH() - 1) {
                    List<Component> tip = new ArrayList<>();
                    tip.add(name.copy().withStyle(c -> c.withColor(color)));
                    tip.add(Component.translatable("hexcraft.worldhex.tierstyle." + d.rarity)
                            .withStyle(c -> c.withColor(0xFF6A7B96)));
                    tip.add(Component.translatable("hexcraft.worldhex." + d.id + ".desc"));
                    g.renderComponentTooltip(this.font, tip, mx, my);
                }
                row++;
            }
            if (list.isEmpty()) {
                g.drawString(this.font, Component.translatable("hexcraft.gui.world_empty"),
                        bx + 8, eTop + 4, 0xFF6A7B96);
            }
        }
        this.renderWidgets(g, mx, my, pt);
    }

    private static int rarityColor(String key) {
        return switch (key) {
            case "gold" -> 0xFFFFC94A;
            case "prismatic" -> 0xFFC77DFF;
            default -> 0xFFD0D8E8;
        };
    }
}
