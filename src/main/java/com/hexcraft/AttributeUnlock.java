package com.hexcraft;

import net.minecraft.world.entity.ai.attributes.Attribute;

/** v0.6.0 原版属性上限说明（冗余保险）：
 *  原版 1.20.1 中 maxValue 只钳制 baseValue（setBaseValue 时 clamp），而 AttributeModifier 总和
 *  （computeValue 纯求和）不受 maxValue 限制——本 mod 全属性修正均走 AttributeModifier 稳定 UUID 路径，
 *  无上限锻造天然突破原版上限，无需反射修改 maxValue。
 *  反射方案（setAccessible）在 Forge 受 JDK 强封装/启动参数禁用，已弃用；本类保留仅为未来参考。
 */
public final class AttributeUnlock {
    public static void unlock() {
        Hexcraft.LOGGER.info("[Hexcraft] v0.6.2 属性上限：modifier 路径天然突破原版 maxValue（maxValue 仅钳制 baseValue），无需反射；无上限锻造不受上限限制");
    }

    private AttributeUnlock() { }
}
