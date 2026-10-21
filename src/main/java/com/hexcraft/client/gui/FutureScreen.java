package com.hexcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 敬请期待页（v0.4.2）：世界海克斯左侧栏预留按钮点击后打开，提示更多分类开发中 */
public class FutureScreen extends Screen {
    private static final Button.CreateNarration NARR = s -> s.get();

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

    public FutureScreen() {
        super(Component.translatable("hexcraft.gui.future_title"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        this.addRenderableWidget(new WBtn(cx - 53, this.height / 2 + 40, 106, 22,
                Component.translatable("gui.back"), b -> onClose()));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int cx = this.width / 2;
        Component title = Component.translatable("hexcraft.gui.future_title");
        g.drawCenteredString(this.font, title, cx, this.height / 2 - 46, 0xFFFFC94A);
        int barW = this.font.width(title) + 40;
        g.fillGradient(cx - barW / 2, this.height / 2 - 32, cx + barW / 2, this.height / 2 - 31, 0xFFFFC94A, 0xFFC77DFF);
        Component desc = Component.translatable("hexcraft.gui.future_desc");
        int y = this.height / 2 - 10;
        for (net.minecraft.util.FormattedCharSequence line : this.font.split(desc, Math.min(380, this.width - 60))) {
            g.drawCenteredString(this.font, line, cx, y, 0xFFA9B4CC);
            y += 12;
        }
        super.render(g, mx, my, pt);
    }
}
