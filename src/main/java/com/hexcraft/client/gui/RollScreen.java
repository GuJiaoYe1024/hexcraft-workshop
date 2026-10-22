package com.hexcraft.client.gui;

import com.hexcraft.config.HexConfig;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 三选一抽取界面（海克斯大乱斗风格）：稀有度辉光边框 + 缎带 + 图标 + 粒子 + 入场/缩放动画 + 保底进度
 *  v0.4.4：接入 UI 缩放（配置「界面缩放」）。 */
public class RollScreen extends HexScaledScreen {
    private static final int CARD_W = 170;
    private static final int CARD_H = 142;

    private final Packets.S2COpenRoll msg;
    private final int[] cardX = new int[3];
    private final CardButton[] cards = new CardButton[3];
    private final List<AccentButton> btns = new ArrayList<>(2);
    private final List<float[]> parts = new ArrayList<>();
    private int cardY;
    private boolean closed;
    private float anim;
    private int pTick;
    private boolean partsInit;
    private static final Random P_RAND = new Random();

    public RollScreen(Packets.S2COpenRoll msg) {
        super(Component.translatable(msg.forge ? "hexcraft.gui.forge_title" : "hexcraft.gui.hex_title"));
        this.msg = msg;
    }

    @Override
    protected void init() {
        int centerX = this.guiW() / 2;
        this.cardY = this.guiH() / 2 - CARD_H / 2 - 10;
        for (int i = 0; i < 3; i++) {
            cardX[i] = centerX - CARD_W * 3 / 2 - 26 + i * (CARD_W + 26);
            final int idx = i;
            int color = msg.forge ? forgeTierColor(msg.tier) : rarityColor(msg.rarities.get(idx));
            String label = msg.forge
                    ? Component.translatable("hexcraft.forge.tier." + (msg.tier == 0 ? "basic" : msg.tier == 1 ? "gold" : "prismatic")).getString()
                    : Component.translatable("hexcraft.rarity." + msg.rarities.get(idx)).getString();
            cards[idx] = new CardButton(cardX[i], cardY, CARD_W, CARD_H, color, label, b -> choose(idx));
            this.addRenderableWidget(cards[idx]);
        }
        int btnY = cardY + CARD_H + 18;
        if (msg.canReroll) {
            btns.add(new AccentButton(centerX - 112, btnY, 106, 22,
                    Component.translatable("hexcraft.gui.reroll", msg.rerollCost), 0xFF6FB1FF, b -> reroll()));
            btns.add(new AccentButton(centerX + 6, btnY, 106, 22,
                    Component.translatable("gui.cancel"), 0xFF8A93A8, b -> onClose()));
        } else {
            btns.add(new AccentButton(centerX - 53, btnY, 106, 22,
                    Component.translatable("gui.cancel"), 0xFF8A93A8, b -> onClose()));
        }
        for (AccentButton b : btns) this.addRenderableWidget(b);
    }

    @Override
    public void tick() {
        if (anim < 1f) anim = Math.min(1f, anim + 0.13f); // 高帧率入场：更快更顺滑
        pTick++;
        ensureParts();
        for (float[] p : parts) {
            p[1] -= p[2];
            if (p[1] < -6f) { p[1] = this.guiH() + 6f; p[0] = P_RAND.nextFloat() * this.guiW(); }
        }
    }

    private void ensureParts() {
        if (partsInit) return;
        partsInit = true;
        for (int i = 0; i < 24; i++) {
            parts.add(new float[]{P_RAND.nextFloat() * this.guiW(), P_RAND.nextFloat() * this.guiH(),
                    0.20f + P_RAND.nextFloat() * 0.30f, P_RAND.nextFloat() * 6.28f,
                    1f + P_RAND.nextFloat() * 1.5f, P_RAND.nextBoolean() ? 0xFF9FE4FF : 0xFFC77DFF});
        }
    }

    private void choose(int index) {
        if (closed) return;
        closed = true;
        Packets.C2SChooseRoll p = new Packets.C2SChooseRoll();
        p.index = index;
        PacketHandler.CHANNEL.sendToServer(p);
        this.onClose();
    }

    private void reroll() {
        if (closed) return;
        PacketHandler.CHANNEL.sendToServer(new Packets.C2SReroll());
    }

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        int centerX = this.guiW() / 2;

        // 背景粒子
        ensureParts();
        for (float[] p : parts) {
            int a = (int) ((0.14f + 0.10f * (float) Math.sin(pTick * 0.08f + p[3])) * 255);
            if (a <= 8) continue;
            int sz = (int) p[4];
            g.fill((int) p[0] - 1, (int) p[1] - 1, (int) p[0] + sz + 1, (int) p[1] + sz + 1,
                    (a / 3) << 24 | ((int) p[5] & 0x00FFFFFF));
            g.fill((int) p[0], (int) p[1], (int) p[0] + sz, (int) p[1] + sz, a << 24 | ((int) p[5] & 0x00FFFFFF));
        }

        // 海克斯辉光背景：卡片区两侧光晕
        g.fillGradient(0, cardY - 30, this.guiW(), cardY + CARD_H + 6, 0x2A123056, 0x00000000);
        g.fillGradient(0, cardY + CARD_H - 20, this.guiW(), cardY + CARD_H + 30, 0x00000000, 0x2A123056);

        // 标题与装饰条
        String tierKey = msg.forge
                ? "hexcraft.forge.tier." + (msg.tier == 0 ? "basic" : msg.tier == 1 ? "gold" : "prismatic")
                : null;
        Component title = msg.forge
                ? Component.translatable("hexcraft.gui.forge_title", Component.translatable(tierKey))
                : Component.translatable("hexcraft.gui.hex_title");
        g.drawCenteredString(this.font, title, centerX, cardY - 34, 0xFFFFFF);
        int barW = this.font.width(title) + 40;
        g.fillGradient(centerX - barW / 2, cardY - 20, centerX + barW / 2, cardY - 19, 0xFF6FB1FF, 0xFF1E6FE8);

        // 入场动画：错峰浮入 + 淡入 + 缩放（悬停卡片轻微放大）
        for (int i = 0; i < 3; i++) {
            float p = Math.max(0f, Math.min(1f, (anim - i * 0.13f) / 0.74f));
            float e = p * p;
            cards[i].alpha = e;
            cards[i].scale = 0.90f + 0.10f * e + (isHover(mx, my, cards[i]) ? 0.04f : 0f);
            cards[i].dy = (int) ((1f - p) * 44f);
            cards[i].glowPulse = 0.6f + 0.4f * (float) Math.sin(pTick * 0.15f + i * 2.1f);
        }
        // 底部按钮延迟出现
        float bp = Math.max(0f, Math.min(1f, anim / 0.30f));
        for (AccentButton b : btns) {
            b.alpha = bp;
            b.dy = (int) ((1f - bp) * 26f);
        }

        this.renderWidgets(g, mx, my, pt);

        for (int i = 0; i < 3; i++) {
            String id = msg.options.get(i);
            Component name = Component.translatable(msg.forge
                    ? "hexcraft.forge." + id + ".name" : "hexcraft.hextech." + id + ".name");
            Component desc = Component.translatable(msg.forge
                    ? "hexcraft.forge." + id + ".desc" : "hexcraft.hextech." + id + ".desc");
            int color = rarityColor(msg.rarities.get(i));
            int cx = cardX[i] + CARD_W / 2;
            int a = (int) (cards[i].alpha * 255);
            int cy = cardY + cards[i].dy;

            g.drawCenteredString(this.font, name, cx, cy + 42, (a << 24) | (color & 0x00FFFFFF));
            g.drawCenteredString(this.font, name, cx + 1, cy + 43, (a / 3) << 24 | (color & 0x00FFFFFF)); // 名称柔光
            g.fill(cardX[i] + 14, cy + 56, cardX[i] + CARD_W - 14, cy + 57, 0x33505F88);
            List<FormattedCharSequence> lines = this.font.split(desc, CARD_W - 24);
            int dy = cy + 62;
            for (FormattedCharSequence line : lines) {
                g.drawCenteredString(this.font, line, cx, dy, (a << 24) | 0x00C8D0E8);
                dy += 11;
                if (dy > cy + CARD_H - 6) break;
            }
        }

        // 保底进度
        int pG = HexConfig.PITY_GOLD.get();
        int pP = HexConfig.PITY_PRISMATIC.get();
        if (!msg.forge && (pG > 0 || pP > 0)) {
            Component line = Component.empty();
            if (pG > 0) {
                line = line.copy().append(Component.translatable("hexcraft.gui.pity_gold",
                        Math.max(0, pG - msg.pityGold)));
            }
            if (pP > 0) {
                if (!line.getString().isEmpty()) line = line.copy().append("   ");
                line = line.copy().append(Component.translatable("hexcraft.gui.pity_prismatic",
                        Math.max(0, pP - msg.pityPrismatic)));
            }
            g.drawCenteredString(this.font, line, centerX, cardY + CARD_H + 5, 0xFFC8D0E8);
        }
    }

    private static int rarityColor(String key) {
        return switch (key) {
            case "gold" -> 0xFFFFC94A;
            case "prismatic" -> 0xFFC77DFF;
            case "forge" -> 0xFF6FB1FF;
            default -> 0xFFD0D8E8;
        };
    }

    private static int forgeTierColor(int tier) {
        return switch (tier) {
            case 0 -> 0xFFD0D8E8;
            case 1 -> 0xFFFFC94A;
            default -> 0xFFC77DFF;
        };
    }

    /** 抽取卡片：辉光边框 + 渐变底 + 稀有度缎带（入场淡入/浮入/缩放 + 辉光脉动） */
    static class CardButton extends Button {
        private final int color;
        private final String label;
        float alpha = 1f;
        int dy;
        float scale = 1f;
        float glowPulse = 1f;

        CardButton(int x, int y, int w, int h, int color, String label, OnPress onPress) {
            super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
            this.color = color;
            this.label = label;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int yy = getY() + dy;
            int a = (int) (alpha * 255);
            int border = isHover(mx, my, this) ? 0xFFFFFFFF : color;
            int cx = getX() + width / 2;
            int cy = yy + height / 2;
            int w2 = (int) (width * scale);
            int h2 = (int) (height * scale);
            int x1 = cx - w2 / 2, y1 = cy - h2 / 2;
            // 外辉光（稀有度色，随入场与脉动呼吸）
            int glow = (int) (a * glowPulse);
            g.fill(x1 - 3, y1 - 3, x1 + w2 + 3, y1 + h2 + 3, glow << 24 | (color & 0x00FFFFFF));
            g.fill(x1 - 1, y1 - 1, x1 + w2 + 1, y1 + h2 + 1, (a << 24) | (border & 0x00FFFFFF));
            // 渐变底
            g.fillGradient(x1, y1, x1 + w2, y1 + h2,
                    (a * 240 / 255) << 24 | 0x002C2E58, (a * 244 / 255) << 24 | 0x000D1220);
            // 稀有度缎带
            g.fill(x1, y1, x1 + w2, y1 + 16, (a << 24) | (color & 0x00FFFFFF));
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                    label == null ? "" : label, cx, y1 + 4, (a << 24) | 0x00101018);
            // 图标（菱形晶核）
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, "◆", cx, y1 + 22,
                    (a << 24) | (color & 0x00FFFFFF));
        }
    }

    /** 强调按钮（海克斯蓝描边；支持入场淡入/浮入） */
    static class AccentButton extends Button {
        private final int border;
        float alpha = 1f;
        int dy;

        AccentButton(int x, int y, int w, int h, Component label, int border, OnPress onPress) {
            super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
            this.border = border;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int a = (int) (alpha * 255);
            int yy = getY() + dy;
            int col = isHover(mx, my, this) ? 0xFFFFFFFF : border;
            g.fill(getX() - 1, yy - 1, getX() + width + 1, yy + height + 1, a << 24 | (col & 0x00FFFFFF));
            g.fill(getX(), yy, getX() + width, yy + height, a * 240 / 255 << 24 | 0x00151B33);
            int textCol = isHover(mx, my, this) ? 0xFFFFFFFF : 0xFFC8D0E8;
            g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, this.getMessage(),
                    getX() + width / 2, yy + (height - 8) / 2, a << 24 | (textCol & 0x00FFFFFF));
        }
    }
}
