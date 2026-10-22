package com.hexcraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 可缩放界面基类（v0.4.4）：
 *  - 界面内容在「逻辑坐标系」（guiW × guiH = 窗口 / UI_SCALE）中布局与绘制；
 *  - 渲染时对 GuiGraphics 整体缩放（pose.scale），鼠标事件坐标同步逆缩放，
 *    保证按钮点击区域、悬停判定与绘制位置一致；
 *  - 背景（renderBackground）在缩放前全屏绘制，避免缩放后四边露黑。
 *  子类约定：用 guiW() / guiH() 替代 this.width / this.height；
 *  原 render() 逻辑移到 renderScaled()，末尾用 renderWidgets() 绘制按钮。
 */
public abstract class HexScaledScreen extends Screen {

    protected HexScaledScreen(Component title) {
        super(title);
    }

    /** 当前 UI 缩放系数（配置「界面缩放」） */
    protected final double uiScale() {
        return HexGuiScale.s();
    }

    /** 逻辑界面宽（窗口宽 ÷ 缩放，至少 320 防止过小布局崩坏） */
    protected final int guiW() {
        return Math.max(320, (int) (this.width / uiScale()));
    }

    /** 逻辑界面高（窗口高 ÷ 缩放，至少 240） */
    protected final int guiH() {
        return Math.max(240, (int) (this.height / uiScale()));
    }

    @Override
    public final void render(GuiGraphics g, int mx, int my, float pt) {
        // 配置「界面缩放」变化时重建控件，保证按钮位置/尺寸与渲染一致（即时生效，无需重开界面）
        double s = uiScale();
        if (this.lastScale != s) {
            this.lastScale = s;
            this.rebuildWidgets();
        }
        this.renderBackground(g);
        g.pose().pushPose();
        g.pose().scale((float) s, (float) s, 1);
        this.renderScaled(g, (int) (mx / s), (int) (my / s), pt);
        g.pose().popPose();
    }

    private double lastScale = 1.0;

    /** 子类绘制主体：在逻辑坐标系中布局（用 guiW()/guiH()），坐标已被缩放还原为逻辑值 */
    protected abstract void renderScaled(GuiGraphics g, int mx, int my, float pt);

    /** 在逻辑坐标系中渲染全部按钮（替代子类原 super.render 调用，避免背景二次绘制） */
    protected final void renderWidgets(GuiGraphics g, int mx, int my, float pt) {
        for (Renderable r : this.renderables) {
            r.render(g, mx, my, pt);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        double s = uiScale();
        return super.mouseClicked(mx / s, my / s, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        double s = uiScale();
        return super.mouseReleased(mx / s, my / s, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        double s = uiScale();
        return super.mouseDragged(mx / s, my / s, btn, dx / s, dy / s);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        double s = uiScale();
        return super.mouseScrolled(mx / s, my / s, delta);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        double s = uiScale();
        super.mouseMoved(mx / s, my / s);
    }

    /** 逻辑坐标系悬停判定：按钮 hover 高亮统一用传入的（已缩放的）鼠标坐标，
     *  避免依赖 Screen.mouseX/mouseY（原版屏幕坐标，缩放后与按钮位置不一致）。 */
    protected static boolean isHover(int mx, int my, Button b) {
        return mx >= b.getX() && mx <= b.getX() + b.getWidth()
                && my >= b.getY() && my <= b.getY() + b.getHeight();
    }
}
