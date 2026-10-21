package com.hexcraft.client.gui;

import com.hexcraft.hextech.WorldHexDefinition;
import com.hexcraft.hextech.WorldHexRegistry;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** 世界海克斯三选一界面（服务端下发三个候选，点击确认后全服即时生效） */
public class WorldRollScreen extends Screen {
    private static final int CARD_W = 170;
    private static final int CARD_H = 142;
    private static final Button.CreateNarration NARR = s -> s.get();

    /** 科技风按钮：深蓝紫渐变底 + 霓虹顶边 + 悬停亮框（与主界面风格统一，替代原版灰色按钮） */
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

    private final Packets.S2CWorldDrawOptions msg;
    private final int[] cardX = new int[3];
    private final CardButton[] cards = new CardButton[3];
    private int cardY;
    private boolean closed;

    public WorldRollScreen(Packets.S2CWorldDrawOptions msg) {
        super(Component.translatable("hexcraft.gui.world_hex_title"));
        this.msg = msg;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.cardY = this.height / 2 - CARD_H / 2 - 10;
        for (int i = 0; i < 3 && i < msg.options.size(); i++) {
            cardX[i] = centerX - CARD_W * 3 / 2 - 26 + i * (CARD_W + 26);
            final int idx = i;
            WorldHexDefinition def = WorldHexRegistry.get(msg.options.get(idx));
            String label = def == null ? "" : Component.translatable("hexcraft.worldhex.category." + def.category).getString();
            int color = def == null ? 0xFFD0D8E8 : rarityColor(def.rarity);
            cards[idx] = new CardButton(cardX[i], cardY, CARD_W, CARD_H, color, label, b -> choose(idx));
            this.addRenderableWidget(cards[idx]);
        }
        this.addRenderableWidget(new WBtn(centerX - 53, cardY + CARD_H + 18, 106, 22,
                Component.translatable("gui.cancel"), b -> onClose()));
    }

    private void choose(int index) {
        if (closed) return;
        closed = true;
        Packets.C2SConfirmWorldHex p = new Packets.C2SConfirmWorldHex();
        p.index = index;
        PacketHandler.CHANNEL.sendToServer(p);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int centerX = this.width / 2;

        // 标题与装饰条
        Component title = Component.translatable("hexcraft.gui.world_hex_title");
        g.drawCenteredString(this.font, title, centerX, cardY - 34, 0xFFFFFF);
        int barW = this.font.width(title) + 40;
        g.fillGradient(centerX - barW / 2, cardY - 20, centerX + barW / 2, cardY - 19, 0xFFFFC94A, 0xFFC77DFF);

        super.render(g, mx, my, pt);

        for (int i = 0; i < 3 && i < msg.options.size(); i++) {
            WorldHexDefinition def = WorldHexRegistry.get(msg.options.get(i));
            if (def == null) continue;
            Component name = Component.translatable("hexcraft.worldhex." + def.id + ".name");
            Component desc = Component.translatable("hexcraft.worldhex." + def.id + ".desc");
            int color = rarityColor(def.rarity);
            int cx = cardX[i] + CARD_W / 2;
            int cy = cardY;

            g.drawCenteredString(this.font, name, cx, cy + 42, color);
            g.drawCenteredString(this.font, name, cx + 1, cy + 43, color);
            g.fill(cardX[i] + 14, cy + 56, cardX[i] + CARD_W - 14, cy + 57, 0x33505F88);
            List<FormattedCharSequence> lines = this.font.split(desc, CARD_W - 24);
            int dy = cy + 62;
            for (FormattedCharSequence line : lines) {
                g.drawCenteredString(this.font, line, cx, dy, 0xFFC8D0E8);
                dy += 11;
                if (dy > cy + CARD_H - 6) break;
            }
            g.drawCenteredString(this.font,
                    Component.translatable("hexcraft.worldhex.cost_xp", msg.drawXp), cx, cy + CARD_H - 4, 0xFF9FB6FF);
        }
    }

    private static int rarityColor(String key) {
        return switch (key) {
            case "gold" -> 0xFFFFC94A;
            case "prismatic" -> 0xFFC77DFF;
            default -> 0xFFD0D8E8;
        };
    }

    /** 抽取卡片：辉光边框 + 渐变底 + 分类缎带 */
    static class CardButton extends Button {
        private final int color;
        private final String label;

        CardButton(int x, int y, int w, int h, int color, String label, OnPress onPress) {
            super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
            this.color = color;
            this.label = label;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int border = this.isHoveredOrFocused() ? 0xFFFFFFFF : color;
            g.fill(getX() - 3, getY() - 3, getX() + width + 3, getY() + height + 3, 0x66 << 24 | (color & 0x00FFFFFF));
            g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, 0xFF << 24 | (border & 0x00FFFFFF));
            g.fillGradient(getX(), getY(), getX() + width, getY() + height, 0xF02C2E58, 0xF00D1220);
            g.fill(getX(), getY(), getX() + width, getY() + 16, 0xFF << 24 | (color & 0x00FFFFFF));
            g.drawCenteredString(Minecraft.getInstance().font, label, getX() + width / 2, getY() + 4, 0xFF101018);
            g.drawCenteredString(Minecraft.getInstance().font, "◆", getX() + width / 2, getY() + 22,
                    0xFF << 24 | (color & 0x00FFFFFF));
        }
    }
}
