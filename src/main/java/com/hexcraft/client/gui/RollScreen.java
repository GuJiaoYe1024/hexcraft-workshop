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
    /** v0.6.3 三选一「选中→确认」：CONFIRM_SELECT 开启时点卡片仅选中，点确认按钮才生效（防手滑） */
    private int selectedIdx = -1;
    /** v0.6.3 界面停留：确认抽取后不关闭，展示结果直到玩家点「完成」 */
    private String chosenId;
    private Component chosenName;
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
            cards[idx] = new CardButton(cardX[i], cardY, CARD_W, CARD_H, color, label, b -> onCardClick(idx));
            this.addRenderableWidget(cards[idx]);
        }
        int btnY = cardY + CARD_H + 18;
        if (msg.canReroll) {
            btns.add(new AccentButton(centerX - 112, btnY, 106, 22,
                    Component.translatable("hexcraft.gui.reroll", msg.rerollCost), 0xFF6FB1FF, b -> reroll()));
            btns.add(new AccentButton(centerX + 6, btnY, 106, 22,
                    Component.translatable("gui.cancel"), 0xFF8A93A8, b -> cancelRoll()));
        } else {
            btns.add(new AccentButton(centerX - 53, btnY, 106, 22,
                    Component.translatable("gui.cancel"), 0xFF8A93A8, b -> cancelRoll()));
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

    /** v0.6.3 卡片点击：CONFIRM_SELECT 开启=仅选中待确认；关闭=直接选择生效 */
    private void onCardClick(int index) {
        if (closed) return;
        if (HexConfig.CONFIRM_SELECT.get()) select(index);
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
        for (AccentButton b : btns) this.removeWidget(b);
        btns.clear();
        btns.add(new AccentButton(centerX - 112, btnY, 106, 22,
                Component.translatable("hexcraft.gui.confirm_choose"), 0xFF6FB1FF, b -> choose(selectedIdx)));
        btns.add(new AccentButton(centerX + 6, btnY, 106, 22,
                Component.translatable("gui.cancel"), 0xFF8A93A8, b -> cancelRoll()));
        for (AccentButton b : btns) this.addRenderableWidget(b);
    }

    private void choose(int index) {
        if (closed) return;
        closed = true;
        chosenId = msg.options.get(index);
        chosenName = Component.translatable(msg.forge
                ? "hexcraft.forge." + chosenId + ".name" : "hexcraft.hextech." + chosenId + ".name");
        Packets.C2SChooseRoll p = new Packets.C2SChooseRoll();
        p.index = index;
        PacketHandler.CHANNEL.sendToServer(p);
        // v0.6.3 确认选择后立即离开（不再停留展示结果 + 完成按钮；返回/关闭按 KEEP_WORKSHOP_OPEN 配置）
        done();
    }

    /** v0.6.3 取消本轮三选一：通知服务端清理待选状态（防「请完成当前三选一」死锁），
     *  并按配置返回抽取入口页（KEEP_WORKSHOP_OPEN=true）或直接关闭界面 */
    private void cancelRoll() {
        if (closed) return;
        closed = true;
        PacketHandler.CHANNEL.sendToServer(new Packets.C2SCancelRoll());
        if (HexConfig.KEEP_WORKSHOP_OPEN.get()) {
            HextechPageScreen fresh = new HextechPageScreen();
            fresh.setInitialTab(msg.forge ? 2 : 1);
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

    /** v0.6.3 完成：KEEP_WORKSHOP_OPEN=true 返回工坊对应页面继续抽取（锻造→锻造页/海克斯→海克斯页）；false 直接关闭 */
    private void done() {
        if (!HexConfig.KEEP_WORKSHOP_OPEN.get()) { net.minecraft.client.Minecraft.getInstance().setScreen(null); return; }
        HextechPageScreen fresh = new HextechPageScreen();
        fresh.setInitialTab(msg.forge ? 2 : 1);
        net.minecraft.client.Minecraft.getInstance().setScreen(fresh);
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

        // 标题与装饰条（v0.6.3 界面停留：确认后标题切换为「已获得：名称」）
        String tierKey = msg.forge
                ? "hexcraft.forge.tier." + (msg.tier == 0 ? "basic" : msg.tier == 1 ? "gold" : "prismatic")
                : null;
        Component title;
        if (chosenId != null) {
            title = Component.translatable("hexcraft.gui.got", chosenName);
        } else if (msg.forge) {
            title = Component.translatable("hexcraft.gui.forge_title", Component.translatable(tierKey));
        } else {
            title = Component.translatable("hexcraft.gui.hex_title");
        }
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
            Component desc;
            if (msg.forge) {
                // v0.6.2 锻造卡：lang 模板占位符 {tierName}/{displayValue} 运行时填充（与属性页 tooltip 同口径）
                String forgeTierKey = msg.tier == 0 ? "basic" : msg.tier == 1 ? "gold" : "prismatic";
                String tierName = Component.translatable("hexcraft.forge.tier." + forgeTierKey).getString();
                String real = forgeCardValueText(id, msg.tier);
                desc = Component.literal(Component.translatable("hexcraft.forge." + id + ".desc").getString()
                        .replace("{tierName}", tierName).replace("{displayValue}", real));
            } else {
                desc = Component.translatable("hexcraft.hextech." + id + ".desc");
            }
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
            // v0.6.2 海克斯卡叠层标签后缀（运行时拼接【叠层：xxx｜xxx｜xxx】）
            if (!msg.forge) {
                String tagSuffix = com.hexcraft.hextech.StackTagKeys.tooltipSuffix(
                        com.hexcraft.hextech.HextechRegistry.get(id),
                        net.minecraft.client.Minecraft.getInstance().options.languageCode.startsWith("zh"));
                if (tagSuffix != null) {
                    g.drawCenteredString(this.font, tagSuffix, cx, dy + 2, (a << 24) | 0x008FA6C8);
                }
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
        // v0.6.3 选中提示（确认模式）：选中卡底部显示提示条
        if (selectedIdx >= 0 && HexConfig.CONFIRM_SELECT.get()) {
            int cx = cardX[selectedIdx] + CARD_W / 2;
            g.drawCenteredString(this.font, Component.translatable("hexcraft.gui.selected_hint"),
                    cx, cardY + cards[selectedIdx].dy + CARD_H - 14, 0xFFFFFFFF);
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
        boolean selected;
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
            int border = selected ? 0xFFFFFFFF : (isHover(mx, my, this) ? 0xFFFFFFFF : color);
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
            // v0.6.3 选中态：底部白色选中条
            if (selected) {
                g.fill(x1 + 20, y1 + h2 - 4, x1 + w2 - 20, y1 + h2 - 2, 0xFFFFFFFF);
            }
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

    /** v0.6.2 锻造卡实际加成文本：基础值 × 档位倍率（与属性页 tooltip 同口径，百分比词条转百分号） */
    private static String forgeCardValueText(String stat, int tier) {
        com.hexcraft.hextech.HextechDefinition.ModSpec m = com.hexcraft.hextech.HextechRegistry.forgeMod(stat);
        if (m == null) return "?";
        double v = m.perLevel * com.hexcraft.hextech.HextechRegistry.tierMult(tier);
        if (stat.equals("hppct") || stat.equals("aspdpct")) {
            String p = String.format(java.util.Locale.ROOT, "%.2f", v * 100.0).replaceFirst("\\.?0+$", "");
            return "+" + p + "%";
        }
        String s = String.format(java.util.Locale.ROOT, "%.4f", v);
        s = s.replaceFirst("0+$", "");
        if (s.endsWith(".")) s += "0";
        return "+" + s;
    }
}
