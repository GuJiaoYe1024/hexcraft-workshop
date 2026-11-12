package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * 骑乘专属海克斯事件（v0.9.12 新增）。
 * mount_speed：坐骑移动速度提升；mount_health：坐骑生命上限提升；mount_attack：骑乘攻击增伤。
 * 下马时移除加到坐骑上的属性修正。数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class MountEvents {
    private static final UUID SPEED_UUID = UUID.fromString("d4e5f6a7-1234-4b5c-9d8e-7f6a5b4c3d2e");
    private static final UUID HEALTH_UUID = UUID.fromString("e5f6a7b8-2345-4c6d-ae9f-8a7b6c5d4e3f");

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        if (!(sp.getControlledVehicle() instanceof LivingEntity mount)) return;
        PlayerData data = ModCapabilities.get(sp);
        double speed = data == null ? 0 : HextechEvents.effect(data, "mount_speed");
        double health = data == null ? 0 : HextechEvents.effect(data, "mount_health");
        syncModifier(mount, Attributes.MOVEMENT_SPEED, SPEED_UUID, "HexMountSpeed", speed);
        boolean healthAdded = syncModifier(mount, Attributes.MAX_HEALTH, HEALTH_UUID, "HexMountHealth", health);
        // 首次加生命上限时把当前生命补到新上限，让生命加成体现为实际生命
        if (healthAdded) mount.setHealth(mount.getMaxHealth());
    }

    @SubscribeEvent
    public void onDismount(EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof LivingEntity mount) {
            removeIfPresent(mount, Attributes.MOVEMENT_SPEED, SPEED_UUID);
            removeIfPresent(mount, Attributes.MAX_HEALTH, HEALTH_UUID);
        }
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer sp)) return;
        if (!sp.isPassenger()) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null) return;
        double atk = HextechEvents.effect(data, "mount_attack");
        if (atk > 0) event.setAmount(event.getAmount() * (float) (1 + atk));
    }

    /** 同步某属性上的固定 modifier，返回是否本次新加 */
    private static boolean syncModifier(LivingEntity mount, Attribute attr, UUID uuid, String name, double amount) {
        AttributeInstance inst = mount.getAttribute(attr);
        if (inst == null) return false;
        boolean has = inst.getModifier(uuid) != null;
        if (amount > 0 && !has) {
            inst.addTransientModifier(new AttributeModifier(uuid, name, amount,
                    AttributeModifier.Operation.MULTIPLY_BASE));
            return true;
        } else if (amount <= 0 && has) {
            inst.removeModifier(uuid);
        }
        return false;
    }

    private static void removeIfPresent(LivingEntity mount, Attribute attr, UUID uuid) {
        AttributeInstance inst = mount.getAttribute(attr);
        if (inst != null && inst.getModifier(uuid) != null) inst.removeModifier(uuid);
    }
}
