package com.hexcraft.client.gui;

import com.hexcraft.config.HexConfig;
import net.minecraft.client.Minecraft;

/** 客户端 UI 缩放工具（v0.4.4）：读取配置「客户端 - 界面缩放」，供各界面整体放大/缩小。
 *  配置修改后重新打开界面即生效（0.5x ~ 2.0x，默认 1.0）。
 *  v0.9.8 新增「界面尺寸档位自适应」：海克斯界面以前固定 304×300 逻辑像素，
 *  不随原版「视频设置 - 界面尺寸（GUI Scale）」变化——界面尺寸大档（3/4）时超出屏幕看不全、
 *  小档（1/2）时又太小。现按窗口逻辑分辨率自动缩放：
 *  - 以主界面设计尺寸 304×300 + 边距为基准，逻辑分辨率越大放大越多、越小收缩防超屏；
 *  - 最终缩放同时受「界面缩放」配置放大与屏幕完整性上限约束（保留约 8% 边距）。
 *  所有继承 HexScaledScreen 的界面（渲染 pose、鼠标事件、guiW/guiH 布局）自动生效。 */
public final class HexGuiScale {
    private HexGuiScale() {}

    /** 自适应基准：主界面设计尺寸 304×300 + 24/48px 边距（宽 352、高 348） */
    private static final double AUTO_BASE_W = 352.0;
    private static final double AUTO_BASE_H = 348.0;
    /** 自适应上下限：最小 0.75x（极小窗口保底不崩）、最大 2.0x（防过分放大） */
    private static final double AUTO_MIN = 0.75;
    private static final double AUTO_MAX = 2.0;
    /** 完整性边距系数：界面放大后最多占屏 ~92%（宽高各自计算） */
    private static final double FIT_MARGIN = 0.92;

    /** 当前缩放系数（限制在 0.5 ~ 2.0）× 界面尺寸档位自适应系数 */
    public static double s() {
        double cfg = Math.max(0.5, Math.min(2.0, HexConfig.UI_SCALE.get()));
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) return cfg;
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();
        if (sw <= 0 || sh <= 0) return cfg;
        // 界面尺寸档位自适应：逻辑分辨率 ÷ 设计基准（含边距）
        double auto = Math.min(sw / AUTO_BASE_W, sh / AUTO_BASE_H);
        auto = Math.max(AUTO_MIN, Math.min(AUTO_MAX, auto));
        double s = cfg * auto;
        // 完整性上限：主界面 304×300 缩放后不得超出屏幕（按屏幕实际宽高反算）
        double cap = Math.min(sw / 304.0, sh / 300.0) * FIT_MARGIN;
        return Math.min(s, Math.max(cap, AUTO_MIN));
    }
}
