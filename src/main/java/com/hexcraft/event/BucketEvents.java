package com.hexcraft.event;

import com.hexcraft.capability.ModCapabilities;
import com.hexcraft.capability.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 桶类专属海克斯事件（v0.9.12 新增）。
 * bucket_safe：舀取岩浆时熄灭身上的火并获得短暂防火，避免被岩浆烧伤。
 * 数值由玩家海克斯 event token 数据驱动，不写死 id。
 */
public class BucketEvents {

    @SubscribeEvent
    public void onFillBucket(FillBucketEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerData data = ModCapabilities.get(sp);
        if (data == null || HextechEvents.effect(data, "bucket_safe") <= 0) return;
        boolean lava = event.getFilledBucket().is(Items.LAVA_BUCKET);
        if (!lava && event.getTarget() instanceof BlockHitResult bhr) {
            lava = event.getLevel().getBlockState(bhr.getBlockPos()).is(Blocks.LAVA);
        }
        if (!lava) return;
        sp.clearFire();
        sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
    }
}
