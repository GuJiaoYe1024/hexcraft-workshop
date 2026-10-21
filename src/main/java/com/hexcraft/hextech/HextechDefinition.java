package com.hexcraft.hextech;

import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.List;

/** 海克斯增幅定义（内置 + 数据驱动） */
public class HextechDefinition {

    public final String id;
    public final HextechRarity rarity;
    public final int weight;
    public final List<ModSpec> mods;
    /** 事件型效果：kill_heal = 击杀回复 2×等级 生命 */
    public final String event;
    /** 装备图标（文本字符，仅装备用；海克斯为 null） */
    public final String icon;
    /** 装备卖出价（仅装备用；海克斯为 0） */
    public final int sell;
    /** 内容来源 modid："vanilla"(默认) / "tacz" / 未来联动 modid；决定抽取池是否启用 */
    public final String source;

    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event) {
        this(id, rarity, weight, mods, event, null, 0, null);
    }

    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event, String icon, int sell) {
        this(id, rarity, weight, mods, event, icon, sell, null);
    }

    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event, String icon, int sell, String source) {
        this.id = id;
        this.rarity = rarity;
        this.weight = weight;
        this.mods = mods;
        this.event = event;
        this.icon = icon;
        this.sell = sell;
        // 来源归一化：显式指定优先；否则按 id 前缀自动归类（tacz_ → tacz，其余 vanilla；未来联动注册见 HextechRegistry.registerSourcePrefix）
        this.source = source != null && !source.isEmpty() ? source : HextechRegistry.sourceOf(id);
    }

    public static class ModSpec {
        public final Attribute attr;
        public final double perLevel;
        public ModSpec(Attribute attr, double perLevel) {
            this.attr = attr;
            this.perLevel = perLevel;
        }
    }
}
