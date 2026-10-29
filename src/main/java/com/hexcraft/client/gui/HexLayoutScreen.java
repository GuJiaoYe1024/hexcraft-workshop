package com.hexcraft.client.gui;

import com.hexcraft.config.HexConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * v0.7.3 界面布局编辑器（透明画布，所见即所得）：
 * 游戏内按 L 打开后背景完全透明（游戏画面直接透出），三个元素框按 0~4000 坐标
 * 等比映射到【真实全屏位置】绘制——框落在哪，实际就显示在哪，拖动所见即所得。
 * 点顶部按钮切换元素、按住蓝色框拖动实时改位置、松手自动保存、ESC 返回游戏。
 */
public class HexLayoutScreen extends Screen {

    /** 可拖元素：滚动提示 / 状态栏 / 背包页签 */
    private static final String[] ELEM_KEYS = {
            "hexcraft.config.v070.layoutElemTip",
            "hexcraft.config.v070.layoutElemHp",
            "hexcraft.config.v070.layoutElemTab",
    };

    private final Screen parent;
    private int current = 0;       // 当前选中元素
    private boolean dragging = false;
    private int dragOffX = 0;
    private int dragOffY = 0;

    public HexLayoutScreen(Screen parent) {
        super(Component.translatable("hexcraft.config.v070.layoutTitle"));
        this.parent = parent;
    }

    /** 各元素模拟框宽高（透明画布上按真实比例缩放显示） */
    private int boxW(int idx) {
        return idx == 0 ? 200 : (idx == 1 ? 102 : 62);
    }
    private int boxH(int idx) {
        return idx == 0 ? 16 : (idx == 1 ? 42 : 26);
    }

    /** 读取元素当前位置：0=自动默认位（显示与实际渲染一致的默认位置示意），>0 用配置值。
     *  v0.7.4 默认位换算与实际渲染公式对齐（TIP 左上角 / HP 原版血条位 / TAB 容器右上方）。
     *  v0.7.5 页签坐标语义修正：TAB 配置值是【相对容器左上角】的偏移（实际渲染 guiLeft+tabX*sw/4000），
     *  画布框必须显示在【绝对屏幕位置】（假设容器居中），拖动才与真实页签重合（所见即所得）。 */
    private int elemX(int idx) {
        int v = idx == 0 ? HexConfig.TIP_X.get() : (idx == 1 ? HexConfig.HP_X.get() : HexConfig.TAB_X.get());
        if (idx == 2) { // 页签：绝对位置 = 容器左上 + 偏移
            int guiLeft = (this.width - 176) / 2;
            int tabXpx = v > 0 ? (int) (v * this.width / 4000.0) : 124;
            return (int) ((guiLeft + tabXpx) * 4000.0 / this.width);
        }
        if (v > 0) return v;
        if (idx == 0) return 8;
        return (int) ((this.width / 2 - 91) * 4000.0 / this.width);       // sw/2-91 的等价比例
    }
    private int elemY(int idx) {
        int v = idx == 0 ? HexConfig.TIP_Y.get() : (idx == 1 ? HexConfig.HP_Y.get() : HexConfig.TAB_Y.get());
        if (idx == 2) { // 页签：绝对位置 = 容器左上 + 偏移（默认 -28 容器上方）
            int guiTop = (this.height - 166) / 2;
            int tabYpx = v > 0 ? (int) (v * this.height / 4000.0) : -28;
            return (int) ((guiTop + tabYpx) * 4000.0 / this.height);
        }
        if (v > 0) return v;
        if (idx == 0) return 34;
        return (int) ((this.height - 67) * 4000.0 / this.height);         // sh-39-28
    }

    private void setElemXY(int idx, int x, int y) {
        if (idx == 0) {
            HexConfig.TIP_X.set(clamp(x)); HexConfig.TIP_Y.set(clamp(y));
        } else if (idx == 1) {
            HexConfig.HP_X.set(clamp(x)); HexConfig.HP_Y.set(clamp(y));
        } else {
            // 页签：鼠标位置是绝对屏幕比例 → 转回【相对容器左上角】偏移比例写回（与实际渲染公式互逆）
            int guiLeft = (this.width - 176) / 2;
            int guiTop = (this.height - 166) / 2;
            int relXpx = (int) (x * this.width / 4000.0) - guiLeft;
            int relYpx = (int) (y * this.height / 4000.0) - guiTop;
            HexConfig.TAB_X.set(clamp((int) (relXpx * 4000.0 / this.width)));
            HexConfig.TAB_Y.set(clamp((int) (relYpx * 4000.0 / this.height)));
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(4000, v));
    }

    /** v0.7.5 重置：全部元素恢复默认位置并保存 */
    private void resetAll() {
        HexConfig.TIP_X.set(8); HexConfig.TIP_Y.set(34);
        HexConfig.HP_X.set(0); HexConfig.HP_Y.set(0);
        HexConfig.TAB_X.set(0); HexConfig.TAB_Y.set(0);
        try {
            HexConfig.CLIENT_SPEC.save();
        } catch (Exception ignored) { }
    }

    /** 0~4000 → 真实屏幕坐标（左上角，所见即所得） */
    private int boxScreenX(int idx) {
        return (int) (elemX(idx) * this.width / 4000.0);
    }
    private int boxScreenY(int idx) {
        return (int) (elemY(idx) * this.height / 4000.0);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        // 背景完全透明：不绘制任何底，游戏画面直接透出 → 框落在哪实际就在哪
        // 顶部标题（左上角小字，半透明底避免看不清）
        String title = Component.translatable("hexcraft.config.v070.layoutTitle").getString();
        g.fill(4, 4, 4 + mc.font.width(title) + 8, 16, 0xAA101820);
        g.drawString(mc.font, title, 8, 6, 0xFF00E5FF, true);
        // 元素切换按钮（顶部右侧横排）
        int btnY = 6;
        int acc = this.width - 12;
        for (int i = ELEM_KEYS.length - 1; i >= 0; i--) {
            String t = Component.translatable(ELEM_KEYS[i]).getString();
            int bw = mc.font.width(t) + 20;
            int bx = acc - bw;
            int col = i == current ? 0xFF00E5FF : 0xAA2A4A6A;
            g.fill(bx, btnY, bx + bw, btnY + 18, col);
            g.drawCenteredString(mc.font, t, bx + bw / 2, btnY + 5, 0xFF101820);
            acc = bx - 6;
        }
        // 三个元素框：直接画在真实屏幕位置（当前元素蓝色可拖，其它灰色占位）
        for (int i = 0; i < 3; i++) {
            int bx = Math.max(0, Math.min(this.width - boxW(i), boxScreenX(i)));
            int by = Math.max(0, Math.min(this.height - boxH(i), boxScreenY(i)));
            // v0.8.0 选中元素白色描边 + 框内实时坐标，拖动时一眼可辨
            // v0.8.1 拖动中改半透明：不蒙住游戏画面，位置调整所见即所得
            if (i == current) {
                boolean moving = dragging && i == current;
                g.fill(bx - 1, by - 1, bx + boxW(i) + 1, by + boxH(i) + 1, moving ? 0x40FFFFFF : 0xFFFFFFFF);
                g.fill(bx, by, bx + boxW(i), by + boxH(i), moving ? 0x4066CCFF : 0xFF66CCFF);
                int ex = current == 0 ? HexConfig.TIP_X.get() : (current == 1 ? HexConfig.HP_X.get() : HexConfig.TAB_X.get());
                int ey = current == 0 ? HexConfig.TIP_Y.get() : (current == 1 ? HexConfig.HP_Y.get() : HexConfig.TAB_Y.get());
                String xy = "(" + ex + ", " + ey + ")";
                g.drawString(mc.font, xy, bx + boxW(i) - mc.font.width(xy) - 2, by + 2, 0xFFFFFFFF, true);
            } else {
                g.fill(bx, by, bx + boxW(i), by + boxH(i), 0x66556688);
            }
            String lab = Component.translatable(ELEM_KEYS[i]).getString();
            g.drawString(mc.font, lab, bx + 4, by + 3, 0xFF101820, true);
        }
        // 底部说明 + 当前坐标
        String cur = Component.translatable(ELEM_KEYS[current]).getString();
        int cx = current == 0 ? HexConfig.TIP_X.get() : (current == 1 ? HexConfig.HP_X.get() : HexConfig.TAB_X.get());
        int cy = current == 0 ? HexConfig.TIP_Y.get() : (current == 1 ? HexConfig.HP_Y.get() : HexConfig.TAB_Y.get());
        String hint = Component.translatable("hexcraft.config.v070.layoutHint").getString() + "  " + cur + " (" + cx + ", " + cy + ")";
        g.fill(4, this.height - 16, 4 + mc.font.width(hint) + 8, this.height - 2, 0xAA101820);
        g.drawString(mc.font, hint, 8, this.height - 13, 0xFF8FA6C8, true);
        // 完成按钮（右下角）
        String done = Component.translatable("gui.done").getString();
        int dw = mc.font.width(done) + 24;
        g.fill(this.width - dw - 6, this.height - 24, this.width - 6, this.height - 6, 0xFF00E5FF);
        g.drawCenteredString(mc.font, done, this.width - dw / 2 - 6, this.height - 19, 0xFF101820);
        // v0.7.5 重置按钮（完成左侧）：恢复全部元素默认位置
        String rst = Component.translatable("hexcraft.config.v070.layoutReset").getString();
        int rw = mc.font.width(rst) + 24;
        g.fill(this.width - dw - rw - 16, this.height - 24, this.width - dw - 6 - 4, this.height - 6, 0xFF8A6AFF);
        g.drawCenteredString(mc.font, rst, this.width - dw - rw / 2 - 11, this.height - 19, 0xFF101820);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Minecraft mc = Minecraft.getInstance();
            // 顶部元素切换按钮
            int btnY = 6;
            int acc = this.width - 12;
            for (int i = ELEM_KEYS.length - 1; i >= 0; i--) {
                String t = Component.translatable(ELEM_KEYS[i]).getString();
                int bw = mc.font.width(t) + 20;
                int bx = acc - bw;
                if (mouseX >= bx && mouseX <= bx + bw && mouseY >= btnY && mouseY <= btnY + 18) {
                    current = i;
                    dragging = false;
                    return true;
                }
                acc = bx - 6;
            }
            // 拖动当前元素（真实屏幕位置）
            int bx = Math.max(0, Math.min(this.width - boxW(current), boxScreenX(current)));
            int by = Math.max(0, Math.min(this.height - boxH(current), boxScreenY(current)));
            if (mouseX >= bx && mouseX <= bx + boxW(current) && mouseY >= by && mouseY <= by + boxH(current)) {
                dragging = true;
                dragOffX = (int) (mouseX - bx);
                dragOffY = (int) (mouseY - by);
                return true;
            }
            // 完成按钮（右下角）
            String done = Component.translatable("gui.done").getString();
            int dw = mc.font.width(done) + 24;
            if (mouseX >= this.width - dw - 6 && mouseX <= this.width - 6
                    && mouseY >= this.height - 24 && mouseY <= this.height - 6) {
                close();
                return true;
            }
            // v0.7.5 重置按钮（完成左侧）：恢复全部元素默认位置
            String rst = Component.translatable("hexcraft.config.v070.layoutReset").getString();
            int rw = mc.font.width(rst) + 24;
            if (mouseX >= this.width - dw - rw - 16 && mouseX <= this.width - dw - 10
                    && mouseY >= this.height - 24 && mouseY <= this.height - 6) {
                resetAll();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            // 0~4000 与全屏等比映射
            double sx = this.width / 4000.0;
            double sy = this.height / 4000.0;
            if (sx > 0 && sy > 0) {
                int nx = (int) ((mouseX - dragOffX) / sx);
                int ny = (int) ((mouseY - dragOffY) / sy);
                setElemXY(current, nx, ny);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /** v0.8.0 键盘微调：方向键按 1px 步进移动当前元素（Shift 按住 10px 大步进），
     *  精确改配置不用再手填坐标值；ESC 交给父类关闭。 */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263 || keyCode == 262 || keyCode == 265 || keyCode == 264) {
            int step = (modifiers & 1) != 0 ? 10 : 1; // Shift=大步进
            int x = current == 0 ? HexConfig.TIP_X.get() : (current == 1 ? HexConfig.HP_X.get() : HexConfig.TAB_X.get());
            int y = current == 0 ? HexConfig.TIP_Y.get() : (current == 1 ? HexConfig.HP_Y.get() : HexConfig.TAB_Y.get());
            if (keyCode == 263) x -= step;
            else if (keyCode == 262) x += step;
            else if (keyCode == 265) y -= step;
            else if (keyCode == 264) y += step;
            setElemXY(current, x, y);
            try {
                HexConfig.CLIENT_SPEC.save();
            } catch (Exception ignored) { }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
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
        Minecraft.getInstance().setScreen(parent != null ? parent : null);
    }
}
