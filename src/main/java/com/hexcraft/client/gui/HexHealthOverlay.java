package com.hexcraft.client.gui;

import com.hexcraft.config.HexConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** v0.6.16 海克斯风简易血条：方框内显示「当前生命/生命上限」，开启后取代原版生命条（原版不再渲染）。
 *  v0.7.0 合并护甲 + 饱食度：开启后原版生命条/护甲条/饱食度条全部不再渲染，三合一列显示——
 *  生命（❤ 当前/上限，比例色）、护甲（⛨ 当前/20，青色）、饱食（🍗 当前/20，橙色，附饱和度）。
 *  海克斯风格双色描边（青/紫）+ 半透明底 + 中央数字；生命比例越低数字越红。
 *  配置（HUD 显示优化分组）：hpEnabled 默认关闭、hpX/hpY 默认 0=自动原版血条位置。 */
public class HexHealthOverlay {
    /** v0.7.1 紧凑化：120x16 → 96x13，三行竖排总高收窄，不再遮挡视线 */
    private static final int BOX_W = 96;
    private static final int BOX_H = 13;
    private static final int ROW_GAP = 2;
    /** 三行总高：生命/护甲/饱食 */
    private static final int TOTAL_H = BOX_H * 3 + ROW_GAP * 2;

    /** 开启时取消原版生命条/护甲条/饱食度条渲染（三合一取代而非叠加） */
    @SubscribeEvent
    public static void onOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (!HexConfig.HP_ENABLED.get()) return;
        if (Minecraft.getInstance().player == null) return;
        var t = event.getOverlay();
        if (t == VanillaGuiOverlay.PLAYER_HEALTH.type()
                || t == VanillaGuiOverlay.ARMOR_LEVEL.type()
                || t == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!HexConfig.HP_ENABLED.get()) return;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        // v0.7.4 坐标语义统一：0~4000 = 全屏比例坐标（与布局画布同一公式，所见即所得）
        int x = HexConfig.HP_X.get() > 0 ? (int) (HexConfig.HP_X.get() * sw / 4000.0) : sw / 2 - 91;
        int y = HexConfig.HP_Y.get() > 0 ? (int) (HexConfig.HP_Y.get() * sh / 4000.0) : sh - 39 - (TOTAL_H - BOX_H);
        // 防止配置越界
        x = Math.max(0, Math.min(sw - BOX_W, x));
        y = Math.max(0, Math.min(sh - TOTAL_H, y));
        GuiGraphics g = event.getGuiGraphics();

        // 生命行
        float cur = mc.player.getHealth();
        float max = mc.player.getMaxHealth();
        float ratio = max > 0 ? cur / max : 0;
        int hpCol = ratio > 0.5f ? 0xFFFFFFFF : (ratio > 0.3f ? 0xFFFFFF00 : 0xFFFF5555);
        row(g, x, y, Component.translatable("hexcraft.hud.hp").getString(),
                String.format("%.0f/%.0f", cur, max), ratio, 0xFF00E5FF, hpCol);

        // 护甲行
        int armor = mc.player.getArmorValue();
        float armorRatio = Math.min(1.0f, armor / 20.0f);
        row(g, x, y + BOX_H + ROW_GAP, Component.translatable("hexcraft.hud.armor").getString(),
                armor + "/20", armorRatio, 0xFF00C8FF, 0xFFFFFFFF);

        // 饱食行（含饱和度小字）
        FoodData food = mc.player.getFoodData();
        int foodLv = food.getFoodLevel();
        float sat = food.getSaturationLevel();
        float foodRatio = Math.min(1.0f, foodLv / 20.0f);
        row(g, x, y + (BOX_H + ROW_GAP) * 2, Component.translatable("hexcraft.hud.food").getString(),
                foodLv + "/20", foodRatio, 0xFFFFB74D, 0xFFFFFFFF);
    }

    /** 绘制一行海克斯风数值框：外框描边 + 半透明底 + 底部比例色条 + 中央「标签 数值」 */
    private static void row(GuiGraphics g, int x, int y, String label, String value,
                            float ratio, int frameCol, int textCol) {
        // 外框：海克斯风描边
        g.fill(x - 1, y - 1, x + BOX_W + 1, y + BOX_H + 1, frameCol);
        // 内底：半透明黑（v0.8.0 透明度略降更通透，数值仍可读）
        g.fill(x, y, x + BOX_W, y + BOX_H, 0x8C101010);
        // 比例色条（底部 2px）：青/橙随比例
        int barCol = ratio > 0.5f ? frameCol : (ratio > 0.3f ? 0xFFFFFF00 : 0xFFFF3B30);
        g.fill(x, y + BOX_H - 2, (int) (x + BOX_W * ratio), y + BOX_H, barCol);
        // 标签（左，描边色）
        g.drawString(Minecraft.getInstance().font, label, x + 2, y + 2, frameCol, true);
        // 数值（右对齐，紧凑字号）
        g.drawString(Minecraft.getInstance().font, value,
                x + BOX_W - Minecraft.getInstance().font.width(value) - 3, y + 2, textCol, true);
    }
}
