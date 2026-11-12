package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * 位移冲刺专属海克斯事件（v0.9.12 新增）。
 * dash：疾跑时给移动速度加修正，停止疾跑即移除；dash_strike：疾跑中攻击额外增伤。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class DashEvents {
    /** dash 速度修正的固定 UUID，保证添加与移除的是同一个 modifier */
    private static final UUID DASH_MOD_UUID = UUID.fromString("7c1f5a2e-3b4d-4e6f-9a8b-0c1d2e3f4a5b");
    private static final String DASH_MOD_NAME = "HexDash";

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        AttributeInstance spd = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd == null) return;

        PlayerData data = ModCapabilities.get(sp);
        double dash = data == null ? 0 : HextechEvents.effect(data, "dash");
        boolean want = dash > 0 && sp.isSprinting();
        boolean has = spd.getModifier(DASH_MOD_UUID) != null;
        if (want && !has) {
            spd.addTransientModifier(new AttributeModifier(DASH_MOD_UUID, DASH_MOD_NAME, dash,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!want && has) {
            spd.removeModifier(DASH_MOD_UUID);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (!sp.isSprinting()) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double strike = HextechEvents.effect(data, "dash_strike");
        if (strike > 0) {
            event.setAmount(event.getAmount() * (float) (1 + strike));
        }
    }
}
