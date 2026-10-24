package com.hexcraft.client.gui;

import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 装备三选一抽取界面（斗魂竞技场/海克斯大乱斗风格）：图标 + 机制 + 属性卡片，入场动画同海克斯抽取
 *  v0.4.4：接入 UI 缩放（配置「界面缩放」）。 */
public class EquipRollScreen extends HexScaledScreen {
    private static final int CARD_W = 180;
    private static final int CARD_H = 158;

    private final Packets.S2COpenRoll msg;
    private final int[] cardX = new int[3];
    private final RollScreen.CardButton[] cards = new RollScreen.CardButton[3];
    private final List<RollScreen.AccentButton> btns = new ArrayList<>(2);
    private final List<float[]> parts = new ArrayList<>();
    private int cardY;
    private boolean closed;
    /** v0.6.3 三选一「选中→确认」：CONFIRM_SELECT 开启时点卡片仅选中，点确认按钮才生效（防手滑） */
    private int selectedIdx = -1;
    /** v0.6.3 界面停留：确认抽取后不关闭，展示结果直到玩家点「完成」 */
    private String chosenId;
    private Component chosenName;
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
        int centerX = this.guiW() / 2;
        this.cardY = this.guiH() / 2 - CARD_H / 2 - 10;
        for (int i = 0; i < 3; i++) {
            cardX[i] = centerX - CARD_W * 3 / 2 - 26 + i * (CARD_W + 26);
            final int idx = i;
            int color = rarityColor(msg.rarities.get(idx));
            String label = Component.translatable("hexcraft.rarity." + msg.rarities.get(idx)).getString();
            cards[idx] = new RollScreen.CardButton(cardX[i], cardY, CARD_W, CARD_H, color, label, b -> onCardClick(idx));
            this.addRenderableWidget(cards[idx]);
        }
        int btnY = cardY + CARD_H + 18;
        if (msg.canReroll) {
            btns.add(new RollScreen.AccentButton(centerX - 112, btnY, 106, 22,
                    Component.translatable("hexcraft.gui.reroll", msg.rerollCost), 0xFF6FB1FF, b -> reroll()));
            btns.add(new RollScreen.AccentButton(centerX + 6, btnY, 106, 22,
                    Component.translatable("gui.cancel"), 0xFF8A93A8, b -> cancelRoll()));
        } else {
            btns.add(new RollScreen.AccentButton(centerX - 53, btnY, 106, 22,
                    Component.translatable("gui.cancel"), 0xFF8A93A8, b -> cancelRoll()));
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

    /** v0.6.3 卡片点击：CONFIRM_SELECT 开启=仅选中待确认；关闭=直接选择生效 */
    private void onCardClick(int index) {
        if (closed) return;
        if (com.hexcraft.config.HexConfig.CONFIRM_SELECT.get()) select(index);
        else choose(index);
    }

    private void select(int index) {
        selectedIdx = index;
        for (int i = 0; i < 3; i++) cards[i].selected = (i == index);
        rebuildConfirmButtons();
    }

    /** 选中态底部按钮：确认选择 + 取消（替换刷新/取消） */
    private void rebuildConfirmButtons() {
        int centerX = this.guiW() / 2;
        int btnY = cardY + CARD_H + 18;
        for (RollScreen.AccentButton b : btns) this.removeWidget(b);
        btns.clear();
        btns.add(new RollScreen.AccentButton(centerX - 112, btnY, 106, 22,
                Component.translatable("hexcraft.gui.confirm_choose"), 0xFF6FB1FF, b -> choose(selectedIdx)));
        btns.add(new RollScreen.AccentButton(centerX + 6, btnY, 106, 22,
                Component.translatable("gui.cancel"), 0xFF8A93A8, b -> cancelRoll()));
        for (RollScreen.AccentButton b : btns) this.addRenderableWidget(b);
    }

    private void choose(int index) {
        if (closed) return;
        closed = true;
        chosenId = msg.options.get(index);
        chosenName = Component.translatable("hexcraft.equip." + chosenId + ".name");
        Packets.C2SChooseRoll p = new Packets.C2SChooseRoll();
        p.index = index;
        PacketHandler.CHANNEL.sendToServer(p);
        // v0.6.3 确认选择后立即离开（不再停留展示结果 + 完成按钮；返回/关闭按 KEEP_WORKSHOP_OPEN 配置）
        done();
    }

    /** v0.6.3 取消本轮三选一：通知服务端清理待选状态（防「请完成当前三选一」死锁），
     *  并按配置返回装备抽取入口页（KEEP_WORKSHOP_OPEN=true）或直接关闭界面 */
    private void cancelRoll() {
        if (closed) return;
        closed = true;
        PacketHandler.CHANNEL.sendToServer(new Packets.C2SCancelRoll());
        if (com.hexcraft.config.HexConfig.KEEP_WORKSHOP_OPEN.get()) {
            HextechPageScreen fresh = new HextechPageScreen();
            fresh.setInitialTab(3);
            net.minecraft.client.Minecraft.getInstance().setScreen(fresh);
        } else {
            net.minecraft.client.Minecraft.getInstance().setScreen(null);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && !closed) { // Esc = 取消本轮（与服务端取消包同步，防死锁）
            cancelRoll();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** v0.6.3 完成：KEEP_WORKSHOP_OPEN=true 返回工坊装备页继续抽取；false 直接关闭 */
    private void done() {
        if (!com.hexcraft.config.HexConfig.KEEP_WORKSHOP_OPEN.get()) { net.minecraft.client.Minecraft.getInstance().setScreen(null); return; }
        HextechPageScreen fresh = new HextechPageScreen();
        fresh.setInitialTab(3);
        net.minecraft.client.Minecraft.getInstance().setScreen(fresh);
    }

    private void reroll() {
        if (closed) return;
        PacketHandler.CHANNEL.sendToServer(new Packets.C2SReroll());
    }

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        int centerX = this.guiW() / 2;

        ensureParts();
        for (float[] p : parts) {
            int a = (int) ((0.14f + 0.10f * (float) Math.sin(pTick * 0.08f + p[3])) * 255);
            if (a <= 8) continue;
            int sz = (int) p[4];
            g.fill((int) p[0] - 1, (int) p[1] - 1, (int) p[0] + sz + 1, (int) p[1] + sz + 1,
                    (a / 3) << 24 | ((int) p[5] & 0x00FFFFFF));
            g.fill((int) p[0], (int) p[1], (int) p[0] + sz, (int) p[1] + sz, a << 24 | ((int) p[5] & 0x00FFFFFF));
        }

        g.fillGradient(0, cardY - 30, this.guiW(), cardY + CARD_H + 6, 0x2A123056, 0x00000000);
        g.fillGradient(0, cardY + CARD_H - 20, this.guiW(), cardY + CARD_H + 30, 0x00000000, 0x2A123056);

        Component title;
        if (chosenId != null) {
            title = Component.translatable("hexcraft.gui.got", chosenName);
        } else {
            title = Component.translatable("hexcraft.gui.equip_title");
        }
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

        this.renderWidgets(g, mx, my, pt);

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
            // v0.6.5 已拥有角标：重复装备折算卖出价/叠层，提前提示避免困惑
            com.hexcraft.capability.PlayerData.EquipEntry owned = ownedEquip(id);
            if (owned != null) {
                g.drawCenteredString(this.font,
                        Component.translatable("hexcraft.gui.owned_tag", owned.level),
                        cx, cy + 14, (a << 24) | 0x00FFD28C);
            }
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
        // v0.6.3 选中提示（确认模式）
        if (selectedIdx >= 0 && com.hexcraft.config.HexConfig.CONFIRM_SELECT.get()) {
            g.drawCenteredString(this.font, Component.translatable("hexcraft.gui.selected_hint"),
                    cardX[selectedIdx] + CARD_W / 2, cardY + cards[selectedIdx].dy + CARD_H + 5, 0xFFFFFFFF);
        }
    }

    private static int rarityColor(String key) {
        return switch (key) {
            case "prismatic" -> 0xFFC77DFF;
            default -> 0xFFFFC94A;
        };
    }

    /** v0.6.5 已拥有装备查询（客户端缓存，仅显示用） */
    private static com.hexcraft.capability.PlayerData.EquipEntry ownedEquip(String id) {
        for (com.hexcraft.capability.PlayerData.EquipEntry e : com.hexcraft.client.ClientDataStore.equipment) {
            if (e.id.equals(id)) return e;
        }
        return null;
    }
}
