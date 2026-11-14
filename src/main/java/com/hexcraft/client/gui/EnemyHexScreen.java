package com.hexcraft.client.gui;

import com.hexcraft.client.ClientDataStore;
import com.hexcraft.network.PacketHandler;
import com.hexcraft.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** 难度提升页（v0.9.18，原「敌方海克斯」）：
 *  展示已累积的敌方海克斯 / 敌方锻造器层数、下一层进度，并提供两条「花经验+晶币手动抽取」入口。
 *  实际加成由 HextechEvents.onMobFinalizeSpawn 在精英/BOSS 生成时按玩家清单逐条应用（层数不设上限）。 */
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
        int cx = this.guiW() / 2;
        this.addRenderableWidget(new WBtn(cx - 190, 96, 180, 20,
                Component.translatable("hexcraft.gui.enemy_hex_btn_draw"), b -> {
                    if (Minecraft.getInstance().getConnection() != null) {
                        PacketHandler.CHANNEL.sendToServer(new Packets.C2SBuyEnemyHex());
                    }
                }));
        this.addRenderableWidget(new WBtn(cx + 10, 96, 180, 20,
                Component.translatable("hexcraft.gui.enemy_forge_btn_draw"), b -> {
                    if (Minecraft.getInstance().getConnection() != null) {
                        PacketHandler.CHANNEL.sendToServer(new Packets.C2SBuyEnemyForge());
                    }
                }));
    }

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        tips.clear();
        int cx = this.guiW() / 2;
        Component title = Component.translatable("hexcraft.gui.enemy_hex_title");
        HexFonts.centered(g, title, cx, 12, 0xFFFFC94A, false);
        int barW = HexFonts.ui().width(title) + 40;
        g.fillGradient(cx - barW / 2, 24, cx + barW / 2, 25, 0xFFFFC94A, 0xFFC77DFF);

        int y = 40;
        Component desc = Component.translatable("hexcraft.gui.enemy_hex_desc");
        for (net.minecraft.util.FormattedCharSequence line : HexFonts.ui().split(desc, Math.min(440, this.guiW() - 40))) {
            HexFonts.centered(g, line, cx, y, 0xFFA9B4CC, false);
            y += 11;
        }
        y += 3;
        HexFonts.centered(g, Component.translatable("hexcraft.gui.enemy_hex_status",
                ClientDataStore.enemyHexCount, ClientDataStore.enemyForgeCount), cx, y, 0xFFFFD97A, false);
        y += 12;
        HexFonts.centered(g, Component.translatable("hexcraft.gui.enemy_hex_progress",
                ClientDataStore.drawToEnemy, ClientDataStore.enemyHexNeed,
                ClientDataStore.forgeToEnemy, ClientDataStore.enemyForgeNeed), cx, y, 0xFFD0D8E8, false);

        int btnY = 96;
        HexFonts.centered(g, Component.translatable("hexcraft.gui.enemy_hex_price",
                ClientDataStore.enemyHexXpCost, ClientDataStore.enemyHexCoinCost), cx - 100, btnY + 24, 0xFF8FA6C8, false);
        HexFonts.centered(g, Component.translatable("hexcraft.gui.enemy_forge_price",
                ClientDataStore.enemyForgeXpCost, ClientDataStore.enemyForgeCoinCost), cx + 100, btnY + 24, 0xFF8FA6C8, false);

        int poolY = btnY + 40;
        drawOwned(g, "hexcraft.gui.enemy_hex_owned", com.hexcraft.hextech.HextechRegistry.ENEMY_HEX_POOL,
                ClientDataStore.enemyHexSummary, "hexcraft.gui.enemy_hex.", cx - 200, poolY);
        drawOwned(g, "hexcraft.gui.enemy_forge_owned", com.hexcraft.hextech.HextechRegistry.ENEMY_FORGE_POOL,
                ClientDataStore.enemyForgeSummary, "hexcraft.gui.enemy_forge.", cx + 10, poolY);

        g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.enemy_hex_note"),
                cx - 200, this.guiH() - 24, 0xFF8FA6C8, false);

        this.renderWidgets(g, mx, my, pt);
        drawTips(g, mx, my);
    }

    /** 悬停提示：每条强化悬停时显示完整效果（名称 + 数值/机制说明） */
    private final java.util.List<Object[]> tips = new java.util.ArrayList<>();

    /** 只列玩家「已获得」的词条与层数（未获得的不显示），三列排布；悬停显示完整效果 */
    private void drawOwned(GuiGraphics g, String titleKey, java.util.List<String> pool,
                           String summary, String prefix, int x, int y) {
        java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
        if (summary != null && !summary.isEmpty()) {
            for (String part : summary.split("\\|")) {
                int idx = part.lastIndexOf(':');
                if (idx <= 0) continue;
                try {
                    counts.put(part.substring(0, idx), Integer.parseInt(part.substring(idx + 1)));
                } catch (NumberFormatException ignored) { }
            }
        }
        int total = 0;
        for (int v : counts.values()) total += v;
        g.drawString(HexFonts.ui(), Component.translatable(titleKey).getString() + " · " + total,
                x, y, 0xFF9FB6FF, false);
        if (counts.isEmpty()) {
            g.drawString(HexFonts.ui(), Component.translatable("hexcraft.gui.enemy_hex_empty"),
                    x, y + 14, 0xFF6C7A93, false);
            return;
        }
        final int rows = 5;
        int col = 0, row = 0;
        for (String id : pool) {
            Integer n = counts.get(id);
            if (n == null) continue;
            String full = Component.translatable(prefix + id).getString();
            int sp = full.indexOf(' ');
            String name = sp > 0 ? full.substring(0, sp) : full;
            if (row == rows) {
                row = 0;
                col++;
            }
            int tx = x + col * 64;
            int ty = y + 14 + row * 12;
            g.drawString(HexFonts.ui(), name + " ×" + n, tx, ty, 0xFFD0D8E8, false);
            tips.add(new Object[]{tx, ty, HexFonts.ui().width(name + " ×" + n), full + " · " + Component.translatable("hexcraft.unit.layers_n", n).getString()});
            row++;
        }
    }

    /** 悬停提示：画在所有控件之后，覆盖在最上层 */
    private void drawTips(GuiGraphics g, int mx, int my) {
        for (Object[] o : tips) {
            int x = (Integer) o[0], y = (Integer) o[1], w = (Integer) o[2];
            if (mx >= x && mx <= x + w && my >= y - 1 && my <= y + 9) {
                String t = (String) o[3];
                int tw = HexFonts.ui().width(t) + 8;
                int bx = Math.min(x, this.guiW() - tw - 6);
                g.fill(bx - 2, y - 5, bx + tw, y + 11, 0xF00B1530);
                g.fill(bx - 2, y - 5, bx + tw, y - 4, 0xFF4AC7FF);
                g.drawString(HexFonts.ui(), t, bx + 2, y - 2, 0xFFD0D8E8, false);
            }
        }
    }
}
