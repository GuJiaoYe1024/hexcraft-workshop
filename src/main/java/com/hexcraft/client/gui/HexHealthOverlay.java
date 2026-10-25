package com.hexcraft.client.gui;

import com.hexcraft.config.HexConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** v0.6.16 海克斯风简易血条：方框内显示「当前生命/生命上限」，开启后取代原版生命条（原版不再渲染）。
 *  海克斯风格双色描边（青/紫）+ 半透明底 + 中央数字；生命比例越低数字越红。
 *  配置（HUD 显示优化分组）：hpEnabled 默认关闭、hpX/hpY 默认 0=自动右上角。 */
public class HexHealthOverlay {
    private static final int BOX_W = 120;
    private static final int BOX_H = 16;

    /** 开启时取消原版 PLAYER_HEALTH overlay 渲染（取代而非叠加；饥饿/护甲等其它 HUD 不受影响） */
    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (HexConfig.HP_ENABLED.get() && Minecraft.getInstance().player != null
                && event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!HexConfig.HP_ENABLED.get()) return;
        float cur = mc.player.getHealth();
        float max = mc.player.getMaxHealth();
        if (max <= 0) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        // 默认固定到原版血条位置（左下角）；hpX/hpY 填非 0 值可手动改位
        int x = HexConfig.HP_X.get() > 0 ? HexConfig.HP_X.get() : sw / 2 - 91;
        int y = HexConfig.HP_Y.get() > 0 ? HexConfig.HP_Y.get() : sh - 39;
        // 防止配置越界
        x = Math.max(0, Math.min(sw - BOX_W, x));
        y = Math.max(0, Math.min(sh - BOX_H, y));
        GuiGraphics g = event.getGuiGraphics();
        // 外框：青色描边（海克斯科技感）
        g.fill(x - 1, y - 1, x + BOX_W + 1, y + BOX_H + 1, 0xFF00E5FF);
        // 内底：半透明黑
        g.fill(x, y, x + BOX_W, y + BOX_H, 0xB0101010);
        // 生命比例色条（底部 2px）：青→绿→红
        float ratio = cur / max;
        int barCol = ratio > 0.5f ? 0xFF00E5FF : (ratio > 0.3f ? 0xFFFFFF00 : 0xFFFF3B30);
        g.fill(x, y + BOX_H - 2, (int) (x + BOX_W * ratio), y + BOX_H, barCol);
        // 中央数字：当前/上限
        String text = String.format("%.0f/%.0f", cur, max);
        int col = ratio > 0.3f ? 0xFFFFFFFF : 0xFFFF5555;
        g.drawString(mc.font, text, x + (BOX_W - mc.font.width(text)) / 2, y + 3, col, true);
        // 生命值标签（小，青色）：❤
        g.drawString(mc.font, "\u2665", x + 3, y + 3, 0xFF00E5FF, true);
    }
}
