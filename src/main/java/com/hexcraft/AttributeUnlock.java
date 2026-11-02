package com.hexcraft;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/** v0.6.0 → v0.9.7 属性上限解锁：
 *  v0.6.0 原假设「maxValue 只钳制 baseValue、modifier 天然突破」在 1.20.1 不成立：
 *  Attribute.sanitizeValue() 对 computeValue 的最终总值整体 clamp 到 [minValue, maxValue]，
 *  MAX_HEALTH 上限恰为 1024.0，无上限锻造/叠层叠加到 1024 即被硬顶死（用户实测血量卡 1024）。
 *  v0.9.7 改为真解锁：反射把 maxValue 放大（AttributeFix 同款做法；net.minecraft 类在 classpath
 *  非模块路径，setAccessible 在 JDK17 有效），全部基础属性统一放大到 1e7（远高于任何锻造/叠层
 *  可达数值；1e7 转 float 仍有限值，不会出现 Infinity）。与 AttributeFix 等同类 MOD 并存无冲突
 *  （谁后写谁生效，目标一致都是放开上限）。minValue 保持原版下限不动。
 *
 *  2026-11-02 联动实测修复（正式客户端启动日志 8/8 解锁失败）：
 *  旧实现 Attribute.class.getDeclaredField("maxValue") 在正式客户端必抛 NoSuchFieldException：
 *  ① maxValue 字段声明在实现类 RangedAttribute，Attribute 抽象类本身没有该字段；
 *  ② 打包 reobf 后字段是 SRG 名 f_22308_（1.20.1 srg→official 映射表实测确认，
 *     f_22307_=minValue），反射字符串字面量不随 reobf 改写，按 Mojang 名查找必然失败。
 *  修复：改用 Forge 官方 ObfuscationReflectionHelper + SRG 名，开发（Mojang 名）与正式
 *  （SRG 名）环境都能正确定位；目标类改为 RangedAttribute。
 */
public final class AttributeUnlock {
    /** 1.20.1 SRG 字段名：RangedAttribute.maxValue（srg→official 映射表实测：f_22308_ maxValue） */
    private static final String MAX_VALUE_SRG = "f_22308_";

    public static void unlock() {
        int ok = 0;
        for (Attribute attr : new Attribute[]{
                Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
                Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, Attributes.MOVEMENT_SPEED,
                Attributes.LUCK, Attributes.KNOCKBACK_RESISTANCE, Attributes.ATTACK_KNOCKBACK}) {
            if (liftMax(attr)) ok++;
        }
        Hexcraft.LOGGER.info("[Hexcraft] v0.9.7 属性上限解锁完成：{} 项 attribute maxValue 已放大至 1e7（原 MAX_HEALTH=1024 等上限不再顶死生命/属性显示与加成）", ok);
    }

    /** 反射放大单个 attribute 的 maxValue 为 1e7；失败仅记录警告，不中断加载 */
    private static boolean liftMax(Attribute attr) {
        try {
            Field f = ObfuscationReflectionHelper.findField(RangedAttribute.class, MAX_VALUE_SRG);
            f.setAccessible(true);
            f.setDouble(attr, 1.0E7);
            return true;
        } catch (Exception e) {
            Hexcraft.LOGGER.warn("[Hexcraft] 属性上限解锁失败（{}）：{}", attr.getDescriptionId(), e.toString());
            return false;
        }
    }

    private AttributeUnlock() { }
}
