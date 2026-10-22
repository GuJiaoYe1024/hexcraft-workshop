package com.hexcraft.client.gui;

import com.hexcraft.client.ClientDataStore;
import com.hexcraft.hextech.WorldHexDefinition;
import com.hexcraft.hextech.WorldHexRegistry;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 世界海克斯界面（背包第二页签 / K 键进入）：
 *  - 左侧按钮栏：第一位"生物"（当前页），"更多"点击打开敬请期待页（v0.4.2）；
 *  - 顶部抽取按钮（消耗经验，三选一）；
 *  - 下方四大类（通用/友好/中立/敌对）按稀有度排序展示已激活的世界海克斯，每条右侧带取消按钮；
 *  - 激活列表全服共享，服务端广播即时更新。
 *  v0.4.2：布局自适应压缩 + 高度不足时滚轮滚动（界面尺寸 3/4 也能完整查看）；文字与按钮分列不遮挡。
 */
public class WorldHexScreen extends Screen {
    private static final Button.CreateNarration NARR = s -> s.get();

    /** 科技风按钮：深蓝紫渐变底 + 霓虹顶边 + 悬停亮框（与海克斯工坊主界面风格统一） */
    static class WBtn extends Button {
        WBtn(int x, int y, int w, int h, Component label, OnPress onPress) {
            super(x, y, w, h, label, onPress, NARR);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hover = this.isHoveredOrFocused();
            int border = hover ? 0xFF8BD4FF : 0xFF1E6FE8;
            int text = hover ? 0xFFFFFFFF : 0xFF9FB6FF;
            g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, border);
            g.fillGradient(getX(), getY(), getX() + width, getY() + height, 0xFF1E3A66, 0xFF0B1530);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF4AC7FF);
            g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, text);
        }
    }

    private static final int SIDEBAR_X = 34;
    private static final int CONTENT_X = 168;
    private static final int COL_W = 300;
    private static final int COL_GAP = 22;
    private static final int ITEM_W = (COL_W - 14) / 2;   // 143
    private static final int ITEM_GAP = 14;
    private static final int BTN_W = 56;
    private static final int TEXT_W = ITEM_W - BTN_W - 6;
    /** 每类最多 4 行（上限 8 = 4 行 × 2 列） */
    private static final int MAX_ROWS = 4;

    /** 自适应行高/区块高度：屏幕矮时压缩（界面尺寸 3+ 也能容纳），高分辨率时舒展 */
    private int rowH() { return this.height < 460 ? 26 : 32; }
    private int panelH() { return 24 + MAX_ROWS * rowH() + 6; }          // 条目区 + 底边
    private int blockStep() { return 20 + panelH() + 14; }               // 标题区 + 面板 + 间距（两排步长）
    private int startY() { return this.height < 460 ? 62 : 78; }

    /** 内容底部（用于滚动范围计算） */
    private int contentBottom() { return startY() + blockStep() + panelH(); }

    private int scrollY;

    private Button drawBtn;
    private final List<Button> cancelBtns = new ArrayList<>();
    private int lastSize = -1;

    public WorldHexScreen() {
        super(Component.translatable("hexcraft.gui.world_hex_title"));
    }

    /** 重建全部按钮（init 与滚动时共用） */
    private void buildWidgets() {
        this.clearWidgets();
        this.cancelBtns.clear();
        int centerX = this.width / 2;
        int top = this.startY() - this.scrollY;

        // 顶部抽取按钮（内容区居中，科技风；v0.4.3：随滚动偏移，不再固定遮挡下方内容）
        drawBtn = new WBtn(centerX - 160, 34 - this.scrollY, 320, 22,
                Component.translatable("hexcraft.gui.world_draw", ClientDataStore.worldDrawXp),
                b -> PacketHandler.CHANNEL.sendToServer(new Packets.C2SRequestWorldDraw()));
        this.addRenderableWidget(drawBtn);

        // 左侧按钮栏：第一位"生物"（当前页，高亮）
        this.addRenderableWidget(new WBtn(SIDEBAR_X, top + 8, 96, 24,
                Component.translatable("hexcraft.worldhex.cat_btn.bio"), b -> {}));
        // "更多"：点击打开敬请期待页（v0.4.2）
        this.addRenderableWidget(new WBtn(SIDEBAR_X, top + 38, 96, 24,
                Component.translatable("hexcraft.gui.world_more"),
                b -> Minecraft.getInstance().setScreen(new FutureScreen())));

        // 四大类区块：每类内部双列网格，每条右侧带取消按钮
        int catIndex = 0;
        for (String cat : WorldHexRegistry.CATEGORIES) {
            int col = catIndex % 2;
            int rowBlock = catIndex / 2;
            int blockX = CONTENT_X + col * (COL_W + COL_GAP);
            int blockY = top + rowBlock * blockStep();
            List<WorldHexDefinition> list = WorldHexRegistry.activeInCategory(ClientDataStore.worldHexes, cat);
            int row = 0;
            for (WorldHexDefinition d : list) {
                int subCol = row % 2;
                int subRow = row / 2;
                int x = blockX + subCol * (ITEM_W + ITEM_GAP);
                int y = blockY + 24 + subRow * rowH();
                String id = d.id;
                cancelBtns.add(new WBtn(x + ITEM_W - BTN_W, y + 4, BTN_W, 20,
                        Component.translatable("hexcraft.gui.world_cancel", ClientDataStore.worldCancelCoin),
                        b -> {
                            Packets.C2SCancelWorldHex p = new Packets.C2SCancelWorldHex();
                            p.id = id;
                            PacketHandler.CHANNEL.sendToServer(p);
                        }));
                row++;
            }
            catIndex++;
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
        int maxScroll = Math.max(0, contentBottom() - this.height + 20);
        if (maxScroll > 0) {
            int ns = (int) (this.scrollY - delta * 24);
            ns = Math.max(0, Math.min(ns, maxScroll));
            if (ns != this.scrollY) {
                this.scrollY = ns;
                buildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // 服务端广播后列表变化 → 重建按钮（即时反映全局生效）
        if (ClientDataStore.worldHexes.size() != lastSize) {
            lastSize = ClientDataStore.worldHexes.size();
            this.scrollY = 0;
            buildWidgets();
        }
        this.renderBackground(g);
        int centerX = this.width / 2;
        int top = this.startY() - this.scrollY;

        // 标题
        Component title = Component.translatable("hexcraft.gui.world_hex_title");
        g.drawCenteredString(this.font, title, centerX, 12, 0xFFFFFF);
        g.fillGradient(centerX - 120, 24, centerX + 120, 25, 0xFFFFC94A, 0xFFC77DFF);

        // 左侧栏装饰
        g.fill(SIDEBAR_X - 8, top + 2, SIDEBAR_X + 104, top + 70, 0xAA0B1530);
        g.fill(SIDEBAR_X - 8, top + 2, SIDEBAR_X - 7, top + 70, 0xFF1E6FE8);

        // 各分类区块：背景固定高度（标题 + 4 行条目区），空分类同样完整显示
        int catIndex = 0;
        for (String cat : WorldHexRegistry.CATEGORIES) {
            int col = catIndex % 2;
            int rowBlock = catIndex / 2;
            int blockX = CONTENT_X + col * (COL_W + COL_GAP);
            int blockY = top + rowBlock * blockStep();
            // 背景盒：标题区（-20..0）+ 条目区（0..panelH）
            g.fill(blockX - 10, blockY - 20, blockX + COL_W + 4, blockY + panelH(), 0x550B1530);
            g.fill(blockX - 10, blockY - 20, blockX - 9, blockY + panelH(), 0xFF2A5DB0);
            Component catTitle = Component.translatable("hexcraft.worldhex.category." + cat);
            g.drawString(this.font, catTitle, blockX - 2, blockY - 15, 0xFF9FB6FF);
            // 激活计数（x/上限）
            List<WorldHexDefinition> list = WorldHexRegistry.activeInCategory(ClientDataStore.worldHexes, cat);
            int max = ClientDataStore.worldMaxPerCategory;
            g.drawString(this.font, Component.literal(list.size() + "/" + (max > 0 ? max : "∞")),
                    blockX + COL_W - 70, blockY - 15, 0xFF6A7B96);
            int row = 0;
            for (WorldHexDefinition d : list) {
                int subCol = row % 2;
                int subRow = row / 2;
                int x = blockX + subCol * (ITEM_W + ITEM_GAP);
                int y = blockY + 24 + subRow * rowH();
                int color = rarityColor(d.rarity);
                Component name = Component.translatable("hexcraft.worldhex." + d.id + ".name");
                g.drawString(this.font, name, x - 2, y, color);
                Component desc = Component.translatable("hexcraft.worldhex." + d.id + ".desc");
                List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(desc, TEXT_W);
                if (!lines.isEmpty()) {
                    g.drawString(this.font, lines.get(0), x - 2, y + 10, 0xFFA9B4CC);
                }
                row++;
            }
            if (list.isEmpty()) {
                g.drawString(this.font, Component.translatable("hexcraft.gui.world_empty"), blockX - 2, blockY + 24, 0xFF6A7B96);
            }
            catIndex++;
        }
        super.render(g, mx, my, pt);
    }

    private static int rarityColor(String key) {
        return switch (key) {
            case "gold" -> 0xFFFFC94A;
            case "prismatic" -> 0xFFC77DFF;
            default -> 0xFFD0D8E8;
        };
    }
}
