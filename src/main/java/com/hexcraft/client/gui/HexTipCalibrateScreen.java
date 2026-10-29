package com.hexcraft.client.gui;

import com.hexcraft.config.HexConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * v0.7.0 提示位置校准全屏界面（替换 v0.6.22 会跑出配置界面的内嵌画布 entry）。
 * 全屏绘制「模拟屏幕 + 提示框」：按住蓝色提示框拖动实时调整滚动提示位置（TIP_X/TIP_Y），
 * 松开自动保存；点「完成」或按 ESC 返回配置界面。拖动映射按 4000 上限等比换算，钳 0~4000。
 */
public class HexTipCalibrateScreen extends Screen {

    private static final int AREA_W = 420;   // 模拟屏幕区宽
    private static final int AREA_H = 236;   // 模拟屏幕区高
    private static final int BOX_W = 132;
    private static final int BOX_H = 16;

    private final Screen parent;
    private boolean dragging = false;
    private int dragOffX = 0;
    private int dragOffY = 0;

    public HexTipCalibrateScreen(Screen parent) {
        super(Component.translatable("hexcraft.config.v060.tipDragCanvas"));
        this.parent = parent;
    }

    private int areaX() {
        return (this.width - AREA_W) / 2;
    }
    private int areaY() {
        return (this.height - AREA_H) / 2 - 20;
    }

    /** TIP_X（0~4000）→ 模拟区内的 x */
    private int boxX() {
        return areaX() + (int) (HexConfig.TIP_X.get() * (AREA_W - BOX_W) / 4000.0);
    }
    private int boxY() {
        return areaY() + (int) (HexConfig.TIP_Y.get() * (AREA_H - BOX_H) / 4000.0);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 半透明全屏底
        g.fill(0, 0, this.width, this.height, 0xCC000000);
        // 标题
        g.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("hexcraft.config.v060.tipDragCanvas").getString(),
                this.width / 2, areaY() - 34, 0xFFFFFFFF);
        // 模拟屏幕区
        g.fill(areaX() - 1, areaY() - 1, areaX() + AREA_W + 1, areaY() + AREA_H + 1, 0xFF00E5FF);
        g.fill(areaX(), areaY(), areaX() + AREA_W, areaY() + AREA_H, 0xAA101828);
        // 提示框（当前位置）
        int bx = boxX();
        int by = boxY();
        g.fill(bx, by, bx + BOX_W, by + BOX_H, 0xFF66CCFF);
        g.drawString(Minecraft.getInstance().font,
                Component.translatable("hexcraft.tip.calibrate_hint").getString(),
                bx + 5, by + 3, 0xFF101820, true);
        // 操作说明 + 当前坐标
        g.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("hexcraft.config.tip.v060.tipDragHint").getString() + "  (" + HexConfig.TIP_X.get() + ", " + HexConfig.TIP_Y.get() + ")",
                this.width / 2, areaY() + AREA_H + 12, 0xFF8FA6C8);
        // 完成按钮
        g.fill(this.width / 2 - 60, this.height - 46, this.width / 2 + 60, this.height - 26, 0xFF00E5FF);
        g.drawCenteredString(Minecraft.getInstance().font,
                Component.translatable("gui.done").getString(),
                this.width / 2, this.height - 41, 0xFF101820);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int bx = boxX();
            int by = boxY();
            if (mouseX >= bx && mouseX <= bx + BOX_W && mouseY >= by && mouseY <= by + BOX_H) {
                dragging = true;
                dragOffX = (int) (mouseX - bx);
                dragOffY = (int) (mouseY - by);
                return true;
            }
            // 完成按钮
            if (mouseX >= this.width / 2 - 60 && mouseX <= this.width / 2 + 60
                    && mouseY >= this.height - 46 && mouseY <= this.height - 26) {
                close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            double sx = (AREA_W - BOX_W) / 4000.0;
            double sy = (AREA_H - BOX_H) / 4000.0;
            if (sx > 0 && sy > 0) {
                int nx = (int) ((mouseX - dragOffX - areaX()) / sx);
                int ny = (int) ((mouseY - dragOffY - areaY()) / sy);
                HexConfig.TIP_X.set(Math.max(0, Math.min(4000, nx)));
                HexConfig.TIP_Y.set(Math.max(0, Math.min(4000, ny)));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            try {
                HexConfig.CLIENT_SPEC.save();
            } catch (Exception ignored) { }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        close();
    }

    private void close() {
        if (parent != null) {
            Minecraft.getInstance().setScreen(parent);
        } else {
            Minecraft.getInstance().setScreen(null);
        }
    }
}
