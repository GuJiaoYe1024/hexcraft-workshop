package com.hexcraft.client.gui;

import com.hexcraft.config.HexConfig;

/** 客户端 UI 缩放工具（v0.4.4）：读取配置「客户端 - 界面缩放」，供各界面整体放大/缩小。
 *  配置修改后重新打开界面即生效（0.5x ~ 2.0x，默认 1.0）。 */
public final class HexGuiScale {
    private HexGuiScale() {}

    /** 当前缩放系数（限制在 0.5 ~ 2.0） */
    public static double s() {
        return Math.max(0.5, Math.min(2.0, HexConfig.UI_SCALE.get()));
    }
}
