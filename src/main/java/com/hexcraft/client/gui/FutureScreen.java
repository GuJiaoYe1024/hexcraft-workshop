package com.hexcraft.client.gui;

import com.hexcraft.hextech.HextechRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 联动总览页（v0.4.2 敬请期待页升级，v0.8.4 实装）：
 *  世界海克斯左侧栏「联动总览」按钮打开，展示各联动 mod 的海克斯/装备统计与安装状态，
 *  未安装的联动 mod 自动隐藏（内容不进入抽取池），已安装的按开关启用。
 *  v0.8.6：列表区支持鼠标滚轮滚动（视口裁剪，行不再画穿屏幕底部）；
 *          返回按钮固定右下角（不再与列表文字重叠，长列表时始终可见可点）。 */
public class FutureScreen extends HexScaledScreen {
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
            g.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                    getX() + width / 2, getY() + (height - 8) / 2, text);
        }
    }

    /** 联动来源显示名（modid → 展示名）；不在表中的 modid 原样显示 */
    private static final Map<String, String> LINK_NAMES = new LinkedHashMap<>();

    static {
        LINK_NAMES.put("tacz", "TACZ 永恒枪械工坊");
        LINK_NAMES.put("irons_spellbooks", "Iron's Spells 法术");
        LINK_NAMES.put("iceandfire", "冰与火之歌");
        LINK_NAMES.put("alexscaves", "Alex's Caves");
        LINK_NAMES.put("twilightforest", "暮色森林");
        LINK_NAMES.put("alexsmobs", "Alex's Mobs");
        LINK_NAMES.put("cataclysm", "Cataclysm 末地灾变");
        LINK_NAMES.put("goety", "Goety 巫术");
        LINK_NAMES.put("mowziesmobs", "Mowzie's Mobs");
        LINK_NAMES.put("artifacts", "Artifacts 神器");
        LINK_NAMES.put("relics", "Relics 遗物");
        LINK_NAMES.put("slashblade", "SlashBlade 拔刀剑");
        LINK_NAMES.put("farmersdelight", "农夫乐事");
        LINK_NAMES.put("ftbquests", "FTB Quests");
        LINK_NAMES.put("projecte", "ProjectE 等价交换");
        LINK_NAMES.put("dimensionstorage", "超越维度");
        LINK_NAMES.put("sophisticatedbackpacks", "精妙背包");
        LINK_NAMES.put("refinedstorage", "精致存储");
    }

    private int scrollY;
    /** 最近一次渲染的列表起点 / 行数（供滚轮计算最大滚动量） */
    private int lastListTop = 44;
    private int lastBodyRows = 0;

    public FutureScreen() {
        super(Component.translatable("hexcraft.gui.future_title"));
    }

    @Override
    protected void init() {
        this.scrollY = 0;
        // v0.8.6 返回按钮固定右下角（guiH-30 区域，列表视口底在其上方，永不重叠）
        this.addRenderableWidget(new WBtn(this.guiW() - 96, this.guiH() - 30, 80, 20,
                Component.translatable("gui.back"), b -> onClose()));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        double s = uiScale();
        int viewH = Math.max(20, this.guiH() - 36 - this.lastListTop);
        int maxScroll = Math.max(0, this.lastBodyRows * 11 - viewH + 11);
        if (maxScroll > 0) {
            int ns = (int) (this.scrollY - delta * 16);
            ns = Math.max(0, Math.min(ns, maxScroll));
            if (ns != this.scrollY) {
                this.scrollY = ns;
                return true;
            }
        }
        return super.mouseScrolled(mx / s, my / s, delta);
    }

    @Override
    protected void renderScaled(GuiGraphics g, int mx, int my, float pt) {
        int cx = this.guiW() / 2;
        Component title = Component.translatable("hexcraft.gui.future_title");
        g.drawCenteredString(this.font, title, cx, 12, 0xFFFFC94A);
        int barW = this.font.width(title) + 40;
        g.fillGradient(cx - barW / 2, 24, cx + barW / 2, 25, 0xFFFFC94A, 0xFFC77DFF);

        // 已装联动统计（按 source 分组：海克斯数 / 装备数）
        Map<String, Integer> hexBy = new LinkedHashMap<>();
        Map<String, Integer> eqBy = new LinkedHashMap<>();
        for (com.hexcraft.hextech.HextechDefinition d : HextechRegistry.all()) {
            if (d.source != null && !d.source.isEmpty()) hexBy.merge(d.source, 1, Integer::sum);
        }
        for (com.hexcraft.hextech.HextechDefinition d : HextechRegistry.allEquips()) {
            if (d.source != null && !d.source.isEmpty()) eqBy.merge(d.source, 1, Integer::sum);
        }
        List<String> installed = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String src : LINK_NAMES.keySet()) {
            int h = hexBy.getOrDefault(src, 0);
            int e = eqBy.getOrDefault(src, 0);
            String disp = LINK_NAMES.get(src);
            if (h + e == 0) continue; // 未注册任何内容的来源不展示
            if (ModList.get().isLoaded(src)) {
                installed.add(disp + "：海克斯 " + h + " / 装备 " + e);
            } else {
                // v0.8.5 未安装行也展示数量，方便了解注册内容（介绍写全）
                String detail = "";
                if (h > 0 && e > 0) detail = "：海克斯 " + h + " / 装备 " + e;
                else if (h > 0) detail = "：海克斯 " + h;
                else if (e > 0) detail = "：装备 " + e;
                missing.add(disp + detail + "（未安装，内容自动隐藏）");
            }
        }

        // 描述区固定（不滚动）
        int y = 44;
        Component desc = Component.translatable("hexcraft.gui.future_desc",
                hexBy.values().stream().mapToInt(Integer::intValue).sum(),
                eqBy.values().stream().mapToInt(Integer::intValue).sum());
        for (net.minecraft.util.FormattedCharSequence line : this.font.split(desc, Math.min(420, this.guiW() - 60))) {
            g.drawCenteredString(this.font, line, cx, y, 0xFFA9B4CC);
            y += 11;
        }
        y += 6;

        // 列表区：组装行（标题行/空态行/内容行），滚动偏移 + 视口裁剪（底部为返回按钮留白）
        List<String> body = new ArrayList<>();
        if (installed.isEmpty()) {
            body.add("__empty__");
        } else {
            body.add("__installed__");
            body.addAll(installed);
        }
        if (!missing.isEmpty()) {
            body.add("__missing__");
            body.addAll(missing);
        }
        int listTop = y;
        int viewBot = this.guiH() - 36;
        for (int i = 0; i < body.size(); i++) {
            int ly = listTop + i * 11 - this.scrollY;
            if (ly < listTop - 11 || ly > viewBot) continue; // 视口外行不绘制
            String s = body.get(i);
            if (s.equals("__empty__")) {
                g.drawCenteredString(this.font, Component.translatable("hexcraft.gui.future_empty"), cx, ly, 0xFF6A7B96);
            } else if (s.equals("__installed__")) {
                g.drawString(this.font, Component.translatable("hexcraft.gui.future_installed"),
                        cx - 200, ly, 0xFF9FB6FF);
            } else if (s.equals("__missing__")) {
                g.drawString(this.font, Component.translatable("hexcraft.gui.future_missing"),
                        cx - 200, ly, 0xFF9FB6FF);
            } else if (installed.contains(s)) {
                g.drawString(this.font, Component.literal("• " + s), cx - 200, ly, 0xFFD0D8E8);
            } else {
                g.drawString(this.font, Component.literal("• " + s), cx - 200, ly, 0xFF8FA6C8);
            }
        }
        this.lastListTop = listTop;
        this.lastBodyRows = body.size();

        this.renderWidgets(g, mx, my, pt);
    }
}
