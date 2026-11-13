package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * 游泳潜水专属海克斯事件（v0.9.12 新增，v0.9.14 改为属性修饰）。
 * swim_swift：在水中时给移动速度加属性修正，离水立即移除。
 *
 * v0.9.14 修正原因：原实现对服务端玩家 setDeltaMovement 不生效。
 * 玩家自身的位移由客户端计算并上报，服务端每 tick 设置的 delta 会被下一帧
 * 客户端位置包覆盖，表现为"完全没感觉"。改为给 MOVEMENT_SPEED 加临时修饰，
 * 原版游泳的行进速度直接读取该属性，服务端权威、客户端同步，一定生效。
 */
public class SwimEvents {
    /** 游泳加速修饰的固定 UUID，保证添加与移除的是同一个 modifier */
    private static final UUID SWIM_UUID = UUID.fromString("a1b2c3d4-5678-4e9f-8a0b-1c2d3e4f5a6b");
    private static final String SWIM_NAME = "HexSwim";

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        AttributeInstance spd = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd == null) return;
        PlayerData data = ModCapabilities.get(sp);
        double boost = data == null ? 0 : HextechEvents.effect(data, "swim_swift");
        boolean want = boost > 0 && sp.isInWater();
        boolean has = spd.getModifier(SWIM_UUID) != null;
        if (want && !has) {
            spd.addTransientModifier(new AttributeModifier(SWIM_UUID, SWIM_NAME, boost,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!want && has) {
            spd.removeModifier(SWIM_UUID);
        }
    }
}
