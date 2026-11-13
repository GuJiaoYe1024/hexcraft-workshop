package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 盔甲纹饰类专属海克斯事件（v0.9.12 新增）。
 * trim_boost：穿戴带有盔甲纹饰的装备时，额外获得抗性效果。
 * 1.20.1 纹饰本身无属性且无锻造台事件，通过玩家定时轮询盔甲纹饰实现。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class TrimEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (sp.tickCount % 40 != 0) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double tb = HextechEvents.effect(data, "trim_boost");
        if (tb <= 0) return;
        boolean trimmed = false;
        for (ItemStack armor : sp.getArmorSlots()) {
            if (ArmorTrim.getTrim(sp.level().registryAccess(), armor).isPresent()) {
                trimmed = true;
                break;
            }
        }
        if (!trimmed) return;
        // v0.9.20 修复：抗性等级按数值生效（1=抗性 I、2=抗性 II）。此前只看 >0，
        // trim_boost:1 与 trim_boost:0.1 效果完全相同，纹饰师描述的「抗性 +10%/级」并不存在。
        int amp = Math.max(0, Math.min(2, (int) Math.round(tb) - 1));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, amp, true, false));
    }
}
