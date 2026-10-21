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

/** 世界海克斯界面（背包第二页签进入）：
 *  - 左侧按钮栏：第一位"生物"（后续分类预留）；
 *  - 顶部抽取按钮（消耗经验，三选一）；
 *  - 下方四大类（通用/友好/中立/敌对）按稀有度排序展示已激活的世界海克斯，每条带取消按钮（消耗晶币）；
 *  - 激活列表全服共享，服务端广播即时更新。
 */
public class WorldHexScreen extends Screen {
    private static final Button.CreateNarration NARR = s -> s.get();

    /** 内部按钮子类：外部类无法直接 new Button（其构造器 protected），子类可调 super */
    static class WBtn extends Button {
        WBtn(int x, int y, int w, int h, Component label, OnPress onPress) {
            super(x, y, w, h, label, onPress, NARR);
        }
    }
    private static final int SIDEBAR_X = 34;
    private static final int CONTENT_X = 168;
    private static final int COL_W = 300;
    private static final int COL_GAP = 22;
    private static final int BLOCK_H = 190;

    private Button drawBtn;
    private final List<Button> cancelBtns = new ArrayList<>();
    private int lastSize = -1;

    public WorldHexScreen() {
        super(Component.translatable("hexcraft.gui.world_hex_title"));
    }

    @Override
    protected void init() {
        this.cancelBtns.clear();
        int centerX = this.width / 2;

        // 顶部抽取按钮（内容区居中）
        drawBtn = new WBtn(centerX - 160, 34, 320, 22,
                Component.translatable("hexcraft.gui.world_draw", ClientDataStore.worldDrawXp),
                b -> PacketHandler.CHANNEL.sendToServer(new Packets.C2SRequestWorldDraw()));
        this.addRenderableWidget(drawBtn);

        // 左侧按钮栏：第一位"生物"（当前页，高亮）
        Button bio = new WBtn(SIDEBAR_X, 70, 96, 24,
                Component.translatable("hexcraft.worldhex.cat_btn.bio"), b -> {});
        this.addRenderableWidget(bio);
        // 预留位（不可用）
        Button more = new WBtn(SIDEBAR_X, 100, 96, 24,
                Component.translatable("hexcraft.gui.world_more"), b -> {});
        this.addRenderableWidget(more);

        // 四大类区块与每行的取消按钮
        int catIndex = 0;
        for (String cat : WorldHexRegistry.CATEGORIES) {
            int col = catIndex % 2;
            int rowBlock = catIndex / 2;
            int blockX = CONTENT_X + col * (COL_W + COL_GAP);
            int blockY = 78 + rowBlock * BLOCK_H;
            List<WorldHexDefinition> list = WorldHexRegistry.activeInCategory(ClientDataStore.worldHexes, cat);
            int row = 0;
            for (WorldHexDefinition d : list) {
                int y = blockY + 26 + row * 34;
                String id = d.id;
                cancelBtns.add(new WBtn(blockX + COL_W - 78, y + 1, 74, 18,
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
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // 服务端广播后列表变化 → 重建按钮（即时反映全局生效）
        if (ClientDataStore.worldHexes.size() != lastSize) {
            lastSize = ClientDataStore.worldHexes.size();
            this.rebuildWidgets();
        }
        this.renderBackground(g);
        int centerX = this.width / 2;

        // 标题
        Component title = Component.translatable("hexcraft.gui.world_hex_title");
        g.drawCenteredString(this.font, title, centerX, 12, 0xFFFFFF);
        g.fillGradient(centerX - 120, 24, centerX + 120, 25, 0xFFFFC94A, 0xFFC77DFF);

        // 左侧栏装饰
        g.fill(SIDEBAR_X - 8, 62, SIDEBAR_X + 104, 130, 0xAA0B1530);
        g.fill(SIDEBAR_X - 8, 62, SIDEBAR_X - 7, 130, 0xFF1E6FE8);

        // 各分类区块
        int catIndex = 0;
        for (String cat : WorldHexRegistry.CATEGORIES) {
            int col = catIndex % 2;
            int rowBlock = catIndex / 2;
            int blockX = CONTENT_X + col * (COL_W + COL_GAP);
            int blockY = 78 + rowBlock * BLOCK_H;
            g.fill(blockX - 10, blockY - 20, blockX + COL_W + 4, blockY + 120, 0x550B1530);
            g.fill(blockX - 10, blockY - 20, blockX - 9, blockY + 120, 0xFF2A5DB0);
            Component catTitle = Component.translatable("hexcraft.worldhex.category." + cat);
            g.drawString(this.font, catTitle, blockX - 2, blockY - 15, 0xFF9FB6FF);
            // 激活计数（x/上限）
            List<WorldHexDefinition> list = WorldHexRegistry.activeInCategory(ClientDataStore.worldHexes, cat);
            int max = ClientDataStore.worldMaxPerCategory;
            g.drawString(this.font, Component.literal(list.size() + "/" + (max > 0 ? max : "∞")),
                    blockX + COL_W - 70, blockY - 15, 0xFF6A7B96);
            int row = 0;
            for (WorldHexDefinition d : list) {
                int y = blockY + 26 + row * 34;
                int color = rarityColor(d.rarity);
                Component name = Component.translatable("hexcraft.worldhex." + d.id + ".name");
                g.drawString(this.font, name, blockX - 2, y, color);
                Component desc = Component.translatable("hexcraft.worldhex." + d.id + ".desc");
                List<net.minecraft.util.FormattedCharSequence> lines = this.font.split(desc, COL_W - 96);
                if (!lines.isEmpty()) {
                    g.drawString(this.font, lines.get(0), blockX - 2, y + 10, 0xFFA9B4CC);
                }
                row++;
            }
            if (list.isEmpty()) {
                g.drawString(this.font, Component.translatable("hexcraft.gui.world_empty"), blockX - 2, blockY + 26, 0xFF6A7B96);
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
