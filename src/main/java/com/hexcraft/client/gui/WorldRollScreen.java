package com.hexcraft.client.gui;

import com.hexcraft.hextech.WorldHexDefinition;
import com.hexcraft.hextech.WorldHexRegistry;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** 世界海克斯三选一界面（服务端下发三个候选，点击确认后全服即时生效）
 *  v0.4.4：接入 UI 缩放（配置「界面缩放」）。 */
public class WorldRollScreen extends HexScaledScreen {
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
            boolean hover = isHover(mx, my, this);
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
    /** v0.6.3 三选一「选中→确认」：CONFIRM_SELECT 开启时点卡片仅选中，点确认按钮才生效（防手滑） */
    private int selectedIdx = -1;
    /** v0.6.3 界面停留：确认抽取后不关闭，展示结果直到玩家点「完成」 */
    private String chosenId;
    private Component chosenName;
    private net.minecraft.client.gui.components.Button cancelBtn;
    private net.minecraft.client.gui.components.Button confirmBtn;

    /** v0.4.9 抽取动画：三张候选卡错峰上浮淡入 + 光柱扫过（总时长） */
    private static final int ANIM_MS = 1100;
    private long openAt = -1;

    public WorldRollScreen(Packets.S2CWorldDrawOptions msg) {
        super(Component.translatable("hexcraft.gui.world_hex_title"));
        this.msg = msg;
    }

    @Override
    protected void init() {
        if (openAt < 0) openAt = System.currentTimeMillis();
        int centerX = this.guiW() / 2;
        this.cardY = this.guiH() / 2 - CARD_H / 2 - 10;
        for (int i = 0; i < 3 && i < msg.options.size(); i++) {
            cardX[i] = centerX - CARD_W * 3 / 2 - 26 + i * (CARD_W + 26);
            final int idx = i;
            WorldHexDefinition def = WorldHexRegistry.get(msg.options.get(idx));
            String label = def == null ? "" : Component.translatable("hexcraft.worldhex.category." + def.category).getString();
            int color = def == null ? 0xFFD0D8E8 : rarityColor(def.rarity);
            cards[idx] = new CardButton(cardX[i], cardY, CARD_W, CARD_H, color, label, b -> onCardClick(idx));
            this.addRenderableWidget(cards[idx]);
        }
        cancelBtn = new WBtn(centerX - 53, cardY + CARD_H + 18, 106, 22,
                Component.translatable("gui.cancel"), b -> cancelRoll());
        this.addRenderableWidget(cancelBtn);
    }

    /** v0.6.3 卡片点击：CONFIRM_SELECT 开启=仅选中待确认；关闭=直接选择生效 */
    private void onCardClick(int index) {
        if (closed) return;
        if (com.hexcraft.config.HexConfig.CONFIRM_SELECT.get()) select(index);
        else choose(index);
    }

    private void select(int index) {
        selectedIdx = index;
        for (int i = 0; i < 3 && i < msg.options.size(); i++) cards[i].selected = (i == index);
        int centerX = this.guiW() / 2;
        this.removeWidget(cancelBtn);
        if (confirmBtn != null) this.removeWidget(confirmBtn);
        confirmBtn = new WBtn(centerX - 112, cardY + CARD_H + 18, 106, 22,
                Component.translatable("hexcraft.gui.confirm_choose"), b -> choose(selectedIdx));
        cancelBtn = new WBtn(centerX + 6, cardY + CARD_H + 18, 106, 22,
                Component.translatable("gui.cancel"), b -> cancelRoll());
        this.addRenderableWidget(confirmBtn);
        this.addRenderableWidget(cancelBtn);
    }

    private void choose(int index) {
        if (closed) return;
        closed = true;
        chosenId = msg.options.get(index);
        WorldHexDefinition def = WorldHexRegistry.get(chosenId);
        chosenName = def == null ? Component.literal(chosenId)
                : Component.translatable("hexcraft.worldhex." + chosenId + ".name");
        Packets.C2SConfirmWorldHex p = new Packets.C2SConfirmWorldHex();
        p.index = index;
        PacketHandler.CHANNEL.sendToServer(p);
        // v0.6.3 确认选择后立即离开（不再停留展示结果 + 完成按钮；返回/关闭按 KEEP_WORKSHOP_OPEN 配置）
        done();
    }

    /** v0.6.3 取消本轮世界海克斯三选一：通知服务端清理待选状态（防死锁），
     *  并按配置返回世界海克斯界面（KEEP_WORKSHOP_OPEN=true）或直接关闭界面 */
    private void cancelRoll() {
        if (closed) return;
        closed = true;
        PacketHandler.CHANNEL.sendToServer(new Packets.C2SCancelRoll());
        if (com.hexcraft.config.HexConfig.KEEP_WORKSHOP_OPEN.get()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.hexcraft.client.gui.WorldHexScreen());
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

    /** v0.6.3 完成：KEEP_WORKSHOP_OPEN=true 返回世界海克斯界面继续抽取；false 直接关闭 */
    private void done() {
        if (!com.hexcraft.config.HexConfig.KEEP_WORKSHOP_OPEN.get()) { net.minecraft.client.Minecraft.getInstance().setScreen(null); return; }
        net.minecraft.client.Minecraft.getInstance().setScreen(new com.hexcraft.client.gui.WorldHexScreen());
    }

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        int centerX = this.guiW() / 2;

        // 标题与装饰条（v0.6.3 界面停留：确认后标题切换为「已获得：名称」）
        Component title = chosenId != null
                ? Component.translatable("hexcraft.gui.got", chosenName)
                : Component.translatable("hexcraft.gui.world_hex_title");
        g.drawCenteredString(this.font, title, centerX, cardY - 34, 0xFFFFFF);
        int barW = this.font.width(title) + 40;
        g.fillGradient(centerX - barW / 2, cardY - 20, centerX + barW / 2, cardY - 19, 0xFFFFC94A, 0xFFC77DFF);

        // v0.4.9 抽取动画：动画期间隐藏按钮（避免半成品可点），手绘卡片；结束后恢复正常渲染
        // v0.6.3 已确认抽取时跳过动画直接渲染结果
        float t = Math.min(1f, (System.currentTimeMillis() - openAt) / (float) ANIM_MS);
        if (chosenId == null && t < 1f) {
            // 光柱从左向右扫过
            int sweep = (int) (t * (this.guiW() + 320)) - 160;
            g.fill(sweep, 0, sweep + 60, this.guiH(), 0x18FFFFFF);
            g.fill(sweep + 14, 0, sweep + 46, this.guiH(), 0x10FFC94A);
            for (int i = 0; i < 3 && i < msg.options.size(); i++) {
                WorldHexDefinition def = WorldHexRegistry.get(msg.options.get(i));
                if (def == null) continue;
                // 每张卡错峰（依次延迟约 1/3 时长）上浮 + 淡入，三次缓动
                float ti = Math.min(1f, Math.max(0f, t * 3f - i));
                float ease = 1f - (1f - ti) * (1f - ti) * (1f - ti);
                int dy = (int) ((1f - ease) * 26f);
                int alpha = 40 + (int) (ease * 215f);
                drawAnimatedCard(g, i, def, dy, alpha);
            }
            g.drawCenteredString(this.font, Component.translatable("hexcraft.worldhex.anim_draw"),
                    centerX, cardY + CARD_H + 26, 0xFF9FB6FF);
        } else {
            this.renderWidgets(g, mx, my, pt);
            for (int i = 0; i < 3 && i < msg.options.size(); i++) {
                WorldHexDefinition def = WorldHexRegistry.get(msg.options.get(i));
                if (def == null) continue;
                int color = rarityColor(def.rarity);
                drawCardText(g, i, def, color, cardX[i] + CARD_W / 2, cardY);
            }
            // v0.6.3 选中提示（确认模式）：选中卡底部显示提示条
            if (selectedIdx >= 0 && com.hexcraft.config.HexConfig.CONFIRM_SELECT.get()) {
                g.drawCenteredString(this.font, Component.translatable("hexcraft.gui.selected_hint"),
                        cardX[selectedIdx] + CARD_W / 2, cardY + CARD_H - 14, 0xFFFFFFFF);
            }
        }
    }

    /** 动画模式手绘整卡（背景 + 缎带 + 文字），带透明度和上浮偏移 */
    private void drawAnimatedCard(GuiGraphics g, int idx, WorldHexDefinition def, int dy, int alpha) {
        int color = rarityColor(def.rarity);
        int x = cardX[idx];
        int y = cardY + dy;
        int cx = x + CARD_W / 2;
        // 辉光 + 边框 + 渐变底（alpha 淡入）
        g.fill(x - 3, y - 3, x + CARD_W + 3, y + CARD_H + 3, (alpha >> 1) << 24 | (color & 0x00FFFFFF));
        g.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, alpha << 24 | (color & 0x00FFFFFF));
        g.fillGradient(x, y, x + CARD_W, y + CARD_H,
                (alpha * 240 / 255) << 24 | 0x2C2E58, (alpha * 240 / 255) << 24 | 0x0D1220);
        g.fill(x, y, x + CARD_W, y + 16, alpha << 24 | (color & 0x00FFFFFF));
        g.drawCenteredString(this.font, catLabel(def), cx, y + 4, 0xFF101018);
        g.drawCenteredString(this.font, "◆", cx, y + 22, color);
        drawCardText(g, idx, def, color, cx, y);
    }

    /** 卡片文字内容（名字 + 分隔线 + 描述 + 消耗），正常与动画模式共用 */
    private void drawCardText(GuiGraphics g, int idx, WorldHexDefinition def, int color, int cx, int cy) {
        Component name = Component.translatable("hexcraft.worldhex." + def.id + ".name");
        Component desc = Component.translatable("hexcraft.worldhex." + def.id + ".desc");
        g.drawCenteredString(this.font, name, cx, cy + 42, color);
        g.fill(cx - CARD_W / 2 + 14, cy + 56, cx + CARD_W / 2 - 14, cy + 57, 0x33505F88);
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

    private static String catLabel(WorldHexDefinition def) {
        return def == null ? "" : Component.translatable("hexcraft.worldhex.category." + def.category).getString();
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
        boolean selected;

        CardButton(int x, int y, int w, int h, int color, String label, OnPress onPress) {
            super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
            this.color = color;
            this.label = label;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int border = selected ? 0xFFFFFFFF : (isHover(mx, my, this) ? 0xFFFFFFFF : color);
            g.fill(getX() - 3, getY() - 3, getX() + width + 3, getY() + height + 3, 0x66 << 24 | (color & 0x00FFFFFF));
            g.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, 0xFF << 24 | (border & 0x00FFFFFF));
            g.fillGradient(getX(), getY(), getX() + width, getY() + height, 0xF02C2E58, 0xF00D1220);
            g.fill(getX(), getY(), getX() + width, getY() + 16, 0xFF << 24 | (color & 0x00FFFFFF));
            g.drawCenteredString(Minecraft.getInstance().font, label, getX() + width / 2, getY() + 4, 0xFF101018);
            g.drawCenteredString(Minecraft.getInstance().font, "◆", getX() + width / 2, getY() + 22,
                    0xFF << 24 | (color & 0x00FFFFFF));
            // v0.6.3 选中态：底部白色选中条
            if (selected) {
                g.fill(getX() + 20, getY() + height - 4, getX() + width - 20, getY() + height - 2, 0xFFFFFFFF);
            }
        }
    }
}
