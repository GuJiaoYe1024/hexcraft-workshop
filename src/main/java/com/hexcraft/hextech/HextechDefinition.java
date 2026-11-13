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
    /** v0.6.0 通用叠层配置（可选；有值则该海克斯在命中时自动叠层，并参与叠层乘区/衰减结算） */
    public final StackSpec stack;
    /** v0.6.2 叠层标签体系：stackTag[] 枚举字符串数组（代码内置中英翻译表），tooltip 运行时拼接【叠层：xxx｜xxx｜xxx】 */
    public final List<String> stackTags;
    /** v0.9.12 自身是否可叠层（默认 true）：false = 叠层增幅类（如叠角龙/迅战之角），只给其他叠层提供增幅，自身不涨层数；
     *  取代此前靠硬编码 id + 散落 continue 的隐式区分，数据驱动、不写死。 */
    public final boolean selfStack;
    /** v0.9.12 是否提供叠层获取倍率（默认 false，仅叠角龙 true）：drakeMult 按此标记查找，倍率值仍读可配置项，不写死 id。 */
    public final boolean stackGainMult;
    /** v0.9.16 功能分类键（对应 category.*，共 14 类；联动内容可为空串）。后续分类图案按此键出图。 */
    public final String category;
    /** v0.9.16 主题标签键（对应 theme.*，体系归属；可为空串）。仅文字展示，不参与筛选与图案。 */
    public final String theme;

    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event) {
        this(id, rarity, weight, mods, event, null, 0, null, null, null);
    }

    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event, String icon, int sell) {
        this(id, rarity, weight, mods, event, icon, sell, null, null, null);
    }

    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event, String icon, int sell, String source) {
        this(id, rarity, weight, mods, event, icon, sell, source, null, null);
    }

    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event, String icon, int sell, String source, StackSpec stack) {
        this(id, rarity, weight, mods, event, icon, sell, source, stack, null);
    }

    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event, String icon, int sell, String source, StackSpec stack, List<String> stackTags) {
        this(id, rarity, weight, mods, event, icon, sell, source, stack, stackTags, true, false);
    }

    /** v0.9.12 完整构造器：显式传入 selfStack / stackGainMult（数据驱动）。 */
    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event, String icon, int sell,
                             String source, StackSpec stack, List<String> stackTags, boolean selfStack, boolean stackGainMult) {
        this(id, rarity, weight, mods, event, icon, sell, source, stack, stackTags, selfStack, stackGainMult, "", "");
    }

    /** v0.9.16 完整构造器：额外携带功能分类与主题标签（数据驱动，供分类图案与筛选使用）。 */
    public HextechDefinition(String id, HextechRarity rarity, int weight, List<ModSpec> mods, String event, String icon, int sell,
                             String source, StackSpec stack, List<String> stackTags, boolean selfStack, boolean stackGainMult,
                             String category, String theme) {
        this.id = id;
        this.rarity = rarity;
        this.weight = weight;
        this.mods = mods;
        this.event = event;
        this.icon = icon;
        this.sell = sell;
        this.stack = stack;
        this.stackTags = stackTags;
        this.selfStack = selfStack;
        this.stackGainMult = stackGainMult;
        this.category = category == null ? "" : category;
        this.theme = theme == null ? "" : theme;
        // 来源归一化：显式指定优先；否则按 id 前缀自动归类（tacz_ → tacz，其余 vanilla；未来联动注册见 HextechRegistry.registerSourcePrefix）
        this.source = source != null && !source.isEmpty() ? source : HextechRegistry.sourceOf(id);
    }

    /** v0.6.0 叠层配置：key=叠层标签、cap=基础上限、combat=战斗叠层（脱战衰减+死亡清空）、
     *  decayDelay=脱战多少 tick 后衰减（-1=不衰减）、decayRate=每 tick 衰减量、
     *  gainTags/effect 由事件层消费（如 on_hit 命中叠层、on_kill 击杀叠层） */
    public static class StackSpec {
        public final String key;
        public final double cap;
        public final boolean combat;
        public final int decayDelay;
        public final double decayRate;
        public final boolean onKill;
        public StackSpec(String key, double cap, boolean combat, int decayDelay, double decayRate, boolean onKill) {
            this.key = key; this.cap = cap; this.combat = combat;
            this.decayDelay = decayDelay; this.decayRate = decayRate; this.onKill = onKill;
        }
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
