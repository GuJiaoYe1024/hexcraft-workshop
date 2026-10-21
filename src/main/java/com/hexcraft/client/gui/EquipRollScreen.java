package com.hexcraft.client.gui;

import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 装备三选一抽取界面（斗魂竞技场/海克斯大乱斗风格）：图标 + 机制 + 属性卡片，入场动画同海克斯抽取 */
public class EquipRollScreen extends Screen {
    private static final int CARD_W = 180;
    private static final int CARD_H = 158;

    private final Packets.S2COpenRoll msg;
    private final int[] cardX = new int[3];
    private final RollScreen.CardButton[] cards = new RollScreen.CardButton[3];
    private final List<RollScreen.AccentButton> btns = new ArrayList<>(2);
    private final List<float[]> parts = new ArrayList<>();
    private int cardY;
    private boolean closed;
    private float anim;
    private int pTick;
    private boolean partsInit;
    private static final Random P_RAND = new Random();

    public EquipRollScreen(Packets.S2COpenRoll msg) {
        super(Component.translatable("hexcraft.gui.equip_title"));
        this.msg = msg;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        this.cardY = this.height / 2 - CARD_H / 2 - 10;
        for (int i = 0; i < 3; i++) {
            cardX[i] = centerX - CARD_W * 3 / 2 - 26 + i * (CARD_W + 26);
            final int idx = i;
            int color = rarityColor(msg.rarities.get(idx));
            String label = Component.translatable("hexcraft.rarity." + msg.rarities.get(idx)).getString();
            cards[idx] = new RollScreen.CardButton(cardX[i], cardY, CARD_W, CARD_H, color, label, b -> choose(idx));
            this.addRenderableWidget(cards[idx]);
        }
        int btnY = cardY + CARD_H + 18;
        if (msg.canReroll) {
            btns.add(new RollScreen.AccentButton(centerX - 112, btnY, 106, 22,
                    Component.translatable("hexcraft.gui.reroll", msg.rerollCost), 0xFF6FB1FF, b -> reroll()));
            btns.add(new RollScreen.AccentButton(centerX + 6, btnY, 106, 22,
                    Component.translatable("gui.cancel"), 0xFF8A93A8, b -> onClose()));
        } else {
            btns.add(new RollScreen.AccentButton(centerX - 53, btnY, 106, 22,
                    Component.translatable("gui.cancel"), 0xFF8A93A8, b -> onClose()));
        }
        for (RollScreen.AccentButton b : btns) this.addRenderableWidget(b);
    }

    @Override
    public void tick() {
        if (anim < 1f) anim = Math.min(1f, anim + 0.11f);
        pTick++;
        ensureParts();
        for (float[] p : parts) {
            p[1] -= p[2];
            if (p[1] < -6f) { p[1] = this.height + 6f; p[0] = P_RAND.nextFloat() * this.width; }
        }
    }

    private void ensureParts() {
        if (partsInit) return;
        partsInit = true;
        for (int i = 0; i < 24; i++) {
            parts.add(new float[]{P_RAND.nextFloat() * this.width, P_RAND.nextFloat() * this.height,
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
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        int centerX = this.width / 2;

        ensureParts();
        for (float[] p : parts) {
            int a = (int) ((0.14f + 0.10f * (float) Math.sin(pTick * 0.08f + p[3])) * 255);
            if (a <= 8) continue;
            int sz = (int) p[4];
            g.fill((int) p[0] - 1, (int) p[1] - 1, (int) p[0] + sz + 1, (int) p[1] + sz + 1,
                    (a / 3) << 24 | ((int) p[5] & 0x00FFFFFF));
            g.fill((int) p[0], (int) p[1], (int) p[0] + sz, (int) p[1] + sz, a << 24 | ((int) p[5] & 0x00FFFFFF));
        }

        g.fillGradient(0, cardY - 30, this.width, cardY + CARD_H + 6, 0x2A123056, 0x00000000);
        g.fillGradient(0, cardY + CARD_H - 20, this.width, cardY + CARD_H + 30, 0x00000000, 0x2A123056);

        Component title = Component.translatable("hexcraft.gui.equip_title");
        g.drawCenteredString(this.font, title, centerX, cardY - 34, 0xFFFFFF);
        int barW = this.font.width(title) + 40;
        g.fillGradient(centerX - barW / 2, cardY - 20, centerX + barW / 2, cardY - 19, 0xFFC77DFF, 0xFF7A6BFF);

        for (int i = 0; i < 3; i++) {
            float p = Math.max(0f, Math.min(1f, (anim - i * 0.13f) / 0.74f));
            float e = p * p;
            cards[i].alpha = e;
            cards[i].scale = 0.90f + 0.10f * e;
            cards[i].dy = (int) ((1f - p) * 44f);
            cards[i].glowPulse = 0.6f + 0.4f * (float) Math.sin(pTick * 0.15f + i * 2.1f);
        }
        float bp = Math.max(0f, Math.min(1f, anim / 0.30f));
        for (RollScreen.AccentButton b : btns) {
            b.alpha = bp;
            b.dy = (int) ((1f - bp) * 26f);
        }

        super.render(g, mx, my, pt);

        for (int i = 0; i < 3; i++) {
            String id = msg.options.get(i);
            Component name = Component.translatable("hexcraft.equip." + id + ".name");
            Component desc = Component.translatable("hexcraft.equip." + id + ".desc");
            int color = rarityColor(msg.rarities.get(i));
            int cx = cardX[i] + CARD_W / 2;
            int a = (int) (cards[i].alpha * 255);
            int cy = cardY + cards[i].dy;

            // 大图标（装备图标字符，稀有度色辉光）
            String icon = i < msg.icons.size() ? msg.icons.get(i) : "◆";
            int iconY = cy + 26;
            g.drawCenteredString(this.font, icon, cx, iconY,
                    (a << 24) | (color & 0x00FFFFFF));
            g.drawCenteredString(this.font, icon, cx, iconY,
                    (a * 130 / 255) << 24 | (color & 0x00FFFFFF)); // 双层加粗感

            g.drawCenteredString(this.font, name, cx, cy + 52, (a << 24) | (color & 0x00FFFFFF));
            g.fill(cardX[i] + 14, cy + 64, cardX[i] + CARD_W - 14, cy + 65, 0x33505F88);
            List<FormattedCharSequence> lines = this.font.split(desc, CARD_W - 26);
            int dy = cy + 70;
            for (FormattedCharSequence line : lines) {
                g.drawCenteredString(this.font, line, cx, dy, (a << 24) | 0x00C8D0E8);
                dy += 11;
                if (dy > cy + CARD_H - 8) break;
            }
            // 卖出价标签
            g.drawCenteredString(this.font, Component.translatable("hexcraft.gui.equip_sell_tag"),
                    cx, cy + CARD_H - 12, (a << 24) | 0x00FFD28C);
        }
    }

    private static int rarityColor(String key) {
        return switch (key) {
            case "prismatic" -> 0xFFC77DFF;
            default -> 0xFFFFC94A;
        };
    }
}
