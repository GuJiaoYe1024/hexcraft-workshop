package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 畜牧繁殖类专属海克斯事件（v0.9.12 新增）。
 * breed_swift：繁殖出的幼崽生长时间减半，更快成年。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class BreedEvents {

    @SubscribeEvent
    public void onBabyEntitySpawn(BabyEntitySpawnEvent event) {
        Player breeder = event.getCausedByPlayer();
        if (!(breeder instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null || HextechEvents.effect(data, "breed_swift") <= 0) return;
        AgeableMob child = event.getChild();
        if (child == null) return;
        int age = child.getAge();
        if (age < 0) {
            child.setAge(age / 2);
        }
    }
}
