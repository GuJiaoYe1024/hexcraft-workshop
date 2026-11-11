package com.hexcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** 敌方海克斯页（v0.9.1）：世界海克斯左侧栏「敌方海克斯」按钮打开，
 *  展示敌方海克斯机制说明、触发概率与海克斯池（狂怒/迅捷/铁壁/护盾/再生）。
 *  纯静态信息页（模仿 FutureScreen 布局：标题 + 描述区 + 列表区 + 右下角返回按钮）。 */
public class EnemyHexScreen extends HexScaledScreen {
    private static final Button.CreateNarration NARR = s -> s.get();

    static class WBtn extends Button {
        WBtn(int x, int y, int w, int h, Component label, OnPress onPress) {
            super(x, y, w, h, label, onPress, NARR);
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean hover = isHover(mx, my, this);
            int border = hover ? 0xFF8BD4FF : 0xFF1E6FE8;
            int text = hover ? 0xFFFFFFFF : 0xFF9FB6FF;
            g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, border);
            g.fillGradient(getX(), getY(), getX() + width, getY() + height, 0xFF1E3A66, 0xFF0B1530);
            g.fill(getX(), getY(), getX() + width, getY() + 1, 0xFF4AC7FF);
            HexFonts.centered(g, this.getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, text, false);
        }
    }

    public EnemyHexScreen() {
        super(Component.translatable("hexcraft.gui.enemy_hex_title"));
    }

    @Override
    protected void init() {
        this.addRenderableWidget(new WBtn(this.guiW() - 96, this.guiH() - 30, 80, 20,
                Component.translatable("gui.back"), b -> onClose()));
    }

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.guiW() / 2;
        Component title = Component.translatable("hexcraft.gui.enemy_hex_title");
        HexFonts.centered(g, title, cx, 12, 0xFFFFC94A, false);
        int barW = HexFonts.ui().width(title) + 40;
        g.fillGradient(cx - barW / 2, 24, cx + barW / 2, 25, 0xFFFFC94A, 0xFFC77DFF);

        int y = 44;
        // 机制说明（自动换行，最多 420px）
        Component desc = Component.translatable("hexcraft.gui.enemy_hex_desc");
        for (net.minecraft.util.FormattedCharSequence line : HexFonts.ui().split(desc, Math.min(420, this.guiW() - 60))) {
            HexFonts.centered(g, line, cx, y, 0xFFA9B4CC, false);
            y += 11;
        }
        y += 4;
        // 概率表一行
        HexFonts.centered(g, Component.translatable("hexcraft.gui.enemy_hex_chance"), cx, y, 0xFFD0D8E8, false);
        y += 14;

        // 池列表标题
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.enemy_hex_pool"),
                cx - 200, y, 0xFF9FB6FF, false);
        y += 12;
        String[] poolKeys = {"fury", "swift", "bulwark", "shield", "regen"};
        for (String k : poolKeys) {
            g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.enemy_hex." + k),
                    cx - 196, y, 0xFFD0D8E8, false);
            y += 12;
        }
        y += 4;
        // 底部提示
        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.enemy_hex_note"),
                cx - 200, y, 0xFF8FA6C8, false);

        this.renderWidgets(g, mx, my, pt);
    }
}